# Phase 16 Plan 03 Summary

## Completed

- Added additive PostgreSQL conversation read-model indexes in `V4__conversation_read_model_indexes.sql`.
- Implemented JDBC conversation read paths:
  - `JdbcSessionRepository.listRecent(...)`
  - `JdbcRunProjectionRepository.listRunsBySession(...)`
  - `JdbcRunEventStore.listBySessionRun(...)`
- Added minimal local SQLite alignment for recent sessions, run inputs, and session/run event reads.
- Added local in-memory repository implementations for the same App ports.
- Added focused JDBC and local persistence tests.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=LocalConversationReadModelPersistenceTest test` — PASS, 4 tests.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure -Dtest=JdbcConversationReadModelIntegrationTest test` — BLOCKED by unavailable Docker/Testcontainers environment (`/var/run/docker.sock` missing), before test logic executed.

## Notes

- Existing diagnostic `RunEventStore.listByRun(...)` remains available.
- Conversation transcript paths use ownership-aware `RequestContext + sessionId + runId` filtering.
- No Phase 20 local profile productization was added.
