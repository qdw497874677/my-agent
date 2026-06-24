---
phase: 13
slug: runtime-cards-timeline-tool-and-approval-ux
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-06-24
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter + AssertJ for Vaadin component contracts; Playwright 1.57 for browser E2E |
| **Config file** | `pom.xml`, `pi-agent-adapter-web/pom.xml`, `playwright.config.ts`, `package.json` |
| **Quick run command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest test` |
| **Full suite command** | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome" --list` plus the Java quick command |
| **Estimated runtime** | ~60 seconds for Java targeted tests; Playwright list gate < 30 seconds |

---

## Sampling Rate

- **After every task commit:** Run the plan-local Java target named in `<verify>`.
- **After every plan wave:** Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest test`.
- **Before `/gsd-verify-work`:** Java targeted suite plus Playwright Phase 13 list gate must be green; full browser run is CI/prepared-runner evidence if local Vaadin/WebKit dependencies are unavailable.
- **Max feedback latency:** 90 seconds for local targeted gates.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 13-01-01 | 01 | 1 | MCARD-01, MCARD-03 | unit/component | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest test` | ✅ | ⬜ pending |
| 13-01-02 | 01 | 1 | MCARD-01, MCARD-05 | unit/component | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest test` | ✅ | ⬜ pending |
| 13-02-01 | 02 | 2 | MCARD-02, MCARD-03 | unit/component | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleCatalogAndToolCardsTest test` | ✅ | ⬜ pending |
| 13-02-02 | 02 | 2 | MCARD-02, MCARD-03 | unit/component | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleCatalogAndToolCardsTest,WebConsoleRuntimeCardsTest test` | ✅ | ⬜ pending |
| 13-03-01 | 03 | 3 | MCARD-04, MCARD-05 | unit/component | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest test` | ✅ | ⬜ pending |
| 13-03-02 | 03 | 3 | MCARD-04, MCARD-05 | unit/component | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest,WebConsoleRuntimeCardsTest test` | ✅ | ⬜ pending |
| 13-04-01 | 04 | 4 | MCARD-01..MCARD-05 | browser/doc | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome" --list` | ✅ | ⬜ pending |
| 13-04-02 | 04 | 4 | MCARD-01..MCARD-05 | docs/regression | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest test` | ✅ | ⬜ pending |

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. Task 13-01-01 creates the dedicated `WebConsoleRuntimeCardsTest` before implementation changes in the same plan.

---

## Manual-Only Verifications

All Phase 13 behaviors have automated Java or Playwright verification. Real-device browser UAT remains Phase 15 scope.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify commands.
- [x] Sampling continuity: no 3 consecutive tasks without automated verify.
- [x] Wave 0 covers all MISSING references.
- [x] No watch-mode flags.
- [x] Feedback latency target documented.
- [x] `nyquist_compliant: true` set in frontmatter.

**Approval:** approved 2026-06-24
