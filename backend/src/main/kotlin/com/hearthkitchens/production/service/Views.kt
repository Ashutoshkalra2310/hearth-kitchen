package com.hearthkitchens.production.service

import com.hearthkitchens.production.domain.BaseUnit
import com.hearthkitchens.production.domain.Ingredient
import com.hearthkitchens.production.domain.MovementType
import com.hearthkitchens.production.domain.OrderRequirement
import com.hearthkitchens.production.domain.OrderStatus
import com.hearthkitchens.production.domain.ProductionOrder
import com.hearthkitchens.production.domain.Recipe
import com.hearthkitchens.production.domain.RecipeIngredient
import com.hearthkitchens.production.domain.StockMovement
import java.math.BigDecimal
import java.time.Instant

// Read models returned by the services. Entities only hold ids of related rows, so the
// mapping functions take the related entities (e.g. the ingredient for its name and unit).
// All quantities are in `unit`.

data class IngredientStock(
    val id: Long,
    val name: String,
    val unit: BaseUnit,
    val onHand: BigDecimal,
    val reserved: BigDecimal,
    val available: BigDecimal,
    val substituteId: Long?,
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
    /** Ingredient used instead (whole requirement, 1:1) when this one can't cover it. */
    val substituteName: String?,
    val substituteAvailable: BigDecimal?,
    /** True once the order is released and the requirement was reserved from the substitute. */
    val substituteUsed: Boolean,
)

data class RecipeView(
    val id: Long,
    val name: String,
    val yieldQuantity: BigDecimal,
    val yieldUnit: String,
    val lines: List<RecipeLineView>,
)

data class RecipeLineView(
    val ingredientId: Long,
    val ingredientName: String,
    val unit: BaseUnit,
    val quantityPerBatch: BigDecimal,
)

data class Shortage(
    val ingredientId: Long,
    val ingredientName: String,
    val unit: BaseUnit,
    val required: BigDecimal,
    val available: BigDecimal,
    val shortfall: BigDecimal,
    /** Set when the ingredient has a substitute that couldn't cover the requirement either. */
    val substituteName: String? = null,
    val substituteAvailable: BigDecimal? = null,
)

fun Ingredient.toStock() = IngredientStock(
    id = id!!,
    name = name,
    unit = unit,
    onHand = onHand,
    reserved = reserved,
    available = available,
    substituteId = substituteId,
)

fun StockMovement.toView(ingredient: Ingredient) = MovementView(
    id = id!!,
    type = type,
    quantity = quantity,
    createdAt = createdAt,
    ingredientId = ingredientId,
    ingredientName = ingredient.name,
    unit = ingredient.unit,
    orderId = orderId,
)

fun ProductionOrder.toSummary(recipe: Recipe) = OrderSummary(
    id = id!!,
    recipeName = recipe.name,
    batches = batches,
    status = status,
    createdAt = createdAt,
)

fun ProductionOrder.toDetail(recipe: Recipe, requirements: List<RequirementView>, movements: List<MovementView>) =
    OrderDetail(
        id = id!!,
        recipeId = recipeId,
        recipeName = recipe.name,
        yieldQuantity = recipe.yieldQuantity,
        yieldUnit = recipe.yieldUnit,
        batches = batches,
        status = status,
        createdAt = createdAt,
        releasedAt = releasedAt,
        completedAt = completedAt,
        cancelledAt = cancelledAt,
        requirements = requirements,
        movements = movements,
    )

fun OrderRequirement.toView(ingredient: Ingredient, substitute: Ingredient?, substituteUsed: Boolean) =
    RequirementView(
        ingredientId = ingredientId,
        ingredientName = ingredient.name,
        unit = ingredient.unit,
        requiredQuantity = requiredQuantity,
        available = ingredient.available,
        substituteName = substitute?.name,
        substituteAvailable = substitute?.available,
        substituteUsed = substituteUsed,
    )

fun RecipeIngredient.toView(ingredient: Ingredient) = RecipeLineView(
    ingredientId = ingredientId,
    ingredientName = ingredient.name,
    unit = ingredient.unit,
    quantityPerBatch = quantityPerBatch,
)

fun Recipe.toView(lines: List<RecipeLineView>) = RecipeView(
    id = id!!,
    name = name,
    yieldQuantity = yieldQuantity,
    yieldUnit = yieldUnit,
    lines = lines,
)
