---
phase: 09-observability-policy-tenancy-and-production-hardening
plan: 10
subsystem: observability
tags: [spring-boot, micrometer, opentelemetry, run-events, admin-governance]

requires:
  - phase: 09-observability-policy-tenancy-and-production-hardening
    provides: RunEventTelemetrySink, PiTelemetry, Admin operations metrics reader, and Phase 9 verification gap report
provides:
  - Production EventSink composition that wraps PersistingEventSink with RunEventTelemetrySink when PiTelemetry is available
  - Spring wiring regression proving pi.run.events.total increments from the real EventSink bean path
  - No-key Phase 9 smoke evidence for run-event telemetry and Admin operations metrics
affects: [observability, admin-governance, production-hardening, OPS-01]

tech-stack:
  added: []
  patterns:
    - Optional telemetry decorator at Adapter Web composition root using ObjectProvider<PiTelemetry>
    - Persist-then-fanout EventSink delegate preserved under RunEventTelemetrySink

key-files:
  created:
    - .planning/phases/09-observability-policy-tenancy-and-production-hardening/09-10-SUMMARY.md
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunTelemetryWiringTest.java

key-decisions:
  - "Wrap the production PersistingEventSink with RunEventTelemetrySink only when PiTelemetry is present, preserving the existing non-telemetry EventSink behavior."
  - "Verify Admin operations run metrics through the observable pi.run.events.total meter instead of adding production delegate accessors or reflection."

patterns-established:
  - "Run-event lifecycle metrics are produced from the durable Spring EventSink path, not from ad hoc unit-test construction."
  - "Adapter Web remains the optional telemetry composition boundary; Domain/App/client/extension API remain free of telemetry implementation imports."

requirements-completed: [OPS-01]

duration: 4m 04s
completed: 2026-06-19T23:59:14Z
---

# Phase 09 Plan 10: Run Event Telemetry Gap Closure Summary

**Production RunEvent telemetry now flows through the durable Spring EventSink path, producing pi.run.events.total for Admin operations metrics while preserving no-telemetry fallback behavior.**

## Performance

- **Duration:** 4m 04s
- **Started:** 2026-06-19T23:55:10Z
- **Completed:** 2026-06-19T23:59:14Z
- **Tasks:** 2
- **Files modified:** 2 implementation/test files, plus this summary

## Accomplishments

- Wired `RunEventTelemetrySink` into `CloudRuntimeBeanConfiguration.eventSink(...)` as an optional decorator around the existing `PersistingEventSink` delegate.
- Added a Spring Boot wiring regression that fetches the real `EventSink` bean, publishes a valid `RunEvent`, and asserts `PiTelemetryNames.RUN_EVENTS_TOTAL` increments in the test `MeterRegistry`.
- Re-ran the selected no-key Phase 9 smoke gate and verified telemetry implementation imports remain isolated from Domain/App/client/extension API modules.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add failing event sink telemetry wiring test** - `fbe0575` (test)
2. **Task 1 GREEN: Wire RunEventTelemetrySink into production EventSink composition** - `7548501` (feat)
3. **Task 2: Prove run metrics flow into Admin operations and rerun Phase 9 smoke gate** - covered by `fbe0575` test commit and verified after `7548501`; no additional file delta remained to commit separately after verification.

_Note: Task 1 was TDD and therefore has separate failing-test and implementation commits._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` - Adds `ObjectProvider<PiTelemetry>` to EventSink composition and returns `new RunEventTelemetrySink(persistingEventSink, telemetry)` when telemetry exists.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunTelemetryWiringTest.java` - Adds `eventSinkIsComposedThroughRunEventTelemetryDecorator`, publishing through the Spring `EventSink` bean and asserting `pi.run.events.total` increments.
- `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-10-SUMMARY.md` - Documents execution, evidence, decisions, and self-check.

## Decisions Made

- Used `ObjectProvider<PiTelemetry>` in the Adapter Web composition root so `PersistingEventSink` remains the default behavior when telemetry is absent.
- Kept the durable `PersistingEventSink` as the single delegate instance and placed telemetry around it; no Domain/App/client contracts were changed.
- Verified run metrics through `MeterRegistry.find(PiTelemetryNames.RUN_EVENTS_TOTAL).counter()` after real Spring `EventSink.publish(...)`, avoiding production-only getters or reflection.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The TDD red test failed as expected before production wiring: the Spring `EventSink` bean was still a `PersistingEventSink` instead of `RunEventTelemetrySink`.
- The repository already had unrelated modified/untracked files before and after this plan. They were left untouched and were not included in any 09-10 commits.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=RunTelemetryWiringTest test` — PASS after implementation.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-infrastructure,pi-agent-infrastructure-observability,pi-agent-infrastructure-model-openai,pi-agent-infrastructure-mcp,pi-agent-infrastructure-plugin,pi-agent-adapter-web -am -Dtest=PlatformIdsTest,TraceIdMigrationTest,PiTelemetryArchitectureTest,RunEventTelemetrySinkTest,RunTelemetryWiringTest,MicrometerOperationsMetricsReaderTest,AdminOperationsControllerTest,AdminOperationsViewTest,Phase09TelemetryRedactionRegressionTest,Phase09CriticalPathRegressionTest test` — PASS.
- `grep -R "new RunEventTelemetrySink" -n pi-agent-adapter-web/src/main/java` — PASS, production composition found in `CloudRuntimeBeanConfiguration.java`.
- `grep -R "io\.micrometer\|io\.opentelemetry\|ch\.qos\.logback" -n pi-agent-domain/src/main pi-agent-app/src/main pi-agent-client/src/main pi-agent-extension-api/src/main; test $? -eq 1` — PASS, no protected-module telemetry implementation imports.

## Known Stubs

None found in files changed by this plan.

## User Setup Required

None - no external service configuration required. Validation used no Docker, Prometheus, OTLP Collector, model keys, or real MCP servers.

## Next Phase Readiness

- Phase 9 OPS-01 gap closure evidence is available for re-verification: production run-event telemetry wiring exists and Admin operations run metrics now have a real `pi.run.events.total` producer.
- No blockers remain for Phase 9 verification from this gap-closure plan.

## Self-Check: PASSED

- FOUND: `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java`
- FOUND: `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunTelemetryWiringTest.java`
- FOUND: `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-10-SUMMARY.md`
- FOUND commit: `fbe0575`
- FOUND commit: `7548501`

---
*Phase: 09-observability-policy-tenancy-and-production-hardening*
*Completed: 2026-06-19*
