---
phase: 20-provider-model-and-local-profile-stability
plan: 05
type: execute
wave: 3
depends_on:
  - 20-provider-model-and-local-profile-stability-02
  - 20-provider-model-and-local-profile-stability-03
  - 20-provider-model-and-local-profile-stability-04
files_modified:
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalProfileRestartRecoveryTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleNoProviderFallbackTest.java
  - docs/phase-20-provider-model-local-profile.md
autonomous: true
requirements:
  - SESS-05
  - PROV-01
  - PROV-02
  - PROV-03
  - PROV-04
  - PROV-05
  - PROV-06
must_haves:
  truths:
    - "After local SQLite restart, recent sessions and typed transcripts recover from persisted data."
    - "After local SQLite restart, provider config and selected model recover from the same DB file."
    - "After local SQLite restart, provider/model/fallback run metadata remains available for restored history/debugging."
  artifacts:
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalProfileRestartRecoveryTest.java"
      provides: "End-to-end same-DB restart proof for SESS-05 and PROV-06"
      min_lines: 120
    - path: "docs/phase-20-provider-model-local-profile.md"
      provides: "Selector, fallback, metadata, and restart verification handoff for Phase 21"
      min_lines: 40
  key_links:
    - from: "SqliteLocalPersistence same DB file"
      to: "LocalDevStores.loadAll / ProviderConfigStore constructor"
      via: "restart/recreate objects"
      pattern: "new SqliteLocalPersistence"
    - from: "ConversationQueryService"
      to: "reloaded SQLite sessions/runs/events"
      via: "recent sessions and transcript after restart"
      pattern: "getTranscript|listRecent"
---

<objective>
Prove and document the local profile restart loop for provider/model/local conversation stability.

Purpose: Phase 20 is only complete if local development can refresh/reopen/restart and recover the conversation, selected model, provider config, and provider/model/fallback run metadata from SQLite.
Output: same-DB restart integration test, focused component regression coverage, and Phase 21 handoff documentation.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@.planning/STATE.md
@.planning/phases/20-provider-model-and-local-profile-stability/20-CONTEXT.md
@.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-01-SUMMARY.md
@.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-02-SUMMARY.md
@.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-03-SUMMARY.md
@.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-04-SUMMARY.md
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalConversationReadModelPersistenceTest.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigStore.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java

<interfaces>
Use the existing Phase 16 local proof style:
```java
SqliteLocalPersistence persistence = new SqliteLocalPersistence(tmp.resolve("...").toString());
LocalDevStores stores = new LocalDevStores(persistence);
stores.loadAll();
new LocalSessionRepository(stores);
new LocalRunProjectionRepository(stores);
new LocalRunEventStore(stores);
```
Restart proof means constructing new persistence/store/config objects against the same SQLite path, not reusing in-memory maps.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add same-DB local profile restart recovery proof</name>
  <files>pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalProfileRestartRecoveryTest.java</files>
  <behavior>
    - Test 1: write provider config and selected model, recreate `ProviderConfigStore`/persistence objects against the same DB, and recover config/model per D-14/D-15.
    - Test 2: create/persist session, run input, assistant/model events, run provider metadata, then recreate `LocalDevStores` and recover recent sessions and transcript via `ConversationQueryService` per SESS-05/PROV-06.
    - Test 3: fallback metadata written before restart is present after restore and suitable for bubble/metadata labeling per D-11.
    - Test 4: foreign tenant/user data remains excluded after restart per D-16.
  </behavior>
  <action>Create `LocalProfileRestartRecoveryTest` that exercises the Phase 20 D-15 end-to-end local profile recovery loop. Seed data through repository/store/config seams as much as possible (not by mutating Vaadin component state). Then instantiate new `SqliteLocalPersistence`, `LocalDevStores`, repositories, `DefaultConversationQueryService`, and provider config store or equivalent against the same DB path. Assert recent sessions, transcript user/assistant turns, run metadata provider/model/fallback facts, provider config, and selected model are recovered. Preserve ownership filters and secret redaction. Do not add a Playwright/browser server restart gate here; Phase 21 owns broad browser regression.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=LocalProfileRestartRecoveryTest test</automated>
  </verify>
  <done>Same SQLite DB restart recovers sessions, transcript, run metadata, provider config, selected model, and ownership filters.</done>
</task>

<task type="auto">
  <name>Task 2: Add final focused regression gate and Phase 21 handoff documentation</name>
  <files>pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleNoProviderFallbackTest.java, docs/phase-20-provider-model-local-profile.md</files>
  <action>Consolidate focused assertions from Plans 01-04 so Phase 20 has a no-key local gate covering readiness selectors, refresh states, selected-model next-run copy, no-key blocked send, explicit fallback labels, and local restart proof. Add `docs/phase-20-provider-model-local-profile.md` documenting stable selectors (`model-selector`, `provider-status`, `refresh-models`, refresh state/fallback hooks), safe run metadata fields, blocked-send/default fallback semantics, local restart test command, and Phase 21 handoff gaps. Mention deferred ideas explicitly: no automatic paid-provider fallback, no multi-provider routing, no provider-specific context-window policy, no search/rename/archive/pin/delete.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=WebConsoleProviderModelBarTest,WebConsoleNoProviderFallbackTest,LocalProfileRestartRecoveryTest test</automated>
  </verify>
  <done>Phase 20 focused verification command passes and documentation gives Phase 21 exact selectors/commands/gaps.</done>
</task>

</tasks>

<verification>
Final focused gate: `mvn -pl pi-agent-adapter-web -Dtest=WebConsoleProviderModelBarTest,WebConsoleNoProviderFallbackTest,LocalProfileRestartRecoveryTest test`. If metadata signatures changed across modules, also run `mvn -pl pi-agent-adapter-web -am -DskipTests compile`.
</verification>

<success_criteria>
- SESS-05: local profile refresh/reopen/restart recovers persisted sessions and transcript.
- PROV-01 through PROV-06 have focused automated proof or direct handoff evidence.
- D-13 through D-17 are honored: harden existing SQLite local profile, prove same-DB restart, keep ownership filters, and use additive migrations.
</success_criteria>

<output>
After completion, create `.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-05-SUMMARY.md`.
</output>
