---
phase: 16-conversation-read-model-and-recent-sessions
plan: 03
type: execute
wave: 3
depends_on:
  - 16-conversation-read-model-and-recent-sessions-02
files_modified:
  - pi-agent-infrastructure/src/main/resources/db/migration/V4__conversation_read_model_indexes.sql
  - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcSessionRepository.java
  - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java
  - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunEventStore.java
  - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcConversationReadModelIntegrationTest.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalConversationReadModelPersistenceTest.java
autonomous: true
requirements:
  - SESS-01
  - SESS-04
must_haves:
  truths:
    - "Production JDBC recent-session and transcript queries are ordered, typed, and tenant/user/session/run filtered."
    - "Run event restore cannot fetch events by runId alone for conversation transcript paths."
    - "Local SQLite profile has only minimal read-model alignment needed for Phase 16 and does not implement Phase 20 restart/product UX."
  artifacts:
    - path: "pi-agent-infrastructure/src/main/resources/db/migration/V4__conversation_read_model_indexes.sql"
      provides: "Conversation read-model query indexes"
    - path: "pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcSessionRepository.java"
      provides: "Recent session summary query implementation"
    - path: "pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java"
      provides: "Session-owned run query implementation"
    - path: "pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunEventStore.java"
      provides: "Ownership-safe session/run event query implementation"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java"
      provides: "Minimal local targeted load/index support"
  key_links:
    - from: "JdbcRunEventStore"
      to: "run_events"
      via: "WHERE tenant_id/user_id/session_id/run_id filters"
      pattern: "tenant_id = .*user_id = .*session_id = .*run_id ="
    - from: "JdbcSessionRepository"
      to: "sessions/runs/run_events"
      via: "latest activity and summary rows"
      pattern: "ORDER BY.*updated_at.*DESC"
    - from: "LocalDevRuntimeBeanConfiguration"
      to: "SqliteLocalPersistence"
      via: "same App repository ports"
      pattern: "listRecent|getTranscript|listBySessionRun"
---

<objective>
Implement Infrastructure and local-profile storage support for the conversation read model.

Purpose: App ports from Plan 02 need concrete JDBC and minimal SQLite implementations that satisfy ownership filtering (D-15), latest activity/session summary semantics (D-09 through D-12), projection-first transcript source (D-05), and the bounded SQLite alignment decision (D-17).

Output: migration/indexes, JDBC repository methods, local SQLite targeted loads, and ownership/ordering integration tests.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/REQUIREMENTS.md
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-CONTEXT.md
@.planning/research/ARCHITECTURE.md
@.planning/research/PITFALLS.md
@.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-02-SUMMARY.md
@pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql
@pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcSessionRepository.java
@pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java
@pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunEventStore.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java
@pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcPersistenceIntegrationTest.java

<interfaces>
Critical existing ownership gaps to close for conversation paths:
```java
// Existing raw query smell: ignores RequestContext/sessionId in lower layer.
public List<RunEvent> listByRun(String runId, long afterSequence, int limit);

// Existing session history query filters only session_id after findById.
SELECT entry_id, session_id, parent_entry_id, sequence, payload_type, payload, created_at
FROM session_entries
WHERE session_id = ?
ORDER BY sequence ASC
```

Existing schema fields available for filtering:
```sql
runs(tenant_id, user_id, session_id, run_id, input_type, input, status, created_at, updated_at, last_event_sequence)
run_events(tenant_id, user_id, session_id, run_id, sequence, event_type, timestamp, payload, visibility, redaction)
sessions(tenant_id, user_id, session_id, status, created_at, updated_at, metadata)
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add JDBC ownership and ordering integration tests</name>
  <files>pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcConversationReadModelIntegrationTest.java</files>
  <behavior>
    - Test 1: recent sessions for tenant/user A exclude tenant/user B and order by latest activity descending.
    - Test 2: transcript event load for session A/run A excludes same runId-like or later events from another tenant/user/session.
    - Test 3: run projection list-by-session returns run input maps needed for USER transcript messages and does not require Vaadin state.
    - Test 4: latest visible preview/status data can be derived without using `SessionHistoryResponse.entries` maps.
  </behavior>
  <action>Create a focused infrastructure integration test using the existing JDBC test style/Testcontainers conventions. Seed sessions, runs, and run_events directly through repositories or JDBC with at least two tenants/users/sessions. The test should fail against current code because list/recent and ownership-aware event methods do not exist yet. This directly addresses D-09, D-11, D-12, D-15, and pitfalls #11/#15/#17.</action>
  <verify>
    <automated>mvn -pl pi-agent-infrastructure -Dtest=JdbcConversationReadModelIntegrationTest test</automated>
  </verify>
  <done>Failing tests demonstrate missing recent-session ordering and ownership-safe transcript query support.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Implement JDBC read-model repositories and migration indexes</name>
  <files>pi-agent-infrastructure/src/main/resources/db/migration/V4__conversation_read_model_indexes.sql, pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcSessionRepository.java, pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java, pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunEventStore.java</files>
  <behavior>
    - Recent sessions query filters `sessions.tenant_id` and `sessions.user_id`, orders by conversation activity descending, derives stable first-user-message title fallback, latest safe preview, and recent run status where available.
    - Run list query filters by tenant/user/session and returns runs ordered by createdAt/updatedAt for assembler input, including input maps.
    - Event list query filters by tenant/user/session/run and sequence, preserving existing `listByRun` for diagnostic compatibility but not using it for conversation transcript.
  </behavior>
  <action>Implement Plan 02 port methods in the JDBC repositories. Add V4 indexes only; do not rewrite V1 or existing migrations. Use SQL-level filters for `tenant_id`, `user_id`, `session_id`, and `run_id` per D-15. Preserve D-10 by deriving default title from the first user input and not retitling every prompt. Preserve D-11 by building last preview from latest visible transcript candidate after redaction/safe fallback. Keep diagnostic run/event endpoints compatible: existing `listByRun` may remain, but conversation read model must use ownership-aware methods. Avoid provider/model/local profile Phase 20 expansion.</action>
  <verify>
    <automated>mvn -pl pi-agent-infrastructure -Dtest=JdbcConversationReadModelIntegrationTest test</automated>
  </verify>
  <done>JDBC integration tests pass and SQL for conversation read paths proves tenant/user/session/run ownership filters at repository level.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Align minimal local SQLite read-model adapter</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/SqliteLocalPersistence.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalConversationReadModelPersistenceTest.java</files>
  <behavior>
    - Local session load is ordered by `updated_at DESC` and can be limited for recent-session summaries.
    - Local runs retain input JSON and trace/correlation fields needed for transcript USER messages.
    - Local events can be loaded by run and by session/run with sequence ordering.
    - Local methods implement the same App repository ports as JDBC and do not introduce browser localStorage, Vaadin memory, or Phase 20 restart UX claims.
  </behavior>
  <action>Add the minimum SQLite schema columns/indexes and targeted load methods required by the Plan 02 App ports, per D-17. Existing local tables are created imperatively, so keep changes local and version-tolerant with `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE` guarded by missing-column checks if needed, and indexes for `local_sessions(user_id, updated_at)`, `local_runs(session_id, updated_at)`, and `local_events(run_id, sequence)`. Update `LocalDevRuntimeBeanConfiguration` in-memory repositories to preserve run input and enforce tenant/user/session filters for conversation paths. Do not implement Phase 20 provider config stability, restart UAT, or full local profile productization.</action>
  <verify>
    <automated>mvn -pl pi-agent-adapter-web -Dtest=LocalConversationReadModelPersistenceTest test</automated>
  </verify>
  <done>Local read-model contract tests pass and local storage supports Phase 16 recent/transcript queries without becoming an alternate product history source.</done>
</task>

</tasks>

<verification>
Run focused repository gates:

```bash
mvn -pl pi-agent-infrastructure -Dtest=JdbcConversationReadModelIntegrationTest test
mvn -pl pi-agent-adapter-web -Dtest=LocalConversationReadModelPersistenceTest test
```
</verification>

<success_criteria>
- SESS-01 recent-session ordering/filtering is implemented in production JDBC and local-profile repositories.
- SESS-04 transcript source data is retrievable from typed persisted run/event/read-model data, not Vaadin memory or raw `SessionHistoryResponse.entries`.
- Ownership filters are applied at SQL/repository level per D-15.
- SQLite work is limited to D-17 minimal adapter alignment and does not claim Phase 20 local profile/restart/provider stability.
</success_criteria>

<output>
After completion, create `.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-03-SUMMARY.md`.
</output>
