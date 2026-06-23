---
phase: 12-console-mobile-first-flow
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
  - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java
autonomous: true
requirements: [MCON-01, MCON-04]
must_haves:
  truths:
    - "Mobile user lands on Chat as the active Console panel, not Agents or Sessions."
    - "Mobile user can open Agents, Sessions, and Run Context from an in-page segmented switcher without route navigation."
    - "Mobile user can browse stacked Agent cards and start or continue the General Agent from a prominent primary CTA."
    - "Mobile user can identify and select an active/past session from card-like session history and return to Chat with state preserved."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Route-local Console panel state, segmented controls, Chat-first default, and session-selection return-to-chat behavior"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java"
      provides: "Full-width mobile Agent card hooks and General Agent primary CTA markers"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java"
      provides: "Mobile session card hooks with active-session identity"
    - path: "pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css"
      provides: "Phone Chat-first panel ordering, segmented switcher, stacked catalog/session card layout"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java"
      provides: "Fast Java contract tests for Phase 12 mobile Console hooks"
  key_links:
    - from: "ConsoleView.java"
      to: "ChatEventStreamPanel.java"
      via: "data-console-panel=chat active by default"
      pattern: "data-console-panel.*chat"
    - from: "ConsoleView.java"
      to: "AgentCatalogPanel.java"
      via: "in-page segmented target data-console-target=agents"
      pattern: "data-console-target.*agents"
    - from: "ConsoleView.java"
      to: "SessionListPanel.java"
      via: "selectSession switches active panel back to chat and preserves selectedSessionId"
      pattern: "showConsolePanel\(.*chat"
---

<objective>
Create the mobile Console structure: Chat-first active panel, in-page segmented access to Agents/Sessions/Run Context, stacked Agent cards, and active-session card hooks.

Purpose: Satisfy MCON-01 and MCON-04 while honoring D-01 through D-04 and D-09 through D-12 without changing public REST/SSE DTOs or adding mobile-only APIs.

Output: Vaadin component hooks, pi-mobile CSS contracts, and Java contract tests that downstream composer/feed/E2E work can rely on.
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
@.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md
@.planning/phases/12-console-mobile-first-flow/12-RESEARCH.md
@.planning/phases/12-console-mobile-first-flow/12-VALIDATION.md
@.planning/phases/11-shared-responsive-shell-and-navigation/11-shared-responsive-shell-and-navigation-01-SUMMARY.md
@.planning/phases/11-shared-responsive-shell-and-navigation/11-shared-responsive-shell-and-navigation-02-SUMMARY.md
@docs/phase-11-responsive-shell.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
@pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleUserFlowTest.java

<interfaces>
Existing Console contracts to preserve:
```java
@Route(value = "console", layout = PiResponsiveShell.class)
public class ConsoleView extends Div {
  public RunSubmissionPlan planChatSubmission(String text);
  public SessionSelectionPlan selectSession(String sessionId);
  public CancelPlan planCancelRunningRun(String reason);
  public List<String> columnOrder(); // currently sessions, chat-event-stream, run-context
}
```

Existing selector contracts to extend, not remove:
```text
[data-route="console"]
[data-layout="three-column-workbench"]
[data-column="sessions"]
[data-column="chat-event-stream"]
[data-column="run-context"]
[data-agent-id]
[data-entry-action]
[data-session-id]
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add route-local Chat-first Console panel state and segmented switcher</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <read_first>
    - .planning/phases/12-console-mobile-first-flow/12-CONTEXT.md lines 18-23 for D-01 through D-04.
    - .planning/phases/12-console-mobile-first-flow/12-RESEARCH.md lines 183-200 for the route-local panel-state pattern.
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java.
    - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css.
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleUserFlowTest.java.
  </read_first>
  <behavior>
    - Test 1: a new ConsoleView exposes a segmented control container with `data-role="console-panel-switcher"` and four controls targeting `chat`, `agents`, `sessions`, and `run-context`.
    - Test 2: Chat is active by default with `[data-console-panel="chat"]` and `data-console-panel-active="true"`; agents/sessions/run-context are inactive by default.
    - Test 3: calling a Java method such as `showConsolePanel("sessions")` changes active panel attributes without reconstructing `ChatEventStreamPanel` or clearing existing chat messages.
    - Test 4: desktop regression remains: `data-layout="three-column-workbench"` and `columnOrder()` still returns `sessions`, `chat-event-stream`, `run-context`.
  </behavior>
  <action>
    Create `WebConsoleMobileFlowContractTest.java` first, then implement. In `ConsoleView`, add a route-local active panel field defaulting to `chat` per D-01; add a simple Flow Button segmented switcher (not Vaadin Tabs) with class `pi-console-panel-switcher`, `data-role="console-panel-switcher"`, `data-action="show-console-panel"`, `data-console-target="chat|agents|sessions|run-context"`, and `aria-pressed="true|false"`. Wrap the existing panels in project-owned containers with `data-console-panel="chat|agents|sessions|run-context"` and `data-console-panel-active="true|false"`. Use labels exactly `Chat`, `Agents`, `Sessions`, `Run` for controls. Preserve existing `data-route="console"`, `data-layout="three-column-workbench"`, and `data-column` attributes for desktop/Phase 05 regression per D-04. Add a public package-visible or public method `showConsolePanel(String target)` and an accessor `activeConsolePanel()` so tests can verify state. Do not create new routes, mobile REST endpoints, or viewport flags. In `styles.css`, add concrete rules for `.pi-console-panel-switcher` and `[data-action="show-console-panel"]` including `display: grid`, `grid-template-columns: repeat(4, minmax(0, 1fr))`, `gap: var(--pi-mobile-space-xs)`, and selected styles using `[aria-pressed="true"]`. On `@media (max-width: 640px)`, hide inactive secondary panels with `[data-console-panel-active="false"] { display: none; }`; keep `[data-console-panel="chat"]` visible by default. On `@media (min-width: 900px)`, allow `.pi-console-workbench` to show three columns and do not hide panels solely because inactive.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <acceptance_criteria>
    - `grep -R "data-role.*console-panel-switcher" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` finds the switcher hook.
    - `grep -R "data-console-target.*agents" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` finds the Agents segmented target.
    - `grep -R "data-console-panel.*chat" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` finds the Chat panel hook.
    - `grep -R "activeConsolePanel" pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java` finds a Java state assertion.
    - Maven command above passes.
  </acceptance_criteria>
  <done>Phone Console state is Chat-first, segmented secondary panels are testable through stable hooks, Chat state is preserved across panel switches, and desktop workbench contracts remain present.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Convert Agent Catalog and Session history into mobile card contracts</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <read_first>
    - .planning/phases/12-console-mobile-first-flow/12-CONTEXT.md lines 30-35 for D-09 through D-12.
    - .planning/phases/12-console-mobile-first-flow/12-RESEARCH.md lines 145-155 for current Console inventory.
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java.
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java.
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java.
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java.
    - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css.
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java.
  </read_first>
  <behavior>
    - Test 1: Agent catalog card list has `data-role="agent-catalog-cards"` and CSS makes `.pi-agent-card` full width at phone breakpoints.
    - Test 2: the General Agent card with `data-agent-id="cloud-general-agent"` exposes a prominent Start/Continue CTA marked `data-primary-action="general-agent-start"` or `data-primary-action="general-agent-continue"` while preserving existing `data-entry-action`.
    - Test 3: session entries expose `data-role="session-card"`, `data-session-id`, `data-session-active="true|false"`, `data-field="session-title"`, `data-field="session-status"`, and `data-field="session-updated-at"`.
    - Test 4: selecting a prior session sets the active session card and switches `ConsoleView.activeConsolePanel()` back to `chat` per D-12.
  </behavior>
  <action>
    Extend `AgentCatalogPanel` with class `pi-agent-catalog-panel` and keep `data-role="agent-catalog-cards"`. In `AgentCard`, keep existing `data-agent-id`, `data-action="choose-agent"`, and `data-entry-action`; additionally, when `agent.id()` equals `cloud-general-agent`, mark the first chat/run entry action button with `data-primary-action="general-agent-start"` if the action id contains `start`, otherwise `data-primary-action="general-agent-continue"` if the action id contains `continue`; add class `pi-agent-card-primary-action` to that button and `data-general-agent="true"` on the card. If action IDs are generic, prefer the first entry action and set `data-primary-action="general-agent-start"`. In `SessionListPanel`, render each session as a `Div` with class `pi-session-card`, `role="button"`, `tabindex="0"`, `data-role="session-card"`, `data-session-id`, and `data-session-active="true|false"`; inside it add spans with exact `data-field="session-title"`, `data-field="session-status"`, and `data-field="session-updated-at"`. Use fallback title `Recent session`, fallback status `ready`, and updated fallback `not yet updated` so hooks are never empty. In `ConsoleView.selectSession`, after `sessionListPanel.selectSession(...)`, call `showConsolePanel("chat")` to honor D-12 and preserve selected session identity. In `styles.css`, add phone rules: `.pi-agent-catalog [data-role="agent-catalog-cards"], .pi-console-sessions [data-role="session-list"] { display: grid; gap: var(--pi-mobile-space-sm); }`, `.pi-agent-card, .pi-session-card { width: 100%; border: 1px solid var(--pi-mobile-shell-border); border-radius: 1rem; padding: var(--pi-mobile-space-md); background: var(--pi-mobile-shell-surface); }`, `.pi-agent-card-primary-action { width: 100%; }`, and `.pi-session-card[data-session-active="true"] { border-color: var(--pi-mobile-focus-color); background: #e8f0ff; }`. Do not redesign tool/approval cards or Admin surfaces.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <acceptance_criteria>
    - `grep -R "data-primary-action.*general-agent" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java` finds the General Agent primary CTA marker.
    - `grep -R "data-role.*session-card" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java` finds session card hooks.
    - `grep -R "data-session-active" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java` finds active-session hook logic.
    - `grep -R "showConsolePanel(\"chat\")" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` confirms selecting sessions returns to Chat.
    - Maven command above passes.
  </acceptance_criteria>
  <done>Agents and Sessions are reachable as mobile card panels with prominent General Agent CTA, safe session metadata, active-session identity, and select-session return-to-Chat behavior.</done>
</task>

</tasks>

<verification>
Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test`. Confirm no changes introduce public REST/SSE DTOs, `/mobile/*` APIs, React/Hilla React/Next.js, or Phase 13 card-detail redesign.
</verification>

<success_criteria>
- MCON-01: General Agent catalog is full-width/card-ready on phone and exposes a prominent primary CTA hook.
- MCON-04: Session history exposes card metadata, active-session identity, and selection returns to Chat.
- D-01 through D-04 and D-09 through D-12 are referenced in implementation and locked into Java/CSS contract tests.
</success_criteria>

<output>
After completion, create `.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-01-SUMMARY.md`.
</output>
