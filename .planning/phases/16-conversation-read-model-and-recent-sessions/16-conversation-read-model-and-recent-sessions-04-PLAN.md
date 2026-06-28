---
phase: 16-conversation-read-model-and-recent-sessions
plan: 04
type: execute
wave: 4
depends_on:
  - 16-conversation-read-model-and-recent-sessions-03
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/WebRuntimeBeanConfiguration.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppConsoleRunExecutionBridge.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SessionConversationControllerTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleConversationReadModelHookTest.java
autonomous: true
requirements:
  - SESS-01
  - SESS-04
must_haves:
  truths:
    - "REST clients can query typed recent sessions and typed session transcript through session-centric endpoints."
    - "Console bridge can load recent summaries and typed transcript data from App use cases without repository or Vaadin-memory shortcuts."
    - "Minimal Console proof hooks exist for Phase 17 without implementing full Kimi-style restore polish."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java"
      provides: "Session-centric typed conversation REST endpoints"
      exports: ["GET /api/sessions/recent", "GET /api/sessions/{sessionId}/transcript"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java"
      provides: "Vaadin-to-App conversation query seam"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java"
      provides: "Minimal proof hook for loading recent summaries/transcripts"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleConversationReadModelHookTest.java"
      provides: "Console does not use raw history maps as transcript proof"
  key_links:
    - from: "SessionController"
      to: "ConversationQueryService"
      via: "delegate immediately with RequestContext"
      pattern: "conversationQueryService\.listRecentSessions|getTranscript"
    - from: "AppConsoleRunExecutionBridge"
      to: "ConversationQueryService"
      via: "bridge methods only"
      pattern: "listRecentSessions|getTranscript"
    - from: "ConsoleView"
      to: "ConversationRunExecutionBridge methods"
      via: "minimal hook, not direct repository"
      pattern: "executionBridge\.listRecentSessions|executionBridge\.getTranscript"
---

<objective>
Expose the conversation read model through session-centric REST and minimal Console proof hooks.

Purpose: Phase 16 decisions D-02 and D-03 require session-oriented REST endpoints and only enough Console integration to prove the typed read model. This final plan wires the App read model into Adapter Web while preserving D-14 diagnostic endpoint compatibility and deferring Phase 17 visual restore UX.

Output: `/api/sessions/recent`, `/api/sessions/{sessionId}/transcript`, Spring wiring, bridge methods, and focused REST/Console hook tests.
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
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-CONTEXT.md
@.planning/research/ARCHITECTURE.md
@.planning/research/PITFALLS.md
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-01-SUMMARY.md
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-02-SUMMARY.md
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-03-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppConsoleRunExecutionBridge.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java

<interfaces>
Existing REST shape:
```java
@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    @GetMapping("/{sessionId}/history")
    public SessionHistoryResponse getSessionHistory(...)
}
```

Existing Console bridge shape:
```java
public interface ConsoleRunExecutionBridge {
    SessionResponse createSession();
    RunResponse createRun(String sessionId, CreateRunRequest request);
    EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence);
    RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request);
}
```

Required Plan 02 App service:
```java
public interface ConversationQueryService {
    PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor);
    ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor);
}
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add session-centric REST endpoints for typed conversation read model</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/WebRuntimeBeanConfiguration.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SessionConversationControllerTest.java</files>
  <behavior>
    - `GET /api/sessions/recent?limit=20&cursor=...` returns `PageResponse<SessionSummaryDto>` and delegates to `ConversationQueryService.listRecentSessions` with authenticated RequestContext.
    - `GET /api/sessions/{sessionId}/transcript?limit=100&cursor=...` returns `ConversationTranscriptResponse` and delegates to `ConversationQueryService.getTranscript` with authenticated RequestContext.
    - Existing `GET /api/sessions/{sessionId}/history` remains available as diagnostic/compatibility path, but tests prove typed transcript endpoint does not return `entries: List<Map<...>>`.
  </behavior>
  <action>Inject `ConversationQueryService` into `SessionController` and add session-centric endpoints per D-02. Preserve old `/history` behavior per D-14. Add/adjust Spring bean wiring for `DefaultConversationQueryService` and `ConversationTranscriptAssembler` in the web composition root, using repositories from Plans 02-03. Validate query params defensively: default limit 20 for recent, 100 for transcript; cap to a safe maximum (e.g. 100/500) if no existing helper exists. Do not add `/api/mobile/*`, `/api/conversations/*`, or a new unrelated top-level product fork.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=SessionConversationControllerTest test</automated>
  </verify>
  <done>REST controller tests pass for typed recent/transcript endpoints and old history endpoint remains compatible.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add minimal Console bridge proof hooks</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppConsoleRunExecutionBridge.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleConversationReadModelHookTest.java</files>
  <behavior>
    - Console bridge exposes `listRecentSessions(limit, cursor)` and `getTranscript(sessionId, limit, cursor)` using App `ConversationQueryService`, not repositories or HTTP calls from server-side Vaadin.
    - Console construction/attach can load recent summaries into `SessionListPanel` for proof without changing the Phase 17 Kimi layout.
    - Selecting a session can fetch typed transcript data and store/render a minimal proof state using `ConversationMessageDto`; no raw history maps are consumed.
    - Stable data hooks identify conversation summaries/messages, but no full restore UX/polish/streaming reducer is implemented.
  </behavior>
  <action>Extend `ConsoleRunExecutionBridge` and `AppConsoleRunExecutionBridge` with read-only conversation query methods per D-03. Add matching path helpers to `ConsoleHttpClient` for future client/browser use. Update `ConsoleView` minimally: load recent summaries on attach or construction through the bridge, and when `selectSession` is called, obtain typed transcript response and expose it to tests via a small accessor or minimal `ChatEventStreamPanel.replaceTranscriptForProof(List<ConversationMessageDto>)`. Keep this a proof hook: do not implement full Phase 17 Kimi-style visible restore UX, active-session banner, continuation polish, streaming bubble lifecycle, multi-turn model context, search, rename, archive, pin, delete, or provider/model readiness. Add tests proving `SessionHistoryResponse.entries` is not used by the Console proof path.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=WebConsoleConversationReadModelHookTest test</automated>
  </verify>
  <done>Console hook tests pass and Vaadin code consumes only typed conversation DTOs through the bridge.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Run adapter-web focused regression gate</name>
  <files>pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SessionConversationControllerTest.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleConversationReadModelHookTest.java</files>
  <behavior>
    - Existing Console mobile flow and user-flow tests continue to pass after bridge method additions.
    - Typed read-model tests pass without requiring a real provider API key, browser, Docker, or Phase 17 visual UX.
  </behavior>
  <action>Run a focused adapter-web regression and fix any compile/test failures introduced by constructor signature changes, demo bridge implementation, or message path helpers. Keep demo constructors deterministic and no-key. If existing tests instantiate `DemoConsoleRunExecutionBridge`, implement the new bridge methods there with typed sample DTOs so tests remain stable.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=SessionConversationControllerTest,WebConsoleConversationReadModelHookTest,WebConsoleMobileFlowContractTest,WebConsoleUserFlowTest test</automated>
  </verify>
  <done>Focused adapter-web regression passes and Phase 16 has executable proof that typed recent/transcript read models reach REST and Console seams.</done>
</task>

</tasks>

<verification>
Run the focused final Phase 16 adapter gate:

```bash
mvn -pl pi-agent-adapter-web -Dtest=SessionConversationControllerTest,WebConsoleConversationReadModelHookTest,WebConsoleMobileFlowContractTest,WebConsoleUserFlowTest test
```

If time permits, run the cross-module focused gate from Plans 01-04:

```bash
mvn -pl pi-agent-client,pi-agent-app,pi-agent-infrastructure,pi-agent-adapter-web -Dtest=ConversationDtoContractTest,ConversationTranscriptAssemblerTest,DefaultConversationQueryServiceTest,JdbcConversationReadModelIntegrationTest,LocalConversationReadModelPersistenceTest,SessionConversationControllerTest,WebConsoleConversationReadModelHookTest test
```
</verification>

<success_criteria>
- SESS-01 is visible through typed `/api/sessions/recent` and Console bridge recent-summary hooks.
- SESS-04 is visible through typed `/api/sessions/{sessionId}/transcript` and Console bridge transcript hooks, not raw run-event maps or Vaadin memory.
- Session-centric REST paths are used per D-02.
- Console work remains minimal proof only per D-03; full Phase 17 restore UX and all deferred ideas remain out of scope.
</success_criteria>

<output>
After completion, create `.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-04-SUMMARY.md`.
</output>
