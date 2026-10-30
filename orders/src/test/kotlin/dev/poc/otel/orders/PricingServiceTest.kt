package dev.poc.otel.orders

import io.micrometer.observation.tck.TestObservationRegistry
import io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat
import org.junit.jupiter.api.Test

/** TestObservationRegistry records observations in memory: no exporter, no Spring context. */
class PricingServiceTest {

    private val registry = TestObservationRegistry.create()
    private val service = PricingService(registry)

    @Test
    fun `price is calculated inside an orders_pricing observation`() {
        service.price("book", 2)

        assertThat(registry)
            .hasObservationWithNameEqualTo("orders.pricing")
            .that()
            .hasContextualNameEqualTo("calculate-price")
            .hasLowCardinalityKeyValue("item", "book")
            .hasHighCardinalityKeyValue("quantity", "2")
            .hasBeenStarted()
            .hasBeenStopped()
    }
}
