package com.hearthkitchens.production.repository

import com.hearthkitchens.production.domain.Ingredient
import com.hearthkitchens.production.domain.ProductionOrder
import com.hearthkitchens.production.domain.Recipe
import com.hearthkitchens.production.domain.StockMovement
import org.springframework.data.jpa.repository.JpaRepository

interface IngredientRepository : JpaRepository<Ingredient, Long>

interface RecipeRepository : JpaRepository<Recipe, Long>

interface ProductionOrderRepository : JpaRepository<ProductionOrder, Long>

interface StockMovementRepository : JpaRepository<StockMovement, Long>
