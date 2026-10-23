package com.hearthkitchens.production.web

import com.hearthkitchens.production.domain.OrderStatus
import com.hearthkitchens.production.service.IngredientDetail
import com.hearthkitchens.production.service.IngredientStock
import com.hearthkitchens.production.service.OrderDetail
import com.hearthkitchens.production.service.OrderSummary
import com.hearthkitchens.production.service.ProductionOrderService
import com.hearthkitchens.production.service.RecipeService
import com.hearthkitchens.production.service.RecipeView
import com.hearthkitchens.production.service.StockService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/ingredients")
class StockController(private val stockService: StockService) {

    @GetMapping
    fun list(): List<IngredientStock> = stockService.listStock()

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): IngredientDetail = stockService.getIngredient(id)
}

@RestController
@RequestMapping("/api/recipes")
class RecipeController(private val recipeService: RecipeService) {

    @GetMapping
    fun list(): List<RecipeView> = recipeService.listRecipes()
}

/** Fields are nullable so a missing value returns a validation message instead of a JSON parse error. */
data class CreateOrderRequest(
    @field:NotNull val recipeId: Long?,
    @field:NotNull val batches: Int?,
)

@RestController
@RequestMapping("/api/orders")
class ProductionOrderController(private val orderService: ProductionOrderService) {

    @GetMapping
    fun list(@RequestParam(required = false) status: OrderStatus?): List<OrderSummary> =
        orderService.list(status)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): OrderDetail = orderService.get(id)

    @PostMapping
    fun create(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<OrderDetail> {
        val order = orderService.create(request.recipeId!!, request.batches!!)
        return ResponseEntity.created(URI.create("/api/orders/${order.id}")).body(order)
    }

    @PostMapping("/{id}/release")
    fun release(@PathVariable id: Long): OrderDetail = orderService.release(id)

    @PostMapping("/{id}/complete")
    fun complete(@PathVariable id: Long): OrderDetail = orderService.complete(id)

    @PostMapping("/{id}/cancel")
    fun cancel(@PathVariable id: Long): OrderDetail = orderService.cancel(id)
}
