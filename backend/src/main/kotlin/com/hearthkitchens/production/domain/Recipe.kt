package com.hearthkitchens.production.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.math.BigDecimal

@Entity
class Recipe(
    @Column(nullable = false, unique = true)
    val name: String,

    @Column(nullable = false)
    val yieldQuantity: BigDecimal,

    @Column(nullable = false)
    val yieldUnit: String,

    @OneToMany(mappedBy = "recipe")
    val lines: MutableList<RecipeIngredient> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

@Entity
class RecipeIngredient(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    val recipe: Recipe,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    val ingredient: Ingredient,

    @Column(nullable = false)
    val quantityPerBatch: BigDecimal,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
