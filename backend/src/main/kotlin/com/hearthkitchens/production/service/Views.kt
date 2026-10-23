package com.hearthkitchens.production.service

import com.hearthkitchens.production.domain.BaseUnit
import com.hearthkitchens.production.domain.Ingredient
import com.hearthkitchens.production.domain.MovementType
import com.hearthkitchens.production.domain.OrderRequirement
import com.hearthkitchens.production.domain.OrderStatus
import com.hearthkitchens.production.domain.ProductionOrder
import com.hearthkitchens.production.domain.StockMovement
import java.math.BigDecimal
import java.time.Instant

// Read models returned by the services. Built inside a transaction so lazy
// relations are loaded before they leave the service. All quantities are in `unit`.

data class IngredientStock(
    val id: Long,
    val name: String,
    val unit: BaseUnit,
    val onHand: BigDecimal,
    val reserved: BigDecimal,
    val available: BigDecimal,
)

data class IngredientDetail(
    val stock: IngredientStock,
    val movements: List<MovementView>,
)

data class MovementView(
    val id: Long,
    val type: MovementType,
    val quantity: BigDecimal,
    val createdAt: Instant,
    val ingredientId: Long,
    val ingredientName: String,
    val unit: BaseUnit,
    val orderId: Long?,
)

data class OrderSummary(
    val id: Long,
    val recipeName: String,
    val batches: Int,
    val status: OrderStatus,
    val createdAt: Instant,
)

data class OrderDetail(
    val id: Long,
    val recipeId: Long,
    val recipeName: String,
    val yieldQuantity: BigDecimal,
    val yieldUnit: String,
    val batches: Int,
    val status: OrderStatus,
    val createdAt: Instant,
    val releasedAt: Instant?,
    val completedAt: Instant?,
    val cancelledAt: Instant?,
    val requirements: List<RequirementView>,
    val movements: List<MovementView>,
)

data class RequirementView(
    val ingredientId: Long,
    val ingredientName: String,
    val unit: BaseUnit,
    val requiredQuantity: BigDecimal,
    /** Current available stock, so a draft order shows what would be short. */
    val available: BigDecimal,
)

data class Shortage(
    val ingredientId: Long,
    val ingredientName: String,
    val unit: BaseUnit,
    val required: BigDecimal,
    val available: BigDecimal,
    val shortfall: BigDecimal,
)

fun Ingredient.toStock() = IngredientStock(
    id = id!!,
    name = name,
    unit = unit,
    onHand = onHand,
    reserved = reserved,
    available = available,
)

fun StockMovement.toView() = MovementView(
    id = id!!,
    type = type,
    quantity = quantity,
    createdAt = createdAt,
    ingredientId = ingredient.id!!,
    ingredientName = ingredient.name,
    unit = ingredient.unit,
    orderId = order?.id,
)

fun ProductionOrder.toSummary() = OrderSummary(
    id = id!!,
    recipeName = recipe.name,
    batches = batches,
    status = status,
    createdAt = createdAt,
)

fun ProductionOrder.toDetail(movements: List<MovementView>) = OrderDetail(
    id = id!!,
    recipeId = recipe.id!!,
    recipeName = recipe.name,
    yieldQuantity = recipe.yieldQuantity,
    yieldUnit = recipe.yieldUnit,
    batches = batches,
    status = status,
    createdAt = createdAt,
    releasedAt = releasedAt,
    completedAt = completedAt,
    cancelledAt = cancelledAt,
    requirements = requirements.map { it.toView() },
    movements = movements,
)

fun OrderRequirement.toView() = RequirementView(
    ingredientId = ingredient.id!!,
    ingredientName = ingredient.name,
    unit = ingredient.unit,
    requiredQuantity = requiredQuantity,
    available = ingredient.available,
)
