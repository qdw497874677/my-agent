# Phase 9 Research: Observability, Policy, Tenancy, and Production Hardening

**Phase:** 09 — Observability, Policy, Tenancy, and Production Hardening  
**Requirement:** OPS-01  
**Date:** 2026-06-19

## Executive Summary

Phase 9 should implement OPS-01 by adding a dedicated observability infrastructure boundary, W3C-compatible trace IDs, explicit context propagation, Micrometer meters, OpenTelemetry-compatible spans, structured logs, Admin operational read models, and no-key regression gates. The safest implementation path is incremental and decorator-based: keep Domain/App/client free of Micrometer/OpenTelemetry/Logback/exporter types, add infrastructure observability adapters, then compose wrappers in Adapter Web.

This matches locked user decisions D-01 through D-22 from `09-CONTEXT.md`.

## External Findings

### Spring Boot 3.5.9

- Spring Boot Actuator exposes Prometheus via `GET /actuator/prometheus` when the Prometheus registry dependency and endpoint exposure are configured.
- Boot supports OTLP metric export with:
  ```yaml
  management:
    otlp:
      metrics:
        export:
          url: "https://otlp.example.com:4318/v1/metrics"
  ```
- Boot supports OTLP logging endpoint properties, but Logback/OpenTelemetry appenders still require explicit logging configuration.
- Actuator should remain operational infrastructure; Pi Admin should expose product-specific operational DTOs rather than raw Micrometer internals.

### Micrometer

- `SimpleMeterRegistry` provides an in-memory registry for deterministic tests without Prometheus/OTLP.
- Use low-cardinality metric names and tags. Recommended Phase 9 names:
  - `pi.run.events.total` tags: `event_type`, `status`, `source`
  - `pi.run.dispatch.duration` tags: `status`
  - `pi.model.calls.total`, `pi.model.call.duration` tags: `provider`, `status`
  - `pi.tool.executions.total`, `pi.tool.execution.duration` tags: `tool_id`, `status`, `policy_decision`
  - `pi.policy.decisions.total` tags: `decision`, `policy_ref`
  - `pi.mcp.invocations.total`, `pi.mcp.discovery.duration` tags: `server_id`, `status`
  - `pi.plugin.lifecycle.total`, `pi.plugin.discovery.duration` tags: `plugin_id`, `status`
- Avoid high-cardinality tags: never tag by raw runId, sessionId, userId, prompt, tool args, provider body, raw error text, or secret refs.

### OpenTelemetry Java

- OTel SDK supports W3C propagation with `W3CTraceContextPropagator.getInstance()`.
- W3C trace IDs are 32 lowercase hex characters and must not be all zeroes.
- Tests can use in-memory exporters/collectors to assert spans and attributes without an external collector.
- Use span attributes for low-cardinality context and sanitized identifiers only:
  - Safe: `pi.tenant_id`, `pi.session_id`, `pi.run_id`, `pi.trace_id`, `pi.correlation_id`, `pi.component`, `pi.status`, `pi.event_type`, `pi.tool_id`, `pi.provider_id`, `pi.mcp.server_id`, `pi.plugin_id`.
  - Unsafe: raw secrets, auth headers, unredacted tool args/results, prompt/provider payloads, MCP remote bodies/headers, plugin absolute paths or raw metadata.

## Recommended Architecture

### Module Boundary

Create `pi-agent-infrastructure-observability` (D-01) with production dependencies on Micrometer and OTel API/SDK/test support as needed. It should depend on App/Domain/client interfaces where necessary, not the reverse.

Allowed dependencies:
- `pi-agent-domain`
- `pi-agent-app`
- `pi-agent-client`
- Micrometer
- OpenTelemetry API/SDK/exporters where needed

Forbidden leakage:
- Domain/App/client/public extension API must not import `io.micrometer..`, `io.opentelemetry..`, `ch.qos.logback..`, or exporter types (D-05).

### Trace ID Migration

Implement W3C trace IDs in two steps:
1. Add `TraceId` validation/factory helpers in Domain with deterministic UUID-to-hex conversion.
2. Add Flyway migration to convert UUID-shaped persisted trace IDs to lowercase 32-hex while leaving `correlation_id` untouched for operator continuity (D-06, D-07, D-08).

Recommended deterministic conversion: remove UUID hyphens and lowercase. This preserves UUID entropy and produces valid W3C shape for existing values.

### Instrumentation Model

Use both event-derived telemetry and chokepoint decorators (D-02, D-04):
- Event-derived metrics from `PersistingEventSink` / `RunEventFanout` for persisted run lifecycle truth.
- Dispatcher wrapper or observer around `DefaultRunDispatcher` for run dispatch duration and failure status.
- `ToolExecutionGateway` decorator for tool execution duration/counts and policy outcome tags.
- `ToolPolicyEvaluator` decorator for policy decision metrics/spans.
- `StreamingModelClient` decorator in/around `pi-agent-infrastructure-model-openai` for model call spans/metrics.
- MCP registry/executor decorators around discovery and remote invocation.
- Plugin discovery/governance decorators around discovery, refresh, disable, quarantine, and lifecycle summary.

### Explicit Context Propagation

Do not rely on request-thread MDC inheritance or OTel auto-instrumentation (D-09). Add a small `PiTelemetryContext` / `TelemetryScope` utility that accepts `RequestContext`, `RunEvent`, or tool/model/MCP/plugin command context and explicitly writes:
- MDC keys: `traceId`, `correlationId`, `tenantId`, `userId`, `sessionId`, `runId`
- OTel span attributes: low-cardinality, redacted equivalents

### Admin Operational Metrics

Expose product-specific operational summaries, not a generic metrics explorer (D-11 through D-14):
- Add client DTOs such as `OperationsSummaryResponse`, `OperationMetricDto`, `OperationalWarningDto`.
- Add App port `OperationsMetricsReader` returning redacted summaries.
- Implement Micrometer-backed reader in infrastructure-observability.
- Add `GET /api/admin/governance/operations` and surface selected summary values inside Admin Governance overview/details.

### Structured Logging and Export Configuration

Use `logstash-logback-encoder` or Spring Boot structured logging support if available. Minimum production config:
- JSON log fields include `traceId`, `correlationId`, `tenantId`, `userId`, `sessionId`, `runId` when present.
- `/actuator/prometheus` exposed only under authenticated actuator security in production.
- OTLP endpoints are configuration-driven and disabled by default in tests.

## Validation Architecture

Nyquist validation for Phase 9 must sample every critical telemetry surface without real external backends.

### Default Test Infrastructure

- Unit/integration tests use:
  - `SimpleMeterRegistry` for Micrometer assertions.
  - In-memory span exporter/collector for OTel span assertions.
  - Log capture/appender for structured MDC/log redaction assertions.
  - Existing no-key fake runtime/model/tool/MCP/plugin fixtures.
- No default test may require Docker, Prometheus, OTLP collector, model keys, MCP network servers, or real plugins beyond existing sample plugin JAR tests.

### Required Verification Categories

1. Trace ID shape and migration tests.
2. Architecture leak tests for observability dependency boundaries.
3. Event-derived metrics tests for persisted run/model/tool lifecycle events.
4. Decorator tests for tool, policy, model, MCP, and plugin chokepoints.
5. Explicit MDC/span context propagation tests.
6. Telemetry redaction tests for fake sensitive values across spans, metrics tags, logs, Admin DTOs, events, audit, and persisted payloads.
7. Admin operations API and UI smoke tests.
8. Final critical-path regression gate covering cancellation, timeout, event ordering, policy decisions, audit, extension, MCP, plugin, and telemetry.

## Pitfalls

- Do not add Micrometer/OTel imports to Domain/App/client contracts.
- Do not tag metrics by raw run IDs, session IDs, user IDs, remote error bodies, or plugin absolute paths.
- Do not expose raw Micrometer meters directly to Vaadin/Admin DTOs.
- Do not change `correlationId` semantics while migrating `traceId`.
- Do not claim full SaaS tenancy/RBAC, KMS/vault, or sandbox implementation; document hooks and safe defaults only.
- Do not require external observability backends for default CI.
