---
phase: 10-responsive-baseline-and-mobile-test-harness
plan: 03
subsystem: testing
tags: [playwright, mobile, responsive, vaadin, route-smoke, overflow]

requires:
  - phase: 10-responsive-baseline-and-mobile-test-harness
    provides: pi-mobile theme baseline, stable Console/Admin route selectors, representative Playwright matrix, and mobile smoke helpers
provides:
  - Route-level Playwright smoke coverage for all D-05 Console/Admin Governance routes
  - Shared helper support for content-or-action route assertions
  - Targeted pi-mobile CSS constraints for Console workbench and dense Admin route surfaces
  - Phase 10 route coverage documentation and Phase 15 real-device/UAT handoff
affects: [phase-10, phase-11, phase-12, phase-14, phase-15, mobile-verification]

tech-stack:
  added: []
  patterns: [route metadata-driven Playwright smoke, data-route selector contract, page-level no-horizontal-overflow helper, scoped pi-mobile overflow fixes]

key-files:
  created: [e2e/phase-10-mobile-route-smoke.spec.ts]
  modified: [e2e/fixtures/mobile-smoke.ts, scripts/e2e-web-server.sh, pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css, docs/phase-10-mobile-baseline.md]

key-decisions:
  - "Keep Phase 10 route smoke focused on route load, stable selectors, primary content/actions, no page-level overflow, and one deterministic non-mutating interaction per route category."
  - "Run the Vaadin E2E server from the adapter-web module base in development mode so the project-owned `pi-mobile` theme is discoverable without requiring a production frontend bundle in this local harness."
  - "Limit CSS changes to targeted baseline overflow hotspots; final navigation, Console flow, runtime-card, approval UX, and Admin card/table migrations remain deferred."

patterns-established:
  - "D-05 routes are enumerated in a single typed route-smoke table and verified through `data-route` plus route-specific content/action selectors."
  - "Route smoke interactions avoid issuing mutation requests; they focus/read/inspect safe UI controls and empty/list states."
  - "Admin dense rows are baseline-constrained with `max-width`, `min-width: 0`, and `overflow-wrap: anywhere` before later mobile card migrations."

requirements-completed: [MH5-01, MH5-03, MVER-02]

duration: 37m09s
completed: 2026-06-21
---

# Phase 10 Plan 03: Route-Level Mobile Smoke Gates and Targeted Overflow Fixes Summary

**Playwright route smoke coverage for all Console/Admin Governance routes with no-overflow assertions, safe route interactions, and scoped pi-mobile baseline overflow fixes.**

## Performance

- **Duration:** 37m09s
- **Started:** 2026-06-21T01:39:58Z
- **Completed:** 2026-06-21T02:17:07Z
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments

- Added `e2e/phase-10-mobile-route-smoke.spec.ts` covering all eight D-05 routes: `/console`, `/admin/governance`, `/admin/governance/overview`, `/admin/governance/registry`, `/admin/governance/operations`, `/admin/governance/policy-decisions`, `/admin/governance/audits`, and `/admin/governance/approvals`.
- Extended the shared mobile smoke fixture with a content-or-action assertion helper so route smoke can support simple landing pages and action-heavy pages without faking missing selectors.
- Added targeted `pi-mobile` CSS for high-risk overflow surfaces: Console workbench collapse to one mobile column, Admin governance route containers, dense status rows, operations sections, policy/audit rows, and approval contexts.
- Updated Phase 10 mobile baseline documentation with route coverage, exact assertions/interactions, representative commands, local execution limitations, and Phase 15 real-device/orientation/accessibility handoff.

## Task Commits

Each task was committed atomically:

1. **Task 1: Create route-level mobile smoke spec** - `0cfd750` (feat)
2. **Task 2: Apply targeted high-risk overflow fixes for baseline viability** - `dec59a6` (fix)
3. **Task 3: Run representative matrix and document smoke coverage** - `7afd944` (docs)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `e2e/phase-10-mobile-route-smoke.spec.ts` - Route metadata-driven Playwright smoke spec for the D-05 Console/Admin route set, asserting route markers, primary content/action selectors, safe interactions, and page-level horizontal overflow.
- `e2e/fixtures/mobile-smoke.ts` - Adds `expectPrimaryContentOrActionVisible` for route smoke cases that require at least one primary content/action selector without forcing both categories on simple pages.
- `scripts/e2e-web-server.sh` - Starts the Vaadin E2E app from `pi-agent-adapter-web` with an explicit `project.basedir` and development mode so module-local frontend theme files are discoverable during browser smoke runs.
- `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` - Adds scoped high-risk overflow constraints for Console workbench and Admin Governance route surfaces.
- `docs/phase-10-mobile-baseline.md` - Documents route list, assertions, commands, local environment limitation, and Phase 15 handoff.

## Decisions Made

- Route smoke remains a Phase 10 baseline gate, not a final mobile product-flow conversion. It verifies route availability, stable markers, primary content/action visibility, safe interaction, and no page-level overflow.
- Registry-route interaction verifies the `data-mutation-controls="absent"` marker rather than clicking refresh/disable/quarantine controls, keeping this smoke non-mutating and deterministic.
- Local E2E server startup was adjusted to adapter-web module context because Vaadin resolves theme files relative to the project base; this preserves the Java/Vaadin-first architecture and avoids adding a frontend build artifact dependency to the route smoke plan.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed E2E server theme discovery for Vaadin route smoke**
- **Found during:** Task 1 verification
- **Issue:** Browser smoke startup logged that the `pi-mobile` theme folder could not be found because the E2E server was launched from the repository root while the theme lives under `pi-agent-adapter-web/src/main/frontend`.
- **Fix:** Updated `scripts/e2e-web-server.sh` to launch the Java process from the adapter-web module with absolute classpath entries and `-Dproject.basedir` set to that module.
- **Files modified:** `scripts/e2e-web-server.sh`
- **Verification:** Vaadin development startup then found the module frontend/theme and reported that a development mode bundle build was not needed.
- **Committed in:** `0cfd750`

**2. [Rule 3 - Blocking] Restored Playwright dependencies after Vaadin dev-mode pruning**
- **Found during:** Task 1/3 verification
- **Issue:** Vaadin dev-mode startup pruned Playwright-related Node packages from `node_modules`, causing Playwright module resolution failures in this local workspace.
- **Fix:** Re-ran `npm install --no-audit --no-fund` before Playwright list checks; no package manifest or lockfile changes were required.
- **Files modified:** none committed
- **Verification:** `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --list` listed all 40 configured matrix tests.
- **Committed in:** not applicable; environment repair only

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both were necessary to exercise the planned Playwright/Vaadin verification harness locally and did not expand product scope.

## Issues Encountered

- Full browser execution in this container reached Vaadin route bootstrap but timed out during the first route's client-side initialization. The route smoke spec itself loads and enumerates all expected tests, and the E2E server now resolves the `pi-mobile` theme from the module base. The full smoke command should be run in CI or a developer environment with stable Vaadin frontend/dev-server startup and Playwright browser host dependencies.
- `src/main/frontend/` is ignored by repository rules, so the planned theme CSS update was staged with `git add -f`, matching Plan 10-01's precedent for intentional Vaadin theme files.
- Unrelated parallel-agent working-tree changes remain present and were not staged or modified by this plan.

## Verification

- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --project="Mobile Chrome" --list` — passed; listed the eight D-05 route smoke tests for Mobile Chrome.
- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --list` — passed; listed 40 tests across `chromium`, `Mobile Chrome`, `Mobile Safari`, `Mobile Firefox`, and `Tablet`.
- `npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --project="Mobile Chrome"` — attempted; server/theme discovery fixed, but local Vaadin client bootstrap timed out before the first route completed in this container.

## Known Stubs

None. Stub-pattern scan found no plan-introduced placeholders, TODO/FIXME markers, mock data feeds, or empty UI data sources that prevent the Phase 10 route smoke goal. Existing empty/list states are intentional route-state assertions for no-data smoke conditions.

## Auth Gates

None.

## Deferred Issues

- Full local browser execution remains environment-sensitive due to Vaadin dev-mode/frontend startup and Playwright dependency pruning interactions. CI should run the documented full commands after `npm run e2e:install -- --with-deps=false` on a stable browser runner.
- Final mobile navigation shell, Console mobile flow, runtime cards, approval UX, and Admin card/table migrations remain intentionally deferred to Phases 11-15.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 11 can build shared responsive shell/navigation on top of a route smoke gate that already enumerates every current Console/Admin Governance route.
- Phase 12 and Phase 14 can extend the same route metadata and no-overflow helper patterns for deeper Console and Admin product-path mobile tests.
- Phase 15 has explicit documentation for real-device, orientation, accessibility, and cross-browser UAT expectations beyond Phase 10 emulation.

## Self-Check: PASSED

- Found created/modified plan files: `e2e/phase-10-mobile-route-smoke.spec.ts`, `e2e/fixtures/mobile-smoke.ts`, `scripts/e2e-web-server.sh`, `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css`, `docs/phase-10-mobile-baseline.md`, and this summary.
- Found task commits: `0cfd750`, `dec59a6`, and `7afd944`.

---
*Phase: 10-responsive-baseline-and-mobile-test-harness*
*Completed: 2026-06-21*
