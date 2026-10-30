package dev.poc.otel.orders

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.math.BigDecimal
import java.time.Instant

@Table("orders")
data class Order(
    @Id val id: Long? = null,
    val item: String,
    val quantity: Int,
    val userId: String?,
    val price: BigDecimal,
    val createdAt: Instant = Instant.now(),
)

interface OrderRepository : CrudRepository<Order, Long>
