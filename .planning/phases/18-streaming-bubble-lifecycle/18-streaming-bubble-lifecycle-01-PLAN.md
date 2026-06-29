---
phase: 18-streaming-bubble-lifecycle
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingBubbleLifecycleTest.java
autonomous: true
requirements: [STRM-01, STRM-02, STRM-03, STRM-04]
must_haves:
  truths:
    - "User sees one pending assistant bubble after a run identity exists."
    - "Non-empty model.delta text appends in order to that same assistant bubble."
    - "Replay duplicates by event sequence or eventId do not duplicate visible assistant text."
    - "Tool/status/approval/runtime events do not become primary assistant prose."
    - "Terminal complete/failed/cancelled/partial states update the existing assistant bubble instead of creating blank messages."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java"
      provides: "Idempotent RunEventDto-to-chat-bubble reducer keyed by sessionId/runId/stepId"
      exports: ["ConversationEventReducer"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java"
      provides: "Live assistant bubble API with stable selectors and stream state mutation"
      contains: "beginAssistantMessage"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingBubbleLifecycleTest.java"
      provides: "Fast component/reducer coverage for pending, delta, dedupe, terminal, and secondary-card routing"
      min_lines: 160
  key_links:
    - from: "ConversationEventReducer"
      to: "ChatEventStreamPanel"
      via: "typed UI operations, not raw RunEventRenderer assistant text"
      pattern: "beginAssistantMessage|appendAssistantDelta|markAssistantTerminal"
    - from: "RunEventDto.eventId/sequence"
      to: "ConversationEventReducer dedupe state"
      via: "per-run rendered event identity guard"
      pattern: "eventId|sequence"
    - from: "RunEventDto.type=model.delta"
      to: "assistant primary bubble"
      via: "non-empty payload text/textDelta/delta/content only"
      pattern: "model\\.delta"
---

<objective>
Create the Phase 18 reducer and ChatEventStreamPanel live-bubble contract before wiring any runtime stream transport.

Purpose: STRM-01 through STRM-04 require deterministic same-bubble aggregation and dedupe semantics that cannot depend on the current `activeAssistantLine` heuristic. This plan implements the interface-first foundation required by later Console push/SSE and cancellation plans while preserving Phase 17 typed transcript restore.

Output: A reusable adapter-web reducer, formal live assistant bubble APIs, and focused Java tests proving pending/delta/dedupe/terminal behavior.
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
@.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-05-SUMMARY.md
@.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-06-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/event/RunEventDto.java

<interfaces>
From `RunEventDto`:
```java
public record RunEventDto(
        String eventId, String tenantId, String userId, String sessionId,
        String runId, String stepId, String workspaceId, long sequence,
        Instant timestamp, String type, String traceId, String correlationId,
        String causationId, String visibility, RedactionDto redaction,
        String payloadSchema, int payloadVersion, Map<String, Object> payload) {}
```

From `ChatEventStreamPanel` current Phase 17 contract:
```java
public void appendUserMessage(String text);
public void appendEvent(RunEventRenderer.RenderedEvent event);
public void replaceTranscript(List<ConversationMessageDto> transcriptMessages);
public void showComposerRunStatus(String status, boolean cancellable);
public int messageCount();
public List<String> messages();
public int componentCount();
```

Required new live-bubble contract for downstream plans:
```java
public void beginAssistantMessage(String sessionId, String runId, String stepId);
public void appendAssistantDelta(String sessionId, String runId, String stepId, String delta);
public void markAssistantTerminal(String sessionId, String runId, String stepId, ConversationMessageStatus status, String safeSummary);
public void showErrorBubble(String sessionId, String runId, String stepId, String safeSummary);
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add live assistant bubble API to ChatEventStreamPanel</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingBubbleLifecycleTest.java</files>
  <behavior>
    - Test 1: `beginAssistantMessage("session-1", "run-1", "step-1")` creates exactly one primary assistant bubble with `data-message-role="assistant"`, `data-session-id="session-1"`, `data-run-id="run-1"`, `data-stream-state="pending"`, and pending label text.
    - Test 2: two `appendAssistantDelta(...)` calls for the same session/run/step append visible text to the same component and do not increase primary assistant bubble count.
    - Test 3: `markAssistantTerminal(..., COMPLETED, null)` changes `data-stream-state`/`data-message-status` to `completed` without creating a blank message; FAILED/CANCELLED/PARTIAL show safe non-prose status text or chip on the same bubble.
  </behavior>
  <action>
    Extend `ChatEventStreamPanel` with formal live assistant methods per Phase 18 decisions D-05, D-07, D-11, D-12, and D-14. Maintain a map keyed by `sessionId + runId + stepId-or-default` instead of relying on `activeAssistantLine`; preserve Phase 17 `replaceTranscript(...)` behavior by clearing live state on transcript replacement and new user messages. Add stable selectors: `data-message-role="assistant"`, `data-message-kind="primary-bubble"`, `data-session-id`, `data-run-id`, `data-step-id` when present, `data-stream-state`, `data-message-status`, and `data-stream-aggregation-key`. Add readable pending/streaming/failed/cancelled/partial fallback labels through the existing bundle fallback pattern; do not add provider/model readiness or multi-turn context copy because those are deferred.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingBubbleLifecycleTest test</automated>
  </verify>
  <done>Pending assistant bubble creation, same-component delta append, and terminal state mutation are covered by failing-then-passing component tests with stable selectors.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create idempotent ConversationEventReducer</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingBubbleLifecycleTest.java</files>
  <behavior>
    - Test 1: duplicate replay of the same `eventId` or an event sequence already rendered for a run produces no second append operation.
    - Test 2: `model.delta` events with `text`, `textDelta`, `delta`, or `content` payload fields append in sequence; blank finish chunks do not create messages.
    - Test 3: tool, approval, policy, and generic runtime events are returned as secondary-card/runtime operations, not assistant-delta operations.
    - Test 4: `run.completed`, `run.failed`, and `run.cancelled` produce terminal operations for the existing assistant bubble.
  </behavior>
  <action>
    Add `ConversationEventReducer` in the adapter-web Console package. It should own per-run reducer state: aggregation key (`sessionId + runId + stepId/default`), highest rendered sequence, rendered event ID set, stopped/terminal keys, and optional buffered text metadata for later coalescing. Expose a small nested operation record/enum such as `BEGIN_ASSISTANT`, `APPEND_ASSISTANT_DELTA`, `MARK_TERMINAL`, `SECONDARY_EVENT`, and `IGNORE`. Use sequence/eventId dedupe per D-08; never dedupe by text content. Route only non-empty `model.delta` text into assistant delta per D-06. Treat terminal/failure/cancel as state transitions per D-07/D-12. Once a key is cancelled/failed/terminal, later deltas for that key must reduce to `IGNORE`.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingBubbleLifecycleTest test</automated>
  </verify>
  <done>Reducer produces deterministic operations for model deltas, terminal events, duplicates, and secondary operational events without referencing Vaadin types outside adapter-web.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Apply reducer operations to panel in test harness</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConversationEventReducer.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleStreamingBubbleLifecycleTest.java</files>
  <behavior>
    - Test 1: reduce/apply sequence pending + `A` + duplicate `A` + tool event + `B` + completed leaves one assistant bubble with exact text `AB` and one secondary card/component.
    - Test 2: failed/provider-error terminal marks the bubble failed and renders only a safe status/summary, not raw payload maps or secret-like metadata.
    - Test 3: cancelled terminal preserves partial text and ignores a later delta event.
  </behavior>
  <action>
    Add a reducer operation application helper only if it belongs naturally with the reducer/panel API (e.g. `ConversationEventReducer.apply(operation, chatPanel, runEventRenderer)`) or keep it in tests if later Console wiring should own application. Reuse existing `RunEventRenderer` only for `SECONDARY_EVENT` operations so tool/approval/runtime cards remain compact and redacted per D-06 and Phase 13. Do not wire `ConsoleView` in this plan; that belongs to downstream plans to avoid shared-file conflicts.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingBubbleLifecycleTest test</automated>
  </verify>
  <done>End-to-end reducer-to-panel tests prove one-bubble streaming semantics independent of polling, Push, or SSE transport.</done>
</task>

</tasks>

<verification>
Run the focused Java gate and ensure no existing Phase 17 transcript tests regress:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingBubbleLifecycleTest,WebConsoleTranscriptHydrationTest test
```
</verification>

<success_criteria>
- `ConversationEventReducer` exists and dedupes by sequence/eventId, not text.
- `ChatEventStreamPanel` exposes live assistant APIs and selectors required by Phase 18 D-04 through D-15.
- Tool/status/approval/runtime events remain secondary cards/details and do not fragment assistant prose.
- Failed/cancelled/partial terminal state is visible on the existing assistant bubble and safe from raw payload leakage.
</success_criteria>

<output>
After completion, create `.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-01-SUMMARY.md`.
</output>
