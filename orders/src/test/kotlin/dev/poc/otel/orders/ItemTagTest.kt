package dev.poc.otel.orders

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ItemTagTest {

    @Test
    fun `known items are kept, anything else collapses to other`() {
        assertEquals("book", itemTag("book"))
        assertEquals("other", itemTag("random-${System.nanoTime()}"))
    }
}
