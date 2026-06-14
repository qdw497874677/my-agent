---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 13
subsystem: testing-api-docs
tags: [spring-boot, testcontainers, postgres, sse, rest, pi-testkit]
requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: REST/SSE controllers, JDBC persistence, DB queue, worker scheduler, and runtime composition root
provides:
  - No-key Cloud Server headless E2E tests over REST, SSE, PostgreSQL, and fake runtime
  - Cancellation, timeout, max-step, terminal-event, and SSE reconnect assertions
  - Phase 2 public API contract index for downstream clients
affects: [phase-03-model-provider-registry, phase-04-governed-tools, phase-05-web-console, clients]
tech-stack:
  added: [pi-testkit test dependency in adapter-web]
  patterns: [Testcontainers-backed no-key E2E, product-path worker activation, persisted SSE replay contract]
key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudServerHeadlessE2ETest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunCancellationIntegrationTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/TestCloudRuntimeConfiguration.java
    - pi-agent-adapter-web/src/test/resources/application-e2e.yml
    - docs/phase-02-cloud-server-api.md
  modified:
    - pi-agent-adapter-web/pom.xml
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunApiIntegrationTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunQueryIntegrationTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunSseIntegrationTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SecurityAndCorrelationIntegrationTest.java
key-decisions:
  - "Use TestCloudRuntimeConfiguration to provide a test-only no-key AgentRuntime built from pi-testkit GeneralAgentLoop, FakeModelClient, FakeToolInvoker, and FakePolicy."
  - "Treat Docker absence as an environment gate for Testcontainers validation; non-container regressions remain green locally."
  - "Honor AgentRuntime RunHandle terminal status in DefaultRunDispatcher so max-step/policy/runtime failure outcomes are not incorrectly marked completed."
patterns-established:
  - "Headless E2E must exercise REST create-run and RunWorkerScheduler activation instead of invoking dispatcher internals."
  - "Controller/SSE/security tests that mock App use cases should also mock JDBC/transaction/runtime beans when the composition root is active."
requirements-completed: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-05, CLOUD-06, E2E-01, E2E-04, E2E-05]
duration: 9m 06s
completed: 2026-06-14
---

# Phase 02 Plan 13: Headless Cloud Server E2E and API Contract Summary

**No-key Cloud Server E2E over REST/SSE/PostgreSQL with fake runtime, cancellation/timeout/max-step hardening, and downstream API contract documentation**

## Performance

- **Duration:** 9m 06s
- **Started:** 2026-06-14T06:11:39Z
- **Completed:** 2026-06-14T06:20:45Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments

- Added Testcontainers-backed headless Cloud Server E2E covering session creation, run creation, automatic worker activation, SSE stream/reconnect, event history, status, result, timeout, max-step, and terminal event invariants without provider keys.
- Added cancellation integration coverage for queued, running, terminal-noop, and timeout cases with exactly-one-terminal-event assertions.
- Added `TestCloudRuntimeConfiguration` test wiring around pi-testkit `GeneralAgentLoop`, `FakeModelClient`, `FakeToolInvoker`, `FakePolicy`, `DeterministicIds`, and `DeterministicClock`.
- Documented the Phase 2 downstream REST/SSE API contract and non-goals in `docs/phase-02-cloud-server-api.md`.
- Stabilized existing non-container web/security tests so they continue to verify mocked controller/SSE/security behavior after the runtime composition root became active.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add no-key Cloud Server E2E and cancellation integration tests** - `c30aaeb` (test)
2. **Task 2: Document Phase 2 API contract and mark requirement status** - `40d8ed2` (docs)
3. **Auto-fix for Task 2 verification: Stabilize web tests with composition root** - `c7ccdfe` (test)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudServerHeadlessE2ETest.java` - Headless REST/SSE/Testcontainers E2E for success, timeout, max-step, cancellation, and replay paths.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunCancellationIntegrationTest.java` - Focused cancellation/timeout terminal event integration tests.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/TestCloudRuntimeConfiguration.java` - Test-only fake runtime bean using pi-testkit components.
- `pi-agent-adapter-web/src/test/resources/application-e2e.yml` - E2E profile properties for Flyway, virtual threads, worker polling, timeout, and SSE timeout.
- `pi-agent-adapter-web/pom.xml` - Adds `pi-testkit` as a test-scoped dependency for adapter-web E2E.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` - Uses runtime `RunHandle` status to mark failed/policy/cancelled outcomes correctly.
- `docs/phase-02-cloud-server-api.md` - Public Phase 2 API contract index.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunApiIntegrationTest.java` - Mocked JDBC/transaction/runtime beans for non-container controller tests.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunQueryIntegrationTest.java` - Mocked JDBC/transaction/runtime beans for query controller tests.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunSseIntegrationTest.java` - Mocked JDBC/transaction/runtime beans for SSE tests.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SecurityAndCorrelationIntegrationTest.java` - Mocked JDBC/transaction/runtime beans for security tests.

## Decisions Made

- Use the existing pi-testkit fake runtime assets for no-key Cloud Server E2E rather than introducing provider credentials or new fake provider frameworks.
- Keep Phase 2 timeout semantics as run status `TIMED_OUT` paired with an allowed terminal `run.failed` event payload.
- Preserve product-path activation in E2E by creating runs over REST and relying on `RunWorkerScheduler.triggerAsync()`/polling, not dispatcher internals.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added adapter-web test dependency on pi-testkit**
- **Found during:** Task 1 (Add no-key Cloud Server E2E and cancellation integration tests)
- **Issue:** `TestCloudRuntimeConfiguration` imports `GeneralAgentLoop`, `FakeModelClient`, `FakeToolInvoker`, `FakePolicy`, `DeterministicIds`, and `DeterministicClock`, but adapter-web did not depend on `pi-testkit`.
- **Fix:** Added `pi-testkit` as a test-scoped dependency in `pi-agent-adapter-web/pom.xml`.
- **Verification:** Targeted E2E compilation progressed to the expected Docker/Testcontainers environment gate.
- **Committed in:** `c30aaeb`

**2. [Rule 1 - Bug] Honored runtime terminal status in dispatcher**
- **Found during:** Task 1 (max-step E2E design)
- **Issue:** `DefaultRunDispatcher` ignored `AgentRuntime.start(...)` return status and marked any non-cancelled/non-timeout completed runtime call as `COMPLETED`, which would incorrectly complete max-step failures.
- **Fix:** Capture `RunHandle` and mark `SUCCEEDED`, `FAILED`, `CANCELLED`, and `POLICY_BLOCKED` outcomes appropriately while retaining idempotent terminal event guards.
- **Verification:** Non-container dispatcher regression tests pass in the targeted regression command.
- **Committed in:** `c30aaeb`

**3. [Rule 3 - Blocking] Mocked composition-root infrastructure beans in non-container web tests**
- **Found during:** Task 2 full verification
- **Issue:** Existing controller/SSE/security tests mock App use cases but now load the active Cloud runtime composition root, which required `JdbcTemplate`, `TransactionTemplate`, and `AgentRuntime` beans even when persistence is outside those tests' scope.
- **Fix:** Added explicit `@MockBean` declarations for those infrastructure beans in the affected non-container tests.
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='CloudRuntimeWiringIntegrationTest,RunSseIntegrationTest,RunApiIntegrationTest,RunQueryIntegrationTest,SecurityAndCorrelationIntegrationTest,DefaultRunDispatcherTest,InMemoryCancellationRegistryTest,RunWorkerSchedulerTest'` passed.
- **Committed in:** `c7ccdfe`

---

**Total deviations:** 3 auto-fixed (1 bug, 2 blocking)
**Impact on plan:** All fixes were required to make the planned E2E and verification paths correct. No new product scope was added.

## Issues Encountered

- Docker is unavailable in this execution environment (`/var/run/docker.sock` missing). Testcontainers-backed gates fail locally for that reason and must run on Docker-enabled CI/local runner.
- Full `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` reaches infrastructure Testcontainers tests and fails at Docker discovery before adapter-web E2E can run in this environment.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='CloudServerHeadlessE2ETest,RunCancellationIntegrationTest'` — **blocked by environment**: Testcontainers cannot find Docker (`/var/run/docker.sock` absent).
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` — **blocked by environment**: existing infrastructure Testcontainers tests cannot find Docker.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='CloudRuntimeWiringIntegrationTest,RunSseIntegrationTest,RunApiIntegrationTest,RunQueryIntegrationTest,SecurityAndCorrelationIntegrationTest,DefaultRunDispatcherTest,InMemoryCancellationRegistryTest,RunWorkerSchedulerTest'` — **passed**.
- Acceptance greps confirmed required endpoint names, event DTO fields, non-goal phrases, required test method names, no provider-key strings, and no direct dispatcher calls in E2E tests.

## Known Stubs

None. Grep findings for `null`/placeholder text are test null-handling logic and documentation wording for tenant/user placeholders, not data-source stubs.

## User Setup Required

Docker-enabled verification environment is required for final PostgreSQL/Testcontainers E2E gates:

- Ensure a local Docker daemon or CI container runtime is available.
- Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='CloudServerHeadlessE2ETest,RunCancellationIntegrationTest'`.
- Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` as the full Phase 2 gate.

## Next Phase Readiness

- Phase 2 public REST/SSE contract is documented for Phase 3 model provider and later Web Console/Admin clients.
- No real provider, ToolExecutionGateway, UI, MCP, plugin, external broker, or crash-resumable replay behavior is implied by the Phase 2 contract.

## Self-Check: PASSED

- Created files exist: `CloudServerHeadlessE2ETest.java`, `RunCancellationIntegrationTest.java`, `TestCloudRuntimeConfiguration.java`, `application-e2e.yml`, and `docs/phase-02-cloud-server-api.md`.
- Task commits exist: `c30aaeb`, `40d8ed2`, and `c7ccdfe`.
- Verification commands and Docker environment gates are recorded above.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
