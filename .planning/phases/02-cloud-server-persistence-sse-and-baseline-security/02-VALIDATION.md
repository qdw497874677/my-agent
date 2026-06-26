---
phase: 02
slug: cloud-server-persistence-sse-and-baseline-security
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-14
---

# Phase 02 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 5.10.3, AssertJ 3.26.3, ArchUnit 1.3.0; add Spring Boot Test and Testcontainers Java 2.0.3 for Phase 2 |
| **Config file** | Parent `pom.xml`; module `pom.xml` files; Flyway migrations under `pi-agent-infrastructure/src/main/resources/db/migration/` |
| **Quick run command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl <touched-module> -am` |
| **Full suite command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` |
| **Estimated runtime** | ~60-180 seconds locally without containers; Testcontainers integration requires Docker-enabled environment |

---

## Sampling Rate

- **After every task commit:** Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl <touched-module> -am`.
- **After every plan wave:** Run targeted module suites plus any integration tests introduced in that wave.
- **Before `/gsd-verify-work`:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` must be green, including PostgreSQL/Testcontainers-backed E2E in a Docker-enabled environment.
- **Max feedback latency:** 180 seconds for local non-container checks; container checks may exceed this and must run at phase gate.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | CLOUD-01..CLOUD-06, E2E-01, E2E-04, E2E-05 | Maven dependency foundation | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-client,pi-agent-app -am` | planned in task | ⬜ pending |
| 02-02-01 | 02 | 2 | CLOUD-01, CLOUD-03, CLOUD-04, CLOUD-05, E2E-01 | client DTO contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-client -am -Dtest=CloudApiDtoContractTest` | planned in task | ⬜ pending |
| 02-03-01 | 03 | 2 | CLOUD-02, CLOUD-03, CLOUD-05, E2E-05 | event DTO contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-client -am -Dtest=RunEventDtoContractTest` | planned in task | ⬜ pending |
| 02-04-01 | 04 | 3 | CLOUD-01, CLOUD-03, CLOUD-04, CLOUD-05 | App use-case interface contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-app -am -Dtest=AppUseCaseContractTest,AppDependencyArchTest` | planned in task | ⬜ pending |
| 02-05-01 | 05 | 4 | CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-06, E2E-04 | App ports + terminal publisher contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-app -am -Dtest=AppPortContractTest,AppDependencyArchTest` | planned in task | ⬜ pending |
| 02-06-01 | 06 | 5 | CLOUD-01, CLOUD-03, CLOUD-05, CLOUD-06 | App session/query implementation unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-app -am -Dtest='DefaultSessionUseCaseTest,DefaultRunQueryServiceTest'` | planned in task | ⬜ pending |
| 02-06-02 | 06 | 5 | CLOUD-01, CLOUD-04, CLOUD-05, CLOUD-06, E2E-04 | App run command + queued terminal event unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-app -am -Dtest='DefaultRunCommandServiceTest,AppUseCaseImplementationArchTest,AppDependencyArchTest'` | planned in task | ⬜ pending |
| 02-07-01 | 07 | 5 | CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-06, E2E-05 | Flyway/JDBC/Testcontainers | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-infrastructure -am -Dtest=JdbcPersistenceIntegrationTest` | planned in task | ⬜ pending |
| 02-07-02 | 07 | 5 | CLOUD-02, CLOUD-06, E2E-05 | persist-then-emit EventSink integration | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-infrastructure -am -Dtest=JdbcPersistenceIntegrationTest` | planned in task | ⬜ pending |
| 02-08-01 | 08 | 6 | CLOUD-01, CLOUD-04, CLOUD-06, E2E-04 | queue/cancellation registry unit + Testcontainers | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-infrastructure -am -Dtest='PostgresRunQueueTest,InMemoryCancellationRegistryTest'` | planned in task | ⬜ pending |
| 02-08-02 | 08 | 6 | CLOUD-01, CLOUD-04, CLOUD-06, E2E-01, E2E-04 | dispatcher terminal events + plain scheduler unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-infrastructure -am -Dtest='DefaultRunDispatcherTest,RunWorkerSchedulerTest'` | planned in task | ⬜ pending |
| 02-09-01 | 09 | 6 | CLOUD-01, CLOUD-05 | Spring shell/security/correlation integration | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=SecurityAndCorrelationIntegrationTest` | planned in task | ⬜ pending |
| 02-10-01 | 10 | 7 | CLOUD-01, CLOUD-03, CLOUD-04, CLOUD-05 | REST controller integration + activation hook seam | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='RunApiIntegrationTest,RunQueryIntegrationTest'` | planned in task | ⬜ pending |
| 02-10-02 | 10 | 7 | CLOUD-02, CLOUD-03, CLOUD-05 | RunEvent DTO mapper integration | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=RunQueryIntegrationTest` | planned in task | ⬜ pending |
| 02-11-01 | 11 | 8 | CLOUD-02, CLOUD-03, CLOUD-05, E2E-05 | SSE fanout integration | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=RunSseIntegrationTest` | planned in task | ⬜ pending |
| 02-11-02 | 11 | 8 | CLOUD-02, CLOUD-03, CLOUD-05, E2E-05 | SSE replay-before-subscribe integration | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=RunSseIntegrationTest` | planned in task | ⬜ pending |
| 02-12-01 | 12 | 9 | CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-06, E2E-01, E2E-04, E2E-05 | complete Spring bean wiring + worker activation + EventSink→DB→SSE fanout | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=CloudRuntimeWiringIntegrationTest` | planned in task | ⬜ pending |
| 02-13-01 | 13 | 10 | CLOUD-01..CLOUD-06, E2E-01, E2E-04, E2E-05 | headless E2E + cancellation/timeout/max-step terminal rows; max-step must use public REST/product path, deterministic fake runtime or `RuntimeLimits`, terminal status assertion, exactly one terminal event row last in `run_events` and `/events`, no hanging tasks, and no direct dispatcher call | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='CloudServerHeadlessE2ETest,RunCancellationIntegrationTest'` | planned in task | ⬜ pending |
| 02-13-02 | 13 | 10 | CLOUD-01..CLOUD-06, E2E-01, E2E-04, E2E-05 | docs + full phase gate | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` | planned in task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Parent/module POMs include Spring Boot Test, Spring Security Test, PostgreSQL JDBC, Flyway, and Testcontainers dependencies where needed (Plan 02-01).
- [ ] `pi-agent-client/src/main/java/...` contains public REST/SSE DTO package stubs for run create/detail/status/event/history/cancel responses (Plans 02-02, 02-03).
- [ ] `pi-agent-app/src/main/java/...` contains use-case, persistence/query/queue, dispatcher, cancellation, and `RunTerminalEventPublisher` port stubs (Plans 02-04, 02-05).
- [ ] `pi-agent-app/src/main/java/.../DefaultRunTerminalEventPublisher.java` and `DefaultRunCommandService.java` cover queued cancellation terminal event publishing exactly once (Plan 02-06).
- [ ] `pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql` covers sessions, runs, run_events, steps, messages, tool_calls, audit_records, and run_queue (Plan 02-07).
- [ ] `pi-agent-infrastructure/src/test/java/.../JdbcPersistenceIntegrationTest.java` covers CLOUD-06 persistence, terminal detection, and persist-then-emit ordering (Plan 02-07).
- [ ] `pi-agent-infrastructure/src/test/java/.../PostgresRunQueueTest.java` covers DB queue claim/cancel ordering and `cancelQueuedAndReturn` returning `QueuedRun` payload (Plan 02-08).
- [ ] `pi-agent-infrastructure/src/test/java/.../DefaultRunDispatcherTest.java` covers timeout/failure/cancellation fallback terminal event rows exactly once (Plan 02-08).
- [ ] `pi-agent-infrastructure/src/test/java/.../RunWorkerSchedulerTest.java` covers plain scheduler `pollOnce`/`triggerAsync` behavior without Spring bean registration (Plan 02-08).
- [ ] `pi-agent-adapter-web/src/test/java/.../RunApiIntegrationTest.java` covers CLOUD-01, CLOUD-03, CLOUD-04, and the activation hook seam without direct dispatcher calls (Plan 02-10).
- [ ] `pi-agent-adapter-web/src/test/java/.../CloudRuntimeWiringIntegrationTest.java` covers exactly one bean for RunQueue, CancellationRegistry, RunDispatcher, RunWorkerScheduler, EventSink, and RunEventFanout, plus Controller -> App service -> JDBC repositories/queue/audit -> dispatcher -> AgentRuntime -> EventSink -> DB -> SSE fanout wiring (Plan 02-12).
- [ ] `pi-agent-adapter-web/src/test/java/.../RunSseIntegrationTest.java` covers CLOUD-02 and E2E-05, including replay-before-subscribe and cleanup (Plan 02-11).
- [ ] `pi-agent-adapter-web/src/test/java/.../SecurityAndCorrelationIntegrationTest.java` covers CLOUD-05 (Plan 02-09).
- [ ] `pi-agent-adapter-web/src/test/java/.../CloudServerHeadlessE2ETest.java` covers E2E-01, E2E-04, and E2E-05 through automatic worker activation, including `maxStepRunEmitsSingleTerminalEventAndDoesNotHang`, not manual dispatcher calls (Plan 02-13). The max-step test must use the public REST/product path, configure deterministic max-step through fake runtime setup or `RuntimeLimits`, assert terminal status through REST, assert exactly one terminal event row exists and is last in `run_events`, assert the same terminal event is last via `/events`, and assert no hanging worker/model/tool task remains.
- [ ] `pi-agent-adapter-web/src/test/java/.../RunCancellationIntegrationTest.java` covers queued cancellation, timeout, and failure terminal event rows exactly once (Plan 02-13).
- [ ] Docker/Testcontainers availability exists in CI or an equivalent Docker-enabled local environment for PostgreSQL integration gates.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Docker/Testcontainers availability on the execution host | CLOUD-06, E2E-01, E2E-05 | This environment may not have Docker available; automated tests require a Docker-enabled runner | Run `docker ps` and then `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-infrastructure -am -Dtest=JdbcPersistenceIntegrationTest` in CI/local Docker environment. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies.
- [x] Sampling continuity: no 3 consecutive tasks without automated verify.
- [x] Wave 0 covers all MISSING references.
- [x] No watch-mode flags.
- [x] Feedback latency < 180s for local non-container checks; container checks explicitly gated.
- [x] `nyquist_compliant: true` set in frontmatter.

**Approval:** approved 2026-06-14
