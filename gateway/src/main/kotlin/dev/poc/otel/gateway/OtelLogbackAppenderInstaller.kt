package dev.poc.otel.gateway

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component

/**
 * Logback is initialized before the Spring context, so the OTel appender has no SDK yet.
 * Once Spring Boot has built the [OpenTelemetry] bean, hand it to the appender.
 */
@Component
class OtelLogbackAppenderInstaller(private val openTelemetry: OpenTelemetry) : InitializingBean {
    override fun afterPropertiesSet() = OpenTelemetryAppender.install(openTelemetry)
}
