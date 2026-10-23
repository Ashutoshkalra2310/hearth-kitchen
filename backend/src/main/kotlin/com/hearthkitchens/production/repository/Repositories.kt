package com.hearthkitchens.production.repository

import com.hearthkitchens.production.domain.Ingredient
import com.hearthkitchens.production.domain.OrderRequirement
import com.hearthkitchens.production.domain.OrderStatus
import com.hearthkitchens.production.domain.ProductionOrder
import com.hearthkitchens.production.domain.Recipe
import com.hearthkitchens.production.domain.RecipeIngredient
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
        where i.id in (select r.ingredientId from OrderRequirement r where r.orderId = :orderId)
        order by i.id
        """
    )
    fun lockAllRequiredBy(orderId: Long): List<Ingredient>
}

interface RecipeRepository : JpaRepository<Recipe, Long>

interface RecipeIngredientRepository : JpaRepository<RecipeIngredient, Long> {

    @Query("select l from RecipeIngredient l where l.recipeId = :recipeId order by l.id")
    fun findAllForRecipe(recipeId: Long): List<RecipeIngredient>
}

interface OrderRequirementRepository : JpaRepository<OrderRequirement, Long> {

    @Query("select r from OrderRequirement r where r.orderId = :orderId order by r.id")
    fun findAllForOrder(orderId: Long): List<OrderRequirement>
}

interface ProductionOrderRepository : JpaRepository<ProductionOrder, Long> {

    fun findAllByOrderByIdDesc(): List<ProductionOrder>

    fun findAllByStatusOrderByIdDesc(status: OrderStatus): List<ProductionOrder>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ProductionOrder o where o.id = :id")
    fun findByIdForUpdate(id: Long): ProductionOrder?
}

interface StockMovementRepository : JpaRepository<StockMovement, Long> {

    @Query("select m from StockMovement m where m.orderId = :orderId order by m.id")
    fun findAllForOrder(orderId: Long): List<StockMovement>

    @Query("select m from StockMovement m where m.ingredientId = :ingredientId order by m.id")
    fun findAllForIngredient(ingredientId: Long): List<StockMovement>
}
