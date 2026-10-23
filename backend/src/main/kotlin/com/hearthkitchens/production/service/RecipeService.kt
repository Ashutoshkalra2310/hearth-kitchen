package com.hearthkitchens.production.service

import com.hearthkitchens.production.repository.IngredientRepository
import com.hearthkitchens.production.repository.RecipeIngredientRepository
import com.hearthkitchens.production.repository.RecipeRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecipeService(
    private val recipeRepository: RecipeRepository,
    private val recipeIngredientRepository: RecipeIngredientRepository,
    private val ingredientRepository: IngredientRepository,
) {

    @Transactional(readOnly = true)
    fun listRecipes(): List<RecipeView> {
        val ingredients = ingredientRepository.findAll().associateBy { it.id!! }
        val linesByRecipe = recipeIngredientRepository.findAll(Sort.by("id")).groupBy { it.recipeId }
        return recipeRepository.findAll(Sort.by("name")).map { recipe ->
            val lines = linesByRecipe[recipe.id].orEmpty().map { it.toView(ingredients.getValue(it.ingredientId)) }
            recipe.toView(lines)
        }
    }
}
