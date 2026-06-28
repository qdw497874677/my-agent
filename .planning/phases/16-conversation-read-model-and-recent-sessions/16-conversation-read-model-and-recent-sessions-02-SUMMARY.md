---
phase: 16-conversation-read-model-and-recent-sessions
plan: 02
subsystem: app
tags: [java, app, usecase, conversation-read-model, transcript-assembler, ownership-aware-ports, archunit]

# Dependency graph
requires:
  - "16-conversation-read-model-and-recent-sessions-01 (Plan 01 typed DTO contracts in pi-agent-client)"
provides:
  - "App ConversationQueryService boundary for recent sessions (SESS-01) and typed transcripts (SESS-04)"
  - "DefaultConversationQueryService orchestrating ownership-safe ports + ConversationTranscriptAssembler"
  - "Canonical ConversationTranscriptAssembler folding run input + model/tool/error/cancel events into typed DTOs"
  - "Ownership-aware repository port methods carrying RequestContext + session/run filters (D-15)"
  - "ConversationRunView App-internal orchestration read model (run identity/createdAt/input/status)"
  - "Golden App tests covering user/assistant/tool/error/cancelled ordering + redaction (D-16)"
  - "Additive ArchUnit rule locking conversation boundary from Spring/Vaadin/JDBC/SQLite/infra/adapter"
affects:
  - "Phase 16 plan 03 (Infrastructure JDBC ownership-aware implementations of listRecent / listRunsBySession / listBySessionRun)"
  - "Phase 16 plan 04 (REST session-centric endpoints + Console proof hooks consuming ConversationQueryService)"
  - "Phase 17 Console chat-first restore UX (typed transcript consumer)"
  - "Phase 19 multi-turn context assembly (typed ConversationMessageDto consumer)"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Ownership-safe port extension: new repository/query methods accept RequestContext + session/run filters; declared as throwing default methods so existing fakes/infra stubs keep compiling until plan 03"
    - "Pure assembler-as-function: ConversationTranscriptAssembler takes (sessionId, List<ConversationRunView>, List<RunEvent>) and returns List<ConversationMessageDto> with no I/O"
    - "Run-input -> USER message extraction (input[text] falling back to input[prompt])"
    - "Event-sequence ordering within a run, run-creation ordering across runs (D-09, D-16)"
    - "Recursive sensitive-key redaction (secret/token/password/apikey/executor/class) reused for tool summary metadata"
    - "Terminal events are state transitions, not blank assistant messages (D-07); failure with no assistant text yields an ERROR message"

key-files:
  created:
    - "pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationQueryService.java"
    - "pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultConversationQueryService.java"
    - "pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssembler.java"
    - "pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationRunView.java"
    - "pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssemblerTest.java"
    - "pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultConversationQueryServiceTest.java"
  modified:
    - "pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/SessionRepository.java (added listRecent default)"
    - "pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java (added listRunsBySession default)"
    - "pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunEventStore.java (added listBySessionRun default)"
    - "pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java (additive conversation boundary rule)"

key-decisions:
  - "Created a dedicated Conversation read-model use case (D-01) instead of overloading SessionQueryService; old SessionHistoryResponse diagnostic path left fully intact (D-14)."
  - "Output is restricted to Plan 01 typed client DTOs (SessionSummaryDto, ConversationMessageDto, ConversationTranscriptResponse); the assembler never emits raw Map<String,Object> history entries (D-13)."
  - "Introduced an App-internal ConversationRunView record (runId/createdAt/input/status) as the assembler input shared between the query service and RunProjectionRepository.listRunsBySession; it is not a public client contract."
  - "New ownership-aware port methods (listRecent, listRunsBySession, listBySessionRun) are declared as throwing default methods so all existing test fakes and infrastructure stubs continue to compile; Phase 16 plan 03 replaces them with JDBC implementations enforcing tenant/user/session/run filters at SQL level (D-15)."
  - "getTranscript performs an ownership proof via SessionRepository.findById(context, sessionId) before loading any runs/events, so missing/foreign sessions are refused early (D-15)."
  - "MODEL_DELTA events accumulate into a single ASSISTANT message preserving the first/last sequence range; RUN_COMPLETED/FAILED/CANCELLED are status transitions (not blank messages); a failed run with no assistant text yields an ERROR message with a safe summary (D-05, D-06, D-07)."
  - "Tool lifecycle events become TOOL messages with recursively redacted summary metadata; raw executor/class/secret/token/apikey fields are stripped (D-06 + existing redaction discipline)."
  - "Additive ArchUnit rule explicitly forbids the conversation classes and persistence ports from depending on Spring/Vaadin/JDBC/SQLite/Infrastructure/adapter; the existing broad app rule was not weakened."

patterns-established:
  - "Throwing-default port extension for ownership-aware query methods during phased rollout."
  - "App-internal read-model record (ConversationRunView) bridging a persistence port and a pure assembler."
  - "Pure transcript assembler covering user/assistant/tool/error/cancelled cases with deterministic ordering."

requirements-completed:
  - SESS-01
  - SESS-04

# Metrics
duration: ~25min
completed: 2026-06-28
---

# Phase 16 Plan 02: Conversation Read-Model App Boundary + Transcript Assembler Summary

**ConversationQueryService use case + ownership-aware persistence ports + canonical transcript assembler folding runs/events into Plan 01 typed DTOs**

## Performance

- **Duration:** ~25 min
- **Tasks:** 3 (TDD: assembler tests, contracts/ports, query-service + architecture coverage)
- **Files:** 10 (6 created, 4 modified)

## Accomplishments
- Added a dedicated App Conversation read-model boundary (`ConversationQueryService` + `DefaultConversationQueryService`) per decision D-01, returning Plan 01 typed DTOs (`SessionSummaryDto`, `ConversationTranscriptResponse`, `ConversationMessageDto`) for SESS-01 and SESS-04.
- Implemented `ConversationTranscriptAssembler` as a pure function that folds run inputs and persisted `RunEvent`s into typed transcript messages, covering chat input + model deltas (USER + completed ASSISTANT), failures (ERROR or ASSISTANT failed), cancellation (cancelled/partial preserving text + sequence range), tool lifecycle (redacted TOOL message), and multi-run ordering by creation then event sequence.
- Extended `SessionRepository`, `RunProjectionRepository`, and `RunEventStore` with ownership-aware methods (`listRecent`, `listRunsBySession`, `listBySessionRun`) that carry `RequestContext` plus session/run identifiers; declared as throwing defaults so existing fakes and infrastructure stubs keep compiling until plan 03 (D-15).
- `DefaultConversationQueryService` performs an ownership proof via `SessionRepository.findById` before loading runs/events, threads the same `RequestContext` into every port call, and derives the active run from the latest non-terminal run.
- Preserved the old `SessionHistoryResponse` diagnostic path untouched (D-14) and kept transcript output free of raw map entries (D-13).
- Added an additive ArchUnit rule that explicitly forbids the conversation classes and persistence ports from depending on Spring/Vaadin/JDBC/SQLite/Infrastructure/adapter without weakening the existing app dependency rule.

## Task Commits

Per instructions, no commits were made. All changes remain in the working tree only. `.gitignore` was not touched.

## Files Created/Modified
- **Created** `ConversationQueryService.java` — App read-model boundary interface (`listRecentSessions`, `getTranscript`).
- **Created** `DefaultConversationQueryService.java` — Orchestrates `SessionRepository`, `RunProjectionRepository`, `RunEventStore`, and `ConversationTranscriptAssembler`; ownership-proof-first; derives active run + transcript cursor.
- **Created** `ConversationTranscriptAssembler.java` — Canonical reduction: USER from run input, ASSISTANT from accumulated model deltas, TOOL from tool lifecycle (redacted), ERROR from failed-no-text runs; terminal events are state transitions; ordered by run creation then event sequence.
- **Created** `ConversationRunView.java` — App-internal run read model (runId/createdAt/input/status) shared by the port and the assembler.
- **Created** `ConversationTranscriptAssemblerTest.java` — 6 golden tests (chat+deltas, failed-no-text -> ERROR, failed-with-deltas -> ASSISTANT FAILED, cancelled-with-deltas, redacted TOOL message, multi-run ordering).
- **Created** `DefaultConversationQueryServiceTest.java` — 3 tests (recent-session delegation with context passthrough, missing-session refusal before load, typed transcript output + context threading into ports).
- **Modified** `SessionRepository.java` — added `default PageResponse<SessionSummaryDto> listRecent(RequestContext, int, String)`.
- **Modified** `RunProjectionRepository.java` — added `default PageResponse<ConversationRunView> listRunsBySession(RequestContext, String sessionId, int, String)`.
- **Modified** `RunEventStore.java` — added `default List<RunEvent> listBySessionRun(RequestContext, String sessionId, String runId, long, int)`.
- **Modified** `AppDependencyArchTest.java` — added additive `conversation_read_model_and_persistence_ports_must_not_leak_outer_layers` ArchUnit rule.

## Decisions Made
- Modeled the run input as a `Map<String,Object>` on `ConversationRunView` and extracted the user text via `text` then `prompt` keys (matching the runtime's chat/task input shapes in `DefaultRunDispatcher`), so the assembler does not presuppose a single input shape.
- Used throwing `default` methods for the new port contracts (rather than abstract methods) specifically so existing test fakes (`RecordingRunEventStore`, `RecordingRunProjectionRepository`, `RecordingSessionRepository`) and infrastructure stubs keep compiling; the plan explicitly permitted defaults for this transitional state.
- Kept assistant message status as `PENDING` while deltas stream and only transitions on terminal run events (`COMPLETED`/`FAILED`/`CANCELLED`); a cancelled run with accumulated text surfaces `CANCELLED` status with preserved partial text and sequence range (D-07).
- Reused the conservative sensitive-key redaction discipline (secret/token/password/apikey/api_key/executor/class) recursively for tool summary metadata rather than introducing a new redactor, staying within the App layer.

## Deviations from Plan

None material. The plan listed `ConversationTranscriptAssembler.java` as the only new assembler type; an additional App-internal `ConversationRunView` record was introduced as the minimal, documented input contract shared between `RunProjectionRepository.listRunsBySession` and the assembler. This is an App-layer internal type (not a public client contract) and keeps output restricted to Plan 01 DTOs as required.

## Issues Encountered
- `mvn -pl pi-agent-app test` initially failed with a JUnit discovery `NoClassDefFoundError` for the Plan 01 `ConversationMessageDto` because the upstream `pi-agent-client` jar in the local Maven repo predated Plan 01. Resolved by running `mvn -pl pi-agent-client,pi-agent-domain install -DskipTests`; the plan's exact focused gate then passes 11/11.
- The environment's default `JAVA_HOME` targeted Java 17 while the project targets Java 21; re-ran Maven with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (consistent with Plan 01's note).
- A pre-existing `DefaultToolExecutionGatewayTest` failure (`traceId must be a W3C-compatible 32-character lowercase hex value`, 5 errors) exists on the original codebase and is unrelated to this plan; verified by stashing these changes and reproducing the identical failure. No code in `DefaultToolExecutionGateway` or its test was modified here.

## User Setup Required

None for this plan. The throwing-default port methods mean no runtime configuration is required until Phase 16 plan 03 supplies ownership-aware JDBC implementations.

## Next Phase Readiness
- The App conversation boundary is ready for Phase 16 plan 03 (Infrastructure: implement `listRecent`, `listRunsBySession`, `listBySessionRun` with tenant/user/session/run SQL filters) and plan 04 (REST session-centric endpoints + minimal Console proof hooks consuming `ConversationQueryService`).
- The typed `ConversationTranscriptResponse`/`ConversationMessageDto` output and role/status enums are ready for Phase 17 restore UX and Phase 19 context assembly without reshaping.
- `ConversationRunView` gives plan 03 a concrete projection target shape to populate from `runs`/`run_inputs`.

---
*Phase: 16-conversation-read-model-and-recent-sessions*
*Plan: 02*
*Completed: 2026-06-28*
