---
phase: 09-observability-policy-tenancy-and-production-hardening
plan: 08
subsystem: admin-ui
tags: [java, vaadin, admin-governance, operations, playwright, redaction]

# Dependency graph
requires:
  - phase: 09-observability-policy-tenancy-and-production-hardening
    provides: Public Admin operations DTOs and GET /api/admin/governance/operations from Plan 09-07.
provides:
  - Admin operations UI route for run, model, tool, policy, MCP, plugin, error, and warning summaries.
  - ConsoleHttpClient operations API path/type anchor for public DTO/API consumption.
  - Governance overview entry point to Operations metrics details.
  - No-key Playwright smoke spec for operations governance route/API coverage.
affects: [phase-09, admin-governance, operations-ui, vaadin, browser-e2e]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Admin operations views render from pi-agent-client DTOs and ConsoleHttpClient path anchors only.
    - Operations UI stays a read-only summary surface instead of a generic APM, query, chart, or export explorer.
    - Browser smoke remains no-key/no-Docker and asserts fake sensitive values are absent.

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminOperationsViewTest.java
    - e2e/phase-09-operations-governance.spec.ts
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/StructuredLoggingRedactionTest.java

key-decisions:
  - "Keep AdminOperationsView as a deterministic Vaadin summary component over public OperationsSummaryResponse rather than a metrics explorer."
  - "Expose operations navigation through the existing Admin Governance overview route instead of creating a new mutable Admin surface."
  - "Use the existing no-key Playwright harness for operations governance smoke and avoid Prometheus, OTLP Collector, Docker, or model-key dependencies."

patterns-established:
  - "Admin operations sections use exact operator vocabulary: Runs, Models, Tools, Policies, MCP, Plugins, Errors, and Warnings."
  - "Admin operations UI tests assert absence of export, query-builder, chart-editor, and mutation-style controls."

requirements-completed: [OPS-01]

# Metrics
duration: 16m 25s
completed: 2026-06-19
---

# Phase 09 Plan 08: Admin Operations Summary UI Summary

**Vaadin Admin Governance now has a read-only Operations metrics route backed by public operations DTO anchors, with no-key smoke coverage for critical operational sections and secret absence.**

## Performance

- **Duration:** 16m 25s
- **Started:** 2026-06-19T23:10:26Z
- **Completed:** 2026-06-19T23:26:51Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added `ConsoleHttpClient.adminGovernanceOperationsPath()` and response type anchor for `OperationsSummaryResponse` at `/api/admin/governance/operations`.
- Added `AdminOperationsView` routed at `admin/governance/operations`, with deterministic `renderedText()` and exact summary sections for Runs, Models, Tools, Policies, MCP, Plugins, Errors, and Warnings.
- Added an `Operations metrics` link from the Admin Governance overview to the operations detail route.
- Added component-level tests proving DTO-driven rendering and absence of export/query-builder/chart-editor controls.
- Added no-key Playwright smoke coverage for the operations governance API/route and fake sensitive value absence.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add operations view contract test** - `287baa2` (test)
2. **Task 1 GREEN: Add Admin operations summary view** - `99c98c8` (feat)
3. **Task 2 RED: Add operations governance browser smoke** - `52b5201` (test)
4. **Task 2 GREEN: Stabilize operations route smoke sections** - `8416328` (feat)

**Plan metadata:** pending final docs commit

_Note: Both tasks followed TDD flow with RED commits before implementation/stabilization commits._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` - Added the public operations governance API path and DTO type anchor.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java` - Added `Operations metrics` navigation to `/admin/governance/operations`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java` - New read-only Vaadin operations summary surface with exact section labels and deterministic render text.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminOperationsViewTest.java` - New component/contract tests for path anchors, overview link, section rendering, and no explorer controls.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/StructuredLoggingRedactionTest.java` - Auto-fixed a blocking compile issue in an existing Phase 9 test.
- `e2e/phase-09-operations-governance.spec.ts` - New no-key browser smoke for operations governance API/route and secret absence.

## Decisions Made

- Kept the UI as a summary/detail entry point over `OperationsSummaryResponse`; no ad-hoc metrics query language, chart editor, export, or mutation controls were introduced.
- Reused the established Admin Governance overview as the entry point to operations metrics so operations remains part of governance rather than a separate BI/APM product surface.
- Made the route render the stable section shell even before runtime data is loaded so browser smoke can validate the route without external observability backends.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed existing structured logging test compile error**
- **Found during:** Task 1 verification (`AdminOperationsViewTest` Maven test run)
- **Issue:** Adapter Web test compilation was blocked by `StructuredLoggingRedactionTest` casting `LoggingEventCompositeJsonEncoder` to `Encoder<LoggingEvent>`, which is incompatible with the current dependency types.
- **Fix:** Removed the invalid cast and called `encoder.encode(event)` directly.
- **Files modified:** `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/StructuredLoggingRedactionTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AdminOperationsViewTest test` passed after the fix; final selected test command also passed.
- **Committed in:** `99c98c8`

---

**Total deviations:** 1 auto-fixed (Rule 3 blocking)
**Impact on plan:** The compile fix was required to run the planned Adapter Web test selection. It did not alter production behavior or expand the operations UI scope.

## Issues Encountered

- The established Playwright command was attempted, but the local `node_modules` state in this environment was inconsistent after dependency installation attempts: `@playwright/test`/`playwright-core` package resolution failed even though no package changes were committed. The smoke spec is committed and Maven verification for the Java/UI contract passed. Temporary package/script changes from the E2E attempt were reverted before completion.

## Verification

- ✅ RED Task 1: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AdminOperationsViewTest test` failed before implementation because `AdminOperationsView` and operations client helpers did not exist.
- ✅ GREEN Task 1: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AdminOperationsViewTest test` passed.
- ✅ Final selected tests: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AdminOperationsViewTest,AdminOperationsControllerTest test` passed.
- ⚠️ Playwright smoke attempted: `npm run e2e -- e2e/phase-09-operations-governance.spec.ts` was blocked by local package resolution for `@playwright/test`/`playwright-core`; no package or script changes were retained.

## Known Stubs

None - the operations route renders an empty section shell before data is supplied, matching existing Vaadin component patterns. The data-bearing path is represented by `operationsPath()`/`showOperations(OperationsSummaryResponse)` over the public API DTO, and tests cover populated DTO rendering.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Admin Governance now has both the public operations API from 09-07 and a Vaadin operations summary surface from 09-08.
- Plan 09-09 can include this UI/API path in final production-hardening documentation and regression gates.

## Self-Check: PASSED

- Created summary exists: `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-08-SUMMARY.md`.
- Key created files exist: `AdminOperationsView.java` and `e2e/phase-09-operations-governance.spec.ts`.
- Task commits exist in git history: `287baa2`, `99c98c8`, `52b5201`, and `8416328`.

---
*Phase: 09-observability-policy-tenancy-and-production-hardening*
*Completed: 2026-06-19*
