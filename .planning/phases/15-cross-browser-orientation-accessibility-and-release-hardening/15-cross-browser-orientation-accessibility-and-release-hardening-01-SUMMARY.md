---
phase: 15-cross-browser-orientation-accessibility-and-release-hardening
plan: 01
subsystem: testing
tags: [playwright, mobile, orientation, viewport, release-smoke]

requires:
  - phase: 10-responsive-baseline-and-mobile-test-harness
    provides: Mobile smoke route metadata style and no-overflow helper foundation
  - phase: 11-shared-responsive-shell-and-navigation
    provides: Shared responsive shell, drawer/navigation selectors, and route navigation contract
  - phase: 14-admin-governance-full-site-mobile-coverage
    provides: Full Admin Governance mobile route selector coverage
provides:
  - Phase 15 portrait/landscape/tablet viewport cases for reusable browser smoke coverage
  - All-route orientation release smoke spec covering Console and seven Admin Governance routes
  - Layered discovery gate across chromium, Mobile Chrome, Mobile Safari, Mobile Firefox, and Tablet projects
affects: [phase-15, MVER-05, mobile-release-hardening, playwright]

tech-stack:
  added: []
  patterns:
    - In-test viewport switching with page.setViewportSize instead of dedicated landscape Playwright projects
    - Structural route/shell/navigation/critical-control assertions rather than screenshot baselines

key-files:
  created:
    - e2e/phase-15-orientation-release-smoke.spec.ts
  modified:
    - e2e/fixtures/mobile-smoke.ts

key-decisions:
  - "Use reusable named viewport cases in mobile-smoke.ts so future Phase 15 gates share portrait, landscape, and tablet dimensions."
  - "Keep landscape coverage inside the existing Playwright project matrix with page.setViewportSize rather than adding dedicated landscape projects."
  - "Assert structural shell, route, nav, critical control, and no-overflow contracts instead of screenshot visual baselines."

patterns-established:
  - "Phase 15 orientation gates iterate route metadata × phase15ViewportCases and validate shared shell, route marker, primary content/action, active nav, critical controls, and page-level overflow."
  - "Layered browser release smoke uses the existing Playwright projects unchanged: chromium, Mobile Chrome, Mobile Safari, Mobile Firefox, and Tablet."

requirements-completed: [MVER-05]

duration: 4m30s
completed: 2026-06-25
---

# Phase 15 Plan 01: All-Route Orientation Release Smoke Summary

**All-route Playwright orientation smoke matrix with reusable portrait, landscape, and tablet viewport switching for MVER-05.**

## Performance

- **Duration:** 4m30s
- **Started:** 2026-06-25T11:11:48Z
- **Completed:** 2026-06-25T11:16:18Z
- **Tasks:** 3
- **Files modified:** 2

## Accomplishments

- Added `Phase15ViewportCase`, `phase15ViewportCases`, and `expectPhase15RouteViewportBaseline` to the shared mobile smoke fixture.
- Created an all-route Phase 15 orientation release smoke spec covering `/console` plus all seven Admin Governance routes from D-01.
- Verified the spec lists deterministic portrait, landscape, and tablet coverage under all five existing Playwright projects without changing `playwright.config.ts`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add reusable Phase 15 viewport/orientation helpers** - `fce098c` (test)
2. **Task 2: Create all-route portrait/landscape/tablet orientation smoke spec** - `f46c057` (test)
3. **Task 3: Prove the layered browser matrix discovers Phase 15 orientation coverage** - `5f1bccf` (test)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `e2e/fixtures/mobile-smoke.ts` - Exports the Phase 15 viewport case type, named portrait/landscape/tablet cases, and a reusable route viewport baseline assertion that switches viewport, navigates, verifies shell/route markers, primary content/action visibility, and no page-level horizontal overflow.
- `e2e/phase-15-orientation-release-smoke.spec.ts` - Adds a route × viewport matrix for the Console and Admin Governance routes, with active navigation, critical control, landscape shell/drawer/nav, and no-overflow checks.

## Verification

- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --project="Mobile Chrome" --list` — passed, 8 tests listed.
- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-orientation-release-smoke.spec.ts --project="Mobile Chrome" --list` — passed, 24 tests listed.
- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-orientation-release-smoke.spec.ts --project="chromium" --project="Mobile Chrome" --project="Mobile Safari" --project="Mobile Firefox" --project="Tablet" --list` — passed, 120 tests listed.

## Decisions Made

- Use 390×844 as representative phone portrait, 844×390 as representative phone landscape, and 834×1194 as the tablet bridge case.
- Keep viewport/orientation coverage in test helpers/specs only; no app code, screenshots, axe-core, or Playwright project changes were introduced.
- Use critical control selectors per route so landscape checks prove more than route load while remaining deterministic and no-key.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None - stub scan found no TODO/FIXME/placeholder text or hardcoded empty UI data stubs in the created/modified files.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02 can reuse `phase15ViewportCases` and the new spec's route metadata pattern for deeper Console/Admin critical-flow and desktop regression gates.
- MVER-05 now has a deterministic list/discovery gate across the existing layered browser matrix.

## Self-Check: PASSED

- Found `e2e/fixtures/mobile-smoke.ts`.
- Found `e2e/phase-15-orientation-release-smoke.spec.ts`.
- Found `.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-01-SUMMARY.md`.
- Found task commits `fce098c`, `f46c057`, and `5f1bccf` in git history.

---
*Phase: 15-cross-browser-orientation-accessibility-and-release-hardening*
*Completed: 2026-06-25*
