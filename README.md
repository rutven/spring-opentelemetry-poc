# Spring Boot 4 + OpenTelemetry PoC

A proof of concept for learning how OpenTelemetry works in a Spring Boot 4 app written in Kotlin and running on Java 25. It shows:

- **traces**, **metrics** and **logs** exported over OTLP to a Grafana LGTM stack;
- distributed context propagation across two services;
- custom instrumentation;
- correlation between the three signals.

It uses Spring Boot's **native** `spring-boot-starter-opentelemetry`. With that starter, Micrometer Observation API produces the signals, and the OpenTelemetry SDK exports them. The OTel Java agent is not used.

## Stack

| | |
|---|---|
| Runtime | Java 25, Kotlin 2.3, Spring Boot 4.1 (MVC on virtual threads) |
| Build | Gradle (Kotlin DSL), multi-module |
| Telemetry | `spring-boot-starter-opentelemetry`; the OTel Logback appender; `datasource-micrometer` for JDBC spans |
| Backend | `grafana/otel-lgtm`: OTel Collector, Tempo, Loki, Prometheus and Grafana |
| DB | Postgres 18 with Spring Data JDBC |

## Architecture

```
curl ──► gateway :8080 ──RestClient (traceparent + baggage)──► orders :8081 ──JDBC──► Postgres
             │                                                     │
             └──────────── OTLP/HTTP :4318 (traces, metrics, logs) ┴──► otel-lgtm ──► Grafana :3000
```

- **gateway** exposes the public API. It turns the `X-User-Id` header into baggage (`user.id`) and calls `orders`. It also has one `suspend` endpoint that fans out in parallel coroutines.
- **orders** handles business logic and persistence. It uses `@Observed`, a manual `Observation`, a custom counter, simulated latency and simulated failures.

## Run

```bash
docker compose up -d                 # LGTM + Postgres
./gradlew :orders:bootRun            # terminal 1
./gradlew :gateway:bootRun           # terminal 2
scripts/load.sh 100                  # generate traffic
```

Open Grafana at http://localhost:3000 (admin/admin) → **Explore**.

### Endpoints (gateway)

| Request | Shows |
|---|---|
| `POST /api/orders` `{"item":"book","quantity":2}` + `X-User-Id: alice` | A full trace through gateway → orders → pricing → DB, with baggage |
| `GET /api/orders/{id}` | A simple read |
| `GET /api/orders/{id}?fail=true` | An error span plus an ERROR log linked to the trace |
| `GET /api/orders/summary?ids=1,2,3` | Context propagation through coroutines on `Dispatchers.IO` |

## What to look at

1. **Distributed trace (Tempo).** Search for service `gateway` and open a `POST /api/orders` trace. The spans are:
   - `http post /api/orders` (gateway)
   - `http post`: the client span from `RestClient`
   - `http post /orders` (orders)
   - `create-order`: from `@Observed`
   - `calculate-price`: the manual `Observation`, with random latency
   - `connection` / `query`: JDBC

   The `user.id` attribute comes from baggage.
2. **Trace → logs.** From a span, click "Logs for this span". Loki has the log lines with matching `trace_id`/`span_id` and `user_id`.
3. **Logs → trace.** In Loki, run `{service_name="orders"} |= "Simulated failure"` and follow the trace link.
4. **Errors.** Use the TraceQL query `{status=error}` to list the failed requests from `?fail=true`.
5. **Metrics (Prometheus).**
   - `http_server_requests_milliseconds_bucket`: latency histograms. Try `histogram_quantile(0.95, sum by (le, service_name) (rate(http_server_requests_milliseconds_bucket[1m])))`.
   - `orders_pricing_milliseconds_*` and `orders_create_milliseconds_*`: timers from the observations.
   - `orders_created_total`: a custom business counter, tagged by `item`.
   - `hikaricp_connections*` and `jvm_*`: built-in metrics.
6. **Coroutines.** A `summary` trace shows N parallel `http get` child spans under one root. If you remove `withContext(PropagationContextElement())` in `GatewayController`, they become separate, disconnected traces.
7. **Console.** Each log line carries `[traceId,spanId,user=…]` (`logging.pattern.correlation`).

## Key concepts in the code

| Concept | Where |
|---|---|
| OTLP endpoints, sampling, baggage, resource attributes | `*/src/main/resources/application.yaml` |
| OTel log export (Spring Boot does not ship the appender) | `logback-spring.xml` + `OtelLogbackAppenderInstaller.kt` |
| Instrumented HTTP client (must use the injected `RestClient.Builder`) | `gateway/.../OrdersClient.kt` |
| Baggage | `gateway/.../UserBaggageFilter.kt` |
| Coroutine context propagation | `gateway/.../GatewayController.kt` |
| `@Observed` + custom counter | `orders/.../OrderService.kt` |
| Manual `Observation` (span + timer in one) | `orders/.../PricingService.kt` |
| Testing instrumentation with `TestObservationRegistry` | `*/src/test/...` |

## Notes and gotchas

- Spring Boot samples **10%** of traces by default. This PoC sets the probability to `1.0`.
- Log export to OTLP is **off** until `management.opentelemetry.logging.export.otlp.endpoint` is set. You also need the separate Logback appender and its `install(openTelemetry)` call.
- Spring Boot does not trace JDBC queries out of the box. This PoC adds `net.ttddyy.observation:datasource-micrometer-spring-boot` for that.
- The `opentelemetry-logback-appender` version must match the OTel SDK version managed by Spring Boot. See `gradle/libs.versions.toml`.
- With OTLP, metric timers are in **milliseconds** (`*_milliseconds_*`), not seconds as with the Prometheus registry.
- Keep `lowCardinalityKeyValue` for bounded values: these become metric tags. Use `highCardinalityKeyValue` for IDs: these become span attributes only.
- If logs stop appearing in Loki while traces and metrics still arrive, check the host disk. Loki inside `otel-lgtm` throttles writes once disk usage exceeds 90%. To see the errors, run the container with `ENABLE_LOGS_LOKI=true ENABLE_LOGS_OTELCOL=true`.
