---
phase: 09-observability-policy-tenancy-and-production-hardening
plan: 07
subsystem: observability
tags: [java, micrometer, admin-api, governance, operations, redaction]

# Dependency graph
requires:
  - phase: 09-observability-policy-tenancy-and-production-hardening
    provides: Run, model, tool, policy, MCP, and plugin telemetry meters from prior Phase 09 plans.
provides:
  - Public client operations summary DTOs for Admin Governance.
  - App-layer OperationsMetricsReader read-model port and GovernanceQueryService.operations use case.
  - Micrometer-backed operations reader mapping Pi telemetry meters into redacted summary sections.
  - Authenticated GET /api/admin/governance/operations endpoint.
affects: [phase-09, observability, admin-governance, operations-api, telemetry-redaction]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Public Admin DTOs remain plain Java records with defensive copies and no telemetry implementation imports.
    - App read-model port hides Micrometer from Governance use cases.
    - Infrastructure reader translates known Pi meters into stable operations sections.

key-files:
  created:
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/OperationsSummaryResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/OperationMetricDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/OperationalWarningDto.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/observability/OperationsMetricsReader.java
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/MicrometerOperationsMetricsReader.java
    - pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/MicrometerOperationsMetricsReaderTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminOperationsControllerTest.java
  modified:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ObservabilityBeanConfiguration.java

key-decisions:
  - "Expose Admin operations as Pi-specific public DTOs instead of a generic metrics explorer or raw Micrometer surface."
  - "Keep OperationsMetricsReader optional in DefaultGovernanceQueryService with a clock-based empty fallback for minimal/test contexts."
  - "Map non-zero error/status meters into generic operational warnings while redacting all meter tag keys and values."

patterns-established:
  - "Operations summary sections mirror critical path vocabulary: runs, models, tools, policies, MCP, plugins, errors, and warnings."
  - "MicrometerOperationsMetricsReader is the only production path that touches MeterRegistry for Admin operations read models."

requirements-completed: [OPS-01]

# Metrics
duration: 7m 48s
completed: 2026-06-19
---

# Phase 09 Plan 07: Admin Operations Metrics API Summary

**Redacted Pi-specific operations summaries now expose run, model, tool, policy, MCP, plugin, error, and warning metrics through Admin Governance public DTOs.**

## Performance

- **Duration:** 7m 48s
- **Started:** 2026-06-19T22:59:28Z
- **Completed:** 2026-06-19T23:07:16Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments

- Added `OperationMetricDto`, `OperationalWarningDto`, and `OperationsSummaryResponse` as defensive public client records with no Domain, Spring, Jakarta, Micrometer, or OpenTelemetry imports.
- Added the App-layer `OperationsMetricsReader` port and `GovernanceQueryService.operations(RequestContext)` use case with an optional empty fallback for contexts without observability wiring.
- Implemented `MicrometerOperationsMetricsReader` to map known `PiTelemetryNames` meters into stable Admin operations sections and generic warning summaries.
- Added authenticated `GET /api/admin/governance/operations` and Spring wiring for the Micrometer-backed reader.
- Added tests for App delegation/fallback, redaction of fake sensitive meter tags, absent-meter safety, and Admin API endpoint delegation.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define operations summary DTOs and App read port** - `ff3b928` (feat)
2. **Task 2: Implement Micrometer-backed operations reader and Admin REST endpoint** - `2ec4aa8` (feat)

**Plan metadata:** pending final docs commit

_Note: Both tasks followed TDD flow; RED verification was captured before implementation and GREEN/scoped verification after implementation._

## Files Created/Modified

- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/OperationMetricDto.java` - Public metric entry DTO with area/name/status/value/unit/metadata and defensive validation.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/OperationalWarningDto.java` - Public warning DTO for generic operational warnings.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/OperationsSummaryResponse.java` - Public operations summary DTO with runs, models, tools, policies, MCP, plugins, errors, warnings, and generatedAt.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/observability/OperationsMetricsReader.java` - App read-model port for operations summaries.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java` - Added `operations(RequestContext)`.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java` - Delegates operations to the reader or returns a safe empty summary.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java` - Injects optional `OperationsMetricsReader` into governance service composition.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/MicrometerOperationsMetricsReader.java` - Maps known Pi Micrometer meters into redacted operations DTOs.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java` - Adds `GET /operations` endpoint.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ObservabilityBeanConfiguration.java` - Registers operations metrics reader bean.
- Tests: `DefaultGovernanceQueryServiceTest`, `MicrometerOperationsMetricsReaderTest`, and `AdminOperationsControllerTest` cover the new API/read-model path.

## Decisions Made

- Chose explicit section lists over generic maps so Admin clients have a stable typed contract for the full critical path.
- Kept status derivation conservative: absent meters produce empty lists; error/failed/denied statuses become `DEGRADED` metrics and generic warnings.
- Used `PiTelemetryRedactor` for both meter tag keys and values before exposing metadata in public DTOs.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated existing governance service tests for new constructor dependency**
- **Found during:** Task 1 (DTO/App read port implementation)
- **Issue:** Adding the optional `OperationsMetricsReader` dependency to `DefaultGovernanceQueryService` required existing governance tests to pass the new constructor argument.
- **Fix:** Updated existing extension, MCP, and plugin governance service tests to pass `Optional.empty()`.
- **Files modified:** `DefaultGovernanceQueryServiceExtensionTest.java`, `GovernanceQueryServiceMcpTest.java`, `PluginGovernanceQueryServiceTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client,pi-agent-app -am -Dtest=DefaultGovernanceQueryServiceTest test`
- **Committed in:** `ff3b928`

---

**Total deviations:** 1 auto-fixed (Rule 3 blocking)
**Impact on plan:** Constructor-test updates were required to preserve existing governance coverage after the planned App use-case dependency was added. No scope expansion.

## Issues Encountered

- The full requested Task 2 Maven command was blocked in the shared worktree by an unrelated pre-existing/concurrent `StructuredLoggingRedactionTest` compile error in Adapter Web (`LoggingEventCompositeJsonEncoder` type mismatch). This file is outside 09-07 scope and was not modified.
- Scoped verification for 09-07 implementation passed for the infrastructure reader test, and Adapter Web main sources compiled. The Admin operations controller test source is committed but the selected Adapter Web test goal cannot complete until the unrelated structured logging test is fixed by its owning plan/wave.

## Verification

- ✅ RED Task 1: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client,pi-agent-app -am -Dtest=DefaultGovernanceQueryServiceTest test` failed before implementation because DTOs, port, and operations method did not exist.
- ✅ GREEN Task 1: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client,pi-agent-app -am -Dtest=DefaultGovernanceQueryServiceTest test` passed.
- ✅ Isolation gate: `grep -R "io\.micrometer\|io\.opentelemetry" -n pi-agent-client/src/main/java pi-agent-app/src/main/java; test $? -eq 1` passed.
- ✅ RED Task 2: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability,pi-agent-adapter-web -am -Dtest=MicrometerOperationsMetricsReaderTest,AdminOperationsControllerTest test` failed before implementation because `MicrometerOperationsMetricsReader` did not exist.
- ✅ GREEN Task 2 reader: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am -Dtest=MicrometerOperationsMetricsReaderTest test` passed.
- ✅ Adapter main compile: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -DskipTests compile` passed.
- ⚠️ Full Task 2 selected test command is blocked by unrelated `StructuredLoggingRedactionTest` compile error described above.

## Known Stubs

None - created/modified 09-07 files do not contain intentional placeholders or UI-facing empty/mock data. The empty operations fallback is intentional for minimal/test contexts where no `OperationsMetricsReader` bean is configured.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Admin Governance now has a public API read model for operations telemetry that can be consumed by Vaadin/Admin UI work without touching Micrometer internals.
- Later Phase 9 hardening/regression plans should fix or account for the unrelated structured logging test compile issue before running full Adapter Web test selections.

## Self-Check: PASSED

- Created summary exists: `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-07-SUMMARY.md`.
- Key created files exist: `OperationsSummaryResponse.java`, `OperationsMetricsReader.java`, and `MicrometerOperationsMetricsReader.java`.
- Task commits exist in git history: `ff3b928` and `2ec4aa8`.

---
*Phase: 09-observability-policy-tenancy-and-production-hardening*
*Completed: 2026-06-19*
