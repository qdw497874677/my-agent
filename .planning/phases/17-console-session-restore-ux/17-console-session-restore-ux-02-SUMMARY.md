---
phase: 17-console-session-restore-ux
plan: 02
subsystem: ui
tags: [vaadin, console, transcript-restore, conversation-read-model, tdd]

requires:
  - phase: 16-conversation-read-model-and-recent-sessions
    provides: Typed ConversationMessageDto roles/statuses/session-run refs for persisted transcript restore.
provides:
  - Formal ChatEventStreamPanel.replaceTranscript(...) typed transcript hydration API.
  - Restored user/assistant primary bubbles with role/session/run/status/stream-state selectors.
  - Compact secondary tool/error transcript cards with abnormal status visibility and metadata redaction discipline.
affects: [18-streaming-bubble-lifecycle, 21-verification-security-and-regression-hardening, console-session-restore-ux]

tech-stack:
  added: []
  patterns:
    - Vaadin component-level transcript hydration from typed DTOs.
    - Stable data-* selector contract for browser and Phase 18 reducer tests.

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java

key-decisions:
  - "Promoted replaceTranscriptForProof(...) to a compatibility delegator over formal replaceTranscript(...)."
  - "Kept tool/error transcript items visible as compact secondary Vaadin cards instead of primary assistant prose."
  - "Mapped data-stream-state directly to persisted message status; live pending/delta/terminal mutation remains Phase 18."

patterns-established:
  - "Restored transcript components expose data-message-role, data-session-id, data-run-id, data-message-status, and data-stream-state."
  - "Completed secondary transcript cards stay visually quiet; failed/cancelled/partial states render visible status chips."
  - "Transcript metadata/redacted details are not dumped into main chat prose."

requirements-completed: [SESS-02, CIA-03, CIA-04]

duration: 9min
completed: 2026-06-29
---

# Phase 17 Plan 02: Typed Transcript Bubble/Card Hydration Summary

**Typed persisted transcript hydration for Vaadin Console with stable bubble/card selectors and secondary tool/error status cards.**

## Performance

- **Duration:** 9 min
- **Started:** 2026-06-29T01:44:54Z
- **Completed:** 2026-06-29T01:53:54Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added `ChatEventStreamPanel.replaceTranscript(...)` as the formal typed transcript hydration API, with `replaceTranscriptForProof(...)` preserved as a delegating compatibility helper.
- Restored typed user/assistant messages as primary chat bubbles in original order and exposed stable role/session/run/status/stream-state selectors for Phase 18 and browser tests.
- Rendered TOOL and ERROR transcript entries as compact secondary cards, keeping operational truth visible without promoting tool/error content to assistant prose.
- Added focused Vaadin component tests covering feed clearing, empty-state restore, selector metadata, bubble alignment, secondary card rendering, abnormal status chips, and metadata non-dumping.

## Task Commits

Each task was committed atomically using TDD commits:

1. **Task 1: Add typed transcript bubble hydration API**
   - `3226ee3` test: add failing transcript hydration tests
   - `5708f4d` feat: add typed transcript hydration API
2. **Task 2: Render tool and error transcript items as compact secondary cards**
   - `7bc9ee5` test: add failing secondary transcript card tests
   - `5e3f424` feat: render secondary transcript cards

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` - Added typed transcript hydration, selector attributes, primary bubbles, and compact secondary tool/error cards.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java` - Added component-level tests for restored transcript bubbles/cards and selector/redaction contracts.

## Decisions Made

- Promoted `replaceTranscriptForProof(...)` by delegation rather than removing it, preserving Phase 16 proof-hook compatibility while establishing `replaceTranscript(...)` as the formal API.
- Used persisted `ConversationMessageStatus.wireValue()` directly for `data-message-status` and `data-stream-state`; live streaming lifecycle mutation is intentionally deferred to Phase 18.
- Counted secondary transcript cards in `eventComponents` to preserve existing component accounting semantics for non-primary feed components.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None. The grep stub scan found only existing i18n placeholder-key usage and null guards in `ChatEventStreamPanel`, not UI-flow stubs or placeholder transcript data.

## Issues Encountered

- During one focused test run, Maven test compilation observed concurrent Plan 01 `SessionListPanel` changes from another parallel executor. This was out of scope for Plan 02 and resolved once the parallel work completed; the final focused Plan 02 verification passed.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleTranscriptHydrationTest test` — passed after Task 1 implementation.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleTranscriptHydrationTest,WebConsoleConversationReadModelHookTest test` — passed after Task 2 implementation; 10 tests, 0 failures.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 17 Plan 03 can call `replaceTranscript(...)` from session-selection orchestration and rely on stable restored-message selectors.
- Phase 18 can build the live streaming reducer on top of `data-message-role`, `data-message-status`, and `data-stream-state` without this plan introducing pending/delta/terminal mutation semantics.

## Self-Check: PASSED

- Found summary file: `.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-02-SUMMARY.md`
- Found implementation file: `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java`
- Found test file: `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java`
- Found task commits in `git log --oneline --all`: `3226ee3`, `5708f4d`, `7bc9ee5`, `5e3f424`

---
*Phase: 17-console-session-restore-ux*
*Completed: 2026-06-29*
