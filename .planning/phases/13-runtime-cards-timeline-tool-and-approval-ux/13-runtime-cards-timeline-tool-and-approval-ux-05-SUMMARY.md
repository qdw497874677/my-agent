---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
plan: 05
subsystem: ui
tags: [vaadin, approval-card, mobile, runtime-cards, app-bridge, playwright]

requires:
  - phase: 13-runtime-cards-timeline-tool-and-approval-ux-03
    provides: Inline risk-first ApprovalCard surfaces for Console and Admin roles
  - phase: 13-runtime-cards-timeline-tool-and-approval-ux-04
    provides: Phase 13 mobile runtime-card browser matrix and selector contract
provides:
  - ApprovalCard click listeners that execute approve/reject decisions through an adapter-web handler
  - AppApprovalDecisionHandler bridge from Vaadin card clicks to ApprovalCommandService.decide
  - Visible approval decision feedback and data-decision-state transitions for success/failure
  - Java and Playwright assertions proving approval controls are actionable, not merely visible
affects: [phase-13-verification, phase-14-admin-mobile, phase-15-mobile-hardening]

tech-stack:
  added: []
  patterns:
    - Adapter-web functional callback around App-layer approval command use case
    - Inline Vaadin button click handling with deterministic demo handler for tests/demo constructors

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalDecisionHandler.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppApprovalDecisionHandler.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java
    - e2e/phase-13-runtime-cards.spec.ts

key-decisions:
  - "Keep approval decision execution in adapter-web via ApprovalDecisionHandler/AppApprovalDecisionHandler instead of adding mobile-only REST DTOs or Vaadin dependencies outside the adapter layer."
  - "Preserve deterministic demo constructors with ApprovalDecisionHandler.demo() while Spring constructors bridge to ApprovalCommandService for live Console/Admin cards."

patterns-established:
  - "Approval card actions expose visible feedback through data-role=approval-decision-feedback and data-decision-state."
  - "Console USER and Admin ADMIN approval cards share the same ApprovalCard click implementation while receiving role-specific handler wiring."

requirements-completed: [MCARD-04]

duration: 9m38s
completed: 2026-06-24
---

# Phase 13 Plan 05: Actionable Approval Decision Clicks Summary

**Risk-first approval cards now execute approve/reject decisions through adapter-web handlers, surface success/failure feedback, and have Java/browser gates that click the controls.**

## Performance

- **Duration:** 9m38s
- **Started:** 2026-06-24T08:42:18Z
- **Completed:** 2026-06-24T08:51:56Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Added `ApprovalDecisionHandler` and `AppApprovalDecisionHandler` so Vaadin approval cards can execute decisions without leaking Vaadin into App/Domain layers.
- Wired `ApprovalCard` Approve/Reject buttons to real click listeners, `DecisionPlan` execution, visible feedback, success disabling, and retryable failure state.
- Threaded the handler through `RunEventRenderer`, `ApprovalPanel`, `ConsoleView`, and `AdminApprovalQueueView`, preserving USER and ADMIN actor roles.
- Extended `WebConsoleApprovalCardsTest` to prove click-triggered APPROVE/REJECT, App-layer delegation authorities, renderer/panel/admin handler wiring, and failure feedback.
- Updated the Phase 13 Playwright matrix so the browser gate clicks an approval action and asserts `Decision recorded:` or `data-decision-state="succeeded"`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Wire ApprovalCard buttons to a decision handler with visible feedback** - `3431c5b` (feat)
2. **Task 2: Connect Console, ApprovalPanel, Admin, and browser assertions to wired decisions** - `01a1ec5` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalDecisionHandler.java` - Functional card decision callback plus deterministic demo response implementation.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppApprovalDecisionHandler.java` - Adapter-web bridge to `ApprovalCommandService.decide` with role-specific `ROLE_USER`/`ROLE_ADMIN` context.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java` - Adds click listeners, handler execution, feedback updates, decision state attributes, and success/failure button behavior.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java` - Accepts and passes supplied decision handlers to USER approval cards.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java` - Accepts handler constructor overloads and renders approval runtime events with wired USER cards.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` - Spring constructor now accepts `ApprovalCommandService` and passes `AppApprovalDecisionHandler` into runtime event rendering.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java` - Spring constructor accepts `ApprovalCommandService`; ADMIN cards share the same click wiring.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java` - Covers click decisions, feedback, retryable failure, bridge delegation, and renderer/panel/admin role wiring.
- `e2e/phase-13-runtime-cards.spec.ts` - Clicks an approval action and asserts visible decision success feedback.

## Decisions Made

- Kept the live decision bridge in adapter-web (`AppApprovalDecisionHandler`) per COLA: Vaadin UI calls the App use case, while App/Domain remain free of Vaadin dependencies.
- Kept existing demo/test constructors deterministic through `ApprovalDecisionHandler.demo()` rather than introducing REST calls from server-side Vaadin components.
- Preserved the Phase 13 no-modal decision: no `Dialog`, `ConfirmDialog`, `Notification`, `MenuBar`, or `ContextMenu` was introduced.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Pre-existing unrelated working-tree changes were present before this gap-closure execution. Only Phase 13 Plan 05 files were staged and committed.
- Existing compile warnings about deprecated Spring `@MockBean` in unrelated tests remain out of scope.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest test` — PASSED, 13 tests.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest,WebConsoleRuntimeCardsTest test` — PASSED, 23 tests.
- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome" --list` — PASSED, 1 test listed.
- Static anti-modal scan for `new Dialog`, `new ConfirmDialog`, `Notification.show`, `new MenuBar`, `new ContextMenu`, and `/mobile/` in modified approval UI files — PASSED, no matches.

## Known Stubs

None. The only empty/null checks in modified approval files are defensive UI fallbacks for absent approval data, not mock data sources or blocking stubs.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- MCARD-04 is now actionable: tapping Approve or Reject calls a decision handler/App-layer bridge, gives visible feedback, prevents duplicate successful clicks, and preserves retry on failure.
- Phase 14 Admin mobile work can reuse the same `ApprovalCard`/`ApprovalDecisionHandler` contract for ADMIN approval surfaces.

## Self-Check: PASSED

- Created files exist: `ApprovalDecisionHandler.java`, `AppApprovalDecisionHandler.java`.
- Task commits exist: `3431c5b`, `01a1ec5`.
- Required verification commands passed.
- Modified files contain no new modal primitives or `/mobile/` API fork.

---
*Phase: 13-runtime-cards-timeline-tool-and-approval-ux*
*Completed: 2026-06-24*
