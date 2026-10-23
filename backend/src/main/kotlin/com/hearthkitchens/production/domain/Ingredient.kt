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

    /** Ingredient to use instead (same unit, 1:1) when this one can't cover a requirement. */
    val substituteId: Long? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) {
    val available: BigDecimal
        get() = onHand - reserved

    // Balance changes. Callers must also record a StockMovement (see StockService).

    fun reserve(quantity: BigDecimal) {
        check(quantity <= available) { "Cannot reserve $quantity of $name: only $available available" }
        reserved += quantity
    }

    fun unreserve(quantity: BigDecimal) {
        check(quantity <= reserved) { "Cannot unreserve $quantity of $name: only $reserved reserved" }
        reserved -= quantity
    }

    fun consume(quantity: BigDecimal) {
        check(quantity <= reserved) { "Cannot consume $quantity of $name: only $reserved reserved" }
        reserved -= quantity
        onHand -= quantity
    }
}
