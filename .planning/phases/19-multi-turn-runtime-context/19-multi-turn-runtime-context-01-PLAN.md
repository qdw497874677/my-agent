---
phase: 19-multi-turn-runtime-context
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextPolicy.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextMetadata.java
  - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationContextAssemblerTest.java
autonomous: true
requirements:
  - CTX-01
  - CTX-02
  - CTX-03
  - CTX-05
must_haves:
  truths:
    - "Runtime context can be assembled from selected-session prior transcript turns before model execution."
    - "Only eligible prior user/assistant messages enter model context."
    - "Recent-turn and character budgets are configurable and drop older history first while preserving included chronological order."
    - "Sensitive, non-visible, redacted, tool, error, provider, audit, credential, approval, policy, and diagnostic messages are excluded and counted."
  artifacts:
    - path: "pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java"
      provides: "App-layer transcript-to-SessionContext message assembly"
      exports: ["ConversationContextAssembler"]
    - path: "pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextPolicy.java"
      provides: "Configurable recent-turn and character budget defaults"
      exports: ["ConversationContextPolicy"]
    - path: "pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextMetadata.java"
      provides: "Included/dropped/excluded/truncated context observability metadata"
      exports: ["ConversationContextMetadata"]
  key_links:
    - from: "ConversationContextAssembler"
      to: "ConversationQueryService.getTranscript"
      via: "typed transcript source"
      pattern: "getTranscript\\(context, sessionId"
    - from: "ConversationContextAssembler"
      to: "SessionEntryPayload.MessageEntry"
      via: "provider-neutral Domain message carrier"
      pattern: "new SessionEntryPayload\\.MessageEntry"
---

<objective>
Create the App-layer multi-turn context policy and assembler that converts the Phase 16 typed transcript into bounded, safe `SessionContext.messages` history for selected-session continuation.

Purpose: selected-session continuation must be model-visible and safe without making Vaadin, provider adapters, or persistence code own context business rules.
Output: `ConversationContextAssembler`, `ConversationContextPolicy`, `ConversationContextMetadata`, and deterministic App tests.
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
@.planning/phases/19-multi-turn-runtime-context/19-CONTEXT.md
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-02-SUMMARY.md
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationQueryService.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultConversationQueryService.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssembler.java
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageRole.java
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageStatus.java
@pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionContext.java
@pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionEntryPayload.java

<interfaces>
Existing contracts to use directly:

```java
// pi-agent-app/src/main/java/.../ConversationQueryService.java
PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor);
ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor);

// pi-agent-domain/src/main/java/.../SessionContext.java
public record SessionContext(
    List<SessionEntryPayload.MessageEntry> messages,
    List<Artifact> artifacts,
    List<Attachment> attachments,
    List<ExternalReference> externalReferences,
    List<SessionEntryPayload.MemoryReferenceEntry> memoryRefs,
    Optional<WorkspaceScope> workspaceScope,
    List<SessionEntry> activeEntryPath) {}

// pi-agent-domain/src/main/java/.../SessionEntryPayload.java
record MessageEntry(String role, String content) implements SessionEntryPayload {}
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Define context policy and metadata contracts</name>
  <files>pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextPolicy.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextMetadata.java, pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationContextAssemblerTest.java</files>
  <behavior>
    - Default policy is conservative and configurable: max recent eligible messages/turns plus max total characters per D-05 and D-08.
    - Policy rejects zero/negative budgets with clear IllegalArgumentException.
    - Metadata records included count, dropped count, excluded count, max character budget, resulting character count, and truncated flag per D-07.
  </behavior>
  <action>Create App-layer records `ConversationContextPolicy` and `ConversationContextMetadata` under `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase`. Keep them plain Java with no Spring, Vaadin, JDBC, SQLite, Infrastructure, Adapter, Spring AI, or provider SDK imports (D-02, D-20). Use default values suitable for Phase 19 (for example 12 recent messages and 12000 chars) but expose `defaults()` and constructor validation so later configuration can override them (D-08). Start tests in `ConversationContextAssemblerTest` covering policy defaults/validation and metadata construction before assembler implementation.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app -Dtest=ConversationContextAssemblerTest test</automated>
  </verify>
  <done>Policy and metadata records compile, default/validation tests pass, and contracts contain no outer-layer imports.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Implement safe transcript-to-message context assembly</name>
  <files>pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java, pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationContextAssemblerTest.java</files>
  <behavior>
    - Given typed transcript messages with prior completed user/assistant turns, assembler emits `SessionEntryPayload.MessageEntry` values in chronological order (D-01, D-14).
    - Tool, error, audit/provider/credential/approval/policy/diagnostic-like roles or metadata, invisible messages, redacted messages, failed/cancelled/error diagnostics, and blank text are excluded and counted (D-09, D-10, D-11, D-12).
    - Recent-message budget keeps newest eligible history and drops older entries first, while preserving chronological order among included messages (D-05, D-06).
    - Character budget trims by dropping older eligible messages first; no `[redacted]` placeholders are sent to the model (D-12).
  </behavior>
  <action>Create `ConversationContextAssembler` in the App usecase package. It must call `ConversationQueryService.getTranscript(context, sessionId, policy.transcriptLimit(), null)` or equivalent, filter eligible `ConversationMessageDto` entries, exclude the current run id when supplied so the just-created input is not reloaded as history (D-04), convert eligible prior `ConversationMessageRole.USER`/`ASSISTANT` messages into Domain `SessionEntryPayload.MessageEntry(role.wireValue(), text)`, apply budget/truncation, and return a result record or nested record containing the included messages plus `ConversationContextMetadata`. Keep this as App/runtime seam logic only; do not use Vaadin state, raw JDBC, provider adapters, or Spring AI memory (D-02, D-05 through D-12, CTX-05).</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app -Dtest=ConversationContextAssemblerTest test</automated>
  </verify>
  <done>Assembler tests prove eligible prior turns are included, ineligible/sensitive messages are excluded and counted, truncation metadata is correct, newest history is retained, and the App-layer assembler has no outer-layer dependencies.</done>
</task>

</tasks>

<verification>
Run the focused App test gate plus the existing App architecture gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app -Dtest=ConversationContextAssemblerTest,AppDependencyArchTest test
```
</verification>

<success_criteria>
- CTX-01 foundation exists: selected-session transcript can produce prior `user`/`assistant` `MessageEntry` context.
- CTX-02 is covered by configurable recent-message and character budgets plus truncation metadata.
- CTX-03 is covered by role/status/visibility/redaction filtering and excluded counts.
- CTX-05 is preserved because assembly lives in App usecase code, not Vaadin or provider adapters.
</success_criteria>

<output>
After completion, create `.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-01-SUMMARY.md`.
</output>
