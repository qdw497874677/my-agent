---
phase: 12-console-mobile-first-flow
plan: 06
type: execute
wave: 6
depends_on: [12-console-mobile-first-flow-05]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppConsoleRunExecutionBridge.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java
  - e2e/phase-12-console-mobile-flow.spec.ts
  - docs/phase-12-console-mobile-flow.md
autonomous: true
gap_closure: true
requirements: [MCON-03, MCON-04, MVER-03]
must_haves:
  truths:
    - "Mobile user sees run events appended after createRun without another Send click or panel reset."
    - "Mobile user can open Sessions after sending and see a browser-visible session card for the created or continued session."
    - "Mobile user can select the visible session card, return to Chat, and keep the active session identity clear."
    - "MVER-03 proves incremental feed and session-card selection with stable data-* selectors, not empty-state fallbacks."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Live/bounded run event append orchestration and session-list population after send/select"
      contains: "appendRunEvents"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java"
      provides: "Fakeable bridge seam for later event replay/polling evidence without mobile-only APIs"
      contains: "listEvents"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java"
      provides: "Visible selectable session cards with active identity metadata"
      contains: "data-session-active"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java"
      provides: "Fast contracts for post-createRun event append and non-empty session history card flow"
      contains: "mcon03"
    - path: "e2e/phase-12-console-mobile-flow.spec.ts"
      provides: "Browser MVER-03 checks for incremental feed and real session-card selection"
      contains: "session-card"
  key_links:
    - from: "ConsoleView.java"
      to: "EventStreamClient / ConsoleRunExecutionBridge"
      via: "createRun starts live SSE or bounded replay loop from streamSpec/listEvents and appends later events"
      pattern: "runEventStream|listEvents"
    - from: "ConsoleView.java"
      to: "ChatEventStreamPanel.java"
      via: "rendered RunEventDto values are appended after initial run creation"
      pattern: "appendEvent"
    - from: "ConsoleView.java"
      to: "SessionListPanel.java"
      via: "create/continue/select session calls showSession/selectSession so Sessions panel is not empty"
      pattern: "showSession|selectSession"
    - from: "e2e/phase-12-console-mobile-flow.spec.ts"
      to: "SessionListPanel.java"
      via: "browser clicks visible [data-role=session-card] and asserts Chat active panel returns"
      pattern: "data-role=\"session-card\"|session-card"
---

<objective>
Close the remaining Phase 12 verification gaps: live/incremental run output and production-visible session history/continue flow.

Purpose: Plans 04 and 05 made Console controls actionable and created a DTO-backed run path, but verification found two roadmap truths still narrowed below Phase 12 scope. This plan makes run-event progression append after createRun without another user action, and makes the created/continued session visible and selectable in the Sessions panel.
Output: The Console mobile flow satisfies MCON-03, MCON-04, and MVER-03 with Java contracts plus strengthened browser assertions.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md
@.planning/phases/12-console-mobile-first-flow/12-RESEARCH.md
@.planning/phases/12-console-mobile-first-flow/12-VERIFICATION.md
@.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-04-SUMMARY.md
@.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-05-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/EventStreamClient.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppConsoleRunExecutionBridge.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java
@e2e/phase-12-console-mobile-flow.spec.ts

<interfaces>
<!-- Key contracts. Preserve public REST/SSE DTO boundaries and do not add /mobile/* APIs. -->

From `EventStreamClient.java`:
```java
public ConnectionSpec runEventStream(String sessionId, String runId, long afterSequence);
public record ConnectionSpec(String url, boolean withCredentials) {
    public String eventSourceExpression();
}
```

From `ConsoleRunExecutionBridge.java`:
```java
SessionResponse createSession();
RunResponse createRun(String sessionId, CreateRunRequest request);
EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence);
RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request);
```

From `SessionListPanel.java`:
```java
public void showSession(String sessionId, String title, Instant updatedAt);
public void selectSession(String sessionId);
public void setSessionActivationHandler(Consumer<String> sessionActivationHandler);
```

From `ChatEventStreamPanel.java`:
```java
public void appendEvent(RunEventRenderer.RenderedEvent event);
public int messageCount();
public List<String> messages();
```

Locked decisions implemented here: D-12 (selecting historical/current session returns to Chat and preserves identity), D-13 (vertical feed shows live/incremental output), D-15 (meaningful status/model/terminal/cancellation progression), D-16/D-18/D-20 (deterministic no-key browser evidence on stable data-* selectors). Do not implement deferred Phase 13 runtime/tool/approval card redesign, filtering, copy/share, or new mobile runtime capability.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Append post-createRun events through a live or bounded replay path</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppConsoleRunExecutionBridge.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <behavior>
    - Test 1: after `planChatSubmission(...)`, a fake bridge that returns no initial events but returns later events on a subsequent `listEvents(sessionId, runId, afterSequence)` call can be consumed by a ConsoleView live/replay hook and appended to `ChatEventStreamPanel` without another user action.
    - Test 2: the append hook tracks `nextAfterSequence` so duplicate calls do not append the same event twice.
    - Test 3: terminal later events update composer and Run Context status to non-cancellable terminal feedback.
  </behavior>
  <action>Add the smallest adapter-web-only live evidence path for MCON-03. Prefer a bounded replay/poll hook callable from Vaadin UI lifecycle/tests (for example `refreshActiveRunEvents()` / `appendLatestRunEvents()`) if full browser EventSource callback plumbing is too invasive in this gap closure; it must use the existing `EventStreamClient.runEventStream(...)`/`ConsoleRunExecutionBridge.listEvents(...)` seams and must append later events after createRun without another Send click. Track the last appended sequence per active run from `EventHistoryResponse.nextAfterSequence()` and avoid duplicate rendering. If adding browser EventSource JavaScript, keep it in `ConsoleView`/adapter-web only, use `UI.access` or Vaadin-safe callback mechanics, and close/replace prior active-run subscriptions when a new run starts or terminal status arrives. Do not create mobile-only REST/SSE endpoints or redesign RunEventRenderer card interiors.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>Later run events can be appended into the mobile feed after the run is created, duplicate replay is guarded by sequence, and terminal later events update both visible run-status surfaces.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Populate visible session cards after send and preserve continue-session identity</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <behavior>
    - Test 1: normal `ConsoleView` construction no longer leaves the Sessions panel as the only production path after a successful Send; sending a prompt creates or continues a session and adds a visible `[data-role=session-card]` row.
    - Test 2: the created/continued session card contains title/summary, status, updated time, and `data-session-active="true"` for the active session.
    - Test 3: activating that card returns to the Chat panel and keeps `selectedSessionId` unchanged so a later Send continues the same session.
  </behavior>
  <action>Close the MCON-04 gap by populating `SessionListPanel` through existing/current flow seams. At minimum, after `planChatSubmission(...)` receives the `SessionResponse`/run result, call `sessionListPanel.showSession(...)` and `sessionListPanel.selectSession(...)` with a safe title derived from the prompt prefix or session metadata, status from session/run state, and updatedAt from DTO timestamps. If an existing session history/read model is already available in adapter-web without public DTO changes, use it; otherwise do not invent a new list endpoint. Ensure selecting the visible card calls the existing `selectSession(...)`, returns to Chat, preserves Chat feed/input state per D-03/D-12, and does not repopulate fake placeholder rows.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>After Send, the Sessions panel contains a real browser-visible active session card; selecting it returns to Chat and subsequent sends continue the selected session.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Strengthen MVER-03 browser proof and Phase 12 docs for closed gaps</name>
  <files>e2e/phase-12-console-mobile-flow.spec.ts, docs/phase-12-console-mobile-flow.md, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <behavior>
    - Test 1: Playwright waits for event feed count/text to increase after the Send click through the new live/bounded replay path, not just immediate demo replay present at send time.
    - Test 2: Playwright opens Sessions, requires `[data-role="session-card"]`, clicks or keyboard-activates it, and asserts Chat returns active with the same session marked active.
    - Test 3: empty-session fallback selectors no longer satisfy the main MVER-03 session-history assertion.
  </behavior>
  <action>Update `e2e/phase-12-console-mobile-flow.spec.ts` so MVER-03 proves the two verification gaps are closed: (1) record feed count immediately after Send, trigger/wait for the app's live/bounded replay append, and assert count or meaningful text increases without another Send; (2) open Sessions and require a real `[data-role="session-card"]` with `data-session-active="true|false"`, activate it, and assert `[data-console-panel="chat"][data-console-panel-active="true"]`. Remove the previous empty-state fallback from the main session assertion. Update `docs/phase-12-console-mobile-flow.md` to document the final selector contract, local commands, remaining environment-only WebKit/dev-mode limitations if still present, and that Phase 13 card interiors remain deferred.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list</automated>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>MVER-03 no longer passes on one-shot replay plus empty session state; it requires incremental feed evidence and a selectable session card while keeping Phase 13/15 handoffs documented.</done>
</task>

</tasks>

<verification>
Run the focused Java contract gate and Playwright list gate after each task. If the local browser runner is prepared, also run: `npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome"`. Full Mobile Safari/Tablet matrix remains allowed to be CI/prepared-runner verification if this container lacks WebKit host dependencies, but the spec must be syntactically listed locally.
</verification>

<success_criteria>
- MCON-03: Later/live or bounded replay events append to the feed after run creation without another user action, with sequence de-duplication and terminal status propagation.
- MCON-04: Sessions panel contains a real active session card after send/continue; selecting it returns to Chat and preserves active session identity.
- MVER-03: Browser spec asserts incremental feed behavior and real session-card selection using stable selectors; empty-state fallback no longer satisfies the main product path.
- No new mobile-only APIs, public DTO changes, React/Next/Hilla React, PWA/native scope, or Phase 13 card interior redesign is introduced.
</success_criteria>

<output>
After completion, create `.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-06-SUMMARY.md`
</output>
