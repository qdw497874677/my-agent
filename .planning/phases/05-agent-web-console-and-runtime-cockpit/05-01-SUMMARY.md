---
phase: 05-agent-web-console-and-runtime-cockpit
plan: 01
subsystem: ui
tags: [vaadin, spring-security, sse, adapter-web, public-api-boundary]

requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Authenticated public REST/SSE session and run APIs consumed by the Web Console boundary.
  - phase: 05-agent-web-console-and-runtime-cockpit
    provides: Phase 5 route separation decisions D-03 and public API-only UI constraint GUI-08.
provides:
  - Vaadin Flow/Spring dependency management and adapter-web-only dependency placement.
  - Separated `/console` and `/admin/governance` Vaadin route roots.
  - Spring Security allowances for Vaadin routes/static resources while preserving `/api/**` authentication.
  - Public REST and SSE URL helper boundary for downstream Vaadin views.
affects: [05-agent-web-console-and-runtime-cockpit, web-console, admin-governance, browser-e2e]

tech-stack:
  added: [Vaadin Flow Spring Boot Starter 24.8.4]
  patterns: [Adapter-only Vaadin UI, public `/api/**` client boundary, browser-facing SSE connection metadata]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/MainConsoleLayout.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLayout.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/EventStreamClient.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleFoundationTest.java
  modified:
    - pom.xml
    - pi-agent-adapter-web/pom.xml
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/SecurityConfig.java

key-decisions:
  - "Vaadin is imported through root dependencyManagement but the production starter dependency is declared only in pi-agent-adapter-web."
  - "Web Console security permits only Vaadin route/static assets and keeps /api/** authenticated before the deny-all fallback."
  - "Vaadin-side data access is represented as relative public /api and /api stream URL helpers anchored to pi-agent-client DTOs."

patterns-established:
  - "Adapter Web owns all Vaadin classes under io.github.pi_java.agent.adapter.web.ui."
  - "Downstream Vaadin views must use ConsoleHttpClient/EventStreamClient instead of injecting App, Domain, runtime, repository, JDBC, or persistence objects."

requirements-completed: [GUI-08]

duration: 12m 20s
completed: 2026-06-15
---

# Phase 05 Plan 01: Vaadin Web Console Foundation Summary

**Adapter-only Vaadin route foundation with protected public API boundary and reusable REST/SSE URL helpers for Phase 5 views**

## Performance

- **Duration:** 12m 20s
- **Started:** 2026-06-15T04:49:19Z
- **Completed:** 2026-06-15T05:01:39Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added Vaadin BOM management and `vaadin-spring-boot-starter` only to `pi-agent-adapter-web`, keeping Domain/App free of Vaadin dependencies.
- Added separated Vaadin route roots: `/console` for the user Agent Console and `/admin/governance` for inspect-only Admin Governance.
- Updated `SecurityConfig` so Vaadin routes and static resources are reachable while `/api/**` remains authenticated and health/info stay public.
- Added `ConsoleHttpClient` and `EventStreamClient` as the reusable Vaadin-side public REST/SSE boundary for downstream Phase 5 UI plans.
- Added `WebConsoleFoundationTest` to enforce adapter-only dependencies, route security, UI import boundaries, and public API/SSE URL construction.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add failing web console foundation test** - `2ab935b` (test)
2. **Task 1 GREEN: Add Vaadin web console foundation** - `af1a8ea` (feat)
3. **Task 2 RED: Add failing web console client boundary tests** - `ca9dc9a` (test)
4. **Task 2 GREEN: Add web console public API clients** - `b42c021` (feat)

_Note: Both plan tasks were TDD tasks, so each produced a failing-test commit followed by an implementation commit._

## Files Created/Modified

- `pom.xml` - Added Vaadin version and BOM dependency management.
- `pi-agent-adapter-web/pom.xml` - Added adapter-only Vaadin Spring Boot starter dependency.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/SecurityConfig.java` - Permits Vaadin route/static assets while preserving authenticated `/api/**` and deny-all fallback.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/MainConsoleLayout.java` - User Console route root at `/console`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLayout.java` - Admin Governance route root at `/admin/governance`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` - Public REST path helper with `pi-agent-client` DTO type anchors.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/EventStreamClient.java` - Browser-facing run SSE connection metadata helper.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleFoundationTest.java` - TDD coverage for dependency placement, security routes, UI boundary imports, and helper URLs.

## Decisions Made

- Vaadin dependency management belongs in the parent POM for version consistency, but the Vaadin starter belongs only in Adapter Web so core/App/Domain remain UI-free.
- Vaadin routes/static resources are explicitly permitted before the final deny-all; `/api/**` remains authenticated and was regression-tested with dev/test auth defaults disabled.
- `ConsoleHttpClient` and `EventStreamClient` currently provide transport-neutral relative URL and DTO boundary metadata rather than injecting lower-layer services into Vaadin views.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written.

## Issues Encountered

- The first Vaadin dependency resolution attempt hit an SSL handshake failure from Maven Central. Retrying the same focused test resolved dependency download without code changes.
- Running the full adapter-web reactor test command reached pre-existing Testcontainers-backed infrastructure tests and failed because this environment has no Docker socket. This matches prior phase documentation; focused `WebConsoleFoundationTest` passed and no plan-scoped regression was found.
- Vaadin dev-mode generated `pi-agent-adapter-web/src/main/frontend/` during tests. These generated files were removed after verification and not committed.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. The two route layouts are intentionally minimal foundation route roots for downstream feature views; they do not block this plan's goal because data boundaries and route separation are the deliverables.

## Verification

- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=WebConsoleFoundationTest test`
- ⚠️ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am test` failed outside plan scope at Testcontainers Docker discovery (`/var/run/docker.sock` missing) in infrastructure tests.

## Next Phase Readiness

- Plan 05-02 can add `/api/agents` and Agent Catalog views on top of the established Vaadin route foundation.
- Plan 05-05 and later UI plans can consume `ConsoleHttpClient` and `EventStreamClient` without violating GUI-08 public API-only access.
- Admin Governance remains route-separated and ready for read-only governance APIs/views in later plans.

## Self-Check: PASSED

- Referenced created/modified files exist.
- Task commits verified: `2ab935b`, `af1a8ea`, `ca9dc9a`, `b42c021`.

---
*Phase: 05-agent-web-console-and-runtime-cockpit*
*Completed: 2026-06-15*
