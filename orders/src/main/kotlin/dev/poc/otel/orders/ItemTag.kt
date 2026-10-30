package dev.poc.otel.orders

private val KNOWN_ITEMS = setOf("book", "pen", "laptop", "coffee")

/**
 * Metric tags must have bounded cardinality: every distinct value creates a new time series.
 * Client-supplied values are mapped onto a fixed set before being used as a tag.
 */
fun itemTag(item: String): String = if (item in KNOWN_ITEMS) item else "other"
