---
phase: 16-conversation-read-model-and-recent-sessions
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java
  - pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageRole.java
  - pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageStatus.java
  - pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationTranscriptResponse.java
  - pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/SessionSummaryDto.java
  - pi-agent-client/src/test/java/io/github/pi_java/agent/client/ConversationDtoContractTest.java
autonomous: true
requirements:
  - SESS-01
  - SESS-04
must_haves:
  truths:
    - "Future clients can compile against typed recent-session and transcript DTOs without Domain, Vaadin, Spring, or raw Map history contracts."
    - "Transcript messages have first-class user, assistant, tool, and error roles plus explicit lifecycle status."
    - "Recent session summaries carry title, latest activity, status, safe preview, and optional active run refs for UI/client use."
  artifacts:
    - path: "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/SessionSummaryDto.java"
      provides: "Typed recent-session summary contract"
      exports: ["SessionSummaryDto"]
    - path: "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java"
      provides: "Typed transcript message contract"
      exports: ["ConversationMessageDto"]
    - path: "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationTranscriptResponse.java"
      provides: "Typed transcript response contract"
      exports: ["ConversationTranscriptResponse"]
    - path: "pi-agent-client/src/test/java/io/github/pi_java/agent/client/ConversationDtoContractTest.java"
      provides: "Golden client DTO schema proof"
  key_links:
    - from: "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationTranscriptResponse.java"
      to: "pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java"
      via: "messages list uses typed DTO"
      pattern: "List<ConversationMessageDto>"
    - from: "pi-agent-client/src/test/java/io/github/pi_java/agent/client/ConversationDtoContractTest.java"
      to: "conversation DTO records"
      via: "record component assertions"
      pattern: "recordComponents"
---

<objective>
Create the public typed conversation read-model contracts for Phase 16.

Purpose: Phase 16 user decisions D-01, D-04, D-06, D-07, D-08, D-13, and D-16 require downstream App, REST, Console, and future clients to use a new conversation boundary instead of raw `List<Map<String,Object>>` session history as the chat transcript contract.

Output: Plain Java client DTO records/enums under `pi-agent-client/.../conversation` plus a focused DTO schema contract test.
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
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/session/SessionHistoryResponse.java
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/api/PageResponse.java
@pi-agent-client/src/test/java/io/github/pi_java/agent/client/CloudApiDtoContractTest.java

<interfaces>
Existing raw history contract to replace for chat transcript use per D-13:
```java
public record SessionHistoryResponse(
        SessionResponse session,
        List<Map<String, Object>> entries) {
}
```

Existing client pattern: `pi-agent-client` DTOs are public Java records with no Domain/Spring/Vaadin/Jakarta imports.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add conversation DTO contract tests first</name>
  <files>pi-agent-client/src/test/java/io/github/pi_java/agent/client/ConversationDtoContractTest.java</files>
  <behavior>
    - Test 1: `SessionSummaryDto` record components are exactly sessionId, title, status, lastMessagePreview, lastActivityAt, createdAt, activeRunId, activeRunStatus, metadata.
    - Test 2: `ConversationMessageDto` record components are exactly messageId, sessionId, runId, stepId, role, text, status, createdAt, updatedAt, firstSequence, lastSequence, metadata, visible, redacted.
    - Test 3: `ConversationTranscriptResponse` exposes sessionId, messages, activeRunId, activeRunStatus, nextCursor, hasMore, metadata and its `messages` component is typed as `List<ConversationMessageDto>`.
    - Test 4: `ConversationMessageRole` contains USER, ASSISTANT, TOOL, ERROR and wire values `user`, `assistant`, `tool`, `error`.
    - Test 5: `ConversationMessageStatus` contains PENDING, COMPLETED, FAILED, CANCELLED, PARTIAL and wire values `pending`, `completed`, `failed`, `cancelled`, `partial`.
    - Test 6: DTO constructors defensively normalize null maps/lists to empty immutable values where records own collection fields.
  </behavior>
  <action>Create the failing contract test for the new conversation DTO package. Use reflection/record component assertions like existing client contract tests. The tests must intentionally reject raw transcript entries by asserting `ConversationTranscriptResponse.messages()` is not a `List<Map<String,Object>>`. This implements D-01, D-04, D-06, D-07, D-08, D-13, and D-16 at the contract boundary.</action>
  <verify>
    <automated>mvn -pl pi-agent-client -Dtest=ConversationDtoContractTest test</automated>
  </verify>
  <done>The test exists and fails before DTO implementation because the conversation package/classes are missing.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Implement typed conversation DTO records and enums</name>
  <files>pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java, pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageRole.java, pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageStatus.java, pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationTranscriptResponse.java, pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/SessionSummaryDto.java</files>
  <behavior>
    - Constructing a transcript response returns typed `ConversationMessageDto` messages only.
    - Message role/status expose stable lowercase wire values for JSON clients while retaining enum type safety in Java.
    - Metadata fields never return null and are safe immutable copies.
  </behavior>
  <action>Implement the DTO records/enums under `io.github.pi_java.agent.client.conversation`. Keep the package plain Java: no Domain imports, no Spring/Jackson/Jakarta/Vaadin annotations, no persistence classes. Use enums with a `wireValue()` accessor rather than exposing arbitrary strings inside App code. Include fields required by D-08: role, text, session/run refs, status, timestamps, ordering identity, metadata, redaction/visibility. Do not modify or delete `SessionHistoryResponse`; per D-14 it can remain for diagnostics/compatibility, but no new transcript path should depend on it.</action>
  <verify>
    <automated>mvn -pl pi-agent-client -Dtest=ConversationDtoContractTest test</automated>
  </verify>
  <done>Conversation DTO contract tests pass and no new client DTO imports Domain, Spring, Jackson, Jakarta, Vaadin, or Infrastructure classes.</done>
</task>

</tasks>

<verification>
Run the focused client module gate:

```bash
mvn -pl pi-agent-client -Dtest=ConversationDtoContractTest test
```

Then optionally run the broader client DTO contract gate if local time permits:

```bash
mvn -pl pi-agent-client test
```
</verification>

<success_criteria>
- SESS-01 has a typed `SessionSummaryDto` suitable for recent-session lists.
- SESS-04 has a typed `ConversationTranscriptResponse` whose message list is not raw maps or Vaadin state.
- DTOs implement all locked Phase 16 shape decisions D-01, D-04, D-06, D-07, D-08, D-13, and D-16.
- No deferred Phase 17-20 UI polish, streaming reducer, model context, provider readiness, or full local profile productization is implemented here.
</success_criteria>

<output>
After completion, create `.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-01-SUMMARY.md`.
</output>
