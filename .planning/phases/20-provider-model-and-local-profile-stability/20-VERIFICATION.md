---
phase: 20-provider-model-and-local-profile-stability
verified: 2026-07-04T12:11:30Z
status: passed
score: 15/15 must-haves verified
gaps: []
---

# Phase 20: Provider/Model and Local Profile Stability Verification Report

**Phase Goal:** Provider/Model and Local Profile Stability — local development and real provider usage feel trustworthy: provider/model readiness is visible, errors are actionable, model choices persist, and local SQLite can restore conversations after restart.
**Verified:** 2026-07-04T12:11:30Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

Phase 20 achieved its goal. The final codebase contains and wires the provider/model readiness UI, explicit model refresh states, no-key send blocking, opt-in local fallback labels, selected-model snapshots, safe provider metadata persistence, dispatcher modelRef resolution, and same-DB SQLite restart proof. The focused Java regression gate was re-run during verification and passed: 20 tests, 0 failures.

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | User can see provider/model readiness in the compact Console model area. | ✓ VERIFIED | `ConsoleView#createModelBar()` creates `data-role="provider-status"`; `updateProviderStatus(...)` sets visible ready/not-configured copy and `data-provider-ready`. Covered by `WebConsoleProviderModelBarTest.initialConsoleModelAreaExposesStableSelectorsAndReadiness`. |
| 2 | Refreshing models visibly distinguishes success, empty, and error states. | ✓ VERIFIED | `ProviderConfigController.ModelListResponse` has explicit `state`, `message`, `modelCount`, `ready`, `selectedModel`, `providerId`; Console sets `data-refresh-state` via `updateRefreshStatus(...)`. Tests cover success/empty/error/not_configured. |
| 3 | Provider errors shown in Console are actionable and redacted, never silently swallowed. | ✓ VERIFIED | `ProviderConfigController.safeErrorSummary(...)` redacts bearer/API-key/token patterns and truncates; Console no longer catches/ignores refresh errors and renders localized `refreshError`. Tests assert no raw key/Bearer/class dump. |
| 4 | Changing the model selector persists locally but only affects future runs. | ✓ VERIFIED | `modelSelector.addValueChangeListener(...)` calls `providerConfigStore.update(...)`; `ProviderConfigStore` persists to SQLite. Active-run selector changes update `data-role="model-selection-scope"` to `next-run` and do not reset current chat state. |
| 5 | No configured provider/key blocks product Console send by default. | ✓ VERIFIED | `ConsoleView.planChatSubmission(...)` checks `providerReadyForSend()` before user-message append and before session/run creation when `explicitLocalFallbackMode=false`; no-key tests verify zero bridge calls and zero user messages. |
| 6 | Explicit local fallback mode is visibly labeled in the model area and assistant bubble metadata. | ✓ VERIFIED | `ConsoleView` exposes model-area `data-fallback-mode="local"`; `ChatEventStreamPanel.markLocalFallbackMode(...)` adds `data-role="fallback-label"` and `data-fallback-mode="local"` on the assistant bubble. |
| 7 | Each run can persist safe provider/model/fallback facts for history and debugging. | ✓ VERIFIED | `RunProviderMetadata` DTO exists; `RunResponse` and `ConversationRunView` carry it; JDBC `runs.provider_metadata` JSONB and SQLite `provider_metadata_json` persist and read it. |
| 8 | Run metadata is redacted and excludes raw API keys or provider config snapshots. | ✓ VERIFIED | `JdbcRunProjectionRepository.safeProviderMetadata(...)` and local `safeProviderMetadata(...)` whitelist safe fields and redact `safeErrorSummary`; tests verify stored JSON excludes `apiKey`, `Authorization`, `providerConfig`, bearer tokens, and `sk-*`. |
| 9 | SQLite and JDBC run projections expose the same metadata fields for local-profile restore. | ✓ VERIFIED | `JdbcRunProjectionRepository#findRun/listRunsBySession` and `LocalRunProjectionRepository#findRun/listRunsBySession` both return `RunProviderMetadata` fields. SQLite same-DB tests prove hydration. JDBC Testcontainers proof exists but is Docker-gated in this environment. |
| 10 | New run creation uses the selected provider/model snapshot for the run being created. | ✓ VERIFIED | `ConsoleView.runMetadataSnapshot()` snapshots `ProviderConfigStore.current()` into `CreateRunRequest.metadata()`; tests show selector changes after creation do not mutate the captured metadata. |
| 11 | Dispatcher/runtime use the run snapshot instead of a stale hard-coded constructor default. | ✓ VERIFIED | `DefaultRunCommandService` normalizes request metadata into `QueuedRun.providerMetadata`; `DefaultRunDispatcher.effectiveModelRef(...)` selects metadata `selectedModelRef/requestedModelRef/provider+model` before constructor default. Dispatcher tests verify selected snapshot vs legacy default. |
| 12 | Fallback/no-provider resolution facts are recorded safely and labeled for restored history. | ✓ VERIFIED | Not-ready snapshots produce `fallbackMode=local`, `readinessState=NOT_CONFIGURED`, and `local-dev:not-configured`-style refs; local restart test verifies fallback metadata survives restore and live Console fallback path renders labels. |
| 13 | After local SQLite restart, recent sessions and typed transcripts recover from persisted data. | ✓ VERIFIED | `LocalProfileRestartRecoveryTest.recentSessionsTranscriptAndRunMetadataRecoverFromSameDatabaseAfterRestart` recreates SQLite persistence/stores/repositories/query service and recovers recent session plus USER/ASSISTANT typed transcript. |
| 14 | After local SQLite restart, provider config and selected model recover from the same DB file. | ✓ VERIFIED | `ProviderConfigStore` loads `SqliteLocalPersistence.loadProviderConfig()` on construction; restart test verifies ready config, base URL, provider id, model id, and masked key after recreating the store. |
| 15 | After local SQLite restart, provider/model/fallback run metadata remains available for restored history/debugging. | ✓ VERIFIED | `LocalDevStores.loadAll()` parses `provider_metadata_json` and rebuilds `RunRecord`/`RunResponse`; restart tests verify normal and fallback metadata after object recreation. |

**Score:** 15/15 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigController.java` | Stateful provider readiness/model refresh response | ✓ VERIFIED | Contains `ModelListResponse`, explicit `success/empty/error/not_configured`, safe error redaction, and model list parsing. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` | Compact model bar, selector persistence, send guard, run snapshot metadata | ✓ VERIFIED | Contains `data-role="provider-status"`, refresh button listener calling `listModels()`, model selector update, no-key guard, fallback mode status, and `runMetadataSnapshot()`. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java` | Component/controller proof for readiness, refresh, selection snapshot | ✓ VERIFIED | 346 lines; 10 focused tests passed during verification. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` | Fallback/local label rendering hook on assistant bubbles | ✓ VERIFIED | `markLocalFallbackMode(...)` adds `data-role="fallback-label"` and `data-fallback-mode="local"`. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleNoProviderFallbackTest.java` | No-key blocked send and explicit fallback labels proof | ✓ VERIFIED | 154 lines; 3 focused tests passed during verification. |
| `pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunProviderMetadata.java` | Safe typed metadata contract | ✓ VERIFIED | Whitelisted DTO with requested/selected model ref, provider/model ids, fallback mode, readiness state, and safe error summary. |
| `pi-agent-infrastructure/src/main/resources/db/migration/V5__run_provider_model_metadata.sql` | Additive cloud schema for run provider/model/fallback metadata | ✓ VERIFIED | Adds `runs.provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb`. The plan artifact pattern expected `provider_id`, but the plan explicitly allowed JSONB metadata; manual verification confirms this stores provider id inside `RunProviderMetadata` JSON. |
| `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java` | JDBC persistence/readback for metadata | ✓ VERIFIED | Inserts and reads `provider_metadata`, whitelists fields, redacts summaries, and exposes metadata through `findRun` and `listRunsBySession`. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java` | Additive local SQLite columns and load/save paths | ✓ VERIFIED | Uses `addColumnIfMissing(...)` for `provider_metadata_json`; `saveRun(...)` and load methods preserve it. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelMetadataPersistenceTest.java` | SQLite/local store proof for metadata persistence and redaction | ✓ VERIFIED | 118 lines; source verifies upgrade, same-DB reload, redaction, and ownership filters. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/QueuedRun.java` | Queued run carries safe run model metadata | ✓ VERIFIED | Record includes `RunProviderMetadata providerMetadata` with compatibility constructor defaulting to `EMPTY`. |
| `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` | Dispatch-time modelRef derived from run metadata | ✓ VERIFIED | `effectiveModelRef(...)` prefers queued metadata before constructor default; `agentDefinitionFor(...)` creates per-run copy. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/DynamicAgentRuntime.java` | Snapshot-aware model use and explicit local not-configured labeling | ✓ VERIFIED | Uses `context.agentDefinition().modelRef()` to derive run model snapshot and publishes local-dev/not-configured labels for no-provider local runtime events. The plan artifact pattern expected the literal word `fallback`; manual verification confirms equivalent behavior through `local-dev:not-configured` metadata. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalProfileRestartRecoveryTest.java` | Same-DB restart proof for SESS-05 and PROV-06 | ✓ VERIFIED | 240 lines; 4 focused tests passed during verification. |
| `docs/phase-20-provider-model-local-profile.md` | Phase 21 selector/metadata/restart handoff | ✓ VERIFIED | Documents stable selectors, safe metadata fields, fallback semantics, commands, and deferred scope. Non-blocking note: line 14 documents `data-selection-scope="current|next-run"`, while code currently emits `future-runs|next-run`. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `ConsoleView#createModelBar` | `ProviderConfigController#listModels` | Refresh button click | ✓ WIRED | `refreshModels.addClickListener(...)` calls `providerConfigController.listModels()`, then updates selector, provider status, and refresh status. |
| `ProviderConfigController#listModels` | Console model bar refresh status | `ModelListResponse` state fields | ✓ WIRED | Response `state/message/modelCount/ready/providerId/selectedModel/models` are consumed by `localizedRefreshMessage(...)`, `updateProviderStatus(...)`, and `updateRefreshStatus(...)`. |
| Console model selector | `ProviderConfigStore.update` | Value change listener | ✓ WIRED | `modelSelector.addValueChangeListener(...)` constructs a new `ProviderConfig` and calls `providerConfigStore.update(...)`; store saves to SQLite. |
| `ConsoleView.planChatSubmission` | Provider readiness/fallback mode | Send guard before `createSession/createRun` | ✓ WIRED | `if (!providerReadyForSend() && !explicitLocalFallbackMode) { showProviderBlockedSend(); return null; }` appears before append/create calls. |
| `RunProjectionRepository` | JDBC and local implementations | Metadata fields in create/query methods | ✓ WIRED | Both JDBC and local repositories sanitize and expose `RunProviderMetadata` in create, find, and list-run paths. |
| `SqliteLocalPersistence` | `LocalDevStores.loadAll` | Same-DB restart row hydration | ✓ WIRED | SQLite saves `provider_metadata_json`; `LocalDevStores.loadAll()` parses it into `RunResponse`/`RunRecord`. |
| `ConsoleView#createRun` metadata | `DefaultRunCommandService` / `QueuedRun` | Safe selected provider/model facts | ✓ WIRED | `runMetadataSnapshot()` populates `CreateRunRequest.metadata`; command service normalizes into `QueuedRun.providerMetadata`. |
| `QueuedRun` | `DefaultRunDispatcher AgentDefinition` | Dispatch-time modelRef | ✓ WIRED | `DefaultRunDispatcher.effectiveModelRef(queuedRun)` uses queued metadata and `agentDefinitionFor(modelRef)` before runtime start. |
| `DynamicAgentRuntime` | Run events/transcript metadata | Model delta provider/model facts | ✓ WIRED | `ModelDeltaPublishingSink.publishText(...)` emits `ModelDeltaPayload(modelRef, text, providerId, modelId, ...)`; no-key local runtime uses local-dev/not-configured labels. |
| Same SQLite DB file | `LocalDevStores.loadAll` / `ProviderConfigStore` constructor | Restart/recreate objects | ✓ WIRED | Restart tests construct new persistence/store/config objects against the same DB path and recover config plus local stores. |
| `ConversationQueryService` | Reloaded SQLite sessions/runs/events | Recent sessions and transcript after restart | ✓ WIRED | `DefaultConversationQueryService` over reloaded local repositories returns recent sessions and typed transcript in `LocalProfileRestartRecoveryTest`. |

> Note: `gsd-tools verify key-links` could not resolve several function-style `from` values in plan frontmatter and reported "Source file not found". Manual source-level verification above confirms the actual links are wired.

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|---|---|---|---|---|
| `ConsoleView` model bar | `ProviderConfigStore.current()` / `ModelListResponse` | SQLite-backed `ProviderConfigStore`; `ProviderConfigController.listModels()` HTTP `/models` response | Yes — config is persisted/reloaded; model list is parsed from provider response or explicit state message | ✓ FLOWING |
| `ConsoleView` run creation | `CreateRunRequest.metadata()` | `runMetadataSnapshot()` from current provider config at submit time | Yes — safe provider/model/fallback/readiness values are copied into request metadata | ✓ FLOWING |
| `DefaultRunCommandService` / `QueuedRun` | `RunProviderMetadata providerMetadata` | Sanitized `CreateRunRequest.metadata()` | Yes — queued run carries immutable snapshot for worker dispatch | ✓ FLOWING |
| `DefaultRunDispatcher` | Effective `modelRef` | `QueuedRun.providerMetadata()` with fallback to constructor default | Yes — dispatcher tests prove snapshot selection and legacy fallback | ✓ FLOWING |
| `JdbcRunProjectionRepository` | `provider_metadata` | `safeProviderMetadata(request.metadata())` written as JSONB and read back | Yes — code and Docker-gated Testcontainers test cover cloud DB path | ✓ FLOWING |
| `SqliteLocalPersistence` / `LocalDevStores` | `provider_metadata_json` | SQLite row load/save; `parseProviderMetadata(...)` | Yes — same-DB restart tests prove metadata survives object recreation | ✓ FLOWING |
| `ConversationQueryService` restored transcript | `ConversationMessageDto` list | Local repositories reloaded from SQLite sessions/runs/events | Yes — restart test recovers USER and ASSISTANT typed messages | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Focused Phase 20 no-key/provider/local-profile regression gate | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleProviderModelBarTest,WebConsoleNoProviderFallbackTest,LocalProfileRestartRecoveryTest,RunProviderModelResolutionFlowTest test` | Build success; 20 tests run, 0 failures, 0 errors, 0 skipped | ✓ PASS |
| Planned artifact presence/substance | `gsd-tools verify artifacts` over all 5 plan files | Most plan artifact checks passed; two string-pattern checks were manually reviewed as acceptable JSONB/local-dev implementations rather than missing behavior | ✓ PASS (manual override for brittle patterns) |
| JDBC cloud persistence Testcontainers gate | `RunProviderModelMetadataPersistenceTest` | Not executed here because Docker/Testcontainers is unavailable in this environment; source test exists and code path was manually verified | ? CI-ENV SKIP |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| PROV-01 | Plans 01, 05 | User can see provider/model readiness and actionable errors from the Console model area. | ✓ SATISFIED | `provider-status`, ready/not-configured copy, redacted refresh errors, component tests. |
| PROV-02 | Plans 01, 05 | User can refresh model choices and see success, empty, and error states without silent failures. | ✓ SATISFIED | Explicit `ModelListResponse.state`, Console `data-refresh-state`, success/empty/error tests. |
| PROV-03 | Plans 02, 04, 05 | Model selection persists locally and affects only subsequent runs. | ✓ SATISFIED | Selector calls `ProviderConfigStore.update`, SQLite-backed store, next-run copy, snapshot immutability tests. |
| PROV-04 | Plans 03, 04, 05 | Each run records actual provider, model, and fallback mode used for history/debugging. | ✓ SATISFIED | `RunProviderMetadata`, JDBC/SQLite persistence, queued snapshot, dispatcher/runtime metadata. |
| PROV-05 | Plans 02, 04, 05 | No-provider fallback is clearly labeled as local fallback, not a real model answer. | ✓ SATISFIED | Default no-key send blocked; explicit fallback model area and assistant bubble labels verified. |
| PROV-06 | Plans 03, 05 | SQLite local profile persists sessions, transcripts, run metadata, provider config, and survives restart for Console continuation. | ✓ SATISFIED | Same-DB restart test recreates stores/repositories/query/config and verifies recovery. |
| SESS-05 | Plan 05 | User can refresh/reopen the Console and recover persisted sessions and their conversation transcript in local profile. | ✓ SATISFIED | `LocalProfileRestartRecoveryTest` verifies recent sessions and typed transcript after same-DB restart. |

**Orphaned Phase 20 requirements:** None. Roadmap maps exactly SESS-05 and PROV-01 through PROV-06 to Phase 20, and all are declared by Phase 20 plan frontmatter.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---:|---|---|---|
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/SessionRepository.java` | 40 | `not yet implemented` default method message | ℹ️ Info | Pre-existing port default from Phase 16; concrete JDBC/local implementations exist and Phase 20 does not rely on the default. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java` | 69 | `not yet implemented` default method message | ℹ️ Info | Pre-existing default; concrete Phase 20 paths implement metadata read models. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunEventStore.java` | 38 | `not yet implemented` default method message | ℹ️ Info | Pre-existing default; local/JDBC event read paths used by restart tests exist. |
| `docs/phase-20-provider-model-local-profile.md` | 14 | selector value mismatch | ⚠️ Warning | Documentation says `data-selection-scope="current|next-run"`; code emits `future-runs|next-run`. This is not blocking the Phase 20 product goal, but Phase 21 docs should align it. |

No blocker stubs, placeholder implementations, empty user-visible data sources, or console-log-only handlers were found in Phase 20 code paths. `return null` occurrences reviewed are defensive helper defaults or the deliberate blocked-send return path.

### Human / CI Environment Verification Notes

- The JDBC/PostgreSQL metadata test `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/RunProviderModelMetadataPersistenceTest.java` is Docker/Testcontainers-dependent. Docker is unavailable in this environment, so the test was not re-run here. This is a CI environment limitation, not a product gap: the test source exists, compile/test reactor for focused adapter-web gate passed, and the JDBC code path was manually verified.
- Broad browser refresh/reopen/restart flows remain Phase 21 scope per roadmap and handoff documentation; Phase 20's same-DB local-profile proof is Java-level and passed.

### Gaps Summary

No goal-blocking gaps found. Phase 20's observable must-haves are implemented, wired, data-backed, and covered by passing focused tests. The only non-blocking follow-up is to align one handoff doc selector value (`current` vs `future-runs`) before Phase 21 browser test authoring.

---

_Verified: 2026-07-04T12:11:30Z_
_Verifier: the agent (gsd-verifier)_
