---
phase: 18-streaming-bubble-lifecycle
plan: 02
subsystem: ui
tags: [vaadin, push, streaming, reducer, console, tdd]

requires:
  - phase: 18-streaming-bubble-lifecycle
    plan: 01
    provides: ConversationEventReducer and ChatEventStreamPanel live assistant bubble API
provides:
  - Vaadin Push-enabled application shell for Console streaming
  - ConsoleLiveRunEventSubscriber bridge from SseRunEventFanout to UI.access(...)
  - Console stream-mode selectors distinguishing push from polling-fallback
  - Replay-before-subscribe Console wiring with shared reducer/dedupe state
affects: [console-view, streaming-transport, run-event-fanout, vaadin-push]

tech-stack:
  added: []
  patterns: [Vaadin Push, UI.access bridge, replay-before-subscribe, reducer-shared live and fallback paths]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleLiveRunEventSubscriber.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleLiveStreamingPushTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiWebAppShell.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java

key-decisions:
  - "Vaadin Push with UI.access(...) over SseRunEventFanout is the Console product streaming path; polling is explicitly labeled polling-fallback."
  - "Console replays persisted run events before subscribing to live fanout events, sharing one reducer/dedupe cursor."
  - "Run submission creates a reducer-owned pending assistant bubble immediately after run identity exists."

patterns-established:
  - "ConsoleLiveRunEventSubscriber owns fanout subscription lifecycle and closes on detach, terminal event, or explicit close."
  - "ConsoleView exposes data-stream-mode=push when a live subscriber is available and polling-fallback otherwise."
  - "Live and fallback events both enter ConversationEventReducer before mutating ChatEventStreamPanel."

requirements-completed: [STRM-01, STRM-02, STRM-04]

duration: 15m14s
completed: 2026-06-30
---

# Phase 18 Plan 02: Vaadin Push Live Streaming Wiring Summary

**Vaadin Push live run-event fanout now drives one reducer-owned assistant bubble, with polling kept as an explicit fallback mode**

## Performance

- **Duration:** 15m14s
- **Started:** 2026-06-30T02:15:23Z
- **Completed:** 2026-06-30T02:30:37Z
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments

- Enabled Vaadin Push on `PiWebAppShell` with `@Push`, making the product path server-driven instead of only poll-backed.
- Added `ConsoleLiveRunEventSubscriber`, an adapter-web bridge that subscribes to `SseRunEventFanout`, dispatches callbacks through `UI.access(...)`, and unregisters on detach, terminal event, or explicit close.
- Updated `ConsoleView` to expose `data-stream-mode="push"` when live subscription support is available and `data-stream-mode="polling-fallback"` otherwise.
- Updated run submission to append the user message, create/reuse session, create the run, immediately begin a pending assistant bubble once the run id exists, replay persisted events, and then subscribe to live events.
- Routed live fanout events and fallback `refreshActiveRunEvents()` events through the same `ConversationEventReducer` and cursor/dedupe state.
- Preserved public SSE metadata through `EventStreamClient.runEventStream(...)` while ensuring Vaadin live updates do not rely on browser-side SSE.

## Task Commits

Each task was committed atomically:

1. **Task 1: Enable Vaadin Push and add live subscriber bridge**
   - `a833abb` test: add failing live push tests
   - `824f6a2` feat: enable push live subscriber bridge
2. **Task 2: Wire Console submission to pending bubble and live stream mode**
   - `5556747` test: add failing console live mode tests
   - `df3e58a` feat: wire console push stream mode
3. **Task 3: Route live and fallback events through the reducer**
   - `dff4783` test: add failing reducer route tests
   - `55ce56e` feat: route live and fallback through reducer

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiWebAppShell.java` — Adds Vaadin `@Push` shell configuration.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleLiveRunEventSubscriber.java` — New Push-safe fanout-to-UI bridge with lifecycle cleanup.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` — Wires stream mode, pending bubble creation, replay-before-subscribe, live subscription, and shared reducer application.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java` — Adds a formal begin operation so pending bubble creation uses the reducer contract.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleLiveStreamingPushTest.java` — Focused Push/live subscription/Console wiring tests covering app shell, UI.access, cleanup, stream modes, replay-before-live, reducer dedupe, and secondary cards.

## Decisions Made

- Vaadin Push over `SseRunEventFanout` is treated as the product streaming path for Console UI updates; the old polling loop remains a fallback/test seam and is explicitly labeled.
- Replay is applied before live subscription so persisted events establish reducer state and live callbacks only append later events.
- Pending assistant bubble creation is now expressed as a reducer operation to keep initial, live, and fallback mutations on one contract.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Aligned pending bubble aggregation key with live fake/runtime step IDs**
- **Found during:** Task 3
- **Issue:** Console initially began the pending bubble with a default step key while live test/runtime events carried `step-1`, producing a second assistant bubble for the same run.
- **Fix:** Introduced a Console default assistant step id and reducer begin operation so pending and later events share the same aggregation key.
- **Files modified:** `ConsoleView.java`, `ConversationEventReducer.java`
- **Commit:** `55ce56e`

## Issues Encountered

- Existing unrelated working-tree changes were present at start (`.gitignore` and `.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md`). They were left untouched and not included in task commits.
- Test helper domain-event construction required using existing domain enum/payload/redaction contracts; this was corrected inside test development and did not affect production code.

## Known Stubs

None. Stub-pattern scan found null/default guards, existing translated placeholder keys, and defensive fallbacks; no unresolved UI stubs prevent this plan's streaming path goal.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleLiveStreamingPushTest test` — passed, 9 tests.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleLiveStreamingPushTest,WebConsoleSessionRestoreUxTest test` — passed, 17 tests.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleLiveStreamingPushTest,WebConsoleStreamingBubbleLifecycleTest,WebConsoleSessionRestoreUxTest test` — passed, 30 tests.

## Next Phase Readiness

- Phase 18 terminal/cancellation plans can build on the live subscription bridge and shared reducer state instead of reintroducing poll-only streaming semantics.
- `data-stream-mode` is available for browser/component tests to distinguish Push from fallback polling.

## Self-Check: PASSED

- Found created files: `ConsoleLiveRunEventSubscriber.java`, `WebConsoleLiveStreamingPushTest.java`.
- Found modified files: `PiWebAppShell.java`, `ConsoleView.java`, `ConversationEventReducer.java`.
- Found task commits: `a833abb`, `824f6a2`, `5556747`, `df3e58a`, `dff4783`, `55ce56e`.
- Final focused verification passed with 30 tests.

---
*Phase: 18-streaming-bubble-lifecycle*  
*Completed: 2026-06-30*
