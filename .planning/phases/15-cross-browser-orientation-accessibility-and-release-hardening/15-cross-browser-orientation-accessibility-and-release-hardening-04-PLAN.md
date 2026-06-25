---
phase: 15-cross-browser-orientation-accessibility-and-release-hardening
plan: 04
type: execute
wave: 3
depends_on:
  - 15-01
  - 15-02
  - 15-03
files_modified:
  - docs/phase-15-release-hardening.md
  - .planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md
autonomous: true
requirements:
  - MVER-07
must_haves:
  truths:
    - "Maintainer can find one concentrated Phase 15 release hardening document with CI coverage, viewport matrix, UAT matrix, known gaps, and go/no-go criteria."
    - "Real-device/UAT expectations explicitly list Android Chrome, iOS Safari, Edge mobile, and Firefox mobile and distinguish true devices from Playwright proxies."
    - "Uncompleted or failed true-device validation is classified as blocker, known limitation, or follow-up rather than presented ambiguously as passed."
  artifacts:
    - path: "docs/phase-15-release-hardening.md"
      provides: "Concentrated release hardening, CI matrix, UAT, gap, and go/no-go documentation"
      contains: "Android Chrome"
    - path: ".planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md"
      provides: "Scripted human real-device UAT checklist for Phase 15"
      contains: "status: partial"
  key_links:
    - from: "docs/phase-15-release-hardening.md"
      to: "e2e/phase-15-orientation-release-smoke.spec.ts"
      via: "CI/browser coverage command references"
      pattern: "phase-15-orientation-release-smoke"
    - from: "15-HUMAN-UAT.md"
      to: "docs/phase-15-release-hardening.md"
      via: "manual validation status feeds release go/no-go classification"
      pattern: "blocker|known limitation|follow-up"
---

<objective>
Create the Phase 15 release hardening and real-device/UAT documentation for MVER-07.

Purpose: Make final mobile H5 release readiness auditable by separating CI/emulation proof from true-device expectations and by classifying unresolved gaps explicitly.
Output: `docs/phase-15-release-hardening.md` and phase-local `15-HUMAN-UAT.md`.
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
@.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-CONTEXT.md
@.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-01-SUMMARY.md
@.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-02-SUMMARY.md
@.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-03-SUMMARY.md
@docs/phase-10-mobile-baseline.md
@docs/phase-11-responsive-shell.md
@docs/phase-12-console-mobile-flow.md
@docs/phase-13-runtime-cards.md
@docs/phase-14-admin-governance-mobile.md
@.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-HUMAN-UAT.md
@.planning/phases/12-console-mobile-first-flow/12-HUMAN-UAT.md

<interfaces>
Documentation must reference these planned Phase 15 gates:
```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-orientation-release-smoke.spec.ts --project="chromium" --project="Mobile Chrome" --project="Mobile Safari" --project="Mobile Firefox" --project="Tablet" --list
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="Mobile Chrome" --list
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="chromium" --list
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-accessibility-hardening.spec.ts --project="Mobile Chrome" --list
```

Phase 15 locked decisions implemented here: D-08, D-15, D-16, D-17, D-18.
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Create concentrated release hardening document</name>
  <files>docs/phase-15-release-hardening.md</files>
  <action>Create a single Phase 15 release hardening doc per D-15. Include: scope/boundaries; MVER-05/MVER-06/MVER-07 traceability; CI/browser coverage table; viewport/orientation matrix from Plan 01; critical-flow and desktop regression gates from Plan 02; accessibility/reduced-motion/no-hover contracts from Plan 03; known Playwright proxy limitations for Mobile Safari/WebKit and Mobile Firefox; and release go/no-go criteria. Explicitly state screenshot visual regression is not the primary gate per D-09 and that skipped/flaky browser-specific failures must be documented per D-08.</action>
  <verify>
    <automated>test -f docs/phase-15-release-hardening.md && grep -E "MVER-05|MVER-06|MVER-07|Android Chrome|iOS Safari|Edge mobile|Firefox mobile|known limitation|follow-up|blocker" docs/phase-15-release-hardening.md</automated>
  </verify>
  <done>Release doc exists and contains coverage, UAT, CI/emulation gap, and go/no-go sections with required browser names and classifications.</done>
</task>

<task type="auto">
  <name>Task 2: Create scripted human UAT checklist</name>
  <files>.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md</files>
  <action>Create a phase-local UAT file modeled after prior `*-HUMAN-UAT.md` examples but more complete. Frontmatter should start with `status: partial`, `phase: 15-cross-browser-orientation-accessibility-and-release-hardening`, and a timestamp placeholder/current date. Include scripted critical-path checklists for Android Chrome, iOS Safari, Edge mobile, and Firefox mobile per D-16/D-17. Each checklist must guide: Console run/chat/session/cancel, Admin card/detail inspection, orientation switch, keyboard/focus where applicable, and no-horizontal-overflow inspection. Include result fields `[pending]` and issue classification fields: blocker / known limitation / follow-up per D-18.</action>
  <verify>
    <automated>test -f .planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md && grep -E "status: partial|Android Chrome|iOS Safari|Edge mobile|Firefox mobile|Console run|Admin|orientation|blocker|known limitation|follow-up" .planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md</automated>
  </verify>
  <done>Human UAT doc exists with explicit browser/device matrix, scripted critical-path steps, pending results, and classification fields.</done>
</task>

<task type="auto">
  <name>Task 3: Cross-link docs and preserve explicit gap language</name>
  <files>docs/phase-15-release-hardening.md, .planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md</files>
  <action>Ensure both docs link to each other and use consistent wording for true-device versus proxy coverage. The release doc must say Playwright `Mobile Safari` is a WebKit proxy and `Mobile Firefox` is a Firefox-engine mobile viewport/user-agent proxy, not proof of every true mobile browser. The UAT doc must tell reviewers how to record unrun or failed items as blocker, known limitation, or follow-up; do not mark true-device coverage passed unless humans actually fill results later.</action>
  <verify>
    <automated>grep -E "WebKit proxy|Firefox.*proxy|true-device|15-HUMAN-UAT|phase-15-release-hardening" docs/phase-15-release-hardening.md .planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md</automated>
  </verify>
  <done>Docs are cross-linked, proxy limitations are explicit, and no pending true-device validation is ambiguously presented as passed.</done>
</task>

</tasks>

<verification>
Automated gate:
`test -f docs/phase-15-release-hardening.md && test -f .planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md && grep -E "Android Chrome|iOS Safari|Edge mobile|Firefox mobile|blocker|known limitation|follow-up" docs/phase-15-release-hardening.md .planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md`
</verification>

<success_criteria>
- MVER-07 is covered by concentrated release docs and a scripted UAT checklist.
- D-15/D-16/D-17/D-18 are implemented exactly.
- Deferred native app/PWA/new features/exhaustive screenshot matrix are not added.
</success_criteria>

<output>
After completion, create `.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-04-SUMMARY.md`.
</output>
