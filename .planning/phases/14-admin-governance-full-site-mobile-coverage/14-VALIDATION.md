---
phase: 14
slug: admin-governance-full-site-mobile-coverage
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-06-25
---

# Phase 14 — Validation Strategy

> Per-phase validation contract for Admin Governance mobile coverage.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter + AssertJ for Vaadin component contracts; Playwright for mobile browser route gates |
| **Config file** | `pom.xml`, `playwright.config.ts` |
| **Quick run command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test` |
| **Full suite command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest,McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest,AdminOperationsViewTest test && PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-14-admin-governance-mobile.spec.ts --project="Mobile Chrome" --list` |
| **Estimated runtime** | ~60 seconds for focused Java contracts; Playwright list gate under 60 seconds |

---

## Sampling Rate

- **After every task commit:** Run the task-specific Maven or Playwright list command in the PLAN.
- **After every plan wave:** Run the full suite command above.
- **Before `/gsd-verify-work`:** Full suite must be green; full browser execution is optional if host browser/Vaadin dev-mode dependencies are available.
- **Max feedback latency:** 60 seconds for required local gates.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 14-01-01 | 01 | 1 | MADM-01 | Java contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test` | ✅ | ⬜ pending |
| 14-01-02 | 01 | 1 | MADM-01 | Java contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebMobileBaselineContractTest,AdminGovernanceViewsTest test` | ✅ | ⬜ pending |
| 14-02-01 | 02 | 2 | MADM-03,MADM-04,MADM-05 | Java contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest,AdminGovernanceViewsTest test` | ✅ | ⬜ pending |
| 14-02-02 | 02 | 2 | MADM-02,MADM-03,MADM-04,MADM-05 | Java contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest test` | ✅ | ⬜ pending |
| 14-03-01 | 03 | 2 | MADM-02 | Java contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminOperationsViewTest test` | ✅ | ⬜ pending |
| 14-04-01 | 04 | 2 | MADM-06,MADM-07 | Java contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test` | ✅ | ⬜ pending |
| 14-05-01 | 05 | 3 | MVER-04 | Playwright list gate | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-14-admin-governance-mobile.spec.ts --project="Mobile Chrome" --list` | ❌ W0 | ⬜ pending |
| 14-05-02 | 05 | 3 | MVER-04 | Documentation grep | `test -f docs/phase-14-admin-governance-mobile.md && grep -q "MVER-04" docs/phase-14-admin-governance-mobile.md` | ❌ W0 | ⬜ pending |

---

## Wave 0 Requirements

- Existing Java test infrastructure covers Admin component contracts.
- Existing Playwright infrastructure covers mobile projects/helpers.
- `e2e/phase-14-admin-governance-mobile.spec.ts` is created in Plan 05 before its verification command is required.
- `docs/phase-14-admin-governance-mobile.md` is created in Plan 05 before its documentation grep command is required.

---

## Manual-Only Verifications

All Phase 14 required behaviors have automated local verification. Real-device/mobile-browser UAT remains Phase 15.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60 seconds for required local gates
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-06-25
