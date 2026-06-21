---
phase: 10-responsive-baseline-and-mobile-test-harness
plan: 02
subsystem: testing
tags: [playwright, mobile, responsive, e2e, browser-matrix]

requires:
  - phase: 10-responsive-baseline-and-mobile-test-harness
    provides: Phase 10 responsive baseline context and MVER-01 representative matrix requirement
provides:
  - Desktop Chrome plus representative Mobile Chrome, Mobile Safari/WebKit, Mobile Firefox proxy, and Tablet Playwright projects
  - Browser install script coverage for chromium, firefox, and webkit engines
  - Reusable mobile smoke helpers for no-horizontal-overflow and stable selector assertions
  - Phase 10 matrix documentation with WebKit-as-Mobile-Safari and Firefox mobile emulation limits
affects: [phase-10-route-smoke, phase-15-release-hardening, mobile-verification]

tech-stack:
  added: []
  patterns: [Playwright named project matrix, mobile route smoke helper module, documented CI emulation boundary]

key-files:
  created: [e2e/fixtures/mobile-smoke.ts, docs/phase-10-mobile-baseline.md]
  modified: [playwright.config.ts, scripts/e2e-install.sh]

key-decisions:
  - "Represent Mobile Firefox with Firefox engine plus mobile viewport/touch/user-agent flags because Playwright CI does not provide true Firefox for Android."
  - "Use WebKit iPhone emulation as the Phase 10 Mobile Safari proxy and defer real-device Safari UAT to Phase 15."

patterns-established:
  - "Stable Playwright project names: chromium, Mobile Chrome, Mobile Safari, Mobile Firefox, Tablet."
  - "Mobile smoke specs should assert data-route markers, primary content/actions, and document/body horizontal overflow through shared helpers."

requirements-completed: [MVER-01]

duration: 5min
completed: 2026-06-21
---

# Phase 10 Plan 02: Representative Playwright Mobile Matrix Summary

**Representative mobile/tablet Playwright matrix with browser install coverage and reusable no-overflow smoke helpers for Phase 10 mobile verification.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-06-21T01:30:47Z
- **Completed:** 2026-06-21T01:35:27Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Preserved existing Desktop Chrome regression coverage under `chromium` while adding `Mobile Chrome`, `Mobile Safari`, `Mobile Firefox`, and `Tablet` projects.
- Updated the E2E browser install script so CI/developers install `chromium firefox webkit` through the local Playwright CLI while preserving `--with-deps=false` normalization.
- Added shared mobile smoke helpers for stable selector visibility, primary content/action assertions, and page-level no-horizontal-overflow checks against both document and body.
- Documented the exact Phase 10 project matrix and CI emulation gaps, including WebKit-as-Mobile-Safari and Firefox mobile proxy limitations, with Phase 15 real-device/UAT handoff.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add representative Playwright projects** - `cad24ec` (feat)
2. **Task 2: Install browser engines needed by matrix** - `0004193` (chore)
3. **Task 3: Add shared mobile smoke helpers and matrix documentation** - `b2ff8cf` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `playwright.config.ts` - Adds named Desktop Chrome, Mobile Chrome, Mobile Safari/WebKit, Mobile Firefox proxy, and Tablet Playwright projects.
- `scripts/e2e-install.sh` - Installs Chromium, Firefox, and WebKit through the existing local Playwright CLI while keeping `--with-deps=false` normalization.
- `e2e/fixtures/mobile-smoke.ts` - Provides route metadata types, `expectNoPageHorizontalOverflow`, stable selector visibility checks, primary content/action checks, and a route baseline helper.
- `docs/phase-10-mobile-baseline.md` - Documents project names, coverage approximations, install command, helper usage, emulation gaps, and Phase 15 real-device/UAT handoff.

## Decisions Made

- Represented Mobile Firefox with Firefox engine plus a mobile-sized/touch/user-agent context because Playwright standard CI does not ship a true Firefox for Android target.
- Used WebKit iPhone emulation for `Mobile Safari` and documented it as a CI proxy rather than real iOS Safari coverage.
- Kept Phase 10 to a representative matrix only; broad orientation and real-device coverage remain Phase 15.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None. The `selectors: MobileSmokeSelector[] = []` helper default is a defensive optional-argument default and does not feed UI rendering or mask missing route smoke data; route-level primary content/action helpers explicitly require non-empty selectors.

## Issues Encountered

- `npm run e2e:install -- --with-deps=false` completed browser installation but Playwright reported host dependency warnings for WebKit/Firefox runtime libraries in this container. This is an environment warning, not a script failure; CI images should provide these OS packages or run Playwright dependency installation separately.
- An extra exploratory TypeScript-only compile command failed because the repository does not currently include Node type declarations for standalone `tsc` against Playwright types. This was not part of the plan verification; `npm run e2e -- --list` successfully loaded the helper-adjacent Playwright setup.

## User Setup Required

None - no external service configuration required. CI runners for actual browser execution must include Playwright host OS dependencies for WebKit/Firefox, as documented by Playwright's dependency warning.

## Verification

- `npm run e2e:install -- --with-deps=false` — passed with Playwright host dependency warning in this container.
- `npm run e2e -- --list` — passed; listed 35 tests across `chromium`, `Mobile Chrome`, `Mobile Safari`, `Mobile Firefox`, and `Tablet`.

## Auth Gates

None.

## Deferred Issues

None for this plan. The Playwright host dependency warning is an execution environment prerequisite, not an implementation defect in the matrix or install script.

## Next Phase Readiness

- Plan 10-03 can import `e2e/fixtures/mobile-smoke.ts` to build route-level mobile smoke gates.
- Downstream route smoke specs should use the stable project names and documented emulation boundaries from `docs/phase-10-mobile-baseline.md`.
- Phase 15 should expand from this representative baseline to final cross-browser, orientation, accessibility, and real-device/UAT release hardening.

## Self-Check: PASSED

- FOUND: `playwright.config.ts`
- FOUND: `scripts/e2e-install.sh`
- FOUND: `e2e/fixtures/mobile-smoke.ts`
- FOUND: `docs/phase-10-mobile-baseline.md`
- FOUND commits: `cad24ec`, `0004193`, `b2ff8cf`

---
*Phase: 10-responsive-baseline-and-mobile-test-harness*
*Completed: 2026-06-21*
