package com.hearthkitchens.production.domain

/** Unit every quantity of an ingredient is stored in. */
enum class BaseUnit { G, ML, PCS }

enum class OrderStatus { DRAFT, RELEASED, COMPLETED, CANCELLED }

/**
 * What a stock movement's (always positive) quantity did to the ingredient:
 * - OPENING:   on hand +quantity
 * - RESERVE:   reserved +quantity
 * - UNRESERVE: reserved -quantity
 * - CONSUME:   on hand -quantity and reserved -quantity
 */
enum class MovementType { OPENING, RESERVE, UNRESERVE, CONSUME }
