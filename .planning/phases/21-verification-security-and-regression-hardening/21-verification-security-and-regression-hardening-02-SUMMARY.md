---
phase: 21-verification-security-and-regression-hardening
plan: 02
subsystem: testing
tags: [java, junit, sqlite, jdbc, testcontainers, ownership, security, regression]

requires:
  - phase: 16-conversation-read-model-and-recent-sessions
    provides: typed conversation read-model queries and ownership-aware repository ports
  - phase: 20-provider-model-and-local-profile-stability
    provides: SQLite local profile persistence and provider metadata regression gate
provides:
  - Always-runnable local VER-02 ownership leakage matrix for tenant/user/session/run filters
  - Documented split between Docker-free SQLite local gate and Docker/Testcontainers JDBC CI gate
  - JDBC metadata test class name disambiguation so adapter-web local gate does not run infrastructure Testcontainers tests
affects: [phase-21-verification, security-hardening, conversation-read-model, local-profile, jdbc-ci]

tech-stack:
  added: []
  patterns: [SQLite local substitute for Docker-gated JDBC proof, ownership leakage matrix, CI-only Testcontainers documentation]

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ConversationOwnershipLeakageMatrixTest.java
  modified:
    - docs/phase-21-verification-hardening.md
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProviderModelMetadataPersistenceTest.java

key-decisions:
  - "Keep VER-02 local proof in adapter-web SQLite repositories so developers can run ownership leakage checks without Docker."
  - "Rename the JDBC provider metadata regression test to JdbcRunProviderModelMetadataPersistenceTest so the local RunProviderModelMetadataPersistenceTest selector does not collide with Docker/Testcontainers infrastructure tests."

patterns-established:
  - "Ownership matrix seeds allowed plus foreignTenant, foreignUser, foreignSession, and foreignRun rows, then verifies both listRecentSessions and getTranscript."
  - "Documentation pairs every local no-Docker ownership gate with its Docker/Testcontainers JDBC CI counterpart and explicit CI-ENV SKIP policy."

requirements-completed: [VER-02]

duration: 7m04s
completed: 2026-07-05
---

# Phase 21 Plan 02: Ownership Leakage Gates Summary

**SQLite local ownership matrix plus documented JDBC/Testcontainers CI gate for tenant, user, session, and run leakage prevention**

## Performance

- **Duration:** 7m04s
- **Started:** 2026-07-05T06:43:28Z
- **Completed:** 2026-07-05T06:50:32Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- Added `ConversationOwnershipLeakageMatrixTest`, an always-runnable SQLite-backed local test proving tenant/user/session/run ownership filters through `DefaultConversationQueryService`.
- Documented the VER-02 local gate and the CI-only JDBC/Testcontainers ownership proof in `docs/phase-21-verification-hardening.md`.
- Resolved a Maven test-selector collision by renaming the infrastructure Docker-backed provider metadata test to include the `Jdbc` prefix.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add local ownership leakage matrix** - `858537b` (test)
2. **Task 2: Document JDBC ownership gate and local substitute** - `0052ab3` (docs)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ConversationOwnershipLeakageMatrixTest.java` - local SQLite ownership leakage matrix with `foreignTenant`, `foreignUser`, `foreignSession`, and `foreignRun` negative rows.
- `docs/phase-21-verification-hardening.md` - VER-02 local and JDBC CI ownership gate commands and Docker/Testcontainers policy.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProviderModelMetadataPersistenceTest.java` - renamed JDBC/Testcontainers metadata test to avoid local Maven selector collision.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=ConversationOwnershipLeakageMatrixTest test` — PASS.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=ConversationOwnershipLeakageMatrixTest,RunProviderModelMetadataPersistenceTest test` — PASS.

## Decisions Made

- Keep VER-02 local proof in adapter-web SQLite repositories so developers can run ownership leakage checks without Docker.
- Rename the JDBC provider metadata regression test to `JdbcRunProviderModelMetadataPersistenceTest` so the local `RunProviderModelMetadataPersistenceTest` selector does not collide with Docker/Testcontainers infrastructure tests.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Renamed JDBC provider metadata test to avoid local selector collision**
- **Found during:** Task 2 (Document JDBC ownership gate and local substitute)
- **Issue:** The required local command `-Dtest=ConversationOwnershipLeakageMatrixTest,RunProviderModelMetadataPersistenceTest` matched both the adapter-web SQLite test and an infrastructure JDBC/Testcontainers test with the same simple class name, causing the Docker-gated test to run and fail in a no-Docker local environment.
- **Fix:** Renamed `RunProviderModelMetadataPersistenceTest` in the infrastructure JDBC package to `JdbcRunProviderModelMetadataPersistenceTest`, preserving its CI behavior while keeping the documented adapter-web local gate Docker-free.
- **Files modified:** `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProviderModelMetadataPersistenceTest.java`
- **Verification:** Final local gate passed with 5 tests and no Docker/Testcontainers startup.
- **Committed in:** `0052ab3`

---

**Total deviations:** 1 auto-fixed (Rule 3 blocking)
**Impact on plan:** The fix was necessary to make the exact required local command behave as a Docker-free local gate. No product behavior changed.

## Issues Encountered

- The first combined local gate attempt triggered the infrastructure Docker-backed `RunProviderModelMetadataPersistenceTest` because Maven matched tests by simple class name across the reactor. This was resolved by the Rule 3 rename above.

## User Setup Required

None - no external service configuration required for the local VER-02 gate. Docker/Testcontainers remains required only for the documented CI JDBC command.

## Known Stubs

None.

## Next Phase Readiness

- VER-02 now has a no-Docker local gate and a documented production JDBC CI gate.
- Subsequent Phase 21 plans can rely on `docs/phase-21-verification-hardening.md` as the release-hardening gate index.

## Self-Check: PASSED

- Found `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ConversationOwnershipLeakageMatrixTest.java`.
- Found `docs/phase-21-verification-hardening.md`.
- Found `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProviderModelMetadataPersistenceTest.java`.
- Found task commit `858537b`.
- Found task commit `0052ab3`.

---
*Phase: 21-verification-security-and-regression-hardening*
*Completed: 2026-07-05*
