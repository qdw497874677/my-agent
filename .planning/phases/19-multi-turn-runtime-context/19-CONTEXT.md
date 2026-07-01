# Phase 19: Multi-Turn Runtime Context - Context

**Gathered:** 2026-07-01
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 19 makes selected-session continuation real at model execution time: when a user sends the next message in a selected session, the runtime/model path must receive bounded, redacted prior `user` and `assistant` turns from that session, not only the same `sessionId`. This phase owns App/runtime context assembly, budget/truncation policy, model message formatting, safety filtering, and fake-model proof that prior turns are present and the current prompt appears exactly once.

This phase does **not** implement long-term memory, RAG/vector search, prompt libraries, branching/edit/regenerate behavior, provider/model readiness UX, local profile restart persistence, full prompt-stack/system-message policy, or broad Phase 21 regression hardening.

</domain>

<decisions>
## Implementation Decisions

### Context Assembly and Injection Seam
- **D-01:** Prefer filling the existing Domain `SessionContext.messages` carrier with prior turns rather than introducing a separate history channel as the primary Phase 19 seam. Use existing `SessionEntryPayload.MessageEntry(role, content)` semantics unless research finds a blocking issue.
- **D-02:** Implement context assembly as an App-layer assembler/policy/redaction seam, similar in spirit to `ConversationTranscriptAssembler`. It may be called by Infrastructure runtime dispatch, but the business rules for selecting, filtering, budgeting, and reporting context must not live in Vaadin, provider adapters, or raw JDBC code.
- **D-03:** The production path should assemble context during run dispatch before constructing `RunContext`: `DefaultRunDispatcher` or equivalent infrastructure orchestration calls the App context assembler, then builds `SessionContext` with prior messages populated.
- **D-04:** The current user prompt must come only from the current `RunInput` and be appended to provider messages exactly once. Historical context must include only prior turns before the current run/message; do not load the just-created run input from transcript and then duplicate it.

### Budget and Truncation Policy
- **D-05:** Use a conservative configurable budget based on recent turns plus character budget. Default behavior should take the most recent N eligible user/assistant turns and then enforce a max character budget; do not require provider-specific tokenizer logic in Phase 19.
- **D-06:** Truncation keeps the newest eligible history nearest the current prompt and drops older history first. Preserve chronological order among included messages after truncation.
- **D-07:** Context assembly must produce truncation metadata suitable for run/context observability and tests: at minimum included message/turn count, dropped count, excluded count, character budget, resulting character count, and `truncated=true/false`.
- **D-08:** Provide platform defaults plus configuration for recent-turn and character budgets. Avoid per-provider/per-user dynamic context-window policy until Phase 20 or a later provider-policy phase.

### Role Filtering, Redaction, and Sensitive Data Handling
- **D-09:** Only `user` and `assistant` conversation turns are eligible for model context in Phase 19. `tool`, `error`, audit, provider, credential, approval, policy, and runtime diagnostic records must not be sent to the model as context.
- **D-10:** Assistant history should include completed assistant messages and safe partial assistant text when it represents user-visible prior output. Failed/cancelled/error diagnostics must not enter context as assistant prose.
- **D-11:** Add or centralize an App-layer context redactor/filter. It should reuse the conservative sensitive-key discipline from Phase 16, but must not depend on adapter-web `RuntimeDetailRedactor` or Vaadin classes.
- **D-12:** Sensitive, non-visible, redacted, tool/error/provider/audit/credential, or otherwise ineligible transcript items should be excluded from model context and counted in metadata. Do not send `[redacted]` placeholders to the model by default, and do not fail normal runs solely because historical diagnostic/tool items were excluded.

### Model Message Formatting and Provider Boundary
- **D-13:** Provider adapters should send ordered chat messages, not a single concatenated prompt string. Convert `SessionContext.messages` plus the current `RunInput` into a provider-neutral ordered message list, and map it to Spring AI/OpenAI-compatible `UserMessage`/`AssistantMessage` equivalents at the infrastructure/provider boundary.
- **D-14:** Message order is: included historical turns in chronological order, then the current user message last. Truncation may drop older turns, but must not reverse remaining history.
- **D-15:** Update the OpenAI-compatible streaming boundary away from `stream(String prompt, ...)` toward a messages-based API. A short-lived overload/adapter is acceptable during migration, but the planned endpoint should avoid permanent dual semantics where one path silently loses role information.
- **D-16:** Do not introduce new system/developer prompt behavior in Phase 19. This phase only carries selected-session prior `user`/`assistant` turns plus the current user input.

### Verification and Safety Gates
- **D-17:** CTX-04 proof must capture the actual `ModelRequest` and/or provider-neutral message list in fake streaming model/testkit code. Tests must assert prior turns are present, ordered correctly, and the current prompt appears exactly once.
- **D-18:** Add App/repository-level tests proving context assembly uses ownership-safe query boundaries and cannot load cross-tenant, cross-user, cross-session, or cross-run history.
- **D-19:** Add safety tests for role filtering, sensitive key/value filtering, non-visible/redacted message exclusion, and excluded/redacted/truncated metadata counts.
- **D-20:** Add or extend ArchUnit rules so Phase 19 context assembler/redactor/policy classes remain in App/Domain-safe packages and do not depend on Vaadin, Spring AI, provider SDKs, adapter-web, infrastructure, or persistence implementations. Context assembly must not be implemented in Vaadin component state.

### Folded Todos
- No pending todos matched Phase 19 scope.

### the agent's Discretion
- Exact class names (`ConversationContextAssembler`, `RuntimeContextPolicy`, `ModelContextRedactor`, etc.) are planner discretion if the semantics above are preserved.
- Exact default values for recent-turn limit and character budget are implementation/research discretion, provided they are conservative, configurable, and covered by tests.
- Exact metadata storage path is planner discretion, provided it is observable/testable and does not leak sensitive data.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 19 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 19 — Phase goal, dependency on Phase 16/18, CTX-01 through CTX-05 mapping, success criteria, and non-UI focus.
- `.planning/REQUIREMENTS.md` §Multi-Turn Runtime Context — CTX-01 through CTX-05 define bounded prior turns, configurable budget, sensitive-data exclusion/redaction, fake-model proof, and App/runtime seam ownership.
- `.planning/REQUIREMENTS.md` §Out of Scope — vector DB/RAG, prompt templates, branching/edit/regenerate, automatic fallback, React rewrite, and localStorage history remain out of scope.
- `.planning/PROJECT.md` §Current Milestone: v1.2 Console 对话产品化 — Multi-turn context is one of the remaining v1.2 productization gaps.
- `.planning/STATE.md` §Accumulated Context — Prior v1.2 decisions around typed read models, selected-session continuation, streaming reducer, redaction, and stable boundaries.

### Prior Phase Decisions That Must Be Carried Forward
- `.planning/phases/16-conversation-read-model-and-recent-sessions/16-CONTEXT.md` — D-01 through D-08 define the Conversation read-model boundary, typed transcript shape, roles/status/metadata, and Phase 19 context assembly needs; D-13 through D-16 prohibit raw maps as the chat transcript contract and require ownership filters.
- `.planning/phases/17-console-session-restore-ux/17-CONTEXT.md` — D-09 through D-17 define selected-session continuation, typed transcript hydration, and explicitly defer real model-context assembly to Phase 19.
- `.planning/phases/18-streaming-bubble-lifecycle/18-CONTEXT.md` — D-04 through D-12 define assistant bubble identity, runtime/tool/provider detail separation, cancellation/failure safety, and explicitly defer multi-turn runtime context to Phase 19.
- `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-CONTEXT.md` — Runtime/tool/approval diagnostics remain secondary, compact, expandable, and redacted; these diagnostics must not become model-context prose.
- `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-CONTEXT.md` — Redaction, tenant/user boundaries, and observability/correlation discipline.
- `.planning/phases/04-governed-tool-registry-workspace-and-invocation-pipeline/04-CONTEXT.md` — Tool payload, policy, audit, approval, and credential safety constraints; tool/audit details are not eligible Phase 19 model context.

### v1.2 Research Inputs
- `.planning/research/ARCHITECTURE.md` — Conversation read model, ConsoleView/AppConsoleRunExecutionBridge seams, multi-turn context sequencing, and anti-patterns around session-id-as-context and Vaadin-as-history.
- `.planning/research/PITFALLS.md` — Especially pitfalls about session-id-only continuation, ownership leakage, typed transcript reliance, sensitive diagnostic leakage, and semantic tests.
- `.planning/research/SUMMARY.md` — v1.2 phase sequencing rationale and handoffs from read model → restore UX → streaming → context.

### Existing Code Contracts to Inspect
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` — Current dispatch seam; builds `RunContext` and currently constructs empty `SessionContext`.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionContext.java` — Existing session context carrier with `messages` field.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionEntryPayload.java` — `MessageEntry(role, content)` payload suitable for prior turns.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunContext.java` — Runtime context object passed into agent runtime.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelRequest.java` — Model request boundary currently carrying `RunContext` and tool results.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationQueryService.java` — App-layer typed transcript query boundary.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultConversationQueryService.java` — Ownership-safe transcript orchestration and repository use.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationTranscriptAssembler.java` — Pure typed transcript assembly and existing App-level redaction/filtering discipline.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageDto.java` — Typed transcript message fields, role/status/visibility/redaction metadata.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/ConversationMessageRole.java` and `ConversationMessageStatus.java` — Role/status vocabulary for context eligibility.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java` — Current provider adapter extracts a single prompt string.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java` — Current single-string stream interface to migrate toward messages.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java` — Current Spring AI prompt creation path using one `UserMessage`.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java` and `FakeModelClient.java` — Need request/message capture for CTX-04 proof.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` — Test runtime path that must preserve context for streaming and non-streaming branches.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java` and `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java` — Architecture gates to extend for context assembly boundaries.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ConversationMessageDto`, `ConversationMessageRole`, `ConversationMessageStatus`, and `ConversationTranscriptResponse`: typed prior-turn source created in Phase 16; includes visibility/redaction/status metadata needed for filtering.
- `ConversationQueryService` / `DefaultConversationQueryService`: App-layer ownership-safe transcript access that already checks `RequestContext`/session ownership before loading runs/events.
- `ConversationTranscriptAssembler`: pure App-level assembler and redaction pattern; useful model for a Phase 19 context assembler.
- `SessionContext.messages` and `SessionEntryPayload.MessageEntry`: existing Domain carrier for prior session messages; currently unused/empty in dispatch.
- `RunContext` and `ModelRequest`: current runtime/model boundaries that can carry populated `SessionContext` through to provider adapters.
- `FakeStreamingModelClient`, `FakeModelClient`, `GeneralAgentLoop`, `DeterministicClock`, `DeterministicIds`, and fake repositories: useful for no-key deterministic context assembly and model-request capture tests.

### Established Patterns
- COLA boundaries are enforced by ArchUnit. Domain stays JDK-only and free of Spring/Jackson/Jakarta/Vaadin/PF4J/Spring AI/JDBC/App/Infra/Adapter dependencies; App depends on client/domain and stays free of outer layers.
- Conversation transcript data must come from typed App/client read models and ownership-filtered repositories, not Vaadin component state, raw event maps, or browser storage.
- Runtime/tool/provider/audit details remain diagnostics; user-facing assistant prose and model context must not absorb raw operational payloads.
- Existing Console streaming reducer work is adapter-web UI state only; Phase 19 should not use `ChatEventStreamPanel` state as model context source.
- Tests prefer deterministic no-key fake model/runtime fixtures and semantic assertions over real provider calls.

### Integration Points
- `DefaultRunDispatcher` is the main production injection point: it has queued run identity, request context data, repositories/event store access, and currently constructs empty `SessionContext`.
- App-layer context assembler should load/consume prior transcript with the same ownership boundaries as `DefaultConversationQueryService`, produce eligible `MessageEntry` history plus metadata, and hand it to dispatch.
- `OpenAiCompatibleStreamingModelClient` should convert `ModelRequest.context().sessionContext().messages()` plus current input into provider message list before calling the OpenAI/Spring AI source.
- `OpenAiStreamSource` / `OpenAiSpringAiModelFactory` must move from single prompt to message-list semantics to preserve user/assistant roles.
- Testkit fake model clients need request capture APIs so tests can inspect exact history/current prompt payloads without calling a real provider.

</code_context>

<specifics>
## Specific Ideas

- User selected the recommended path for all discussed Phase 19 areas.
- The main semantic anchor is: selected-session continuation must be model-visible, bounded, and safe; `sessionId` reuse alone is insufficient.
- The preferred low-churn implementation direction is: App context assembler → dispatch-time `SessionContext.messages` population → model client converts messages + current input into ordered provider chat messages.
- Keep Phase 19 focused on current-session bounded history, not memory/compaction/summarization/prompt-stack design.

</specifics>

<deferred>
## Deferred Ideas

- Long-term memory, RAG/vector search, summarization/compaction controls, and advanced context management UI — future phases.
- Provider/model-specific context-window budgeting and per-run provider/model display/persistence — Phase 20 or later provider-policy work.
- Broad end-to-end milestone regression/security matrix beyond Phase 19 targeted gates — Phase 21.
- Conversation search, rename/archive/pin/delete, branching, editing, regeneration, import/export, prompt templates, and automatic paid-provider fallback — future/out of scope.

</deferred>

---

*Phase: 19-multi-turn-runtime-context*
*Context gathered: 2026-07-01*
