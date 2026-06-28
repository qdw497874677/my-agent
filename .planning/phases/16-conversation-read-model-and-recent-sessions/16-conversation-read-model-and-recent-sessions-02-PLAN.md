---
phase: 16-conversation-read-model-and-recent-sessions
plan: 02
type: execute
wave: 2
depends_on:
  - 16-conversation-read-model-and-recent-sessions-01
files_modified:
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationQueryService.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultConversationQueryService.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssembler.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/SessionRepository.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunEventStore.java
  - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssemblerTest.java
  - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultConversationQueryServiceTest.java
  - pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java
autonomous: true
requirements:
  - SESS-01
  - SESS-04
must_haves:
  truths:
    - "App layer exposes a dedicated conversation query use case for recent sessions and typed transcripts."
    - "Transcript assembly folds run input and model/tool/error/cancel events into stable typed conversation messages."
    - "App ports require RequestContext plus session/run identifiers for ownership-safe repository implementations."
  artifacts:
    - path: "pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationQueryService.java"
      provides: "App read-model boundary for recent sessions and transcripts"
      exports: ["ConversationQueryService"]
    - path: "pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssembler.java"
      provides: "Canonical transcript reduction logic"
      exports: ["ConversationTranscriptAssembler"]
    - path: "pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssemblerTest.java"
      provides: "Golden assembler behavior coverage"
    - path: "pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunEventStore.java"
      provides: "Ownership-aware event query port"
      contains: "RequestContext"
  key_links:
    - from: "ConversationQueryService"
      to: "SessionRepository"
      via: "list recent sessions with RequestContext"
      pattern: "listRecent.*RequestContext"
    - from: "ConversationTranscriptAssembler"
      to: "RunEventStore"
      via: "session/run-owned event loading"
      pattern: "listBySessionRun"
    - from: "ConversationTranscriptAssembler"
      to: "ConversationTranscriptResponse"
      via: "typed DTO output"
      pattern: "ConversationMessageDto"
---

<objective>
Create the App-layer conversation read model and canonical transcript assembler.

Purpose: Phase 16 requires a new Conversation boundary (D-01) with projection-table/read-model-first transcript semantics (D-05), explicit roles/statuses (D-06/D-07), recent-session summary semantics (D-09 through D-12), and ownership-aware repository ports (D-15). This plan defines the App contracts and behavior before Infrastructure/REST/UI wiring.

Output: `ConversationQueryService`, `DefaultConversationQueryService`, `ConversationTranscriptAssembler`, port extensions, and golden App tests.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/REQUIREMENTS.md
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-CONTEXT.md
@.planning/research/ARCHITECTURE.md
@.planning/research/PITFALLS.md
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-01-SUMMARY.md
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/SessionQueryService.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultSessionQueryService.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunQueryService.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/SessionRepository.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunEventStore.java
@pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java

<interfaces>
Existing App seams:
```java
public interface SessionQueryService {
    SessionResponse getSession(RequestContext context, String sessionId);
    SessionHistoryResponse getSessionHistory(RequestContext context, String sessionId);
}

public interface SessionRepository {
    SessionResponse create(RequestContext context, CreateSessionRequest request, String sessionId, Instant now);
    Optional<SessionResponse> findById(RequestContext context, String sessionId);
    SessionHistoryResponse history(RequestContext context, String sessionId);
}

public interface RunProjectionRepository {
    Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId);
    RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId);
}

public interface RunEventStore {
    List<RunEvent> listByRun(String runId, long afterSequence, int limit);
}
```

Required new contracts from Plan 01:
```java
SessionSummaryDto
ConversationMessageDto
ConversationTranscriptResponse
ConversationMessageRole
ConversationMessageStatus
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add golden transcript assembler tests</name>
  <files>pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssemblerTest.java</files>
  <behavior>
    - Test 1: chat run input `{text: "hello"}` plus two `model.delta` events plus finish produces one USER completed message and one ASSISTANT completed message with combined text.
    - Test 2: failed provider/run event produces a USER message plus either an ASSISTANT failed state or ERROR message with a safe summary; finish/error events must not create blank assistant text.
    - Test 3: cancelled run with prior deltas produces an ASSISTANT cancelled/partial message preserving accumulated text and sequence range.
    - Test 4: tool lifecycle event produces a TOOL message with redacted summary metadata and no raw executor/class/secret/token fields.
    - Test 5: messages are ordered by run creation then event sequence, carry sessionId/runId/stepId where available, and preserve firstSequence/lastSequence.
  </behavior>
  <action>Create focused App tests around a pure `ConversationTranscriptAssembler` using in-memory fake runs/events, deterministic timestamps, and the Plan 01 client DTOs. The tests must encode D-05, D-06, D-07, D-08, D-13, and D-16 before production implementation. Use fake domain `RunEvent` objects or a minimal App-local fixture; do not import Infrastructure/JDBC/Vaadin/Spring.</action>
  <verify>
    <automated>mvn -pl pi-agent-app -Dtest=ConversationTranscriptAssemblerTest test</automated>
  </verify>
  <done>Assembler tests exist and initially fail because the assembler/use-case contracts are missing.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Implement ConversationQueryService and assembler contracts</name>
  <files>pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationQueryService.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultConversationQueryService.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssembler.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/SessionRepository.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunEventStore.java</files>
  <behavior>
    - `ConversationQueryService.listRecentSessions(RequestContext, int, String)` returns `PageResponse<SessionSummaryDto>`.
    - `ConversationQueryService.getTranscript(RequestContext, String, int, String)` returns `ConversationTranscriptResponse`.
    - App ports expose ownership-safe list methods that include `RequestContext` and session/run filters, with default methods only where needed to preserve existing tests until Infrastructure plan 03 implements them.
    - Assembler produces only Plan 01 typed DTOs, never `SessionHistoryResponse.entries()` maps.
  </behavior>
  <action>Implement the new App read-model boundary per D-01 instead of overloading `SessionQueryService` with product transcript semantics. `DefaultConversationQueryService` should orchestrate repositories and `ConversationTranscriptAssembler`; it may delegate old session lookup to `SessionRepository.findById(context, sessionId)` only for ownership proof. Extend repository ports for the needs of Phase 16: recent sessions by tenant/user ordered by latest activity, runs by session with tenant/user filters, and events by session+run with tenant/user/session/run filters. Preserve COLA: no Infrastructure, Spring, JDBC, Vaadin, or provider SDK imports in App. Leave old `SessionHistoryResponse` path intact for diagnostics/compatibility per D-14.</action>
  <verify>
    <automated>mvn -pl pi-agent-app -Dtest=ConversationTranscriptAssemblerTest test</automated>
  </verify>
  <done>Assembler tests pass and App production code compiles with only App/Domain/Client/JDK dependencies.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Add query-service and architecture coverage</name>
  <files>pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultConversationQueryServiceTest.java, pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java</files>
  <behavior>
    - Test 1: recent sessions service delegates to repository with the same RequestContext and returns summaries sorted by repository order.
    - Test 2: transcript service refuses/throws on missing session before loading run events.
    - Test 3: transcript service output uses `ConversationTranscriptResponse` and `ConversationMessageDto`, not `SessionHistoryResponse` or raw `Map` entries.
    - Test 4: App architecture gate still passes and explicitly protects the new conversation package from Spring/Vaadin/JDBC/SQLite/provider classes.
  </behavior>
  <action>Add App use-case tests with fake repository implementations proving the ownership context is threaded through listRecent/getTranscript and that raw-map session history is not used as the transcript output. If `AppDependencyArchTest` already covers the package broadly, add comments or a targeted assertion only if needed; do not weaken the existing architecture rule. This implements D-15 and D-16.</action>
  <verify>
    <automated>mvn -pl pi-agent-app -Dtest=DefaultConversationQueryServiceTest,ConversationTranscriptAssemblerTest,AppDependencyArchTest test</automated>
  </verify>
  <done>Focused App tests and architecture gate pass without adding any outer-layer dependency to `pi-agent-app`.</done>
</task>

</tasks>

<verification>
Run the focused App gate:

```bash
mvn -pl pi-agent-app -Dtest=DefaultConversationQueryServiceTest,ConversationTranscriptAssemblerTest,AppDependencyArchTest test
```
</verification>

<success_criteria>
- SESS-01 is represented in App by `listRecentSessions` and typed `SessionSummaryDto` output.
- SESS-04 is represented in App by `getTranscript` and typed `ConversationTranscriptResponse` output.
- Locked decisions D-01, D-05 through D-13, D-15, and D-16 are implemented at the App contract/assembler layer.
- No Phase 17 full restore UX, Phase 18 streaming reducer, Phase 19 context assembler, Phase 20 provider/local profile productization, or future search/rename/branching work is included.
</success_criteria>

<output>
After completion, create `.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-02-SUMMARY.md`.
</output>
