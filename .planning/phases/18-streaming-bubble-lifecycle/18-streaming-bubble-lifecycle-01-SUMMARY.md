---
phase: 18-streaming-bubble-lifecycle
plan: 01
subsystem: ui
tags: [vaadin, streaming, reducer, conversation, tdd]

requires:
  - phase: 17-console-session-restore-ux
    provides: Typed transcript restoration and ChatEventStreamPanel message selector baseline
provides:
  - Idempotent ConversationEventReducer for RunEventDto-to-chat operations
  - ChatEventStreamPanel live assistant bubble API with stable stream selectors
  - Focused component/reducer coverage for pending, delta, dedupe, terminal, and secondary-card routing
affects: [phase-18-streaming-transport, phase-19-context, console-view, run-event-rendering]

tech-stack:
  added: []
  patterns: [typed reducer operations, stable aggregation key, TDD red-green task commits]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingBubbleLifecycleTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java

key-decisions:
  - "Reducer operations, not raw RunEventRenderer assistant text, own primary assistant streaming semantics."
  - "Secondary runtime/tool/approval/policy events remain compact cards and are not added to primary assistant prose during reducer application."
  - "Live assistant bubbles are keyed by sessionId/runId/stepId-or-default and clear on transcript replacement or new user messages."

patterns-established:
  - "ConversationEventReducer: eventId and per-run sequence dedupe before UI mutation."
  - "ChatEventStreamPanel live bubble API: begin, append delta, terminal/error mutation on a keyed existing bubble."

requirements-completed: [STRM-01, STRM-02, STRM-03, STRM-04]

duration: 12m33s
completed: 2026-06-30
---

# Phase 18 Plan 01: Reducer and Live Bubble Contract Summary

**Idempotent RunEventDto reducer plus keyed Vaadin assistant bubbles that aggregate deltas, dedupe replays, and keep runtime cards out of assistant prose**

## Performance

- **Duration:** 12m33s
- **Started:** 2026-06-30T01:58:59Z
- **Completed:** 2026-06-30T02:11:32Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- Added formal live assistant APIs to `ChatEventStreamPanel`: `beginAssistantMessage`, `appendAssistantDelta`, `markAssistantTerminal`, and `showErrorBubble`.
- Added stable selectors for live assistant bubbles including role, kind, session/run/step IDs, stream state, message status, and aggregation key.
- Added `ConversationEventReducer` to convert `RunEventDto` into typed operations with eventId/sequence dedupe, terminal-state suppression, and secondary-card routing.
- Added reducer-to-panel application support proving one primary assistant bubble can coexist with secondary tool/runtime cards without polluting assistant text.
- Added focused component/reducer tests for pending, delta append, dedupe, terminal state mutation, secondary routing, safe failed summaries, and cancelled-late-delta suppression.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add live assistant bubble API to ChatEventStreamPanel**
   - `0381c3c` test: add failing live bubble tests
   - `bc6f7cf` feat: add live assistant bubble API
2. **Task 2: Create idempotent ConversationEventReducer**
   - `dbaf677` test: add failing reducer tests
   - `8aab9b3` feat: add idempotent conversation reducer
3. **Task 3: Apply reducer operations to panel in test harness**
   - `56b6389` test: add failing reducer-to-panel tests
   - `289dbd1` feat: apply reducer operations to chat panel

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java` — Stateless UI-facing reducer state holder that maps run events to typed chat operations with replay dedupe and terminal suppression.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` — Adds keyed live assistant bubble lifecycle methods and secondary-event append support for reducer application.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingBubbleLifecycleTest.java` — Covers pending bubble creation, same-bubble delta aggregation, reducer dedupe/routing, terminal mutation, safe errors, and cancellation behavior.

## Decisions Made

- Reducer operations own primary assistant streaming semantics; `RunEventRenderer` is reused only for secondary events/cards.
- Live assistant aggregation uses `sessionId::runId::stepId-or-default` to keep downstream Push/SSE/poll replay paths stable.
- `appendSecondaryEvent(...)` intentionally bypasses `messages()` so operational cards do not become primary conversation prose.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The first live-delta implementation trimmed leading spaces from deltas through `requireText(...)`, which collapsed `"Hello" + " world"` into `"Helloworld"`. Fixed within Task 1 by preserving non-blank delta text exactly.
- Existing unrelated working-tree changes were present at start (`.gitignore`, `.planning/STATE.md`, and Phase 17 verification doc). They were left untouched and not included in task commits.

## Known Stubs

None. Stub-pattern scan found null/default guards and translation placeholders used as existing defensive code, not unresolved UI stubs that prevent the plan goal.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingBubbleLifecycleTest test` — passed after each GREEN step.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingBubbleLifecycleTest,WebConsoleTranscriptHydrationTest test` — passed, 19 tests.

## Next Phase Readiness

- Phase 18 transport plans can call the reducer and apply operations into `ChatEventStreamPanel` without relying on `activeAssistantLine` heuristics.
- Console wiring remains intentionally deferred to the Push/SSE/poll transport plans to avoid shared-file conflicts.

## Self-Check: PASSED

- Found created files: `ConversationEventReducer.java`, `WebConsoleStreamingBubbleLifecycleTest.java`.
- Found modified file: `ChatEventStreamPanel.java`.
- Found task commits: `0381c3c`, `bc6f7cf`, `dbaf677`, `8aab9b3`, `56b6389`, `289dbd1`.
- Final focused verification passed.

---
*Phase: 18-streaming-bubble-lifecycle*
*Completed: 2026-06-30*
