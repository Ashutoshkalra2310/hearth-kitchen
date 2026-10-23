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
class ProductionOrder(
    @Column(nullable = false)
    val recipeId: Long,

    @Column(nullable = false)
    val batches: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.DRAFT,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    var releasedAt: Instant? = null,
    var completedAt: Instant? = null,
    var cancelledAt: Instant? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)

@Entity
class OrderRequirement(
    @Column(nullable = false)
    val orderId: Long,

    @Column(nullable = false)
    val ingredientId: Long,

    @Column(nullable = false)
    val requiredQuantity: BigDecimal,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
