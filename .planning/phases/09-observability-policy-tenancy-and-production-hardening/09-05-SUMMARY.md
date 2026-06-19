---
phase: 09-observability-policy-tenancy-and-production-hardening
plan: 05
subsystem: observability
tags: [micrometer, opentelemetry, model-provider, mcp, plugins, redaction]
requires:
  - phase: 09-observability-policy-tenancy-and-production-hardening
    provides: Phase 09 Plan 02 telemetry primitives, redaction helpers, metric names, MDC/span facade
  - phase: 07-mcp-client-bridge-and-governed-remote-tools
    provides: MCP governance catalog and remote tool executor binding seams
  - phase: 08-controlled-dynamic-plugin-jars
    provides: Plugin governance catalog refresh/disable/quarantine lifecycle seams
provides:
  - Redacted model-provider telemetry decorator for StreamingModelClient
  - Redacted MCP discovery/refresh and tool invocation telemetry decorators
  - Redacted plugin lifecycle telemetry decorator for plugins/refresh/disable/quarantine
  - Adapter Web wiring for telemetry-aware model, MCP, and plugin governance composition
affects: [phase-09, observability, provider-runtime, mcp-governance, plugin-governance]
tech-stack:
  added: []
  patterns:
    - Infrastructure telemetry decorators wrap App/Domain seams without changing core interfaces
    - Metrics/spans use low-cardinality sanitized tags only and never raw request bodies, headers, provider errors, or paths
key-files:
  created:
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryStreamingModelClient.java
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryMcpGovernanceCatalog.java
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryMcpToolExecutorBinding.java
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryPluginGovernanceCatalog.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/McpGovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistry.java
key-decisions:
  - "Model, MCP, and plugin observability is implemented as outer Infrastructure/Adapter decorators so Domain/App contracts remain telemetry-implementation-free."
  - "Telemetry tags are limited to stable low-cardinality identifiers and statuses; raw prompts, provider bodies, MCP headers, credential refs, plugin paths, and raw exception messages are intentionally excluded."
patterns-established:
  - "Telemetry decorators accept PiTelemetry optionally from Adapter Web composition and fall back to existing behavior when no PiTelemetry bean exists."
  - "Secret regression tests include PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK and assert the marker is absent from meter tags."
requirements-completed: [OPS-01]
duration: 10m 57s
completed: 2026-06-19
---

# Phase 09 Plan 05: Model/MCP/Plugin Telemetry Chokepoints Summary

**Redacted Micrometer/OpenTelemetry decorators for model calls, MCP discovery/invocation, and plugin lifecycle governance surfaces**

## Performance

- **Duration:** 10m 57s
- **Started:** 2026-06-19T22:44:39Z
- **Completed:** 2026-06-19T22:55:36Z
- **Tasks:** 3
- **Files modified:** 13

## Accomplishments

- Added `TelemetryStreamingModelClient` to emit `pi.model.calls.total`, `pi.model.call.duration`, and `pi.model.call` spans with sanitized `provider`, `model`, and `status` attributes.
- Added MCP telemetry for governance discovery/refresh (`pi.mcp.discovery.duration`) and remote invocation (`pi.mcp.invocations.total`) without tagging remote URLs, headers, credential refs, arguments, or error bodies.
- Added `TelemetryPluginGovernanceCatalog` to cover `plugins()`, `refresh()`, `disable()`, and `quarantine()` through `pi.plugin.lifecycle.total`, `pi.plugin.discovery.duration`, and `pi.plugin.lifecycle` spans.
- Wired Adapter Web composition so model provider, MCP governance, MCP tool registry, and plugin governance are telemetry-decorated when a `PiTelemetry` bean is present.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add model-provider telemetry decorator and wiring** - `79972dc` (feat)
2. **Task 2: Add MCP telemetry decorators and wiring** - `84803ca` (feat)
3. **Task 2 follow-up fix: Correct MCP telemetry transport tag accessor** - `ff6e8ad` (fix)
4. **Task 3: Add plugin lifecycle telemetry decorator and wiring** - `ca6354d` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryStreamingModelClient.java` - Streaming model client decorator for redacted model call metrics/spans.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryMcpGovernanceCatalog.java` - MCP governance discovery/refresh telemetry decorator.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryMcpToolExecutorBinding.java` - MCP tool invocation telemetry decorator.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryPluginGovernanceCatalog.java` - Plugin lifecycle telemetry decorator.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetry.java` - Exposes controlled timer stopping through the underlying registry support used by decorators.
- `pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/TelemetryStreamingModelClientTest.java` - Model telemetry and redaction regression coverage.
- `pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/TelemetryMcpTelemetryTest.java` - MCP discovery/invocation telemetry and redaction regression coverage.
- `pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/TelemetryPluginGovernanceCatalogTest.java` - Plugin lifecycle telemetry and path redaction regression coverage.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java` - Optional telemetry wrapping for OpenAI-compatible `StreamingModelClient`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/McpGovernanceBeanConfiguration.java` - Optional telemetry wrapping for MCP catalog and tool registry.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java` - Optional telemetry wrapping for plugin governance catalog.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ProviderMcpPluginTelemetryWiringTest.java` - Adapter wiring coverage proving telemetry wrappers are selected when `PiTelemetry` exists.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistry.java` - MCP registry binding factory now decorates executor bindings with invocation telemetry when configured.
- `pi-agent-infrastructure-mcp/pom.xml` - Adds observability infrastructure dependency for MCP invocation decorator access.
- `pi-agent-adapter-web/pom.xml` - Uses the observability infrastructure dependency introduced by concurrent Phase 09 observability configuration work.

## Decisions Made

- Model, MCP, and plugin observability is implemented as outer Infrastructure/Adapter decorators so Domain/App contracts remain telemetry-implementation-free.
- Telemetry tags are limited to stable low-cardinality identifiers and statuses; raw prompts, provider bodies, MCP headers, credential refs, plugin paths, and raw exception messages are intentionally excluded.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Corrected MCP transport accessor in telemetry registry binding**
- **Found during:** Task 3 verification after MCP telemetry wiring
- **Issue:** `McpServerProperties` exposes `transport()` rather than `transportKind()`, causing compilation failure once the MCP module was pulled into the plugin/adapter verification reactor.
- **Fix:** Updated `McpToolRegistry` telemetry binding factory to use `server.transport().name()`.
- **Files modified:** `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistry.java`
- **Verification:** Re-ran the plugin verification command; this compile error was resolved before the reactor reached an unrelated pre-existing `StructuredLoggingRedactionTest` failure.
- **Committed in:** `ff6e8ad`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** The fix was required for correctness of Task 2 MCP telemetry wiring and did not expand scope.

## Issues Encountered

- Task verification commands compile additional tests in the selected Maven modules. Two unrelated pre-existing/concurrent failures remain outside this plan's scope:
  - `TelemetryToolPolicyEvaluatorTest` references `ToolSideEffect.NONE`, which is not present in the current enum.
  - `StructuredLoggingRedactionTest` has a Logback encoder type mismatch in Adapter Web.
- These failures prevented full reactor green verification for commands that include all tests in affected modules, but the new plan-specific tests were added with focused assertions and the blocking MCP compile issue introduced by this plan was fixed.

## Known Stubs

None - no placeholders or unwired mock data were introduced by this plan. Empty strings used by existing App DTOs remain their established safe optional-field representation.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Provider, MCP, and plugin extension surfaces now emit redacted observability data through infrastructure/adapter wrappers only.
- Future Phase 09 policy/tenancy hardening can reuse the same `PiTelemetry` + `PiTelemetryRedactor` decorator pattern.
- Before whole-phase verification, the unrelated `TelemetryToolPolicyEvaluatorTest` and `StructuredLoggingRedactionTest` compile failures should be reconciled by their owning parallel plans.

## Self-Check: PASSED

- Found summary file at `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-05-SUMMARY.md`.
- Found key created decorators: `TelemetryStreamingModelClient.java`, `TelemetryMcpGovernanceCatalog.java`, and `TelemetryPluginGovernanceCatalog.java`.
- Found task commits in git history: `79972dc`, `84803ca`, `ff6e8ad`, and `ca6354d`.

---
*Phase: 09-observability-policy-tenancy-and-production-hardening*
*Completed: 2026-06-19*
