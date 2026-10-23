package com.hearthkitchens.production.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal

@Entity
class Recipe(
    @Column(nullable = false, unique = true)
    val name: String,

    @Column(nullable = false)
    val yieldQuantity: BigDecimal,

    @Column(nullable = false)
    val yieldUnit: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

@Entity
class RecipeIngredient(
    @Column(nullable = false)
    val recipeId: Long,

    @Column(nullable = false)
    val ingredientId: Long,

    @Column(nullable = false)
    val quantityPerBatch: BigDecimal,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
