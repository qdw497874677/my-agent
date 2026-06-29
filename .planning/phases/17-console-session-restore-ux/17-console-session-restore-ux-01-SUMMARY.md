---
phase: 17-console-session-restore-ux
plan: 01
subsystem: ui
tags: [vaadin, console, recent-sessions, session-restore, component-tests]

requires:
  - phase: 16-conversation-read-model-and-recent-sessions
    provides: SessionSummaryDto recent-session read model for Console history cards
provides:
  - Formal SessionListPanel.showRecentSessions API consuming SessionSummaryDto
  - Compact recent-session cards with title, preview, last activity, and status/active-run status selectors
  - Deterministic selected-state and activation contracts for downstream restore orchestration
affects: [phase-17-console-session-restore-ux, phase-20-provider-model-and-local-profile-stability, phase-21-verification-security-and-regression-hardening]

tech-stack:
  added: []
  patterns: [Vaadin adapter-web component contract, stable data-* selector testing, DTO-backed compact history cards]

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionListPanelTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
    - pi-agent-adapter-web/src/main/resources/messages.properties
    - pi-agent-adapter-web/src/main/resources/messages_zh.properties

key-decisions:
  - "Recent history cards consume SessionSummaryDto directly in adapter-web and do not render provider/model metadata by default."
  - "The hasMore signal is represented by a lightweight data-role=session-more marker only, deferring full management/search UX."
  - "Selecting a session outside the bounded list creates a minimal selected card so later restore orchestration can highlight historical selections deterministically."

patterns-established:
  - "SessionListPanel.showRecentSessions(List<SessionSummaryDto>, selectedSessionId, hasMore) is the formal compact history API."
  - "Session card browser/component contracts use data-role=session-card plus data-session-id, data-session-active, and data-field selectors."
  - "Active run status is presentation-prioritized over summary status, while provider/model metadata remains hidden from compact history cards."

requirements-completed: [CIA-01]

duration: 9min
completed: 2026-06-29
---

# Phase 17 Plan 01: Compact Recent-History Card Surface Summary

**DTO-backed Vaadin recent-session history cards with stable selectors, selected-state highlighting, preview/status fields, and no provider/model management creep**

## Performance

- **Duration:** 9min
- **Started:** 2026-06-29T01:44:56Z
- **Completed:** 2026-06-29T01:54:08Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Promoted `SessionListPanel` from the proof-only recent session hook to a formal `showRecentSessions(...)` API backed by `SessionSummaryDto`.
- Rendered compact cards with stable selectors for title, preview, last activity, and status, with `activeRunStatus` taking precedence when present.
- Preserved click, Enter, and Space activation semantics while making selected-state deterministic, including the case where the selected session is outside the bounded recent list.
- Added focused component contract tests for bounded rendering, selector coverage, empty state, metadata exclusion, activation callbacks, and selection behavior.

## Task Commits

Each task was committed atomically:

1. **Task 1: Formalize bounded recent-session card rendering**
   - `eecb80c` (test): add failing recent-history card contracts
   - `f4c2502` (feat): formalize `showRecentSessions(...)` and compact cards
2. **Task 2: Preserve activation and selected-state semantics**
   - `672be6b` (test): cover selected-session activation semantics

**Plan metadata:** pending final docs commit

_Note: Task 2's added contracts passed against the Task 1 implementation because the selection/activation hardening was implemented while satisfying Task 1's selected-card requirements._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java` - Adds formal `showRecentSessions(...)`, compact card rendering, preview/status selectors, `hasMore` marker, and minimal selected-card fallback.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionListPanelTest.java` - Adds fast component contracts for recent history card ordering, selectors, metadata exclusion, empty state, and activation/selection behavior.
- `pi-agent-adapter-web/src/main/resources/messages.properties` - Adds English fallback labels for preview and lightweight more marker.
- `pi-agent-adapter-web/src/main/resources/messages_zh.properties` - Adds Chinese fallback labels for preview and lightweight more marker.

## Decisions Made

- Recent history cards render only Phase 16 summary fields: title, latest preview, last activity, and status/active-run status.
- Provider/model metadata is intentionally not rendered in compact cards, preserving the Phase 17/20 boundary.
- The `hasMore` flag is intentionally lightweight (`data-role="session-more"`) and does not introduce search, rename, archive, pin, delete, or management controls.
- `showRecentSessionsForProof(...)` remains as a compatibility helper delegating to the formal API.

## Deviations from Plan

None - plan scope was executed as written.

## Issues Encountered

- Full `WebConsoleMobileFlowContractTest` currently has unrelated pre-existing failures when run as a complete class in this environment (missing translation/test bootstrap and unrelated run-event expectations). The session-history/activation-focused mobile methods required by this plan pass and broad cleanup is outside Phase 17 Plan 01 scope per Phase 17 D-20.
- Task 2's TDD RED step did not fail because Task 1's implementation already satisfied the added activation/selection contracts. The focused verification was still run and committed as a task-specific contract.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionListPanelTest test` — passed, 7 tests.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionListPanelTest,WebConsoleMobileFlowContractTest#sessionHistoryRendersCardMetadataAndActiveIdentity+mcon04SessionCardActivatesByClickEnterAndSpaceAndReturnsToChat+mcon04SuccessfulSendAddsVisibleActiveSessionCardWithPromptTitle+mcon04ActivatingCreatedSessionReturnsToChatAndNextSendContinuesSameSession test` — passed, 11 tests.

## Known Stubs

None found in files created/modified for this plan.

## Next Phase Readiness

- Console restore orchestration can now call `showRecentSessions(...)` with Phase 16 summaries and rely on deterministic selected-card highlighting.
- Downstream transcript hydration and active-session banner work can use the stable session-card selectors and activation helper contracts.

## Self-Check: PASSED

- Found `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java`.
- Found `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionListPanelTest.java`.
- Found `.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-01-SUMMARY.md`.
- Verified task commits exist: `eecb80c`, `f4c2502`, `672be6b`.

---
*Phase: 17-console-session-restore-ux*
*Completed: 2026-06-29*
