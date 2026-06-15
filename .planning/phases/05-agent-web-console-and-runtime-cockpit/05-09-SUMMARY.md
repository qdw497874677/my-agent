---
phase: 05-agent-web-console-and-runtime-cockpit
plan: 09
subsystem: testing
tags: [playwright, vaadin, spring-boot, browser-e2e, fake-runtime, traceability]

# Dependency graph
requires:
  - phase: 05-agent-web-console-and-runtime-cockpit
    provides: Agent Catalog, Console, approval, tool-card, and Admin Governance UI/API surfaces from Plans 05-05 through 05-08
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: governed tool lifecycle, approval-required, audit, and redacted tool events consumed by Web Console E2E
provides:
  - Deterministic Playwright browser E2E harness for Phase 5 Web Console/Admin happy path and key branches
  - No-key Spring Boot test fixture for browser tests using fake runtime data and in-memory infrastructure
  - Phase 5 Web Console/Admin contract index and requirement traceability updates
affects: [phase-06-extension-surface, phase-07-mcp, phase-08-plugins, phase-09-hardening, web-console-e2e]

# Tech tracking
tech-stack:
  added: [Playwright, Node test scripts, Spring Boot e2e test profile fixture]
  patterns: [public-API browser E2E, no-key fake runtime webServer, failure-only screenshots/traces, read-only governance contract documentation]

key-files:
  created:
    - e2e/phase-05-web-console.spec.ts
    - e2e/fixtures/fake-runtime.ts
    - playwright.config.ts
    - scripts/e2e-install.sh
    - scripts/e2e-web-server.sh
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java
    - docs/phase-05-web-console.md
  modified:
    - package.json
    - package-lock.json
    - .gitignore
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/InMemoryCloudE2EConfiguration.java
    - .planning/REQUIREMENTS.md

key-decisions:
  - "Use Playwright as a test-only browser E2E harness while keeping production Web Console/Admin implementation Java-first."
  - "Run browser E2E through Spring Boot test/e2e profiles with fake runtime fixtures, in-memory state, no Docker, and no model keys."
  - "Document Phase 5 public REST/SSE/read-model contracts as the boundary for future extension, MCP, plugin, and governance work."

patterns-established:
  - "Playwright webServer starts the Vaadin/Spring Boot app from the test classpath and waits on a deterministic readiness endpoint."
  - "Browser assertions use public REST/API behavior and redacted event payloads rather than private runtime/database access."
  - "Admin extension/MCP/plugin views remain read-only placeholder status metadata until Phases 6/7/8."

requirements-completed: [E2E-07, GUI-01, GUI-02, GUI-03, GUI-04, GUI-05, GUI-06, GUI-07, GUI-08]

# Metrics
duration: 2m 43s retry verification; implementation commits pre-existed from this plan retry window
completed: 2026-06-15
---

# Phase 05 Plan 09: Playwright Browser E2E and Phase 5 Contract Summary

**Deterministic no-key Playwright browser E2E plus Phase 5 Web Console/Admin contract and requirement traceability documentation.**

## Performance

- **Duration:** 2m 43s for this retry verification and summary pass
- **Started:** 2026-06-15T11:10:55Z
- **Completed:** 2026-06-15T11:13:38Z
- **Tasks:** 3
- **Files modified:** 11 plan-related files plus this summary

## Accomplishments

- Added a Playwright harness with `npm run e2e:install`, `npm run e2e`, failure-only screenshots, failure trace retention, and a Spring Boot webServer targeting the Vaadin app.
- Added deterministic no-key browser fixtures and a Spring Boot `test,e2e` fixture configuration that exposes catalog, streaming output, governed tool events, approval-required paths, session continuation, cancellation, and Admin Governance data without real model keys, Docker, or external services.
- Implemented four Phase 5 browser scenarios covering Agent Catalog/chat shell, run streaming/event history, tool lifecycle cards, approval approve/reject branches, session continuation, cancellation, and inspect-only Admin Governance APIs/views.
- Documented Phase 5 downstream public contracts in `docs/phase-05-web-console.md` and updated `GUI-01` through `GUI-08` plus `E2E-07` requirement traceability without marking later EXT/MCP/PLUG/OPS-01 requirements complete.

## Task Commits

Each task was committed atomically during this plan's implementation history:

1. **Task 1: Add Playwright harness and no-key browser fixtures** - `628bbd9` (feat)
   - Supporting auto-fix commits: `7fb7a64`, `3d260e0`, `30d3ecf`, `5276342`, `7e69285`
2. **Task 2: Implement Phase 5 browser E2E scenarios** - `0882678` (feat)
3. **Task 3: Document Phase 5 contracts and update requirement statuses** - `029a063` (docs)

**Plan metadata:** captured in final docs commit for this summary/state update.

## Files Created/Modified

- `package.json` / `package-lock.json` - Declares test-only Playwright and Vaadin frontend dependencies plus E2E scripts.
- `playwright.config.ts` - Configures Playwright base URL, Chromium project, failure artifacts, and Spring Boot webServer startup.
- `scripts/e2e-install.sh` - Normalizes the plan's `--with-deps=false` install command for local/CI browser setup.
- `scripts/e2e-web-server.sh` - Builds and launches the Spring Boot Vaadin app under Java 21 with `test,e2e` profiles and deterministic readiness.
- `e2e/fixtures/fake-runtime.ts` - Provides typed Playwright helpers for sessions, runs, events, approvals, cancellation, dev headers, and screenshots.
- `e2e/phase-05-web-console.spec.ts` - Implements behavior-first browser/API scenarios for E2E-07.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java` - Provides fake no-key runtime and readiness controller for browser E2E.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/InMemoryCloudE2EConfiguration.java` - Allows e2e in-memory runtime bean overrides.
- `docs/phase-05-web-console.md` - Documents public Phase 5 API/UI contracts, security/redaction boundaries, E2E commands, and explicit deferrals.
- `.planning/REQUIREMENTS.md` - Marks Phase 5 GUI/E2E requirements complete with validation evidence only for delivered scope.
- `.gitignore` - Ignores generated Playwright/Vaadin runtime artifacts.

## Decisions Made

- Use Playwright as the browser E2E standard for Phase 5 because it validates modern browser/API behavior and artifacts while remaining test-only.
- Keep the browser E2E runtime no-key and deterministic by running Spring Boot from the test classpath with fake model/tool/approval data and in-memory stores.
- Assert behavior and public contracts instead of brittle pixel baselines; screenshots are attached for key pages/debugging and traces/videos are retained on failure.
- Preserve inspect-only Admin Governance scope: extension, MCP, and plugin panels remain placeholder status metadata until later dedicated phases.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added explicit Spring Boot Maven plugin path for E2E startup**
- **Found during:** Task 1 (Playwright harness)
- **Issue:** Browser webServer startup needed a reliable way to launch the application from the test harness.
- **Fix:** Added/adjusted E2E startup scripting so the app is built and launched deterministically for Playwright.
- **Files modified:** `scripts/e2e-web-server.sh`, `playwright.config.ts`
- **Verification:** `npm run e2e -- --list`; `npm run e2e`
- **Committed in:** `7fb7a64`

**2. [Rule 3 - Blocking] Started E2E server from test classpath**
- **Found during:** Task 1 (fake runtime fixture)
- **Issue:** The Playwright server had to see test-only fixture beans that are not on the production runtime classpath.
- **Fix:** Built the test classpath and launched `PiCloudServerApplication` with test classes before main classes.
- **Files modified:** `scripts/e2e-web-server.sh`
- **Verification:** `npm run e2e`
- **Committed in:** `3d260e0`

**3. [Rule 3 - Blocking] Forced Java 21 for E2E web server script**
- **Found during:** Task 1 (environment setup)
- **Issue:** Maven/Java launcher environments can default to a non-project Java version.
- **Fix:** Parameterized E2E Java home with a Java 21 default and used it for Maven/server launch.
- **Files modified:** `scripts/e2e-web-server.sh`
- **Verification:** `npm run e2e`
- **Committed in:** `30d3ecf`

**4. [Rule 3 - Blocking] Allowed E2E in-memory runtime bean overrides**
- **Found during:** Task 1 (Spring test profile wiring)
- **Issue:** The no-key fake runtime fixture needed to override normal runtime beans in the browser E2E profile.
- **Fix:** Enabled bean override for the E2E server and adjusted in-memory fixture wiring.
- **Files modified:** `scripts/e2e-web-server.sh`, `InMemoryCloudE2EConfiguration.java`, `WebConsoleE2EFixtureConfiguration.java`
- **Verification:** `npm run e2e`
- **Committed in:** `5276342`

**5. [Rule 3 - Blocking] Restored Playwright package after Vaadin dev bootstrap**
- **Found during:** Task 1/2 verification
- **Issue:** Vaadin frontend bootstrap modified package metadata needed by Playwright and Vaadin together.
- **Fix:** Reconciled `package.json`/lock metadata so Playwright and Vaadin frontend dependencies coexist.
- **Files modified:** `package.json`, `package-lock.json`
- **Verification:** `npm run e2e -- --list`; `npm run e2e`
- **Committed in:** `7e69285`

---

**Total deviations:** 5 auto-fixed blocking issues.
**Impact on plan:** All fixes were required to make the planned deterministic browser E2E executable in this repository; no product scope was added beyond the plan.

## Issues Encountered

- Full reactor validation with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am test` was blocked by environment because Testcontainers could not find `/var/run/docker.sock`.
- Focused adapter validation with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web test` also ran non-Docker tests but ended with Docker-gated Testcontainers errors in `CloudServerHeadlessE2ETest` and `RunCancellationIntegrationTest`.
- Per the execution instruction, Docker/Testcontainers absence is documented rather than fixed here; deterministic no-key browser E2E passed.

## Validation Results

- `npm run e2e:install -- --with-deps=false` — PASSED.
- `npm run e2e -- --list` — PASSED; discovered 4 Chromium tests in `e2e/phase-05-web-console.spec.ts`.
- `npm run e2e` — PASSED; 4/4 Playwright tests passed against the fake/no-key Spring Boot Vaadin app.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am test` — BLOCKED by missing Docker/Testcontainers environment.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web test` — BLOCKED by Docker-gated Testcontainers tests after running available focused adapter tests.

## Known Stubs

- `docs/phase-05-web-console.md:15` and `e2e/phase-05-web-console.spec.ts:78` refer to extension/MCP/plugin placeholder governance views. These are intentional Phase 5 inspect-only status placeholders; real extension, MCP, and plugin implementations are explicitly deferred to Phases 6, 7, and 8.

## User Setup Required

None for default verification. Browser E2E uses no real model keys, no Docker, and no external services. Docker is required only for the pre-existing Testcontainers-backed Maven tests.

## Next Phase Readiness

- Phase 6 can use the documented public Admin Governance placeholder contracts to add real SPI/Spring extension status data.
- Phases 7 and 8 can replace MCP/plugin placeholders with real read models while preserving the Phase 5 inspect-only/public-API UI boundary.
- Phase 9 can build on the browser E2E harness for observability and hardening regressions.

## Self-Check: PASSED

- Verified key files exist: `playwright.config.ts`, `e2e/phase-05-web-console.spec.ts`, `e2e/fixtures/fake-runtime.ts`, `WebConsoleE2EFixtureConfiguration.java`, `docs/phase-05-web-console.md`.
- Verified task commits exist in git history: `628bbd9`, `0882678`, `029a063` plus supporting fixes.
- Verified deterministic validation passed: `npm run e2e:install -- --with-deps=false`, `npm run e2e -- --list`, and `npm run e2e`.

---
*Phase: 05-agent-web-console-and-runtime-cockpit*
*Completed: 2026-06-15*
