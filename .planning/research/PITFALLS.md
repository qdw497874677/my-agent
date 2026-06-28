# Pitfalls Research: Console Conversation Productization

**Domain:** Kimi-style chat UX, historical session restore, real streaming delta aggregation, local persistence, and multi-turn context in an existing Java/Spring/Vaadin Agent Platform  
**Project:** Pi Java Agent Platform  
**Researched:** 2026-06-28  
**Overall confidence:** HIGH for pitfalls visible in current code and official Vaadin/Spring AI behavior; MEDIUM for phase numbering until the v1.2 roadmap is generated.

## Summary

The current Console is close enough to demonstrate a reply, but its architecture is still event-workbench oriented rather than conversation-product oriented. `ConsoleView` appends the submitted user text to an in-memory `ChatEventStreamPanel`, creates or reuses a session, creates a run, then polls `listEvents()` every 750 ms. Selecting a session currently records `selectedSessionId` and returns a history path, but the UI does not restore persisted chat bubbles. This is the highest-risk productization gap: if session restore is implemented as a visual list selection without a canonical conversation transcript read model, users will continue to see isolated prompts and blank historical sessions.

The second major risk is confusing “event streaming” with “chat streaming.” The platform already has run events and SSE concepts, but the current Vaadin Console path uses polling and appends rendered events. A Kimi-style UX needs a stable mapping from persisted run events to user/assistant/tool/error/cancel bubbles, and a stream aggregator that updates the same assistant bubble in order, deduplicates replayed events, finalizes terminal states, and survives refresh/reconnect. Vaadin official docs recommend server push plus `UI.access()` for background-thread UI updates; polling can be a safe interim gate, but it should not be called “real streaming.”

The third major risk is treating multi-turn context as a UI concern. `DynamicAgentRuntime` currently calls `new ModelRequest(context, List.of())`, so the model gets no historical messages even when the Console reuses `selectedSessionId`. Spring AI documentation explicitly distinguishes chat history from chat memory: memory is the subset used for model context, while complete history should be stored separately. The project should own a session transcript/context assembler in the App/Domain boundary and feed provider-neutral messages into the runtime, rather than relying on Vaadin component state or Spring AI auto-memory as the product source of truth.

## Pitfalls Table

| # | Pitfall | Severity | Warning Signs in This Codebase | Prevention Strategy | Phase Ownership |
|---|---------|----------|--------------------------------|---------------------|-----------------|
| 1 | Session selection does not restore a transcript | Critical | `ConsoleView.selectSession()` selects the id but does not load messages; `ChatEventStreamPanel.messages` is in-memory only | Build a canonical `ConversationTranscriptQueryService` from persisted runs/events/messages; selecting a session must clear and hydrate bubbles before allowing continuation | Phase 16: Conversation IA + Session Restore |
| 2 | Polling is mistaken for real streaming | Critical | `addAttachListener` sets `UI` poll interval to 750 ms; `EventStreamClient.ConnectionSpec` is returned but not used for live UI updates | Implement Vaadin push or a client-side SSE bridge with replay-before-subscribe semantics; keep polling only as fallback/test seam | Phase 17: Real Streaming UI Pipeline |
| 3 | Delta chunks are appended as events, not aggregated as one assistant answer | Critical | `ChatEventStreamPanel.append()` concatenates assistant text only while `activeAssistantLine` survives; tool/status components can interrupt bubble semantics | Introduce `ChatStreamAggregator` keyed by `sessionId + runId + messageId/stepId`; aggregate only text deltas for the active assistant turn and finalize on finish/status/error/cancel | Phase 17 |
| 4 | Multi-turn context is not actually sent to the model | Critical | `DynamicAgentRuntime.start()` uses `new ModelRequest(context, List.of())`; `CreateRunRequest` carries only current `text` | Add App-layer context assembly: load bounded session turns, preserve roles/tool results, apply token/window policy, then pass into provider-neutral `ModelRequest` | Phase 18: Multi-Turn Context + Runtime Contract |
| 5 | Chat history and model memory are collapsed into one table/model | Critical | Temptation to reuse Spring AI `ChatMemory` as product history | Store complete transcript/audit in project-owned persistence; derive model memory/window from transcript. Do not make Spring AI memory the authoritative history store | Phase 18 |
| 6 | Historical replay duplicates live stream events | High | `appendRunEvents()` has `activeRunNextAfterSequence`, but replay/session switching and new run state are coupled to one active run | Use per-run sequence cursors and idempotent event IDs; restore from durable transcript first, then subscribe with `afterSequence` from last rendered event | Phase 17 |
| 7 | Run event sequence collisions or ordering gaps corrupt delta aggregation | High | `DynamicAgentRuntime.ModelDeltaPublishingSink` starts sequence at `1` independently; other lifecycle publishers may also publish events for same run | Ensure sequence assignment is centralized in `RunEventStore/EventSink`, not per sink; add tests with lifecycle + model.delta interleaving | Phase 17 / Phase 18 |
| 8 | Cancel appears supported but provider stream continues | High | `DynamicAgentRuntime.cancel()` is empty; `start()` returns `SUCCEEDED` after streaming; UI can show cancellable status based on run projection only | Wire cancellation token to streaming client, persist cancel terminal event, stop delta appends after cancel, and display partial answer as cancelled | Phase 17 |
| 9 | Provider/model configuration changes mid-session produce inconsistent conversation behavior | High | Model selector writes directly to `ProviderConfigStore`; active sessions/runs do not visibly pin model/provider used | Persist provider/model ref per run and display it; changing selector affects only subsequent runs and should not rewrite history | Phase 19: Local Profile Persistence + Config UX |
| 10 | Local SQLite/profile persistence becomes a dev-only fork of production persistence | High | v1.2 target mentions SQLite/local stability; prior stack is PostgreSQL/JDBC/Flyway oriented | Use same repository ports and Flyway migrations with a local profile datasource; avoid UI-only JSON/local files for sessions/runs | Phase 19 |
| 11 | Restored session leaks or mixes tenant/user data | High | `DefaultRunQueryService.listEvents()` receives `RequestContext` and `sessionId` but queries `runEventStore.listByRun(runId, ...)` only | Enforce tenant/user/session filters in repositories and tests; never trust only `runId` from UI | Phase 16 / Phase 20: Security Regression |
| 12 | Kimi-style simplification hides necessary run/tool safety feedback | Moderate | Current Console hides advanced panels; chat-first UI may suppress tool/status details entirely | Keep chat primary, but render tool approvals/errors/cancel/provider status as compact inline cards with expandable details | Phase 16 / Phase 17 |
| 13 | Vaadin background updates mutate UI outside the session lock | High | Real streaming will likely arrive from background/provider threads rather than Vaadin request thread | Follow Vaadin push pattern: capture `UI` on attach, use `ui.access(...)`, unregister/detach safely, and avoid holding UI references after detach | Phase 17 |
| 14 | Frequent delta UI updates overload Vaadin/server and mobile browser | Moderate | Token-by-token `setText()` on every delta can create excessive server-client traffic | Buffer/coalesce deltas by time or character threshold; preserve perceived streaming while limiting UI updates | Phase 17 / Phase 20 |
| 15 | Transcript restore is built from rendered text instead of typed domain events | High | `ChatEventStreamPanel.messages()` stores only strings, losing role/run/tool/error/metadata | Introduce typed `ChatTurnDto` / `ConversationMessageDto` with role, status, timestamps, runId, event refs, redaction metadata | Phase 16 |
| 16 | Model finish/error chunks create blank or misleading assistant bubbles | Moderate | `Finished` publishes `ModelDeltaPayload` with empty text; `append()` rejects blank text via `requireText` path depending renderer behavior | Treat finish/error as state transitions, not text; render finish reason/usage separately and never create empty message rows | Phase 17 |
| 17 | Session title and ordering remain based only on first local prompt | Moderate | `sessionTitle(message)` uses current prompt; existing sessions are not loaded from a persisted summary | Persist session title/last message/updated time; support fallback generated from first user message but update recent list after each turn | Phase 16 |
| 18 | Tool-call messages vanish from memory/context | High | Spring AI docs note some chat memory repositories filter tool call messages; project has governed tools/MCP/plugins | Own a provider-neutral context representation that includes tool call and tool result summaries subject to policy/redaction | Phase 18 |
| 19 | Context window grows without token budget or summarization policy | High | Requirement says multi-turn context, but no context selection policy is visible | Start with bounded recent turns plus system/tool summaries; record truncation metadata; defer vector/long-term memory until validated | Phase 18 |
| 20 | Tests assert “text eventually appears” but miss streaming/session semantics | High | Existing mobile E2E patterns may pass with final reply only | Add deterministic fake streaming model with slow deltas, refresh/reconnect, session switch, cancel mid-stream, and continue-history assertions | Phase 20: Product Hardening + Verification |

## Critical Pitfalls

### Pitfall 1: Session history restore is implemented as sidebar selection only

**What goes wrong:**  
Users can click a historical session, but the conversation feed remains empty or shows only newly submitted local messages. Continuing the session creates the illusion of continuity while the model receives only the latest prompt.

**Why it happens:**  
The current Console state is view-local: `ChatEventStreamPanel` stores messages in an in-memory `List<String>`, and `ConsoleView.selectSession()` only updates `selectedSessionId` and the session panel state. The existing run/event APIs expose runtime events, but there is no product-level transcript read model that maps runs/events into stable chat turns.

**Consequences:**
- Historical sessions look broken.
- Multi-turn appears to work in UI labels but not in model behavior.
- Refresh/navigation loses the conversation.
- Roadmap phases later have to rewrite both UI and persistence.

**Prevention:**
1. Create a canonical conversation transcript query boundary before building polished UI: `listSessionTranscript(sessionId, limit/cursor)` returning typed user, assistant, tool, error, and status items.
2. On session selection: clear the feed, hydrate persisted transcript, set selected session, restore last run state if active, then allow continuation.
3. Derive transcript from durable App/Infrastructure data, not `ChatEventStreamPanel.messages()`.
4. Add a test that sends two messages, refreshes/reopens Console, selects the session, and verifies both prior turns are rendered before sending a third.

**Detection:**
- Selecting a session changes only a highlighted row.
- Browser refresh drops all chat bubbles.
- Tests use `selectedSessionId()` but do not assert restored bubble content.

**Phase owner:** Phase 16: Conversation IA + Session Restore.

### Pitfall 2: “Real streaming” is delivered through polling or final replay

**What goes wrong:**  
The UI updates every 750 ms or only after run completion, but the roadmap calls it real streaming. Users see clumped chunks, delayed first token, and poor cancellation feedback.

**Why it happens:**  
`ConsoleView` currently uses `UI.setPollInterval(750)` and `refreshActiveRunEvents()`. The method returns an `EventStreamClient.ConnectionSpec`, but the server-side Vaadin view does not consume SSE for live updates. This can be acceptable as an interim compatibility layer, but it is not Kimi-style token streaming.

**Consequences:**
- First-token latency feels high.
- Cancel/terminal state races with delayed polling.
- Mobile users may submit again before understanding the active run state.
- Product claims do not match behavior.

**Prevention:**
1. Decide one real-time path: Vaadin Push from backend event subscription, or browser-side SSE bridge that calls back to Vaadin/JS component state.
2. Preserve existing replay-before-subscribe semantics: load history up to `afterSequence`, then subscribe for new events.
3. Keep polling only as explicit fallback with `data-stream-mode="polling-fallback"` and tests that distinguish push/SSE mode.
4. Use official Vaadin push rules for server-driven UI changes: capture the `UI`, update inside `UI.access()`, and unregister listeners on detach.

**Detection:**
- Streaming E2E passes even when fake model emits one final event only.
- No `@Push`/push configuration or browser `EventSource` path is exercised.
- Logs show repeated list-events calls instead of a single subscription.

**Phase owner:** Phase 17: Real Streaming UI Pipeline.

### Pitfall 3: Delta aggregation has no stable message identity

**What goes wrong:**  
Model chunks append to the wrong assistant bubble, mix with status/tool components, duplicate after reconnect, or continue appending after error/cancel.

**Why it happens:**  
The current aggregation heuristic is `activeAssistantLine`: any assistant text after the first assistant line is concatenated until a new user message resets it. That is insufficient once multiple runs, replay, tool cards, provider errors, cancel events, or session switching are introduced.

**Consequences:**
- Historical replay can merge separate assistant replies.
- Tool/status cards can split or corrupt a response.
- Reconnect duplicates text.
- Final answer cannot be audited back to event IDs.

**Prevention:**
1. Introduce an explicit stream aggregation key: at minimum `sessionId + runId + stepId`; preferably a model message id if the domain has one.
2. Maintain a rendered-event idempotency set per run/session.
3. Treat `model.delta` as mutable assistant draft until finish/error/cancel terminal event.
4. Treat tool/status/approval events as separate inline cards, not assistant text.
5. Add fake stream tests: deltas `A`, `B`, tool card, delta `C`, finish → one assistant bubble `ABC` plus one tool card.

**Detection:**
- The aggregator has no runId/sessionId/messageId in its state.
- Session switch during active stream changes the current bubble.
- Replaying the same events appends duplicate text.

**Phase owner:** Phase 17.

### Pitfall 4: Multi-turn context is implemented by reusing `sessionId` only

**What goes wrong:**  
Users see a continuous session, but the model still receives only the latest user text. Follow-up questions like “summarize what I just asked” fail.

**Why it happens:**  
`DynamicAgentRuntime.start()` extracts the current input text and calls the model with `new ModelRequest(context, List.of())`. The empty list is the key smell: no prior user/assistant/tool turns are included.

**Consequences:**
- Product fails the core “conversation” expectation.
- Provider behavior appears random or “forgetful.”
- Later memory/RAG work may be blamed for what is actually missing short-term context.

**Prevention:**
1. Add an App-layer `ConversationContextAssembler` that loads recent session transcript and converts it into provider-neutral model messages.
2. Keep complete chat history separate from model memory/window. Spring AI docs explicitly distinguish full chat history from chat memory used for current context.
3. Apply a bounded turn/token policy from day one; record truncation in metadata for debugging.
4. Include tool call/result summaries where needed for agent continuity, with redaction.

**Detection:**
- Model request tests assert only the current prompt.
- Follow-up fake model tests cannot see previous assistant/user content.
- Context size is unbounded or always zero.

**Phase owner:** Phase 18: Multi-Turn Context + Runtime Contract.

### Pitfall 5: Chat history, memory, audit, and UI state are conflated

**What goes wrong:**  
One storage mechanism is asked to serve every purpose: UI restore, audit, model context, and summaries. Tool-call details may disappear, sensitive fields may leak, or memory truncation may delete product history.

**Why it happens:**  
Spring AI provides `ChatMemory`, but its documentation says it is designed for model memory, not complete chat history. It also notes limitations around tool-call message persistence for some repositories/advisors. This platform already has governed tools, audit, redaction, and run events; losing tool context would be a regression.

**Consequences:**
- Audit and replay become incomplete.
- Tool calls vanish from future context.
- UI history changes when memory window policy changes.
- Compliance/security reviews become harder.

**Prevention:**
1. Store full transcript/run/event/audit in project-owned tables/read models.
2. Derive model memory from transcript via a policy: recent N turns, token budget, summaries, redaction.
3. Do not expose raw provider or Spring AI message classes across Domain/App boundaries.
4. Preserve tool calls/results as first-class transcript items or summarized context entries.

**Detection:**
- Changing `maxMessages` changes what the Console history shows.
- Tool results are absent in restored conversations.
- Product history uses in-memory Spring AI repository in local profile.

**Phase owner:** Phase 18.

### Pitfall 6: Local SQLite persistence becomes a separate product path

**What goes wrong:**  
Local development works through SQLite or local config files, but production PostgreSQL behavior differs in schema, ordering, transactionality, or migrations. Bugs appear only after deployment.

**Why it happens:**  
The milestone explicitly includes local profile stabilization. There is a temptation to patch local chat with ad-hoc files or UI state because it is “just local.”

**Consequences:**
- Local verification gives false confidence.
- Session restore and context behavior differ between profiles.
- Schema changes are not migration-controlled.

**Prevention:**
1. Use the same repository ports for local SQLite and production PostgreSQL.
2. Use Flyway for both profiles; do not let Vaadin/provider config write unversioned blobs as the only state.
3. Keep provider config local, but persist run/session/transcript through the normal App/Infrastructure path.
4. Add a local-profile integration gate that restarts the app or repository and proves sessions survive.

**Detection:**
- `local` profile bypasses `RunProjectionRepository` / `RunEventStore`.
- SQLite schema is created imperatively outside migrations.
- E2E tests pass only without restart.

**Phase owner:** Phase 19: Local Profile Persistence + Config UX.

### Pitfall 7: Cancellation is a UI state change, not runtime cancellation

**What goes wrong:**  
The cancel button changes status text, but the provider stream continues to publish deltas or the run is marked succeeded after a cancelled stream.

**Why it happens:**  
`DynamicAgentRuntime.cancel()` is currently empty, while `start()` calls `client.stream(...)` and returns `RunStatus.SUCCEEDED`. Without a real cancellation token path and terminal event semantics, the UI cannot make truthful promises.

**Consequences:**
- Users see text continue after cancel.
- Audit says succeeded when user cancelled.
- Provider resources continue burning.

**Prevention:**
1. Wire run cancellation into `context.cancellationToken()` and the streaming client.
2. Persist a cancel requested event and a terminal cancelled event.
3. Stop aggregation for a run after terminal cancel; show partial assistant bubble with cancelled badge.
4. Add E2E with fake slow stream: send, observe first delta, cancel, assert no later delta appears.

**Detection:**
- Runtime `cancel()` is empty.
- Cancel tests assert button text only.
- Terminal status can be `SUCCEEDED` after cancel.

**Phase owner:** Phase 17.

### Pitfall 8: Repository/query boundaries allow session or tenant mix-ups

**What goes wrong:**  
Historical restore can fetch events for a run that does not belong to the selected session/user/tenant, especially if IDs are guessed or reused.

**Why it happens:**  
`DefaultRunQueryService.listEvents(context, sessionId, runId, ...)` passes only `runId` into `runEventStore.listByRun(runId, ...)`. The method returns `sessionId` from the request, not necessarily from each event. Maybe lower layers validate ownership, but this code does not prove it.

**Consequences:**
- Cross-session or cross-tenant data leak.
- Wrong transcript hydrated into current chat.
- Security regression hidden inside “history restore” work.

**Prevention:**
1. Enforce tenant/user/session/run filtering at repository SQL level.
2. Add negative tests: same run-like id under another tenant/session must not appear.
3. Return session id from persisted projection/event ownership, not merely the request parameter.
4. Keep redaction and sensitive-key filtering active for transcript APIs.

**Detection:**
- Query methods accept context but do not use it.
- Tests run only with one tenant/user/session.
- Transcript restore uses `/sessions/{id}/runs/{runId}` without ownership checks.

**Phase owner:** Phase 16 and Phase 20 security regression.

## Moderate Pitfalls

### Pitfall 9: Kimi-style minimalism hides operational truth

**What goes wrong:**  
The Console becomes visually clean but hides active run status, provider-not-configured state, tool approvals, model errors, and cancel state. Users get a pretty chat box with no actionable diagnostics.

**Prevention:**
- Keep the default view chat-first, but render compact inline cards for provider status, tool approval, tool result, error, cancellation, and model metadata.
- Move deep details behind expanders/drawers; do not remove them.
- Add tests for not-configured provider, provider error, tool approval, and cancel states.

**Phase owner:** Phase 16 / Phase 17.

### Pitfall 10: Provider/model selector mutates global config during active conversations

**What goes wrong:**  
Changing the model selector alters global provider config and may make historical sessions appear as if they used a different model. An active run may continue on old config while UI indicates new config.

**Prevention:**
- Persist provider/model ref per run at creation.
- Make selector affect only next run.
- Display model used on each assistant answer or run detail.
- Disable or warn when changing model during active stream if behavior is ambiguous.

**Phase owner:** Phase 19.

### Pitfall 11: Event renderer throws away details needed by transcript/context

**What goes wrong:**  
The renderer produces components/text for the UI, but future transcript/context code tries to reverse-engineer roles and content from rendered strings.

**Prevention:**
- Add typed conversation DTOs before UI rendering.
- Let `RunEventRenderer` render from DTOs/events, not become the data model.
- Keep role/status/run/tool metadata separate from display text.

**Phase owner:** Phase 16.

### Pitfall 12: Delta update frequency harms mobile performance

**What goes wrong:**  
Every token creates a Vaadin server round-trip/update, causing jank, battery drain, and server load.

**Prevention:**
- Coalesce deltas by 50-150 ms or by character threshold.
- Keep final text exact; streaming perception does not require every token as a separate UI mutation.
- Add a fake long response performance smoke that verifies no runaway component count.

**Phase owner:** Phase 17 / Phase 20.

### Pitfall 13: Empty finish chunks and provider errors render as malformed bubbles

**What goes wrong:**  
Finish chunks have empty text, provider errors are published as text, and UI may render blank bubbles or mix errors into assistant answers.

**Prevention:**
- Model finish/error/cancel as assistant-message state transitions.
- Render provider errors as error cards with safe messages.
- Do not pass blank text through `requireText`-based bubble creation.

**Phase owner:** Phase 17.

### Pitfall 14: Session list recency/title is not driven by durable conversation state

**What goes wrong:**  
Recent sessions show stale titles, wrong order, or only sessions created during the current Vaadin view lifetime.

**Prevention:**
- Add a persisted session summary read model: title, last message preview, updatedAt, active/terminal state, model/provider, unread/active run indicator if needed.
- Update summary after every user message and terminal assistant result.

**Phase owner:** Phase 16.

## Minor Pitfalls

### Pitfall 15: Composer allows duplicate submits during active run

**What goes wrong:**  
Double click or Enter spam creates overlapping runs in one session, confusing context order and stream aggregation.

**Prevention:**
- Disable send or queue intent while a run is active unless explicit parallel runs are supported.
- Include active run guard in App service, not only button state.

**Phase owner:** Phase 17.

### Pitfall 16: Scroll anchoring fights user reading history

**What goes wrong:**  
Auto-scroll jumps to the bottom while the user is reading older restored messages.

**Prevention:**
- Auto-scroll only if user is near bottom or for own submitted message.
- Provide “new output” affordance when user has scrolled up.

**Phase owner:** Phase 16 / Phase 17.

### Pitfall 17: Text-only assertions miss role and grouping regressions

**What goes wrong:**  
E2E sees response text, but user/assistant roles, bubble grouping, status badges, and restored order are broken.

**Prevention:**
- Add stable `data-role`, `data-message-role`, `data-run-id`, `data-session-id`, `data-stream-state` selectors.
- Assert grouping and order, not only visible text.

**Phase owner:** Phase 20.

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|----------------|------------|
| Phase 16: Conversation IA + Session Restore | Building Kimi visual layout before transcript read model | First define typed transcript/session summary APIs, then hydrate Vaadin bubbles from them |
| Phase 16: Historical sessions | Selecting session id without clearing/restoring feed | `selectSession` must clear feed, load transcript, mark active session, restore active run state |
| Phase 16: Security | History API leaks run events by runId only | Add tenant/user/session filters and negative multi-tenant tests |
| Phase 17: Real streaming | Polling/final replay passes as streaming | Gate with fake slow delta model and assertion that assistant bubble changes before run terminal |
| Phase 17: Delta aggregation | Duplicate/reordered chunks after reconnect | Idempotent event IDs, per-run cursor, replay-before-subscribe, aggregation keyed by run/step/message |
| Phase 17: Vaadin integration | Background stream updates outside UI lock | Use Vaadin Push + `UI.access()` or explicit client-side SSE component; detach cleanup required |
| Phase 17: Cancellation | Cancel button only changes local status | Wire cancellation token through runtime/provider and assert no post-cancel deltas |
| Phase 18: Multi-turn context | Reusing session id but passing empty history | Add `ConversationContextAssembler` and provider-neutral message list in `ModelRequest` |
| Phase 18: Memory | Spring AI `ChatMemory` used as product history | Own full transcript; derive bounded memory separately |
| Phase 18: Tool context | Tool calls omitted from context | Include redacted tool call/result summaries in context policy |
| Phase 19: Local persistence | SQLite/local config bypasses production repositories | Same ports/migrations; local datasource only swaps infrastructure implementation |
| Phase 19: Provider config | Model selector changes historical meaning | Pin provider/model per run and show it in UI |
| Phase 20: Verification | Tests only wait for final text | Cover restore, refresh, reconnect, slow streaming, cancel, provider error, context follow-up, and local restart |

## Prevention Checklist for Requirements/Roadmap

- [ ] Define conversation transcript and session summary read models before UI polish.
- [ ] Require session restore after browser refresh/reopen, not only same-view selection.
- [ ] Require a fake slow streaming model that emits multiple deltas and terminal state.
- [ ] Require same assistant bubble to update incrementally, with no duplicate text after replay/reconnect.
- [ ] Require cancellation to stop provider/runtime emission and render partial answer as cancelled.
- [ ] Require multi-turn context proof with fake model inspecting previous turns.
- [ ] Require context window policy and metadata for truncation/summarization.
- [ ] Require tool call/result preservation in transcript and redacted context.
- [ ] Require tenant/user/session ownership tests for history/event APIs.
- [ ] Require local profile persistence to use the same repository ports and migrations as production.
- [ ] Require provider/model used to be pinned per run.
- [ ] Require Vaadin push/SSE implementation tests that are distinguishable from polling fallback.

## What Not To Do

- Do not call the current `UI` polling loop “real streaming.” It can remain a fallback, not the target behavior.
- Do not use `ChatEventStreamPanel.messages()` as durable chat history.
- Do not assume `selectedSessionId` means the model has context.
- Do not make Spring AI `ChatMemory` the only product history store.
- Do not build local SQLite persistence as an ad-hoc dev-only fork.
- Do not aggregate assistant deltas based only on “last assistant DOM line.”
- Do not let cancel be a CSS/status-only state.
- Do not lose governed tool/MCP/plugin context when constructing multi-turn prompts.
- Do not ship history restore without cross-session/tenant negative tests.

## Sources and Confidence

| Source | Finding Used | Confidence |
|--------|--------------|------------|
| Current `ConsoleView.java` | Polling-based event refresh, session selection without transcript restore, run creation request shape, provider selector behavior | HIGH |
| Current `ChatEventStreamPanel.java` | In-memory messages, active assistant line heuristic, composer/cancel UI behavior | HIGH |
| Current `DynamicAgentRuntime.java` | Empty model history list, dynamic provider client/config, local fallback, empty cancel method, model delta publishing | HIGH |
| Current `DefaultRunQueryService.java` | Event history DTO mapping and run-only event query smell | HIGH |
| Vaadin official docs: Server Push / Pushing updates (`https://vaadin.com/docs/latest/building-apps/server-push/updates`) | Background UI updates should use Vaadin push and `UI.access()` patterns; polling is not the only option for live UI | HIGH |
| Vaadin official docs: Server Push configuration (`https://vaadin.com/docs/latest/flow/advanced/server-push`) | Push must be explicitly configured/understood for server-driven UI updates | HIGH |
| Spring AI official docs: Chat Memory (`https://docs.spring.io/spring-ai/reference/api/chat-memory.html`) | LLMs are stateless; chat memory differs from full chat history; conversation id required for advisors; some repositories/advisors have tool-call limitations | HIGH |
