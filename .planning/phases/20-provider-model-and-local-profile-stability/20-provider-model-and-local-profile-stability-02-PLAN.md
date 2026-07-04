---
phase: 20-provider-model-and-local-profile-stability
plan: 02
type: execute
wave: 2
depends_on:
  - 20-provider-model-and-local-profile-stability-01
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfig.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigStore.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/main/resources/messages.properties
  - pi-agent-adapter-web/src/main/resources/messages_zh.properties
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleNoProviderFallbackTest.java
autonomous: true
requirements:
  - PROV-03
  - PROV-05
must_haves:
  truths:
    - "Changing the model selector persists locally but only affects future runs."
    - "No configured provider/key blocks product Console send by default."
    - "Explicit local fallback mode is visibly labeled in the model area and assistant bubble metadata."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "model selection persistence, next-run notice, no-key send guard"
      contains: "applies"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java"
      provides: "fallback/local label rendering hook on assistant bubbles or metadata"
      contains: "fallback"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleNoProviderFallbackTest.java"
      provides: "component proof for blocked no-key send and explicit fallback labels"
      min_lines: 80
  key_links:
    - from: "ConsoleView model selector"
      to: "ProviderConfigStore.update"
      via: "value change listener"
      pattern: "providerConfigStore\.update"
    - from: "ConsoleView planChatSubmission"
      to: "provider readiness/fallback mode"
      via: "send guard before createSession/createRun"
      pattern: "isReady|fallback"
---

<objective>
Make model selection trustworthy and make no-provider/fallback semantics explicit in the product Console.

Purpose: users must not mistake no-key demo output for real model output, and model changes must be clearly scoped to subsequent runs.
Output: selected model persistence UX, blocked send behavior for no-provider default, explicit fallback labeling hooks, and component tests.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@.planning/STATE.md
@.planning/phases/20-provider-model-and-local-profile-stability/20-CONTEXT.md
@.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-01-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfig.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigStore.java

<interfaces>
Executor should preserve these existing flows:
```java
modelSelector.addValueChangeListener(event -> providerConfigStore.update(new ProviderConfig(... event.getValue() ...)));

public RunSubmissionPlan planChatSubmission(String text) {
    chatPanel.appendUserMessage(message);
    String sessionId = needsSession ? executionBridge.createSession().sessionId() : selectedSessionId;
    RunResponse run = executionBridge.createRun(sessionId, request);
}
```
Add the provider readiness/fallback guard before session/run creation so blocked sends do not persist fake runs.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Persist model selection with next-run-only feedback</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfig.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigStore.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/resources/messages.properties, pi-agent-adapter-web/src/main/resources/messages_zh.properties, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java</files>
  <behavior>
    - Test 1: selecting a custom model calls `ProviderConfigStore.update(...)` immediately and persists the selected model.
    - Test 2: when a run is active, changing the selector renders an “applies to next run” status and does not mutate `activeRunId` or current stream state.
    - Test 3: existing `data-role="model-selector"` remains stable and the next-run notice has a testable hook such as `data-role="model-selection-scope"`.
  </behavior>
  <action>Harden the existing model selector value-change listener per D-05. Persist selected model immediately using `ProviderConfigStore.update(...)`, but render localized copy that the change applies to the next run only when a run is active or recently selected. Do not try to mutate `activeRunId`, live stream reducer state, or already-created run metadata. Keep provider SDK types out of these adapter-web classes per D-06.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=WebConsoleProviderModelBarTest test</automated>
  </verify>
  <done>Selected model persists locally and the Console communicates next-run-only semantics without changing active runs.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Block default no-provider send and label explicit fallback</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfig.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/main/resources/messages.properties, pi-agent-adapter-web/src/main/resources/messages_zh.properties, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleNoProviderFallbackTest.java</files>
  <behavior>
    - Test 1: with `enabled=false` or blank API key and no explicit fallback mode, `planChatSubmission(...)` does not call bridge `createSession` or `createRun` and shows a provider-not-ready message.
    - Test 2: the blocked-send message is visible in composer/model area with an actionable configure-provider instruction per D-04/D-09.
    - Test 3: when explicit local/dev fallback mode is enabled, the model area exposes `data-fallback-mode="local"` and assistant bubble/metadata exposes a local fallback label per D-10/D-11.
  </behavior>
  <action>Implement D-09 by guarding Console send before appending/persisting a user message or creating a session/run when the provider is not ready and explicit local fallback is not enabled. Add the smallest explicit fallback-mode flag needed in adapter-web local provider config or a local/test profile property; do not implement automatic paid-provider fallback or cross-provider routing per D-12. When explicit fallback is enabled, add visible localized labels in both the model area and assistant bubble/metadata using stable selectors (`data-fallback-mode`, `data-role="fallback-label"` or equivalent). Do not use browser localStorage or Vaadin memory as fallback history truth.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=WebConsoleNoProviderFallbackTest test</automated>
  </verify>
  <done>No-key product Console blocks sends by default; explicit local fallback mode is visibly and testably labeled.</done>
</task>

</tasks>

<verification>
Run both focused tests after implementation: `mvn -pl pi-agent-adapter-web -Dtest=WebConsoleProviderModelBarTest,WebConsoleNoProviderFallbackTest test`.
</verification>

<success_criteria>
- PROV-03: model selection is persisted and scoped to subsequent runs.
- PROV-05: no-provider fallback cannot masquerade as a real model answer.
- D-05, D-09, D-10, D-11, and D-12 are implemented exactly; no automatic paid-provider fallback is added.
</success_criteria>

<output>
After completion, create `.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-02-SUMMARY.md`.
</output>
