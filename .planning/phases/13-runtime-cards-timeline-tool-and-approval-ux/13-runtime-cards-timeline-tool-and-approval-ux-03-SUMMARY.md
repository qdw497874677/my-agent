---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
plan: 03
subsystem: ui
tags: [vaadin, mobile, approvals, runtime-cards, governance]

# Dependency graph
requires:
  - phase: 13-runtime-cards-timeline-tool-and-approval-ux
    provides: Runtime card renderer seams and redaction utility from Plans 01-02
provides:
  - Risk-first reusable ApprovalCard for Console USER and Admin ADMIN approval decisions
  - Inline touch-safe approval action row hooks without dialog or second confirmation
  - Approval card redaction and stable mobile selector contracts
affects: [phase-13-runtime-cards, phase-14-admin-mobile-coverage, phase-15-accessibility-hardening]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Vaadin approval cards expose stable data-* selectors for mobile browser tests
    - RuntimeDetailRedactor is reused for approval preview and argument rendering

key-files:
  created:
    - .planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-03-SUMMARY.md
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java

key-decisions:
  - "Keep approval decisions inline in ApprovalCard with no Dialog, ConfirmDialog, Notification, MenuBar, or ContextMenu confirmation layer."
  - "Reuse the same ApprovalCard constructor for Console USER and Admin ADMIN roles while preserving public approval decision API paths."
  - "Render approval preview, arguments, and eligible roles through RuntimeDetailRedactor so mobile summaries stay conservative."

patterns-established:
  - "ApprovalCard risk-first hierarchy: title, risk/side-effect/policy/consequence fields, provision preview, arguments, inline actions, and feedback."
  - "Approval action row contract: .pi-action-row with data-approval-actions=inline and data-risk-action approve/reject button hooks."

requirements-completed: [MCARD-04, MCARD-03, MCARD-05]

# Metrics
duration: 5min
completed: 2026-06-24
---

# Phase 13 Plan 03: Approval Card UX Summary

**Risk-first inline approval cards with redacted decision context and shared USER/ADMIN decision planning**

## Performance

- **Duration:** 5min
- **Started:** 2026-06-24T06:04:55Z
- **Completed:** 2026-06-24T06:09:56Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Refactored `ApprovalCard` into a risk-first hierarchy that exposes approval requirement, risk, side effect, policy reason, expected consequence, provision preview, and arguments before action controls.
- Reused `RuntimeDetailRedactor` for provision previews, argument summaries, and eligible-role details so raw token/API key/bearer values are hidden in summaries and detail text.
- Added inline touch-safe action hooks with `.pi-action-row`, `data-approval-actions="inline"`, `data-risk-action="approve|reject"`, and visible feedback selector `data-role="approval-decision-feedback"`.
- Extended approval card tests to cover risk selectors, redaction, USER/ADMIN actor-role decision plans, and Admin queue reuse without broad Admin page conversion.

## Task Commits

Each task was committed atomically:

1. **Task 1: Refactor ApprovalCard into risk-first hierarchy** - `9bafb4a` (feat)
2. **Task 2: Harden inline approval actions and USER/ADMIN reuse** - `8b54a13` (feat)

**Plan metadata:** pending final docs commit

_Note: TDD tasks were executed with RED/GREEN verification; the RED tests and implementation were kept together in each task commit to preserve the requested per-task atomic commit boundary._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java` - Risk-first reusable Vaadin approval card with redacted summaries, stable selector attributes, and inline decision action row.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java` - Approval card contract tests for redaction, stable selectors, inline actions, and USER/ADMIN decision planning.
- `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-03-SUMMARY.md` - Execution summary and traceability record.

## Decisions Made

- Kept approval actions inline with no second modal/confirmation layer to preserve D-07/D-08 mobile UX.
- Kept Admin scope limited to existing `ApprovalCard` reuse through the `ADMIN` actor role, avoiding Phase 14 full Admin conversion work.
- Treated approval preview/argument rendering as sensitive runtime detail and routed it through the existing adapter-web redactor instead of adding a second redaction path.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None. The fallback/null checks in `ApprovalCard` are defensive display defaults and do not introduce mock or unwired UI data.

## Issues Encountered

- Existing unrelated working-tree changes were present before execution. They were not touched or staged; only plan-owned files and planning metadata were committed.
- The first self-check attempt used unavailable `rg`; reran the self-check with `git cat-file` and file existence checks successfully.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest test` — passed after Task 1.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest,WebConsoleRuntimeCardsTest test` — passed after Task 2.
- Modal primitive scan for `new Dialog`, `new ConfirmDialog`, `Notification.show`, `new MenuBar`, and `new ContextMenu` in UI Java files returned no matches.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 14 can reuse the same `ApprovalCard` pattern for Admin mobile coverage without changing approval API contracts.
- Phase 15 can target stable approval selectors for browser, touch, and accessibility hardening.

## Self-Check: PASSED

- Found `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java`.
- Found `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java`.
- Found task commit `9bafb4a`.
- Found task commit `8b54a13`.

---
*Phase: 13-runtime-cards-timeline-tool-and-approval-ux*
*Completed: 2026-06-24*
