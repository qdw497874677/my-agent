---
phase: 01
slug: runtime-spine-workspace-and-domain-contracts
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-13
---

# Phase 01 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter + AssertJ + ArchUnit |
| **Config file** | root `pom.xml` plus module POMs created by Plan 01 |
| **Quick run command** | `mvn -q test` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~45 seconds after dependencies are cached |

---

## Sampling Rate

- **After every task commit:** Run `mvn -q test`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds after Maven dependencies are cached

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | CORE-06, CORE-09 | build/arch | `mvn -q test` | ❌ W0 | ⬜ pending |
| 01-01-02 | 01 | 1 | CORE-09 | arch | `mvn -q -pl pi-agent-domain test` | ❌ W0 | ⬜ pending |
| 01-02-01 | 02 | 2 | CORE-01, CORE-07 | unit | `mvn -q -pl pi-agent-domain test` | ✅ | ⬜ pending |
| 01-02-02 | 02 | 2 | CORE-02, CORE-05, OPS-04 | unit | `mvn -q -pl pi-agent-domain test` | ✅ | ⬜ pending |
| 01-02-03 | 02 | 2 | CORE-04 | contract | `mvn -q -pl pi-agent-domain test` | ✅ | ⬜ pending |
| 01-03-01 | 03 | 2 | WORK-01, CORE-08 | unit | `mvn -q -pl pi-agent-domain test` | ✅ | ⬜ pending |
| 01-03-02 | 03 | 2 | WORK-02, WORK-04, WORK-05 | unit | `mvn -q -pl pi-agent-domain test` | ✅ | ⬜ pending |
| 01-03-03 | 03 | 2 | CORE-02, CORE-08 | unit | `mvn -q -pl pi-agent-domain test` | ✅ | ⬜ pending |
| 01-04-01 | 04 | 3 | CORE-03, OPS-06 | unit | `mvn -q -pl pi-testkit -am test` | ✅ | ⬜ pending |
| 01-04-02 | 04 | 3 | CORE-03, CORE-05 | unit | `mvn -q -pl pi-testkit -am test` | ✅ | ⬜ pending |
| 01-04-03 | 04 | 3 | CORE-04, OPS-06 | unit | `mvn -q -pl pi-testkit -am test` | ✅ | ⬜ pending |
| 01-05-01 | 05 | 4 | CORE-06, CORE-09 | arch | `mvn -q test` | ✅ | ⬜ pending |
| 01-05-02 | 05 | 4 | CORE-04, CORE-05 | contract | `mvn -q test` | ✅ | ⬜ pending |
| 01-05-03 | 05 | 4 | OPS-06 | docs/contracts | `mvn test` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `pom.xml` — Maven parent with Java 21, JUnit Jupiter, AssertJ, ArchUnit, compiler, and surefire setup.
- [ ] `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java` — Domain forbidden dependency test.
- [ ] `pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java` — COLA direction test.
- [ ] Module POMs for `pi-agent-client`, `pi-agent-domain`, `pi-agent-app`, `pi-agent-infrastructure`, `pi-agent-adapter-web`, and `pi-testkit`.

---

## Manual-Only Verifications

All Phase 1 behaviors have automated verification. No manual verification checkpoint is required because there is no UI and no external service in this phase.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency target < 60s after dependency cache warm-up
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending execution
