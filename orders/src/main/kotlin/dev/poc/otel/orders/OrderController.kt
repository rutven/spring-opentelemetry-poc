package dev.poc.otel.orders

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class CreateOrderRequest(val item: String, val quantity: Int, val userId: String?)

@RestController
@RequestMapping("/orders")
class OrderController(private val service: OrderService) {

    @PostMapping
    fun create(@RequestBody request: CreateOrderRequest): Order =
        service.create(request.item, request.quantity, request.userId)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, @RequestParam(defaultValue = "false") fail: Boolean): Order =
        service.get(id, fail)

    @ExceptionHandler
    fun notFound(e: OrderNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.message)
}
