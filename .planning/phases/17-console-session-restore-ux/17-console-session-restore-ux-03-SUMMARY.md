---
phase: 17-console-session-restore-ux
plan: 03
subsystem: ui
tags: [vaadin, console, session-restore, transcript, continuation]

requires:
  - phase: 17-console-session-restore-ux-01
    provides: Compact recent session cards backed by SessionSummaryDto.
  - phase: 17-console-session-restore-ux-02
    provides: Typed transcript hydration API and stable message selectors.
provides:
  - Active-session banner distinguishing new conversations from selected historical sessions.
  - New Conversation escape action that clears continuation state and de-highlights history cards.
  - ConsoleView selection orchestration for typed transcript restore, active run cursor restore, selected-card highlight, and return-to-chat behavior.
  - Same-session send proof that follow-up messages reuse selectedSessionId while reset sends create a fresh session.
affects: [phase-18-streaming-bubble-lifecycle, phase-19-multi-turn-runtime-context, console-ui-tests]

tech-stack:
  added: []
  patterns: [Vaadin component state orchestration, bridge-backed typed transcript restore, TDD component tests]

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java

key-decisions:
  - "Use adapter-web ConsoleView state for active session identity while preserving App/read-model access through ConsoleRunExecutionBridge."
  - "Treat New Conversation as the explicit reset path for selected-session continuation."
  - "Restore activeRunId and numeric transcript nextCursor only as a Phase 17 polling cursor seam; full delta aggregation remains Phase 18."

patterns-established:
  - "Active-session banner exposes data-role=active-session-banner and data-active-session-state=new|continued."
  - "Session selection loads typed ConversationTranscriptResponse through the bridge, hydrates ChatEventStreamPanel.replaceTranscript(...), then returns to chat."
  - "Continuation sends use selectedSessionId; only null selectedSessionId triggers createSession()."

requirements-completed: [CIA-02, SESS-03]

duration: 9min
completed: 2026-06-29
---

# Phase 17 Plan 03: Active Session Restore UX Summary

**Vaadin Console active-session identity, typed transcript restore orchestration, and selected-session continuation semantics.**

## Performance

- **Duration:** 9min
- **Started:** 2026-06-29T01:59:07Z
- **Completed:** 2026-06-29T02:07:48Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- Added a compact active-session banner with stable selectors showing `New conversation` vs `Continue: {stable title}`.
- Added an explicit `data-action="new-conversation"` reset action that clears selected-session continuation, clears the feed, de-highlights session cards, and returns to Chat.
- Wired `selectSession(...)` to load typed transcripts through `ConsoleRunExecutionBridge.getTranscript(...)`, hydrate `ChatEventStreamPanel.replaceTranscript(...)`, highlight the selected card, restore active run/cursor state, and return mobile users to Chat.
- Proved follow-up sends after restore call `createRun(selectedSessionId, request)` without `createSession()`, while sends after New Conversation create a fresh session.

## Task Commits

1. **Task 1 RED: Active-session banner behavior tests** - `8dd06e6` (test)
2. **Task 1 GREEN: Active-session banner and New Conversation action** - `485b6df` (feat)
3. **Task 2 RED/GREEN coverage: transcript restore orchestration** - `16dfdac` (test; implementation already covered by `485b6df`)
4. **Task 3 RED/GREEN coverage: selected-session continuation sends** - `20422c9` (test; implementation already covered by `485b6df`)

_Note: TDD tasks produced separate test and implementation commits; Task 2/3 implementation was included in the Task 1 feature commit because the banner reset, selection orchestration, and continuation state share one ConsoleView state machine._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` - Added active-session banner, reset action, typed restore orchestration, active run cursor restore, and selected-session continuation updates.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java` - Added `clearSelection()` so New Conversation can de-highlight cards without dropping recent history.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java` - Added component tests for banner states, reset action, restore sequencing, active cursor restore, and same-session send semantics.

## Decisions Made

- Kept active-session identity in adapter-web Vaadin state and used `ConsoleRunExecutionBridge` for read-model access, preserving COLA boundaries.
- Used the Phase 16 stable `SessionSummaryDto.title()` cache for the continued banner, falling back to the selected session id only if no title is known.
- Parsed transcript `nextCursor` as a numeric polling cursor when possible and ignored non-numeric cursors, leaving full stream replay/delta aggregation to Phase 18.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added explicit session-list de-selection API**
- **Found during:** Task 1 (New Conversation escape)
- **Issue:** `SessionListPanel` had `selectSession(...)` but no formal way to clear active-card state while preserving recent cards.
- **Fix:** Added `clearSelection()` and used it from `ConsoleView.startNewConversation()`.
- **Files modified:** `SessionListPanel.java`, `ConsoleView.java`
- **Verification:** `WebConsoleSessionRestoreUxTest.newConversationActionClearsContinuationAndReturnsBannerToNewState`
- **Committed in:** `485b6df`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Required for the planned New Conversation escape action; no architectural or feature scope expansion.

## Issues Encountered

- Focused restore tests passed.
- The plan's broader mobile-flow command was also run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest,WebConsoleMobileFlowContractTest test`. `WebConsoleSessionRestoreUxTest` passed, but `WebConsoleMobileFlowContractTest` still has unrelated pre-existing selector/i18n/run-event expectations from the current chat-home Console shape (for example direct-child panel switcher lookups and missing test translations). These failures are outside this plan's restore/continuation changes and match the Phase 17 context warning to avoid broad mobile-flow cleanup unless directly blocking restore UX.

## Verification

- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest test`
- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest,WebConsoleConversationReadModelHookTest test`
- ⚠️ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest,WebConsoleMobileFlowContractTest test` — restore tests passed; pre-existing mobile-flow contract failures remain deferred.

## Known Stubs

None. Null/empty handling in modified files is defensive UI state handling, not placeholder data flowing to the user as a stub.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 18 can build streaming bubble lifecycle on top of restored `activeRunId` and `activeRunNextAfterSequence` state.
- Phase 19 can rely on selected-session send semantics being explicit in ConsoleView, but must still implement actual App/runtime multi-turn context assembly.
- Plan 04 should handle i18n/browser selector documentation and can decide whether to update legacy mobile component tests to the current chat-home DOM shape.

## Self-Check: PASSED

- Found summary file: `.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-03-SUMMARY.md`
- Found created test file: `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java`
- Found modified implementation files: `ConsoleView.java`, `SessionListPanel.java`
- Found task commits: `8dd06e6`, `485b6df`, `16dfdac`, `20422c9`

---
*Phase: 17-console-session-restore-ux*
*Completed: 2026-06-29*
