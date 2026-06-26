---
phase: 09-observability-policy-tenancy-and-production-hardening
verified: 2026-06-19T23:50:00Z
status: gaps_found
score: 3/5 must-haves verified
gaps:
  - truth: "Platform emits structured logs, metrics, and OpenTelemetry-compatible spans for run, model, tool, MCP, plugin, and policy lifecycles."
    status: partial
    reason: "Run-event telemetry exists as RunEventTelemetrySink, but it is not wired into the production EventSink/Fanout path. CloudRuntimeBeanConfiguration still returns PersistingEventSink directly, so persisted run/model/tool lifecycle events do not produce pi.run.events.total or pi.run.event spans in real Cloud Server execution."
    artifacts:
      - path: "pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/RunEventTelemetrySink.java"
        issue: "Substantive implementation exists, but only referenced by tests; no production wiring found."
      - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java"
        issue: "eventSink(...) returns new PersistingEventSink(...) without optional RunEventTelemetrySink wrapper."
    missing:
      - "Wrap the production EventSink with RunEventTelemetrySink when PiTelemetry is available, preserving default behavior when telemetry is absent."
      - "Add/adjust a wiring test proving the EventSink bean chain includes RunEventTelemetrySink."
      - "Re-run the Phase 9 selected Maven smoke gate after wiring."
  - truth: "Admin Governance surfaces runtime health and key operational metrics for runs, providers, tools, MCP servers, and plugins."
    status: partial
    reason: "Admin operations API/UI exists and reads Pi meter names, but the run section depends on pi.run.events.total; because RunEventTelemetrySink is not wired, real run events will not populate run-event metrics."
    artifacts:
      - path: "pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/MicrometerOperationsMetricsReader.java"
        issue: "Reader maps RUN_EVENTS_TOTAL for the runs section, but the producing run-event sink is not connected in production."
      - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java"
        issue: "GET /api/admin/governance/operations is wired, but its run data source is hollow until run-event telemetry is wired."
    missing:
      - "Connect run-event metric production to the production EventSink path so Admin operations runs section receives real data."
---

# Phase 9: Observability, Policy, Tenancy, and Production Hardening Verification Report

**Phase Goal:** Complete production-readiness around telemetry, security, audit, tenancy context, and operational reliability.
**Verified:** 2026-06-19T23:50:00Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

Phase 9 delivered most of the requested observability and production-hardening surface: W3C trace IDs, a dedicated observability module, telemetry decorators for tools/policy/model/MCP/plugin/dispatcher, Prometheus/OTLP/logging configuration, Admin operations API/UI, documentation, and regression tests. However, the central persisted `RunEvent` telemetry sink is implemented but not wired into the production Cloud Server event path. Because run/model/tool lifecycle events are persisted and fanned out through the `EventSink`, leaving `RunEventTelemetrySink` orphaned means the platform does not actually emit `pi.run.events.total`/`pi.run.event` telemetry for real runtime events, and the Admin operations `runs` section has a hollow data source.

## Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Platform emits structured logs, metrics, and OTel-compatible spans for run, model, tool, MCP, plugin, and policy lifecycles. | ✗ FAILED / PARTIAL | Tool/policy/model/MCP/plugin/dispatcher decorators are wired in Adapter Web config. `RunEventTelemetrySink.java` exists and is substantive, but `grep RunEventTelemetrySink` found production references only in tests; `CloudRuntimeBeanConfiguration.eventSink(...)` returns `new PersistingEventSink(...)` directly. |
| 2 | Admin Governance surfaces runtime health and key operational metrics for runs, providers/models, tools/policy, MCP, and plugins. | ✗ FAILED / PARTIAL | `GET /api/admin/governance/operations`, `MicrometerOperationsMetricsReader`, and `AdminOperationsView` exist and are wired. Data-flow trace shows the runs section reads `PiTelemetryNames.RUN_EVENTS_TOTAL`, but the producing run-event telemetry sink is not wired. |
| 3 | Trace/run/session IDs are consistently correlated across API responses, SSE/events, logs, audit records, and traces. | ✓ VERIFIED | `PlatformIds.TraceId` enforces `[0-9a-f]{32}` and rejects all-zero; `CorrelationFilter` sets request `pi.traceId`, MDC `traceId`, `correlationId`, tenant/user; `V3__w3c_trace_ids.sql` migrates legacy trace columns while preserving correlation IDs. |
| 4 | Production configuration documents cover secrets, policy extension, tenancy/RBAC hooks, sandbox strategy, retention/redaction, and deployment. | ✓ VERIFIED | `docs/phase-09-production-hardening.md` contains required sections for observability deployment, actuator security, Prometheus/OTLP, structured logs, secrets/KMS/vault hooks, policy engine extension, tenant/RBAC hooks, sandbox strategy, retention/redaction, operational safety, deployment, verification, and explicit deferrals. |
| 5 | Regression tests cover critical policy, audit, cancellation, timeout, extension, event-ordering, and telemetry redaction paths. | ✓ VERIFIED with caveat | Selected Phase 9 Maven smoke gate passed. `Phase09TelemetryRedactionRegressionTest` covers fake-secret absence across telemetry/log/operations/event/audit fixtures. `Phase09CriticalPathRegressionTest` is mostly synthetic DTO aggregation rather than executing full product paths, but earlier focused tests remain present and the requested selected gate passes. |

**Score:** 3/5 truths verified

## Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/common/PlatformIds.java` | W3C trace-id validation and migration helper | ✓ VERIFIED | Contains `[0-9a-f]{32}` validation, all-zero rejection, `TraceId.newRandom()`, and `fromLegacyUuid(...)`. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/correlation/CorrelationFilter.java` | W3C-shaped request trace IDs and preserved correlation headers | ✓ VERIFIED | Uses `TraceId.newRandom().value()`, preserves `X-Correlation-ID`, sets request attributes and MDC. |
| `pi-agent-infrastructure/src/main/resources/db/migration/V3__w3c_trace_ids.sql` | Historical trace-id migration | ✓ VERIFIED | Updates UUID-shaped `trace_id` values in `sessions` when present, `runs`, `run_events`, `audit_records`, and `run_queue`; no `correlation_id` updates. |
| `pi-agent-infrastructure-observability/pom.xml` | Isolated observability dependencies | ✓ VERIFIED | Module exists in reactor and contains Micrometer/OpenTelemetry local dependencies. |
| `PiTelemetry.java`, `PiTelemetryContext.java`, `PiTelemetryRedactor.java`, `PiTelemetryNames.java` | Telemetry facade/context/redaction/names | ✓ VERIFIED | Substantive primitives with MDC restore, safe attributes, redaction markers, and stable meter/span names. |
| `RunEventTelemetrySink.java` | Event-derived lifecycle telemetry | ⚠️ ORPHANED | Implementation records metrics/spans and delegates once, but production wiring is missing. |
| `TelemetryRunDispatcher.java` | Run dispatch timing/context decorator | ✓ VERIFIED | Wired in `CloudRuntimeBeanConfiguration` when `PiTelemetry` exists. |
| `TelemetryToolExecutionGateway.java` | Tool execution decorator | ✓ VERIFIED | Implements `ToolExecutionGateway`, records tool counters/timers/spans, wired by `ToolGovernanceBeanConfiguration`. |
| `TelemetryToolPolicyEvaluator.java` | Policy decision decorator | ✓ VERIFIED | Implements `ToolPolicyEvaluator`, wired by `ToolGovernanceBeanConfiguration`. |
| `TelemetryStreamingModelClient.java` | Model call telemetry decorator | ✓ VERIFIED | Implements `StreamingModelClient`, wired by `ModelProviderBeanConfiguration` when telemetry exists. |
| `TelemetryMcpGovernanceCatalog.java` / `TelemetryMcpToolExecutorBinding.java` | MCP discovery/invocation telemetry | ✓ VERIFIED | Governance catalog wired in Adapter Web; MCP invocation binding is wrapped in `McpToolRegistry`. |
| `TelemetryPluginGovernanceCatalog.java` | Plugin lifecycle telemetry | ✓ VERIFIED | Implements `PluginGovernanceCatalog`, wired by `PluginGovernanceBeanConfiguration`. |
| `application.yml` | Actuator, Prometheus, OTLP config | ✓ VERIFIED | Exposes `health,info,metrics,prometheus`; includes `PI_OTLP_METRICS_URL` and `PI_OTLP_LOGS_URL`. |
| `logback-spring.xml` | Structured JSON logging with MDC fields | ✓ VERIFIED | Uses redacting message/MDC providers; includes structured JSON providers. |
| `OperationsSummaryResponse.java` / `OperationsMetricsReader.java` | Public operations DTO and App port | ✓ VERIFIED | Public DTOs exist without telemetry imports; App port exists. |
| `MicrometerOperationsMetricsReader.java` | Read Pi metrics into Admin operations DTOs | ⚠️ HOLLOW for runs | Substantive, but run metrics source (`RUN_EVENTS_TOTAL`) is unwired. Other sections map real meters. |
| `AdminGovernanceController.java` | GET `/api/admin/governance/operations` | ✓ VERIFIED | Contains `@GetMapping("/operations")` delegating to `governanceQueryService.operations(...)`. |
| `AdminOperationsView.java` | Operations summary UI | ✓ VERIFIED | Route exists, renders expected sections, uses `ConsoleHttpClient` operations path. |
| `docs/phase-09-production-hardening.md` | Production hardening guide | ✓ VERIFIED | Required topics and explicit deferrals present. |
| `Phase09TelemetryRedactionRegressionTest.java` | Telemetry redaction regression | ✓ VERIFIED | Present and included in selected Maven gate. |
| `Phase09CriticalPathRegressionTest.java` | Critical path regression | ⚠️ PARTIAL | Present and selected gate passes, but test is mostly constructed DTO/audit/status fixtures, not actual end-to-end execution. |

## Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `CorrelationFilter` | RequestContext/RunEvent trace IDs | request attributes/MDC | ✓ WIRED | `TRACE_ATTRIBUTE`, `CORRELATION_ATTRIBUTE`, `MDC.put("traceId")`, and `TraceId.newRandom()` present. |
| `V3__w3c_trace_ids.sql` | run/session/event/audit trace columns | `lower(replace(trace_id, '-', ''))` | ✓ WIRED | Migration includes guarded `sessions` and explicit `runs`, `run_events`, `audit_records`, `run_queue`. |
| `pi-agent-infrastructure-observability` | Micrometer/OpenTelemetry | module-local dependencies | ✓ WIRED | Observability module contains Micrometer/OTel types; protected module grep found no production imports in Domain/App/client/extension API. |
| `PiTelemetryContext` | MDC/span attributes | explicit scope helper | ✓ WIRED | Provides `from(RequestContext)`, `from(RunEvent)`, MDC writes/restores, and safe span attributes. |
| `PersistingEventSink / RunEventFanout` | `RunEventTelemetrySink` | composite event/fanout wrapper | ✗ NOT_WIRED | Production `EventSink` bean returns `PersistingEventSink` directly; no production `RunEventTelemetrySink` reference found. |
| `DefaultRunDispatcher` | `PiTelemetryContext` / dispatch telemetry | `TelemetryRunDispatcher` decorator | ✓ WIRED | `CloudRuntimeBeanConfiguration` wraps default dispatcher with `TelemetryRunDispatcher` when `PiTelemetry` exists. |
| `ToolGovernanceBeanConfiguration` | Tool gateway/policy telemetry | telemetry wrappers | ✓ WIRED | Contains `TelemetryToolExecutionGateway` and `TelemetryToolPolicyEvaluator` wrappers. |
| `ModelProviderBeanConfiguration` | Streaming model telemetry | `TelemetryStreamingModelClient` | ✓ WIRED | OpenAI-compatible client wrapped when telemetry exists. |
| `McpGovernanceBeanConfiguration` / `McpToolRegistry` | MCP discovery/invocation telemetry | catalog + binding wrappers | ✓ WIRED | Catalog is wrapped; `McpToolRegistry` wraps executor bindings with `TelemetryMcpToolExecutorBinding`. |
| `PluginGovernanceBeanConfiguration` | plugin lifecycle telemetry | `TelemetryPluginGovernanceCatalog` | ✓ WIRED | Catalog wrapped when telemetry exists. |
| `AdminGovernanceController` | `GovernanceQueryService.operations` | public REST DTO boundary | ✓ WIRED | `/operations` endpoint returns `OperationsSummaryResponse`. |
| `AdminOperationsView` | GET `/api/admin/governance/operations` | `ConsoleHttpClient.adminGovernanceOperationsPath()` | ✓ WIRED | View exposes `operationsPath()` and renders `OperationsSummaryResponse`. |

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|---|---|---|---|---|
| `MicrometerOperationsMetricsReader` | `runs` | `meterRegistry.find(PiTelemetryNames.RUN_EVENTS_TOTAL).meters()` | No, for production run events until `RunEventTelemetrySink` is wired | ⚠️ HOLLOW |
| `MicrometerOperationsMetricsReader` | `models/tools/policies/mcp/plugins` | MeterRegistry meters produced by wired telemetry decorators | Yes, decorators are wired into production config | ✓ FLOWING |
| `AdminGovernanceController.operations` | `OperationsSummaryResponse` | `governanceQueryService.operations(RequestContext)` | Partially; delegates to operations reader, but runs section affected by hollow source | ⚠️ PARTIAL |
| `AdminOperationsView` | `OperationsSummaryResponse operations` | `showOperations(OperationsSummaryResponse)` and `ConsoleHttpClient.adminGovernanceOperationsPath()` | Yes for supplied DTO; actual route/client transport is a public API anchor rather than private DB access | ✓ FLOWING |
| `RunEventTelemetrySink` | event metric/span | Intended production `EventSink` chain | No production source found | ✗ DISCONNECTED |

## Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Phase 9 selected no-key smoke tests pass | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-infrastructure,pi-agent-infrastructure-observability,pi-agent-infrastructure-model-openai,pi-agent-infrastructure-mcp,pi-agent-infrastructure-plugin,pi-agent-adapter-web -am -Dtest=PlatformIdsTest,TraceIdMigrationTest,PiTelemetryArchitectureTest,RunEventTelemetrySinkTest,TelemetryToolExecutionGatewayTest,TelemetryToolPolicyEvaluatorTest,TelemetryStreamingModelClientTest,TelemetryMcpTelemetryTest,TelemetryPluginGovernanceCatalogTest,ObservabilityConfigurationTest,ActuatorSecurityTest,StructuredLoggingRedactionTest,MicrometerOperationsMetricsReaderTest,AdminOperationsControllerTest,AdminOperationsViewTest,Phase09TelemetryRedactionRegressionTest,Phase09CriticalPathRegressionTest test` | Maven command completed without reported failure. | ✓ PASS |
| Protected modules do not import Micrometer/OTel/Logback implementation types | Grep over `pi-agent-domain/src/main`, `pi-agent-app/src/main`, `pi-agent-client/src/main`, `pi-agent-extension-api/src/main` for `io.micrometer|io.opentelemetry|ch.qos.logback` | No matches. | ✓ PASS |
| RunEventTelemetrySink production wiring exists | Grep for `RunEventTelemetrySink` across repo | Matches only implementation and tests; no production config usage. | ✗ FAIL |
| Anti-pattern scan on observability production code and Admin operations UI | Grep for TODO/FIXME/PLACEHOLDER/coming soon/not implemented/empty returns in `pi-agent-infrastructure-observability/src/main/java` and Admin UI package | No matches. | ✓ PASS |

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| OPS-01 | 09-01 through 09-09 | Platform emits structured logs, metrics, and OpenTelemetry-compatible trace/span hooks for runs, model calls, tool calls, MCP calls, plugin lifecycle, and policy decisions. | ✗ PARTIAL / BLOCKED | Model/tool/policy/MCP/plugin/dispatcher telemetry and Admin operations/docs/tests exist. Run-event telemetry hook is orphaned from production EventSink, so run lifecycle event telemetry is not emitted in real execution. |

**Orphaned requirements:** None found. ROADMAP Phase 9 maps only `OPS-01`; all Phase 9 PLAN frontmatter declares `requirements: [OPS-01]`.

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---:|---|---|---|
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` | 104-110 | Missing telemetry wrapper around production EventSink | 🛑 Blocker | Prevents real run-event metrics/spans and leaves Admin operations run metrics hollow. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/Phase09CriticalPathRegressionTest.java` | 23-55 | Synthetic DTO aggregation rather than exercising product paths | ⚠️ Warning | Useful smoke, but weaker than the claimed critical-path regression; existing focused tests should remain part of release gates. |

## Human Verification Required

Automated checks found blocking wiring gaps, so human verification is not sufficient yet. After the gap is fixed, a human/operator should still verify:

1. **Admin Operations UI route**
   - **Test:** Start the Cloud Server with test/dev auth, create a run, visit `/admin/governance/operations`.
   - **Expected:** Runs, Models, Tools, Policies, MCP, Plugins sections render and runs metrics are non-empty after run events are produced.
   - **Why human:** Browser-level behavior and Vaadin rendering were not run end-to-end in this verification session.

2. **Prometheus scrape visibility**
   - **Test:** Authenticated scrape of `/actuator/prometheus` after creating a run and invoking a governed tool.
   - **Expected:** `pi_run_events_total`, tool/policy/model/MCP/plugin meters appear as applicable and contain no raw payload/secret data.
   - **Why human:** Requires a running service and realistic operational scrape context.

## Gaps Summary

The phase is close, but not fully achieved. The root blocker is that `RunEventTelemetrySink` is an orphaned production artifact: it exists, is tested, and is part of the Phase 9 evidence, but it is not connected to the production `EventSink` chain. This violates the core OPS-01 promise for run lifecycle telemetry and makes Admin operations run metrics hollow. Wire the sink in `CloudRuntimeBeanConfiguration` when `PiTelemetry` is available, add a wiring test, and rerun the selected Phase 9 smoke gate.

---

_Verified: 2026-06-19T23:50:00Z_
_Verifier: the agent (gsd-verifier)_
