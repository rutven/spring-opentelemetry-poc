package dev.poc.otel.gateway

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.core.PropagationContextElement
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class PlaceOrderRequest(val item: String, val quantity: Int)

data class OrdersSummary(val orders: List<Order>, val total: java.math.BigDecimal)

@RestController
@RequestMapping("/api/orders")
class GatewayController(private val orders: OrdersClient) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun place(@RequestBody request: PlaceOrderRequest, @RequestHeader("X-User-Id", required = false) userId: String?): Order {
        log.info("Placing order item={} quantity={}", request.item, request.quantity)
        return orders.create(CreateOrderRequest(request.item, request.quantity, userId))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, @RequestParam(defaultValue = "false") fail: Boolean): Order {
        log.info("Fetching order {}", id)
        return orders.get(id, fail)
    }

    /**
     * Coroutine endpoint: fetches several orders in parallel on Dispatchers.IO.
     * PropagationContextElement carries the current observation (and MDC/baggage) across
     * thread hops; without it the downstream calls would start new, disconnected traces.
     */
    @GetMapping("/summary")
    suspend fun summary(@RequestParam ids: List<Long>): OrdersSummary =
        withContext(PropagationContextElement()) {
            log.info("Building summary for {}", ids)
            val fetched = coroutineScope {
                ids.map { id -> async(Dispatchers.IO) { orders.get(id, fail = false) } }.map { it.await() }
            }
            OrdersSummary(fetched, fetched.sumOf { it.price })
        }
}
