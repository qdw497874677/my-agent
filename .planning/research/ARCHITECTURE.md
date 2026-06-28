# Architecture Research: Console Conversation Productization

**Project:** Pi Java Agent Platform  
**Milestone:** v1.2 Console 对话产品化  
**Researched:** 2026-06-28  
**Scope:** Integration architecture for historical sessions, restored messages, real streaming assistant bubble, multi-turn context, and local provider/model stability in the existing Java/Vaadin/COLA platform.  
**Overall confidence:** **HIGH** for current-code integration points and COLA boundary recommendations; **MEDIUM-HIGH** for the exact class names because the roadmap may choose smaller or larger increments; **LOW** for any broad market/UX claim not directly tied to existing code.

## Summary

v1.2 should add a **conversation product layer** on top of the existing run/session/event platform, not fork the Console into a mobile-era UI or create Vaadin-only history behavior. The platform already has the right primitives: COLA layering, session/run command/query services, persisted run projections, run event history, Spring MVC REST/SSE DTOs, a Vaadin Console route, local SQLite-backed repositories, and `DynamicAgentRuntime` streaming `model.delta` events. The missing architecture is the glue that turns those primitives into a coherent conversation: recent session summaries, typed transcript projection, run-event-to-bubble reduction, bounded session context assembly, and provider/model readiness snapshots.

The most important design decision is to **make conversation state an App/client read model**, not a Vaadin component state. `ConsoleView` may coordinate the UI, but it must not infer durable history from `ChatEventStreamPanel.messages()` or reconstruct model context from rendered text. The App layer should expose provider-neutral, UI-neutral DTOs such as `SessionSummaryDto`, `ConversationTranscriptResponse`, and `ConversationMessageDto`. Vaadin renders them; REST/SSE clients can reuse them; future CLI/TUI clients are not forced to reverse-engineer Vaadin behavior.

Real streaming should preserve the existing public `RunEventDto` / `EventHistoryResponse` boundary while adding a Console-specific adapter reducer. The public contract remains run events and replay-before-subscribe SSE. The Vaadin Console should reduce `model.delta`, terminal lifecycle events, errors, and cancellation into a single mutable assistant bubble keyed by `sessionId + runId + stepId/messageId`. Polling every 750 ms can remain a fallback, but the target product architecture is Vaadin Push or an equally explicit live event subscription path using `UI.access(...)` and detach cleanup.

Multi-turn context should be assembled from the same canonical transcript, with a bounded turn/token/character policy, and inserted into `RunContext.sessionContext()` or a provider-neutral model-request message list before `DynamicAgentRuntime` calls the model. The current `DefaultRunDispatcher.sessionContext()` returns empty lists and `DynamicAgentRuntime` sends `new ModelRequest(context, List.of())`; v1.2 must close that seam in App/Infrastructure/runtime code, not in `ConsoleView`.

This document intentionally replaces stale v1.1 mobile architecture. The milestone is **not** “adapt mobile APIs to Console”; it is “productize conversation using general platform boundaries”.

## Current Architecture Evidence

| Area | Current Evidence | Architectural Meaning |
|------|------------------|-----------------------|
| Console UI | `ConsoleView` owns `selectedSessionId`, `activeRunId`, `activeRunNextAfterSequence`, polls every 750 ms, and `selectSession()` only returns a history path. | Current conversation continuity is mostly view-local. Session selection is not transcript restore. |
| Chat feed | `ChatEventStreamPanel` stores `List<String> messages`, clears empty state on first append, and aggregates assistant chunks via one `activeAssistantLine`. | Good starter for visual bubbles, but not durable history or stable stream aggregation. |
| Bridge | `AppConsoleRunExecutionBridge` delegates create session/run, list events, cancel to App services. | Correct adapter seam exists; extend it rather than injecting repositories into Vaadin. |
| Run command | `DefaultRunCommandService.createRun()` persists a run, enqueues `QueuedRun`, and records audit. | Run creation is already App-layer orchestration; model/provider snapshots and user input persistence should be added here/repository-side, not UI-only. |
| Run query | `DefaultRunQueryService.listEvents()` maps `RunEvent` to `RunEventDto`, including `textDelta`, `modelRef`, provider/model IDs, finish reason, and tool-safe summaries. | Existing event DTO is sufficient for public streaming/replay; add conversation projection above it. |
| Local profile | `LocalDevRuntimeBeanConfiguration.LocalDevStores.history()` returns empty entries; SQLite loads all sessions/runs/events globally. | Local history restore is not product-ready; add same ports/read-model semantics to local profile. |
| Runtime context | `DefaultRunDispatcher.sessionContext()` returns empty message/artifact lists. | Multi-turn context has no injection point yet beyond the existing `SessionContext` record. |
| Dynamic runtime | `DynamicAgentRuntime.start()` streams with `new ModelRequest(context, List.of())`, publishes fallback text if unconfigured, and `cancel()` is empty. | Real model calls are stateless today; provider fallback exists but must be labeled and included in transcript safely. |
| Existing REST | `SessionController` has `GET /api/sessions/{sessionId}/history` returning generic `SessionHistoryResponse(List<Map<String,Object>> entries)`. | Preserve route compatibility if useful, but add typed conversation DTOs instead of rendering raw maps. |

## Recommended Architecture

### Layered Shape

```text
Adapter Web (Vaadin + REST/SSE)
  ConsoleView
  ChatEventStreamPanel
  ConversationEventReducer / ChatStreamAggregator
  ConversationConsoleBridge (extends current AppConsoleRunExecutionBridge)
  SessionController / optional ConversationController
        |
        v
App Layer (Use cases + assemblers)
  SessionQueryService.listRecentSessions(...)
  ConversationQueryService.getTranscript(...)
  ConversationTranscriptAssembler
  SessionConversationContextAssembler
  RunCommandService.createRun(...) [pin model/provider + current user input]
        |
        v
Domain / Runtime Core
  SessionContext with provider-neutral message entries
  ModelRequest / RunContext carrying bounded history
  RunEvent / RunEventPayload.ModelDeltaPayload
  AgentRuntime remains Vaadin/Spring/SQLite-free
        ^
        |
Infrastructure
  JDBC/PostgreSQL repositories
  local SQLite repository/profile implementation
  RunEventStore / RunProjectionRepository / SessionRepository extensions
  DynamicAgentRuntime and OpenAI-compatible adapter
```

### Architectural Principle

Use **one canonical conversation projection** for three consumers:

1. **UI restore** — selected session hydrates bubbles from persisted conversation messages.
2. **Model context** — next run receives bounded prior user/assistant/tool-summary turns derived from the same projection.
3. **Public clients** — REST/CLI/TUI can consume typed transcript/session summaries without Vaadin or mobile-specific DTOs.

Do **not** create separate history logic for Vaadin, mobile, local SQLite, and REST. That would make restore, context, and audit disagree.

## Component Boundaries

| Component | New / Modified | Layer | Responsibility | Communicates With |
|-----------|----------------|-------|----------------|-------------------|
| `SessionSummaryDto` | New | `pi-agent-client` | Stable recent-session item: session id, title, status, last message preview, updated time, active run id/status, provider/model snapshot. | App query, REST, Vaadin, future CLI/TUI. |
| `ConversationMessageDto` | New | `pi-agent-client` | Stable transcript item: role, text, status, session/run/step refs, sequence range, created/updated time, metadata, redaction. | Transcript API, Vaadin renderer, context assembler tests. |
| `ConversationTranscriptResponse` | New | `pi-agent-client` | Typed replacement/projection for raw `SessionHistoryResponse.entries`; includes messages, active run, cursor/hasMore. | Conversation query service and adapters. |
| `ConversationQueryService` or expanded `SessionQueryService` | New/Modified | App | List recent sessions and load typed transcript. Prefer a dedicated `ConversationQueryService` if `SessionQueryService` would become too broad. | Repositories, transcript assembler, REST/Vaadin bridge. |
| `ConversationTranscriptAssembler` | New | App | Reduce persisted runs/events/messages into ordered user/assistant/tool/status transcript. Coalesce model deltas by run/step. | `RunProjectionRepository`, `RunEventStore`, `SessionRepository`. |
| `SessionConversationContextAssembler` | New | App or Domain-facing App service | Build bounded provider-neutral history for the next run. Applies role filter, redaction, token/char/window budget. | Transcript assembler, dispatcher/runtime context. |
| `ConversationContextPolicy` | New | Domain/App | Explicit budget policy: max turns, max chars/tokens approximation, tool summary inclusion, truncation metadata. | Context assembler, tests, optional UI disclosure. |
| `ConversationConsoleBridge` | Modified/new from `AppConsoleRunExecutionBridge` | Adapter Web | Vaadin-facing facade: create/reuse session, create run, list live events, list recent sessions, load transcript, cancel. | App use cases only. No direct repositories. |
| `ConsoleView` | Modified | Adapter Web | UI coordinator only: load recent sessions on attach, select session, hydrate chat panel, submit, subscribe/poll live events. | Bridge, reducer, Vaadin components. |
| `ChatEventStreamPanel` | Modified | Adapter Web | Bubble component API: clear/replace transcript, append user, begin assistant, append delta, mark terminal, show tool/error cards. | `ConsoleView` and reducer. Stores UI state only. |
| `ConversationEventReducer` / `ChatStreamAggregator` | New | Adapter Web, with App-level unit-testable logic if useful | Convert `RunEventDto` into UI deltas. Idempotent by event id/sequence; keyed by session/run/step/message. | `ConsoleView`, `ChatEventStreamPanel`, `RunEventDto`. |
| `RunProjectionRepository` extensions | Modified | App port + Infrastructure | Query runs by session ordered by time; get active/latest run; filter by tenant/user/session. Store run input and provider/model snapshot. | App assemblers, JDBC/SQLite impls. |
| `RunEventStore` extensions | Modified | App port + Infrastructure | Query events by run(s) and session with ownership filters; avoid `runId`-only leak. | Query service, transcript assembler, live event replay. |
| `SessionRepository` extensions | Modified | App port + Infrastructure | List recent sessions by tenant/user and update session summary metadata. | Session query, transcript restore, local profile. |
| `DefaultRunDispatcher` | Modified | Infrastructure execution | Build non-empty `SessionContext` before runtime start; pass context and cancellation faithfully. | Context assembler, runtime. |
| `DynamicAgentRuntime` | Modified | Adapter Web local runtime | Use `RunContext.sessionContext().messages()` or model-request history; label fallback; cancel stream if token requested. | Model client, EventSink, provider config. |
| `SqliteLocalPersistence` | Modified | Adapter Web local infra | Add indexes and targeted loads for sessions/runs/events; keep behavior aligned with App ports. | LocalDevStores repositories. |
| Provider config response/readiness | Modified | Adapter Web/client DTO | Surface ready/error/masked key/model list/selected model; pin model at run creation. | Vaadin model bar, provider config store/controller. |

## Data Flow Changes

### 1. Initial Console Load → Recent Sessions

```text
ConsoleView.attach
  -> ConversationConsoleBridge.listRecentSessions(context, limit)
  -> App ConversationQueryService.listRecentSessions
  -> SessionRepository.listRecent(context, limit)
     + RunProjectionRepository.latestRunBySession(...)
  -> List<SessionSummaryDto>
  -> SessionListPanel.showSessions(...)
```

**Key rule:** recent sessions must come from durable App/Infrastructure state. Do not populate history only from sessions created during the current Vaadin view lifetime.

### 2. Session Selection → Transcript Restore

```text
User selects session card
  -> ConsoleView.selectSession(sessionId)
  -> bridge.getTranscript(sessionId, limit/cursor)
  -> ConversationQueryService.getTranscript(context, sessionId)
  -> ConversationTranscriptAssembler
       - loads session with tenant/user ownership
       - loads runs for session ordered by createdAt/updatedAt
       - loads user input / run messages / model.delta events
       - folds deltas into assistant message per run/step
       - marks failed/cancelled/partial terminal state
  -> ConversationTranscriptResponse
  -> ChatEventStreamPanel.replaceTranscript(messages)
  -> ConsoleView sets selectedSessionId, activeRunId if response has non-terminal active run
```

**Key rule:** restore renders typed `ConversationMessageDto`, not `SessionHistoryResponse.entries` maps and not raw `RunEventRenderer` cards.

### 3. Submit Message → Run Creation → Assistant Streaming

```text
User submits text
  -> ChatEventStreamPanel emits submit text
  -> ConsoleView/bridge ensures selected session exists
  -> RunCommandService.createRun(context, sessionId, CreateRunRequest)
       - persists run input as conversation user message source
       - pins provider/model ref for this run in run metadata
       - enqueues QueuedRun
  -> ConsoleView appends user bubble and begins pending assistant bubble keyed by runId
  -> RunDispatcher claims queued run
  -> SessionConversationContextAssembler builds bounded history for session
  -> RunContext(sessionContext = bounded messages) passed to AgentRuntime
  -> DynamicAgentRuntime streams model.delta through EventSink
  -> public RunEventStore persists events and SSE can replay/stream them
  -> Vaadin live path receives/listEvents deltas
  -> ConversationEventReducer appends textDelta to same assistant bubble
  -> terminal run event marks bubble complete/failed/cancelled
```

**Key rule:** append the user's message only once visually and only once in model context. The context assembler must prove the current prompt is not duplicated when it derives history from persisted run input.

### 4. Multi-Turn Context Assembly

```text
RunDispatcher.dispatchClaimed(queuedRun)
  -> requestContext(queuedRun)
  -> conversationContextAssembler.assemble(context, queuedRun.sessionId(), queuedRun.runId(), queuedRun.input())
       - uses transcript before/current run according to policy
       - includes previous user/assistant turns
       - may include safe tool summaries later
       - excludes raw audit/provider/tool sensitive payloads
       - applies max turns/chars/token estimate
  -> new SessionContext(messages = bounded MessageEntry list, ...)
  -> new RunContext(..., sessionContext, ...)
  -> AgentRuntime.start(context)
  -> ModelRequest(context, toolResults)
```

**Integration preference:** use the existing `SessionContext.messages` field as the runtime-facing history carrier if its `SessionEntryPayload.MessageEntry` role/content shape is adequate. If it is too session-tree-specific, introduce a small provider-neutral `ConversationTurn`/`ModelMessage` in Domain and adapt it inside `ModelRequest`. Do not pass Spring AI message classes across Domain/App.

### 5. Provider/Model Stability

```text
Model bar loads ProviderConfigStore.current()
  -> ProviderConfigController.validation/readiness endpoint
  -> ProviderValidationResponse(ready, maskedKey, selectedModel, models, error, validatedAt)
  -> UI status chip and refresh errors

On createRun
  -> resolve selected provider/model from current config
  -> persist providerId/modelId/modelRef in run metadata/result/projection
  -> DynamicAgentRuntime uses run snapshot if available; otherwise current local config fallback
```

**Key rule:** model selector changes affect subsequent runs only. History should display the provider/model actually used by each assistant response.

## Recommended API / DTO Shape

These are product-level APIs; they are **not mobile-only** and **not Vaadin DTOs**.

```java
public interface ConversationQueryService {
    PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit, String cursor);
    ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit, String cursor);
}

public record SessionSummaryDto(
        String sessionId,
        String title,
        String status,
        String lastMessagePreview,
        Instant createdAt,
        Instant updatedAt,
        String activeRunId,
        String activeRunStatus,
        String providerId,
        String modelId,
        Map<String, Object> metadata) {
}

public record ConversationTranscriptResponse(
        String sessionId,
        List<ConversationMessageDto> messages,
        String activeRunId,
        String activeRunStatus,
        long nextAfterSequence,
        String nextCursor,
        boolean hasMore) {
}

public record ConversationMessageDto(
        String messageId,
        String sessionId,
        String runId,
        String stepId,
        String role,       // user | assistant | tool | system | error
        String text,
        String status,     // complete | streaming | failed | cancelled | partial
        Instant createdAt,
        Instant updatedAt,
        Long firstSequence,
        Long lastSequence,
        Map<String, Object> metadata) {
}
```

### REST Boundary Recommendation

Keep existing REST/SSE run endpoints unchanged. Add conversation endpoints or extend session endpoints with typed DTOs:

| Endpoint | Recommendation | Why |
|----------|----------------|-----|
| `GET /api/sessions/{id}/history` | Keep for compatibility; may internally delegate to transcript assembler but should not be the only product API if it stays generic maps. | Avoid breaking existing tests/clients. |
| `GET /api/conversations/recent?limit=...` or `GET /api/sessions/recent` | Add typed recent session summaries. | Needed by Console load and future clients. |
| `GET /api/conversations/{sessionId}/transcript?limit=...` or `GET /api/sessions/{id}/transcript` | Add typed transcript. | Avoid raw map rendering and mobile-only fork. |
| `GET /api/sessions/{sessionId}/runs/{runId}/events` / SSE | Preserve existing run-event contract. | Public real-time stream remains provider-neutral event history. |

Prefer naming around **sessions/conversations** rather than mobile. `conversation` is a product projection of sessions/runs/events; it should not imply a new storage root unless the implementation later needs a materialized read table.

## Seams and Integration Points

### ConsoleView seam

`ConsoleView` should remain the Vaadin coordinator but lose product-state authority.

**Modify:**
- On attach, call bridge `listRecentSessions()` and populate `SessionListPanel`.
- On `selectSession()`, load `ConversationTranscriptResponse`, call `chatPanel.replaceTranscript(...)`, set `selectedSessionId`, restore active run cursor if needed.
- On send, prevent overlapping active run in the selected session unless explicitly supported.
- Route new events through `ConversationEventReducer` before rendering.
- Keep `RunEventRenderer` for collapsible tool/runtime/debug cards, not primary assistant text.

**Do not:**
- Build model context in `ConsoleView`.
- Use `chatPanel.messages()` as persisted history.
- Create `/mobile/...` or Console-only history APIs.

### ChatEventStreamPanel seam

Turn the panel into a typed bubble component:

```java
void replaceTranscript(List<ConversationMessageDto> messages);
void appendUserMessage(String text, String messageId, String runId);
void beginAssistantMessage(String runId, String stepId);
void appendAssistantDelta(String runId, String stepId, String delta);
void markAssistantTerminal(String runId, String status, Map<String,Object> metadata);
void showErrorBubble(String runId, String safeMessage);
void showToolCard(ConversationMessageDto toolMessage);
```

Keep `messages()` only as a test/helper view of rendered text, not source-of-truth. Add stable selectors: `data-message-role`, `data-session-id`, `data-run-id`, `data-stream-state`.

### AppConsoleRunExecutionBridge seam

Extend or replace it with `ConversationConsoleBridge`:

```java
interface ConversationConsoleBridge extends ConsoleRunExecutionBridge {
    PageResponse<SessionSummaryDto> listRecentSessions(int limit, String cursor);
    ConversationTranscriptResponse getTranscript(String sessionId, int limit, String cursor);
}
```

This preserves the correct dependency direction: Vaadin → bridge → App use cases. The bridge should not load `SqliteLocalPersistence`, `RunEventStore`, or repositories directly.

### DefaultRunQueryService seam

Keep `DefaultRunQueryService` as a run-scoped event/detail query. Add conversation read-model logic beside it rather than overloading it with UI semantics.

**Required security improvement:** the current `listEvents(context, sessionId, runId, ...)` calls `runEventStore.listByRun(runId, ...)`. v1.2 transcript/event restore should add ownership-aware repository methods such as:

```java
List<RunEvent> listBySessionRun(RequestContext context, String sessionId, String runId, long afterSequence, int limit);
List<RunEvent> listBySessionRuns(RequestContext context, String sessionId, List<String> runIds, int limit);
```

At minimum, validate `runProjectionRepository.findRun(context, sessionId, runId)` before returning events. Avoid relying on UI-provided `sessionId` in responses.

### DefaultRunDispatcher seam

`DefaultRunDispatcher` is the right place to hydrate runtime context because it owns the transition from queued App run to Domain `RunContext`.

**Modify:**
- Inject a `ConversationContextAssembler`/`SessionContextAssembler` port.
- Replace static empty `sessionContext(queuedRun)` with assembled bounded context.
- Include context metadata in audit/debug result if useful: included turn count, truncated count, policy id.
- Preserve Domain purity: assembler returns Domain/App-neutral messages, not Vaadin or Spring AI types.

### DynamicAgentRuntime seam

`DynamicAgentRuntime` should consume the non-empty context rather than querying persistence.

**Modify:**
- Convert `context.sessionContext().messages()` into the provider request format inside the model adapter/runtime path.
- Stop assuming `new ModelRequest(context, List.of())` implies no history; either `ModelRequest` reads history from `RunContext` or add a new message list field.
- Use provider/model snapshot from the run if available; current global config is fallback for local profile.
- Make `cancel(runId, reason)` meaningful if the streaming client supports cancellation; otherwise at least respect `context.cancellationToken()` during sink publishing and stop appending after terminal cancellation.

### Local SQLite seam

`LocalDevRuntimeBeanConfiguration` should stay a local infrastructure composition root, not a separate product architecture.

**Modify:**
- `LocalDevStores.history()` must return typed transcript or delegate to the same assembler.
- `SqliteLocalPersistence.loadSessions()` should order by `updated_at DESC` and support limit.
- Add targeted methods: `loadRunsBySession(sessionId)`, `loadEventsByRun(runId, afterSequence, limit)`, `loadEventsBySession(sessionId, ...)`.
- Add indexes: `local_sessions(user_id, updated_at)`, `local_runs(session_id, updated_at)`, `local_events(run_id, sequence)`.
- Store run input and provider/model snapshot. Current `local_runs` has no input JSON or model metadata, which makes user-message transcript restore depend on UI memory unless fixed.

## Patterns to Follow

### Pattern 1: Conversation Projection as App Read Model

**What:** A transcript is assembled from durable session/run/event state into typed conversation messages.

**When:** Session restore, recent session previews, context assembly, and future transcript APIs.

**Why:** It prevents Vaadin from becoming the history source and prevents raw runtime events from leaking into chat UX.

```java
public final class ConversationTranscriptAssembler {
    public ConversationTranscriptResponse assemble(RequestContext context, String sessionId, int limit) {
        var session = sessionRepository.findById(context, sessionId).orElseThrow();
        var runs = runProjectionRepository.listRunsBySession(context, session.sessionId(), limit);
        var messages = new ArrayList<ConversationMessageDto>();
        for (var run : runs.items()) {
            messages.add(userMessageFromRunInput(run));
            messages.addAll(assistantAndToolMessagesFromEvents(run));
        }
        return new ConversationTranscriptResponse(sessionId, messages, activeRunId(runs), activeRunStatus(runs), 0, null, false);
    }
}
```

### Pattern 2: Event Reducer for Live UI

**What:** Convert run events into UI deltas separate from durable transcript assembly.

**When:** Live stream/poll updates after run creation or after restoring an active run.

**Why:** Streaming needs mutation (`append delta`) while transcript restore needs stable messages (`full text`). They share semantics but not rendering mechanics.

```java
public final class ConversationEventReducer {
    public List<ConversationUiDelta> reduce(RunEventDto event) {
        if ("model.delta".equals(event.type())) {
            String delta = string(event.payload().get("textDelta"));
            if (!delta.isBlank()) {
                return List.of(ConversationUiDelta.appendAssistant(event.runId(), event.stepId(), delta, event.eventId()));
            }
            if (event.payload().containsKey("finishReason")) {
                return List.of(ConversationUiDelta.completeAssistant(event.runId(), event.stepId(), event.payload()));
            }
        }
        if (isTerminalStatus(event)) {
            return List.of(ConversationUiDelta.markTerminal(event.runId(), status(event)));
        }
        return List.of(ConversationUiDelta.optionalRuntimeCard(event));
    }
}
```

### Pattern 3: Context Derivation, Not Memory-as-History

**What:** Complete transcript is stored independently; model context is a bounded derivation.

**When:** Every queued run before `AgentRuntime.start()`.

**Why:** Users need full history restore, while models need a safe, bounded prompt context. These have different retention and redaction policies.

```java
public final class SessionConversationContextAssembler {
    public SessionContext assemble(RequestContext context, QueuedRun run, ConversationContextPolicy policy) {
        var transcript = conversationQueryService.getTranscript(context, run.sessionId(), policy.maxTranscriptItems(), null);
        var prior = transcript.messages().stream()
                .filter(message -> isContextEligible(message, run.runId()))
                .filter(message -> List.of("user", "assistant").contains(message.role()))
                .collect(toBoundedRecentTurns(policy));
        return new SessionContext(toMessageEntries(prior, run.input()), List.of(), List.of(), List.of(), List.of(), Optional.of(workspaceScope(run)), List.of());
    }
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Vaadin as the History Database

**What:** Use `ChatEventStreamPanel.messages()` or component tree text to restore history or assemble context.  
**Why bad:** Loses role/status/run metadata, disappears on refresh, cannot serve REST/CLI, and cannot enforce redaction.  
**Instead:** Persist run input/events and assemble typed transcript in App.

### Anti-Pattern 2: Mobile-Only Conversation APIs

**What:** Add endpoints or DTO names scoped to mobile/v1.1 responsive work.  
**Why bad:** v1.2 Console conversation productization is a general product capability; mobile-specific APIs will fork future CLI/TUI/Admin clients.  
**Instead:** Use session/conversation DTOs under existing REST/client module boundaries.

### Anti-Pattern 3: Raw Event Log as Chat Transcript

**What:** Render every `run.status`, `model.delta`, tool lifecycle, and audit-like item as a chat row.  
**Why bad:** Users see operational noise, deltas fragment answers, and restored sessions feel broken.  
**Instead:** Project events into user/assistant/tool/error messages; keep raw events in run detail/expanders.

### Anti-Pattern 4: Session ID Equals Context

**What:** Assume reusing `selectedSessionId` means the model remembers prior turns.  
**Why bad:** Current runtime sends empty history; UI continuity and model continuity diverge.  
**Instead:** Add context assembler and test with a fake model that inspects prior turns.

### Anti-Pattern 5: Spring AI Memory or Vector DB for v1.2 Short-Term Context

**What:** Introduce external memory frameworks to solve basic multi-turn session history.  
**Why bad:** Adds scope, leaks library abstractions, and can conflate history/memory/audit.  
**Instead:** Use project-owned transcript and bounded recent-turn context; add long-term memory later as a separate capability.

## Suggested Build Order

This order minimizes rewrites: first create canonical data/read models, then hydrate UI, then stream live deltas, then feed context to the model, then harden provider/local profile.

### Phase A — Conversation Read Model and Recent Sessions

**Goal:** App/client DTOs and repository ports exist before UI polish.

1. Add `SessionSummaryDto`, `ConversationMessageDto`, `ConversationTranscriptResponse` in `pi-agent-client`.
2. Add `ConversationQueryService` or expand `SessionQueryService` with:
   - `listRecentSessions(context, limit, cursor)`
   - `getTranscript(context, sessionId, limit, cursor)`
3. Extend repository ports for:
   - list sessions by tenant/user ordered by `updatedAt DESC`
   - list runs by session with tenant/user filters
   - list events by run/session with ownership filters
4. Implement `ConversationTranscriptAssembler` with golden tests for:
   - user input + deltas + finish → two messages
   - failed provider → user + failed/error assistant state
   - cancelled run → partial/cancelled assistant state
5. Implement local SQLite targeted loads/indexes and cloud JDBC equivalent if present.

**Why first:** Without this, UI restore and context will be built on unstable rendered strings or raw maps.

### Phase B — Console Session Restore UX

**Goal:** Selecting history hydrates bubbles and preserves selected session continuation.

1. Extend `AppConsoleRunExecutionBridge` into `ConversationConsoleBridge`.
2. `ConsoleView.attach` loads recent sessions.
3. `ConsoleView.selectSession()` loads transcript, calls `chatPanel.replaceTranscript(...)`, sets active run/cursor.
4. `ChatEventStreamPanel` gains typed bubble API and stable selectors.
5. Playwright/Vaadin tests assert refresh/select restores prior user and assistant messages.

**Dependency:** Phase A typed transcript.

### Phase C — Streaming Bubble Lifecycle

**Goal:** Active assistant response updates one bubble with correct terminal states.

1. Add `ConversationEventReducer` / `ChatStreamAggregator` keyed by session/run/step/message.
2. Change `ConsoleView.appendRunEvents()` to route `model.delta` and terminal lifecycle through reducer.
3. Begin a pending assistant bubble immediately after run creation.
4. Deduplicate by event id/sequence and stop appending after terminal cancellation/failure.
5. Keep polling fallback initially; add Vaadin Push or explicit SSE integration after reducer tests pass.
6. Add fake slow-stream tests that assert text changes before terminal completion and all chunks land in one assistant bubble.

**Dependency:** Phase B bubble API; existing run-event DTO mapping.

### Phase D — Multi-Turn Runtime Context

**Goal:** The model receives bounded selected-session history.

1. Add `ConversationContextPolicy` defaults: recent N turns + max chars/token estimate.
2. Add `SessionConversationContextAssembler` using typed transcript.
3. Inject assembler into `DefaultRunDispatcher`; replace empty `sessionContext(queuedRun)`.
4. Update `DynamicAgentRuntime` / model adapter to send history from `RunContext.sessionContext().messages()`.
5. Add fake model contract test: second prompt can see first user/assistant turn; current prompt appears exactly once.
6. Add redaction/security tests: raw tool/audit/provider secrets do not enter context.

**Dependency:** Phase A transcript projection and run input persistence.

### Phase E — Provider/Model and Local Profile Stability

**Goal:** Local provider config and persisted history survive reloads and are explainable.

1. Add readiness/validation DTO with `ready`, `maskedKey`, `selectedModel`, `models`, `error`, `validatedAt`.
2. Surface refresh/list/send errors in `ConsoleView` model bar and composer.
3. Pin provider/model ref per run at creation; display a small model chip on assistant messages/history.
4. Store run input/model metadata in SQLite and production run projection.
5. Make no-provider fallback visually labeled as fallback/local, not indistinguishable from real model output.
6. Validate local restart: sessions, transcript, selected model/config restore.

**Dependency:** Phase A repository/schema changes; Phase C UI status semantics.

### Phase F — Verification and Regression Hardening

**Goal:** Prevent productization regressions from passing as “text eventually appears”.

1. App unit tests for transcript and context assembly.
2. Repository tests for tenant/user/session ownership filters.
3. Vaadin component tests for bubble grouping, terminal states, and selectors.
4. Playwright product path:
   - no-key fallback visible and labeled
   - recent session list loads after refresh
   - select old session restores bubbles
   - continue selected session reuses same session ID
   - fake slow deltas aggregate into one assistant bubble
   - cancel marks partial and stops appending
   - provider refresh/send error is visible

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Transcript source is ambiguous | UI restore, context, and audit disagree. | Make `ConversationTranscriptAssembler` the canonical read-model path for restore/context. |
| Run input is not persisted in local projection | Restored transcript lacks user messages after reload. | Add input JSON / message entries to run projection or session entries; update SQLite schema. |
| Event queries leak across session/tenant | Security issue in history restore. | Add ownership-aware repository methods and negative tests. |
| Deltas duplicate on replay/poll | Assistant bubble repeats text. | Idempotent reducer keyed by event id/sequence; track cursor per run. |
| Tool/status events corrupt assistant bubble | Chat becomes fragmented or misleading. | Route tool/status to inline cards; only `model.delta.textDelta` appends assistant text. |
| Multi-turn context duplicates current prompt | Model repeats or overweights prompt. | Context assembler test: current user input appears exactly once. |
| Context includes raw tool/audit secrets | Privacy/security regression. | Default context includes only user/assistant text; tool summaries opt-in and redacted. |
| Vaadin Push updates outside UI lock | Runtime/session lock exceptions. | Use `UI.access(...)`, capture UI on attach, unregister on detach; keep polling fallback until validated. |
| Provider config changes rewrite history meaning | Users cannot tell which model answered. | Pin provider/model at run creation and display per-run chip. |
| Local SQLite diverges from production behavior | Tests pass locally but fail in cloud. | Same App ports/read-model assemblers for SQLite and JDBC; local only swaps infrastructure implementation. |

## Verification Gates for Roadmap

| Gate | Layer | Acceptance Signal |
|------|-------|-------------------|
| Transcript assembler golden tests | App | Multiple `model.delta` events fold into one assistant `ConversationMessageDto`. |
| Recent sessions repository test | Infrastructure | Sessions ordered by updated time and filtered by tenant/user. |
| Session restore UI test | Adapter Web | Selecting historical session clears current feed and renders prior user/assistant bubbles. |
| Streaming UI test | Adapter Web/E2E | Slow fake model changes one assistant bubble before terminal completion. |
| Multi-turn context test | App/Runtime | Fake model receives prior turn and current prompt exactly once. |
| Cancellation test | Runtime/UI | Cancelled run stops appending deltas and shows partial/cancelled state. |
| Provider config test | Adapter Web/local | Masked key only; readiness/error visible; selected model persists after reload. |
| Security negative test | App/Infrastructure | Events/runs from another session/tenant are not included in transcript. |
| Architecture boundary test | ArchUnit/static | `pi-agent-app`/`pi-agent-domain` do not depend on Vaadin, adapter-web provider classes, SQLite, or Spring AI message classes. |

## Decision Log / Recommendations

| Decision | Recommendation | Confidence |
|----------|----------------|------------|
| Where to put historical sessions | App read model + client DTOs, backed by repository ports. | HIGH |
| How to restore messages | Typed transcript projection; do not render raw `SessionHistoryResponse.entries`. | HIGH |
| How to stream UI | Existing run events + Console reducer + Vaadin bubble API; push/SSE preferred, polling fallback allowed. | HIGH |
| How to do multi-turn | Bounded context assembler before runtime; do not build in Vaadin or Spring AI memory. | HIGH |
| Whether to add mobile APIs | Do not. Conversation APIs must be general session/conversation APIs. | HIGH |
| Whether to add React/WebFlux/vector DB | Do not for this milestone. Current stack is sufficient. | HIGH |
| Where to stabilize local provider config | Adapter/local profile for UI/config, but run/session/history through shared App ports. | MEDIUM-HIGH |

## Sources

- Project context: `.planning/PROJECT.md` v1.2 Console 对话产品化 requirements. Confidence: **HIGH**.
- Existing research: `.planning/research/STACK.md`, `.planning/research/FEATURES.md`, `.planning/research/PITFALLS.md` for stack, feature, and risk findings. Confidence: **HIGH**.
- Current code read 2026-06-28: `ConsoleView.java`, `ChatEventStreamPanel.java`, `AppConsoleRunExecutionBridge.java`, `LocalDevRuntimeBeanConfiguration.java`, `DefaultRunCommandService.java`, `DefaultRunQueryService.java`, `DefaultRunDispatcher.java`, `DynamicAgentRuntime.java`, `SessionController.java`, `SessionQueryService.java`, `RunProjectionRepository.java`, `RunEventStore.java`, `SessionRepository.java`, `SqliteLocalPersistence.java`, `ModelRequest.java`, `RunContext.java`, `SessionContext.java`. Confidence: **HIGH**.
- Official Vaadin guidance referenced by prior research: server push/UI updates require proper server-side UI access semantics. Confidence: **HIGH** for push/`UI.access` principle; exact configuration should be validated during implementation.
- Official Spring AI memory distinction referenced by prior pitfalls research: model memory/context is not complete product history. Confidence: **HIGH** for keeping project-owned transcript separate from model context.

---

*Architecture research overwrite for v1.2 Console conversation productization. This is not v1.1 mobile adaptation guidance.*
