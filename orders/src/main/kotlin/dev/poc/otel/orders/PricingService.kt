package dev.poc.otel.orders

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.springframework.stereotype.Service
import java.math.BigDecimal
import kotlin.random.Random

/**
 * Manual instrumentation: one Observation produces both a span ("orders.pricing")
 * and a timer metric ("orders.pricing" -> orders_pricing_milliseconds_* in Prometheus).
 */
@Service
class PricingService(private val observationRegistry: ObservationRegistry) {

    fun price(item: String, quantity: Int): BigDecimal =
        Observation.createNotStarted("orders.pricing", observationRegistry)
            .contextualName("calculate-price")
            .lowCardinalityKeyValue("item", item) // span attribute + metric tag (keep cardinality low!)
            .highCardinalityKeyValue("quantity", quantity.toString()) // span attribute only
            .observe<BigDecimal> {
                Thread.sleep(Random.nextLong(10, 200)) // simulated latency
                BigDecimal(item.length * 3 + 1).multiply(BigDecimal(quantity))
            }
}
