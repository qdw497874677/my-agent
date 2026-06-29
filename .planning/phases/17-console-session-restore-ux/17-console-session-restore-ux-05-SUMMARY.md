---
phase: 17-console-session-restore-ux
plan: 05
subsystem: ui
tags: [vaadin, console, session-restore, i18n, transcript]
requires:
  - phase: 17-console-session-restore-ux-04
    provides: Browser restore selector/docs contract and final restore UX handoff gaps
provides:
  - Visible Console history/session/details panel controls
  - ChatEventStreamPanel direct-construction i18n fallback for abnormal transcript statuses
  - Focused Java proofs for visible restore controls and transcript status labels
affects: [phase-17-console-session-restore-ux, phase-18-streaming-bubble-lifecycle, phase-21-regression-hardening]
tech-stack:
  added: []
  patterns: [Vaadin visible panel switcher, ResourceBundle fallback for direct component construction]
key-files:
  created: []
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java
key-decisions:
  - "Expose the existing Console panel switcher as the visible history/details affordance instead of adding routes or deferred conversation-management features."
  - "Normalize abnormal transcript status fallback labels to lowercase for selector/test stability while retaining bundle-backed copy."
patterns-established:
  - "Console panel wrappers remain secondary, but user-reachable through visible data-action=show-console-panel controls."
  - "Adapter-web Vaadin components should use ResourceBundle fallback when direct tests lack an i18n provider."
requirements-completed: [CIA-01, CIA-04, SESS-02]
duration: 5m15s
completed: 2026-06-29
---

# Phase 17 Plan 05: Visible Console restore affordances Summary

**Visible Console recent-history/details controls with direct-construction transcript status i18n fallback**

## Performance

- **Duration:** 5m15s
- **Started:** 2026-06-29T04:12:48Z
- **Completed:** 2026-06-29T04:18:03Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Made the existing Console panel switcher visible in the real route so users can reach Chat, Agents, History/Sessions, and Run Details without hidden-only test paths.
- Kept Chat primary while exposing `sessions` and `run-context` panels through stable `data-action="show-console-panel"` and `data-console-target` selectors.
- Added `ChatEventStreamPanel` bundle fallback so failed/cancelled/partial secondary transcript cards render readable labels without Vaadin missing-key markers.
- Added regression tests proving visible controls, recent session card opening, details reachability, and abnormal transcript status fallback.

## Task Commits

Each task was committed atomically:

1. **Task 1: Expose visible history and advanced-details affordances in ConsoleView** - `176857a` (feat)
2. **Task 2: Add ChatEventStreamPanel i18n fallback for abnormal transcript statuses** - `cb52a66` (fix)

**Plan metadata:** pending final docs commit

_Note: TDD tasks used red/green locally; only the final task implementations were committed because the plan executor commit protocol requires completed tasks to be committed after verification._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` - Renders the panel switcher visibly and keeps advanced panel wrappers available behind user controls.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` - Adds ResourceBundle fallback for direct construction and abnormal transcript status labels.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java` - Proves history/details controls are visible and open user-reachable panels.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java` - Proves abnormal secondary card statuses render without missing-key markers or raw metadata.

## Decisions Made

- Exposed the existing selector-compatible Console switcher instead of introducing new routes, dialogs, or a deferred conversation-management surface.
- Lowercased abnormal transcript status labels after bundle fallback so existing tests and browser selectors can assert stable `failed`, `cancelled`, and `partial` text independent of title-case bundle copy.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None. Stub-pattern scan found only legitimate null checks/default handling and the chat placeholder resource usage; no UI-blocking stubs were introduced.

## Issues Encountered

- Direct Vaadin construction produced `!{console.session.status.*}!` markers for transcript status labels; resolved with local ResourceBundle fallback in `ChatEventStreamPanel`.
- Existing bundle copy was title-cased (`Failed`), while tests expect lowercase status terms; normalized the chip labels to lowercase after fallback.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest test` — passed, 11 tests.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleTranscriptHydrationTest test` — passed, 9 tests.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest,WebConsoleTranscriptHydrationTest test` — passed, 20 tests.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 06 can now realign the Playwright restore path to visible `session-card`, History/Sessions, and Run Details controls.
- Phase 18 streaming aggregation, Phase 19 context assembly, Phase 20 provider stability, and future session search/rename/archive/pin/delete remain out of scope.

## Self-Check: PASSED

- Found modified files: ConsoleView, ChatEventStreamPanel, WebConsoleSessionRestoreUxTest, WebConsoleTranscriptHydrationTest.
- Found task commits: `176857a`, `cb52a66`.

---
*Phase: 17-console-session-restore-ux*
*Completed: 2026-06-29*
