---
phase: 20-provider-model-and-local-profile-stability
plan: 03
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunResponse.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationRunView.java
  - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java
  - pi-agent-infrastructure/src/main/resources/db/migration/V5__run_provider_model_metadata.sql
  - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelMetadataPersistenceTest.java
autonomous: true
requirements:
  - PROV-04
  - PROV-06
must_haves:
  truths:
    - "Each run can persist safe provider/model/fallback facts for history and debugging."
    - "Run metadata is redacted and excludes raw API keys or provider config snapshots."
    - "SQLite and JDBC run projections expose the same metadata fields for local-profile restore."
  artifacts:
    - path: "pi-agent-infrastructure/src/main/resources/db/migration/V5__run_provider_model_metadata.sql"
      provides: "Additive cloud schema for run provider/model/fallback metadata"
      contains: "provider_id"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java"
      provides: "Additive local SQLite columns and load/save paths"
      contains: "addColumnIfMissing"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelMetadataPersistenceTest.java"
      provides: "JDBC/SQLite/local store proof for metadata persistence and redaction"
      min_lines: 100
  key_links:
    - from: "RunProjectionRepository"
      to: "JdbcRunProjectionRepository and LocalRunProjectionRepository"
      via: "metadata fields in create/update/query methods"
      pattern: "provider|model|fallback"
    - from: "SqliteLocalPersistence"
      to: "LocalDevStores.loadAll"
      via: "same-DB restart row hydration"
      pattern: "loadRuns"
---

<objective>
Add the safe provider/model/fallback metadata storage foundation used by run history, debugging, fallback labels, and SQLite restart proof.

Purpose: model/provider choices must be pinned to the run that used them, not inferred from the current selector after the fact.
Output: additive JDBC and SQLite schema/ports, run response/read-model metadata fields, local store hydration, and persistence tests.
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
@pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunResponse.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationRunView.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java
@pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java
@pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java

<interfaces>
Existing run projection interfaces:
```java
public record RunResponse(String tenantId, String userId, String sessionId, String runId,
        String workspaceId, String status, String traceId, String correlationId,
        Instant createdAt, Instant updatedAt) {}

public record ConversationRunView(String runId, Instant createdAt, Map<String,Object> input, String status) {}

public interface RunProjectionRepository {
    void createRun(RequestContext context, String sessionId, String runId, CreateRunRequest request);
    Optional<RunResponse> findRun(RequestContext context, String sessionId, String runId);
    PageResponse<ConversationRunView> listRunsBySession(RequestContext context, String sessionId, int limit, String cursor);
}
```
Extend these with safe metadata while keeping client DTOs plain records and App/Domain free of SQLite/Vaadin/provider SDK types.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Extend run metadata contracts and JDBC persistence</name>
  <files>pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunResponse.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationRunView.java, pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java, pi-agent-infrastructure/src/main/resources/db/migration/V5__run_provider_model_metadata.sql, pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelMetadataPersistenceTest.java</files>
  <behavior>
    - Test 1: created run can store requested model ref, resolved provider id, resolved model id, fallback mode, readiness state, and safe error summary per D-07.
    - Test 2: `findRun` and `listRunsBySession` return the metadata for history/debugging, while preserving existing tenant/user/session filters.
    - Test 3: metadata never stores raw `apiKey`, bearer token, or complete provider config snapshot.
  </behavior>
  <action>Add an immutable safe metadata shape to run projection contracts. Use either typed optional fields on `RunResponse`/`ConversationRunView` or a defensive `Map<String,Object> providerMetadata`; choose the smallest change that keeps public DTOs JSON-friendly. Add Flyway migration `V5__run_provider_model_metadata.sql` with additive nullable columns or a JSONB metadata column. Required safe facts per D-07: requested/selected model ref, resolved provider id, resolved model id, fallback mode, readiness state, and safe error summary. Do not store raw API keys, full provider config snapshots, headers, request bodies, or provider SDK objects. Keep D-08 in mind: metadata is available to history/debugging surfaces but not forced into recent-session cards.</action>
  <verify>
    <automated>mvn -pl pi-agent-infrastructure,pi-agent-client,pi-agent-app -am -Dtest=RunProviderModelMetadataPersistenceTest test</automated>
  </verify>
  <done>JDBC/cloud run projection can persist and return safe provider/model/fallback metadata with additive schema only.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add matching SQLite/local store metadata persistence</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelMetadataPersistenceTest.java</files>
  <behavior>
    - Test 1: existing SQLite DB files upgrade in place through `addColumnIfMissing(...)` per D-17.
    - Test 2: `saveRun`, `loadRuns`, and `loadRunsBySession` preserve provider/model/fallback metadata across a new `SqliteLocalPersistence` instance.
    - Test 3: `LocalDevStores.loadAll()` hydrates metadata back into local run records and conversation run views.
  </behavior>
  <action>Mirror Task 1 metadata in `SqliteLocalPersistence` using additive columns or safe JSON text with `addColumnIfMissing(...)` per D-13/D-17. Extend `saveRun(...)` overloads and local `RunRecord` hydration in `LocalDevRuntimeBeanConfiguration` so local profiles preserve the same facts as JDBC. Keep ownership-filtered methods from Phase 16 intact per D-16. Avoid replacing local profile with a broad new App-level abstraction, per D-13.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=RunProviderModelMetadataPersistenceTest,LocalConversationReadModelPersistenceTest test</automated>
  </verify>
  <done>SQLite/local profile stores and reloads run provider/model/fallback metadata without breaking Phase 16 read-model filters.</done>
</task>

</tasks>

<verification>
Run focused persistence tests plus compile if constructor signatures changed: `mvn -pl pi-agent-adapter-web,pi-agent-infrastructure -am -DskipTests compile`.
</verification>

<success_criteria>
- PROV-04 storage foundation exists for every run.
- PROV-06 storage foundation includes run metadata in SQLite local profile.
- D-07, D-08, D-13, D-16, and D-17 are honored without secret leakage or global unfiltered local read-model regressions.
</success_criteria>

<output>
After completion, create `.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-03-SUMMARY.md`.
</output>
