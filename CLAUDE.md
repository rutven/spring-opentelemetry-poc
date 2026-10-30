# CLAUDE.md

Learning PoC: OpenTelemetry with Spring Boot 4 (Kotlin, Java 25). See README.md for the architecture and walkthrough.

## Commands

- `docker compose up -d`: starts LGTM (Grafana at :3000, OTLP at :4317/:4318) and Postgres (:5432)
- `./gradlew :orders:bootRun` (8081) and `./gradlew :gateway:bootRun` (8080)
- `./gradlew test`: unit tests (no Docker needed)
- `scripts/load.sh [n]`: generates traffic
- Verify telemetry without the UI: use the Grafana datasource proxy, `curl -u admin:admin localhost:3000/api/datasources/proxy/uid/{tempo|loki|prometheus}/...`

## Decisions (keep them unless the user says otherwise)

- Use only Spring Boot's native `spring-boot-starter-opentelemetry` (Micrometer Observation → OTel SDK). No OTel Java agent, no community OTel Spring starter.
- Export all three signals over OTLP/HTTP to `grafana/otel-lgtm`. Sampling is 100%.
- Use Spring MVC on virtual threads. Coroutines appear only in the gateway's `summary` endpoint.
- Out of scope: Kafka/messaging. Apps run locally; only the backends run in compose.

## Conventions

- Versions live in `gradle/libs.versions.toml`. Spring-managed versions come from the Boot BOM (`platform(SpringBootPlugin.BOM_COORDINATES)`).
- `otel-instrumentation` must match the OTel SDK version in the Boot BOM (e.g. Boot 4.1.2 → SDK 1.62.0 → instrumentation 2.28.0-alpha).
- Add instrumentation through the Observation API (`@Observed` / `Observation`), not the raw OTel `Tracer`. Use the Micrometer `Tracer` only for baggage.
- HTTP clients must be built from the injected `RestClient.Builder`, or they won't be instrumented.
- Modules are deliberately independent: small duplication (for example `OtelLogbackAppenderInstaller`) is OK.
