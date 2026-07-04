---
phase: 20-provider-model-and-local-profile-stability
plan: 04
type: execute
wave: 2
depends_on:
  - 20-provider-model-and-local-profile-stability-03
files_modified:
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandService.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/QueuedRun.java
  - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/DynamicAgentRuntime.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelResolutionFlowTest.java
  - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherProviderModelTest.java
autonomous: true
requirements:
  - PROV-03
  - PROV-04
  - PROV-05
must_haves:
  truths:
    - "New run creation uses the selected provider/model snapshot for the run being created."
    - "Dispatcher/runtime use the run snapshot instead of a stale hard-coded constructor default."
    - "Fallback/no-provider resolution facts are recorded safely and labeled for restored history."
  artifacts:
    - path: "pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/QueuedRun.java"
      provides: "Queued run carries safe run model metadata to the worker"
      contains: "provider"
    - path: "pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java"
      provides: "Dispatch-time modelRef derived from run metadata/resolution"
      contains: "modelRef"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/DynamicAgentRuntime.java"
      provides: "Explicit local fallback labeling and snapshot-aware model use"
      contains: "fallback"
  key_links:
    - from: "ConsoleView#createRun request metadata"
      to: "DefaultRunCommandService / QueuedRun"
      via: "safe selected provider/model facts"
      pattern: "modelRef|providerId|modelId"
    - from: "QueuedRun"
      to: "DefaultRunDispatcher AgentDefinition"
      via: "dispatch-time modelRef"
      pattern: "defaultAgentDefinition"
    - from: "DynamicAgentRuntime"
      to: "run events / transcript metadata"
      via: "ModelDeltaPayload provider/model/fallback facts"
      pattern: "local-dev:not-configured|fallback"
---

<objective>
Wire selected model snapshots through run creation, queueing, dispatch, and local runtime fallback labeling.

Purpose: the model chosen for a run must be the one used and recorded for that run, while fallback/no-provider states must be explicit and safe.
Output: run metadata propagation, dispatch-time modelRef selection, explicit fallback labels, and flow tests.
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
@.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-02-SUMMARY.md
@.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-03-SUMMARY.md
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/CreateRunRequest.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandService.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/QueuedRun.java
@pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/DynamicAgentRuntime.java
@pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderModelRef.java

<interfaces>
Relevant existing signatures:
```java
public record CreateRunRequest(String agentId, String inputType, Map<String,Object> input, String workspaceId, Map<String,Object> metadata) {}

public record QueuedRun(String runId, String sessionId, String tenantId, String userId,
        String workspaceId, String traceId, String correlationId, String inputType,
        Map<String,Object> input, Instant availableAt, int attemptCount) {}

// DefaultRunDispatcher currently validates and uses one agentDefinition.modelRef().
validateModelRef(agentDefinition.modelRef());
RunContext context = new RunContext(agentDefinition, runInput(queuedRun), ...);
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Carry selected model facts from Console run creation into the queued run</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandService.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/QueuedRun.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelResolutionFlowTest.java</files>
  <behavior>
    - Test 1: Console `CreateRunRequest.metadata()` includes selected model/provider facts from `ProviderConfigStore.current()` for a new run.
    - Test 2: `DefaultRunCommandService` persists those facts through the repository metadata path from Plan 03 and enqueues them on `QueuedRun`.
    - Test 3: changing the selector after run creation does not mutate the queued run snapshot.
  </behavior>
  <action>Implement D-05/D-06 by taking a safe selected provider/model snapshot at run creation time. The snapshot may be passed via `CreateRunRequest.metadata()` and normalized by `DefaultRunCommandService` into the Plan 03 metadata contract, then copied into `QueuedRun` for dispatch. Include selected/requested model ref, provider id, model id, and initial readiness/fallback state. Do not put `ProviderConfig` itself, API keys, headers, or adapter-web classes into App/Domain contracts. Ensure active run state is immutable with respect to later selector changes.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web,pi-agent-app -am -Dtest=RunProviderModelResolutionFlowTest test</automated>
  </verify>
  <done>New runs carry a safe selected model snapshot from creation through queued execution, independent of later selector changes.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Use run model snapshot in dispatcher/runtime and record fallback facts</name>
  <files>pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/DynamicAgentRuntime.java, pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherProviderModelTest.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelResolutionFlowTest.java</files>
  <behavior>
    - Test 1: dispatcher builds the run `AgentDefinition`/modelRef from the queued run snapshot rather than the hard-coded default.
    - Test 2: invalid or not-ready model resolution records safe readiness/error metadata and does not leak secrets.
    - Test 3: explicit local fallback mode emits fallback-labeled model delta metadata; default no-key remains blocked by Plan 02 product Console.
  </behavior>
  <action>Replace stale dispatch-time modelRef assumptions per D-06. Derive the effective modelRef for the current run from `QueuedRun`/Plan 03 metadata, falling back to the constructor default only for legacy tests that provide no snapshot. Update the run projection metadata when runtime resolution fails/succeeds if Plan 03 added an update method. In `DynamicAgentRuntime`, prefer the run snapshot over the latest mutable `ProviderConfigStore.current()` for model id/provider id where available. If explicit fallback runs are allowed for local/test, publish model delta payloads and/or assistant metadata with `fallbackMode=local` and `modelRef=local-dev:not-configured` per D-10/D-11. Do not implement automatic paid-provider fallback or provider routing per D-12.</action>
  <verify>
    <automated>mvn -pl pi-agent-infrastructure,pi-agent-adapter-web -am -Dtest=DefaultRunDispatcherProviderModelTest,RunProviderModelResolutionFlowTest test</automated>
  </verify>
  <done>Dispatcher/runtime use per-run selected model facts and record explicit safe fallback/readiness metadata.</done>
</task>

</tasks>

<verification>
Run focused flow tests. If `QueuedRun` signature changes break many fakes, run `mvn -pl pi-agent-app,pi-agent-infrastructure,pi-agent-adapter-web -am -DskipTests compile` and update test fakes with safe empty metadata maps only.
</verification>

<success_criteria>
- PROV-03: selector changes affect subsequent runs only.
- PROV-04: each run records actual requested/resolved provider/model/fallback facts.
- PROV-05: fallback path is clearly labeled and not confused with real provider output.
- D-06, D-07, D-10, D-11, and D-12 are implemented without provider SDK leakage.
</success_criteria>

<output>
After completion, create `.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-04-SUMMARY.md`.
</output>
