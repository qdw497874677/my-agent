---
phase: 19-multi-turn-runtime-context
plan: 03
type: execute
wave: 3
depends_on:
  - 19-multi-turn-runtime-context-02
files_modified:
  - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java
  - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java
  - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java
  - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClientTest.java
autonomous: true
requirements:
  - CTX-01
  - CTX-04
must_haves:
  truths:
    - "OpenAI-compatible provider boundary receives ordered chat messages, not one concatenated prompt string."
    - "Provider message order is prior history in chronological order, then current user input last."
    - "The current prompt appears exactly once in the provider-neutral/OpenAI-compatible message list."
    - "No Spring AI message classes leak outside the OpenAI infrastructure module."
  artifacts:
    - path: "pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java"
      provides: "Messages-based OpenAI streaming source interface"
      contains: "stream(List"
    - path: "pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java"
      provides: "ModelRequest to ordered provider chat messages conversion"
      contains: "messagesFrom"
    - path: "pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java"
      provides: "Spring AI Prompt construction with UserMessage and AssistantMessage roles"
      contains: "new Prompt"
  key_links:
    - from: "OpenAiCompatibleStreamingModelClient"
      to: "RunContext.sessionContext.messages"
      via: "messagesFrom(ModelRequest)"
      pattern: "sessionContext\\(\\)\\.messages\\(\\)"
    - from: "OpenAiSpringAiModelFactory"
      to: "Spring AI Prompt"
      via: "infrastructure-only message mapping"
      pattern: "new (UserMessage|AssistantMessage)"
---

<objective>
Migrate the OpenAI-compatible streaming boundary from single-string prompt streaming to ordered role-preserving chat messages built from `SessionContext.messages` plus the current `RunInput`.

Purpose: prior turns are useless if provider adapters flatten or drop role information; Phase 19 needs provider-neutral context to survive to Spring AI/OpenAI-compatible calls.
Output: messages-based OpenAI stream interface, client conversion tests, and Spring AI prompt mapping.
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
@.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-02-SUMMARY.md
@pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java
@pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java
@pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java
@pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelRequest.java
@pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunInput.java
@pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionEntryPayload.java

<interfaces>
Current implementation loses history:

```java
// OpenAiCompatibleStreamingModelClient
events = modelFactory.create(config).stream(promptFrom(request), cancellationToken);

private static String promptFrom(ModelRequest request) {
    if (request.context().input() instanceof RunInput.ChatInput chat) return chat.text();
    ...
}

// OpenAiStreamSource
Iterable<OpenAiStreamEvent> stream(String prompt, CancellationToken cancellationToken);
```

Plan 03 should introduce an infrastructure-local message record/class such as:

```java
record OpenAiChatMessage(String role, String content) {}
Iterable<OpenAiStreamEvent> stream(List<OpenAiChatMessage> messages, CancellationToken cancellationToken);
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Convert ModelRequest into ordered provider-neutral chat messages</name>
  <files>pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java, pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java, pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClientTest.java</files>
  <behavior>
    - Message list contains historical `SessionEntryPayload.MessageEntry` values from `RunContext.sessionContext().messages()` first, preserving order (D-13, D-14).
    - Current `RunInput.ChatInput`/`TaskInput` text is appended as the final `user` message exactly once (D-04, D-14).
    - Unsupported/blank historical roles are ignored or normalized only for `user` and `assistant`; no system/developer prompt is introduced (D-16).
  </behavior>
  <action>Add an infrastructure-local immutable message type in the OpenAI module (for example nested/package-private `OpenAiChatMessage`) and change `OpenAiStreamSource` to accept `List<OpenAiChatMessage>` instead of `String prompt` (D-15). Update `OpenAiCompatibleStreamingModelClient` to build messages from `request.context().sessionContext().messages()` plus the current `RunInput` as the final user message. Keep a short package-private `promptFrom` helper only if tests still need current input extraction, but the provider call must use messages, not a concatenated prompt string (D-13 through D-16). Write tests with a fake `OpenAiStreamSource` capturing the message list and asserting order, roles, and current prompt exactly once.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiCompatibleStreamingModelClientTest test</automated>
  </verify>
  <done>OpenAI-compatible client tests capture ordered messages `[prior user, prior assistant, current user]` and fail if the implementation calls a single prompt path or duplicates current input.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Map ordered messages to Spring AI Prompt roles inside infrastructure</name>
  <files>pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java, pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClientTest.java</files>
  <behavior>
    - `user` messages map to Spring AI `UserMessage`; `assistant` messages map to `AssistantMessage` (D-13).
    - Message order is preserved when creating `Prompt` (D-14).
    - Spring AI message imports remain isolated to `pi-agent-infrastructure-model-openai`; no App/Domain/client contract changes are made.
  </behavior>
  <action>Update `OpenAiSpringAiModelFactory.SpringAiStreamSource.stream(...)` to build `new Prompt(List<Message>)` from the infrastructure-local message list, mapping only `user` and `assistant` roles to Spring AI message classes. Remove or deprecate the old `stream(String prompt, ...)` implementation so there is no permanent dual semantic where one path silently loses roles (D-15). Keep cancellation/disposable behavior unchanged. Add/extend tests using fake sources/factories to verify the messages path is exercised; do not call a real provider.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiCompatibleStreamingModelClientTest test</automated>
  </verify>
  <done>Spring AI prompt construction preserves roles/order inside infrastructure, the old string-only call path is no longer the primary provider boundary, and focused OpenAI tests pass without external keys.</done>
</task>

</tasks>

<verification>
Run the OpenAI module focused provider-boundary tests:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiCompatibleStreamingModelClientTest test
```
</verification>

<success_criteria>
- CTX-01 reaches provider boundary as ordered role-preserving messages.
- CTX-04 provider-boundary proof exists for prior turns and current prompt exactly once.
- D-13 through D-16 are implemented without leaking Spring AI types into App/Domain.
</success_criteria>

<output>
After completion, create `.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-03-SUMMARY.md`.
</output>
