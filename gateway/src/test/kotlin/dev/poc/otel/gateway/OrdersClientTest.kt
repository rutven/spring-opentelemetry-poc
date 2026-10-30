package dev.poc.otel.gateway

import io.micrometer.observation.tck.TestObservationRegistry
import io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException

/** Shows that a RestClient bound to an ObservationRegistry emits "http.client.requests" observations. */
class OrdersClientTest {

    private val registry = TestObservationRegistry.create()
    private val builder = RestClient.builder().observationRegistry(registry)
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client = OrdersClient(builder, "http://orders")

    @Test
    fun `get creates an http client observation with templated uri`() {
        server.expect(requestTo("http://orders/orders/42?fail=false")).andRespond(
            withSuccess(
                """{"id":42,"item":"book","quantity":1,"userId":"u1","price":13,"createdAt":"2026-01-01T00:00:00Z"}""",
                MediaType.APPLICATION_JSON,
            ),
        )

        client.get(42, fail = false)

        assertThat(registry)
            .hasObservationWithNameEqualTo("http.client.requests")
            .that()
            .hasLowCardinalityKeyValue("method", "GET")
            .hasLowCardinalityKeyValue("uri", "/orders/{id}?fail={fail}")
            .hasLowCardinalityKeyValue("status", "200")
            .hasBeenStopped()
        server.verify()
    }

    @Test
    fun `get passes a 404 from orders through`() {
        server.expect(requestTo("http://orders/orders/999?fail=false")).andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertThatThrownBy { client.get(999, fail = false) }
            .isInstanceOfSatisfying(ResponseStatusException::class.java) {
                org.assertj.core.api.Assertions.assertThat(it.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            }
        server.verify()
    }
}
