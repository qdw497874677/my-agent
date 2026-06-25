---
phase: 15-cross-browser-orientation-accessibility-and-release-hardening
plan: 03
subsystem: ui-accessibility-testing
tags: [vaadin, playwright, accessibility, keyboard, reduced-motion, css, mobile, tablet]

# Dependency graph
requires:
  - phase: 15-cross-browser-orientation-accessibility-and-release-hardening
    provides: Phase 15 portrait/landscape/tablet smoke helper foundation and shared mobile route assertions
  - phase: 14-admin-governance-full-site-mobile-coverage
    provides: Admin Governance mobile card/detail selector contracts
  - phase: 13-runtime-cards-timeline-tool-and-approval-ux
    provides: Runtime/tool/approval card selector and detail-layer contracts
provides:
  - Representative Playwright accessibility hardening checks for shell, Console, runtime/approval, Admin, and reduced-motion behavior
  - pi-mobile CSS contracts for no-hover affordances, reduced-motion users, current nav focus signal, and tablet bridge layout
  - Fast Java static contract tests locking Phase 15 D-10 through D-13 CSS requirements
affects: [phase-15-release-hardening, mobile-h5, accessibility, pi-agent-adapter-web]

# Tech tracking
tech-stack:
  added: []
  patterns: [Playwright representative focus sampling, static CSS contract tests, pi-mobile media-query hardening]

key-files:
  created:
    - e2e/phase-15-accessibility-hardening.spec.ts
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebPhase15AccessibilityContractTest.java
  modified:
    - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css

key-decisions:
  - "Keep Phase 15 accessibility checks representative and deterministic instead of introducing mandatory axe-core audits."
  - "Keep accessibility hardening in the pi-mobile presentation layer and static/browser tests, with no backend DTO or API changes."
  - "Use a bounded 641px-899px tablet bridge media query before the existing 900px desktop shell breakpoint."

patterns-established:
  - "Representative keyboard checks use stable data-* selectors plus expectFocusVisible rather than exhaustive Tab-chain assertions."
  - "Accessibility CSS regressions are protected by fast static JUnit assertions named for D-10 through D-13 release-hardening contracts."

requirements-completed: [MVER-05, MVER-06]

# Metrics
duration: 9m
completed: 2026-06-25
---

# Phase 15 Plan 03: Accessibility, Keyboard, Reduced-Motion, No-Hover, and Tablet Bridge Hardening Summary

**Representative keyboard/focus browser coverage with pi-mobile reduced-motion, no-hover, and tablet bridge CSS contracts locked by fast Java tests**

## Performance

- **Duration:** 9m
- **Started:** 2026-06-25T11:20:39Z
- **Completed:** 2026-06-25T11:29:07Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- Added a Phase 15 Playwright accessibility hardening spec covering representative shell navigation, Console composer/panel controls, runtime Details/approval actions, Admin controls, no-overflow, and reduced-motion shell drawer behavior.
- Hardened `pi-mobile` CSS with explicit no-hover affordance rules, `prefers-reduced-motion` transition/scroll minimization, current-page nav focus signal, and a tablet bridge breakpoint before desktop layout.
- Added `WebPhase15AccessibilityContractTest` static JUnit coverage for D-10 reduced motion, D-11 focus visibility, D-12 no-hover action affordances, and D-13 tablet bridge rules.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add representative keyboard and semantic accessibility checks** - `8cbad9d` (test)
2. **Task 2: Add reduced-motion, hover fallback, and tablet bridge CSS hardening** - `a76e92b` (fix)
3. **Task 3 RED: Add fast Phase 15 accessibility CSS contract test** - `f2def43` (test)
4. **Task 3 GREEN: Implement accessibility CSS contract** - `de63217` (feat)

_Note: Task 3 followed TDD and therefore has separate failing-test and green implementation commits._

## Files Created/Modified

- `e2e/phase-15-accessibility-hardening.spec.ts` - Playwright representative accessibility/focus/reduced-motion browser gate.
- `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` - Presentation-layer focus/no-hover/reduced-motion/tablet bridge CSS hardening.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebPhase15AccessibilityContractTest.java` - Static Java CSS contract gate for Phase 15 accessibility hardening.

## Verification

- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-accessibility-hardening.spec.ts --project="Mobile Chrome" --list` — passed; 5 tests discovered.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebPhase15AccessibilityContractTest,WebMobileBaselineContractTest,WebResponsiveShellContractTest test` — passed; 15 tests, 0 failures.

## Decisions Made

- Kept Phase 15 accessibility coverage as representative deterministic checks rather than mandatory axe-core audits, matching D-14 and avoiding noisy full-page accessibility scans.
- Kept all functional hardening in `pi-mobile` CSS and tests; no Vaadin component redesign, new routes, backend DTOs, or mobile-only APIs were introduced.
- Used an explicit tablet bridge range of `641px-899px` so tablet users get bounded Console/Admin density before the existing `900px` desktop shell layout.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Used forced staging for tracked ignored frontend theme CSS**
- **Found during:** Task 2 (CSS hardening commit)
- **Issue:** `pi-agent-adapter-web/src/main/frontend` is ignored by `.gitignore`, so plain `git add` refused to stage the tracked theme file in this repository state.
- **Fix:** Used targeted `git add -f pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css`; no unrelated ignored files were staged.
- **Files modified:** `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css`
- **Verification:** Final Maven CSS contract gate passed.
- **Committed in:** `a76e92b`, `de63217`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** No scope expansion; this was required to commit the planned tracked CSS file under the repository's ignored frontend directory.

## Issues Encountered

- Existing unrelated working-tree changes from other parallel agents were present before execution and were left untouched. All commits staged only plan-specific files.
- The new static CSS contract intentionally failed before the final CSS implementation, then passed after the green implementation commit.

## Known Stubs

None found in files created/modified by this plan.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 15 Plan 04 can reference the new accessibility spec and CSS contract when documenting real-device/UAT expectations.
- The final release-hardening verifier should run the full Playwright suite with a live web server; this plan's local browser gate used `--list` as specified.

## Self-Check: PASSED

- Verified created files exist: `e2e/phase-15-accessibility-hardening.spec.ts`, `WebPhase15AccessibilityContractTest.java`, and updated `styles.css`.
- Verified task commits exist: `8cbad9d`, `a76e92b`, `f2def43`, `de63217`.

---
*Phase: 15-cross-browser-orientation-accessibility-and-release-hardening*
*Completed: 2026-06-25*
