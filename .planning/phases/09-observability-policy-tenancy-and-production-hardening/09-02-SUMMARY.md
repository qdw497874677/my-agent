---
phase: 09-observability-policy-tenancy-and-production-hardening
plan: 02
subsystem: infra
tags: [observability, micrometer, opentelemetry, mdc, archunit, cola]

requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Typed RunEvent and PlatformIds context identifiers for telemetry correlation
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: RequestContext and correlation context used by explicit telemetry scopes
  - phase: 09-observability-policy-tenancy-and-production-hardening
    provides: W3C trace-id normalization expected by telemetry context propagation
provides:
  - Isolated `pi-agent-infrastructure-observability` Maven module
  - Micrometer/OpenTelemetry telemetry facade with no-op-safe spans
  - Explicit MDC telemetry context scopes for RequestContext and RunEvent
  - Bounded low-cardinality telemetry redaction helper
  - Architecture gate preventing Micrometer, OpenTelemetry, and Logback leakage into core/public modules
affects: [phase-09-observability, run-telemetry, tool-telemetry, policy-telemetry, mcp-telemetry, plugin-telemetry]

tech-stack:
  added: [io.micrometer:micrometer-core, io.opentelemetry:opentelemetry-api, org.slf4j:slf4j-api, test-scope logback-classic]
  patterns: [isolated infrastructure observability module, explicit MDC scope restoration, low-cardinality redacted tags, ArchUnit dependency leak gate]

key-files:
  created:
    - pi-agent-infrastructure-observability/pom.xml
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetry.java
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryContext.java
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryRedactor.java
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryNames.java
    - pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryArchitectureTest.java
    - pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryContextTest.java
    - .planning/phases/09-observability-policy-tenancy-and-production-hardening/deferred-items.md
  modified:
    - pom.xml

key-decisions:
  - "Keep Micrometer, OpenTelemetry, SLF4J, and test Logback dependencies inside pi-agent-infrastructure-observability so Domain/App/client/extension/starter modules remain telemetry-implementation-free."
  - "Use an explicit AutoCloseable PiTelemetryContext that saves and restores MDC values instead of relying on thread inheritance or auto-instrumentation."
  - "Treat raw trace/correlation/tenant/user/session/run identifiers as bounded redacted low-cardinality attributes and never propagate values containing auth/token/secret markers."

patterns-established:
  - "Infrastructure observability facade wraps MeterRegistry and optional Tracer without exposing those dependencies to core contracts."
  - "Context propagation scopes write exact MDC keys and restore/remove previous values on close."
  - "Architecture leak tests import protected package roots and forbid io.micrometer.., io.opentelemetry.., and ch.qos.logback..."

requirements-completed: [OPS-01]

duration: 6m 08s
completed: 2026-06-19
---

# Phase 09 Plan 02: Create Isolated Observability Module and Telemetry Primitives Summary

**Isolated Micrometer/OpenTelemetry infrastructure module with explicit MDC context scopes, redacted low-cardinality telemetry names, and architecture leak gates.**

## Performance

- **Duration:** 6m 08s
- **Started:** 2026-06-19T22:34:49Z
- **Completed:** 2026-06-19T22:40:57Z
- **Tasks:** 2
- **Files modified:** 8 implementation/test files plus one deferred-items note

## Accomplishments

- Added `pi-agent-infrastructure-observability` to the Maven reactor and confined Micrometer/OpenTelemetry dependencies to that infrastructure module.
- Added `PiTelemetry`, `PiTelemetryNames`, `PiTelemetryRedactor`, and `PiTelemetryContext` as reusable primitives for downstream run, model, tool, policy, MCP, and plugin instrumentation.
- Added tests for MDC setup/restoration, sensitive-value redaction, no-op span safety, SimpleMeterRegistry counters, and architecture dependency leak prevention.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add isolated observability Maven module** - `af8762f` (feat)
2. **Task 2: Implement telemetry facade, explicit context scope, and redaction helpers** - `2a80972` (feat)

**Plan metadata:** pending final docs commit

_Note: Both tasks were marked TDD in the plan. Implementation used test-backed iterations and committed each task atomically as requested._

## Files Created/Modified

- `pom.xml` - Adds `pi-agent-infrastructure-observability` to the root reactor after the plugin infrastructure module.
- `pi-agent-infrastructure-observability/pom.xml` - Defines the isolated observability module with local Micrometer, OTel, SLF4J, ArchUnit, JUnit, AssertJ, and test MDC backend dependencies.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetry.java` - Provides a small facade over `MeterRegistry` and optional OTel `Tracer` with no-op-safe span behavior.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryContext.java` - Provides explicit AutoCloseable MDC context propagation for `RequestContext` and `RunEvent` and exposes sanitized attribute maps.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryRedactor.java` - Bounds tag values to 80 characters, maps blanks to `unknown`, and redacts token/secret/password/auth markers.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryNames.java` - Defines stable Phase 9 metric/span/attribute names for run, model, tool, policy, MCP, and plugin telemetry.
- `pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryArchitectureTest.java` - Verifies core/public/starter packages do not depend on Micrometer, OTel, or Logback implementation packages.
- `pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/PiTelemetryContextTest.java` - Verifies MDC lifecycle, redaction, SimpleMeterRegistry usage, and no-op span behavior.
- `.planning/phases/09-observability-policy-tenancy-and-production-hardening/deferred-items.md` - Tracks an upstream reactor verification issue outside this plan's scope.

## Decisions Made

- Keep Micrometer, OpenTelemetry, SLF4J, and test Logback dependencies inside `pi-agent-infrastructure-observability`; protected modules remain free of implementation telemetry dependencies.
- Use explicit `AutoCloseable` context scopes that save previous MDC values and restore/remove them on close to support virtual-thread and worker boundaries.
- Redact suspicious telemetry tag/span attribute values conservatively when they contain `secret`, `password`, `authorization`, `bearer`, `api_key`, `apikey`, or `token`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added a test-scope MDC backend**
- **Found during:** Task 2 (Implement telemetry facade, explicit context scope, and redaction helpers)
- **Issue:** `slf4j-api` alone provides a no-op MDC adapter in module tests, so assertions could not observe explicit MDC writes.
- **Fix:** Added test-scope `logback-classic` only inside `pi-agent-infrastructure-observability` so tests can verify MDC lifecycle without leaking Logback into protected modules.
- **Files modified:** `pi-agent-infrastructure-observability/pom.xml`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability test`
- **Committed in:** `2a80972`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Required only for deterministic in-memory MDC verification; dependency remains test-scoped and module-local.

## Issues Encountered

- The plan's `-am` verification command currently fails in upstream `pi-agent-app` tests because existing tool-gateway test fixtures still create non-W3C trace IDs after Phase 9 trace-id normalization. This is outside the Plan 09-02 observability module scope and is tracked in `deferred-items.md`.
- The local environment does not have `rg`; the protected-module import scan was run with the plan's `grep -R` command instead.

## Verification

- Passed: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability test`
- Passed: `grep -R "io\.micrometer\|io\.opentelemetry\|ch\.qos\.logback" -n pi-agent-domain/src/main pi-agent-app/src/main pi-agent-client/src/main pi-agent-extension-api/src/main pi-agent-spring-boot-starter/src/main; test $? -eq 1`
- Deferred/out-of-scope: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am test` fails before the observability module in upstream `pi-agent-app` tests due pre-existing W3C trace fixture drift.

## Known Stubs

None. Stub scan found only intentional null/no-op handling in telemetry guard code, not UI/data placeholders.

## User Setup Required

None - no external observability backend or credentials are required for this plan.

## Next Phase Readiness

- Downstream Plan 09-03 can consume `PiTelemetry`, `PiTelemetryNames`, `PiTelemetryContext`, and `PiTelemetryRedactor` for run event and dispatcher instrumentation.
- Architecture boundaries are in place to prevent future instrumentation from leaking telemetry dependencies into Domain, App, client, extension API, or starter contracts.

## Self-Check: PASSED

- Verified summary and key created files exist.
- Verified task commits `af8762f` and `2a80972` exist in git history.

---
*Phase: 09-observability-policy-tenancy-and-production-hardening*
*Completed: 2026-06-19*
