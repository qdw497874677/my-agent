---
phase: 05-agent-web-console-and-runtime-cockpit
plan: 02
subsystem: api
tags: [agent-catalog, public-api, vaadin, client-dto, spring-mvc]

# Dependency graph
requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Authenticated REST controller pattern, RequestContext construction, and Cloud Server security boundary
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Governed tool catalog metadata used by Agent Catalog run-decision fields
  - phase: 05-agent-web-console-and-runtime-cockpit
    provides: Vaadin console public API helper boundary from Plan 05-01
provides:
  - Public `AgentCatalogResponse` and `AgentCatalogItemDto` records in `pi-agent-client`
  - Framework-free App-layer `AgentCatalogQueryService` with default runnable General Agent metadata
  - Authenticated read-only `GET /api/agents` Spring MVC endpoint for Vaadin consumption
  - Console helper path and DTO anchor for `/api/agents`
affects: [agent-web-console, runtime-cockpit, vaadin-console, run-creation]

# Tech tracking
tech-stack:
  added: []
  patterns: [plain-client-records, thin-controller-delegation, app-query-service, read-only-catalog-api]

key-files:
  created:
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/agent/AgentCatalogItemDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/agent/AgentCatalogResponse.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/AgentCatalogQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultAgentCatalogQueryService.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AgentCatalogController.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/AgentCatalogBeanConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AgentCatalogControllerTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleFoundationTest.java

key-decisions:
  - "Keep Agent Catalog DTOs in pi-agent-client as plain Java records with string-based model/tool/risk metadata so UI and API clients do not import Domain or Spring types."
  - "Expose Phase 5 Agent Catalog as a read-only App query service plus thin Adapter controller; Agent Studio create/edit/publish remains out of scope."
  - "Use one default Cloud General Agent catalog entry for Phase 5, but source it through App service and public API rather than Vaadin hardcoding."

patterns-established:
  - "Agent catalog controller mirrors tool catalog controller: build RequestContext from Principal/Servlet request and delegate immediately to App service."
  - "Vaadin console helpers expose only relative /api paths and pi-agent-client DTO type anchors."

requirements-completed: [GUI-01, GUI-08]

# Metrics
duration: 8m
completed: 2026-06-15
---

# Phase 05 Plan 02: Agent Catalog API Summary

**Read-only Agent Catalog API with public client DTOs, App query service metadata, and Vaadin `/api/agents` client boundary.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-06-15T05:04:00Z
- **Completed:** 2026-06-15T05:12:00Z
- **Tasks:** 2 planned tasks completed, with TDD red/green commits
- **Files modified:** 9 plan-owned files

## Accomplishments

- Added public Agent Catalog DTO records exposing run-decision metadata: agent id/name/description, input modes, capabilities, safe provider/model reference, allowed tool ids/scopes, risk labels, side-effect labels, entry actions, timeout, and metadata.
- Added framework-free App-layer `AgentCatalogQueryService` and `DefaultAgentCatalogQueryService` with a Phase 5 default runnable Cloud General Agent.
- Added authenticated read-only `GET /api/agents` endpoint that delegates through RequestContext to the App service and returns `AgentCatalogResponse`.
- Anchored the Vaadin console helper to `/api/agents` and `AgentCatalogResponse` so future UI plans do not need hardcoded in-UI Agent definitions.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Define Agent Catalog DTOs and query service tests** - `38c195f` (test)
2. **Task 1 GREEN: Define Agent Catalog DTOs and query service** - `0914773` (feat)
3. **Task 2 RED: Expose read-only GET /api/agents tests** - `e7bef65` (test)
4. **Task 2 GREEN: Expose read-only GET /api/agents** - `b91f225` (feat)
5. **Task 2 client boundary hardening** - `13d18a1` (feat)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/agent/AgentCatalogItemDto.java` - Public catalog item contract with run-decision metadata.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/agent/AgentCatalogResponse.java` - Public catalog response wrapper.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/AgentCatalogQueryService.java` - App-layer query use case interface.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultAgentCatalogQueryService.java` - Default in-memory catalog service for the Phase 5 Cloud General Agent.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AgentCatalogController.java` - Thin read-only REST controller for `GET /api/agents`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/AgentCatalogBeanConfiguration.java` - Adapter composition bean registration.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` - Vaadin-side public API helper for `/api/agents`.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AgentCatalogControllerTest.java` - Catalog DTO/API/read-only tests.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleFoundationTest.java` - Console API helper boundary assertion for Agent Catalog.

## Decisions Made

- Kept DTOs plain and client-owned to preserve the public API boundary and avoid exposing Domain, Spring, or Jakarta types to Vaadin/API clients.
- Kept the catalog source as a default App query service rather than UI static definitions, satisfying D-04 without introducing Agent Studio scope.
- Used string sets for risk/side-effect/capability labels to allow future catalog sources to merge model/provider/tool metadata without API shape churn.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added Vaadin console helper path for `/api/agents`**
- **Found during:** Task 2 (Expose read-only GET /api/agents)
- **Issue:** The plan required Vaadin to consume the public Agent Catalog API, but only adding the REST endpoint would leave the existing console helper without an Agent Catalog path/type anchor.
- **Fix:** Added `ConsoleHttpClient.agentCatalogPath()` and `agentCatalogResponseType()`, plus a Web Console foundation assertion.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java`, `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleFoundationTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AgentCatalogControllerTest,WebConsoleFoundationTest test`
- **Committed in:** `13d18a1`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** No scope creep; this wires the API boundary required for Vaadin consumption.

## Issues Encountered

- Focused tests passed.
- Overall plan verification command `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client,pi-agent-app,pi-agent-adapter-web -am test` reached unrelated pre-existing Testcontainers gates in upstream infrastructure tests and failed because this execution environment has no Docker socket (`/var/run/docker.sock`). This matches prior phase documentation treating Docker absence as an environment gate, not a plan regression.

## Validation Results

- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AgentCatalogControllerTest test`
- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AgentCatalogControllerTest,WebConsoleFoundationTest test`
- ⚠️ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client,pi-agent-app,pi-agent-adapter-web -am test` blocked by environment-level Docker/Testcontainers availability in unrelated infrastructure tests.

## Known Stubs

None. The Phase 5 default catalog contains one intentionally supported runnable Cloud General Agent and is exposed through App service/API rather than UI placeholders.

## User Setup Required

None - no external service configuration required for focused verification. Docker is required only for pre-existing Testcontainers integration gates included by the broader reactor command.

## Next Phase Readiness

- Vaadin can fetch Agent choices from `/api/agents` instead of static in-UI definitions.
- Future Chat/Run UI plans can use catalog `agentId` values directly with `CreateRunRequest.agentId`.
- Agent Studio create/edit/publish remains explicitly deferred.

## Self-Check: PASSED

- Created/modified files exist.
- Task commits exist: `38c195f`, `0914773`, `e7bef65`, `b91f225`, `13d18a1`.

---
*Phase: 05-agent-web-console-and-runtime-cockpit*
*Completed: 2026-06-15*
