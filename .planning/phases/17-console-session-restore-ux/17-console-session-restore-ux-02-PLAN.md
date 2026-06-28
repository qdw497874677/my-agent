---
phase: 17-console-session-restore-ux
plan: 02
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java
autonomous: true
requirements:
  - SESS-02
  - CIA-03
  - CIA-04
must_haves:
  truths:
    - "User and assistant transcript messages restore as primary chat bubbles in their original order."
    - "Tool and error transcript items remain visible as compact secondary cards/status items, not discarded or equal conversational prose."
    - "Restored transcript DOM exposes role/session/run/status metadata selectors for Phase 18 and browser tests."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java"
      provides: "Typed transcript hydration API and message/card selectors"
      contains: "replaceTranscript"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java"
      provides: "Component tests for typed transcript bubbles/cards"
      min_lines: 80
  key_links:
    - from: "ChatEventStreamPanel.replaceTranscript(...)"
      to: "ConversationMessageDto"
      via: "typed role/status/session/run rendering"
      pattern: "ConversationMessageDto"
    - from: "Restored message components"
      to: "Phase 18 streaming reducer"
      via: "data-message-role/data-session-id/data-run-id/data-message-status/data-stream-state"
      pattern: "data-message-role"
---

<objective>
Create the formal typed transcript hydration surface for restored conversation bubbles.

Purpose: Phase 17 must restore persisted typed transcript messages from Phase 16 instead of Vaadin in-memory strings or raw runtime maps (D-09, D-10, D-11, D-12). This plan keeps full live streaming aggregation deferred to Phase 18 while laying the bubble/card selector foundation required by downstream tests.

Output: `ChatEventStreamPanel.replaceTranscript(...)` with stable role/session/run/status selectors and compact tool/error card rendering.
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
@.planning/phases/17-console-session-restore-ux/17-CONTEXT.md
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-02-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageRole.java
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageStatus.java

<interfaces>
Existing contracts to use directly:

From `ChatEventStreamPanel.java`:
```java
public void appendUserMessage(String text);
public void appendEvent(RunEventRenderer.RenderedEvent event);
public List<String> messages();
public void replaceTranscriptForProof(List<ConversationMessageDto> transcriptMessages);
```

From Phase 16 DTO usage:
```java
ConversationMessageDto(messageId, sessionId, runId, stepId, role, text, status, createdAt, updatedAt, firstSequence, lastSequence, metadata, visible, redacted)
ConversationMessageRole.USER / ASSISTANT / TOOL / ERROR
ConversationMessageStatus.COMPLETED / FAILED / CANCELLED / PARTIAL / PENDING
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add typed transcript bubble hydration API</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java</files>
  <behavior>
    - Test 1: `replaceTranscript(List.of(user, assistant))` clears the old feed and renders exactly two visible message elements in order.
    - Test 2: user bubbles are right-aligned primary bubbles; assistant bubbles are left-aligned quiet bubbles; `messages()` returns the restored text in order for compatibility.
    - Test 3: every restored message element exposes `data-message-role`, `data-session-id`, `data-run-id` when present, `data-message-status`, and `data-stream-state`.
    - Test 4: calling `replaceTranscript(List.of())` restores the empty state and clears old messages/components.
  </behavior>
  <action>Introduce `replaceTranscript(List&lt;ConversationMessageDto&gt; transcriptMessages)` as the formal API required by D-11. Keep `replaceTranscriptForProof(...)` as a delegating compatibility helper only. Ensure the method clears the feed, messages list, event components, and active assistant line before rendering (D-14). Render only typed DTOs; do not inspect `SessionHistoryResponse`, raw `RunEventDto` maps, or `ChatEventStreamPanel.messages()` as source-of-truth (D-09). Add stable selectors per D-12, including `data-message-role`, `data-session-id`, `data-run-id` when non-null, `data-message-status`, and `data-stream-state` with status-like values. Do not implement pending/delta/terminal mutation semantics; that is deferred to Phase 18 (D-16/D-17).</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleTranscriptHydrationTest test</automated>
  </verify>
  <done>Typed user/assistant transcript messages hydrate as primary bubbles with role/session/run/status selectors and old feed content is cleared.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Render tool and error transcript items as compact secondary cards</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java</files>
  <behavior>
    - Test 1: TOOL messages render as compact inline cards with `data-message-role="tool"` and do not become assistant prose.
    - Test 2: ERROR/FAILED messages render as compact status/error cards with visible abnormal status and `data-message-status`.
    - Test 3: completed status is visually quiet while failed/cancelled/partial statuses expose a visible status chip/text.
    - Test 4: redacted/metadata details are not dumped as raw JSON in primary prose.
  </behavior>
  <action>Implement compact card/status rendering for `TOOL` and `ERROR` roles per D-10/D-13/D-18/D-19. Use lightweight Vaadin components and existing redaction discipline: show safe text/summary/status only; keep detailed runtime/tool/provider diagnostics secondary and reachable later through existing cards/expanders rather than dumping metadata in chat prose. Preserve `eventComponents` accounting for secondary card components if existing tests rely on it.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleTranscriptHydrationTest,WebConsoleConversationReadModelHookTest test</automated>
  </verify>
  <done>Tool/error restored transcript items remain visible and secondary, with stable selectors and abnormal statuses visible.</done>
</task>

</tasks>

<verification>
Run focused adapter-web transcript hydration tests. Confirm no Phase 18 live streaming reducer, delta coalescing, or pending assistant bubble lifecycle is introduced in this plan.
</verification>

<success_criteria>
- `replaceTranscript(...)` is the formal typed hydration API and clears prior feed state.
- User/assistant/tool/error transcript items render according to D-10 with stable selectors.
- Compatibility proof hook delegates to the formal API.
</success_criteria>

<output>
After completion, create `.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-02-SUMMARY.md`
</output>
