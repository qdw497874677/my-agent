---
phase: 19-multi-turn-runtime-context
plan: 02
type: execute
wave: 2
depends_on:
  - 19-multi-turn-runtime-context-01
files_modified:
  - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java
  - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherContextTest.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java
autonomous: true
requirements:
  - CTX-01
  - CTX-02
  - CTX-05
must_haves:
  truths:
    - "Queued run dispatch populates RunContext.sessionContext.messages before AgentRuntime.start."
    - "The current user prompt remains sourced from RunInput and is not duplicated from transcript history."
    - "Context metadata is observable in safe worker audit/projection metadata without leaking prompt text or secrets."
    - "Local/dev composition wires the App assembler into DefaultRunDispatcher without making Vaadin own context assembly."
  artifacts:
    - path: "pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java"
      provides: "Dispatch-time context injection seam"
      contains: "ConversationContextAssembler"
    - path: "pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherContextTest.java"
      provides: "Runtime context injection and current-prompt-once proof at dispatcher level"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java"
      provides: "Spring/local composition root wiring for assembler-enabled dispatcher"
  key_links:
    - from: "DefaultRunDispatcher.dispatchClaimed"
      to: "ConversationContextAssembler"
      via: "assemble before new RunContext"
      pattern: "conversationContextAssembler\\.assemble"
    - from: "DefaultRunDispatcher"
      to: "RunContext"
      via: "SessionContext with assembled messages"
      pattern: "new RunContext\\(.*sessionContext"
---

<objective>
Wire the App-layer context assembler into the runtime dispatch path so every selected-session queued run starts with bounded prior history in `RunContext.sessionContext().messages()`.

Purpose: Phase 19 must close the existing `DefaultRunDispatcher.sessionContext()` empty-history seam while preserving COLA direction: Infrastructure orchestrates dispatch, App owns assembly rules, Domain carries provider-neutral context.
Output: assembler-aware `DefaultRunDispatcher`, focused dispatcher tests, and local composition wiring.
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
@.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-01-SUMMARY.md
@pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java
@pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunContext.java
@pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunInput.java
@pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionContext.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java

<interfaces>
Executor should use the Plan 01 contracts directly:

```java
ConversationContextAssembler.Result assemble(RequestContext context, String sessionId, String currentRunId, ConversationContextPolicy policy);
List<SessionEntryPayload.MessageEntry> messages = result.messages();
ConversationContextMetadata metadata = result.metadata();
```

Existing dispatch seam:

```java
// DefaultRunDispatcher today constructs empty context:
RunContext context = new RunContext(agentDefinition, runInput(queuedRun), sessionContext(queuedRun), workspaceScope(queuedRun), runtimeLimits, token, queuedRun.traceId(), startedAt);
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Inject assembler into DefaultRunDispatcher context creation</name>
  <files>pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java, pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherContextTest.java</files>
  <behavior>
    - Dispatch calls the App context assembler after `RequestContext` creation and before `AgentRuntime.start` (D-03).
    - `RunContext.sessionContext().messages()` contains only assembler-produced prior messages; current prompt remains only in `RunContext.input()` (D-01, D-04).
    - If assembler returns empty history, dispatch still runs with a valid empty `SessionContext` rather than failing normal no-history first turns.
  </behavior>
  <action>Add constructor support to `DefaultRunDispatcher` for `ConversationContextAssembler` and `ConversationContextPolicy`, preserving existing constructors by delegating to a no-op/empty assembler or default policy only where tests require backwards compatibility. Replace the static empty `sessionContext(queuedRun)` with an instance method that calls the assembler using `requestContext`, `queuedRun.sessionId()`, and `queuedRun.runId()` (D-03). Build `SessionContext` with assembler messages plus existing workspace scope and empty artifact/attachment/memory/path lists. Do not move selection/filtering rules into Infrastructure; it only invokes the App assembler. Write `DefaultRunDispatcherContextTest` with a recording `AgentRuntime` that captures `RunContext` and asserts prior messages are present while the current prompt appears only in `RunContext.input()` and not in history (D-04).</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure -Dtest=DefaultRunDispatcherContextTest test</automated>
  </verify>
  <done>Dispatcher context test proves non-empty prior history reaches runtime and current prompt is not duplicated into `SessionContext.messages`.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Record safe context metadata and wire local composition</name>
  <files>pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java, pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherContextTest.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java</files>
  <behavior>
    - Worker audit/projection metadata includes safe counts from `ConversationContextMetadata`: included count, dropped count, excluded count, max char budget, resulting char count, and truncated flag (D-07).
    - Metadata does not include prior prompt text, assistant text, tool payloads, provider payloads, or secrets (D-07, D-12).
    - Local/dev composition constructs and supplies `ConversationContextAssembler` using existing `ConversationQueryService` wiring, not Vaadin component state (D-02, D-03, D-05, D-08).
  </behavior>
  <action>Extend `DefaultRunDispatcher` audit details for `run.worker.started` or an adjacent safe worker audit/projection metadata map to include only `contextIncludedCount`, `contextDroppedCount`, `contextExcludedCount`, `contextMaxChars`, `contextResultChars`, and `contextTruncated`. Keep metadata value types simple (`int`, `long`, `boolean`) and never serialize actual message content. Update `LocalDevRuntimeBeanConfiguration` or the relevant adapter-web composition root to instantiate `ConversationContextAssembler` and `ConversationContextPolicy.defaults()` and pass them to `DefaultRunDispatcher`; use the existing `DefaultConversationQueryService`/repositories where already wired. Add test assertions that audit metadata contains counts but not prior/current text or secret-looking values.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure,pi-agent-adapter-web -Dtest=DefaultRunDispatcherContextTest test</automated>
  </verify>
  <done>Safe context metadata is observable in tests, composition compiles, and dispatcher wiring remains outside Vaadin UI state.</done>
</task>

</tasks>

<verification>
Run focused infrastructure dispatch tests and a compile of the adapter-web module path touched by composition:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure -Dtest=DefaultRunDispatcherContextTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -DskipTests test
```
</verification>

<success_criteria>
- CTX-01 is active in the runtime path: selected-session prior messages reach `AgentRuntime.start` via `RunContext`.
- CTX-02 metadata is recorded with safe counts.
- CTX-05 is preserved: dispatcher invokes an App assembler; Vaadin does not assemble model context.
- D-04 is proven: current prompt is provided once via `RunInput`, not reloaded as a prior transcript entry.
</success_criteria>

<output>
After completion, create `.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-02-SUMMARY.md`.
</output>
