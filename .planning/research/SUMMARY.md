# Project Research Summary

**Project:** Pi Java Agent Platform  
**Milestone:** v1.2 Console 对话产品化  
**Domain:** Kimi-style conversation productization for an existing Java/Spring MVC/Vaadin Agent Console  
**Researched:** 2026-06-28  
**Confidence:** HIGH overall for codebase-backed direction; MEDIUM-HIGH for Vaadin push/SSE implementation details that need validation in tests.

## Executive Summary

v1.2 should turn the existing Console from an internal run/event workbench into a daily-use, Kimi-homepage-like conversation surface: chat-first entry, visible recent sessions, historical session restore, assistant text streaming into one bubble, selected-session multi-turn context, and clear local provider/model readiness. The current platform already has strong foundations—Java 21, Spring Boot 3.5.x, Vaadin 24.x, Spring MVC REST/SSE, run/session/event persistence, local SQLite profile, and OpenAI-compatible streaming—so this milestone should add product seams and read models, not a new frontend/runtime stack.

The core recommendation is to build a canonical App/client **conversation product layer** over existing session/run/event primitives. Add typed recent-session and transcript DTOs, a transcript assembler, a live event reducer for chat bubbles, and a bounded context assembler before runtime execution. Vaadin should coordinate rendering only; it must not become the source of durable history or model context. Public run-event/SSE contracts should remain, but Console UX should reduce those events into clean user/assistant/tool/error states.

The main risks are shipping visual continuity without real transcript restore, calling polling “real streaming,” and reusing `selectedSessionId` without actually sending prior turns to the model. Mitigate these by sequencing read-model work first, gating streaming with fake slow-delta tests, enforcing ownership-aware repository queries, and proving multi-turn context with a fake model that can inspect previous user/assistant turns.

## Key Findings

### Stack Additions and Direction

No major stack expansion is recommended. Stay on the current Java-first stack and add small App/API/UI seams.

**Keep current stack:**
- **Java 21 + Maven** — stable enterprise runtime/build baseline; no version change needed for v1.2.
- **Spring Boot 3.5.x / Spring MVC** — keep servlet/Vaadin stack and existing REST/SSE; do not switch to WebFlux.
- **Vaadin Flow 24.x** — continue all-Java Console UI; use Vaadin Push + `UI.access(...)` if validated, with polling as fallback.
- **Spring AI 1.1.x / OpenAI-compatible adapter** — keep as provider adapter; do not leak Spring AI messages into App/Domain conversation contracts.
- **SQLite local profile + JDBC repositories** — harden targeted session/run/event queries and indexes; avoid browser localStorage or ad-hoc local files as truth.
- **Playwright/JUnit/fake model tests** — extend existing validation to assert session restore, streaming delta coalescing, context assembly, and provider error states.

**Add product seams, not heavy libraries:**
- `ConversationQueryService` / `SessionQueryService` extensions for recent sessions and typed transcripts.
- `SessionSummaryDto`, `ConversationMessageDto`, `ConversationTranscriptResponse` in `pi-agent-client`.
- `ConversationTranscriptAssembler` in App to fold persisted run inputs/events into ordered messages.
- `ConversationEventReducer` / `ChatStreamAggregator` for live UI bubble mutation.
- `SessionConversationContextAssembler` plus `ConversationContextPolicy` for bounded multi-turn model input.
- Provider readiness/validation response with masked key, selected model, model list, readiness, and actionable errors.

**Do not add in v1.2:** React/Next.js, mobile-only API forks, WebFlux rewrite, WebSocket/STOMP, Kafka/Redis event bus, vector DB/long-term memory, LangChain/LangGraph memory, JPA transcript rewrite, native mobile/PWA/offline stack, or Vault/secret-manager integration.

### Table Stakes and Feature Scope

**Must build:**
- Chat-first default Console home with compact history and model/provider affordances.
- Recent sessions on page load, ordered by durable latest activity.
- Historical session selection that clears/hydrates chat bubbles from persisted transcript.
- Restored message projection into typed user/assistant/tool/error states, not raw event maps.
- Continue selected session on next send; no accidental new session after restore.
- Streaming assistant bubble lifecycle: pending → delta append → complete/failed/cancelled/partial.
- Multi-turn context from selected session using bounded prior user/assistant turns.
- Context guardrails with max turns/chars/token approximation and truncation metadata.
- Persisted model selector and explicit provider/model readiness/error feedback.
- Clearly labeled no-provider local fallback response.
- Automated gates for no-key fallback, restore, same-session continuation, streaming coalescing, context, cancellation, and provider errors.

**Should build if P1 is stable:**
- Persisted session titles and last-message previews.
- Active session banner with title/session/model chip.
- Per-run provider/model snapshot displayed on assistant/history items.
- Retry last failed prompt after configuration fix.
- Inline context inclusion/truncation disclosure.
- Collapsible tool/runtime cards in the conversation flow.

**Defer beyond v1.2:**
- Conversation branching/edit/regenerate trees.
- Global session search, prompt libraries, personas, marketplace.
- Long-term memory/RAG/personalization.
- Cross-device/offline/PWA/mobile push.
- Multi-provider automatic fallback policies.
- Full chat export/import.

### Architecture Approach

v1.2 should introduce a **conversation read model** across App and client DTOs, backed by repository ports and shared by Vaadin, REST, future CLI/TUI, and runtime context assembly. The architecture should preserve COLA boundaries: Adapter Web renders and coordinates; App assembles read models and model context; Domain/Runtime remains provider/UI/persistence neutral; Infrastructure supplies JDBC/SQLite stores and provider adapters.

**Major components:**
1. **Conversation DTOs (`pi-agent-client`)** — `SessionSummaryDto`, `ConversationMessageDto`, `ConversationTranscriptResponse`; stable, provider-neutral, UI-neutral contracts.
2. **Conversation query/read model (`pi-agent-app`)** — list recent sessions and get typed transcript using ownership-aware repository queries.
3. **ConversationTranscriptAssembler** — folds run input, model deltas, tool/status/error events into ordered transcript messages.
4. **SessionConversationContextAssembler** — derives bounded model context from transcript; excludes raw tool/audit/provider secrets by default.
5. **ConversationConsoleBridge** — Vaadin-facing facade that delegates only to App use cases; no direct repository access from views.
6. **ChatEventStreamPanel bubble API** — `replaceTranscript`, `appendUserMessage`, `beginAssistantMessage`, `appendAssistantDelta`, `markAssistantTerminal`, `showErrorBubble`.
7. **ConversationEventReducer / ChatStreamAggregator** — idempotent live event reducer keyed by session/run/step/message, separate from durable transcript assembly.
8. **Repository extensions** — recent sessions by tenant/user, runs by session, events by session+run, active/latest run, provider/model snapshot and run input persistence.
9. **Provider readiness/config UI** — stable validation DTO and visible readiness/error states; model selector affects subsequent runs only.

**Critical boundary rules:**
- Do not build context assembly in `ConsoleView`.
- Do not use `ChatEventStreamPanel.messages()` as product history.
- Do not render `SessionHistoryResponse.entries` raw maps as chat.
- Keep public run-event/SSE APIs, but add typed conversation/session APIs.
- Keep Spring AI, Vaadin, SQLite, and OpenAI SDK classes out of Domain/App contracts.

### Pitfalls and Watch-Outs

1. **Session selection without transcript restore** — selecting a card is not continuity. Build canonical transcript query first; selection must hydrate bubbles before continuation.
2. **Polling/final replay mistaken for real streaming** — 750 ms polling can remain fallback, but product streaming needs Vaadin Push or explicit SSE/live subscription and tests proving incremental updates before terminal completion.
3. **Delta chunks lack stable aggregation identity** — aggregate by session/run/step/message, dedupe by event id/sequence, and treat finish/error/cancel as state transitions rather than text.
4. **Multi-turn implemented as session ID reuse only** — current runtime sends empty history. Add context assembler and fake-model tests proving prior turns are included and current prompt appears once.
5. **History, memory, audit, and UI state conflated** — full transcript/audit must remain project-owned; model memory is a bounded derivation, not the source of product history.
6. **Local SQLite becomes a dev-only fork** — use the same ports/read-model semantics and migrations/indexes; local profile should differ only in infrastructure implementation.
7. **Cancellation is cosmetic** — wire cancellation token/runtime stream stop, persist terminal cancellation, and prevent post-cancel delta appends.
8. **Repository filters leak cross-session/tenant data** — event/transcript queries must filter by tenant/user/session/run and have negative security tests.
9. **Kimi-style minimalism hides operational truth** — keep chat primary but show compact provider/tool/error/cancel/model cards where needed.
10. **Provider/model selector rewrites history meaning** — pin provider/model per run and show the actual model used; selector changes affect future runs only.

## Recommended Requirements Categories

Use these categories when drafting v1.2 requirements:

1. **Conversation IA and Chat-First Console** — home layout, history affordance, active session banner, reduced operational noise.
2. **Recent Sessions and Transcript Restore** — durable recent list, typed transcript DTOs, restore bubbles, same-session continuation.
3. **Streaming Bubble Pipeline** — live reducer, pending/terminal states, delta coalescing, cancellation, optional Push/SSE path with polling fallback.
4. **Multi-Turn Runtime Context** — bounded transcript-derived model context, redaction policy, context metadata, fake-model proof.
5. **Provider/Model Configuration UX** — readiness endpoint, masked secrets, selected model persistence, per-run model snapshot, visible errors/fallback.
6. **Local Persistence and Schema Hardening** — SQLite targeted queries/indexes, persisted run input/model metadata, restart/reload verification, alignment with production ports.
7. **Security and Governance Regression** — ownership filters, redaction, no raw tool/provider secrets in context or transcript APIs.
8. **Verification and Product E2E** — App/unit/repository/Vaadin/Playwright gates with stable selectors and no-key/fake-provider paths.

## Recommended Phase Sequence

### Phase 1: Conversation Read Model and Recent Sessions

**Rationale:** Everything else depends on canonical data. Without typed transcripts and recent-session summaries, UI restore and model context will be built on unstable rendered strings or raw event maps.  
**Delivers:** Client DTOs, App query service, repository ports, transcript assembler, recent session list, SQLite/JDBC query support, golden transcript tests.  
**Addresses:** Recent sessions, message restore, durable history, session ordering, provider/model metadata foundation.  
**Avoids:** Vaadin-as-history, raw event log as chat, cross-session leakage if ownership filters are added here.

### Phase 2: Console Session Restore UX

**Rationale:** Once typed transcript exists, product value becomes visible: users can select an old session, see prior turns, and continue.  
**Delivers:** ConversationConsoleBridge, Console load of recent sessions, `selectSession()` transcript hydrate, typed ChatEventStreamPanel bubble API, stable test selectors.  
**Addresses:** Historical session selection, restored bubbles, same-session continuation, chat-first IA.  
**Avoids:** Session selection that only changes highlighted row; UI-only history.

### Phase 3: Streaming Bubble Lifecycle

**Rationale:** Live streaming should be built after the bubble API exists, otherwise reducers will fight generic event rendering.  
**Delivers:** ConversationEventReducer/aggregator, pending assistant bubble, delta append, terminal complete/failed/cancelled/partial states, dedupe, cancellation UI, slow fake-stream tests.  
**Addresses:** Kimi-style progressive response, no fragmented assistant messages, error/cancel feedback.  
**Avoids:** Polling marketed as real streaming, duplicate replay chunks, blank finish bubbles, post-cancel deltas.

### Phase 4: Multi-Turn Runtime Context

**Rationale:** Context needs the same canonical transcript as restore, but should not block initial UX hydration. Implement after transcript semantics are tested.  
**Delivers:** ConversationContextPolicy, SessionConversationContextAssembler, dispatcher/runtime integration, provider-neutral model messages, redaction/truncation metadata, fake-model contract tests.  
**Addresses:** Selected-session multi-turn behavior, bounded context, separation of history and memory.  
**Avoids:** Empty `ModelRequest` history, duplicate current prompt, raw tool/audit secrets in prompts.

### Phase 5: Provider/Model and Local Profile Stability

**Rationale:** Provider readiness and local persistence become much more visible once sessions and streaming work. Stabilize them with the same ports and run metadata.  
**Delivers:** ProviderValidationResponse, visible model bar errors/readiness, masked key checks, per-run provider/model snapshot, labeled fallback/local response, SQLite indexes and persisted run input/model metadata, local restart verification.  
**Addresses:** Local model config persistence, setup failure recovery, history explainability.  
**Avoids:** Silent errors, model selector rewriting history meaning, local SQLite diverging from production behavior.

### Phase 6: Verification, Security, and Regression Hardening

**Rationale:** Productization correctness is semantic; tests must assert grouping, roles, ownership, streaming timing, and context—not only that text eventually appears.  
**Delivers:** App golden tests, repository ownership tests, Vaadin component tests, Playwright product path, ArchUnit boundary checks, no-key/fake-provider/fake-stream/cancel/error scenarios.  
**Addresses:** All must-have acceptance gates and regression prevention.  
**Avoids:** Passing brittle UI tests while shipping broken restore/stream/context behavior.

### Phase Ordering Rationale

- **Read model first:** session restore, recent sessions, context, previews, and model chips all need the same canonical transcript/session summary projection.
- **UI restore before streaming:** typed bubble methods are a prerequisite for clean live delta aggregation.
- **Streaming before or alongside runtime context only after transcript semantics are stable:** otherwise live reducer and context assembler may encode conflicting interpretations of events.
- **Provider/local hardening after core conversation semantics:** provider config is important but should attach to runs/transcripts rather than drive architecture.
- **Verification throughout, consolidated at the end:** every phase should add targeted tests, with Phase 6 closing product/security gaps.

### Research Flags

**Likely needs deeper phase research during planning:**
- **Phase 3 Streaming Bubble Lifecycle:** choose and validate Vaadin Push vs browser SSE vs polling fallback; deployment/proxy and UI lock behavior need implementation proof.
- **Phase 4 Multi-Turn Runtime Context:** exact Domain/App message type and redaction/tool-summary policy need design validation against current `SessionContext` / `ModelRequest` shape.
- **Phase 5 Local Profile Stability:** SQLite schema migration strategy and alignment with production JDBC/Flyway need implementation-specific validation.

**Standard patterns; skip extra research unless code conflicts emerge:**
- **Phase 1 Read Model:** established COLA/App query + DTO + repository-port pattern; codebase evidence is strong.
- **Phase 2 Restore UX:** standard Vaadin component hydration once DTOs exist.
- **Phase 6 Verification:** standard JUnit/Vaadin/Playwright/ArchUnit/Testcontainers-style gates; focus on execution rather than research.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Existing Java/Spring/Vaadin stack is sufficient; official Vaadin/Spring MVC/Spring AI evidence supports no major additions. Push details are MEDIUM-HIGH until tested. |
| Features | HIGH | Must-haves align with current code gaps and known chat product expectations; competitive differentiators are MEDIUM because public UX details are partly inferred. |
| Architecture | HIGH | Integration points are backed by code inspection; class names may change but boundaries are clear. |
| Pitfalls | HIGH | Critical risks are visible in current code: no transcript restore, empty model history, polling loop, empty cancel, run-only event query smell. |

**Overall confidence:** HIGH for roadmap direction; MEDIUM-HIGH for the exact real-time implementation path.

### Open Questions

- **Realtime path:** Should v1.2 target Vaadin Push as the product path, or ship explicit polling fallback first and add Push once stable? Requirement should distinguish “polling fallback” from “real streaming.”
- **Transcript source of user messages:** Where should run input be persisted for both SQLite and production so transcript restore is not dependent on UI memory?
- **Conversation API naming:** Prefer `/api/sessions/recent` + `/api/sessions/{id}/transcript` or `/api/conversations/...`? Either is acceptable if DTOs remain general and not mobile-only.
- **Model message carrier:** Can existing `SessionContext.messages()` safely carry bounded conversation turns, or should a new provider-neutral `ConversationTurn`/`ModelMessage` be introduced?
- **Tool context policy:** For v1.2, should tool calls/results appear only as transcript cards, or should redacted tool summaries be included in model context when available?
- **Cancellation depth:** Does the current streaming client support true cancellation, or can v1.2 only stop UI aggregation and mark terminal cancellation while provider cancellation is added later?
- **Local migrations:** Should SQLite local profile adopt Flyway immediately or use a controlled versioned initializer as an interim?
- **Provider validation persistence:** Should `lastValidationError` and `validatedAt` be persisted, or only computed for current UI session?

## Sources

### Primary (HIGH confidence)
- `.planning/research/STACK.md` — stack direction, current versions, Vaadin/Spring MVC/SSE, SQLite hardening, no new heavy libraries.
- `.planning/research/FEATURES.md` — table stakes, differentiators, anti-features, acceptance behaviors.
- `.planning/research/ARCHITECTURE.md` — COLA boundaries, component responsibilities, data flows, phase build order.
- `.planning/research/PITFALLS.md` — critical/moderate/minor pitfalls, prevention checklist, phase warnings.
- Current code evidence referenced by research: `ConsoleView`, `ChatEventStreamPanel`, `AppConsoleRunExecutionBridge`, `DefaultRunQueryService`, `DefaultRunDispatcher`, `DynamicAgentRuntime`, `SqliteLocalPersistence`, `SessionController`, repository ports, `ModelRequest`, `RunContext`, `SessionContext`.
- Official Vaadin docs — server push and `UI.access(...)` guidance for background updates.
- Official Spring Framework docs — Spring MVC async/SSE suitability.
- Official Spring AI docs — distinction between full chat history and model memory/context.
- Kimi streaming docs — token-by-token streaming UX and `[DONE]` terminal behavior.
- OpenAI conversation-state docs — stateless model requests require prior messages/context window management.

### Secondary (MEDIUM confidence)
- Competitive chat UX background from public ChatGPT/Kimi-style patterns — used only to shape differentiators, not core requirements.

---
*Research completed: 2026-06-28*  
*Ready for roadmap: yes*  
*Commit status: not committed; orchestrator will handle commits.*
