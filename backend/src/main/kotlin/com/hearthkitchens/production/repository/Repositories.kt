package com.hearthkitchens.production.repository

import com.hearthkitchens.production.domain.Ingredient
import com.hearthkitchens.production.domain.OrderStatus
import com.hearthkitchens.production.domain.ProductionOrder
import com.hearthkitchens.production.domain.Recipe
import com.hearthkitchens.production.domain.StockMovement
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface IngredientRepository : JpaRepository<Ingredient, Long> {

    fun findAllByOrderByNameAsc(): List<Ingredient>

    /**
     * Locks (SELECT ... FOR UPDATE) every ingredient the order needs, in id order so two
     * transactions always lock in the same sequence and can't deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select i from Ingredient i
        where i.id in (select r.ingredient.id from OrderRequirement r where r.order.id = :orderId)
        order by i.id
        """
    )
    fun lockAllRequiredBy(orderId: Long): List<Ingredient>
}

interface RecipeRepository : JpaRepository<Recipe, Long>

interface ProductionOrderRepository : JpaRepository<ProductionOrder, Long> {

    @Query("select o from ProductionOrder o join fetch o.recipe order by o.id desc")
    fun findAllWithRecipe(): List<ProductionOrder>

    @Query("select o from ProductionOrder o join fetch o.recipe where o.status = :status order by o.id desc")
    fun findAllWithRecipeByStatus(status: OrderStatus): List<ProductionOrder>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ProductionOrder o where o.id = :id")
    fun findByIdForUpdate(id: Long): ProductionOrder?
}

interface StockMovementRepository : JpaRepository<StockMovement, Long> {

    @Query("select m from StockMovement m join fetch m.ingredient where m.order.id = :orderId order by m.id")
    fun findAllForOrder(orderId: Long): List<StockMovement>

    @Query("select m from StockMovement m left join fetch m.order where m.ingredient.id = :ingredientId order by m.id")
    fun findAllForIngredient(ingredientId: Long): List<StockMovement>
}
