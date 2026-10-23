package com.hearthkitchens.production.service

import com.hearthkitchens.production.domain.Ingredient
import com.hearthkitchens.production.domain.MovementType
import com.hearthkitchens.production.domain.OrderRequirement
import com.hearthkitchens.production.domain.OrderStatus
import com.hearthkitchens.production.domain.ProductionOrder
import com.hearthkitchens.production.domain.Recipe
import com.hearthkitchens.production.repository.IngredientRepository
import com.hearthkitchens.production.repository.OrderRequirementRepository
import com.hearthkitchens.production.repository.ProductionOrderRepository
import com.hearthkitchens.production.repository.RecipeIngredientRepository
import com.hearthkitchens.production.repository.RecipeRepository
import com.hearthkitchens.production.repository.StockMovementRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * Order lifecycle: DRAFT -> RELEASED -> COMPLETED, and DRAFT/RELEASED -> CANCELLED.
 *
 * Each action runs in one transaction: any exception rolls back every stock change made so far.
 * Actions lock the order row first, then its ingredient rows in id order, so concurrent
 * requests on the same order or the same stock run one after the other.
 */
@Service
class ProductionOrderService(
    private val orderRepository: ProductionOrderRepository,
    private val requirementRepository: OrderRequirementRepository,
    private val recipeRepository: RecipeRepository,
    private val recipeIngredientRepository: RecipeIngredientRepository,
    private val ingredientRepository: IngredientRepository,
    private val movementRepository: StockMovementRepository,
    private val stockService: StockService,
) {

    @Transactional(readOnly = true)
    fun list(status: OrderStatus?): List<OrderSummary> {
        val orders = if (status == null) {
            orderRepository.findAllByOrderByIdDesc()
        } else {
            orderRepository.findAllByStatusOrderByIdDesc(status)
        }
        val recipes = recipeRepository.findAllById(orders.map { it.recipeId }.toSet()).associateBy { it.id!! }
        return orders.map { it.toSummary(recipes.getValue(it.recipeId)) }
    }

    @Transactional(readOnly = true)
    fun get(orderId: Long): OrderDetail {
        val order = orderRepository.findByIdOrNull(orderId) ?: throw orderNotFound(orderId)
        return toDetail(order)
    }

    /** Creates a draft order and snapshots the quantities it needs from the recipe. */
    @Transactional
    fun create(recipeId: Long, batches: Int): OrderDetail {
        require(batches in 1..MAX_BATCHES) { "Batches must be between 1 and $MAX_BATCHES" }
        if (!recipeRepository.existsById(recipeId)) throw NotFoundException("Recipe $recipeId not found")

        val order = orderRepository.save(ProductionOrder(recipeId = recipeId, batches = batches))
        val requirements = recipeIngredientRepository.findAllForRecipe(recipeId).map { line ->
            OrderRequirement(
                orderId = order.id!!,
                ingredientId = line.ingredientId,
                requiredQuantity = line.quantityPerBatch * batches.toBigDecimal(),
            )
        }
        requirementRepository.saveAll(requirements)
        return toDetail(order)
    }

    /**
     * Reserves every requirement, or nothing. Each requirement is reserved from its own ingredient
     * or, if that can't cover it, entirely from the ingredient's substitute. If any requirement
     * can't be covered either way, throws [InsufficientStockException] listing all of them.
     */
    @Transactional
    fun release(orderId: Long): OrderDetail {
        val order = lockOrder(orderId)
        requireStatus(order, "release", OrderStatus.DRAFT)
        val requirements = requirementRepository.findAllForOrder(orderId)
        val requiredIds = requirements.map { it.ingredientId }
        val ingredients = lockIngredients(requiredIds + ingredientRepository.findSubstituteIds(requiredIds))

        // Plan every reservation before changing anything. `remaining` tracks what's left of each
        // ingredient, so two requirements can't both count on the same stock.
        val remaining = ingredients.mapValues { it.value.available }.toMutableMap()
        val reservations = mutableListOf<Pair<Ingredient, BigDecimal>>()
        val shortages = mutableListOf<Shortage>()

        for (requirement in requirements) {
            val quantity = requirement.requiredQuantity
            val ingredient = ingredients.getValue(requirement.ingredientId)
            val substitute = usableSubstitute(ingredient, ingredients)
            val source = listOfNotNull(ingredient, substitute).firstOrNull { remaining.getValue(it.id!!) >= quantity }

            if (source == null) {
                val available = remaining.getValue(ingredient.id!!)
                shortages += Shortage(
                    ingredientId = ingredient.id!!,
                    ingredientName = ingredient.name,
                    unit = ingredient.unit,
                    required = quantity,
                    available = available,
                    shortfall = quantity - available,
                    substituteName = substitute?.name,
                    substituteAvailable = substitute?.let { remaining.getValue(it.id!!) },
                )
            } else {
                remaining[source.id!!] = remaining.getValue(source.id!!) - quantity
                reservations += source to quantity
            }
        }
        if (shortages.isNotEmpty()) throw InsufficientStockException(orderId, shortages)

        reservations.forEach { (ingredient, quantity) -> stockService.reserve(ingredient, orderId, quantity) }
        order.status = OrderStatus.RELEASED
        order.releasedAt = Instant.now()
        return toDetail(order)
    }

    /** Consumes exactly what the order reserved (which may include substitutes). */
    @Transactional
    fun complete(orderId: Long): OrderDetail {
        val order = lockOrder(orderId)
        requireStatus(order, "complete", OrderStatus.RELEASED)

        val reserved = reservedMovements(orderId)
        val ingredients = lockIngredients(reserved.map { it.ingredientId })
        reserved.forEach { stockService.consume(ingredients.getValue(it.ingredientId), orderId, it.quantity) }
        order.status = OrderStatus.COMPLETED
        order.completedAt = Instant.now()
        return toDetail(order)
    }

    /** Cancels a draft or released order, freeing exactly what a released order had reserved. */
    @Transactional
    fun cancel(orderId: Long): OrderDetail {
        val order = lockOrder(orderId)
        requireStatus(order, "cancel", OrderStatus.DRAFT, OrderStatus.RELEASED)

        if (order.status == OrderStatus.RELEASED) {
            val reserved = reservedMovements(orderId)
            val ingredients = lockIngredients(reserved.map { it.ingredientId })
            reserved.forEach { stockService.unreserve(ingredients.getValue(it.ingredientId), orderId, it.quantity) }
        }
        order.status = OrderStatus.CANCELLED
        order.cancelledAt = Instant.now()
        return toDetail(order)
    }

    private fun lockOrder(orderId: Long): ProductionOrder =
        orderRepository.findByIdForUpdate(orderId) ?: throw orderNotFound(orderId)

    /** Locks the ingredient rows and returns them by id. */
    private fun lockIngredients(ids: Collection<Long>): Map<Long, Ingredient> =
        ingredientRepository.lockAllById(ids.toSet()).associateBy { it.id!! }

    /** What a released order is holding: its RESERVE movements. */
    private fun reservedMovements(orderId: Long) =
        movementRepository.findAllForOrder(orderId).filter { it.type == MovementType.RESERVE }

    private fun requireStatus(order: ProductionOrder, action: String, vararg allowed: OrderStatus) {
        if (order.status !in allowed) throw InvalidOrderStatusException(order.id!!, order.status, action)
    }

    private fun toDetail(order: ProductionOrder): OrderDetail {
        val orderId = order.id!!
        val recipe: Recipe = recipeRepository.findByIdOrNull(order.recipeId)
            ?: throw NotFoundException("Recipe ${order.recipeId} not found")
        val requirements = requirementRepository.findAllForOrder(orderId)
        val movements = movementRepository.findAllForOrder(orderId)
        val requiredIds = requirements.map { it.ingredientId }
        val ingredientIds = requiredIds + movements.map { it.ingredientId } +
            ingredientRepository.findSubstituteIds(requiredIds)
        val ingredients = ingredientRepository.findAllById(ingredientIds.toSet()).associateBy { it.id!! }
        val reservedIds = movements.filter { it.type == MovementType.RESERVE }.map { it.ingredientId }.toSet()

        return order.toDetail(
            recipe = recipe,
            requirements = requirements.map { requirement ->
                val ingredient = ingredients.getValue(requirement.ingredientId)
                val substitute = usableSubstitute(ingredient, ingredients)
                requirement.toView(
                    ingredient = ingredient,
                    substitute = substitute,
                    substituteUsed = substitute != null &&
                        requirement.ingredientId !in reservedIds && substitute.id in reservedIds,
                )
            },
            movements = movements.map { it.toView(ingredients.getValue(it.ingredientId)) },
        )
    }

    /** The ingredient's substitute, if it has one with the same unit (substitutes are used 1:1). */
    private fun usableSubstitute(ingredient: Ingredient, ingredients: Map<Long, Ingredient>): Ingredient? =
        ingredient.substituteId?.let { ingredients[it] }?.takeIf { it.unit == ingredient.unit }

    private fun orderNotFound(orderId: Long) = NotFoundException("Order PO-$orderId not found")

    companion object {
        const val MAX_BATCHES = 1000
    }
}
