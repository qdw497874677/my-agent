---
phase: 19-multi-turn-runtime-context
plan: 01
subsystem: app-runtime-context
tags: [java, app-layer, conversation-context, session-context, cola, tdd]

requires:
  - phase: 16-conversation-read-model-and-recent-sessions
    provides: Typed conversation transcript DTOs and ownership-safe ConversationQueryService
  - phase: 18-streaming-bubble-lifecycle
    provides: Runtime distinction between user-visible assistant prose and diagnostic/tool events
provides:
  - App-layer ConversationContextPolicy with conservative configurable context budgets
  - App-layer ConversationContextMetadata for included/dropped/excluded/truncated observability
  - App-layer ConversationContextAssembler that converts typed transcripts into safe SessionContext message history
affects: [phase-19-dispatch-injection, phase-19-provider-message-boundary, phase-21-security-verification]

tech-stack:
  added: []
  patterns: [plain-java-app-usecase, typed-transcript-to-domain-message-assembly, newest-history-budgeting]

key-files:
  created:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextPolicy.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextMetadata.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationContextAssemblerTest.java
  modified: []

key-decisions:
  - "Keep multi-turn context selection and budgeting in App usecase code, not Vaadin, provider adapters, JDBC, or infrastructure."
  - "Represent prior context with existing Domain SessionEntryPayload.MessageEntry role/content values."
  - "Drop unsafe history entries instead of inserting redaction placeholders into model context."

patterns-established:
  - "ConversationContextAssembler loads transcript history only through ConversationQueryService and records exclusion/drop metadata."
  - "Context budgets retain newest eligible history and preserve chronological order among included messages."

requirements-completed: [CTX-01, CTX-02, CTX-03, CTX-05]

duration: 8m23s
completed: 2026-07-01
---

# Phase 19 Plan 01: App Context Policy and Safe Transcript Assembler Summary

**Bounded App-layer transcript context assembly converts selected-session prior user/assistant turns into safe Domain MessageEntry history with truncation metadata.**

## Performance

- **Duration:** 8m23s
- **Started:** 2026-07-01T13:55:17Z
- **Completed:** 2026-07-01T14:03:40Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added `ConversationContextPolicy` with conservative defaults (`12` recent messages, `12000` characters, transcript fetch limit `48`) and constructor validation for configurable budgets.
- Added `ConversationContextMetadata` so downstream runtime/dispatch work can observe included, dropped, excluded, character-budget, resulting-character, and truncation state.
- Added `ConversationContextAssembler` that loads typed transcript entries via `ConversationQueryService.getTranscript(...)`, filters to safe prior `user`/`assistant` messages, excludes current-run input, and emits `SessionEntryPayload.MessageEntry` values in chronological order.
- Covered the contract with deterministic focused App tests for policy validation, metadata, safe filtering, current-run exclusion, newest-history retention, character-budget dropping, and architecture gate compatibility.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define context policy and metadata contracts** - `9217f0a` (test)
2. **Task 2: Implement safe transcript-to-message context assembly** - `2f91ca0` (feat)

**Plan metadata:** pending final docs commit

_Note: Both tasks were TDD tasks; Task 1 used a RED test run before adding the contracts, and Task 2 used a RED test run before adding the assembler._

## Files Created/Modified

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextPolicy.java` - Plain-Java context budget policy with defaults and validation.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextMetadata.java` - Plain-Java metadata record for included/dropped/excluded/truncated context observability.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java` - App-layer transcript-to-`MessageEntry` assembler with safety filtering and newest-history budgeting.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationContextAssemblerTest.java` - Focused contract tests for policy, metadata, filtering, current-run exclusion, and truncation behavior.

## Decisions Made

- Kept Phase 19 Plan 01 context assembly entirely in App usecase code so later dispatch/provider plans can call it without moving context business rules into Vaadin, persistence, Spring AI, or provider adapters.
- Used existing `SessionEntryPayload.MessageEntry(role, content)` as the output carrier to align with the planned `SessionContext.messages` seam.
- Treated redacted/invisible/tool/error/diagnostic/provider/policy/credential-like transcript entries as excluded, not transformed into `[redacted]` placeholders, so unsafe historical material does not enter model context.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added sensitive text exclusion for context safety**
- **Found during:** Task 2 (Implement safe transcript-to-message context assembly)
- **Issue:** The initial assembler implementation filtered redacted flags and diagnostic metadata but would still include a visible assistant message containing obvious sensitive marker text such as `secret`.
- **Fix:** Added conservative sensitive-text checks for redaction placeholders and common credential markers (`secret`, `token`, `password`, `api_key`, `apikey`, `credential`).
- **Files modified:** `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app -Dtest=ConversationContextAssemblerTest test`
- **Committed in:** `2f91ca0` (part of Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 missing critical safety filter)
**Impact on plan:** The fix was required for CTX-03 safety and stayed within the planned App-layer context assembler scope.

## Issues Encountered

- The Task 2 RED test initially also referenced a nonexistent `RequestContext.system()` helper; the test was corrected to construct an explicit deterministic `RequestContext` using existing App context records before implementing the assembler.
- Existing unrelated working-tree changes were present before execution (`.gitignore`, `.planning/STATE.md`, `.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md`). They were not staged in task commits.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app -Dtest=ConversationContextAssemblerTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app -Dtest=ConversationContextAssemblerTest,AppDependencyArchTest test` — passed.

## Next Phase Readiness

- Plan 02 can inject the assembler result into dispatch-time `SessionContext.messages` and wire metadata to runtime observability.
- Plan 03 can rely on ordered `MessageEntry` history as the provider-neutral input before appending the current user prompt exactly once.
- Plan 04 should add fake-model proof for CTX-04 and broaden safety/architecture gates around the dispatch/provider integration.

## Self-Check: PASSED

- Created files verified present: `ConversationContextPolicy.java`, `ConversationContextMetadata.java`, `ConversationContextAssembler.java`, `ConversationContextAssemblerTest.java`, and this summary.
- Task commits verified present in recent git history: `9217f0a`, `2f91ca0`.

---
*Phase: 19-multi-turn-runtime-context*
*Completed: 2026-07-01*
