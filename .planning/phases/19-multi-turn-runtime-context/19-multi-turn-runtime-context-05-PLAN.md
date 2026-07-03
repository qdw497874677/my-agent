---
phase: 19-multi-turn-runtime-context
plan: 05
type: gap-closure
wave: 5
depends_on:
  - 19-multi-turn-runtime-context-03
  - 19-multi-turn-runtime-context-04
source_verification: 19-VERIFICATION.md
files_modified:
  - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java
  - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java
  - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClientTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java
autonomous: true
requirements:
  - CTX-01
  - CTX-04
  - CTX-05
must_haves:
  truths:
    - "OpenAI-compatible provider boundary is coherently migrated to ordered message-list streaming across reactor-dependent code."
    - "Adapter-web fake OpenAI provider test composition implements the current messages-based OpenAiStreamSource signature."
    - "OpenAiStreamSource's public API does not expose a package-private message type that prevents external test/config implementations."
    - "A reactor build of adapter-web with dependencies compiles and runs the targeted test phase without the stale stream(String, ...) path."
  artifacts:
    - path: "pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java"
      provides: "Externally usable OpenAI-compatible chat message carrier for OpenAiStreamSource implementations"
      contains: "public"
    - path: "pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java"
      provides: "Public messages-based OpenAI stream interface"
      contains: "stream(List<OpenAiChatMessage>"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java"
      provides: "Adapter-web fake provider implementation using message-list streaming"
      contains: "stream(List<OpenAiChatMessage>"
  key_links:
    - from: "FakeOpenAiProviderE2EConfiguration.FakeOpenAiStreamSource"
      to: "OpenAiStreamSource.stream(List<OpenAiChatMessage>, CancellationToken)"
      via: "test composition fake provider"
      pattern: "stream\\(List<OpenAiChatMessage>"
    - from: "OpenAiStreamSource"
      to: "OpenAiChatMessage"
      via: "public method signature"
      pattern: "public (record|final class) OpenAiChatMessage"
---

<objective>
Close the Phase 19 verification blocker by completing the OpenAI-compatible stream interface migration across dependent adapter-web test composition and ensuring the messages-based public API can be implemented outside the OpenAI module.

Purpose: Phase 19 core context semantics are implemented, but verification failed because adapter-web still has a stale fake `stream(String, CancellationToken)` implementation and `OpenAiStreamSource` exposes `OpenAiChatMessage` in a public method while the message type is not publicly usable. This plan makes the message-list boundary coherent across the reactor and proves it with the exact failing build gate from `19-VERIFICATION.md`.

Output: public/externally implementable OpenAI chat message type, updated adapter-web fake stream source, focused tests, and successful adapter-web reactor verification.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@.planning/phases/19-multi-turn-runtime-context/19-CONTEXT.md
@.planning/phases/19-multi-turn-runtime-context/19-VERIFICATION.md
@.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-03-SUMMARY.md
@.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-04-SUMMARY.md
@pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java
@pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java
@pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java
@pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java

<verification_gap>
`19-VERIFICATION.md` reports Phase 19 as `gaps_found` with one failed truth: provider boundary migration is not coherent across dependent code. The failing artifacts are:

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java` implements the removed `stream(String, CancellationToken)` signature.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java` is package-private while `OpenAiStreamSource` exposes it in a public method signature.

The required proof is:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -DskipTests test
```
</verification_gap>

<phase_boundaries>
- Preserve Phase 19 decisions D-13 through D-16: provider adapters send ordered chat messages; history remains chronological; current input is final user message; no system/developer prompt behavior is introduced.
- Do not reintroduce a permanent string-prompt stream path.
- Do not move model-context assembly into adapter-web, Vaadin state, or provider-specific test code.
- This is a gap-closure plan only; do not broaden into Phase 20 provider readiness or Phase 21 regression/security matrix work.
</phase_boundaries>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Make OpenAiStreamSource messages API externally implementable</name>
  <files>pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java, pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java, pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClientTest.java</files>
  <behavior>
    - `OpenAiChatMessage` is publicly usable by modules that implement `OpenAiStreamSource`, including adapter-web test configuration.
    - `OpenAiStreamSource` keeps a messages-based signature: `stream(List<OpenAiChatMessage> messages, CancellationToken cancellationToken)`.
    - Existing OpenAI module tests continue to prove ordered prior user/assistant messages followed by the current user input exactly once.
    - No Spring AI message classes leak into App, Domain, client, adapter-web production code, or public project-neutral contracts beyond the OpenAI infrastructure module boundary.
  </behavior>
  <action>Change `OpenAiChatMessage` from package-private to a public immutable carrier if it is currently package-private. Prefer a `public record OpenAiChatMessage(String role, String content)` with compact-constructor validation if consistent with the existing implementation. Keep it in the OpenAI infrastructure module/package; do not move it into Domain/App because it is provider-adapter-specific. Confirm `OpenAiStreamSource` imports/uses `List<OpenAiChatMessage>` as the sole stream input. Update focused tests only if constructor visibility or validation changes require it.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiCompatibleStreamingModelClientTest test</automated>
  </verify>
  <done>The OpenAI module compiles/tests pass, and external modules can import and implement the public `OpenAiChatMessage`-based stream API without package-access errors.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Update adapter-web fake OpenAI stream source to message-list signature</name>
  <files>pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java</files>
  <behavior>
    - `FakeOpenAiProviderE2EConfiguration.FakeOpenAiStreamSource` overrides `stream(List<OpenAiChatMessage>, CancellationToken)`, not `stream(String, CancellationToken)`.
    - The fake provider response behavior remains deterministic for adapter-web tests.
    - The fake may inspect or join message content for scripted responses, but it must receive role-preserving messages and must not restore a production string-prompt boundary.
    - Imports use the public OpenAI infrastructure message type and compile in the adapter-web test module.
  </behavior>
  <action>Replace the stale `stream(String prompt, CancellationToken cancellationToken)` override in `FakeOpenAiProviderE2EConfiguration` with `stream(List<OpenAiChatMessage> messages, CancellationToken cancellationToken)`. If the fake previously used the prompt text to generate a canned response, derive any test response from the final user message or a deterministic joined view inside the fake only. Keep cancellation behavior and emitted `OpenAiStreamEvent` semantics unchanged. Remove obsolete `@Override`/imports tied to the string signature.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -DskipTests test</automated>
  </verify>
  <done>Adapter-web test compilation no longer fails on `FakeOpenAiStreamSource` and there is no remaining override of `stream(String, CancellationToken)` in this configuration.</done>
</task>

<task type="auto" tdd="false">
  <name>Task 3: Run the exact Phase 19 gap verification gate and record summary</name>
  <files>.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-05-SUMMARY.md</files>
  <behavior>
    - The exact reactor command that failed in `19-VERIFICATION.md` now succeeds.
    - Summary records the fixed stale signature, the public message type/API decision, and verification commands/results.
    - If another compile failure appears, distinguish it from the Phase 19 gap and fix it only if it is directly caused by this migration; otherwise report it as a new blocker.
  </behavior>
  <action>Run the focused OpenAI module test and the adapter-web reactor compile with dependencies. Create `19-multi-turn-runtime-context-05-SUMMARY.md` using the standard summary template, listing files changed, gap closed, tests run, and any residual risk. Do not mark Phase 19 verified in ROADMAP/REQUIREMENTS here; leave final status update to the verifier/re-verification workflow.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiCompatibleStreamingModelClientTest test</automated>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -DskipTests test</automated>
  </verify>
  <done>`19-VERIFICATION.md`'s missing items are satisfied and the previously failing reactor build succeeds.</done>
</task>

</tasks>

<verification>
Run these gates in order:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiCompatibleStreamingModelClientTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -DskipTests test
```

Optional sanity search after implementation:

```bash
rg "stream\\(String" pi-agent-infrastructure-model-openai pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java
```
</verification>

<success_criteria>
- The adapter-web fake provider compiles against `OpenAiStreamSource.stream(List<OpenAiChatMessage>, CancellationToken)`.
- `OpenAiChatMessage` is public/usable anywhere the public OpenAI stream interface is implemented.
- OpenAI provider-boundary tests still prove ordered messages and current prompt exactly once.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -DskipTests test` succeeds.
- No Vaadin/UI state, Domain/App API, system-message, or Phase 20/21 scope expansion is introduced.
</success_criteria>

<output>
After completion, create `.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-05-SUMMARY.md`.
</output>
