# Roadmap: Pi Java Agent Platform — v1.2 Console 对话产品化

**Created:** 2026-06-28  
**Granularity:** Standard  
**Mode:** YOLO  
**Milestone:** v1.2 Console 对话产品化  
**Previous milestone:** v1.1 completed Phases 10-15  
**v1.2 Requirements:** 30  
**Mapped:** 30 / 30 ✓

## Overview

Milestone v1.2 turns the existing Vaadin Console from a run/event workbench that can produce a reply into a daily-use, Kimi-homepage-like Agent conversation product. It focuses on durable recent sessions, typed transcript restore, one-bubble streaming, selected-session multi-turn model context, local provider/model stability, and verification/security gates.

The roadmap preserves the project’s Java/Vaadin/COLA direction: no React/Next.js rewrite, no mobile-only API fork, no WebFlux rewrite, no vector memory/RAG, and no provider SDK types in Domain/App contracts. Work proceeds from canonical conversation read models to visible UX, then streaming, context, local/provider stability, and final verification.

## Milestones

- ✅ **v1.0 Agent Platform Foundation** — Phases 1-9
- ✅ **v1.1 适配移动端web** — Phases 10-15
- 🚧 **v1.2 Console 对话产品化** — Phases 16-21

## Phases

**Phase Numbering:** Continue from previous milestone. v1.1 ended at Phase 15; v1.2 starts at Phase 16.

- [x] **Phase 16: Conversation Read Model and Recent Sessions** — Establish typed conversation/session DTOs, query services, repository filters, and transcript assembly before UI polish.
- [x] **Phase 17: Console Session Restore UX** — Make the chat-first Console load recent sessions, restore selected transcripts as bubbles, and continue the active session. (completed 2026-06-29)
- [x] **Phase 18: Streaming Bubble Lifecycle** — Convert run/model events into one live assistant bubble with pending, delta, terminal, error, and cancel states. (completed 2026-06-30)
- [x] **Phase 19: Multi-Turn Runtime Context** — Feed bounded selected-session transcript context into model execution while preserving redaction and provider-neutral boundaries. (completed 2026-07-01)
- [ ] **Phase 20: Provider/Model and Local Profile Stability** — Productize provider/model readiness, model snapshots, fallback labeling, and SQLite local persistence/restart behavior.
- [ ] **Phase 21: Verification, Security, and Regression Hardening** — Prove restore, streaming, context, cancellation, provider errors, ownership filters, and architecture boundaries.

## Phase Details

### Phase 16: Conversation Read Model and Recent Sessions

**Goal:** Users and future clients have a canonical, ownership-safe conversation read model for recent sessions and typed transcripts, instead of relying on Vaadin memory or raw run-event maps.

**Depends on:** Phase 15

**Requirements:** SESS-01, SESS-04

**Plans:** 4 plans

Plans:
- [x] 16-conversation-read-model-and-recent-sessions-01-PLAN.md — Public typed conversation DTO contracts for recent sessions and transcripts.
- [x] 16-conversation-read-model-and-recent-sessions-02-PLAN.md — App-layer conversation query service, transcript assembler, and ownership-aware ports.
- [x] 16-conversation-read-model-and-recent-sessions-03-PLAN.md — JDBC/local persistence implementations for recent-session and transcript read-model queries.
- [x] 16-conversation-read-model-and-recent-sessions-04-PLAN.md — Session-centric REST endpoints and minimal Console proof hooks.

**Success Criteria:**
1. App/client exposes typed session summary and transcript DTOs with roles, text, session/run refs, status, timestamps, and metadata.
2. Recent sessions are queryable by tenant/user and ordered by latest activity.
3. Transcript assembly folds persisted run inputs and model/tool/error events into stable user/assistant/tool/error messages.
4. Repository queries enforce tenant/user/session/run ownership filters.
5. Golden tests prove raw run-event maps are not the UI transcript contract.

**UI hint:** yes — no major visual polish yet; only enough Console hooks to prove the read model.

### Phase 17: Console Session Restore UX

**Goal:** Users can use the Console as a chat product: see recent conversations, select one, restore prior turns as bubbles, and continue the selected session.

**Depends on:** Phase 16

**Requirements:** CIA-01, CIA-02, CIA-03, CIA-04, SESS-02, SESS-03

**Plans:** 6/6 plans complete

Plans:
- [x] 17-console-session-restore-ux-01-PLAN.md — Compact recent-history card surface backed by Phase 16 session summaries.
- [x] 17-console-session-restore-ux-02-PLAN.md — Typed transcript bubble/card hydration API with stable message selectors.
- [x] 17-console-session-restore-ux-03-PLAN.md — Active-session banner, restore orchestration, and same-session continuation.
- [x] 17-console-session-restore-ux-04-PLAN.md — i18n, Playwright restore product-path gate, and selector/deferred-boundary docs.
- [x] 17-console-session-restore-ux-05-PLAN.md — Gap closure for visible recent-history/details access and transcript status i18n fallback.
- [x] 17-console-session-restore-ux-06-PLAN.md — Gap closure for browser restore path and selector documentation aligned with visible controls.

**Success Criteria:**
1. Console defaults to a chat-first home with compact history and model/provider affordances.
2. Selecting a historical session clears the current feed and hydrates typed transcript bubbles.
3. The active session title/banner clearly distinguishes new vs continued conversation.
4. Sending after session selection appends to the selected session, not a new session.
5. Runtime/tool/provider details remain collapsed by default but reachable when relevant.

**UI hint:** yes — Kimi-style IA and Vaadin component state are central.

### Phase 18: Streaming Bubble Lifecycle

**Goal:** Users experience model output as a single live assistant answer, not as runtime cards, delayed final replay, or fragmented token rows.

**Depends on:** Phase 17

**Requirements:** STRM-01, STRM-02, STRM-03, STRM-04, STRM-05

**Plans:** 4/4 plans complete

Plans:
- [x] 18-streaming-bubble-lifecycle-01-PLAN.md — Reducer and ChatEventStreamPanel live assistant bubble contract.
- [x] 18-streaming-bubble-lifecycle-02-PLAN.md — Vaadin Push live subscription wiring and stream-mode hooks.
- [x] 18-streaming-bubble-lifecycle-03-PLAN.md — Cancellation, failure, and terminal-state suppression semantics.
- [x] 18-streaming-bubble-lifecycle-04-PLAN.md — Fake-runtime, Playwright, and documentation verification handoff.

**Success Criteria:**
1. A pending assistant bubble appears promptly after send.
2. Model delta events append to the same assistant bubble in order.
3. Replayed/polled/pushed events are deduped by stable sequence or event identity.
4. Completion, failure, cancellation, and partial response states update the bubble without generic run-status noise.
5. Cancelling an active response prevents later deltas from mutating the stopped bubble.

**UI hint:** yes — requires reducer/aggregator and browser/Vaadin timing verification.

### Phase 19: Multi-Turn Runtime Context

**Goal:** Continuing a selected session means the model actually receives bounded prior turns, not only the same `sessionId`.

**Depends on:** Phase 16, Phase 18

**Requirements:** CTX-01, CTX-02, CTX-03, CTX-04, CTX-05

**Plans:** 5/5 plans complete

Plans:
- [x] 19-multi-turn-runtime-context-01-PLAN.md — App context policy, metadata, and safe transcript-to-message assembler.
- [x] 19-multi-turn-runtime-context-02-PLAN.md — Dispatch-time context injection and safe metadata wiring.
- [x] 19-multi-turn-runtime-context-03-PLAN.md — OpenAI-compatible ordered message provider boundary.
- [x] 19-multi-turn-runtime-context-04-PLAN.md — Fake-model capture, CTX-04 proof, and architecture/safety gates.

**Success Criteria:**
1. Runtime/model execution receives bounded prior user/assistant turns for the selected session.
2. Context policy limits recent turns/chars/token approximation and records truncation metadata.
3. Sensitive tool/audit/provider/credential data is excluded or redacted before model context assembly.
4. Fake-model tests prove prior turns are present and the current prompt appears exactly once.
5. Context assembly lives in App/runtime seams, not in Vaadin component state.

**UI hint:** no — mostly App/runtime/model contract work, with minimal UI disclosure for truncation if needed.

### Phase 20: Provider/Model and Local Profile Stability

**Goal:** Local development and real provider usage feel trustworthy: provider/model readiness is visible, errors are actionable, model choices persist, and local SQLite can restore conversations after restart.

**Depends on:** Phase 16, Phase 17

**Requirements:** SESS-05, PROV-01, PROV-02, PROV-03, PROV-04, PROV-05, PROV-06

**Plans:** 4/5 plans executed

Plans:
- [x] 20-provider-model-and-local-profile-stability-01-PLAN.md — Compact provider/model readiness and explicit refresh feedback states.
- [x] 20-provider-model-and-local-profile-stability-02-PLAN.md — Persisted model selection, next-run-only copy, no-provider block, and explicit fallback labels.
- [x] 20-provider-model-and-local-profile-stability-03-PLAN.md — Safe run provider/model/fallback metadata contracts plus JDBC/SQLite persistence.
- [x] 20-provider-model-and-local-profile-stability-04-PLAN.md — Run creation, queue, dispatch, and runtime wiring for selected model snapshots and fallback facts.
- [ ] 20-provider-model-and-local-profile-stability-05-PLAN.md — Same-DB SQLite local profile restart proof and Phase 21 verification handoff.

**Success Criteria:**
1. Console model area shows provider/model readiness and actionable errors.
2. Model refresh shows success, empty, and error states without silent catches.
3. Model selection persists and applies only to subsequent runs.
4. Each run records provider/model/fallback mode for history and debugging.
5. No-provider fallback is visually labeled as local fallback.
6. SQLite local profile persists sessions, transcript data, run metadata, and provider config across restart.

**UI hint:** yes — provider/model feedback is user-visible.

### Phase 21: Verification, Security, and Regression Hardening

**Goal:** The milestone is release-ready because tests prove conversation semantics, not just that text appears eventually.

**Depends on:** Phase 16, Phase 17, Phase 18, Phase 19, Phase 20

**Requirements:** VER-01, VER-02, VER-03, VER-04, VER-05

**Success Criteria:**
1. Automated tests cover no-key fallback, configured-provider path, recent-session restore, continuation, streaming coalescing, cancellation/error, and provider errors.
2. Repository/query tests prevent cross-tenant, cross-user, cross-session, and cross-run leakage.
3. Architecture tests preserve COLA boundaries and prevent prohibited framework/infra types from leaking into Domain/App contracts.
4. Browser tests verify Kimi-style Console product paths with stable selectors and no raw runtime-event noise.
5. Fake slow-stream tests prove assistant text appears incrementally before terminal completion.

**UI hint:** yes — product E2E gates validate visible UX.

## Progress

**Execution Order:** 16 → 17 → 18 → 19 → 20 → 21

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 16. Conversation Read Model and Recent Sessions | v1.2 | 4/4 | Completed | 2026-06-28 |
| 17. Console Session Restore UX | v1.2 | 6/6 | Complete   | 2026-06-29 |
| 18. Streaming Bubble Lifecycle | v1.2 | 4/4 | Complete    | 2026-06-30 |
| 19. Multi-Turn Runtime Context | v1.2 | 5/5 | Complete    | 2026-07-03 |
| 20. Provider/Model and Local Profile Stability | v1.2 | 4/5 | In Progress|  |
| 21. Verification, Security, and Regression Hardening | v1.2 | 0/TBD | Not started | — |

## Coverage Validation

| Requirement Prefix | Count | Phase |
|--------------------|-------|-------|
| CIA | 4 | Phase 17 |
| SESS | 5 | Phase 16, 17, 20 |
| STRM | 5 | Phase 18 |
| CTX | 5 | Phase 19 |
| PROV | 6 | Phase 20 |
| VER | 5 | Phase 21 |

**Coverage map:**
- CIA-01 → Phase 17
- CIA-02 → Phase 17
- CIA-03 → Phase 17
- CIA-04 → Phase 17
- SESS-01 → Phase 16
- SESS-02 → Phase 17
- SESS-03 → Phase 17
- SESS-04 → Phase 16
- SESS-05 → Phase 20
- STRM-01 → Phase 18
- STRM-02 → Phase 18
- STRM-03 → Phase 18
- STRM-04 → Phase 18
- STRM-05 → Phase 18
- CTX-01 → Phase 19
- CTX-02 → Phase 19
- CTX-03 → Phase 19
- CTX-04 → Phase 19
- CTX-05 → Phase 19
- PROV-01 → Phase 20
- PROV-02 → Phase 20
- PROV-03 → Phase 20
- PROV-04 → Phase 20
- PROV-05 → Phase 20
- PROV-06 → Phase 20
- VER-01 → Phase 21
- VER-02 → Phase 21
- VER-03 → Phase 21
- VER-04 → Phase 21
- VER-05 → Phase 21

**Total mapped:** 30 / 30 ✓  
**Duplicates:** 0 ✓  
**Orphans:** 0 ✓

## Constraints Preserved

- Java/Vaadin-first Console productization only.
- COLA boundaries preserved: Adapter Web renders/coordinations; App owns read models/context assembly; Domain/runtime remains UI/provider/persistence neutral; Infrastructure owns persistence/provider adapters.
- No React/Next.js, mobile-only API fork, WebFlux rewrite, vector DB/RAG, long-term memory, or automatic paid-provider fallback.
- Conversation APIs/read models are general client contracts usable by future desktop/CLI/TUI clients.
- Sensitive tool/audit/provider/credential data remains redacted or excluded from transcript/context surfaces.

---
*Roadmap created: 2026-06-28 for milestone v1.2 Console 对话产品化*
