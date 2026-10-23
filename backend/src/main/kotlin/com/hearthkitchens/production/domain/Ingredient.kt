package com.hearthkitchens.production.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal

@Entity
class Ingredient(
    @Column(nullable = false, unique = true)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val unit: BaseUnit,

    @Column(nullable = false)
    var onHand: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var reserved: BigDecimal = BigDecimal.ZERO,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    val available: BigDecimal
        get() = onHand - reserved
}
