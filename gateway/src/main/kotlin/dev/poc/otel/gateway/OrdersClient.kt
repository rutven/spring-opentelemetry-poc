package dev.poc.otel.gateway

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

data class CreateOrderRequest(val item: String, val quantity: Int, val userId: String?)

data class Order(
    val id: Long,
    val item: String,
    val quantity: Int,
    val userId: String?,
    val price: java.math.BigDecimal,
    val createdAt: java.time.Instant,
)

/**
 * Uses the auto-configured RestClient.Builder: it is already wired with the ObservationRegistry,
 * so every call creates a client span and injects "traceparent" + "baggage" headers.
 * A RestClient built via RestClient.create() would NOT be instrumented.
 */
@Component
class OrdersClient(builder: RestClient.Builder, @Value("\${orders.base-url}") baseUrl: String) {
    private val client = builder.baseUrl(baseUrl).build()

    fun create(request: CreateOrderRequest): Order =
        client.post().uri("/orders").body(request).retrieve().body<Order>()!!

    fun get(id: Long, fail: Boolean): Order =
        client.get().uri("/orders/{id}?fail={fail}", id, fail).retrieve().body<Order>()!!
}
