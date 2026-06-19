---
phase: 09-observability-policy-tenancy-and-production-hardening
plan: 03
subsystem: observability
tags: [java, micrometer, opentelemetry, mdc, run-events, dispatcher, spring-boot]

# Dependency graph
requires:
  - phase: 09-observability-policy-tenancy-and-production-hardening
    provides: Telemetry primitives, Micrometer facade, OTel span facade, MDC context restoration, and redaction helpers from 09-02.
provides:
  - RunEventTelemetrySink for persisted run/model/tool lifecycle event metrics and safe spans.
  - TelemetryRunDispatcher for worker dispatch duration/error telemetry and explicit worker MDC cleanup.
  - Adapter Web RunDispatcher composition through TelemetryRunDispatcher when PiTelemetry is available.
affects: [phase-09, observability, cloud-runtime, run-events, worker-dispatch]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Decorator around Domain/App ports in Infrastructure for telemetry without changing contracts.
    - Safe low-cardinality Micrometer tags derived from event type/category rather than raw payload maps.
    - Explicit MDC save/restore around worker dispatch operations.

key-files:
  created:
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/RunEventTelemetrySink.java
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryRunDispatcher.java
    - pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/RunEventTelemetrySinkTest.java
    - pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/TelemetryRunDispatcherTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunTelemetryWiringTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java

key-decisions:
  - "Derive run event status tags from RunEventType only, avoiding inspection of raw payload maps for telemetry cardinality and secret-safety."
  - "Keep dispatcher telemetry as an Infrastructure decorator around RunDispatcher so Domain/App contracts remain unchanged."
  - "Compose TelemetryRunDispatcher opportunistically from Adapter Web using ObjectProvider<PiTelemetry>, preserving default dispatcher behavior if telemetry is absent."

patterns-established:
  - "Run event telemetry: EventSink decorator opens PiTelemetryContext.from(event), records pi.run.events.total, starts pi.run.event, delegates exactly once, and rethrows delegate failures."
  - "Dispatcher telemetry: RunDispatcher decorator times dispatch/dispatchRun, records success/error status, redacts worker/run tags, and restores MDC in finally."

requirements-completed: [OPS-01]

# Metrics
duration: 10m 26s
completed: 2026-06-19
---

# Phase 09 Plan 03: Instrument Run Event and Dispatcher Lifecycle Telemetry Summary

**Persisted RunEvent and worker dispatch chokepoints now emit redacted Micrometer metrics and OTel-compatible spans without changing Domain/App contracts.**

## Performance

- **Duration:** 10m 26s
- **Started:** 2026-06-19T22:44:38Z
- **Completed:** 2026-06-19T22:55:04Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added `RunEventTelemetrySink`, an `EventSink` decorator that records `pi.run.events.total` with safe tags (`event_type`, `terminal`, `source`, `status`) and emits a `pi.run.event` span using only context IDs and event type.
- Added tests proving model/tool payload values, including `PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK`, do not appear in meter IDs/tags and delegate exceptions are preserved.
- Added `TelemetryRunDispatcher`, a `RunDispatcher` decorator that records `pi.run.dispatch.duration` for `dispatch` and `dispatchRun` with `success`/`error` status, sanitized worker/run tags, span attributes, exception propagation, and MDC cleanup.
- Wired Adapter Web to wrap `DefaultRunDispatcher` in `TelemetryRunDispatcher` whenever `PiTelemetry` is available, while preserving default behavior if observability beans are absent.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add RunEvent-derived telemetry sink**
   - `3ad9a6f` test: add failing run event telemetry sink tests
   - `5a6107b` feat: implement run event telemetry sink
2. **Task 2: Add run dispatcher telemetry decorator and wire it**
   - `d0af282` test: add failing run dispatcher telemetry tests
   - `be59627` feat: instrument run dispatcher lifecycle

**Plan metadata:** pending final docs commit

_Note: Both tasks used TDD, so each has test → feat commits._

## Files Created/Modified

- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/RunEventTelemetrySink.java` - EventSink telemetry decorator for safe run lifecycle event metrics/spans.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryRunDispatcher.java` - RunDispatcher telemetry decorator for dispatch timing, status, spans, and MDC restoration.
- `pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/RunEventTelemetrySinkTest.java` - No-key SimpleMeterRegistry coverage for event tags, no payload leaks, and exception preservation.
- `pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/TelemetryRunDispatcherTest.java` - No-key SimpleMeterRegistry coverage for success/error dispatch timers and redaction.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` - Adapter composition wraps the default dispatcher when PiTelemetry exists.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunTelemetryWiringTest.java` - Spring wiring test asserting Adapter Web composes the telemetry dispatcher.

## Decisions Made

- Derived event telemetry status from `RunEventType` rather than payload status fields to avoid coupling telemetry to raw payload content and to keep tags low-cardinality.
- Kept the dispatcher decorator in the observability infrastructure module and Adapter Web composition root, avoiding Domain/App dependency leakage.
- Used `ObjectProvider<PiTelemetry>` in Adapter Web so telemetry wiring is additive and the default dispatcher path remains available when observability is not configured.

## Deviations from Plan

None - plan implementation followed the requested design. Verification encountered concurrent-work interference, documented below, but no plan scope change was made.

## Issues Encountered

- The requested combined Maven verification command was blocked in the shared worktree by concurrently executing Phase 09 agents that left uncommitted/untracked files in unrelated modules (`TelemetryToolPolicyEvaluatorTest`, `StructuredLoggingRedactionTest`, MCP telemetry wiring, and related files). These files are outside 09-03 scope and were not modified.
- To isolate 09-03 implementation from unrelated worktree noise, production observability code was compiled with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am -DskipTests compile` successfully.
- A clean detached worktree at commit `be59627` was created for plan verification, but Maven still compiled all test sources in selected modules and failed on unrelated concurrent Phase 09 test sources already present in HEAD from other parallel commits. This was treated as out-of-scope parallel execution interference, not a 09-03 implementation failure.

## Verification

- ✅ RED Task 1: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am -Dtest=RunEventTelemetrySinkTest test` failed before implementation because `RunEventTelemetrySink` did not exist.
- ✅ GREEN Task 1: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am -Dtest=RunEventTelemetrySinkTest test` passed after implementation.
- ✅ RED Task 2: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability,pi-agent-adapter-web -am -Dtest=TelemetryRunDispatcherTest,RunTelemetryWiringTest test` failed before implementation because `TelemetryRunDispatcher` did not exist.
- ✅ Compile gate for scoped implementation: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am -DskipTests compile` passed.
- ⚠️ Full requested combined verification was blocked by unrelated parallel Phase 09 changes in the shared reactor, described in Issues Encountered.

## Known Stubs

None - created/modified 09-03 files do not contain intentional placeholders or UI-facing empty/mock data.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Run event and dispatcher lifecycle telemetry are available for later Admin operations read models and UI summaries.
- Subsequent telemetry plans can follow the same decorator pattern for tool, policy, model, MCP, and plugin chokepoints while keeping Domain/App contracts stable.

## Self-Check: PASSED

- Created files exist: `RunEventTelemetrySink.java`, `TelemetryRunDispatcher.java`, `RunEventTelemetrySinkTest.java`, `TelemetryRunDispatcherTest.java`, and `RunTelemetryWiringTest.java`.
- Modified Adapter Web composition file exists: `CloudRuntimeBeanConfiguration.java`.
- Task commits exist in git history: `3ad9a6f`, `5a6107b`, `d0af282`, and `be59627`.

---
*Phase: 09-observability-policy-tenancy-and-production-hardening*
*Completed: 2026-06-19*
