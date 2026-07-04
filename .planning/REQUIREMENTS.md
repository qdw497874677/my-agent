# Requirements: Pi Java Agent Platform — v1.2 Console 对话产品化

**Defined:** 2026-06-28  
**Core Value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。

## v1.2 Requirements

### Conversation IA and Chat-First Console

- [x] **CIA-01**: User sees a chat-first Console home focused on message input, conversation feed, recent history, and compact provider/model status.
- [x] **CIA-02**: User can clearly distinguish a new conversation from a selected historical session through a visible active-session title or banner.
- [x] **CIA-03**: User sees runtime, tool, provider, and diagnostic details collapsed or hidden by default unless needed for tool calls, approvals, errors, cancellation, or diagnostics.
- [x] **CIA-04**: User can access advanced run/session/tool details without leaving the conversation flow when those details are relevant.

### Recent Sessions and Transcript Restore

- [ ] **SESS-01**: User can see recent sessions on Console load, ordered by latest activity, with title, last activity, status, and last-message preview.
- [x] **SESS-02**: User can select a historical session and see previous user and assistant turns restored as chat bubbles.
- [x] **SESS-03**: User can continue a selected historical session without accidentally creating a new session.
- [ ] **SESS-04**: Restored transcript comes from typed persisted data/read models, not Vaadin in-memory state or raw run-event maps.
- [ ] **SESS-05**: User can refresh/reopen the Console and recover persisted sessions and their conversation transcript in local profile.

### Streaming Bubble Pipeline

- [x] **STRM-01**: User sees a pending assistant bubble promptly after sending a message.
- [x] **STRM-02**: Model delta events append to one assistant bubble in order, without fragmented token cards or duplicate replay chunks.
- [x] **STRM-03**: User sees clear completed, failed, cancelled, or partial stream state without generic run-status noise in the main chat.
- [x] **STRM-04**: Active streaming remains replay-safe across polling/push refreshes using event sequence or event id dedupe.
- [x] **STRM-05**: User can cancel an active response and see the assistant bubble marked as stopped or partial, with no later deltas appended after cancellation.

### Multi-Turn Runtime Context

- [x] **CTX-01**: Next message in a selected session includes bounded prior user and assistant turns in model context.
- [x] **CTX-02**: Context assembly applies a configurable budget for recent turns, characters, or token approximation and records whether history was truncated.
- [x] **CTX-03**: Tool, audit, provider, credential, and other sensitive data is excluded or redacted before any context is sent to a model.
- [x] **CTX-04**: Tests prove the current prompt appears exactly once and prior turns are available to the fake model.
- [x] **CTX-05**: Model context assembly is implemented in App/runtime seams, not in Vaadin component state.

### Provider, Model, and Local Profile UX

- [ ] **PROV-01**: User can see provider/model readiness and actionable errors from the Console model area.
- [ ] **PROV-02**: User can refresh model choices and see success, empty, and error states without silent failures.
- [ ] **PROV-03**: Model selection persists locally and affects only subsequent runs.
- [x] **PROV-04**: Each run records the actual provider, model, and fallback mode used for history and debugging.
- [ ] **PROV-05**: No-provider fallback is clearly labeled as local fallback, not a real model answer.
- [x] **PROV-06**: SQLite local profile persists sessions, transcripts, run metadata, provider config, and survives restart for Console continuation.

### Security, Boundaries, and Verification

- [ ] **VER-01**: Automated tests cover no-key fallback, configured-provider path, recent-session restore, same-session continuation, streaming coalescing, cancellation/error states, and provider errors.
- [ ] **VER-02**: Repository/query tests prevent cross-tenant, cross-user, cross-session, or cross-run transcript/event leakage.
- [ ] **VER-03**: Architecture tests preserve COLA boundaries and keep Vaadin, Spring AI, SQLite, and provider SDK types out of Domain/App contracts where prohibited.
- [ ] **VER-04**: Browser tests verify the Kimi-style Console product path with stable selectors and no raw runtime-event noise in the main chat.
- [ ] **VER-05**: Fake slow-stream tests prove assistant text appears incrementally before terminal completion.

## Future Requirements

### Conversation Enhancements

- **FUT-01**: User can search all historical sessions by text.
- **FUT-02**: User can rename, archive, pin, or delete conversations.
- **FUT-03**: User can branch, edit, or regenerate previous messages.
- **FUT-04**: User can export or import full conversations.
- **FUT-05**: User can use prompt templates, personas, or prompt libraries.

### Advanced Memory and Provider Behavior

- **FUT-06**: Agent can use long-term memory, RAG, or personalization beyond current-session context.
- **FUT-07**: Admin can configure explicit multi-provider fallback policies.
- **FUT-08**: User can see advanced context compaction/summarization controls.

## Out of Scope

| Feature | Reason |
|---------|--------|
| React/Next.js Console rewrite | Violates Java/Vaadin-first direction and adds a second frontend stack for a milestone focused on product semantics. |
| Mobile-only or Console-only backend API fork | Conversation APIs should be reusable by future CLI/TUI and desktop clients, not viewport-specific. |
| Vector DB / long-term memory / RAG | Different product domain; v1.2 focuses on bounded current-session context. |
| Conversation branching/edit/regenerate | Complicates transcript model; linear conversation is enough for productization. |
| Automatic paid-provider fallback | Cost/compliance/debugging risk; provider fallback policy should be explicit and later. |
| Full Agent Studio or workflow builder | Existing scope is conversation productization, not visual agent authoring. |
| Browser localStorage as history source of truth | Would hide persistence bugs and diverge from cloud/runtime clients. |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CIA-01 | Phase 17 | Complete |
| CIA-02 | Phase 17 | Complete |
| CIA-03 | Phase 17 | Complete |
| CIA-04 | Phase 17 | Complete |
| SESS-01 | Phase 16 | Pending |
| SESS-02 | Phase 17 | Complete |
| SESS-03 | Phase 17 | Complete |
| SESS-04 | Phase 16 | Pending |
| SESS-05 | Phase 20 | Pending |
| STRM-01 | Phase 18 | Complete |
| STRM-02 | Phase 18 | Complete |
| STRM-03 | Phase 18 | Complete |
| STRM-04 | Phase 18 | Complete |
| STRM-05 | Phase 18 | Complete |
| CTX-01 | Phase 19 | Complete |
| CTX-02 | Phase 19 | Complete |
| CTX-03 | Phase 19 | Complete |
| CTX-04 | Phase 19 | Complete |
| CTX-05 | Phase 19 | Complete |
| PROV-01 | Phase 20 | Pending |
| PROV-02 | Phase 20 | Pending |
| PROV-03 | Phase 20 | Pending |
| PROV-04 | Phase 20 | Complete |
| PROV-05 | Phase 20 | Pending |
| PROV-06 | Phase 20 | Complete |
| VER-01 | Phase 21 | Pending |
| VER-02 | Phase 21 | Pending |
| VER-03 | Phase 21 | Pending |
| VER-04 | Phase 21 | Pending |
| VER-05 | Phase 21 | Pending |

**Coverage:**
- v1.2 requirements: 30 total
- Mapped to phases: 30
- Unmapped: 0 ✓

---
*Requirements defined: 2026-06-28*
*Last updated: 2026-06-28 after milestone v1.2 roadmap creation*
