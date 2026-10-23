package com.hearthkitchens.production.service

import com.hearthkitchens.production.domain.OrderStatus

class NotFoundException(message: String) : RuntimeException(message)

class InvalidOrderStatusException(
    val orderId: Long,
    val status: OrderStatus,
    action: String,
) : RuntimeException("Cannot $action order PO-$orderId: it is $status")

class InsufficientStockException(
    val orderId: Long,
    val shortages: List<Shortage>,
) : RuntimeException("Not enough stock to release order PO-$orderId")
