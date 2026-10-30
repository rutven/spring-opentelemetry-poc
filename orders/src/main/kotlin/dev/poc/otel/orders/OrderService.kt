package dev.poc.otel.orders

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.annotation.Observed
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

class OrderNotFoundException(id: Long) : RuntimeException("Order $id not found")

@Service
class OrderService(
    private val repository: OrderRepository,
    private val pricing: PricingService,
    private val meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Annotation-based instrumentation: span "orders.create" + timer metric. */
    @Observed(name = "orders.create", contextualName = "create-order")
    fun create(item: String, quantity: Int, userId: String?): Order {
        val price = pricing.price(item, quantity)
        val order = repository.save(Order(item = item, quantity = quantity, userId = userId, price = price))
        // Custom business metric -> orders_created_total in Prometheus
        meterRegistry.counter("orders.created", "item", item).increment()
        log.info("Created order {} for item={} quantity={} price={}", order.id, item, quantity, price)
        return order
    }

    @Observed(name = "orders.get", contextualName = "get-order")
    fun get(id: Long, fail: Boolean): Order {
        if (fail) {
            log.error("Simulated failure while loading order {}", id)
            throw IllegalStateException("Simulated failure for order $id")
        }
        return repository.findByIdOrNull(id) ?: throw OrderNotFoundException(id)
    }
}
