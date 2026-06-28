# Stack Research: Console Conversation Productization

**Project:** Pi Java Agent Platform  
**Domain:** Kimi-style Console conversation productization in an existing Java/Spring MVC/Vaadin Agent Platform  
**Researched:** 2026-06-28  
**Scope:** Stack additions/changes for historical session selection/restore, real streaming assistant bubble UI, multi-turn session context, and local provider/model configuration stability.  
**Overall confidence:** **HIGH** for “do not add a new frontend/runtime stack”; **MEDIUM-HIGH** for Vaadin push/poll and Spring MVC SSE details; **MEDIUM** for SQLite local persistence hardening because current local persistence is custom and should be stabilized by implementation tests.

## Summary

The right stack direction is **not a major dependency expansion**. The current stack already contains the essential productization building blocks: Java 21, Maven, COLA boundaries, Spring Boot **3.5.9**, Spring AI **1.1.5**, Vaadin **24.8.4**, Spring MVC REST/SSE, provider-neutral run events, OpenAI-compatible streaming, SQLite local persistence, and Vaadin poll/push-capable UI. The v1.2 work should add **small application/API seams and UI state management**, not React/Next.js, WebFlux, Kafka, Redis, LangChain memory, or a mobile-only API fork.

The core addition should be a **conversation read model** in App/Client backed by existing run/session/event persistence: session list, active session restore, normalized message transcript, and conversation context assembly. Today `ConsoleView` only keeps UI-local state (`selectedSessionId`, `activeRunId`, `activeRunNextAfterSequence`) and `selectSession()` returns a history path without actually restoring chat messages. `DefaultRunQueryService` exposes run-scoped events/messages, but not a product-level session transcript or recent-session list. Productization requires these seams before UI polish can be reliable.

For real streaming UI, use the existing event stream contract and Vaadin UI update model. `DynamicAgentRuntime` already publishes `RunEventType.MODEL_DELTA` chunks from the OpenAI-compatible streaming client, and `DefaultRunQueryService` maps these to `payload.textDelta`/`payload.text`. `ChatEventStreamPanel` already has an `activeAssistantLine` append behavior. The missing stack seam is a **dedicated conversation event reducer** that treats model deltas as assistant-token updates for the current run, not as generic event cards. Use Vaadin `@Push` + `UI.access(...)` where possible for lower-latency server-side UI updates; keep the current 750 ms polling as a fallback or initial implementation if push introduces deployment friction.

For multi-turn context, do **not** add a vector database, long-term memory framework, or LangChain/LangGraph layer. Build a deterministic **SessionConversationContextAssembler** in the App layer that reads the current session transcript, applies a configurable turn/token/window budget, and supplies previous user/assistant messages to the existing `ModelRequest`. The current `DynamicAgentRuntime` calls `client.stream(new ModelRequest(context, List.of()), ...)`, so it explicitly sends no history. Fix this at the runtime/App seam using project-owned message DTOs and provider-neutral model request messages.

## Recommended Stack Additions / Changes

### Keep Current Core Stack

| Area | Current / Recommended | Purpose | Rationale |
|------|-----------------------|---------|-----------|
| Java | **21** | Runtime and SDK baseline | Already validated. No new Java version is needed for conversation UI/productization. |
| Spring Boot | **3.5.9** current; stay on 3.5.x | REST, SSE, security, app composition | Avoid Boot 4/Vaadin 25 migration churn. v1.2 risks are conversation state and UI streaming, not platform major versions. |
| Spring MVC | Existing `spring-boot-starter-web` | REST/SSE endpoints and Vaadin servlet stack | Spring MVC officially supports async request processing and SSE. Existing `SseEmitter` infrastructure is adequate. Do not switch the app to WebFlux for this milestone. |
| Vaadin Flow | **24.8.4** current; stay on 24.x | Console UI | Vaadin Flow can implement chat-style UI with Java components and server push/poll. No React/Next.js rewrite is justified. |
| Spring AI | **1.1.5** | Existing OpenAI-compatible provider adapter | Keep as the provider adapter layer. Do not expose Spring AI types into Domain/App conversation contracts. |
| SQLite JDBC | **3.47.1.0** current in adapter-web | Local developer persistence | Keep for local profile, but harden schema and access patterns for session list/history/provider config. |
| Playwright/JUnit | Existing milestone validation stack | Product-path verification | Extend existing no-key browser E2E to assert historical restore, streaming bubble deltas, and provider config persistence. |

### Add Product Seams, Not Heavy Libraries

| Addition | Layer / Module | Concrete API or Class Shape | Why |
|----------|----------------|-----------------------------|-----|
| `SessionQueryService` | `pi-agent-app` + `pi-agent-client` DTOs | `listRecentSessions(context, limit)`, `getConversationTranscript(context, sessionId, limit)`, optionally `getSessionConversationState(...)` | The Console needs a product-level recent-session list and transcript restore. Reusing run-scoped `listEvents(sessionId, runId, afterSequence)` forces Vaadin UI to reconstruct product state from low-level run events. |
| `ConversationMessageDto` | `pi-agent-client` | Fields: `messageId`, `sessionId`, `runId`, `role`, `text`, `createdAt`, `status`, `sourceEventId`, `sequenceRange`, `metadata` | Gives Console one stable transcript model for history restore and multi-turn context without leaking provider/event internals. |
| `ConversationTranscriptAssembler` | `pi-agent-app` | Reduce persisted `RunEvent` / `messages` rows into ordered user/assistant/tool-status transcript | Keeps UI simple and enables deterministic tests. Deltas should be folded into assistant messages per run. |
| `SessionConversationContextAssembler` | `pi-agent-app` or runtime-facing App service | `List<ModelMessage> assemble(sessionId, currentUserInput, budget)` | Sends multi-turn history to `ModelRequest` while respecting context budgets and without introducing external memory systems. |
| `ConversationEventReducer` | Adapter-web UI helper, possibly mirrored in App tests | `apply(RunEventDto)` -> UI mutations: append delta, finish bubble, mark error/cancel | Turns provider-neutral events into Kimi-style chat bubbles. Prevents every event from becoming a separate feed row. |
| `ConversationRunExecutionBridge` extension | `pi-agent-adapter-web` | Existing `ConsoleRunExecutionBridge` plus `listRecentSessions`, `loadTranscript`, maybe `activeRunForSession` | Keeps Vaadin concerns in adapter-web while delegating product queries to App use cases. |
| Local provider config validation response | `pi-agent-client` or adapter DTO | Include `ready`, `maskedKey`, `modelId`, `baseUrl`, `models`, `lastValidationError`, `validatedAt` | Current `ProviderConfigController.listModels()` returns only models/error and `ConsoleView` catches refresh errors silently. Product UI needs stable feedback and fallback behavior. |
| SQLite local indexes/schema evolution | Adapter-web local persistence | Index `local_runs(session_id, updated_at)`, `local_events(run_id, sequence)`, provider config table versioning | Local session restore and event replay will become common. Current `loadSessions()` has no ordering and `loadEvents()` loads all events globally. |

## Recommended Implementation Stack by Feature

### 1. Historical Session Selection / Restore

**Use:** App-layer query service + client DTOs + SQLite/JDBC-backed repositories + Vaadin card/list UI.

Recommended flow:

1. Add a `SessionQueryService` in `pi-agent-app` rather than making `ConsoleView` query low-level stores directly.
2. Add `ConversationTranscriptResponse` and `ConversationMessageDto` to `pi-agent-client`.
3. Implement persistence adapters for both cloud JDBC and local SQLite/dev stores:
   - Recent sessions ordered by `updated_at DESC`.
   - Session transcript assembled from persisted user input and `model.delta` events.
   - Limit and paging hooks, even if v1.2 only shows the last N sessions.
4. Extend `SessionListPanel` and `ConsoleRunExecutionBridge` so `selectSession()` loads transcript and replaces the chat feed.

**Why:** Product users expect “click old conversation → see prior messages → continue”. A history path alone is not enough. The transcript assembly belongs in App/usecase because future CLI/TUI clients will need the same behavior.

**Do not use:** Browser localStorage as the source of truth. It would hide persistence bugs and diverge from REST/SSE clients.

### 2. Real Streaming Assistant Bubble UI

**Use:** Existing `model.delta` events + Vaadin component mutation + optional `@Push`.

Current state:

- `DynamicAgentRuntime.ModelDeltaPublishingSink` publishes `RunEventPayload.ModelDeltaPayload` for text deltas.
- `DefaultRunQueryService` maps model deltas to `payload.textDelta` and `payload.text`.
- `ChatEventStreamPanel` appends assistant text into a single `activeAssistantLine` when category is `assistant`.
- `ConsoleView` currently polls every 750 ms and calls `refreshActiveRunEvents()`.

Recommended stack seam:

```java
public final class ConversationEventReducer {
    public ConversationUiDelta reduce(RunEventDto event) {
        // run.status -> composer/run state
        // model.delta with non-empty textDelta -> assistant token append
        // model.delta with finishReason -> mark assistant complete
        // provider/tool errors -> error bubble/status
    }
}
```

Then `ConsoleView.appendRunEvents(...)` should route events through the reducer instead of relying on generic `RunEventRenderer` for all event types.

**Push vs poll recommendation:**

| Option | Recommendation | Why |
|--------|----------------|-----|
| Vaadin `@Push` + `UI.access(...)` | Preferred after reducer exists | Vaadin docs support server push for updating UI from background threads; it gives real-time assistant bubble updates without waiting for the next poll tick. |
| Current `setPollInterval(750)` | Acceptable fallback / first increment | Simple and already present. However, “real streaming” will feel chunky under poor latency and wastes polling when idle. |
| Browser `EventSource` directly mutating DOM | Avoid for Vaadin component state | Existing `EventStreamClient` can produce an EventSource expression, but direct DOM mutation bypasses server-side Vaadin state and increases split-brain risk unless encapsulated as a custom component. |
| WebSocket/STOMP | Do not add | Overkill. The app already has SSE and Vaadin push/poll; adding STOMP creates another realtime protocol to govern and test. |

Version-sensitive concern: Vaadin push requires enabling push at the UI/app shell level and respecting the Vaadin session lock. Background callbacks must use `UI.access(...)`; direct component mutation from runtime/dispatcher threads is unsafe. Validate deployment/proxy behavior because push uses Atmosphere/WebSocket/long-poll transports under Vaadin.

### 3. Multi-Turn Session Context

**Use:** App-layer context assembler + existing provider-neutral model request.

Current gap: `DynamicAgentRuntime` sends `new ModelRequest(context, List.of())`, which means the model adapter receives no prior conversation messages.

Recommended implementation:

1. Define a provider-neutral conversation message abstraction in Domain/App if one does not already exist for model requests.
2. Add `SessionConversationContextAssembler` that reads current session transcript and returns bounded previous messages.
3. Pass assembled messages into `ModelRequest` from runtime execution.
4. Make context policy explicit:
   - Include previous user/assistant turns, not raw operational events by default.
   - Exclude redacted/sensitive tool payloads unless a future policy explicitly allows summaries.
   - Apply max messages / token approximation / character budget before provider call.
   - Include the new user message once, avoiding duplication if it is already persisted as current run input.

**Why:** This preserves the platform’s provider-neutral runtime design. It also keeps chat as a first product entry without binding Domain to a specific UI.

**Do not use:** Spring AI memory or vector/RAG memory for v1.2 multi-turn chat. Those are useful later for long-term semantic memory, but v1.2 needs deterministic session-local conversational history.

### 4. Local Provider / Model Configuration Stability

**Use:** Existing `ProviderConfigStore` + `SqliteLocalPersistence` + REST validation endpoint + Vaadin feedback.

Current state:

- `ProviderConfigStore` keeps an `AtomicReference` and writes SQLite provider config.
- `ProviderConfigController` masks the API key and lists `/models` through `RestClient`.
- `ConsoleView.createModelBar()` updates `modelId` directly and silently ignores refresh exceptions.
- `DynamicAgentRuntime` caches the model client by `ProviderConfigStore.version()`.

Recommended changes:

| Change | Why |
|--------|-----|
| Add explicit `ProviderValidationResponse` with `ready`, `error`, `models`, `selectedModel`, `validatedAt` | Console needs clear “configured / not configured / invalid key / endpoint unavailable” feedback. |
| Persist `last_validation_error` and `last_validated_at` locally if useful | Makes local profile stable after page reloads and helps troubleshooting. |
| Normalize base URL and completions path before saving | Prevents subtle invalid endpoint combinations. Current `listModels()` computes `modelsUrl` but does not use it; keep URL handling simple and tested. |
| Replace `version++` with `AtomicLong` or synchronized update if concurrent config edits become possible | Current volatile long is probably fine for local dev but not ideal under concurrent requests. |
| Keep API key masked in all responses | Current masking is good; keep it. Add tests that full key never appears in UI/API. |
| Add local DB indexes and targeted load methods | Current `loadEvents()` loads all events globally. Streaming restore should query by `run_id` and sequence. |

**Do not add:** Vault/secret-manager integration in this milestone. The goal is local configuration stability; cloud secret governance is a separate platform/security milestone.

## Integration Points with Existing Code

| Existing Code | Current Role | Recommended v1.2 Integration |
|---------------|--------------|-------------------------------|
| `ConsoleView` | Orchestrates current Vaadin Console, selected session/run, polling, model selector | Keep as adapter UI coordinator, but move transcript/session querying into bridge/usecases. Add transcript restore on `selectSession()`. Replace generic model delta rendering with conversation reducer. |
| `ChatEventStreamPanel` | Chat feed + composer, already appends assistant deltas into one active line | Evolve into a message-bubble component API: `replaceTranscript(...)`, `appendUserMessage(...)`, `beginAssistantMessage(runId)`, `appendAssistantDelta(...)`, `completeAssistantMessage(...)`, `showErrorBubble(...)`. Keep UI-only state here. |
| `AppConsoleRunExecutionBridge` | Delegates session/run/event/cancel to App usecases | Extend or replace with `ConversationConsoleBridge` that also loads recent sessions and transcripts. Do not inject repositories directly into Vaadin views. |
| `DynamicAgentRuntime` | Local profile runtime and OpenAI-compatible streaming client resolver | Inject/use a conversation context assembler or pass assembled messages through `RunContext`/`ModelRequest`. Stop passing `List.of()` once multi-turn is implemented. |
| `DefaultRunQueryService` | Run-scoped detail/status/events/messages/tool-calls | Keep run query API, but add separate session/conversation query usecase. Do not overload run query service with product transcript concerns unless renamed/segmented cleanly. |
| `SqliteLocalPersistence` | Custom local sessions/runs/events/provider_config tables | Add targeted methods and indexes: `loadSessionsOrderByUpdatedAt(limit)`, `loadRunsBySession(sessionId)`, `loadEventsByRun(runId, afterSequence, limit)`. Avoid global full-table load for conversation restore. |
| `RunEventRenderer` | Generic runtime event cards | Keep for tool/status/debug surfaces. Do not use it as the primary assistant bubble renderer for `model.delta`. |
| `EventStreamClient` / `RunEventStreamService` | Existing public SSE contract | Preserve for REST/SSE clients and tests. Vaadin UI may use App-layer polling/push internally, but public SSE should remain the external realtime contract. |

## Architecture / Boundary Guidance

### COLA Placement

| Concern | Correct Layer | Notes |
|---------|---------------|-------|
| Recent-session query | App usecase + persistence port | UI asks for recent sessions through a usecase. Infrastructure/adapter implements local/JDBC storage. |
| Transcript DTOs | Client module | Stable API for Vaadin, REST, future CLI/TUI. |
| Transcript reduction policy | App layer | Product semantics: which events become messages. Keep provider-neutral. |
| Model context assembly | App/Domain boundary | Should not depend on Vaadin, Spring MVC, SQLite, or OpenAI SDK classes. |
| Bubble rendering | Adapter-web | Vaadin components and CSS only. |
| Provider config UI | Adapter-web | UI and local profile configuration can remain adapter-web, but secrets must stay masked. |

### Suggested Minimal API Shape

```java
public interface SessionQueryService {
    PageResponse<SessionSummaryDto> listRecentSessions(RequestContext context, int limit);
    ConversationTranscriptResponse getTranscript(RequestContext context, String sessionId, int limit);
}

public record ConversationMessageDto(
        String messageId,
        String sessionId,
        String runId,
        String role,          // user | assistant | tool | system-status
        String text,
        String status,        // complete | streaming | failed | cancelled
        Instant createdAt,
        Map<String, Object> metadata) {
}

public record ConversationTranscriptResponse(
        String sessionId,
        List<ConversationMessageDto> messages,
        String activeRunId,
        boolean hasMore) {
}
```

Keep these DTOs provider-neutral and UI-neutral. Vaadin can render them, REST clients can consume them, and future TUI/CLI can reuse them.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Deltas become separate bubbles/cards | Chat feels broken and not Kimi-like | Add `ConversationEventReducer` tests: multiple `model.delta` events for one run produce one assistant message. |
| Session restore reconstructs incomplete/incorrect transcript | Users cannot trust history | Build transcript from persisted events/run inputs with deterministic ordering; add golden tests for user → assistant delta → finish, error, cancel. |
| Multi-turn sends duplicate current prompt | Model repeats or behaves oddly | Context assembler test should verify current input appears exactly once. |
| Raw tool/audit payload enters model context | Security/privacy regression | Default context assembler should include only user/assistant conversational text and safe tool summaries when explicitly allowed. |
| Vaadin push mutates UI from wrong thread | Runtime errors/session lock issues | Use `UI.access(...)`; detach listeners should unsubscribe/cancel callbacks. Keep poll fallback until push path is proven. |
| SQLite local queries become slow or stale | Local Console feels unreliable | Add indexes and query by session/run; avoid `loadEvents()` global scan. Use SQLite UPSERT (`ON CONFLICT`) where update semantics matter. |
| Provider config errors are swallowed | Users see no response and cannot fix setup | Surface validation errors in model bar/composer; keep no-config fallback response but make “not configured” actionable. |
| Config changes during active run | Mixed model/provider state | Snapshot provider/model ref at run creation and display it on assistant bubble/run metadata. Do not let mid-run selector changes mutate an active run. |

## What NOT to Add

| Do Not Add | Why Not | Use Instead |
|------------|---------|-------------|
| React/Next.js/Hilla React frontend | Violates all-Java/Vaadin-first milestone direction, duplicates client state, adds new build/runtime surface | Vaadin Flow components + CSS + Playwright Java tests. |
| Mobile-only API fork | Creates DTO divergence and future CLI/TUI confusion | Product-level conversation DTOs usable by all clients. |
| WebFlux rewrite | Existing app is Spring MVC/Vaadin servlet stack with working SSE; rewrite does not solve transcript/context gaps | Keep Spring MVC `SseEmitter` and Vaadin push/poll. |
| WebSocket/STOMP stack | Adds another realtime protocol to secure, test, and operate | Existing SSE for public API; Vaadin push/poll for server-side UI updates. |
| Kafka/Redis event bus | Not needed for single-node/local v1.2 productization | Existing persistence + in-process event fanout/poll. Revisit for horizontal scale. |
| Vector DB / long-term memory | Multi-turn session context does not require semantic memory | Session transcript + bounded context assembler. |
| LangChain4j/LangGraph for chat history | Duplicates project-owned runtime abstractions and adds concept churn | Project-owned conversation context assembler. |
| JPA/Hibernate for transcript | Existing persistence is JDBC/custom SQLite; ORM migration is unrelated risk | Explicit JDBC/SQLite queries and DTO assemblers. |
| Native mobile app/PWA offline cache | Out of scope; v1.2 is Console productization in existing H5/Vaadin app | Responsive Vaadin Console with stable local persistence. |
| Full secret manager/Vault | Important later, but not required for local provider config stabilization | Masked local config + validation + tests. |

## Version-Sensitive Concerns

| Component | Current | Recommendation | Notes |
|-----------|---------|----------------|-------|
| Vaadin | **24.8.4** | Stay on 24.x | Vaadin 24 docs support server push and UI updates. Do not move to Vaadin 25 because it implies broader platform upgrade risk. |
| Spring Boot | **3.5.9** | Stay on 3.5.x | Boot 3.5 aligns with current Vaadin/Spring AI stack. |
| Spring AI | **1.1.5** | Stay on 1.1.x | Multi-turn context should be project-owned; do not wait for or depend on Spring AI 2.x memory changes. |
| SQLite JDBC | **3.47.1.0** | Keep unless security patch needed | SQLite supports UPSERT via `ON CONFLICT`; use explicit indexes and targeted queries. |
| Vaadin Push | BOM-managed | Enable only after tests cover it | Requires `@Push`/app shell config and correct `UI.access` usage. Proxies and test server behavior must be validated. |

## Verification Recommendations

Add no-key automated gates in `pi-agent-adapter-web` and App tests:

1. **Transcript reducer unit tests**: user input + model deltas + finish produce exactly two visible messages.
2. **Session restore App test**: create two sessions, multiple runs, select older session, transcript order is stable.
3. **Multi-turn context test**: second run includes prior user/assistant turn and current prompt once.
4. **Provider config persistence test**: update provider/model, reload store from SQLite, masked API response never exposes full key.
5. **Vaadin UI test**: send prompt, observe assistant bubble text append incrementally, reload/select session, messages restore.
6. **Error/cancel UI test**: provider error and cancelled run update existing assistant/status bubble rather than leaving indefinite “running”.
7. **Mobile/desktop regression**: keep existing H5 viewport matrix; add assertions for history drawer/list and chat composer still usable.

## Sources

- Project files read 2026-06-28: `.planning/PROJECT.md`, `.planning/ROADMAP.md`, `ConsoleView.java`, `ChatEventStreamPanel.java`, `AppConsoleRunExecutionBridge.java`, `DynamicAgentRuntime.java`, `DefaultRunQueryService.java`, root and adapter-web `pom.xml`.
- Vaadin 24 official docs: Server Push / pushing UI updates and advanced server push configuration. Confidence: **HIGH** for Vaadin push/`UI.access` direction; exact app configuration should be validated in implementation.
- Spring Framework official docs: Spring MVC asynchronous requests and SSE support via Servlet async processing. Confidence: **HIGH** for keeping Spring MVC/SSE.
- SQLite official docs: UPSERT `ON CONFLICT` behavior and limitations. Confidence: **HIGH** for SQLite local persistence hardening guidance.
- Existing code evidence: `DynamicAgentRuntime` publishes `MODEL_DELTA`; `DefaultRunQueryService` maps `textDelta`; `ChatEventStreamPanel` supports assistant-line append; `ConsoleView` currently polls and lacks transcript restore; `SqliteLocalPersistence` currently has basic local tables but no targeted session/run/event query methods.
