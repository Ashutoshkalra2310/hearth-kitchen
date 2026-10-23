package com.hearthkitchens.production.service

import com.hearthkitchens.production.domain.OrderRequirement
import com.hearthkitchens.production.domain.OrderStatus
import com.hearthkitchens.production.domain.ProductionOrder
import com.hearthkitchens.production.repository.IngredientRepository
import com.hearthkitchens.production.repository.ProductionOrderRepository
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
    private val recipeRepository: RecipeRepository,
    private val ingredientRepository: IngredientRepository,
    private val movementRepository: StockMovementRepository,
    private val stockService: StockService,
) {

    @Transactional(readOnly = true)
    fun list(status: OrderStatus?): List<OrderSummary> {
        val orders = if (status == null) {
            orderRepository.findAllWithRecipe()
        } else {
            orderRepository.findAllWithRecipeByStatus(status)
        }
        return orders.map { it.toSummary() }
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
        val recipe = recipeRepository.findByIdOrNull(recipeId)
            ?: throw NotFoundException("Recipe $recipeId not found")

        val order = ProductionOrder(recipe = recipe, batches = batches)
        recipe.lines.forEach { line ->
            order.requirements += OrderRequirement(
                order = order,
                ingredient = line.ingredient,
                requiredQuantity = line.quantityPerBatch * batches.toBigDecimal(),
            )
        }
        return toDetail(orderRepository.save(order))
    }

    /**
     * Reserves every requirement, or nothing: if any ingredient is short, throws
     * [InsufficientStockException] listing all the short ingredients.
     */
    @Transactional
    fun release(orderId: Long): OrderDetail {
        val order = lockOrder(orderId)
        requireStatus(order, "release", OrderStatus.DRAFT)
        ingredientRepository.lockAllRequiredBy(orderId)

        val shortages = order.requirements
            .filter { it.ingredient.available < it.requiredQuantity }
            .map {
                Shortage(
                    ingredientId = it.ingredient.id!!,
                    ingredientName = it.ingredient.name,
                    unit = it.ingredient.unit,
                    required = it.requiredQuantity,
                    available = it.ingredient.available,
                    shortfall = it.requiredQuantity - it.ingredient.available,
                )
            }
        if (shortages.isNotEmpty()) throw InsufficientStockException(orderId, shortages)

        order.requirements.forEach { stockService.reserve(it.ingredient, order, it.requiredQuantity) }
        order.status = OrderStatus.RELEASED
        order.releasedAt = Instant.now()
        return toDetail(order)
    }

    /** Consumes the reserved stock. Reservation is all or nothing, so it equals the requirements. */
    @Transactional
    fun complete(orderId: Long): OrderDetail {
        val order = lockOrder(orderId)
        requireStatus(order, "complete", OrderStatus.RELEASED)
        ingredientRepository.lockAllRequiredBy(orderId)

        order.requirements.forEach { stockService.consume(it.ingredient, order, it.requiredQuantity) }
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
            ingredientRepository.lockAllRequiredBy(orderId)
            order.requirements.forEach { stockService.unreserve(it.ingredient, order, it.requiredQuantity) }
        }
        order.status = OrderStatus.CANCELLED
        order.cancelledAt = Instant.now()
        return toDetail(order)
    }

    private fun lockOrder(orderId: Long): ProductionOrder =
        orderRepository.findByIdForUpdate(orderId) ?: throw orderNotFound(orderId)

    private fun requireStatus(order: ProductionOrder, action: String, vararg allowed: OrderStatus) {
        if (order.status !in allowed) throw InvalidOrderStatusException(order.id!!, order.status, action)
    }

    private fun toDetail(order: ProductionOrder): OrderDetail =
        order.toDetail(movementRepository.findAllForOrder(order.id!!).map { it.toView() })

    private fun orderNotFound(orderId: Long) = NotFoundException("Order PO-$orderId not found")

    companion object {
        const val MAX_BATCHES = 1000
    }
}
