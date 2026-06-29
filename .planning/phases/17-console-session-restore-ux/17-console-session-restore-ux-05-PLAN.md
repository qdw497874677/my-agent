---
phase: 17-console-session-restore-ux
plan: 05
type: execute
wave: 4
depends_on:
  - 17-console-session-restore-ux-04
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java
autonomous: true
gap_closure: true
requirements:
  - CIA-01
  - CIA-04
  - SESS-02
must_haves:
  truths:
    - "User sees a compact recent-history surface with bounded recent sessions instead of only same-view sessions."
    - "User can access advanced run/session/tool details without leaving the conversation flow when those details are relevant."
    - "Tool and error transcript items remain visible as compact secondary cards/status items without missing i18n markers."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Visible chat-first recent-history and advanced details affordances"
      contains: "data-role=advanced-console-panels"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java"
      provides: "Transcript abnormal status labels with direct-construction fallback"
      contains: "console.session.status.failed"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java"
      provides: "Component proof that history/details are user-visible without hidden test-only controls"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java"
      provides: "Regression proof that secondary card statuses do not render missing-key markers"
  key_links:
    - from: "ConsoleView.java"
      to: "SessionListPanel"
      via: "visible history rail/panel added to Console route"
      pattern: "sessionListPanel"
    - from: "ConsoleView.java"
      to: "RunContextPanel"
      via: "visible compact advanced/details affordance"
      pattern: "runContextPanel"
    - from: "ChatEventStreamPanel.java"
      to: "messages.properties"
      via: "resource-bundle fallback when Vaadin getTranslation returns !{...}!"
      pattern: "ResourceBundle|getTranslation"
---

<objective>
Close Phase 17 UI wiring gaps by making recent history and relevant advanced details reachable in the actual Console route, and by fixing direct-construction transcript status i18n fallback.

Purpose: The completed Phase 17 components exist, but verification found they are hidden from users in `ConsoleView` and one transcript hydration test fails because `ChatEventStreamPanel` can emit Vaadin missing-key markers. This plan turns those component contracts into visible product behavior without adding deferred search/management or Phase 18 streaming semantics.

Output: Visible history/details affordances in the chat-first Console route plus passing focused Java restore/transcript component tests.
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
@.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md
@.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-01-SUMMARY.md
@.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-02-SUMMARY.md
@.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-03-SUMMARY.md
@.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-04-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java

<interfaces>
Existing contracts to preserve:
```java
// ConsoleView currently constructs panels but hides them:
Div switcher = createPanelSwitcher();
Div sessionsPanel = panelWrapper("sessions", sessionListPanel);
Div runContextPanelWrapper = panelWrapper("run-context", runContextPanel);
switcher.setVisible(false);
Div advancedPanels = new Div(sessionsPanel, runContextPanelWrapper, agentsPanel);
advancedPanels.setVisible(false);

// Panel switching contract used by existing component tests and E2E:
public void showConsolePanel(String target)
// valid targets: chat, agents, sessions, run-context
// panel buttons expose data-action=show-console-panel and data-console-target={target}

// Session cards from SessionListPanel:
[data-role="session-card"][data-session-id][data-session-active]

// Transcript status currently lacks fallback in ChatEventStreamPanel:
private String translatedStatus(String statusValue) {
  return switch (statusValue) {
    case "failed" -> getTranslation("console.session.status.failed");
    case "cancelled" -> getTranslation("console.session.status.cancelled");
    case "partial" -> getTranslation("console.session.status.partial");
    default -> statusValue;
  };
}
```

Locked decisions to honor from `17-CONTEXT.md`: D-01 visible compact desktop/tablet history rail plus mobile History panel/button; D-02 mobile selection returns to Chat; D-04 bounded list only, no search/rename/archive/pin/delete; D-18/D-19 details collapsed/secondary but reachable and redacted. Deferred: do not implement Phase 18 streaming aggregator, Phase 19 context assembly, Phase 20 provider stability, or future conversation management.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Expose visible history and advanced-details affordances in ConsoleView</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java</files>
  <behavior>
    - Test 1: A new `ConsoleView` exposes a visible user-reachable History/recent-sessions surface or control; no test should need `showConsolePanel("sessions")` to prove initial reachability.
    - Test 2: `data-action="show-console-panel"` controls for `sessions`, `run-context`, and `chat` are visible/reachable in the component tree, and clicking/opening sessions makes `data-console-panel="sessions"` active and visible.
    - Test 3: Selecting a historical session still returns to Chat and preserves D-02 behavior.
    - Test 4: A compact details/run-context affordance is visible/reachable, while the main chat remains primary and details are not dumped into primary transcript bubbles.
  </behavior>
  <action>Fix the verifier blocker in `ConsoleView`: remove the hidden-only wiring that sets `switcher.setVisible(false)` and `advancedPanels.setVisible(false)` with no alternative access path. Implement a chat-first visible layout that satisfies D-01 and D-18: expose the existing `SessionListPanel` as a compact recent-history rail/section or expose a visible History control that opens it; expose a compact Details/Run control for `runContextPanel`; keep Chat visible as the primary panel. Preserve existing `showConsolePanel(...)`, `data-console-panel`, `data-console-panel-active`, `data-action="show-console-panel"`, and `data-console-target` contracts because the E2E spec and Phase 12/15 patterns depend on stable selectors. Do not add search, rename, archive, pin, delete, provider/model management, or new routes. Ensure `loadRecentSessionsForProof()` still backs the visible history list from `SessionSummaryDto` and that selecting a session returns to Chat per D-02.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest test</automated>
  </verify>
  <done>Console users have a visible route to recent session cards and relevant run/session details; restore/continuation component tests prove the controls are not hidden-only.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add ChatEventStreamPanel i18n fallback for abnormal transcript statuses</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java</files>
  <behavior>
    - Test 1: `redactedMetadataIsNotDumpedAsRawJsonInTranscriptCards` passes for failed tool/error transcript cards and the rendered text contains `failed` without `{` or `}` missing-key markers.
    - Test 2: partial and cancelled secondary cards render human-readable status labels in direct component construction, not `!{console.session.status.*}!`.
    - Test 3: completed secondary cards remain visually quiet and still do not show a completed status chip.
  </behavior>
  <action>Apply the same resource-bundle fallback strategy used by `ConsoleView.t(...)` to `ChatEventStreamPanel` for direct component tests without a Vaadin i18n provider. Add private helper logic so `showEmptyState()`, `showComposerCancelling()` if touched, and especially `translatedStatus(...)` call a fallback that returns `messages.properties` text when `getTranslation(...)` returns a `!{...}!` marker. Keep the helper package-private/private to adapter-web; do not introduce App/Domain i18n dependencies. Preserve redaction discipline: do not render metadata maps, secrets, raw JSON, or braces from missing-key markers.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleTranscriptHydrationTest test</automated>
  </verify>
  <done>Focused transcript hydration tests pass and secondary tool/error cards display readable failed/cancelled/partial statuses without missing translation markers.</done>
</task>

</tasks>

<verification>
Run both focused Java gates together after the two tasks:
`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest,WebConsoleTranscriptHydrationTest test`
</verification>

<success_criteria>
- Recent history is visible/reachable on the Console route through user-facing controls or rail, not only through hidden panels or test-only methods.
- Relevant advanced/details panels are reachable from the conversation flow and remain secondary/collapsed by default.
- Transcript secondary card abnormal statuses render readable labels in direct component tests without missing-key markers.
- No deferred Phase 18/19/20/FUT features are introduced.
</success_criteria>

<output>
After completion, create `.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-05-SUMMARY.md`
</output>
