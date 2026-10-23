package com.hearthkitchens.production.service

import com.hearthkitchens.production.domain.Ingredient
import com.hearthkitchens.production.domain.MovementType
import com.hearthkitchens.production.domain.ProductionOrder
import com.hearthkitchens.production.domain.StockMovement
import com.hearthkitchens.production.repository.IngredientRepository
import com.hearthkitchens.production.repository.StockMovementRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * The only place stock balances change. Each change updates the ingredient and records
 * its movement together, so the history always explains the numbers.
 */
@Service
class StockService(
    private val ingredientRepository: IngredientRepository,
    private val movementRepository: StockMovementRepository,
) {

    @Transactional(readOnly = true)
    fun listStock(): List<IngredientStock> =
        ingredientRepository.findAllByOrderByNameAsc().map { it.toStock() }

    @Transactional(readOnly = true)
    fun getIngredient(ingredientId: Long): IngredientDetail {
        val ingredient = ingredientRepository.findByIdOrNull(ingredientId)
            ?: throw NotFoundException("Ingredient $ingredientId not found")
        val movements = movementRepository.findAllForIngredient(ingredientId).map { it.toView() }
        return IngredientDetail(ingredient.toStock(), movements)
    }

    // The methods below must run inside the caller's transaction (MANDATORY), so the
    // balance change, the movement and the order's status change commit or roll back together.

    @Transactional(propagation = Propagation.MANDATORY)
    fun reserve(ingredient: Ingredient, order: ProductionOrder, quantity: BigDecimal) {
        ingredient.reserve(quantity)
        record(ingredient, order, MovementType.RESERVE, quantity)
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun unreserve(ingredient: Ingredient, order: ProductionOrder, quantity: BigDecimal) {
        ingredient.unreserve(quantity)
        record(ingredient, order, MovementType.UNRESERVE, quantity)
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun consume(ingredient: Ingredient, order: ProductionOrder, quantity: BigDecimal) {
        ingredient.consume(quantity)
        record(ingredient, order, MovementType.CONSUME, quantity)
    }

    private fun record(ingredient: Ingredient, order: ProductionOrder, type: MovementType, quantity: BigDecimal) {
        movementRepository.save(StockMovement(ingredient = ingredient, order = order, type = type, quantity = quantity))
    }
}
