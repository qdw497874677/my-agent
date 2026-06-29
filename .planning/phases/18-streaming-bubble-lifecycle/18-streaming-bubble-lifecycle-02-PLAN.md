---
phase: 18-streaming-bubble-lifecycle
plan: 02
type: execute
wave: 2
depends_on: [18-streaming-bubble-lifecycle-01]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiWebAppShell.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleLiveRunEventSubscriber.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleLiveStreamingPushTest.java
autonomous: true
requirements: [STRM-01, STRM-02, STRM-04]
must_haves:
  truths:
    - "Product streaming path is distinguishable from polling fallback through `data-stream-mode`."
    - "Console subscribes to live run events after replaying already persisted events."
    - "Live events update the Vaadin UI through `UI.access(...)` and unregister on detach/terminal."
    - "Polling remains only a fallback/test seam and is labeled as `polling-fallback`."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiWebAppShell.java"
      provides: "Vaadin Push application shell configuration"
      contains: "@Push"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleLiveRunEventSubscriber.java"
      provides: "Adapter-web live subscription bridge using SseRunEventFanout plus UI.access cleanup"
      exports: ["ConsoleLiveRunEventSubscriber"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Console wiring from run creation to replay-before-subscribe live bubble updates"
      contains: "data-stream-mode"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleLiveStreamingPushTest.java"
      provides: "Focused Push/live subscription wiring tests"
      min_lines: 140
  key_links:
    - from: "ConsoleView.planChatSubmission"
      to: "ChatEventStreamPanel.beginAssistantMessage"
      via: "run identity after createRun succeeds"
      pattern: "beginAssistantMessage"
    - from: "SseRunEventFanout.subscribe"
      to: "ConsoleView live reducer application"
      via: "ConsoleLiveRunEventSubscriber with UI.access"
      pattern: "UI\\.access"
    - from: "ConsoleView.refreshActiveRunEvents"
      to: "polling fallback mode"
      via: "explicit data-stream-mode=polling-fallback"
      pattern: "polling-fallback"
---

<objective>
Wire the reducer and bubble API into a real product streaming path using Vaadin Push and the existing run-event fanout, while keeping the old polling loop only as an explicit fallback.

Purpose: Phase 18 decisions D-01 through D-03 require Push or explicit SSE as the product path and forbid calling the current 750ms poll loop real streaming. This plan implements Vaadin Push + `UI.access(...)` over the existing `SseRunEventFanout` and labels stream mode for browser and component tests.

Output: Push-enabled app shell, a live Console subscription bridge, Console run submission wiring, and focused tests for replay-before-subscribe/live subscription/mode hooks.
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
@.planning/research/ARCHITECTURE.md
@.planning/research/PITFALLS.md
@.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-01-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiWebAppShell.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/SseRunEventFanout.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/SseSubscription.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/EventStreamClient.java

<interfaces>
From `SseRunEventFanout`:
```java
public SseSubscription subscribe(String runId, Consumer<RunEventDto> consumer);
public void unsubscribe(String runId, String subscriberId);
public void publish(RunEvent event);
public int subscriberCount(String runId);
```

From `ConsoleRunExecutionBridge`:
```java
SessionResponse createSession();
RunResponse createRun(String sessionId, CreateRunRequest request);
EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence);
RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request);
```

Vaadin Push guidance validated during planning: server-driven background updates must capture a live `UI`, mutate components inside `ui.access(...)`, and unregister/avoid stale UI references on detach. The existing `EventStreamClient.ConnectionSpec` remains useful as public SSE metadata but is not currently consumed by Vaadin.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Enable Vaadin Push and add live subscriber bridge</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiWebAppShell.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleLiveRunEventSubscriber.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleLiveStreamingPushTest.java</files>
  <behavior>
    - Test 1: `PiWebAppShell` carries Vaadin `@Push` so the product path is server push, not only polling.
    - Test 2: `ConsoleLiveRunEventSubscriber` subscribes to `SseRunEventFanout` for one run, dispatches events through a provided handler inside `UI.access(...)`, and closes the subscription on detach/close.
    - Test 3: terminal run events close the live subscription exactly once.
  </behavior>
  <action>Add `@Push` to `PiWebAppShell` using Vaadin Flow push configuration. Create `ConsoleLiveRunEventSubscriber` in adapter-web Console package. It should accept `SseRunEventFanout` optionally, capture `UI.getCurrent()` when Console is attached, subscribe by runId, call a provided `Consumer&lt;RunEventDto&gt;` via `ui.access(...)`, and clean up with `SseSubscription.close()` on detach, terminal event, or explicit close. Keep Vaadin/Push types out of App, Domain, client DTOs, and provider modules.</action>
  <verify><automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleLiveStreamingPushTest test</automated></verify>
  <done>Vaadin Push is configured and the live subscriber bridge proves UI.access and cleanup semantics with focused tests.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Wire Console submission to pending bubble and live stream mode</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleLiveStreamingPushTest.java</files>
  <behavior>
    - Test 1: after `createRun(...)` succeeds, `planChatSubmission(...)` begins a pending assistant bubble keyed to the returned session/run before any delta arrives.
    - Test 2: a Console with live subscriber/fanout exposes `data-stream-mode="push"`; a Console without live subscriber support exposes `data-stream-mode="polling-fallback"`.
    - Test 3: replayed events from `listEvents(..., afterSequence=0)` are applied before live subscription accepts later events.
  </behavior>
  <action>Update `ConsoleView` to own a `ConversationEventReducer` from Plan 01 and apply operations to `ChatEventStreamPanel`. In `planChatSubmission(...)`, append the user message, create/reuse session, create run, then immediately call `chatPanel.beginAssistantMessage(sessionId, runId, defaultStep)` per D-05. Set a root or chat-panel attribute `data-stream-mode="push"` when the live subscriber bridge is active; set `polling-fallback` only when unavailable. Preserve `eventStreamClient.runEventStream(...)` in the returned plan for public route metadata, but do not rely on it as the Vaadin live path.</action>
  <verify><automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleLiveStreamingPushTest,WebConsoleSessionRestoreUxTest test</automated></verify>
  <done>Run submission creates a pending assistant bubble promptly after run identity exists and exposes a stream-mode hook distinguishing Push from fallback polling.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Route live and fallback events through the reducer</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleLiveStreamingPushTest.java</files>
  <behavior>
    - Test 1: live fanout events append to the existing pending assistant bubble, not generic `RunEventRenderer` assistant rows.
    - Test 2: fallback `refreshActiveRunEvents()` applies the same reducer and dedupe state, so replay after live delivery does not duplicate text.
    - Test 3: secondary tool/runtime events still render through `RunEventRenderer` compact cards.
  </behavior>
  <action>Replace direct `runEventRenderer.render(event)` primary assistant handling in `appendRunEvents(...)` with Plan 01 reducer operation application. Both live subscriber events and polling fallback events must share the same reducer instance and cursor state. `model.delta` operations update the assistant bubble; `SECONDARY_EVENT` operations continue using `RunEventRenderer` and `chatPanel.appendEvent(...)`; ignored duplicates do nothing. Do not implement Phase 19 multi-turn context or Phase 20 provider/model labels here.</action>
  <verify><automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleLiveStreamingPushTest,WebConsoleStreamingBubbleLifecycleTest test</automated></verify>
  <done>Live and fallback event paths use one reducer, maintain one assistant bubble, and share dedupe/cursor behavior.</done>
</task>

</tasks>

<verification>
Run focused Push/reducer/restore gates:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleLiveStreamingPushTest,WebConsoleStreamingBubbleLifecycleTest,WebConsoleSessionRestoreUxTest test
```
</verification>

<success_criteria>
- Product path has Vaadin Push enabled and tested with `UI.access(...)` and detach/terminal cleanup.
- Console exposes `data-stream-mode="push"` for live subscription and `polling-fallback` only for fallback/demo mode.
- Pending assistant bubble begins immediately after run creation succeeds.
- Live and polling fallback events share reducer/dedupe semantics.
</success_criteria>

<output>
After completion, create `.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-02-SUMMARY.md`.
</output>
