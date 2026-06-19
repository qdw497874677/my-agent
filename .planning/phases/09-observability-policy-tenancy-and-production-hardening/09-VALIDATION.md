---
phase: 09
slug: observability-policy-tenancy-and-production-hardening
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-06-19
---

# Phase 09 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter + AssertJ + ArchUnit + Spring Boot tests + Playwright smoke where already established |
| **Config file** | `pom.xml`, module `pom.xml` files, `pi-agent-adapter-web/src/main/resources/application.yml` |
| **Quick run command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am test` |
| **Full suite command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-infrastructure,pi-agent-infrastructure-observability,pi-agent-infrastructure-model-openai,pi-agent-infrastructure-mcp,pi-agent-infrastructure-plugin,pi-agent-adapter-web -am test` |
| **Estimated runtime** | ~60-180 seconds depending on focused test subset |

---

## Sampling Rate

- **After every task commit:** Run the task `<automated>` command.
- **After every plan wave:** Run the relevant focused wave command from the completed plans.
- **Before `/gsd-verify-work`:** Full Phase 9 focused suite plus final documentation grep gate must be green.
- **Max feedback latency:** 180 seconds for default no-key gates.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 09-01-* | 01 | 1 | OPS-01 | unit + migration | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-infrastructure -am -Dtest=PlatformIdsTest,TraceIdMigrationTest test` | ✅ | ⬜ pending |
| 09-02-* | 02 | 1 | OPS-01 | module + architecture | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am test` | ✅ / created by plan | ⬜ pending |
| 09-03-* | 03 | 2 | OPS-01 | event + MDC | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure,pi-agent-infrastructure-observability -am -Dtest=RunEventTelemetryTest,RunDispatcherTelemetryTest test` | ✅ | ⬜ pending |
| 09-04-* | 04 | 2 | OPS-01 | tool/policy | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app,pi-agent-infrastructure-observability,pi-agent-adapter-web -am -Dtest=ToolTelemetryTest,PolicyTelemetryTest,ToolGovernanceBeanConfigurationTest test` | ✅ | ⬜ pending |
| 09-05-* | 05 | 2 | OPS-01 | model/MCP/plugin | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai,pi-agent-infrastructure-mcp,pi-agent-infrastructure-plugin,pi-agent-infrastructure-observability -am -Dtest=ModelTelemetryTest,McpTelemetryTest,PluginTelemetryTest test` | ✅ | ⬜ pending |
| 09-06-* | 06 | 2 | OPS-01 | config/security/logging | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web,pi-agent-infrastructure-observability -am -Dtest=ObservabilityConfigurationTest,ActuatorSecurityTest,StructuredLoggingRedactionTest test` | ✅ | ⬜ pending |
| 09-07-* | 07 | 3 | OPS-01 | Admin API | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client,pi-agent-app,pi-agent-adapter-web,pi-agent-infrastructure-observability -am -Dtest=OperationsMetricsReaderTest,AdminOperationsControllerTest test` | ✅ | ⬜ pending |
| 09-08-* | 08 | 4 | OPS-01 | Admin UI | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AdminOperationsViewTest test` | ✅ | ⬜ pending |
| 09-09-* | 09 | 5 | OPS-01 | full regression + docs | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-infrastructure,pi-agent-infrastructure-observability,pi-agent-infrastructure-model-openai,pi-agent-infrastructure-mcp,pi-agent-infrastructure-plugin,pi-agent-adapter-web -am test` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing JUnit/AssertJ/ArchUnit/Spring Boot/Playwright infrastructure covers this phase. Plan 09-02 creates the new observability module and test scaffolding before downstream wrappers consume it.

---

## Manual-Only Verifications

All Phase 9 behaviors have automated no-key verification. Optional real Prometheus/OTLP collector smoke is documented only and must not block default verification.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency target < 180s for focused commands
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-06-19
