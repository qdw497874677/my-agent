---
phase: 18-streaming-bubble-lifecycle
plan: 03
subsystem: ui
tags: [vaadin, streaming, cancellation, reducer, i18n, redaction]

requires:
  - phase: 18-streaming-bubble-lifecycle-01
    provides: Reducer-owned live assistant bubble contract
  - phase: 18-streaming-bubble-lifecycle-02
    provides: Push/poll event ingestion through the reducer path
provides:
  - Runtime cancellation seam wiring that immediately stops the local stream reducer and assistant bubble
  - Failed/cancelled/partial terminal rendering with safe redacted status cards
  - Synchronized English/Chinese streaming state and mode labels
affects: [phase-18-plan-04, phase-21-verification, console-streaming-ux]

tech-stack:
  added: []
  patterns: [local reducer terminal stop, safe public error summary, bundle-backed stream labels]

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingCancellationTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
    - pi-agent-adapter-web/src/main/resources/messages.properties
    - pi-agent-adapter-web/src/main/resources/messages_zh.properties

key-decisions:
  - "Cancel is enforced locally before the runtime cancel response returns so late provider deltas cannot mutate stopped partial output."
  - "Failure summaries prefer public message/status/category fields and drop raw exception/secret-looking bodies instead of rendering provider payload prose."
  - "Stream state selectors remain language-neutral while visible labels come from synchronized resource bundles."

patterns-established:
  - "ConversationEventReducer.stopRun(...) creates a local terminal key and cancelled terminal operation for immediate UI suppression."
  - "ChatEventStreamPanel renders terminal states as status chips/cards while preserving already-generated assistant text."
  - "Streaming state copy uses console.stream.* keys in both English and Chinese bundles."

requirements-completed: [STRM-03, STRM-05]

duration: 9m48s
completed: 2026-06-30
---

# Phase 18 Plan 03: Cancellation, Failure, and Terminal-State Suppression Summary

**Reducer-enforced cancellation and safe failed terminal rendering for live Console assistant bubbles**

## Performance

- **Duration:** 9m48s
- **Started:** 2026-06-30T02:34:48Z
- **Completed:** 2026-06-30T02:44:36Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments

- Cancelling an active Console run now calls `ConsoleRunExecutionBridge.cancelRun(...)` and also locally marks the active reducer key terminal before the App/runtime response returns.
- Already-generated assistant text remains visible after cancellation, and later `model.delta` events for the same run are ignored by the reducer.
- Provider/model failure events now mark the active assistant bubble failed with a safe status chip/card, avoiding raw exception bodies, secret-looking keys, nested maps, and payload dumps in assistant prose.
- Terminal completion/failure/cancellation semantics preserve visible generated text before changing terminal status.
- English and Chinese bundles now contain synchronized `console.stream.*` state labels and `console.stream.mode.*` labels; selectors remain language-neutral through `data-stream-state` and `data-stream-mode`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Mark local stream stopped before and after cancel response**
   - `d5ebc5d` test: add failing cancellation stream tests
   - `07261ee` feat: stop stream locally on cancel
2. **Task 2: Render failed and partial terminal states safely**
   - `93e550d` test: add failing failure-state tests
   - `30bd8db` feat: render safe failed terminal states
3. **Task 3: Add synchronized streaming labels and selector assertions**
   - `03418ba` test: assert synchronized stream labels

_Note: TDD tasks used separate test and implementation commits where applicable._

## Files Created/Modified

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingCancellationTest.java` - Focused cancellation, failure, post-cancel suppression, terminal flush, and i18n label assertions.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` - Cancel flow now locally stops the reducer/bubble before invoking the runtime cancellation seam.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java` - Added `stopRun(...)`, model/provider error terminal mapping, and safe public summary filtering.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` - Terminal status rendering now uses stream labels and a generic redacted failure summary when raw details are unsafe.
- `pi-agent-adapter-web/src/main/resources/messages.properties` - Added English stream state, failure summary, and mode labels.
- `pi-agent-adapter-web/src/main/resources/messages_zh.properties` - Added Chinese stream state, failure summary, and mode labels.

## Decisions Made

- Cancel is enforced locally before the runtime cancel response returns so late provider deltas cannot mutate stopped partial output.
- Failure summaries prefer public `message`, `reason`, `status`, `errorCategory`, or category fields and drop raw exception/secret-looking bodies instead of rendering provider payload prose.
- Stream state selectors remain language-neutral while visible labels come from synchronized resource bundles.

## Deviations from Plan

None - plan executed as written.

## Issues Encountered

- TDD red phases correctly failed before implementation: missing `ConversationEventReducer.stopRun(...)`, missing model error terminal mapping, and unsafe raw error rendering were observed and fixed in the matching implementation commits.
- Existing working tree had unrelated pre-existing changes (`.gitignore` modified and `.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md` untracked). They were left untouched.

## Known Stubs

None. Stub-pattern scan of the files changed by this plan found no goal-blocking placeholders or mock-only UI data paths.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingCancellationTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingCancellationTest,WebConsoleStreamingBubbleLifecycleTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingCancellationTest,WebConsoleStreamingBubbleLifecycleTest,WebConsoleLiveStreamingPushTest test` — passed, 27 tests

## Next Phase Readiness

- Plan 04 can build fake-runtime/browser verification on stable cancellation/failure selectors and reducer semantics.
- Phase 21 can reuse `WebConsoleStreamingCancellationTest` as the focused Java gate for STRM-03 and STRM-05 regression coverage.

## Self-Check: PASSED

- Found summary file at `.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-03-SUMMARY.md`.
- Found created test file at `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingCancellationTest.java`.
- Verified task commits exist: `d5ebc5d`, `07261ee`, `93e550d`, `30bd8db`, `03418ba`.

---
*Phase: 18-streaming-bubble-lifecycle*
*Completed: 2026-06-30*
