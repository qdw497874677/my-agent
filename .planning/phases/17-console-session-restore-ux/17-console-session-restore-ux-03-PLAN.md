---
phase: 17-console-session-restore-ux
plan: 03
type: execute
wave: 2
depends_on:
  - 17-console-session-restore-ux-01
  - 17-console-session-restore-ux-02
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java
autonomous: true
requirements:
  - CIA-02
  - SESS-03
must_haves:
  truths:
    - "User sees whether the chat is a new conversation or continuing a selected historical session."
    - "Selecting a historical session clears the current feed, hydrates typed transcript bubbles, highlights the session card, returns mobile users to Chat, and scrolls/restores near the latest message where practical."
    - "Sending after selecting a historical session appends the run to that selected session instead of creating a new session."
    - "User can explicitly exit continuation mode with a New Conversation action."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Active-session banner, restore orchestration, selected-session continuation"
      contains: "selectSession"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java"
      provides: "Component tests for banner, restore sequencing, and same-session send"
      min_lines: 100
  key_links:
    - from: "ConsoleView.selectSession(sessionId)"
      to: "ConsoleRunExecutionBridge.getTranscript(sessionId, limit, cursor)"
      via: "typed transcript bridge call"
      pattern: "getTranscript"
    - from: "ConsoleView.planChatSubmission(text)"
      to: "ConsoleRunExecutionBridge.createRun(selectedSessionId, request)"
      via: "selectedSessionId is reused when non-null"
      pattern: "needsSession"
    - from: "Active-session banner"
      to: "Chat panel state"
      via: "New conversation vs Continue title state"
      pattern: "data-role.*active-session"
---

<objective>
Wire the Phase 17 restore UX in `ConsoleView`: active-session identity, transcript restore sequence, and same-session continuation.

Purpose: The Console must feel like a chat product rather than a run workbench. This plan consumes Plan 01 history cards and Plan 02 typed hydration to implement D-02/D-05/D-06/D-07/D-08/D-14/D-15 without adding Phase 18 streaming aggregation or Phase 19 model context assembly.

Output: Active-session banner/chip, New Conversation escape action, selected-session restore orchestration, and same-session `createRun` proof.
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
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-04-SUMMARY.md
@.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-06-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java

<interfaces>
Use the outputs from Plans 01 and 02:
```java
sessionListPanel.showRecentSessions(List<SessionSummaryDto> summaries, String selectedSessionId, boolean hasMore);
chatPanel.replaceTranscript(List<ConversationMessageDto> transcriptMessages);
```

Existing bridge methods:
```java
PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor);
ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor);
RunResponse createRun(String sessionId, CreateRunRequest request);
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add active-session banner and New Conversation escape</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java</files>
  <behavior>
    - Test 1: a new ConsoleView starts with a visible banner/chip containing `New conversation` and `data-active-session-state="new"`.
    - Test 2: after selecting a historical session titled `Stable Title`, the banner changes to `Continue: Stable Title` and `data-active-session-state="continued"`.
    - Test 3: the continued banner includes a `data-action="new-conversation"` button/action that clears selected-session continuation and returns banner state to `New conversation`.
  </behavior>
  <action>Add a compact active-session banner/chip near the top of the Chat area per D-05/D-06. Use the Phase 16 stable `SessionSummaryDto.title()` for continued title per D-08; do not implement rename or auto-retitle behavior. Include an explicit New Conversation action only when a historical session is selected per D-07. The action must clear `selectedSessionId`, active transcript/feed state as appropriate, de-highlight session cards, and leave the user in Chat. Add stable selectors such as `data-role="active-session-banner"`, `data-active-session-state="new|continued"`, and `data-action="new-conversation"`.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest test</automated>
  </verify>
  <done>Users can visibly distinguish new vs continued conversation and can exit continuation mode without hidden session reuse.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Orchestrate historical session selection and typed transcript restore</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java</files>
  <behavior>
    - Test 1: `selectSession("session-old")` calls `getTranscript("session-old", ...)` and then `chatPanel.replaceTranscript(...)` with typed messages.
    - Test 2: selection sequence clears the previous current feed before rendering restored messages.
    - Test 3: selected session card has `data-session-active="true"`, active banner shows `Continue: {title}`, and `activeConsolePanel()` is `chat` after selection per D-02.
    - Test 4: returned `ConversationTranscriptResponse.activeRunId()` restores `activeRunId` and `activeRunNextAfterSequence` cursor when present, without implementing Phase 18 delta aggregation.
  </behavior>
  <action>Update `selectSession(...)` to follow D-14 exactly: load typed transcript through `ConsoleRunExecutionBridge.getTranscript(...)` (D-09), clear/hydrate via `chatPanel.replaceTranscript(...)`, set/update selected active-session state/banner, highlight the selected `SessionListPanel` card, return to Chat for mobile/phone layouts via `showConsolePanel("chat")` (D-02), and restore active run/cursor if the transcript response exposes an active run. Preserve the old `SessionSelectionPlan.historyPath()` compatibility return if tests still assert it, but do not use raw `/history` maps for rendering.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest,WebConsoleConversationReadModelHookTest test</automated>
  </verify>
  <done>Selecting a historical session restores typed bubbles/cards, updates identity UI, highlights the selected card, and returns users to Chat.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Ensure send continues selected session and does not duplicate-create sessions</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java</files>
  <behavior>
    - Test 1: after selecting `session-old`, `planChatSubmission("follow up")` calls `createRun("session-old", request)` and does not call `createSession()`.
    - Test 2: after New Conversation action, `planChatSubmission("fresh")` calls `createSession()` and then creates a run in the new session.
    - Test 3: selected session remains highlighted after a same-session send, and session list metadata updates without losing the stable title unless the new run creates a truly new session.
  </behavior>
  <action>Harden `planChatSubmission(...)` continuation semantics per D-15: reuse `selectedSessionId` when present and only create a new session when no historical/current session is selected. Ensure the New Conversation action is the explicit way to exit selected-session continuation mode (D-07). Continue using the existing run/event/polling rendering path for live feedback and do not introduce pending assistant bubble lifecycle or delta aggregation (D-16/D-17). If an active run overlap guard already exists, preserve it; if not, do not add broad App-layer guard here unless a focused component test requires preventing duplicate UI submits.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>Follow-up sends after restore append to the selected session; fresh sends after New Conversation create a new session.</done>
</task>

</tasks>

<verification>
Run focused component tests for session restore UX plus existing mobile-flow contracts. Confirm no Phase 18/19 deferred streaming/context work was implemented.
</verification>

<success_criteria>
- Active-session banner states are visible and testable.
- Historical selection clears/restores typed transcript and returns to Chat.
- Follow-up send uses the selected session ID; New Conversation is the only reset path.
</success_criteria>

<output>
After completion, create `.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-03-SUMMARY.md`
</output>
