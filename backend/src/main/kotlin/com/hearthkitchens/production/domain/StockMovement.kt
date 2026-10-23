package com.hearthkitchens.production.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.Instant

@Entity
class StockMovement(
    @Column(nullable = false)
    val ingredientId: Long,

    /** Null only for OPENING movements. */
    val orderId: Long?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: MovementType,

    @Column(nullable = false)
    val quantity: BigDecimal,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
