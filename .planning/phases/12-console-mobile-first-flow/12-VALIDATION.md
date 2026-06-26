---
phase: 12
slug: console-mobile-first-flow
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-23
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for Console Mobile-First Flow.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 5.10.3 + AssertJ 3.26.3; Playwright Test 1.57.0 |
| **Config file** | `playwright.config.ts`; Maven configs in root `pom.xml` and `pi-agent-adapter-web/pom.xml` |
| **Quick run command** | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test` |
| **Full suite command** | `npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --project="Mobile Safari" --project="Tablet"` |
| **Estimated runtime** | ~30-180 seconds depending on Vaadin dev-server startup |

---

## Sampling Rate

- **After every task touching Java Console components:** Run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test`.
- **After every task touching Playwright specs/fixtures:** Run `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list`.
- **After every plan wave:** Run the Java quick command and, where browser host dependencies are available, `npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome"`.
- **Before `/gsd-verify-work`:** Full suite must be green or any local Vaadin/browser startup limitation must be documented without weakening the spec/list gate.
- **Max feedback latency:** 180 seconds for full browser runs; <60 seconds for Java quick/list checks.

---

## Per-Task Verification Map

> Decomposition aligned to final plans: Plan 01 (Wave 1, MCON-01/MCON-04), Plan 02 (Wave 2, MCON-02/MCON-03/MCON-05), Plan 03 (Wave 3, MVER-03 + regression/docs). Each task's automated command mirrors its plan `<verify>` block.

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 12-01-01 | 01 | 1 | MCON-01, MCON-04 | unit/contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test` | ❌ W0 | ⬜ pending |
| 12-01-02 | 01 | 1 | MCON-01, MCON-04 | unit/contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test` | ❌ W0 | ⬜ pending |
| 12-02-01 | 02 | 2 | MCON-02, MCON-03 | unit/contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test` | ❌ W0 | ⬜ pending |
| 12-02-02 | 02 | 2 | MCON-02, MCON-05 | unit/contract | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test` | ❌ W0 | ⬜ pending |
| 12-03-01 | 03 | 3 | MVER-03 | Playwright E2E | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list` then `npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --project="Mobile Safari" --project="Tablet"` | ❌ W0 | ⬜ pending |
| 12-03-02 | 03 | 3 | MVER-03, MCON-01..05 | regression/documentation | `npm run e2e -- e2e/phase-05-web-console.spec.ts --project="chromium"` | ✅ existing baseline / ❌ docs W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java` — stable hook/CSS/component contract for MCON-01 through MCON-05.
- [ ] `e2e/phase-12-console-mobile-flow.spec.ts` — browser-visible fake/no-key Console mobile product path for MVER-03.
- [ ] `docs/phase-12-console-mobile-flow.md` — developer/operator record of Console mobile flow selectors, verification commands, and Phase 13/15 handoffs.
- [ ] Existing infrastructure covers Playwright config, mobile projects, fake runtime, mobile smoke helpers, and desktop Console baseline.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real iOS Safari keyboard/viewport chrome behavior | MVER-07 (Phase 15) | Phase 12 uses Playwright WebKit proxy; real-device UAT is explicitly deferred | Record handoff in docs; do not block Phase 12. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 180s for representative full browser run, <60s for quick/list checks
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-06-23
