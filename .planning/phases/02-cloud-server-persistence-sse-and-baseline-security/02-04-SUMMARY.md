---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 04
subsystem: app
tags: [cola, app-layer, request-context, use-cases, contract-tests, surefire]

requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Public session/run DTOs and event-history DTOs from plans 02-02 and 02-03
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Framework-free Domain contracts and COLA dependency rules
provides:
  - Framework-free App request context records for tenant/user/trace/correlation propagation
  - Session-centric App use-case interfaces for session creation/query, run creation/cancellation, and CLOUD-03 run queries
  - Contract tests that lock App boundary method signatures and context validation
affects: [adapter-web, infrastructure, persistence, sse, security]

tech-stack:
  added: []
  patterns: [plain Java records, compact-constructor validation, framework-free App interfaces, reflection-based contract tests]

key-files:
  created:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/context/SecurityPrincipalContext.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/context/CorrelationContext.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/context/RequestContext.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/SessionCommandService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/SessionQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/RunCommandService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/RunQueryService.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/AppUseCaseContractTest.java
  modified:
    - pom.xml

key-decisions:
  - "Use plain App-layer records for security and correlation context so Adapter can inject tenant/user/trace IDs without Spring or servlet dependencies in App."
  - "Keep run use-case methods session-centric by requiring sessionId alongside runId for command/query paths."
  - "Configure Surefire to allow filtered reactor builds where upstream modules have no matching tests, preserving exact per-module verification commands."

patterns-established:
  - "RequestContext parameter is mandatory on App use-case interfaces and exposes tenantId/userId/traceId/correlationId convenience accessors."
  - "App query interfaces return pi-agent-client DTOs while remaining free of Spring/JDBC/Servlet/SSE implementation types."

requirements-completed: [CLOUD-01, CLOUD-03, CLOUD-04, CLOUD-05]

duration: 3m 30s
completed: 2026-06-14
---

# Phase 02 Plan 04: Define App Request Context and Use-Case Interfaces Summary

**Framework-free COLA App seam with mandatory tenant/user/trace/correlation context and session-centric run/session use-case interfaces.**

## Performance

- **Duration:** 3m 30s
- **Started:** 2026-06-14T05:10:09Z
- **Completed:** 2026-06-14T05:13:39Z
- **Tasks:** 1
- **Files modified:** 9

## Accomplishments

- Added `SecurityPrincipalContext`, `CorrelationContext`, and `RequestContext` records with mandatory tenant, user, trace, and correlation validation.
- Added App-layer session command/query and run command/query use-case interfaces over public client DTOs.
- Preserved session-centric run signatures for create, cancel, detail, status, events, steps, messages, tool calls, and terminal result queries.
- Added contract tests proving context validation/convenience methods and expected use-case signatures.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add failing App use-case contract tests** - `7c93544` (test)
2. **Task 1 GREEN: Define App context and use-case interfaces** - `cbf6a5b` (feat)
3. **Task 1 verification fix: Allow module-specific test filters** - `06850c0` (chore)

_Note: This was a TDD task, so it has separate test and implementation commits._

## Files Created/Modified

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/context/SecurityPrincipalContext.java` - Tenant/user/authority principal context with null/blank validation.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/context/CorrelationContext.java` - Trace/correlation/optional causation context with required ID validation.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/context/RequestContext.java` - App boundary context with convenience accessors for propagated IDs.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/SessionCommandService.java` - Session creation use-case interface.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/SessionQueryService.java` - Session detail/history query interface.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/RunCommandService.java` - Run create and cancel command interface.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/RunQueryService.java` - Run detail/status/history/result query interface.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/AppUseCaseContractTest.java` - Boundary contract tests for context and use-case signatures.
- `pom.xml` - Surefire configuration allowing filtered reactor test runs across upstream modules.

## Decisions Made

- App context records remain plain Java and contain no Spring, servlet, JDBC, or SSE dependencies.
- Run APIs at the App seam carry both `sessionId` and `runId` where REST paths are nested under sessions.
- `RunQueryService` returns `EventHistoryResponse` for event replay and generic `PageResponse<Map<String,Object>>` for read-model lists until later persistence/read-model ports define concrete item DTOs.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Configured Surefire for reactor test filters**
- **Found during:** Task 1 verification
- **Issue:** The exact command `mvn -q test -pl pi-agent-app -am -Dtest=AppUseCaseContractTest,AppDependencyArchTest` failed in upstream reactor modules that had no matching test classes.
- **Fix:** Added `<failIfNoSpecifiedTests>false</failIfNoSpecifiedTests>` to the managed Surefire configuration so filtered tests can target `pi-agent-app` while `-am` still builds dependencies.
- **Files modified:** `pom.xml`
- **Verification:** Exact plan verification command passed after the fix.
- **Committed in:** `06850c0`

---

**Total deviations:** 1 auto-fixed (Rule 3 blocking)
**Impact on plan:** The fix preserves the planned verification command and avoids changing App boundary scope.

## Issues Encountered

- Initial RED verification required `-Dsurefire.failIfNoSpecifiedTests=false` because upstream modules did not contain the new test class; final verification is now supported directly by Maven configuration.

## Known Stubs

None - scanned created/modified App files for placeholder/TODO/FIXME and UI-flowing hardcoded empty values. Null checks and default empty authority set are intentional validation/defaulting logic, not stubs.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-app -am -Dtest=AppUseCaseContractTest,AppDependencyArchTest` — passed.
- Acceptance grep for forbidden App production dependencies (`org.springframework`, servlet APIs, `JdbcTemplate`, `DataSource`, `Connection`, `SseEmitter`) — no matches.

## Next Phase Readiness

- Adapter controllers can now depend on App use-case interfaces and pass `RequestContext` built from dev/JWT security and correlation filters.
- Infrastructure and App implementation plans can implement the new interfaces and use the session-centric method shapes without introducing framework dependencies into App contracts.

## Self-Check: PASSED

- Created files exist: all App context, use-case, and contract test files listed above.
- Task commits exist: `7c93544`, `cbf6a5b`, and `06850c0`.
- Verification command passed after the Surefire fix.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
