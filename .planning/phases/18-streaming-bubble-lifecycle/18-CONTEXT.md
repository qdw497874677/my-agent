# Phase 18: Streaming Bubble Lifecycle - Context

**Gathered:** 2026-06-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 18 converts active run/model events into a single live assistant answer in the Vaadin Console. Users must see a pending assistant bubble promptly after run creation, watch model deltas append to that same bubble in order, and see clear completed, failed, cancelled, or partial states without raw run-status noise in the main chat.

This phase owns the live streaming bubble lifecycle: pending bubble creation, delta aggregation, replay/push/poll dedupe, terminal state mutation, cancellation feedback, post-cancel delta suppression, and streaming-specific verification. It does **not** implement Phase 19 multi-turn model context, Phase 20 provider/model/local profile stability, conversation search/rename/archive/pin/delete, provider fallback policy, React/Next.js, WebFlux rewrite, or a broad Console cleanup.

</domain>

<decisions>
## Implementation Decisions

### Realtime Product Path
- **D-01:** Phase 18 should target Vaadin Push or an explicit SSE live subscription as the product streaming path. Existing 750ms polling may remain only as a fallback/test seam.
- **D-02:** Polling fallback must be distinguishable from product streaming. The UI should expose a stable hook such as `data-stream-mode="push"`, `data-stream-mode="sse"`, or `data-stream-mode="polling-fallback"` so tests cannot accidentally pass by treating polling/final replay as real streaming.
- **D-03:** Planning/research should validate the exact Vaadin update mechanism, including `UI.access(...)`, attach/detach cleanup, and replay-before-subscribe semantics. Do not call the current poll loop “real streaming.”

### Assistant Bubble Aggregation
- **D-04:** Live assistant aggregation must be keyed at minimum by `sessionId + runId + stepId`. If an explicit assistant `messageId` exists or is easy to introduce, it may be included as an additional identity, but Phase 18 planning should not depend on a mandatory new message-id data model before using the existing run event DTOs.
- **D-05:** A pending assistant bubble begins immediately after session/run creation succeeds and a run identity is available. Do not wait for the first delta. Do not create the pending bubble before run creation unless failure rollback/error handling is explicitly planned.
- **D-06:** Only non-empty `model.delta` text should append to the primary assistant bubble. Tool events, approval events, runtime status, provider diagnostics, and other operational details stay as compact secondary inline cards/details using existing Phase 13 redaction/detail discipline.
- **D-07:** Completion, failure, cancellation, and finish metadata are state transitions on the assistant bubble, not standalone blank messages and not assistant prose. Terminal events should finalize/mark the bubble and flush any buffered delta.
- **D-08:** Replay/poll/push duplicates must be idempotent. Track per-run sequence cursor and rendered event IDs; repeated sequence/eventId must not append duplicate text. Text-content dedupe alone is not acceptable because repeated tokens/words may be legitimate.

### Cancellation and Terminal States
- **D-09:** Cancellation must be more than a CSS-only UI state. Phase 18 should at least connect the Console cancel action to the runtime cancellation seam/token, publish or observe a cancelled terminal state, immediately stop UI aggregation for that run, and prevent later deltas from mutating the stopped bubble.
- **D-10:** If the current provider adapter cannot truly abort the underlying stream, Phase 18 should document that limitation and still enforce runtime/UI suppression and cancelled terminal semantics. Full provider-level abort can be incremental only if the existing adapter seam supports it without destabilizing the phase.
- **D-11:** After user cancellation, preserve already-generated assistant text and mark the bubble as partial/stopped/cancelled. Do not clear the partial answer, and do not present user-initiated stop as a generic model failure.
- **D-12:** Provider/runtime failure should mark the assistant bubble as failed and show a safe, redacted error card or status summary in the conversation flow. Raw provider/tool/audit payloads must not be inserted into assistant prose.

### Delta Coalescing and Performance
- **D-13:** Delta rendering should use a bounded time and/or character coalescing strategy before mutating Vaadin text, e.g. a small 50-150ms-style flush window or accumulated character threshold. The exact thresholds are implementation discretion.
- **D-14:** Terminal/failed/cancelled transitions must force-flush buffered text before final status is shown.
- **D-15:** Verification should assert streaming semantics and component stability, not exact millisecond timing: visible text changes before terminal completion, final text is exact, all deltas land in one assistant bubble, and component count does not grow per token/chunk.

### Verification and Phase 17 Test Foundation
- **D-16:** Phase 18 may include narrow Phase 17 carry-over fixes only when they block streaming verification, especially status i18n fallback and necessary visible controls/panels for browser paths. Do not expand this into broad Console UI/layout cleanup.
- **D-17:** Core Phase 18 gates should cover a fake slow stream, pending bubble appearance, incremental same-bubble delta append, replay/poll/push dedupe, terminal completion, cancellation with no post-cancel append, failed/provider-error safe display, and stable selectors.
- **D-18:** Tests should combine reducer/component-level coverage with product-path browser coverage where feasible. A pure final-text assertion is insufficient because it can miss grouping, replay, cancellation, and “real streaming vs polling fallback” regressions.

### Folded Todos
- No pending todos matched Phase 18 scope.

### the agent's Discretion
- Exact class names (`ConversationEventReducer`, `ChatStreamAggregator`, etc.), package placement within adapter-web/App seams, and internal DTO names are planner discretion as long as COLA boundaries are preserved.
- Exact Push-vs-SSE implementation details are research/planning discretion, but the delivered product path must be distinguishable from polling fallback.
- Exact coalescing thresholds, CSS classes, icons, status badge wording, and visual polish are implementation discretion, provided pending/partial/failed/cancelled states are visible and stable selectors remain available.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 18 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 18 — Phase goal, dependency on Phase 17, STRM-01 through STRM-05 mapping, success criteria, and UI hint for reducer/aggregator and timing verification.
- `.planning/REQUIREMENTS.md` §Streaming Bubble Pipeline — STRM-01 through STRM-05 define pending bubble, ordered delta append, terminal/error/cancel states, replay-safe dedupe, and cancellation semantics.
- `.planning/PROJECT.md` §Current Milestone: v1.2 Console 对话产品化 — Real streaming display and Kimi-style Console direction.
- `.planning/STATE.md` §Accumulated Context — Prior Console/mobile decisions and Phase 17 handoff notes.

### Prior Phase Decisions That Must Be Carried Forward
- `.planning/phases/17-console-session-restore-ux/17-CONTEXT.md` — D-12 defines downstream selectors; D-15 through D-17 explicitly defer pending/live assistant bubble lifecycle, delta coalescing, terminal/error/cancel mutation, replay dedupe, and post-cancel suppression to Phase 18; D-18/D-19 preserve collapsed-but-reachable operational truth.
- `.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md` — Known Phase 17 verification gaps that may affect Phase 18 streaming test foundation, including i18n fallback and live browser path limitations.
- `.planning/phases/16-conversation-read-model-and-recent-sessions/16-CONTEXT.md` — Typed transcript roles/status semantics; finish/error/cancel are status transitions rather than raw chat rows; raw run-event maps are not the main chat transcript contract.
- `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-CONTEXT.md` — Runtime/tool/approval cards stay inline, compact, expandable, redacted, and secondary to the conversation.
- `.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md` — Existing Console feed/composer/cancel seams, stable selector philosophy, and adapter-web bridge pattern.
- `.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-CONTEXT.md` — Stable selector, no-screenshot, deterministic browser gate philosophy.

### v1.2 Research Inputs
- `.planning/research/ARCHITECTURE.md` §ConsoleView seam — Route new events through `ConversationEventReducer`; keep `RunEventRenderer` for collapsible tool/runtime/debug cards, not primary assistant text.
- `.planning/research/ARCHITECTURE.md` §ChatEventStreamPanel seam — Target typed bubble API: `replaceTranscript`, `appendUserMessage`, `beginAssistantMessage`, `appendAssistantDelta`, `markAssistantTerminal`, `showErrorBubble`, `showToolCard`; required selectors include `data-message-role`, `data-session-id`, `data-run-id`, `data-stream-state`.
- `.planning/research/ARCHITECTURE.md` §Pattern 2: Event Reducer for Live UI — Separate live UI mutation from durable transcript assembly.
- `.planning/research/ARCHITECTURE.md` §Phase C — Recommended build order for `ConversationEventReducer`/`ChatStreamAggregator`, pending bubble, dedupe, terminal handling, polling fallback, and fake slow-stream tests.
- `.planning/research/ARCHITECTURE.md` §Risks and Mitigations / §Verification Gates / §Decision Log — Dedupe, tool/status corruption risk, Vaadin UI lock risk, cancellation test, and push/SSE preferred with polling fallback allowed.
- `.planning/research/PITFALLS.md` #2 — Polling must not be mistaken for real streaming; use Push or explicit SSE live bridge and mark polling fallback.
- `.planning/research/PITFALLS.md` #3 — Delta aggregation needs stable identity and must route tool/status separately.
- `.planning/research/PITFALLS.md` #7/#8/#13/#14/#16/#20 — Cancellation, Vaadin `UI.access`, delta coalescing, finish/error as state transitions, and semantic tests.
- `.planning/research/SUMMARY.md` §Open Questions — Realtime path and cancellation depth are now locked by this context: product path is Push/SSE-first with polling fallback; cancellation must connect UI and runtime seams, with provider abort best-effort if supported.

### Existing Code Contracts to Inspect
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` — Current attach polling, `planChatSubmission(...)`, `appendRunEvents(...)`, `refreshActiveRunEvents(...)`, and `handleCancelRunningRun(...)` seams.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` — Current transcript hydration, append behavior, `activeAssistantLine` heuristic, status/stream-state selectors, and i18n fallback area.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java` — Current `model.delta` and terminal event rendering logic; should remain for secondary cards or be bypassed by the reducer for primary bubble mutation.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/EventStreamClient.java` — Existing stream URL/spec builder.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/RunEventStreamService.java` and `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/RunEventStreamController.java` — Existing replay-before-subscribe SSE endpoint, sequence event IDs, `Last-Event-ID`, and terminal close behavior.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java` and `AppConsoleRunExecutionBridge.java` — Vaadin-to-App seam for events, runs, and cancellation.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/event/RunEventDto.java` — Event identity, sequence, type, payload, run/session/step refs used by the reducer.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java`, `ConversationMessageStatus.java`, and `ConversationMessageRole.java` — Existing typed conversation status/role vocabulary.
- `pi-agent-adapter-web/src/main/resources/messages.properties` and `pi-agent-adapter-web/src/main/resources/messages_zh.properties` — Streaming/cancel/failed/pending i18n keys and fallback behavior.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleTranscriptHydrationTest.java`, `RunSseIntegrationTest.java`, and existing Console mobile/critical-flow Playwright specs — Baselines for typed bubbles, SSE replay, selectors, and browser gates.
- `e2e/fixtures/fake-runtime.ts` — Extend or mirror for deterministic slow-stream, replay, cancel, and failure scenarios.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `RunEventDto`: provides `eventId`, `sequence`, `type`, `payload`, `runId`, `sessionId`, and `stepId`; sufficient for the Phase 18 reducer key and dedupe rules.
- `RunEventStreamService` / `RunEventStreamController`: already support server-side SSE, replay-before-subscribe, sequence as SSE id, `Last-Event-ID`, `afterSequence`, and terminal completion.
- `ChatEventStreamPanel`: already owns the chat feed and typed transcript rendering surface; should gain formal live bubble methods rather than relying on `activeAssistantLine`.
- `ConsoleView.appendRunEvents(...)`: current poll/replay event loop and sequence cursor; main integration point for routing events through a reducer.
- `ConsoleView.planChatSubmission(...)`: current create/reuse session + create run seam; correct place to begin pending assistant bubble once run identity exists.
- `ConsoleView.handleCancelRunningRun(...)`: existing cancel UI seam; should update runtime cancellation and local stream aggregation state.
- `RunEventRenderer`, `ToolCallCard`, `ApprovalCard`, `RuntimeEventCard`, and `RuntimeDetailRedactor`: keep for secondary operational cards and redacted details.
- `ConversationMessageStatus`: existing status vocabulary includes `PENDING`, `COMPLETED`, `FAILED`, `CANCELLED`, and `PARTIAL`.

### Established Patterns
- Adapter Web owns Vaadin presentation state and stable selectors; App/Domain/client contracts must stay free of Vaadin/Push/SSE UI types.
- Existing Console updates are poll-backed; Phase 18 must distinguish polling fallback from the product streaming path.
- Prior phases prefer additive changes to existing Console seams over new routes, mobile-only APIs, or frontend stack changes.
- Stable `data-*` selectors are part of the testing contract; tests should assert roles, run/session identity, stream state, grouping, and mode hooks.
- Runtime/tool/approval details must remain redacted and reachable but should not fragment primary assistant prose.

### Integration Points
- Add a live event reducer/aggregator between `ConsoleView.appendRunEvents(...)` and `ChatEventStreamPanel` primary bubble mutation.
- Extend `ChatEventStreamPanel` with live bubble APIs such as `beginAssistantMessage`, `appendAssistantDelta`, `markAssistantTerminal`, and `showErrorBubble` while preserving `replaceTranscript(...)` for restored history.
- Use the existing SSE service or Vaadin Push-backed subscription for product streaming; retain `refreshActiveRunEvents()` as fallback/replay path only.
- Ensure cancellation updates both runtime/app seams and the UI reducer state so post-cancel deltas are ignored even if a provider emits late events.
- Add i18n keys/fallbacks for pending, streaming, completed, failed, cancelled, partial/stopped, and fallback stream mode labels as needed.

</code_context>

<specifics>
## Specific Ideas

- The user selected the recommended path for all discussed Phase 18 areas.
- Product streaming should be Push/SSE-first, not a relabeled polling loop.
- Main chat should feel like a Kimi-style single answer bubble while preserving operational truth in compact secondary cards.
- Cancellation should show the preserved partial answer as stopped/cancelled and should not allow later deltas to mutate it.
- Tests should prove streaming semantics, not just eventual final text.

</specifics>

<deferred>
## Deferred Ideas

- Phase 19 multi-turn runtime context assembly and model history injection.
- Phase 20 provider/model readiness, model selection persistence, per-run model/fallback metadata, and SQLite restart persistence.
- Broad Console cleanup beyond narrow i18n/control visibility fixes needed for Phase 18 test foundation.
- Full provider-level stream abort for adapters that cannot support it through the existing runtime/provider seams; document limitation if not feasible in Phase 18.
- Conversation search, rename, archive, pin, delete, branching, editing, regeneration, import/export, prompt libraries, long-term memory, RAG, vector DB, and automatic paid-provider fallback.

</deferred>

---

*Phase: 18-streaming-bubble-lifecycle*
*Context gathered: 2026-06-29*
