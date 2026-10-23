package com.hearthkitchens.production.service

import com.hearthkitchens.production.domain.Ingredient
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
     * Reserves every requirement, or nothing: if any ingredient is short, throws
     * [InsufficientStockException] listing all the short ingredients.
     */
    @Transactional
    fun release(orderId: Long): OrderDetail {
        val order = lockOrder(orderId)
        requireStatus(order, "release", OrderStatus.DRAFT)
        val ingredients = lockIngredients(orderId)
        val requirements = requirementRepository.findAllForOrder(orderId)

        val shortages = requirements.mapNotNull { requirement ->
            val ingredient = ingredients.getValue(requirement.ingredientId)
            if (ingredient.available >= requirement.requiredQuantity) return@mapNotNull null
            Shortage(
                ingredientId = requirement.ingredientId,
                ingredientName = ingredient.name,
                unit = ingredient.unit,
                required = requirement.requiredQuantity,
                available = ingredient.available,
                shortfall = requirement.requiredQuantity - ingredient.available,
            )
        }
        if (shortages.isNotEmpty()) throw InsufficientStockException(orderId, shortages)

        requirements.forEach {
            stockService.reserve(ingredients.getValue(it.ingredientId), orderId, it.requiredQuantity)
        }
        order.status = OrderStatus.RELEASED
        order.releasedAt = Instant.now()
        return toDetail(order)
    }

    /** Consumes the reserved stock. Reservation is all or nothing, so it equals the requirements. */
    @Transactional
    fun complete(orderId: Long): OrderDetail {
        val order = lockOrder(orderId)
        requireStatus(order, "complete", OrderStatus.RELEASED)
        val ingredients = lockIngredients(orderId)

        requirementRepository.findAllForOrder(orderId).forEach {
            stockService.consume(ingredients.getValue(it.ingredientId), orderId, it.requiredQuantity)
        }
        order.status = OrderStatus.COMPLETED
        order.completedAt = Instant.now()
        return toDetail(order)
    }

    /** Cancels a draft or released order, freeing anything a released order had reserved. */
    @Transactional
    fun cancel(orderId: Long): OrderDetail {
        val order = lockOrder(orderId)
        requireStatus(order, "cancel", OrderStatus.DRAFT, OrderStatus.RELEASED)

        if (order.status == OrderStatus.RELEASED) {
            val ingredients = lockIngredients(orderId)
            requirementRepository.findAllForOrder(orderId).forEach {
                stockService.unreserve(ingredients.getValue(it.ingredientId), orderId, it.requiredQuantity)
            }
        }
        order.status = OrderStatus.CANCELLED
        order.cancelledAt = Instant.now()
        return toDetail(order)
    }

    private fun lockOrder(orderId: Long): ProductionOrder =
        orderRepository.findByIdForUpdate(orderId) ?: throw orderNotFound(orderId)

    /** Locks the order's ingredient rows and returns them by id. */
    private fun lockIngredients(orderId: Long): Map<Long, Ingredient> =
        ingredientRepository.lockAllRequiredBy(orderId).associateBy { it.id!! }

    private fun requireStatus(order: ProductionOrder, action: String, vararg allowed: OrderStatus) {
        if (order.status !in allowed) throw InvalidOrderStatusException(order.id!!, order.status, action)
    }

    private fun toDetail(order: ProductionOrder): OrderDetail {
        val orderId = order.id!!
        val recipe: Recipe = recipeRepository.findByIdOrNull(order.recipeId)
            ?: throw NotFoundException("Recipe ${order.recipeId} not found")
        val requirements = requirementRepository.findAllForOrder(orderId)
        val movements = movementRepository.findAllForOrder(orderId)
        val ingredientIds = requirements.map { it.ingredientId } + movements.map { it.ingredientId }
        val ingredients = ingredientRepository.findAllById(ingredientIds.toSet()).associateBy { it.id!! }

        return order.toDetail(
            recipe = recipe,
            requirements = requirements.map { it.toView(ingredients.getValue(it.ingredientId)) },
            movements = movements.map { it.toView(ingredients.getValue(it.ingredientId)) },
        )
    }

    private fun orderNotFound(orderId: Long) = NotFoundException("Order PO-$orderId not found")

    companion object {
        const val MAX_BATCHES = 1000
    }
}
