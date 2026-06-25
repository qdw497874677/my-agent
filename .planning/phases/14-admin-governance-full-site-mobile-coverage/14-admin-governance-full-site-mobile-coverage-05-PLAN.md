---
phase: 14-admin-governance-full-site-mobile-coverage
plan: 05
type: execute
wave: 3
depends_on:
  - 14-admin-governance-full-site-mobile-coverage-02
  - 14-admin-governance-full-site-mobile-coverage-03
  - 14-admin-governance-full-site-mobile-coverage-04
files_modified:
  - e2e/phase-14-admin-governance-mobile.spec.ts
  - e2e/fixtures/mobile-smoke.ts
  - docs/phase-14-admin-governance-mobile.md
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java
autonomous: true
requirements:
  - MVER-04
  - MADM-01
  - MADM-02
  - MADM-03
  - MADM-04
  - MADM-05
  - MADM-06
  - MADM-07
must_haves:
  truths:
    - "Automated Admin mobile E2E opens landing, overview, registry, operations, policy decisions, audits, and approvals."
    - "Automated Admin mobile E2E verifies card/detail content for overview, registry, operations, MCP, plugin, extension, policy, and audit pages."
    - "Browser verification expands Details, checks redaction, samples tap/focus behavior, and asserts no page-level horizontal overflow."
  artifacts:
    - path: "e2e/phase-14-admin-governance-mobile.spec.ts"
      provides: "MVER-04 Admin mobile browser gate"
      contains: "Phase 14 Admin Governance mobile coverage"
    - path: "docs/phase-14-admin-governance-mobile.md"
      provides: "Phase 14 selector and verification documentation"
      contains: "MVER-04"
  key_links:
    - from: "e2e/phase-14-admin-governance-mobile.spec.ts"
      to: "e2e/fixtures/mobile-smoke.ts"
      via: "no-overflow, tap target, focus helpers"
      pattern: "expectNoPageHorizontalOverflow|expectTapTargetAtLeast|expectFocusVisible"
    - from: "docs/phase-14-admin-governance-mobile.md"
      to: "Phase 14 plan outputs"
      via: "selector contract documentation"
      pattern: "data-admin-card|data-admin-details|MVER-04"
---

<objective>
Add the MVER-04 full Admin mobile browser gate and Phase 14 operator/developer documentation.

Purpose: Plans 01-04 convert Admin surfaces; this final plan proves the complete mobile Admin route set is discoverable through automated no-key verification and records the selector contract for Phase 15.
Output: Playwright Admin mobile spec, any helper extension needed for Details expansion, contract test/doc updates.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@.planning/STATE.md
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-CONTEXT.md
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-VALIDATION.md
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-02-SUMMARY.md
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-03-SUMMARY.md
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-04-SUMMARY.md
@e2e/fixtures/mobile-smoke.ts
@e2e/phase-11-shell-navigation.spec.ts
@e2e/phase-13-runtime-cards.spec.ts
@docs/phase-11-responsive-shell.md
@docs/phase-13-runtime-cards.md

<interfaces>
Existing mobile helper functions:
```ts
expectNoPageHorizontalOverflow(page, tolerance?)
expectStableSelectorVisible(page, selector)
expectTapTargetAtLeast(locator, minimum?, label?)
expectFocusVisible(page, locator, label?)
```

Admin routes from Phase 11:
- `/admin/governance` → `[data-route="admin-governance"]`
- `/admin/governance/overview` → `[data-route="admin-governance-overview"]`
- `/admin/governance/registry` → `[data-route="admin-registry-status"]`
- `/admin/governance/operations` → `[data-route="admin-operations"]`
- `/admin/governance/policy-decisions` → `[data-route="admin-policy-decisions"]`
- `/admin/governance/audits` → `[data-route="admin-audit-summaries"]`
- `/admin/governance/approvals` → `[data-route="admin-approval-queue"]`

Phase 14 locked decisions implemented here: D-01, D-08, D-18, D-19, D-20, D-21, D-22.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add Admin mobile full-route Playwright list gate</name>
  <files>e2e/phase-14-admin-governance-mobile.spec.ts, e2e/fixtures/mobile-smoke.ts</files>
  <read_first>
    - e2e/fixtures/mobile-smoke.ts
    - e2e/phase-11-shell-navigation.spec.ts
    - e2e/phase-13-runtime-cards.spec.ts
    - .planning/phases/14-admin-governance-full-site-mobile-coverage/14-CONTEXT.md
  </read_first>
  <behavior>
    - Test 1: spec defines a route matrix for landing, overview, registry, operations, policy decisions, audits, and approvals.
    - Test 2: every route calls `expectNoPageHorizontalOverflow(page)` after loading and after Details expansion where applicable.
    - Test 3: overview verifies `data-admin-overview-card`; registry verifies `data-mcp-server-card`, `data-mcp-tool-card`, `data-plugin-card`, `data-extension-source-card`; operations verifies `data-operations-card`; policy verifies `data-policy-decision-card`; audit verifies `data-audit-card`; approvals verifies existing `ApprovalCard`/approval route selectors.
    - Test 4: first visible Admin Details/touch control passes 44px tap target and focus-visible checks.
  </behavior>
  <action>Create `e2e/phase-14-admin-governance-mobile.spec.ts` with describe title `Phase 14 Admin Governance mobile coverage`. Use only public Admin routes and existing fake/no-key fixtures. Define an array of route cases with exact paths and selectors listed in the context. For each route: `page.goto(path, { waitUntil: 'domcontentloaded' })`, assert `[data-route="..."]`, assert one or more card/detail selectors, call `expectNoPageHorizontalOverflow(page)`. Add helper code in the spec or `mobile-smoke.ts` to expand the first visible `vaadin-details[data-admin-details], [data-admin-details] vaadin-details, [data-expandable="true"]` control when present; then assert `body` does not contain `sk-test-secret`, `PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK`, `PI_PHASE8_FAKE_SECRET_DO_NOT_LEAK`, `rawSecret`, or `raw-token-value`. Use `expectTapTargetAtLeast` and `expectFocusVisible` on the first visible Admin Details/control/action. Keep the required local gate as Playwright `--list` so it is deterministic without launching a live browser when requested, per D-18/D-20/D-22. Do not add screenshot assertions.</action>
  <acceptance_criteria>
    - `e2e/phase-14-admin-governance-mobile.spec.ts` contains `Phase 14 Admin Governance mobile coverage`.
    - The spec contains all seven paths: `/admin/governance`, `/admin/governance/overview`, `/admin/governance/registry`, `/admin/governance/operations`, `/admin/governance/policy-decisions`, `/admin/governance/audits`, `/admin/governance/approvals`.
    - The spec contains selectors `data-mcp-server-card`, `data-plugin-card`, `data-extension-source-card`, `data-policy-decision-card`, and `data-audit-card`.
    - The spec imports or uses `expectNoPageHorizontalOverflow`, `expectTapTargetAtLeast`, and `expectFocusVisible`.
  </acceptance_criteria>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-14-admin-governance-mobile.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>MVER-04 has an executable no-key Playwright list gate covering the complete Admin mobile route/card/detail surface.</done>
</task>

<task type="auto">
  <name>Task 2: Document Phase 14 Admin mobile selector and verification contract</name>
  <files>docs/phase-14-admin-governance-mobile.md, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java</files>
  <read_first>
    - docs/phase-11-responsive-shell.md
    - docs/phase-13-runtime-cards.md
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java
    - .planning/REQUIREMENTS.md
  </read_first>
  <action>Create `docs/phase-14-admin-governance-mobile.md` documenting: scope and non-scope, MADM-01 through MADM-07 traceability, MVER-04 route coverage, selector contract (`data-admin-card`, `data-admin-details`, `data-admin-overview-card`, `data-admin-registry-section`, `data-mcp-server-card`, `data-mcp-tool-card`, `data-plugin-card`, `data-plugin-capability-card`, `data-extension-source-card`, `data-extension-capability-card`, `data-operations-card`, `data-operations-warning-card`, `data-policy-decision-card`, `data-audit-card`, `data-status-severity`), redaction/detail rules, and verification commands. Include required commands exactly: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest,McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest,AdminOperationsViewTest test` and `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-14-admin-governance-mobile.spec.ts --project="Mobile Chrome" --list`. Update `WebResponsiveShellContractTest` or an existing route contract test only if needed to assert the docs/link or route matrix includes Admin Phase 14 paths; do not broaden to Phase 15 cross-browser/orientation/real-device UAT.</action>
  <acceptance_criteria>
    - `docs/phase-14-admin-governance-mobile.md` contains `MVER-04`, `MADM-01`, `MADM-07`, and every selector listed in the action.
    - `docs/phase-14-admin-governance-mobile.md` contains both required verification commands exactly.
    - `docs/phase-14-admin-governance-mobile.md` states real-device/UAT and broad cross-browser/orientation hardening remain Phase 15.
  </acceptance_criteria>
  <verify>
    <automated>test -f docs/phase-14-admin-governance-mobile.md && grep -q "MVER-04" docs/phase-14-admin-governance-mobile.md && grep -q "data-admin-card" docs/phase-14-admin-governance-mobile.md && grep -q "phase-14-admin-governance-mobile.spec.ts" docs/phase-14-admin-governance-mobile.md</automated>
  </verify>
  <done>Phase 14 Admin mobile coverage is documented with selectors, commands, traceability, and Phase 15 handoffs.</done>
</task>

</tasks>

<verification>
Required final local gate:
`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest,McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest,AdminOperationsViewTest test && PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-14-admin-governance-mobile.spec.ts --project="Mobile Chrome" --list`
</verification>

<success_criteria>
- MVER-04 is covered by an executable Admin mobile E2E spec.
- Full Admin route set is represented with stable selectors and no screenshot dependency.
- Documentation records selectors, commands, redaction rules, and Phase 15 handoffs.
</success_criteria>

<output>
After completion, create `.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-05-SUMMARY.md`
</output>
