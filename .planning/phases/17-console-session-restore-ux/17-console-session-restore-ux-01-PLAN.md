---
phase: 17-console-session-restore-ux
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionListPanelTest.java
autonomous: true
requirements:
  - CIA-01
must_haves:
  truths:
    - "User sees a compact recent-history surface with bounded recent sessions instead of only same-view sessions."
    - "Recent session cards show stable title, preview, last activity, and status/active-run status without provider/model metadata by default."
    - "A selected historical session is visibly highlighted in the history surface."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java"
      provides: "Formal compact recent-session history card API using SessionSummaryDto"
      contains: "showRecentSessions"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionListPanelTest.java"
      provides: "Fast component contracts for bounded history cards and selectors"
      min_lines: 60
  key_links:
    - from: "SessionListPanel.showRecentSessions(...)"
      to: "SessionSummaryDto"
      via: "uses title, lastMessagePreview, lastActivityAt, status, activeRunStatus"
      pattern: "SessionSummaryDto"
    - from: "SessionListPanel card"
      to: "browser/component tests"
      via: "stable data-role/session/field attributes"
      pattern: "data-role.*session-card"
---

<objective>
Create the compact recent-history rail/panel component contract for Phase 17.

Purpose: Users need a Kimi-style recent conversation entry point (D-01, D-03, D-04) before ConsoleView can make selection/restore behavior product-grade. This plan keeps work inside adapter-web Vaadin and consumes the Phase 16 `SessionSummaryDto` read model without introducing search, rename, archive, pin, delete, or provider/model history fields.

Output: A formal `SessionListPanel.showRecentSessions(...)` API with card fields/selectors and fast component tests.
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
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/SessionSummaryDto.java

<interfaces>
Existing contracts to use directly:

From `SessionListPanel.java`:
```java
public void showSession(String sessionId, String title, String status, Instant updatedAt);
public void showRecentSessionsForProof(List<SessionSummaryDto> summaries);
public void selectSession(String sessionId);
public List<String> recentSessionIds();
public List<Div> sessionCards();
public void setSessionActivationHandler(Consumer<String> sessionActivationHandler);
```

From Phase 16 `SessionSummaryDto` usage:
```java
new SessionSummaryDto(sessionId, title, status, lastMessagePreview, createdAt, lastActivityAt, activeRunId, activeRunStatus, metadata)
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Formalize bounded recent-session card rendering</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionListPanelTest.java</files>
  <behavior>
    - Test 1: `showRecentSessions(List.of(summaryA, summaryB), selectedId, hasMore)` renders exactly two `[data-role=session-card]` cards in provided order.
    - Test 2: each card exposes `data-session-id`, `data-session-active`, `data-field=session-title`, `data-field=session-preview`, `data-field=session-updated-at`, and `data-field=session-status`.
    - Test 3: `activeRunStatus` is reflected in the status field when present; otherwise `status` is used. Provider/model metadata is not rendered by default per D-03.
    - Test 4: an empty summary list renders the existing empty state and no cards.
  </behavior>
  <action>Replace the proof-only `showRecentSessionsForProof(...)` path with a formal `showRecentSessions(List&lt;SessionSummaryDto&gt; summaries, String selectedSessionId, boolean hasMore)` method. Keep `showRecentSessionsForProof(...)` as a delegating compatibility helper only. Render compact cards per D-01/D-03/D-04: title, last-message preview, last activity time, and status/active-run status; do not add provider/model fields, search, rename, archive, pin, or delete controls because those are deferred. Preserve click and keyboard activation semantics. Add an optional lightweight `data-role="session-more"` or similar marker only when `hasMore` is true; it must not implement full management/search UX.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionListPanelTest test</automated>
  </verify>
  <done>Recent-session cards render bounded Phase 16 summary fields with stable selectors, selected-state highlighting, and no provider/model/search/management creep.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Preserve activation and selected-state semantics</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionListPanelTest.java</files>
  <behavior>
    - Test 1: `activateSessionCardForTest("session-a", "click")` calls the activation handler with `session-a`.
    - Test 2: after `selectSession("session-b")`, exactly one card has `data-session-active="true"` and it is `session-b`.
    - Test 3: selecting a session not currently in the bounded list creates a minimal selected card without losing existing activation behavior.
  </behavior>
  <action>Harden selection state so ConsoleView can later call `selectSession(...)` after transcript hydration without corrupting metadata. Preserve `data-role="session-card"`, `role="button"`, `tabindex="0"`, click, Enter, and Space activation. Add test helper accessors only if needed; keep them package/test-friendly and do not leak Vaadin concerns outside adapter-web.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionListPanelTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>Session cards remain keyboard/click activatable and selected-state is deterministic for downstream Console restore wiring.</done>
</task>

</tasks>

<verification>
Run the focused adapter-web component tests. Confirm no code path renders deferred management features or provider/model metadata in session cards.
</verification>

<success_criteria>
- `SessionListPanel` has a formal recent-session API consuming `SessionSummaryDto`.
- Compact history cards render title, preview, last activity, status/active-run status, stable selectors, and selected highlighting.
- Existing mobile/session activation tests still pass.
</success_criteria>

<output>
After completion, create `.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-01-SUMMARY.md`
</output>
