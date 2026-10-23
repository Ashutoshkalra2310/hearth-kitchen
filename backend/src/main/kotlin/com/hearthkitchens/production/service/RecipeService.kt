package com.hearthkitchens.production.service

import com.hearthkitchens.production.repository.RecipeRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecipeService(private val recipeRepository: RecipeRepository) {

    @Transactional(readOnly = true)
    fun listRecipes(): List<RecipeView> =
        recipeRepository.findAll(Sort.by("name")).map { it.toView() }
}
