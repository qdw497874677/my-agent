---
phase: 12-console-mobile-first-flow
plan: 02
type: execute
wave: 2
depends_on: [12-console-mobile-first-flow-01]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java
  - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java
autonomous: true
requirements: [MCON-02, MCON-03, MCON-05]
must_haves:
  truths:
    - "Mobile user can type a multi-line prompt in a bounded TextArea and submit it without losing Send or Cancel controls."
    - "Mobile user can read active run state near the composer for queued, running, cancelling, and terminal states."
    - "Mobile user can scroll prior event output in a vertical feed while the composer and primary Cancel remain practically reachable."
    - "Mobile user can cancel an active run from both composer-near controls and a backup page/run-status area."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java"
      provides: "Explicit event feed, sticky composer, bounded TextArea, inline run status, and composer Cancel control"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java"
      provides: "Secondary run context status and backup cancel state"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Run status synchronization between chat composer and run context"
    - path: "pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css"
      provides: "Sticky composer, feed scroll containment, safe-area padding, and touch-safe cancel rules"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java"
      provides: "Fast contracts for composer/feed/cancel hooks"
  key_links:
    - from: "ChatEventStreamPanel.java"
      to: "ConsoleView.planChatSubmission"
      via: "appendUserMessage and showComposerRunStatus on submit"
      pattern: "showComposerRunStatus|setRunState"
    - from: "ConsoleView.planCancelRunningRun"
      to: "ChatEventStreamPanel.java and RunContextPanel.java"
      via: "showCancelling status in both visible run-control surfaces"
      pattern: "showCancelling"
    - from: "styles.css"
      to: "ChatEventStreamPanel.java"
      via: "data-role=event-feed and data-role=chat-composer wrappers"
      pattern: "pi-console-composer|event-feed"
---

<objective>
Make the mobile Chat/Run surface usable during real Console work: bounded multi-line composer, sticky run controls, vertical event feed, inline run state, and dual-position Cancel.

Purpose: Satisfy MCON-02, MCON-03, and MCON-05 while honoring D-05 through D-08 and D-13 through D-15. This plan builds on Plan 01 panel hooks and preserves public REST/SSE paths.

Output: Vaadin component structure and theme CSS that browser E2E can verify in Plan 03.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md
@.planning/phases/12-console-mobile-first-flow/12-RESEARCH.md
@.planning/phases/12-console-mobile-first-flow/12-VALIDATION.md
@.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-01-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java
@pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleUserFlowTest.java
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java

<interfaces>
Plan 01 should provide:
```text
[data-console-panel="chat"]
[data-console-panel-active="true|false"]
[data-role="console-panel-switcher"]
ConsoleView.showConsolePanel(String target)
ConsoleView.activeConsolePanel()
```

Existing run contracts to preserve:
```java
public RunSubmissionPlan planChatSubmission(String text);
public void markRunRunning(String sessionId, String runId);
public CancelPlan planCancelRunningRun(String reason);
public void applyRunStatus(String status, boolean terminal);
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add explicit vertical event feed and sticky bounded composer</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <read_first>
    - .planning/phases/12-console-mobile-first-flow/12-CONTEXT.md lines 24-29 and 36-39 for D-05 through D-08 and D-13 through D-15.
    - .planning/phases/12-console-mobile-first-flow/12-RESEARCH.md lines 202-224 for feed/composer split.
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java.
    - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css.
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java.
  </read_first>
  <behavior>
    - Test 1: `ChatEventStreamPanel` contains a feed wrapper with class `pi-console-event-feed` and `data-role="event-feed"`.
    - Test 2: composer wrapper has class `pi-console-composer` and `data-role="chat-composer"` and contains input, Send, inline status, and primary Cancel.
    - Test 3: TextArea has `setMinRows(2)` and `setMaxRows(6)` observable via component accessors or explicit test methods.
    - Test 4: theme CSS contains `.pi-console-composer { position: sticky; bottom: calc(var(--pi-mobile-safe-area-bottom) + var(--pi-mobile-space-sm)); }` and `.pi-console-event-feed` bottom padding.
  </behavior>
  <action>
    Refactor `ChatEventStreamPanel` so existing `stream`, `input`, and `send` are not direct root siblings. Add `Div feed` with class `pi-console-event-feed` and `data-role="event-feed"`; move empty state and appended user/event components into this feed. Add `Div composer` with class `pi-console-composer` and `data-role="chat-composer"`; inside composer add a `Span composerRunStatus` with `data-role="composer-run-status"` default text `No active run`, the existing `TextArea` with `data-role="chat-input"`, existing Send button with `data-action="send-chat"`, and a new `Button composerCancel` labeled exactly `Cancel run` with `data-action="cancel-run-primary"`. Set `input.setMinRows(2)` and `input.setMaxRows(6)` per D-06; keep placeholder text. Add public methods `showComposerRunStatus(String status, boolean cancellable)`, `showComposerCancelling()`, `composerStatusText()`, `composerCancelVisible()`, `inputMinRows()`, and `inputMaxRows()` if needed for tests. Set the primary cancel invisible by default; when cancellable, visible and `data-prominent="true"`. In `styles.css`, add `.pi-console-chat { display: grid; min-height: 0; }`, `.pi-console-event-feed { display: grid; gap: var(--pi-mobile-space-sm); min-height: 12rem; padding-bottom: calc(var(--pi-mobile-tap-target) * 2); }`, and phone-specific `.pi-console-composer { position: sticky; bottom: calc(var(--pi-mobile-safe-area-bottom) + var(--pi-mobile-space-sm)); z-index: 5; display: grid; gap: var(--pi-mobile-space-sm); padding: var(--pi-mobile-space-sm); border: 1px solid var(--pi-mobile-shell-border); border-radius: 1rem; background: var(--pi-mobile-shell-surface); }`. Add a selector for `vaadin-text-area[data-role="chat-input"]` with `max-height: 14rem` and `overflow: auto`. Do not redesign individual event/tool/approval card interiors.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <acceptance_criteria>
    - `grep -R "data-role.*event-feed" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` finds the event feed hook.
    - `grep -R "data-role.*chat-composer" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` finds the composer hook.
    - `grep -R "setMinRows(2)" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` and `grep -R "setMaxRows(6)" ...` find bounded TextArea row constraints.
    - `grep -R "position: sticky" pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` finds sticky composer CSS.
    - Maven command above passes.
  </acceptance_criteria>
  <done>Chat panel has a testable vertical feed and bottom-reachable bounded composer with inline run state and primary cancel hook.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Synchronize active run state and dual Cancel controls</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java, pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <read_first>
    - .planning/phases/12-console-mobile-first-flow/12-CONTEXT.md lines 24-29 for D-07 and D-08.
    - .planning/phases/12-console-mobile-first-flow/12-RESEARCH.md lines 399-409 for shell backup cancel and fake run timing open questions.
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java.
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java.
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java.
    - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css.
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java.
  </read_first>
  <behavior>
    - Test 1: `ConsoleView.planChatSubmission("...")` sets both `RunContextPanel.statusText()` and `ChatEventStreamPanel.composerStatusText()` to a running/queued phrase and makes both cancel controls prominent.
    - Test 2: `ConsoleView.planCancelRunningRun("...")` sets both run context and composer status to a cancelling phrase.
    - Test 3: `ConsoleView.applyRunStatus("completed", true)` hides both cancel controls and shows terminal status near composer.
    - Test 4: backup cancel remains exposed with existing `data-action="cancel-run"`; primary composer cancel uses `data-action="cancel-run-primary"`.
  </behavior>
  <action>
    Wire active run state through existing Console methods. In `planChatSubmission`, after `runContextPanel.showRunning(sessionId, runId)`, call `chatPanel.showComposerRunStatus("Running run " + runId + " in session " + sessionId, true)`. In `markRunRunning`, update both panels. In `planCancelRunningRun`, call `runContextPanel.showCancelling()` and `chatPanel.showComposerCancelling()` before returning the existing public cancel path. In `applyRunStatus`, call both `runContextPanel.showStatus(status, terminal)` and `chatPanel.showComposerRunStatus("Run status: " + status, !terminal && status is queued/running/cancelling)`. Keep `RunContextPanel` as backup cancel/status area with existing `data-action="cancel-run"`; add `data-role="run-status"` to its status span and class `pi-console-cancel-backup` to the existing cancel button. Do not wire shell private slots unless a minimal existing API is already present; route-local backup in `RunContextPanel` satisfies D-08 when paired with composer primary cancel. In `styles.css`, add `.pi-console-cancel-backup, [data-action="cancel-run-primary"] { min-width: var(--pi-mobile-tap-target); min-height: var(--pi-mobile-tap-target); }` and `[data-role="composer-run-status"], [data-role="run-status"] { font-weight: 700; }`.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <acceptance_criteria>
    - `grep -R "showComposerRunStatus" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` finds run-state synchronization calls.
    - `grep -R "data-action.*cancel-run-primary" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` finds the primary composer cancel.
    - `grep -R "data-action.*cancel-run" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java` confirms backup cancel remains.
    - `grep -R "composerStatusText" pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java` finds Java assertions for composer status.
    - Maven command above passes.
  </acceptance_criteria>
  <done>Active run state and cancelling/terminal feedback are visible near the composer and in backup run context, with two touch-safe cancel hooks and unchanged public cancel API path.</done>
</task>

</tasks>

<verification>
Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test`. Confirm `ConsoleHttpClient` and `EventStreamClient` public path behavior is unchanged, and no Phase 13 runtime-card interior redesign is introduced.
</verification>

<success_criteria>
- MCON-02: multi-line bounded composer submits through existing `planChatSubmission` and shows active run state.
- MCON-03: event output is in a vertical feed with composer/cancel remaining reachable through sticky mobile CSS.
- MCON-05: active run cancellation has primary and backup visible controls plus cancelling/terminal feedback.
</success_criteria>

<output>
After completion, create `.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-02-SUMMARY.md`.
</output>
