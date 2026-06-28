# Feature Research: Kimi-Style Console Conversation Productization

**Domain:** Agent Console conversation productization for an existing Java/Vaadin Agent Platform  
**Project:** Pi Java Agent Platform  
**Researched:** 2026-06-28  
**Confidence:** HIGH for codebase dependency findings and core streaming/context patterns; MEDIUM-HIGH for Kimi-style product expectations verified against Kimi/OpenAI public docs; MEDIUM for competitive UX differentiators because current public product UI details are partly inferred from common AI chat products.

## Summary

This milestone should turn the Console from a run workbench that can display a reply into a **daily-use, Kimi-homepage-like conversation surface**: a centered chat-first home, visible session continuity, assistant text streaming into one bubble, multi-turn context on the selected session, and clear local provider/model controls. The goal is not a generic chat-app wishlist; it is to productize the already-built Agent Console, run/session APIs, event history, local provider profile, SQLite persistence, and recent visible chat reply path.

The current `ConsoleView` already contains useful foundations: Kimi-like hero (`pi-console-home`), a hidden advanced panel area, session card selection, a model bar, run creation, polling-based event refresh, cancellation, and `RunEventRenderer` mapping `model.delta` to assistant text. `ChatEventStreamPanel` already appends consecutive assistant deltas into the same UI line through `activeAssistantLine`. The biggest gaps are: **historical session restore does not render previous messages**, **session list is only populated from current UI interactions**, **multi-turn requests do not include session history/context**, **streaming is effectively poll-driven and status-centric rather than a first-class assistant bubble lifecycle**, and **provider/model error/fallback states are too implicit for a consumer-grade Console**.

Recommended scope: build a narrow conversation product layer over existing run/session/event primitives. Add just enough App/API support to list recent sessions, restore session entries as chat messages, construct session-level context for the next run, and surface provider/model/fallback errors. Avoid new Agent Studio, prompt libraries, memory/RAG, branch editing, cross-device sync, or multi-provider orchestration in this milestone.

## Current Codebase Dependencies

| Area | Existing Implementation | Gap for This Milestone | Complexity |
|------|-------------------------|------------------------|------------|
| Kimi-style home | `ConsoleView` has hero, centered chat card, hidden advanced panels, model bar. | Needs explicit history entry point and simplified first-run/continued-session states; avoid exposing run-context/agent panels by default. | LOW-MEDIUM |
| Session selection | `SessionListPanel` supports cards, active state, click/keyboard activation; `ConsoleView.selectSession()` returns a history path. | No recent-session query/list on first load; selecting a session does not fetch/restore chat messages into `ChatEventStreamPanel`. | MEDIUM |
| Session history API | `/api/sessions/{sessionId}/history` exists; `SessionHistoryResponse` is `session + List<Map<String,Object>> entries`; JDBC reads `session_entries`. | Entry schema is generic and UI has no renderer. Local dev runtime returns `List.of()`. Need stable message-entry convention or projection for user/assistant messages. | HIGH |
| Streaming assistant bubble | `RunEventRenderer` maps `model.delta` to category `assistant`; `ChatEventStreamPanel` appends assistant deltas to `activeAssistantLine`. | Needs lifecycle: create empty/pending assistant bubble immediately, append only current run deltas, finalize on `[done]/run.completed`, mark incomplete on error/cancel. Polling every 750ms is acceptable for first pass but should feel real-time. | MEDIUM-HIGH |
| Run/event replay | `ConsoleView.appendRunEvents()` dedupes by `sequence`, applies status, renders events. | Restore should replay historical completed conversations without duplicating raw runtime cards as chat messages. Active streaming should distinguish assistant content vs tool/runtime details. | MEDIUM |
| Multi-turn context | `CreateRunRequest` input only includes current `Map.of("text", message)`; `DynamicAgentRuntime` streams with `new ModelRequest(context, List.of())`. | Need session conversation history retrieval and context assembly before model call. Must remain provider-neutral and bounded by token/window limits. | HIGH |
| Model/provider controls | `createModelBar()` reads `ProviderConfigStore`, allows custom model value, refreshes model list through `ProviderConfigController.listModels()`, persists selected model. | Needs visible provider identity, saved-state feedback, invalid/unconfigured state, per-run model snapshot, and errors from refresh/list/send. | MEDIUM |
| Error/fallback | `DynamicAgentRuntime` emits a friendly fallback response when no provider is configured. Run renderer handles failed/cancelled terminal events. | Need explicit UI treatment: fallback assistant bubble vs provider error card, retry/configure action, stale/partial stream indicator, and no silent ignored exceptions. | MEDIUM-HIGH |

## Feature Categories

### Table Stakes

Features users will expect from a credible Kimi-style Agent Console. Missing these makes the Console still feel like an internal run debugger.

| Feature | Why Expected | Complexity | Dependencies / Notes |
|---------|--------------|------------|----------------------|
| **Chat-first default home** | Kimi-style entry points focus on one primary input and conversation, not operational panels. Users should immediately know where to type. | LOW-MEDIUM | Keep current hero + centered `ChatEventStreamPanel`; add visible history affordance and model/provider compact control. Advanced run/tool panels should be collapsed or secondary unless relevant. |
| **Recent sessions list on load** | Users expect previous conversations to be discoverable without creating a new run first. | MEDIUM | Requires App/API support beyond `getSession(id)`: list recent sessions for tenant/user, ordered by `updatedAt`, with title/status/last message metadata. Do not rely only on in-memory `SessionListPanel.showSession()` calls. |
| **Historical session selection** | Selecting a prior session is the core continuity action. | MEDIUM | `SessionListPanel` can already activate cards. Need wire-up to fetch `SessionHistoryResponse`, clear current feed, render restored messages, set `selectedSessionId`, and show active-session title. |
| **Message restore as chat bubbles** | Users expect the previous user/assistant turns to appear exactly enough to continue. A raw event log is not a conversation. | HIGH | Define stable projection from `session_entries` / run messages / model deltas into `{role, text, runId, sequence, status, createdAt}`. Avoid dumping raw `Map` payloads. Local dev SQLite/fake runtime must persist enough entries for restore. |
| **Continue selected session, not accidental new session** | After selecting history, the next send must append to that session. Losing context is the most visible product failure. | MEDIUM | `ConsoleView.planChatSubmission()` already reuses `selectedSessionId`; ensure session selection restore sets it and session card remains active. Add tests that second prompt uses same session ID. |
| **Streaming assistant bubble** | Kimi's official docs describe token-by-token streaming as improving UX by reducing wait time; user-facing assistants reveal text progressively. | MEDIUM-HIGH | Existing assistant delta concatenation is a good base. Add pending assistant bubble immediately after send, append `model.delta` to the same bubble, finalize on terminal completion, and mark partial/incomplete on cancel/error. Source: Kimi streaming docs. |
| **No duplicate or fragmented assistant messages** | Streaming chunks should not produce a new card per token/chunk. | MEDIUM | Preserve `activeAssistantLine`, but reset it on new user message, tool/runtime card insertion, selected-session restore, and run terminal. Deduplicate by event sequence as current code does. |
| **Multi-turn context from selected session** | Stateless model requests must include alternating user/assistant history to continue coherently. OpenAI docs explicitly describe multi-turn by passing prior messages or persistent conversation state. | HIGH | Add App/runtime context assembler: collect bounded prior messages for the selected session, append current user input, pass as provider-neutral `ModelRequest` messages. Do not build this in Vaadin. |
| **Context window guardrails** | Conversation history grows; blindly sending everything causes failures/cost spikes. | MEDIUM-HIGH | Start with simple bounded recent-turn strategy and configurable max turns/chars/tokens. Add visible warning only when context is truncated. Defer advanced compaction/summarization. |
| **Model selector with persisted local choice** | Existing milestone includes model/provider config and local SQLite profile. Users expect selected model to stay selected and affect the next send. | MEDIUM | Existing `ProviderConfigStore.update()` is foundation. Add saved feedback, current provider/model label in composer, and ensure run metadata captures model ref for audit/debug. |
| **Unconfigured provider fallback as a helpful assistant response** | Local dev should work even with no key, but users must understand it is fallback, not a real model answer. | LOW-MEDIUM | Existing `DynamicAgentRuntime` emits friendly Chinese fallback. UI should mark it as `fallback/local` and provide “Configure provider” action/link, not just plain assistant text. |
| **Provider/model error handling** | Bad base URL, missing key, unsupported model, timeout, and rate limit are normal setup failures. | MEDIUM-HIGH | Replace silent catches in model refresh with visible inline errors. On send failure, show a failed assistant bubble/card, keep user message, allow retry after config fix. |
| **Run cancellation feedback in conversation** | User may stop a long answer; the current bubble should show stopped/partial state. | MEDIUM | Existing cancel API/UI is available. Add assistant bubble marker: “Stopped” / “Partial response”; prevent further deltas from appending after terminal cancellation. |
| **Automated product-path verification** | This milestone changes UX correctness, not just layout. | MEDIUM-HIGH | Add Vaadin unit/contract tests plus Playwright no-key path: load recent sessions, select/restore, continue same session, see streaming bubble append, configure/fallback/error states. |

### Differentiators

Features that make the Console feel like an Agent platform rather than a generic chatbot. These are valuable but should come after table stakes inside the milestone only if dependencies are already solved.

| Feature | Value Proposition | Complexity | Dependencies / Notes |
|---------|-------------------|------------|----------------------|
| **Conversation + agent run hybrid timeline** | Users get a clean chat while still seeing agent-specific tool/runtime cards inline when relevant. | MEDIUM | Use chat bubbles for user/assistant; collapsible cards for tool/approval/error. Do not show status noise like every `run.status` as chat text. |
| **Session title auto-derived from first prompt** | Makes history usable without manual naming. | LOW | Current `sessionTitle(message)` already truncates first prompt. Persist title/metadata so sessions survive reload and SQLite restarts. |
| **Active session continuity banner** | Clear “Continuing: <title> · model <x>” reduces accidental context confusion. | LOW-MEDIUM | Depends on restored session metadata and selected model state. Especially useful because advanced panels are hidden. |
| **Per-run model/provider snapshot in history** | Helps users explain why answers differ across turns and supports debugging provider config changes. | MEDIUM | Store metadata at run creation: providerId, modelId, fallback/real provider mode. Display as small chip, not a dominant control. |
| **Retry last failed prompt after config fix** | Smooth local developer workflow when API key/model is wrong. | MEDIUM | Keep failed user message and input; add retry action that reuses same session and current model config. Guard against duplicate run creation confusion. |
| **Inline context-truncation disclosure** | Builds trust when old history is excluded. | MEDIUM | Requires context assembler to report included/skipped counts. Display “Using last N turns” quietly. |
| **Keyboard-first send ergonomics** | Chat users expect Enter to send and Shift+Enter for newline, while mobile keeps button submit. | LOW-MEDIUM | Vaadin `TextArea` needs client-side shortcut handling carefully. Add only if tests cover multiline preservation. |
| **Recover interrupted stream from event replay** | If browser refreshes mid-run, restored active session can replay persisted deltas and continue polling/SSE. | HIGH | Depends on active run discovery for session and terminal/incomplete state. Good differentiator, but not required before basic restore. |

### Anti-Features

Features to explicitly avoid for this milestone because they add scope, violate constraints, or obscure the core conversation productization.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Full ChatGPT/Kimi clone** | The project is an Agent Platform Console, not a consumer chat clone. Cloning every UX detail adds scope without improving runtime/product correctness. | Implement the core Kimi-like IA: centered input, history, streaming answer, multi-turn context, compact model controls. |
| **Agent Studio / visual workflow builder** | Already out of v1 scope and distracts from conversation continuity. | Keep agent selection simple; default to General Agent. |
| **Prompt library, templates, personas, marketplace** | Nice consumer features but not required for session restore/streaming/context. | Defer until basic conversation loops are reliable. |
| **Cross-device cloud sync beyond existing authenticated storage** | Requires product/privacy/account decisions. | Use existing tenant/user persistence. Local profile remains local dev convenience. |
| **Branching conversations / edit-and-regenerate tree** | Complicates session entry model and context assembly. | Linear session history only. Add regenerate later if demand appears. |
| **Advanced memory/RAG/long-term personalization** | Different domain from multi-turn session context; can create privacy and correctness issues. | Use bounded current-session history only. |
| **Provider auto-fallback to another paid model** | Dangerous for cost, data routing, compliance, and debugging. | Fallback only to explicit local/no-key explanatory response, or require user/admin-configured fallback policy later. |
| **Silent best-effort errors** | Current refresh model action catches and ignores exceptions; this destroys trust. | Show inline status/error and recovery action. |
| **Raw run/event log as restored conversation** | Users do not want to read `run.status`, JSON payloads, or every event when restoring a chat. | Project events into clean messages plus optional collapsible runtime cards. |
| **Send entire unbounded history every turn** | Costly, slow, may exceed model context window, and can leak stale/tool details into prompt. | Bounded recent turns with future summarization/compaction. |
| **Multiple simultaneous active runs in one chat** | Complex bubble ownership and cancellation semantics; likely confusing in a Kimi-style chat. | One active run per visible session for this milestone; disable send or require stop before new send. |
| **Reactive/WebFlux rewrite just for streaming UI** | The project currently uses Spring MVC/Vaadin and has polling/SSE boundaries. A rewrite would be disproportionate. | Improve current event stream/polling path; use true SSE bridge only if necessary and contained. |

## Recommended Scope for This Milestone

### P1 — Must Build

1. **Recent session list + active session selection**
   - Add backend/App query for recent sessions for current tenant/user.
   - Populate `SessionListPanel` on Console load.
   - Selecting a session sets `selectedSessionId`, shows active visual state, and returns to chat.

2. **Conversation restore projection**
   - Define stable restored message DTO or entry convention: role, text, runId, status, createdAt, sequence.
   - Render restored user/assistant messages into `ChatEventStreamPanel` after clearing old feed.
   - Do not render raw `session_entries` maps directly.

3. **Streaming assistant bubble lifecycle**
   - On send: append user bubble, create pending assistant bubble, disable/mark composer running.
   - On `model.delta`: append to the same assistant bubble.
   - On completion: finalize bubble and hide cancel.
   - On failed/cancelled/timeout: mark bubble partial/failed and show recovery/status.

4. **Session-level multi-turn context**
   - Runtime/App layer assembles bounded prior user/assistant messages from selected session.
   - Pass alternating history + current user message to provider-neutral model request.
   - Add tests proving second turn can see first-turn content with fake model.

5. **Provider/model controls and error states**
   - Show current provider/model readiness near composer.
   - Persist custom/selected model in local profile.
   - Surface refresh/list/send errors; no silent ignored exceptions.
   - Mark no-provider fallback responses clearly.

6. **No-key and configured-provider verification gates**
   - No-key Playwright path: fallback response visible, history restore works, continuation uses same session.
   - Fake streaming path: multiple deltas append to one assistant bubble.
   - Error path: provider/model failure renders actionable UI.

### P2 — Should Build If P1 Is Stable

1. **Session titles persisted and updated from first prompt / latest activity**.
2. **Compact active-session banner** with title, session ID short form, and model chip.
3. **Retry last failed prompt** after provider config changes.
4. **Context inclusion disclosure** such as “Using last 8 turns”.
5. **Inline collapsible tool/runtime cards** inside the clean conversation flow.

### Defer Beyond This Milestone

- Conversation branching/edit/regenerate.
- Global search across all sessions.
- Prompt/template library.
- Long-term memory/RAG.
- PWA/offline/mobile push notifications.
- Multi-provider automatic fallback policies.
- Full chat export/import.

## Feature Dependencies

```text
Recent session query
  -> SessionListPanel initial population
  -> Historical session selection
  -> Message restore
  -> Continue selected session

Stable conversation message projection
  -> Chat bubble restore
  -> Multi-turn context assembler
  -> Fake model context tests

Run event stream / event history
  -> Streaming assistant bubble lifecycle
  -> Error/cancel/terminal bubble state
  -> Refresh/replay recovery later

ProviderConfigStore + ProviderConfigController
  -> Model selector persistence
  -> Provider readiness status
  -> Refresh/send error handling

Local dev SQLite persistence
  -> Sessions survive reload
  -> Restored messages survive process/page lifecycle
  -> No-key product-path E2E
```

## Acceptance Behaviors / Candidate Requirements

| ID | Acceptance Behavior | Complexity | Depends On |
|----|---------------------|------------|------------|
| **CONV-001** | On `/console`, user sees a chat-first Kimi-style home with one primary composer, compact model/provider status, and a visible history entry point; operational panels are not primary noise. | LOW-MEDIUM | Existing `ConsoleView` hero/chat card. |
| **CONV-002** | User can see recent sessions after page load, ordered by latest activity, with title/status/updated time. | MEDIUM | Recent session query API/App/repository. |
| **CONV-003** | User can select a historical session and the chat feed restores prior user and assistant messages as bubbles. | HIGH | Session history projection and UI renderer. |
| **CONV-004** | After restoring a historical session, sending a new message creates a run in the same session ID. | MEDIUM | `selectedSessionId` wiring and tests. |
| **CONV-005** | Assistant response streams into one visible assistant bubble instead of separate chunk lines. | MEDIUM | Existing `model.delta` renderer and `activeAssistantLine`. |
| **CONV-006** | Completion, cancellation, failure, and timeout produce clear terminal bubble/composer states. | MEDIUM-HIGH | Run terminal events/status mapping. |
| **CONV-007** | Second-turn prompts include bounded prior session messages so a fake model can answer using previous user/assistant context. | HIGH | Runtime/App context assembler. |
| **CONV-008** | If context is truncated, UI or metadata indicates bounded context was used without blocking the send. | MEDIUM | Context assembler reports included/skipped turns. |
| **CONV-009** | User can choose/enter a model, refresh available models, and see saved provider/model readiness without leaving the Console. | MEDIUM | Existing provider config store/controller. |
| **CONV-010** | Model refresh and send errors are visible and actionable; no exceptions are silently ignored. | MEDIUM | Provider UI status and error handling. |
| **CONV-011** | With no provider configured, the Console returns a clearly labeled local fallback assistant response and points to provider configuration. | LOW-MEDIUM | Existing `DynamicAgentRuntime` fallback. |
| **CONV-012** | Automated tests cover no-key fallback, history selection/restore, same-session continuation, streaming delta coalescing, and provider error UI. | MEDIUM-HIGH | Vaadin tests + Playwright/fake runtime. |

## Implementation Notes Scoped to Existing Code

- **Do not put context assembly in `ConsoleView`.** Vaadin should send the new user message and selected session ID; App/runtime should decide which historical messages belong in the model request.
- **Do not render `SessionHistoryResponse.entries` directly.** Its `List<Map<String,Object>>` shape is too generic for product UI. Add a typed client DTO or a well-documented projection method.
- **Use existing `RunEventRenderer` categorization but reduce status noise.** User-facing feed should prioritize assistant text and important cards; raw statuses can update composer/run state.
- **Improve `ChatEventStreamPanel` with explicit methods** such as `clearConversation()`, `appendRestoredMessage(role,text,metadata)`, `startAssistantMessage()`, `appendAssistantDelta()`, `markAssistantTerminal(status)`, rather than overloading generic `append()` for every lifecycle state.
- **Keep one active run per selected session in the UI.** Disable send during active run or require cancel/finish first.
- **Persist enough local dev data.** Current local dev history returns empty entries; this milestone needs SQLite/local profile path to restore visible chat messages after reload.

## Sources

- Project context: `.planning/PROJECT.md` current v1.2 Console 对话产品化 goal and active requirements. Confidence: HIGH.
- Existing roadmap/context: `.planning/ROADMAP.md` confirms v1.1 mobile Console/session/SSE foundations already completed and this research should focus only on new conversation productization. Confidence: HIGH.
- Codebase inspection: `ConsoleView.java`, `ChatEventStreamPanel.java`, `SessionListPanel.java`, `RunEventRenderer.java`, `SessionController.java`, `RunController.java`, `JdbcSessionRepository.java`, `SessionHistoryResponse.java`, `DynamicAgentRuntime.java` grep findings. Confidence: HIGH.
- Kimi official streaming docs: token-by-token streaming over SSE improves perceived latency; chunks contain deltas and `[DONE]` marks completion; interrupted streams should be treated as incomplete. URL: https://platform.kimi.ai/docs/guide/utilize-the-streaming-output-feature-of-kimi-api Confidence: HIGH for streaming behavior.
- OpenAI official conversation-state docs: model requests are stateless unless prior alternating user/assistant messages or conversation state are provided; context windows require bounded management/compaction for long conversations. URL: https://platform.openai.com/docs/guides/conversation-state Confidence: HIGH for multi-turn context principles.
- Web search for ChatGPT/history patterns confirmed sidebar/search/history expectations but was mostly secondary/community/help content; used only as LOW confidence background, not as primary basis for requirements.

---
*Feature research for: Kimi-style Console conversation productization milestone*  
*Researched: 2026-06-28*
