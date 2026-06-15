---
phase: 05-agent-web-console-and-runtime-cockpit
plan: 03
subsystem: api
tags: [approval-api, tool-governance, spring-mvc, app-usecase, vaadin-backend]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: tool.lifecycle events, approval-required gateway outcomes, redacted preview/input summaries
  - phase: 05-agent-web-console-and-runtime-cockpit
    provides: Vaadin/web adapter foundation and Agent Catalog API patterns
provides:
  - Public approval DTO contracts for approval cards and API clients
  - App-layer approval query/command seam over original session/run/tool lifecycle context
  - Session/run scoped REST endpoints to list pending approvals and record approve/reject decisions
affects: [05-07-approval-cards, 05-09-browser-e2e, admin-governance, tool-cards]

tech-stack:
  added: []
  patterns: [client DTO records, thin Spring MVC controller, App use-case seam, event/audit-backed approval decisions]

key-files:
  created:
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/approval/ApprovalDecisionRequest.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/approval/ApprovalDecisionResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/approval/ApprovalSummaryDto.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ApprovalCommandService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ApprovalQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultApprovalService.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/ApprovalController.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ApprovalBeanConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ApprovalControllerTest.java
  modified: []

key-decisions:
  - "Approval IDs are derived from the approval-required lifecycle preview id when present, otherwise the original toolCallId."
  - "Approve/reject decisions are recorded as audit records plus public run events while preserving a future-compatible resume seam."
  - "Phase 5 dev/test actor eligibility allows USER and ADMIN roles through an explicit App-layer role seam."

patterns-established:
  - "Approval read models are built from existing tool.approval_required lifecycle events instead of UI component state."
  - "Approval endpoints remain thin Adapter controllers using SessionController.toRequestContext and App use cases."

requirements-completed: [GUI-06, GUI-08]

duration: 12m 08s
completed: 2026-06-15
---

# Phase 05 Plan 03: Approval DTOs, Service Seam, and Public Approval API Summary

**Backend approval contracts and run-scoped approve/reject API backed by original tool lifecycle events and auditable actor decisions**

## Performance

- **Duration:** 12m 08s
- **Started:** 2026-06-15T05:14:37Z
- **Completed:** 2026-06-15T05:26:45Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Added public `pi-agent-client` approval DTO records for summary cards and decisions.
- Added App-layer approval query/command interfaces plus `DefaultApprovalService` that reads `tool.approval_required` lifecycle events, preserves session/run/tool context, records audit decisions, and emits decision/outcome events.
- Exposed `GET /api/sessions/{sessionId}/runs/{runId}/approvals` and `POST /api/sessions/{sessionId}/runs/{runId}/approvals/{approvalId}/decision` through a thin Spring MVC controller.
- Verified approval summary context, actor/principal/role decision recording, controller JSON contracts, and existing governed-tool E2E/redaction paths.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define approval DTOs and App service seam** - `d6bca5a` (feat)
2. **Task 2: Expose approval list and decide API** - `f78ae73` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/approval/ApprovalDecisionRequest.java` - Public approve/reject request contract with decision and actor role.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/approval/ApprovalDecisionResponse.java` - Public persisted decision response with actor, role, run, session, approval, and tool-call identifiers.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/approval/ApprovalSummaryDto.java` - Public approval card read model with policy, risk/side-effect, preview, redacted argument, consequence, and eligibility fields.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ApprovalQueryService.java` - App read-use-case seam.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ApprovalCommandService.java` - App command-use-case seam.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultApprovalService.java` - Event/audit-backed approval service implementation and future-compatible resume seam.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/ApprovalController.java` - Public session/run scoped approval REST API.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ApprovalBeanConfiguration.java` - Spring composition for approval query/command services.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ApprovalControllerTest.java` - Focused service and controller contract coverage.

## Decisions Made

- Approval identity uses `ProvisionPreview.previewId` when available, with `toolCallId` fallback, so approval cards can bind to original waiting tool calls without inventing UI-only IDs.
- Rejections emit an observable same-run `run.policy_blocked` outcome event in addition to an approval decision event; approvals record the decision and leave full durable resume to the seam for later runtime evolution.
- Actor eligibility is explicit and role-aware (`USER`/`ADMIN`) while staying compatible with current dev/test simulated authentication.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Qualified approval service bean aliases to avoid duplicate Spring candidates**
- **Found during:** Task 2 (Expose approval list and decide API)
- **Issue:** Exposing both `DefaultApprovalService` and interface beans created duplicate candidates in Spring contexts.
- **Fix:** Registered the same service instance under explicit bean aliases and qualified controller injection.
- **Files modified:** `ApprovalBeanConfiguration.java`, `ApprovalController.java`
- **Verification:** `ApprovalControllerTest,CloudServerGovernedToolE2ETest` passed after fix.
- **Committed in:** `f78ae73`

**2. [Rule 3 - Blocking] Removed generated Vaadin frontend artifacts from test runs**
- **Found during:** Verification after Task 2
- **Issue:** Spring/Vaadin test startup generated `pi-agent-adapter-web/src/main/frontend/`, which was unrelated runtime output.
- **Fix:** Removed generated directory and left it uncommitted.
- **Files modified:** none committed
- **Verification:** `git status --short` shows no plan-owned untracked generated frontend files.
- **Committed in:** not applicable

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Fixes were necessary to keep the planned API wired and repository clean; no product scope was added.

## Issues Encountered

- Standalone controller tests require correlation request attributes because `SessionController.toRequestContext` validates trace/correlation/causation IDs. Tests now provide those attributes explicitly.
- Existing unrelated planning artifacts under Phase 02/03 are modified/untracked in the working tree from parallel work and were not touched or committed by this plan.

## Known Stubs

None. Empty strings/maps in approval DTO constructors are null-safety defaults for optional request fields or absent preview summaries, not UI-rendered placeholder data.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ApprovalControllerTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ApprovalControllerTest,CloudServerGovernedToolE2ETest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ApprovalControllerTest,CloudServerGovernedToolE2ETest,GovernedToolSecurityRedactionE2ETest test` — passed

## Next Phase Readiness

- Phase 5 approval cards can now consume a public approval list API and post approve/reject decisions without touching Vaadin component state.
- Admin/user approval UX can display tool, policy, risk/side-effect, preview, redacted arguments, consequences, and actor eligibility from public DTOs.
- Full durable suspended-run resume remains behind the App service seam and can be expanded without changing the public approval API shape.

## Self-Check: PASSED

- Created files exist: all 9 key files listed above are present.
- Task commits exist: `d6bca5a` and `f78ae73` are present in git history.
- Verification commands passed as listed above.

---
*Phase: 05-agent-web-console-and-runtime-cockpit*
*Completed: 2026-06-15*
