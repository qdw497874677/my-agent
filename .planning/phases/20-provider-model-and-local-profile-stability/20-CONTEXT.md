# Phase 20: Provider/Model and Local Profile Stability - Context

**Gathered:** 2026-07-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 20 productizes the local development and real-provider conversation loop. Users must be able to understand provider/model readiness from the Console model area, refresh model choices with visible success/empty/error feedback, persist model selection for subsequent runs, see clear local fallback labeling when fallback behavior occurs, and recover sessions/transcripts/run metadata/provider configuration from the SQLite local profile after restart.

This phase does **not** add automatic paid-provider fallback policy, multi-provider routing, provider-specific context-window policy, conversation search/rename/archive/pin/delete, long-term memory/RAG, React/Next.js, mobile-only APIs, or the full Phase 21 release regression matrix. Phase 21 owns broad verification/security hardening; Phase 20 must still add targeted proof for provider/model/local profile stability.

</domain>

<decisions>
## Implementation Decisions

### Console Provider/Model Feedback
- **D-01:** Use a compact status row/bar inside the existing Console model area for provider/model readiness, selected model, refresh result, and actionable provider errors. Do not introduce a large always-visible operational panel that breaks the Kimi-style chat-first Console.
- **D-02:** Model refresh must explicitly distinguish success, empty, and error states. Success should indicate the number/list of refreshed models, empty should explain that the provider returned no choices or configuration is incomplete, and error should show a safe redacted summary. Silent catches like the current `catch (Exception ignored)` behavior are not acceptable.
- **D-03:** The model area should preserve stable selectors such as `data-role="model-selector"`, `data-role="provider-status"`, and `data-action="refresh-models"`, adding more `data-*` hooks as needed for refresh state/readiness/fallback assertions.
- **D-04:** Provider configuration details and diagnostics may remain compact/collapsed or linked to an existing configuration/admin surface, but the Console model area must tell users what action to take when the provider is not ready.

### Model Selection and Run Metadata
- **D-05:** Selecting a model in the Console persists immediately to the local profile/provider config, but applies only to subsequent runs. It must not mutate an active run or active stream. The UI should clearly communicate “applies to next run” or equivalent when a selection changes during an active conversation.
- **D-06:** New run creation should resolve the selected provider/model at run creation/dispatch time through existing provider/model boundaries rather than hard-coding a stale constructor default. The selected model should influence the next run’s `modelRef`/agent definition path without leaking provider SDK types into App/Domain contracts.
- **D-07:** Each run must record the actual provider/model/fallback facts used for history and debugging: at minimum requested model ref or selected model, resolved provider id, resolved model id, fallback mode, readiness state, and safe error summary when resolution/provider readiness failed. Raw API keys, full provider config snapshots, or secret-bearing payloads must not be stored in run metadata.
- **D-08:** Run metadata should be available to history/debugging surfaces but remain secondary in the main chat. Provider/model metadata is useful for troubleshooting and historical traceability, not a default recent-session card field unless later phases decide otherwise.

### Local Fallback and No-Key Semantics
- **D-09:** When no provider/key is configured, Console sending should be blocked by default rather than silently producing a local fake answer. This makes local development trustworthy and prevents users from mistaking a no-key demo response for a real model answer.
- **D-10:** If local fallback mode is deliberately enabled by configuration/test profile or occurs through an explicit fallback path, it must be clearly labeled as local fallback / not a real model answer in both the model area and the resulting assistant bubble or bubble metadata.
- **D-11:** Fallback labeling should survive transcript/history restore through persisted run metadata so that a restarted Console does not lose the fact that an answer came from fallback behavior.
- **D-12:** Do not implement automatic paid-provider fallback or cross-provider routing in Phase 20. Fallback is either blocked no-key behavior or an explicitly labeled local/dev fallback mode.

### SQLite Local Profile Persistence
- **D-13:** Phase 20 should harden the existing local profile/SQLite implementation rather than introducing a broad new App-level LocalProfile abstraction. Use existing `SqliteLocalPersistence`, local dev repository/store seams, and provider config store as the starting point, while keeping COLA boundaries intact.
- **D-14:** SQLite local profile must persist and restore sessions, typed transcript data/events needed by the conversation read model, run metadata including provider/model/fallback facts, provider config, and selected model across application/store restart.
- **D-15:** Restart proof should exercise an end-to-end local profile recovery loop: write provider config/model selection, create or persist session/run/events/run metadata, recreate the local persistence/store/config objects against the same SQLite DB, then verify recent sessions, transcript, run metadata, provider config, and selected model are recovered.
- **D-16:** SQLite queries and local-profile restore paths must preserve the ownership filtering discipline from Phase 16/19. Do not regress into global `loadSessions()`/`loadEvents()` behavior for product read-model paths when tenant/user/session/run filters are required.
- **D-17:** SQLite schema changes should be additive/migratable for existing local DB files, following the current `addColumnIfMissing(...)` pattern or a similarly safe local-profile migration approach.

### Folded Todos
- No pending todos matched Phase 20 scope.

### the agent's Discretion
- Exact class and DTO names for readiness/refresh/fallback status are planner discretion, provided App/Domain boundaries stay provider-neutral and secret-free.
- Exact wording, icons, chip colors, and compact model-bar layout are implementation discretion, provided readiness, refresh states, blocked send, and fallback labels are visible and testable.
- Exact storage column names for run provider/model/fallback metadata are planner discretion, provided they are persisted, redacted, restored, and queryable for history/debugging.
- Exact test layering is planner discretion, but Phase 20 should include targeted Java/local-profile proof and enough Console/component/browser selector coverage to hand a reliable surface to Phase 21.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 20 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 20 — Phase goal, dependencies on Phase 16/17, SESS-05 and PROV-01 through PROV-06 mapping, success criteria, and UI hint.
- `.planning/REQUIREMENTS.md` §Provider, Model, and Local Profile UX — PROV-01 through PROV-06 define readiness, refresh states, persisted selection, per-run provider/model/fallback recording, fallback labeling, and SQLite local profile persistence.
- `.planning/REQUIREMENTS.md` §Recent Sessions and Transcript Restore — SESS-05 requires Console refresh/reopen/restart recovery of persisted sessions and transcript in local profile.
- `.planning/REQUIREMENTS.md` §Out of Scope — browser localStorage as history source of truth, automatic paid-provider fallback, React rewrite, vector/RAG, and branching/edit/regenerate remain out of scope.
- `.planning/PROJECT.md` §Current Milestone: v1.2 Console 对话产品化 — Local provider/model/SQLite stability is one of the remaining v1.2 productization gaps.
- `.planning/STATE.md` §Accumulated Context — Prior v1.2 decisions around typed conversation read models, selected-session continuation, streaming reducer, App-layer context assembly, redaction, and stable Vaadin selectors.

### Prior Phase Decisions That Must Be Carried Forward
- `.planning/phases/16-conversation-read-model-and-recent-sessions/16-CONTEXT.md` — Typed conversation read model, session-centric REST shape, projection-table-first transcript strategy, ownership filters, and limited SQLite alignment before Phase 20.
- `.planning/phases/17-console-session-restore-ux/17-CONTEXT.md` — Kimi-style compact Console, recent session cards do not default to provider/model metadata, active-session continuation, typed transcript hydration, and provider/model/local profile stability deferred to Phase 20.
- `.planning/phases/18-streaming-bubble-lifecycle/18-CONTEXT.md` — One live assistant bubble, runtime/provider diagnostics secondary, cancellation/failure safety, and provider/model/local profile stability deferred to Phase 20.
- `.planning/phases/19-multi-turn-runtime-context/19-CONTEXT.md` — App/runtime owns context assembly; provider-specific details stay at provider boundary; provider/model-specific policy and per-run display/persistence are deferred to Phase 20 or later provider-policy work.
- `.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/03-CONTEXT.md` — Provider registry, explicit `provider:model` refs, provider-neutral streaming chunks/events, secret redaction, and OpenAI-compatible provider boundary.
- `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-CONTEXT.md` — Trace/correlation, tenant/user safety, and redaction discipline for diagnostics/metadata.

### v1.2 Research Inputs
- `.planning/research/ARCHITECTURE.md` — v1.2 architecture notes around Console/provider/local stability, anti-patterns against Vaadin-as-history and localStorage, and recommended sequencing.
- `.planning/research/PITFALLS.md` — Pitfalls around provider errors, local fallback confusion, typed transcript reliance, ownership leakage, and semantic tests.
- `.planning/research/SUMMARY.md` — v1.2 sequencing rationale and handoff into provider/local stability before final verification.

### Existing Code Contracts to Inspect
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` — Existing model bar, model selector, refresh button, provider status, selected-session send path, stable selectors, and current silent refresh catch.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfig.java` — Current provider config shape, readiness rule, defaults, and masked response behavior.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigStore.java` — Current immediate update/versioning and SQLite-backed provider config persistence.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigController.java` — Provider config and model-list API surface used by Console refresh.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java` — Local SQLite schema and persistence methods for sessions, runs, events, and provider config; add run metadata/fallback persistence here if continuing existing local-profile path.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java` — Local dev store/repository seams and current SQLite mirroring for conversation read-model queries.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ModelProviderQueryService.java` and `DefaultModelProviderQueryService.java` — Existing App provider catalog/resolve seam.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/ModelProviderRegistry.java` — Provider registry port for model resolution.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderModelRef.java`, `ProviderDescriptor.java`, `ModelDescriptor.java`, and `ModelProviderResolution.java` — Provider/model neutral Domain contracts.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` — Current run dispatch agent definition/modelRef path and audit details; likely integration point for resolved model/run metadata.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/DynamicAgentRuntime.java` — Runtime-to-event mapping for model chunks and provider/model event payloads.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java` and `OpenAiSpringAiModelFactory.java` — Actual OpenAI-compatible model client path and provider error mapping.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapper.java` — Existing safe provider error summary mapping.
- `pi-agent-adapter-web/src/main/resources/messages.properties` and `messages_zh.properties` — Console model selector/readiness/fallback/error i18n labels to update.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalConversationReadModelPersistenceTest.java` — Existing SQLite/local conversation persistence proof baseline.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ModelProviderWiringIntegrationTest.java`, `OptionalRealProviderSmokeTest.java`, and `FakeOpenAiProviderE2EConfiguration.java` — Provider wiring and fake/optional provider test patterns.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ProviderConfig` / `ProviderConfigStore`: existing local provider config model and persistence store; already masks API keys and can report `isReady()`.
- `SqliteLocalPersistence`: existing SQLite table owner for `local_sessions`, `local_runs`, `local_events`, and `local_provider_config`; supports additive schema changes with `addColumnIfMissing(...)`.
- `ConsoleView#createModelBar()`: existing Vaadin model selector, refresh button, provider status span, and stable selectors; current refresh success/error behavior is incomplete and should be hardened.
- `ModelProviderQueryService` / `ModelProviderRegistry`: existing provider catalog and resolve seam for keeping provider/model decisions out of Vaadin-specific state.
- `ProviderModelRef`, `ProviderDescriptor`, `ModelDescriptor`, `ModelProviderResolution`: Domain provider/model vocabulary for `provider:model` refs and neutral resolution.
- `OpenAiProviderErrorMapper`: existing safe provider error mapping to reuse for actionable but redacted Console errors.
- `ConversationQueryService`, `ConversationTranscriptResponse`, `SessionSummaryDto`, and local read-model repository seams: existing typed transcript/recent-session read model that SQLite restart proof should exercise.

### Established Patterns
- Adapter Web owns Vaadin presentation and stable selectors; App owns provider/model query semantics; Domain remains provider SDK/UI/persistence neutral.
- Prior phases prefer additive changes over endpoint churn, frontend rewrite, or mobile-only APIs.
- Conversation history must come from typed read models and SQLite/JDBC-backed repository data, not Vaadin memory or browser localStorage.
- Runtime/provider diagnostics and metadata are secondary/debugging information; user-facing chat remains clean, with important error/fallback signals visible but compact.
- Secrets must be masked/redacted before API responses, logs, metadata, UI, or persisted debugging fields.

### Integration Points
- Extend `ConsoleView` model bar to render readiness, refresh state, empty/error summaries, selected model persistence, blocked send state, and fallback hooks with stable selectors.
- Route model refresh through `ProviderConfigController`/provider registry with explicit success/empty/error outcomes instead of silent catches.
- Ensure model selection updates `ProviderConfigStore`/SQLite immediately but is consumed only by subsequent run creation/dispatch.
- Extend run creation/dispatch/projection/local persistence to record safe provider/model/fallback metadata for later history/debugging and restored fallback labels.
- Add SQLite columns/methods for provider/model/fallback run metadata and verify re-instantiation against the same DB restores config, selection, sessions, transcript data, and metadata.
- Keep no-key send blocking and explicit fallback labeling in adapter-web/product paths while preserving deterministic fake/local test profiles where fallback is intentionally enabled.

</code_context>

<specifics>
## Specific Ideas

- The model area should stay compact and chat-first, not become an operations dashboard.
- Refreshing models must never silently fail; users should see success, empty, or redacted error feedback.
- Model selection is a local preference/config update for future runs only; active streaming/running answers are immutable with respect to later selector changes.
- No provider/key should block sending by default. If fallback is explicitly enabled for local/dev/test, it must be visibly marked in both model area and assistant response metadata/bubble.
- Phase 20 should harden the existing SQLite local profile rather than pause for a broad new abstraction, but restart behavior must be proven end-to-end with the same DB file.

</specifics>

<deferred>
## Deferred Ideas

- Automatic paid-provider fallback policy, multi-provider routing, and provider priority rules — future provider-policy phase.
- Provider-specific context-window budgeting and model-specific context policy — future provider/context policy work.
- Conversation search, rename/archive/pin/delete, branching, editing, regeneration, import/export, prompt templates, long-term memory, RAG, and vector DB — future/out of scope.
- Broad milestone release matrix for provider errors, ownership leakage, architecture, fake slow streams, and complete browser regression — Phase 21, though Phase 20 should add targeted local/provider proof.
- Replacing the existing local profile with a broad formal App-level LocalProfile abstraction — defer unless Phase 20 research finds a blocking boundary issue.

</deferred>

---

*Phase: 20-provider-model-and-local-profile-stability*
*Context gathered: 2026-07-04*
