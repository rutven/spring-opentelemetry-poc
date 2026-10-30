package dev.poc.otel.gateway

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.core.PropagationContextElement
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

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
    suspend fun summary(@RequestParam ids: List<Long>): OrdersSummary {
        val uniqueIds = ids.distinct()
        if (uniqueIds.isEmpty() || uniqueIds.size > MAX_SUMMARY_IDS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ids must contain 1..$MAX_SUMMARY_IDS distinct values")
        }
        return withContext(PropagationContextElement()) {
            log.info("Building summary for {}", uniqueIds)
            val fetched = coroutineScope {
                uniqueIds.map { id -> async(summaryDispatcher) { orders.get(id, fail = false) } }.map { it.await() }
            }
            OrdersSummary(fetched, fetched.sumOf { it.price })
        }
    }

    private companion object {
        const val MAX_SUMMARY_IDS = 20

        /** Caps concurrent downstream calls per gateway instance. */
        val summaryDispatcher = Dispatchers.IO.limitedParallelism(5)
    }
}
