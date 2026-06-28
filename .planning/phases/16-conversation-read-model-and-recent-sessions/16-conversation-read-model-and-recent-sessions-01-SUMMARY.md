---
phase: 16-conversation-read-model-and-recent-sessions
plan: 01
subsystem: api
tags: [java, records, dto, conversation-read-model, contract-test]

# Dependency graph
requires: []
provides:
  - "Typed SessionSummaryDto contract for recent-session lists (SESS-01)"
  - "Typed ConversationMessageDto contract with role/status enums (D-06, D-07, D-08)"
  - "Typed ConversationTranscriptResponse contract replacing raw List<Map<String,Object>> transcript (SESS-04, D-13)"
  - "ConversationMessageRole enum with stable lowercase wire values (user/assistant/tool/error)"
  - "ConversationMessageStatus enum with stable lowercase wire values (pending/completed/failed/cancelled/partial)"
  - "ConversationDtoContractTest golden schema proof (D-16)"
affects:
  - "Phase 16 plans 02-04 (App query use case, REST endpoints, Console proof hooks, repository/SQL)"
  - "Phase 17 Console chat-first restore UX"
  - "Phase 19 multi-turn model context assembly"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Compact-record-constructor defensive normalization: null collections -> empty immutable; non-null collections -> Map.copyOf/List.copyOf"
    - "Enum wireValue() accessor for stable lowercase JSON/REST client schema"

key-files:
  created:
    - "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/SessionSummaryDto.java"
    - "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java"
    - "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageRole.java"
    - "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageStatus.java"
    - "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationTranscriptResponse.java"
    - "pi-agent-client/src/test/java/io/github/pi_java/agent/client/ConversationDtoContractTest.java"
  modified: []

key-decisions:
  - "Created new Conversation read-model boundary in pi-agent-client/.../conversation rather than extending Session boundary (D-01, D-04)."
  - "role and status are typed enums (ConversationMessageRole, ConversationMessageStatus) with wireValue(); status fields on SessionSummaryDto and activeRunStatus remain plain Strings since Phase 16 does not require a new enum for those run-status string aliases."
  - "Compact record constructors normalize null collections to empty immutable values and copy non-null collections/maps immutably so callers cannot mutate record state."
  - "SessionHistoryResponse left untouched for diagnostic/compat use per D-14; no new transcript path depends on it."

patterns-established:
  - "Defensive immutable copy pattern for client DTO collection/map fields (null -> empty immutable; non-null -> copyOf)."
  - "Enum wireValue() convention for client DTO enums that need a stable JSON wire shape."

requirements-completed:
  - SESS-01
  - SESS-04

# Metrics
duration: ~10min
completed: 2026-06-28
---

# Phase 16 Plan 01: Conversation Read-Model DTO Contracts Summary

**Typed SessionSummaryDto + ConversationMessageDto + ConversationTranscriptResponse contracts with role/status enums replacing raw List<Map<String,Object>> transcript for recent-session and transcript clients**

## Performance

- **Duration:** ~10 min
- **Tasks:** 2 (TDD: failing contract test first, then DTO implementation)
- **Files modified:** 6 (5 created in main, 1 created in test)

## Accomplishments
- Added a new `io.github.pi_java.agent.client.conversation` package in `pi-agent-client` with plain Java record/enum contracts and zero framework imports.
- Locked the exact record component schemas for `SessionSummaryDto`, `ConversationMessageDto`, and `ConversationTranscriptResponse` via reflection-based golden contract tests (D-16).
- Replaced raw-map transcript shape at the contract boundary: `ConversationTranscriptResponse.messages` is a typed `List<ConversationMessageDto>` (D-05, D-13, D-16).
- Implemented stable lowercase `wireValue()` accessors on `ConversationMessageRole` (user/assistant/tool/error) and `ConversationMessageStatus` (pending/completed/failed/cancelled/partial) so JSON/REST clients get a stable wire shape while Java code keeps enum type safety (D-06, D-07).
- Defensive immutable normalization of all collection/map fields so records never expose null or mutable state.

## Task Commits

Per instructions, no commits were made. All changes are staged in the working tree only.

## Files Created/Modified
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/SessionSummaryDto.java` - Recent-session summary record (sessionId, title, status, lastMessagePreview, lastActivityAt, createdAt, activeRunId, activeRunStatus, metadata) for SESS-01.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java` - Typed transcript message record (messageId, sessionId, runId, stepId, role, text, status, createdAt, updatedAt, firstSequence, lastSequence, metadata, visible, redacted) implementing D-08.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageRole.java` - Enum USER/ASSISTANT/TOOL/ERROR with `wireValue()` (D-06).
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageStatus.java` - Enum PENDING/COMPLETED/FAILED/CANCELLED/PARTIAL with `wireValue()` (D-07).
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationTranscriptResponse.java` - Typed transcript response (sessionId, messages, activeRunId, activeRunStatus, nextCursor, hasMore, metadata) with `List<ConversationMessageDto> messages` for SESS-04.
- `pi-agent-client/src/test/java/io/github/pi_java/agent/client/ConversationDtoContractTest.java` - 7 golden contract tests covering record component names, enum wire values, defensive normalization, immutability, and forbidden imports.

## Decisions Made
- Kept `status` and `activeRunStatus` on `SessionSummaryDto` as plain `String` because Phase 16 decisions only mandate two enums (message role + message status) and D-12 only requires that recent-session status be derived from recent run state, not a new enum.
- Implemented defensive normalization inside compact record constructors using `Map.copyOf` / `List.copyOf` (null-safe via ternary) to satisfy the immutability requirement and the contract test.
- Did NOT modify or delete `SessionHistoryResponse` (D-14 preserves diagnostic/compat endpoints); the new transcript path simply does not depend on it.

## Deviations from Plan

None - plan executed exactly as written. The two TDD tasks (write failing contract test, then implement DTOs) were completed as specified; the focused gate `mvn -pl pi-agent-client -Dtest=ConversationDtoContractTest test` passes (7/7), and the broader client gate `mvn -pl pi-agent-client test` also passes (15/15).

## Issues Encountered
- Initial `mvn` run failed because the environment's `JAVA_HOME` pointed at Java 17 while the project targets Java 21. Re-ran with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`; build and tests succeeded. No code change was required.

## User Setup Required
None - no external service configuration required. This plan only adds plain Java DTO records/enums plus a unit-level contract test.

## Next Phase Readiness
- The conversation DTO boundary is ready for Phase 16 plan 02 (App-layer `ConversationQueryService` returning these DTOs) and plan 03 (session-centric REST endpoints exposing the typed transcript).
- Enum `wireValue()` accessors are ready for downstream REST/JSON serialization work; Jackson adapter modules may map these directly when needed.
- `ConversationMessageDto` carries all ordering/redaction/visibility fields required by Phase 17 restore UX and Phase 19 context assembly, so later phases can consume this contract without reshaping.

---
*Phase: 16-conversation-read-model-and-recent-sessions*
*Plan: 01*
*Completed: 2026-06-28*
