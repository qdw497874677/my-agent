---
phase: 15-cross-browser-orientation-accessibility-and-release-hardening
plan: 02
subsystem: testing
tags: [playwright, mobile, desktop-regression, console, admin-governance, release-gate]

requires:
  - phase: 12-console-mobile-first-flow
    provides: Mobile Console panel, session, event-feed, send, and cancellation selector contracts
  - phase: 13-runtime-cards-timeline-tool-and-approval-ux
    provides: Runtime/tool/approval card details, redaction markers, and deterministic fake-runtime prompts
  - phase: 14-admin-governance-full-site-mobile-coverage
    provides: Admin Governance card/detail selector matrix and redaction contracts
  - phase: 15-cross-browser-orientation-accessibility-and-release-hardening
    provides: Plan 01 reusable viewport/no-overflow helper foundation
provides:
  - Phase 15 critical Console flow release gate for run creation, feed growth, runtime inspection, sessions, and cancel/terminal behavior
  - Phase 15 Admin critical inspection release gate for representative card/detail surfaces, redaction, focus, tap, and no-overflow behavior
  - Phase 15 desktop Console/Admin release summary gate supplementing the existing Phase 05 desktop baseline
affects: [phase-15, MVER-05, MVER-06, playwright, release-hardening]

tech-stack:
  added: []
  patterns:
    - Single Phase 15 layered release spec with Console, Admin mobile inspection, and desktop summary describe blocks
    - Deterministic no-key fake-runtime prompts for deep Console release coverage without screenshots
    - Structural selector-based Admin/desktop assertions reusing prior mobile helper contracts

key-files:
  created:
    - e2e/phase-15-critical-flow-regression.spec.ts
  modified: []

key-decisions:
  - "Keep Phase 15 critical-flow coverage in a new release spec rather than modifying Phase 05 desktop baseline or prior mobile phase specs."
  - "Use stable data-* selectors and fake-runtime hints for Console run/runtime coverage; no screenshots or backend/API changes were introduced."
  - "Represent desktop release confidence as a compact Console plus Admin route summary under chromium while preserving detailed desktop Phase 05 tests unchanged."

patterns-established:
  - "Phase 15 release gates can layer mobile deep flows and desktop summary checks in one spec while discovery runs under both Mobile Chrome and chromium projects."
  - "Admin critical inspection routes share a local selector matrix with redaction, details, touch, focus, and overflow assertions."

requirements-completed: [MVER-05, MVER-06]

duration: 6m59s
completed: 2026-06-25
---

# Phase 15 Plan 02: Critical Console/Admin Flow and Desktop Regression Release Gate Summary

**Layered Playwright release gate for deep Console runtime/session behavior, Admin card/detail inspection, and desktop Console/Admin regression confidence.**

## Performance

- **Duration:** 6m59s
- **Started:** 2026-06-25T11:20:52Z
- **Completed:** 2026-06-25T11:27:51Z
- **Tasks:** 3
- **Files modified:** 1 primary plan artifact

## Accomplishments

- Created `e2e/phase-15-critical-flow-regression.spec.ts` with a Console critical-flow test that opens `/console`, selects the General Agent, submits deterministic fake-runtime prompts, observes event feed growth, inspects runtime/tool/approval surfaces, opens Sessions, and cancels or accepts terminal status.
- Added Admin critical inspection coverage for landing, overview, registry, operations, policy decisions, audits, and approvals with card/detail selector assertions, redaction checks, tap target sampling, focus visibility, and no-horizontal-overflow checks.
- Added a desktop release summary block under the same spec that supplements (without editing) `e2e/phase-05-web-console.spec.ts` by checking desktop Console workbench content/actions and representative Admin routes under `chromium`.

## Task Commits

Each task was committed atomically. TDD tasks include red and green commits:

1. **Task 1: Add stable Console critical path release checks**
   - `6b04bf9` (test): failing Console critical-flow release gate
   - `640875c` (feat): implemented deterministic fake-runtime Console critical-flow listing gate
2. **Task 2: Add Admin card/detail inspection release checks**
   - `eaec5ae` (test): failing Admin inspection release gate
   - `68a8924` (feat): implemented Admin route selector matrix and inspection listing gate
   - `b44590f` (chore): removed unrelated parallel theme changes accidentally captured during this plan's task commit
3. **Task 3: Add desktop release summary regression checks**
   - `fbdeba7` (test): failing desktop regression summary gate
   - `9268994` (feat): implemented compact desktop Console/Admin summary matrix

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `e2e/phase-15-critical-flow-regression.spec.ts` - New Phase 15 release-gate spec containing Console critical-flow, Admin card/detail inspection, and desktop Console/Admin regression summary coverage.

## Verification

- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="Mobile Chrome" --list` — passed, 15 tests listed.
- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="chromium" --list` — passed, 15 tests listed.

## Decisions Made

- Kept release hardening additive by creating a new Phase 15 spec instead of changing `e2e/phase-05-web-console.spec.ts`, preserving the existing desktop baseline.
- Used `mobileToolApprovalHint()` plus `phase13RuntimeCardMatrixHint()` for deterministic no-key Console coverage of runtime/tool/approval cards without introducing new app code or APIs.
- Used a representative desktop Admin route subset (landing, overview, registry, operations, policy decisions, audits) for the compact MVER-06 release summary while the mobile Admin inspection block keeps approvals coverage too.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Removed unrelated parallel-agent theme changes from a Plan 02 task commit**
- **Found during:** Task 2 (Admin inspection release checks)
- **Issue:** Because this agent ran in parallel with another Phase 15 executor, `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` was already staged/modified by parallel Plan 03 work and was accidentally included in the Task 2 red commit.
- **Fix:** Restored that file to the pre-Plan-02 state in `b44590f` so this plan's effective scope remains the Playwright release gate. The parallel Plan 03 agent later re-applied its own theme changes in its own commits.
- **Files modified:** `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` (transient restoration only)
- **Verification:** Final Plan 02 verification commands passed and `git status --short` shows no remaining uncommitted Plan 02 spec changes.
- **Committed in:** `b44590f`

---

**Total deviations:** 1 auto-fixed (Rule 3 blocking/parallel hygiene)
**Impact on plan:** No product/API scope change; the deviation kept Plan 02 commits scoped to the intended release-gate spec despite parallel executor contention.

## Known Stubs

None - stub scan found no TODO/FIXME/placeholder text or hardcoded empty UI data stubs in `e2e/phase-15-critical-flow-regression.spec.ts`.

## Issues Encountered

- Parallel execution interleaved Plan 03 commits into the git history during this plan. This did not block Plan 02 verification, but it required a scoped cleanup commit (`b44590f`) to avoid leaving unrelated theme changes attributed to Plan 02.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- MVER-06 now has a Phase 15-specific chromium discovery gate supplementing existing desktop Console/Admin browser coverage.
- Plan 03 can continue accessibility, keyboard, no-hover, and tablet hardening independently; its parallel theme/test commits are intentionally outside this Plan 02 summary.

## Self-Check: PASSED

- Found `e2e/phase-15-critical-flow-regression.spec.ts`.
- Found `.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-02-SUMMARY.md`.
- Found task commits `6b04bf9`, `640875c`, `eaec5ae`, `68a8924`, `b44590f`, `fbdeba7`, and `9268994` in git history.
- Verified Mobile Chrome and chromium list gates each discover 15 tests.

---
*Phase: 15-cross-browser-orientation-accessibility-and-release-hardening*
*Completed: 2026-06-25*
