---
phase: 18-streaming-bubble-lifecycle
plan: 03
type: execute
wave: 3
depends_on: [18-streaming-bubble-lifecycle-02]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/main/resources/messages.properties
  - pi-agent-adapter-web/src/main/resources/messages_zh.properties
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingCancellationTest.java
autonomous: true
requirements: [STRM-03, STRM-05]
must_haves:
  truths:
    - "Cancelling an active response immediately marks the assistant bubble stopped/partial/cancelled."
    - "Already-generated assistant text remains visible after cancel."
    - "Deltas arriving after cancellation do not mutate the stopped bubble."
    - "Provider/runtime failure marks the bubble failed and shows a safe redacted status/error card, not raw payload prose."
    - "Terminal transitions force-flush buffered text before status becomes completed/failed/cancelled/partial."
  artifacts:
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingCancellationTest.java"
      provides: "Focused cancellation/failure/post-cancel suppression tests"
      min_lines: 140
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Cancel action connected to App/runtime seam and UI reducer stop state"
      contains: "planCancelRunningRun"
    - path: "pi-agent-adapter-web/src/main/resources/messages.properties"
      provides: "English streaming state labels"
      contains: "console.stream.cancelled"
    - path: "pi-agent-adapter-web/src/main/resources/messages_zh.properties"
      provides: "Chinese streaming state labels"
      contains: "console.stream.cancelled"
  key_links:
    - from: "ConsoleView.planCancelRunningRun"
      to: "ConsoleRunExecutionBridge.cancelRun"
      via: "existing App/runtime cancellation seam"
      pattern: "cancelRun"
    - from: "ConsoleView.planCancelRunningRun"
      to: "ConversationEventReducer stop state"
      via: "mark run cancelled before any late deltas are applied"
      pattern: "cancelled|PARTIAL"
    - from: "failed/cancelled terminal events"
      to: "ChatEventStreamPanel existing assistant bubble"
      via: "markAssistantTerminal safe status mutation"
      pattern: "markAssistantTerminal"
---

<objective>
Complete the cancellation, failure, and terminal-state semantics on top of the live reducer path.

Purpose: STRM-03 and STRM-05 require truthful stopped/partial/failed states, not CSS-only cancellation or generic run-status noise. The Console must call the runtime cancellation seam, immediately stop local aggregation for the run, preserve partial output, and suppress late provider deltas.

Output: Cancellation/failure reducer behavior, Console cancel wiring, localized stream state labels, and focused tests for post-cancel suppression and safe failure display.
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
@.planning/phases/18-streaming-bubble-lifecycle/18-CONTEXT.md
@.planning/research/PITFALLS.md
@.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-01-SUMMARY.md
@.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-02-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppConsoleRunExecutionBridge.java
@pi-agent-adapter-web/src/main/resources/messages.properties
@pi-agent-adapter-web/src/main/resources/messages_zh.properties

<interfaces>
Existing cancellation seam:
```java
RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request);
```

Existing Console cancel method to preserve and harden:
```java
public CancelPlan planCancelRunningRun(String reason) {
    runContextPanel.showCancelling();
    chatPanel.showComposerCancelling();
    RunStatusResponse response = executionBridge.cancelRun(selectedSessionId, activeRunId, request);
    applyRunStatus(response.status(), response.terminal());
    activeRunId = response.terminal() ? null : response.runId();
}
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Mark local stream stopped before and after cancel response</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingCancellationTest.java</files>
  <behavior>
    - Test 1: after two deltas append partial text, calling `planCancelRunningRun(...)` preserves the partial text and marks the assistant bubble `data-stream-state="cancelled"` or `partial` with stopped/cancelled copy.
    - Test 2: the reducer ignores a later `model.delta` for the cancelled run even if the event sequence is higher than the cancellation sequence.
    - Test 3: `ConsoleRunExecutionBridge.cancelRun(...)` is still called exactly once with the active session/run/reason; cancellation is not UI-only.
  </behavior>
  <action>Update cancellation flow per D-09 through D-11. Before or immediately after calling `executionBridge.cancelRun(...)`, mark the current aggregation key as stopping/stopped in `ConversationEventReducer` and update the existing assistant bubble to cancelled/partial. Preserve existing generated text and do not clear the bubble. Keep provider-level abort best-effort only: if current runtime/provider cannot truly abort the stream, document the limitation in comments/tests summary but still enforce App cancel call + UI/runtime suppression.</action>
  <verify><automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingCancellationTest test</automated></verify>
  <done>Cancel invokes the bridge, marks the current assistant bubble stopped/partial, preserves partial output, and blocks late deltas.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Render failed and partial terminal states safely</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingCancellationTest.java</files>
  <behavior>
    - Test 1: a `run.failed` or model/provider error event marks the active assistant bubble failed and renders a secondary error/status card with safe text.
    - Test 2: raw provider/tool/audit payload keys such as `apiKey`, `token`, `secret`, nested maps, or raw exception bodies are not inserted into assistant prose.
    - Test 3: terminal complete/failed/cancelled events force any buffered/coalesced delta text to appear before status mutation.
  </behavior>
  <action>Harden reducer terminal mapping and panel terminal rendering for D-07, D-12, D-14, and D-15. Safe summary extraction should prefer public fields like `message`, `reason`, `status`, or `errorCategory` and otherwise use generic localized failure text; never dump raw payload maps. If Plan 01 added buffering/coalescing state, force-flush it before `MARK_TERMINAL`; otherwise keep the contract explicit in reducer tests so future coalescing cannot reorder terminal status before visible text.</action>
  <verify><automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingCancellationTest,WebConsoleStreamingBubbleLifecycleTest test</automated></verify>
  <done>Failure and partial terminal states are safe, visible, and state-based; they do not produce raw provider prose or blank assistant messages.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Add synchronized streaming labels and selector assertions</name>
  <files>pi-agent-adapter-web/src/main/resources/messages.properties, pi-agent-adapter-web/src/main/resources/messages_zh.properties, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingCancellationTest.java</files>
  <behavior>
    - Test 1: English and Chinese bundles both contain `console.stream.pending`, `console.stream.streaming`, `console.stream.completed`, `console.stream.failed`, `console.stream.cancelled`, `console.stream.partial`, `console.stream.stopped`, and `console.stream.mode.*` labels.
    - Test 2: rendered cancelled/failed/partial labels use bundle fallback and do not show Vaadin missing-key markers under direct component construction.
  </behavior>
  <action>Add synchronized i18n keys for pending, streaming, completed, failed, cancelled, partial/stopped, safe error summary, and stream mode labels. Reuse the ResourceBundle fallback pattern established in Phase 17; do not add broad Console copy cleanup. Ensure selectors remain language-neutral (`data-stream-state`, `data-stream-mode`) and tests do not assert exact Chinese/English sentence text beyond key presence/readability.</action>
  <verify><automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingCancellationTest test</automated></verify>
  <done>Streaming terminal/cancel labels are synchronized in English/Chinese and direct component tests render readable labels without missing-key markers.</done>
</task>

</tasks>

<verification>
Run focused cancellation/failure and prior reducer gates:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingCancellationTest,WebConsoleStreamingBubbleLifecycleTest,WebConsoleLiveStreamingPushTest test
```
</verification>

<success_criteria>
- Cancelling calls the App/runtime cancellation seam and immediately marks/suppresses the UI reducer for the active run.
- Partial generated text remains visible after cancel; late deltas are ignored.
- Failed provider/runtime state is visible as failed + safe redacted status/error card, not assistant prose.
- Stream-state labels and selectors are stable and language-neutral.
</success_criteria>

<output>
After completion, create `.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-03-SUMMARY.md`.
</output>
