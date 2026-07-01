---
phase: 19-multi-turn-runtime-context
plan: 04
type: execute
wave: 4
depends_on:
  - 19-multi-turn-runtime-context-03
files_modified:
  - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java
  - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeModelClient.java
  - pi-testkit/src/test/java/io/github/pi_java/agent/testkit/FakeModelContextCaptureTest.java
  - pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java
  - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java
autonomous: true
requirements:
  - CTX-03
  - CTX-04
  - CTX-05
must_haves:
  truths:
    - "Fake model/testkit can capture actual ModelRequest context messages for semantic assertions."
    - "Tests prove prior turns are available to fake model and current prompt appears exactly once."
    - "Architecture gates prevent Phase 19 context policy/assembler code from depending on Vaadin, Spring AI, provider SDKs, adapter-web, infrastructure, or persistence implementations."
    - "Safety tests prove tool/error/provider/audit/credential-like historical items are excluded before model context."
  artifacts:
    - path: "pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java"
      provides: "Captured ModelRequest access for streaming fake model tests"
      contains: "lastRequest"
    - path: "pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeModelClient.java"
      provides: "Captured ModelRequest access for non-streaming fake model tests"
      contains: "lastRequest"
    - path: "pi-testkit/src/test/java/io/github/pi_java/agent/testkit/FakeModelContextCaptureTest.java"
      provides: "CTX-04 no-key fake-model semantic proof"
    - path: "pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java"
      provides: "CTX-05 App boundary architecture guard"
  key_links:
    - from: "FakeStreamingModelClient"
      to: "ModelRequest.context.sessionContext.messages"
      via: "request capture accessor"
      pattern: "lastRequest\\(\\)"
    - from: "AppDependencyArchTest"
      to: "ConversationContextAssembler"
      via: "no outer layer dependency rule"
      pattern: "ConversationContext(Assembler|Policy|Metadata)"
---

<objective>
Add final Phase 19 fake-model capture, semantic context proof, and architecture/safety gates so the phase can be verified without real providers or UI-driven history.

Purpose: CTX-04 and CTX-05 require proof at the model/testkit and architecture levels, not only unit tests around the assembler.
Output: fake model request capture APIs, no-key context capture tests, and strengthened ArchUnit boundaries.
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
@.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-02-SUMMARY.md
@.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-03-SUMMARY.md
@pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java
@pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeModelClient.java
@pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java
@pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java
@pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java

<interfaces>
Existing streaming fake entrypoint:

```java
public final class FakeStreamingModelClient implements StreamingModelClient {
    @Override
    public void stream(ModelRequest request, CancellationToken cancellationToken, ModelStreamSink sink) { ... }
}
```

Add simple capture APIs without changing production model contracts, e.g.:

```java
public Optional<ModelRequest> lastRequest();
public List<ModelRequest> requests();
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add fake model request capture and CTX-04 semantic proof</name>
  <files>pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java, pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeModelClient.java, pi-testkit/src/test/java/io/github/pi_java/agent/testkit/FakeModelContextCaptureTest.java</files>
  <behavior>
    - Fake streaming and non-streaming model clients expose captured `ModelRequest` history in test-only accessors without requiring real provider keys (D-17).
    - Test constructs a `ModelRequest`/runtime path with prior user+assistant `SessionEntryPayload.MessageEntry` values and current `RunInput.ChatInput`; fake capture proves prior turns are present and ordered before current prompt.
    - Test asserts the current prompt text appears exactly once across captured history+current input semantics (D-04, D-17).
  </behavior>
  <action>Add request capture storage to `FakeStreamingModelClient` and `FakeModelClient` (for example `List<ModelRequest> requests = new CopyOnWriteArrayList<>()`, `lastRequest()`, `requests()`). Capture before scripted actions execute. Create `FakeModelContextCaptureTest` in `pi-testkit/src/test/java/...` that uses the fake client and/or `GeneralAgentLoop` path to send a `ModelRequest` carrying prior messages in `RunContext.sessionContext().messages()` and current input in `RunInput`; assert history roles/order and current prompt exactly once. Keep this deterministic/no-key and avoid any OpenAI/Spring AI imports.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-testkit -Dtest=FakeModelContextCaptureTest test</automated>
  </verify>
  <done>Fake model clients expose captured `ModelRequest`s and CTX-04 semantic test fails on missing prior turns or duplicated current prompt.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Strengthen safety and architecture gates for context boundaries</name>
  <files>pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java, pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java, pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationContextAssemblerTest.java</files>
  <behavior>
    - App ArchUnit explicitly includes `ConversationContextAssembler`, `ConversationContextPolicy`, and `ConversationContextMetadata` in the no-outer-layer rule (D-20, CTX-05).
    - Domain ArchUnit continues to reject Spring AI/OpenAI/Vaadin/App/Infra/Adapter dependencies; add a message/context-specific assertion only if needed for clarity (D-20).
    - Context assembler safety tests cover role filtering, sensitive key/value filtering, invisible/redacted exclusion, failed/cancelled/error exclusion, and excluded/truncated metadata counts (D-18, D-19).
  </behavior>
  <action>Update `AppDependencyArchTest.conversation_read_model_and_persistence_ports_must_not_leak_outer_layers` or add a new test to lock Phase 19 context classes away from Spring, Vaadin, JDBC, SQLite, Infrastructure, Adapter, Spring AI, OpenAI SDK, and provider packages. Keep Domain architecture test intact and extend only if a Phase 19-specific message/context guard adds value without weakening the broader rule. Add any missing safety cases to `ConversationContextAssemblerTest`: cross-session/current-run exclusion where feasible, role/status/visibility/redaction exclusion, sensitive metadata/value discipline, and metadata counts. Do not introduce broad Phase 21 regression matrices here (deferred by 19-CONTEXT).</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app,pi-agent-domain -Dtest=ConversationContextAssemblerTest,AppDependencyArchTest,DomainDependencyArchTest test</automated>
  </verify>
  <done>Architecture and safety gates pass and explicitly prove Phase 19 context assembly stays in App/runtime-safe seams, not Vaadin/provider/persistence implementations.</done>
</task>

</tasks>

<verification>
Run the final focused Phase 19 no-key gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app,pi-agent-domain,pi-testkit -Dtest=ConversationContextAssemblerTest,AppDependencyArchTest,DomainDependencyArchTest,FakeModelContextCaptureTest test
```
</verification>

<success_criteria>
- CTX-03 safety is proven by role/redaction/sensitive exclusion tests.
- CTX-04 fake-model capture proves prior turns are present and current prompt appears exactly once.
- CTX-05 architecture gates prove context assembly is not in Vaadin component state or provider SDK contracts.
- Deferred broad Phase 21 regression/security matrix is not implemented in this plan.
</success_criteria>

<output>
After completion, create `.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-04-SUMMARY.md`.
</output>
