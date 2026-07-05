---
phase: 21-verification-security-and-regression-hardening
plan: 04
subsystem: testing
tags: [playwright, browser-regression, console-product-path, streaming, no-key-gate]

requires:
  - phase: 17-console-session-restore-ux
    provides: Recent session restore selectors, active session banner, and same-session continuation browser coverage.
  - phase: 18-streaming-bubble-lifecycle
    provides: Fake-runtime stream helpers, reducer-owned assistant bubble selectors, cancellation, and failure semantics.
provides:
  - Consolidated Phase 21 Playwright product-path regression spec for VER-04.
  - Browser assertions for recent session restore, continuation, slow streaming, cancellation, provider failure, and raw runtime-event noise suppression.
  - VER-04 documentation with local list and live Chromium release commands.
affects: [phase-21-verification-hardening, console-browser-release-gates, v1.2-regression]

tech-stack:
  added: []
  patterns: [stable data-role Playwright selectors, fake-runtime seeded product paths, no-key list gate plus live release gate]

key-files:
  created:
    - e2e/phase-21-console-product-path-regression.spec.ts
    - .planning/phases/21-verification-security-and-regression-hardening/21-verification-security-and-regression-hardening-04-SUMMARY.md
  modified:
    - docs/phase-21-verification-hardening.md

key-decisions:
  - "VER-04 uses one consolidated Phase 21 Playwright spec to cover the Kimi-style Console product path across restore, continuation, streaming, cancellation, and failure paths."
  - "Keep the local VER-04 gate as Playwright --list/no-key while documenting the live Chromium command as the release gate that requires a running Vaadin server."

patterns-established:
  - "Browser product-path specs should assert stable data-* selectors and semantic chat text rather than screenshots or Vaadin internals."
  - "Main chat no-noise checks explicitly reject raw runtime-event names such as run.started, model.delta, model.completed, tool.call, and RunEvent."

requirements-completed: [VER-04]

duration: 3m53s
completed: 2026-07-05
---

# Phase 21 Plan 04: Console Browser Product Path Regression Summary

**Phase 21 Playwright coverage now exercises the Console restore/continue/stream/cancel/failure product path and documents the live VER-04 browser release gate.**

## Performance

- **Duration:** 3m53s
- **Started:** 2026-07-05T06:59:45Z
- **Completed:** 2026-07-05T07:03:38Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added `e2e/phase-21-console-product-path-regression.spec.ts`, a consolidated Phase 21 Playwright spec with stable Console selectors for recent session cards, active-session continuation banners, chat input, primary assistant bubbles, and cancel actions.
- Covered restored session selection, same-session follow-up sends, one assistant bubble for slow streams, cancellation preserving partial output, failed provider rendering, and explicit raw runtime-event noise suppression.
- Documented the VER-04 local/no-key Playwright `--list` command and the live Chromium browser command requiring a running server.

## Verification

The planned local syntax/list gate passed after each task:

```bash
npx playwright test e2e/phase-21-console-product-path-regression.spec.ts --list
```

Result: 20 tests listed across the configured Chromium, Mobile Chrome, Mobile Safari, Mobile Firefox, and Tablet projects.

The release/live browser gate is documented but was not run in this local no-server execution context:

```bash
PI_E2E_PORT=18080 npx playwright test e2e/phase-21-console-product-path-regression.spec.ts --project=chromium
```

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Phase 21 browser product-path spec** - `73595c6` (test)
2. **Task 2: Document browser release gates** - `53be078` (docs)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `e2e/phase-21-console-product-path-regression.spec.ts` - New consolidated browser product-path regression spec for VER-04.
- `docs/phase-21-verification-hardening.md` - Added the VER-04 browser product path gate with exact local and live Playwright commands.
- `.planning/phases/21-verification-security-and-regression-hardening/21-verification-security-and-regression-hardening-04-SUMMARY.md` - Execution summary and self-check for this plan.

## Decisions Made

- VER-04 uses one consolidated Phase 21 Playwright spec to cover the Kimi-style Console product path across restore, continuation, streaming, cancellation, and failure paths.
- Keep the local VER-04 gate as Playwright `--list`/no-key while documenting the live Chromium command as the release gate that requires a running Vaadin server.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None found in files created or modified by this plan.

## Issues Encountered

- The live Chromium command was documented but not executed because no running Vaadin server was established for this local execution. This matches the plan's CI/release-gated acceptance note; VER-04 still requires that live gate before release sign-off.
- The working tree contained pre-existing parallel-executor changes (`.gitignore`, Phase 21 planning artifacts, Phase 17 verification notes, and a Phase 21 Plan 05 test file). They were left untouched and were not included in task commits.

## User Setup Required

None - no external service configuration required for the local `--list` gate. Release owners must start the server on `PI_E2E_PORT=18080` before running the live browser gate.

## Next Phase Readiness

- VER-04 has a no-key local browser syntax gate and a documented live product-path release command.
- Phase 21 Plan 05 can build on the documented Phase 21 hardening gates without changing Console runtime behavior.

## Self-Check: PASSED

- Verified created/modified files exist: summary, `e2e/phase-21-console-product-path-regression.spec.ts`, and `docs/phase-21-verification-hardening.md`.
- Verified task commits exist: `73595c6` and `53be078`.

---
*Phase: 21-verification-security-and-regression-hardening*
*Completed: 2026-07-05*
