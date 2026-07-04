---
phase: 20-provider-model-and-local-profile-stability
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigController.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/resources/messages.properties
  - pi-agent-adapter-web/src/main/resources/messages_zh.properties
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java
autonomous: true
requirements:
  - PROV-01
  - PROV-02
must_haves:
  truths:
    - "User can see provider/model readiness in the compact Console model area."
    - "Refreshing models visibly distinguishes success, empty, and error states."
    - "Provider errors shown in Console are actionable and redacted, never silently swallowed."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigController.java"
      provides: "Provider readiness/model refresh response with success, empty, and error state"
      contains: "ModelListResponse"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Compact model bar status row with stable selectors"
      contains: "data-role\", \"provider-status"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java"
      provides: "Component tests for readiness and refresh state selectors"
      min_lines: 80
  key_links:
    - from: "ConsoleView#createModelBar"
      to: "ProviderConfigController#listModels"
      via: "refresh button click"
      pattern: "refreshModels.addClickListener"
    - from: "ProviderConfigController#listModels"
      to: "Console model bar refresh status"
      via: "ModelListResponse state fields"
      pattern: "state|error|models"
---

<objective>
Create the compact provider/model readiness and model-refresh feedback surface for Phase 20.

Purpose: users must know whether a real provider is ready and what happened when models are refreshed, without turning the Kimi-style Console into an operations dashboard.
Output: redacted readiness/refresh response shape, Console model-bar status rendering, i18n copy, and fast component tests.
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
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfig.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigController.java
@pi-agent-adapter-web/src/main/resources/messages.properties

<interfaces>
Existing contracts to preserve:
```java
public record ProviderConfig(boolean enabled, String baseUrl, String apiKey, String modelId, String providerId, String completionsPath) {
    public boolean isReady() { return enabled && apiKey != null && !apiKey.isBlank(); }
    public ProviderConfig masked();
}

// ConsoleView existing stable hooks:
modelSelector.getElement().setAttribute("data-role", "model-selector");
refreshModels.getElement().setAttribute("data-action", "refresh-models");
status.getElement().setAttribute("data-role", "provider-status");
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add explicit refresh/readiness response states</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigController.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java</files>
  <behavior>
    - Test 1: not-ready provider returns an empty model list with state `not_configured` and a safe action message, not an exception.
    - Test 2: ready provider returning models yields state `success`, model count, and no error.
    - Test 3: ready provider returning no model ids yields state `empty` with an explanatory safe message.
    - Test 4: provider HTTP/client failure yields state `error` with a redacted/truncated summary that does not contain API keys or bearer tokens.
  </behavior>
  <action>Extend `ProviderConfigController.ModelListResponse` from the current `(models, error)` shape into an explicit response that preserves existing callers while adding `state`, `message`, `modelCount`, `ready`, `selectedModel`, and `providerId` fields. Implement states per D-02: `success`, `empty`, `error`, and `not_configured`. Continue using `ProviderConfig.masked()` semantics; never include `apiKey`, bearer headers, or raw response bodies in messages. Remove unused `modelsUrl` if still present. Do not add a new provider SDK or change REST path names.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=WebConsoleProviderModelBarTest test</automated>
  </verify>
  <done>`ProviderConfigController.listModels()` reports success/empty/error/not_configured explicitly, with safe strings and backwards-compatible model list access.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Render compact model-bar feedback with stable selectors</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/resources/messages.properties, pi-agent-adapter-web/src/main/resources/messages_zh.properties, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java</files>
  <behavior>
    - Test 1: initial Console model area exposes `data-role="provider-status"`, `data-role="model-selector"`, and `data-action="refresh-models"` per D-03.
    - Test 2: ready config renders selected provider/model readiness in a compact status row per D-01.
    - Test 3: refresh success, empty, and error set `data-refresh-state` and visible localized copy.
    - Test 4: redacted provider error copy is visible and actionable but does not dump raw exception details.
  </behavior>
  <action>Replace the silent `catch (Exception ignored)` in `ConsoleView#createModelBar()` with a small model status row/bar. Keep the existing compact model area per D-01; do not add a large operational panel. Add stable hooks as needed: `data-role="provider-status"`, `data-role="model-refresh-status"`, `data-refresh-state`, `data-provider-ready`, and keep existing `model-selector`/`refresh-models` hooks per D-03. Use localized messages for ready/not configured/refresh success/empty/error/action guidance per D-02 and D-04. The UI may use spans/chips only; avoid dialogs, notifications as the only feedback, or deferred Admin-only visibility.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=WebConsoleProviderModelBarTest test</automated>
  </verify>
  <done>Console model bar visibly reports readiness and refresh outcomes with stable selectors and no silent refresh failure path.</done>
</task>

</tasks>

<verification>
Run the focused adapter-web test after both tasks. If a broader compile is needed because record signatures changed, run `mvn -pl pi-agent-adapter-web -am -DskipTests compile`.
</verification>

<success_criteria>
- PROV-01: readiness and actionable errors are visible from the Console model area.
- PROV-02: model refresh has explicit success/empty/error/not_configured states.
- D-01 through D-04 are implemented without deferred search/rename/archive/provider-routing scope.
</success_criteria>

<output>
After completion, create `.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-01-SUMMARY.md`.
</output>
