---
phase: 12-console-mobile-first-flow
plan: 04
type: execute
wave: 4
depends_on: [12-console-mobile-first-flow-01, 12-console-mobile-first-flow-02]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java
autonomous: true
gap_closure: true
requirements: [MCON-01, MCON-02, MCON-04, MCON-05, MVER-03]
must_haves:
  truths:
    - "Mobile user can open Agents panel and see General Agent in the actual agents panel, not a hollow wrapper."
    - "Mobile user can activate General Agent CTA and return/keep Chat active without a route change."
    - "Mobile user can activate a session card by click, Enter, or Space and return to Chat with active session identity visible."
    - "Mobile user Send and both Cancel controls invoke ConsoleView action seams through real Vaadin UI events."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Route-local wiring between component callbacks and Console state; no AgentCatalogPanel reparenting into SessionListPanel"
      contains: "new ChatEventStreamPanel("
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java"
      provides: "Catalog rendering and callback binding for agent entry actions"
      contains: "showCatalog"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java"
      provides: "General Agent primary CTA with click listener callback"
      contains: "addClickListener"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java"
      provides: "Session card click and keyboard activation callbacks"
      contains: "addClickListener"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java"
      provides: "Fast regression contracts proving user event handlers exist"
  key_links:
    - from: "ConsoleView.java"
      to: "AgentCatalogPanel.java"
      via: "Agent selection callback"
      pattern: "onAgentAction|handleAgentAction|selectAgent"
    - from: "ConsoleView.java"
      to: "SessionListPanel.java"
      via: "Session activation callback returning to Chat"
      pattern: "selectSession\\(sessionId\\)"
    - from: "ChatEventStreamPanel.java"
      to: "ConsoleView.planChatSubmission"
      via: "Send button listener reads TextArea value"
      pattern: "addClickListener.*submit|setSubmitHandler"
    - from: "RunContextPanel.java"
      to: "ConsoleView.planCancelRunningRun"
      via: "Backup cancel button listener"
      pattern: "addClickListener.*cancel|setCancelHandler"
---

<objective>
Close Phase 12 verification gaps for hollow/mobile-selector-only Console controls by making Agent, Session, Send, and Cancel controls real Vaadin user actions.

Purpose: The previous phase execution created stable selectors and programmatic seams, but verification proved the browser-visible workflow was not connected. This plan fixes the UI action layer before adding backend run/event execution.
Output: Console components expose callback-based action contracts; ConsoleView wires them; Agent Catalog stays in the Agents panel; Java contracts fail if controls regress to selector-only buttons/cards.
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
@.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-01-SUMMARY.md
@.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-02-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java

<interfaces>
<!-- Current contracts and known gaps from verification. Use these directly; do not re-discover. -->

From `ConsoleView.java`:
```java
public RunSubmissionPlan planChatSubmission(String text);
public SessionSelectionPlan selectSession(String sessionId);
public CancelPlan planCancelRunningRun(String reason);
public void showConsolePanel(String target);
public String activeConsolePanel();
public AgentCatalogPanel agentCatalogPanel();
public ChatEventStreamPanel chatPanel();
public RunContextPanel runContextPanel();
```

Current anti-pattern to remove:
```java
sessionListPanel.add(agentCatalogPanel); // reparenting leaves [data-console-panel=agents] hollow
```

From `ChatEventStreamPanel.java`:
```java
private final TextArea input = new TextArea("Message");
private final Button send = new Button("Send");              // currently no listener
private final Button composerCancel = new Button("Cancel run"); // currently no listener
public void appendUserMessage(String text);
public void showComposerRunStatus(String status, boolean cancellable);
```

From `RunContextPanel.java`:
```java
private final Button cancel = new Button("Cancel run"); // currently no listener
public void showRunning(String sessionId, String runId);
public void showCancelling();
public void showStatus(String runStatus, boolean terminal);
```

Locked decisions: D-01 through D-20 from `12-CONTEXT.md` remain binding. Especially D-02, D-03, D-08, D-09, D-10, D-12, D-16, and D-20.
Deferred ideas remain out of scope: Phase 13 card interior redesign, Phase 14 Admin conversion, Phase 15 broad real-device/accessibility hardening, native/PWA/mobile-only APIs.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add failing contracts for real Console UI activation</name>
  <files>pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <behavior>
    - Test 1: A new `ConsoleView` keeps `AgentCatalogPanel` inside `[data-console-panel="agents"]`; the same `AgentCatalogPanel` must not also be a child of `SessionListPanel` (gap MCON-01, D-02/D-09).
    - Test 2: Activating/clicking the General Agent primary action calls a ConsoleView-level callback, sets `selectedAgentId` to `cloud-general-agent`, and leaves/returns `activeConsolePanel()` as `chat` (gap MCON-01, D-10).
    - Test 3: Activating a session card by click and by keyboard Enter/Space calls `selectSession(sessionId)`, marks `data-session-active="true"`, and returns to Chat (gap MCON-04, D-12).
    - Test 4: Send, primary Cancel, and backup Cancel controls expose listener-backed test seams; invoking them through Vaadin click events calls the same ConsoleView methods used by programmatic tests (gaps MCON-02/MCON-05).
  </behavior>
  <action>
    Extend `WebConsoleMobileFlowContractTest` with RED tests before production changes. Prefer direct Vaadin component event invocation (`Button.click()` or component helper methods) over brittle DOM assumptions. The tests must fail against current code because buttons/cards are selector-only and because `sessionListPanel.add(agentCatalogPanel)` reparents the catalog. Reference verification gaps from `12-VERIFICATION.md` in test names/comments for traceability. Do not add browser/E2E assertions in this task; this is the fast contract gate.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>New contract tests fail for the current selector-only/reparented implementation and explicitly cover MCON-01, MCON-02, MCON-04, MCON-05 action wiring gaps.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Implement callback contracts for Agent, Session, Send, and Cancel controls</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <behavior>
    - Agent card primary CTA invokes a callback with agent id + entry action id; ConsoleView stores the selected agent and switches to Chat per D-10.
    - Session cards invoke a callback on click and keyboard Enter/Space; ConsoleView calls `selectSession(sessionId)` per D-12.
    - Send invokes a callback with the current TextArea value and clears or preserves input according to existing Console behavior after a non-blank submission; blank values must not create runs/messages.
    - Both cancel controls invoke a callback using the same `planCancelRunningRun("mobile user requested cancellation")` seam so composer and Run Context statuses update together per D-08.
  </behavior>
  <action>
    Implement the minimum callback APIs needed by Task 1 tests: constructors or setter methods such as `AgentCatalogPanel.setAgentActionHandler(...)`, `AgentCard(..., handler)`, `SessionListPanel.setSessionActivationHandler(...)`, `ChatEventStreamPanel.setSubmitHandler(...)`, `ChatEventStreamPanel.setCancelHandler(...)`, and `RunContextPanel.setCancelHandler(...)`. Use Java functional interfaces (`Consumer`, `BiConsumer`, `Runnable`) or tiny records if clearer. Wire these in `ConsoleView` constructor. Remove `sessionListPanel.add(agentCatalogPanel)` so `AgentCatalogPanel` remains only inside `panelWrapper("agents", agentCatalogPanel)` (verification blocker MCON-01). Preserve existing public methods and selector attributes so Plan 03 E2E selectors continue to work. Do not introduce `/mobile/*`, DTO changes, React/Next/Hilla, or Phase 13 card redesign.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>Contract tests pass; Agent Catalog is not reparented into Sessions; user-like click/keyboard/send/cancel activation reaches ConsoleView state methods through actual Vaadin event listeners.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Keep catalog and session surfaces browser-visible with safe initial data paths</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <behavior>
    - AgentCatalogPanel must support non-empty initial catalog rendering in production route creation, using the existing Agent Catalog read model or `/api/agents` DTO shape; it must still show a clear empty state only when the read model truly returns no agents.
    - The General Agent card must be visible to a browser user by default in the normal Console route when the existing catalog read model contains `cloud-general-agent` (MCON-01).
    - SessionListPanel must keep empty-state behavior for no sessions while retaining click/keyboard activation for any rendered historical session (MCON-04).
  </behavior>
  <action>
    Add an initialization seam that production `ConsoleView` can use to supply the current `AgentCatalogResponse` to `AgentCatalogPanel` without hardcoding fake data. Prefer Adapter → App wiring (COLA allowed) through the existing `AgentCatalogQueryService` if straightforward in Vaadin Spring route construction; otherwise create a small adapter-web-only provider class/interface that can be injected by Spring and faked in tests. Keep all code in `pi-agent-adapter-web`; do not move Vaadin concerns into App/Domain. Preserve `AgentCatalogPanel.showCatalog(...)` for tests. For Sessions, do not invent a global session-list endpoint if one does not exist; keep the current empty state and ensure sessions added/selected by current flow render as activatable cards. Document in test names that browser-visible historical session population remains limited to existing session/history read models unless an existing list endpoint is present.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleCatalogAndToolCardsTest,WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>Normal Console construction can render General Agent catalog data through an adapter-web/App read-model seam; empty states remain explicit; no new mobile-only backend API or hardcoded fake catalog is introduced.</done>
</task>

</tasks>

<verification>
Run the Java contract gate after each task. Before finishing, search the changed Console Java files for the blocker anti-pattern `sessionListPanel.add(agentCatalogPanel)` and for selector-only buttons without listeners in Send/Cancel/Agent/Session components.

Required final command:
`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleCatalogAndToolCardsTest,WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test`
</verification>

<success_criteria>
- MCON-01 gap reduced: Agents panel contains a populated/initializable AgentCatalogPanel and General Agent CTA activates Chat.
- MCON-02 gap reduced: Send button reads TextArea value through an actual click listener and calls ConsoleView submission seam.
- MCON-04 gap reduced: Session cards activate by click and keyboard and return to Chat with active identity.
- MCON-05 gap reduced: Primary and backup Cancel buttons call the same Console cancel seam through actual listeners.
- MVER-03 ready for Plan 05: browser-visible controls are no longer selector-only, so run/event execution can be wired and tested.
</success_criteria>

<output>
After completion, create `.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-04-SUMMARY.md`
</output>
