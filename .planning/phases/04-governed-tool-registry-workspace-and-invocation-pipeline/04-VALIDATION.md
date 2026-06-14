---
phase: 04
slug: governed-tool-registry-workspace-and-invocation-pipeline
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-06-14
---

# Phase 04 — Validation Strategy

> Per-phase validation contract for governed tool registry, workspace execution, gateway policy, audit/event, and redaction behavior.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Maven + JUnit Jupiter + AssertJ + ArchUnit |
| **Config file** | `pom.xml` Maven reactor |
| **Quick run command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl <module> -am -Dtest=<FocusedTest> test` |
| **Full suite command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-app,pi-agent-infrastructure,pi-testkit,pi-agent-adapter-web -am test` |
| **Estimated runtime** | Focused tests ~10-60s; full reactor may be Docker/Testcontainers gated |

---

## Sampling Rate

- **After every task commit:** Run the task's `<verify><automated>` command.
- **After every plan wave:** Run that plan's `<verification>` command.
- **Before `/gsd-verify-work`:** Full suite or documented Docker-gated equivalent must be green.
- **Max feedback latency:** 60 seconds for focused tests.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | TOOL-01/04/OPS-05 | domain unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain -Dtest=ToolDescriptorContractTest test` | ✅ planned | ⬜ pending |
| 04-01-02 | 01 | 1 | TOOL-06 | domain unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain -Dtest=RunEventContractTest,ToolDescriptorContractTest test` | ✅ planned | ⬜ pending |
| 04-02-01 | 02 | 2 | TOOL-01/02 | app contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=ToolRegistryAppPortContractTest test` | ✅ planned | ⬜ pending |
| 04-02-02 | 02 | 2 | TOOL-01 | app/client unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app,pi-agent-client -am -Dtest=DefaultToolRegistryQueryServiceTest test` | ✅ planned | ⬜ pending |
| 04-03-01 | 03 | 3 | TOOL-02..05/OPS-03 | app compile | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -DskipTests compile` | ✅ planned | ⬜ pending |
| 04-03-02 | 03 | 3 | TOOL-02..06/OPS-02/05/WORK-08 | app unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=DefaultToolExecutionGatewayTest test` | ✅ planned | ⬜ pending |
| 04-04-01 | 04 | 4 | TOOL-03 | infrastructure unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am -Dtest=ToolInfrastructureGovernanceTest test` | ✅ planned | ⬜ pending |
| 04-04-02 | 04 | 4 | TOOL-04/05/OPS-05/WORK-08 | infrastructure unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am -Dtest=ToolInfrastructureGovernanceTest test` | ✅ planned | ⬜ pending |
| 04-05-01 | 05 | 5 | TOOL-02/E2E-02 | testkit compile | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am -DskipTests compile` | ✅ planned | ⬜ pending |
| 04-05-02 | 05 | 5 | E2E-02/03 | testkit unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am -Dtest=FakeGeneralAgentLoopTest test` | ✅ planned | ⬜ pending |
| 04-06-01 | 06 | 5 | WORK-03/07 | infrastructure unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am -Dtest=LocalTempWorkspaceBoundaryTest test` | ✅ planned | ⬜ pending |
| 04-06-02 | 06 | 5 | TOOL-07/WORK-08 | infrastructure unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am -Dtest=BuiltinWorkspaceToolsTest,ToolInfrastructureGovernanceTest test` | ✅ planned | ⬜ pending |
| 04-07-01 | 07 | 6 | TOOL-01/06/OPS-05 | adapter unit | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ToolRegistryControllerTest test` | ✅ planned | ⬜ pending |
| 04-07-02 | 07 | 6 | TOOL-02/OPS-02 | adapter wiring | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ToolGovernanceWiringTest,CloudRuntimeWiringIntegrationTest test` | ✅ planned | ⬜ pending |
| 04-08-01 | 08 | 7 | E2E-02/03 | cloud E2E | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=CloudServerGovernedToolE2ETest test` | ✅ planned | ⬜ pending |
| 04-08-02 | 08 | 7 | E2E-06 | security E2E | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=GovernedToolSecurityRedactionE2ETest test` | ✅ planned | ⬜ pending |
| 04-08-03 | 08 | 7 | traceability | docs grep | `test -f docs/phase-04-governed-tool-contracts.md && grep -q "TOOL-01" .planning/REQUIREMENTS.md && grep -q "ToolExecutionGateway" docs/phase-04-governed-tool-contracts.md` | ✅ planned | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing Maven/JUnit/AssertJ/ArchUnit infrastructure covers all phase requirements. No separate Wave 0 scaffold is required because every production task creates or updates a focused test in the same task before implementation.

---

## Manual-Only Verifications

All Phase 4 behaviors have automated verification. No manual checkpoints are planned.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify commands.
- [x] Sampling continuity: no 3 consecutive tasks without automated verify.
- [x] Wave 0 covers all MISSING references — none required.
- [x] No watch-mode flags.
- [x] Feedback latency target < 60s for focused tests.
- [x] `nyquist_compliant: true` set in frontmatter.

**Approval:** approved 2026-06-14
