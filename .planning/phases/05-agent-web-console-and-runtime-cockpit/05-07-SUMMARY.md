---
phase: 05-agent-web-console-and-runtime-cockpit
plan: 07
subsystem: ui
tags: [vaadin, approval-cards, admin-governance, approval-api, tool-governance]

requires:
  - phase: 05-agent-web-console-and-runtime-cockpit
    provides: public approval DTOs, approval list API, and decision endpoint from Plan 05-03
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: tool.approval_required lifecycle events with redacted preview and policy context
provides:
  - Actionable user approval cards rendered from approval-required tool lifecycle events
  - Console approval panel and public REST helper paths for approval list/decision APIs
  - Separated Admin Governance approval queue route preserving ADMIN actor role decisions
affects: [05-08-admin-governance-views, 05-09-browser-e2e, approval-flow, tool-cards]

tech-stack:
  added: []
  patterns: [Vaadin component read models, public API decision plans, redacted approval context rendering, separated admin approval surface]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/SecurityConfig.java

key-decisions:
  - "Approval UI components build backend decision plans instead of mutating local-only approval state."
  - "Approval-required tool lifecycle events are promoted from generic tool cards to dedicated approval cards when status/type indicates a gate."
  - "Admin approval decisions reuse ApprovalCard behavior but force an explicit ADMIN actor role under a separated Admin Governance route."

patterns-established:
  - "Vaadin approval components consume pi-agent-client ApprovalSummaryDto and ConsoleHttpClient paths only; they do not import App, Domain, or persistence types."
  - "Decision buttons expose ApprovalDecisionRequest plans containing original sessionId/runId/approvalId/toolCallId context for transport wiring."

requirements-completed: [GUI-06, GUI-08]

duration: 7m 52s
completed: 2026-06-15
---

# Phase 05 Plan 07: Approval Cards and Admin Approval Queue Summary

**Actionable Vaadin approval cards with redacted tool-risk context and backend-bound user/admin decision plans**

## Performance

- **Duration:** 7m 52s
- **Started:** 2026-06-15T05:58:44Z
- **Completed:** 2026-06-15T06:06:36Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added `ApprovalCard` and `ApprovalPanel` to display D-10 safe decision context: tool, policy reason, risk/side-effect labels, preview/impact, redacted arguments, expected consequence, and Approve/Reject actions.
- Extended `ConsoleHttpClient` and `RunEventRenderer` so approval-required lifecycle events render as backend-bound approval cards using `/api/sessions/{sessionId}/runs/{runId}/approvals/{approvalId}/decision`.
- Updated the chat stream panel to append component-backed approval cards into the same integrated run narrative.
- Added a separated `AdminApprovalQueueView` under `/admin/governance/approvals` that lists pending approval context and reuses approval decisions with an explicit `ADMIN` actor role.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Render approval-required events as actionable cards** - `479d127` (test)
2. **Task 1 GREEN: Render approval-required events as actionable cards** - `654f9e1` (feat)
3. **Task 2: Add Admin approval queue/read-action view** - `59be072` (feat)

**Plan metadata:** pending final docs commit

_Note: Task 1 followed TDD with separate failing-test and implementation commits._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java` - Vaadin approval decision card with redacted safety context and backend decision plans.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java` - Run-scoped pending approvals container sourced from public API read models.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java` - Separated Admin Governance approval queue/decision surface.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java` - TDD coverage for card safety context, decision paths, renderer wiring, panel rendering, and admin queue behavior.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` - Approval list/decision API path and DTO anchors.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java` - Approval-required event detection and approval DTO mapping from redacted lifecycle payloads.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` - Component-backed event append support for approval cards in the integrated narrative.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/SecurityConfig.java` - Public Vaadin route allowance for the separated admin approval queue.

## Decisions Made

- Approval cards generate `ApprovalDecisionRequest` plans that target the original run-scoped backend endpoint, preventing local-only approval completion.
- Approval-required lifecycle events are treated as approval cards before generic tool lifecycle cards so gated decisions are prominent and actionable.
- The admin queue reuses the same approval component mechanics as the user Console but forces `ADMIN` actor role to preserve the D-12 actor/principal seam.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The TDD red test initially surfaced missing component classes and renderer/stream panel seams as expected.
- Existing unrelated Phase 02/03 planning artifacts remain modified/untracked in the working tree from parallel work and were not touched or committed by this plan.

## Known Stubs

None. Empty maps/strings and null checks in UI code are defensive DTO/path handling and do not render placeholder data that blocks the approval-card goal.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=WebConsoleApprovalCardsTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=WebConsoleApprovalCardsTest,ApprovalControllerTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=WebConsoleApprovalCardsTest,ApprovalControllerTest,CloudServerGovernedToolE2ETest test` — passed

## Next Phase Readiness

- Plan 05-08 can link Admin Governance navigation to `/admin/governance/approvals` alongside inspect-only provider/tool/audit views.
- Plan 05-09 browser E2E can validate approval card display and approve/reject paths through public APIs without real model keys.

## Self-Check: PASSED

- Created files exist: `ApprovalCard.java`, `ApprovalPanel.java`, `AdminApprovalQueueView.java`, `WebConsoleApprovalCardsTest.java`, and this summary are present.
- Task commits exist in recent git history: `479d127`, `654f9e1`, and `59be072`.
- Verification commands passed as listed above.

---
*Phase: 05-agent-web-console-and-runtime-cockpit*
*Completed: 2026-06-15*
