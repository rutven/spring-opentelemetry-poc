package dev.poc.otel.gateway

import io.micrometer.tracing.Tracer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Turns the X-User-Id header into OTel baggage "user.id". Because it is listed in
 * management.tracing.baggage.remote-fields, it travels to downstream services in the
 * W3C "baggage" header; correlation.fields puts it in the MDC; tag-fields onto spans.
 * Runs after ServerHttpObservationFilter, so the server span is already current.
 */
@Component
class UserBaggageFilter(private val tracer: Tracer) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val userId = request.getHeader("X-User-Id")
        if (userId == null) {
            chain.doFilter(request, response)
            return
        }
        // Baggage ends up in logs, spans and downstream headers: only accept well-formed ids
        if (!USER_ID.matches(userId)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-Id")
            return
        }
        tracer.createBaggageInScope("user.id", userId).use { chain.doFilter(request, response) }
    }

    private companion object {
        val USER_ID = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}
