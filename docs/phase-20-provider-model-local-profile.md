# Phase 20 Provider/Model Local Profile Handoff

Phase 20 stabilizes the no-key local development loop for provider/model selection, explicit local fallback, safe run metadata, and SQLite local profile restart recovery. This document is the Phase 21 handoff for focused regression and release hardening.

## Stable Console selectors

- `data-role="model-selector"` identifies the selected model combo box.
- `data-role="provider-status"` identifies compact provider/model readiness text.
  - `data-provider-ready="true|false"` is the readiness hook.
- `data-action="refresh-models"` identifies the model refresh action.
- `data-role="model-refresh-status"` identifies visible refresh/blocked-send feedback.
  - `data-refresh-state="idle|success|empty|error|blocked"` is the refresh state hook.
- `data-role="model-selection-scope"` identifies the copy explaining selection applies to the next run while a run is active.
  - `data-selection-scope="current|next-run"` is the scope hook.
- `data-role="fallback-label"` identifies explicit local fallback labels in the model area and restored assistant bubble metadata.
  - `data-fallback-mode="local"` marks intentionally enabled local fallback.

## Provider readiness and model refresh semantics

- Disabled provider or blank API key is `not_configured`; sending is blocked before session/run creation and before appending a user message.
- Refresh success reports a positive model count and populates the model selector.
- Refresh empty reports that the provider returned no model choices or configuration is incomplete.
- Refresh error reports a safe redacted provider summary. Raw exceptions, bearer headers, API keys, provider config snapshots, and SDK objects must not be shown.
- Model selection persists immediately to `ProviderConfigStore`/SQLite and applies only to subsequent runs.

## Safe per-run metadata fields

Runs may persist and render only these provider/model/fallback facts:

- `requestedModelRef`
- `selectedModelRef`
- `resolvedProviderId`
- `resolvedModelId`
- `fallbackMode`
- `readinessState`
- `safeErrorSummary`

These fields live in `RunProviderMetadata` and are safe for history/debugging surfaces. Do not persist API keys, authorization headers, raw provider config snapshots, request bodies, raw provider responses, or provider SDK objects.

## Local fallback semantics

- No provider/key blocks send by default.
- Explicit local fallback can be enabled for local/dev/test paths only.
- Explicit fallback must be labeled in the model area and assistant bubble metadata.
- Fallback labels must survive transcript/history restore through persisted `RunProviderMetadata`.
- There is no automatic paid-provider fallback in Phase 20.

## SQLite restart verification

The same-DB restart proof is:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=LocalProfileRestartRecoveryTest test
```

The final focused Phase 20 gate is:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleProviderModelBarTest,WebConsoleNoProviderFallbackTest,LocalProfileRestartRecoveryTest test
```

`LocalProfileRestartRecoveryTest` recreates `SqliteLocalPersistence`, `LocalDevStores`, repositories, `DefaultConversationQueryService`, and `ProviderConfigStore` against the same SQLite DB path. It proves recovery of provider config, selected model, recent sessions, typed transcript, run provider metadata, fallback metadata, and ownership filters.

## Phase 21 handoff gaps

- Add broad browser regression for live server refresh/reopen/restart flows.
- Add release security checks around provider error redaction and ownership leakage across routes.
- Keep Java 21 explicit in local Maven commands when Maven defaults to Java 17.
- Treat previously reported planned restart links as expected proof/wiring targets unless a real missing prerequisite appears.

## Explicitly deferred ideas

- No automatic paid-provider fallback.
- No multi-provider routing or provider priority policy.
- No provider-specific context-window policy.
- No conversation search, rename, archive, pin, or delete.
- No broad App-level local profile abstraction replacement.
