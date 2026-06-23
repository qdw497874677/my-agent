---
phase: 12-console-mobile-first-flow
plan: 05
type: execute
wave: 5
depends_on: [12-console-mobile-first-flow-04]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java
  - e2e/phase-12-console-mobile-flow.spec.ts
autonomous: true
gap_closure: true
requirements: [MCON-02, MCON-03, MCON-05, MVER-03]
must_haves:
  truths:
    - "Mobile user can submit a multi-line prompt from the browser and see user message plus queued/running status."
    - "Mobile user can observe at least one browser-visible event/feed progression generated from the run/event path, not from a static placeholder."
    - "Mobile user can cancel an active run from primary or backup control and see cancelling/cancelled/terminal feedback."
    - "MVER-03 browser gate asserts real UI event paths rather than relying only on programmatic ConsoleView method tests."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Submit/cancel orchestration through existing session/run/status/events/cancel use cases or public path-equivalent bridge"
      contains: "handleChatSubmit"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java"
      provides: "Feed append helpers and submit/cancel control state for browser-visible run progression"
      contains: "appendEvent"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java"
      provides: "Public RunEventDto to mobile feed rendering without Phase 13 card redesign"
      contains: "RenderedEvent"
    - path: "e2e/phase-12-console-mobile-flow.spec.ts"
      provides: "MVER-03 product-path assertion over actual UI clicks"
      contains: "send.click"
  key_links:
    - from: "ChatEventStreamPanel.java"
      to: "ConsoleView.java"
      via: "submit handler with TextArea value"
      pattern: "handleChatSubmit|planChatSubmission"
    - from: "ConsoleView.java"
      to: "RunCommandService or public run path-equivalent bridge"
      via: "create session/run then update selectedSessionId/activeRunId"
      pattern: "createSession|createRun"
    - from: "ConsoleView.java"
      to: "RunEventRenderer.java"
      via: "list/replay events and append rendered feed items"
      pattern: "appendEvent\\(runEventRenderer.render"
    - from: "ConsoleView.java"
      to: "cancel path"
      via: "primary/backup cancel handlers"
      pattern: "cancelRun|planCancelRunningRun"
---

<objective>
Close Phase 12 verification gaps for run submission, live/near-live feed progression, and cancellation feedback through browser-visible UI paths.

Purpose: Plan 04 makes controls actionable. This plan connects those actions to the existing create-session/create-run/event/cancel semantics so the mobile Console workflow is genuinely end-to-end enough for MVER-03.
Output: User-triggered Send creates or continues a run, renders meaningful feed/status progression, and user-triggered Cancel reaches the existing cancel behavior with visible feedback; E2E assertions are updated to prove this path.
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
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/RunController.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/EventStreamClient.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java
@e2e/phase-12-console-mobile-flow.spec.ts

<interfaces>
<!-- DTO and rendering contracts to use. Keep public REST/SSE DTO boundaries stable; no mobile-only API. -->

From `ConsoleHttpClient.java`:
```java
public String createSessionPath();                    // /api/sessions
public String createRunPath(String sessionId);        // /api/sessions/{sessionId}/runs
public String runEventsPath(String sessionId, String runId, long afterSequence);
public String cancelRunPath(String sessionId, String runId);
public String agentCatalogPath();                     // /api/agents
```

From client DTOs:
```java
public record SessionResponse(String tenantId, String userId, String sessionId, String workspaceId,
        String currentEntryId, String status, Instant createdAt, Instant updatedAt, Map<String, Object> metadata) {}
public record RunResponse(String tenantId, String userId, String sessionId, String runId, String workspaceId,
        String status, String traceId, String correlationId, Instant createdAt, Instant updatedAt) {}
public record RunStatusResponse(String sessionId, String runId, String status, boolean terminal,
        Instant updatedAt, String traceId, String correlationId) {}
public record EventHistoryResponse(String sessionId, String runId, List<RunEventDto> events,
        long afterSequence, long nextAfterSequence, boolean terminal) {}
```

From `RunEventRenderer.java`:
```java
public RenderedEvent render(RunEventDto event);
public record RenderedEvent(String category, String text, boolean terminal, Component component) {}
```

Locked decisions: D-05 through D-08 and D-13 through D-20 are the primary constraints. Do not implement deferred Phase 13 runtime/tool/approval card interiors; only render existing event/card surfaces sufficiently for feed progression.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add failing contracts for user-triggered run execution and event rendering</name>
  <files>pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <behavior>
    - Test 1: After Plan 04 submit handler is invoked with a multi-line prompt, ConsoleView creates/uses a session id, stores an active run id from a real `RunResponse`-like result instead of `pending-run`, appends the user message, and shows queued/running composer status (MCON-02).
    - Test 2: Given an `EventHistoryResponse` with status/model/terminal events, ConsoleView renders them through `RunEventRenderer` into `[data-role="event-feed"]`; `messageCount()` or feed event count increases beyond the user message (MCON-03, D-15).
    - Test 3: Cancel handler calls the existing cancel service/path-equivalent seam for the active run and applies returned `RunStatusResponse` to both composer and Run Context surfaces (MCON-05).
  </behavior>
  <action>
    Add RED tests using a fake adapter-web bridge/service object rather than real HTTP or model calls. The fake should return deterministic `SessionResponse`, `RunResponse`, `EventHistoryResponse`, and `RunStatusResponse` DTOs. Keep the test in `WebConsoleMobileFlowContractTest` unless it becomes too large; if split, create `WebConsoleMobileActionFlowTest` and include it in verify commands. Tests must fail against current code because current `planChatSubmission` uses `pending-session`/`pending-run` and no event source appends streamed events after user Send.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>Failing tests prove the remaining MCON-02/MCON-03/MCON-05 gaps: no real run id, no event feed progression, and no cancel result application from user-triggered flow.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Wire Send to create/continue run and append event feed progression</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java</files>
  <behavior>
    - Send button reads current TextArea value and invokes the new execution path from Plan 04.
    - If no session is selected, create a session through an adapter-web/App bridge or public path-equivalent service before creating the run; if a session is selected, reuse it.
    - Create run uses existing `CreateRunRequest` semantics from `planChatSubmission`: selected agent id, `chat` input mode, `Map.of("text", message)`, and source `vaadin-console`.
    - Feed shows the user message plus at least one rendered run status/model/terminal event from event history/replay; composer and Run Context status reflect the latest run status.
  </behavior>
  <action>
    Implement a small adapter-web-only execution bridge for ConsoleView if one does not already exist. It may wrap App layer `SessionCommandService`, `RunCommandService`, and `RunQueryService` (COLA Adapter → App dependency is allowed) or an equivalent existing Console service; do not put Vaadin types into App/Domain. The bridge must return public client DTOs and should be fakeable in Java tests. Update `ConsoleView` so the actual submit handler uses the bridge, stores `selectedSessionId` and `activeRunId` from returned DTOs, calls `runContextPanel.showRunning(...)`, calls `chatPanel.showComposerRunStatus(...)`, and renders replayed/listed events through `RunEventRenderer.render(...)` into `chatPanel.appendEvent(...)`. Keep `planChatSubmission(...)` either as a planning/helper method used by tests or delegate it into the same path, but do not leave browser Send on the old pending-only path.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>Send click path creates/continues a real DTO-backed run, appends browser-visible event feed content beyond the initial user message, and preserves public REST/SSE DTO boundaries without mobile-only APIs.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Wire cancellation results and tighten MVER-03 browser evidence</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java, e2e/phase-12-console-mobile-flow.spec.ts</files>
  <behavior>
    - Primary Cancel and backup Cancel invoke the same active-run cancellation bridge and apply the returned `RunStatusResponse` to both composer and Run Context.
    - If cancellation races with terminal status, UI still shows either cancelling/cancelled or terminal feedback; no uncaught exception is thrown from double-click/no-active-run races.
    - Playwright spec waits for user-click Send to produce feed progression (`[data-event-category]` count >= 1 or terminal/cancel text) and then clicks an actual visible Cancel control when it is present.
  </behavior>
  <action>
    Update Console cancel handlers to call the adapter-web execution bridge for `cancelRun`, then `applyRunStatus(response.status(), response.terminal())`; keep the immediate `showCancelling()` optimistic update so users get feedback instantly per D-07/D-08. Harden no-active-run/double-click behavior by showing a non-terminal status message instead of throwing from the UI handler, while preserving `planCancelRunningRun(...)` throwing behavior if existing unit tests require it. Update `e2e/phase-12-console-mobile-flow.spec.ts` so it no longer passes when only selector shells exist: after `send.click()`, require either event count progression in `[data-role="event-feed"]` or status text from the DTO-backed run, and after cancel require cancelling/cancelled/terminal feedback from the actual clicked UI. Keep tolerant browser matrix behavior from Plan 03; do not add real-device or Phase 13 detail assertions.
  </action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test && PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Cancel buttons produce visible cancellation/terminal feedback through real UI handlers, and MVER-03 spec would fail on selector-only controls or static feed placeholders.</done>
</task>

</tasks>

<verification>
Required final commands:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list
```

If the local container still cannot run full Playwright due Vaadin dev-mode/WebKit host dependency issues, keep that documented in the summary, but do not weaken the Java contract or Playwright selector/action assertions.
</verification>

<success_criteria>
- MCON-02 closed: browser Send path reads multi-line prompt and creates/continues a DTO-backed run.
- MCON-03 closed: feed content progresses beyond empty/user-message state through rendered run events/statuses.
- MCON-05 closed: primary and backup Cancel controls invoke cancellation and update both status surfaces.
- MVER-03 strengthened: E2E cannot pass with selector-only buttons or a static feed; it asserts actual UI action consequences.
- No deferred Phase 13/14/15 scope or mobile-only backend API is added.
</success_criteria>

<output>
After completion, create `.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-05-SUMMARY.md`
</output>
