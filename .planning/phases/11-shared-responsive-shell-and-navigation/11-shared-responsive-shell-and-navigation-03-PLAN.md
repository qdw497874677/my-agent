---
phase: 11-shared-responsive-shell-and-navigation
plan: 03
type: execute
wave: 3
depends_on: [11-shared-responsive-shell-and-navigation-01, 11-shared-responsive-shell-and-navigation-02]
files_modified:
  - e2e/fixtures/mobile-smoke.ts
  - e2e/phase-11-shell-navigation.spec.ts
  - docs/phase-10-mobile-baseline.md
  - docs/phase-11-responsive-shell.md
autonomous: true
requirements: [MH5-02, MH5-04, MH5-05]
must_haves:
  truths:
    - "Browser gate verifies every current Console/Admin route is reachable through shared shell navigation on representative phone/tablet/desktop projects."
    - "Browser gate verifies drawer open/close, active nav state, page title visibility, primary content/action visibility, and no page-level horizontal overflow."
    - "Browser gate samples real controls for 44px target sizing, visible focus, and drawer close focus return."
  artifacts:
    - path: "e2e/phase-11-shell-navigation.spec.ts"
      provides: "Playwright shell/navigation/touch/focus gate for all current routes"
      min_lines: 120
    - path: "e2e/fixtures/mobile-smoke.ts"
      provides: "Shared helpers for tap target geometry, focus visibility, and shell navigation assertions"
      exports: ["expectTapTargetAtLeast", "expectFocusVisible"]
    - path: "docs/phase-11-responsive-shell.md"
      provides: "Operator/developer documentation for Phase 11 shell/nav contract and verification commands"
      min_lines: 40
  key_links:
    - from: "phase-11-shell-navigation.spec.ts"
      to: "PiResponsiveShell.java"
      via: "stable `data-shell`, `data-nav`, `data-nav-item`, `data-page-title` selectors"
      pattern: "data-shell|data-nav-item|data-page-title"
    - from: "phase-11-shell-navigation.spec.ts"
      to: "mobile-smoke.ts"
      via: "shared no-overflow/tap/focus helpers"
      pattern: "expectTapTargetAtLeast|expectFocusVisible|expectNoPageHorizontalOverflow"
    - from: "docs/phase-11-responsive-shell.md"
      to: "ROADMAP Phase 11 success criteria"
      via: "documented commands and known local limitations"
      pattern: "Phase 11"
---

<objective>
Add the Phase 11 Playwright browser navigation gate and documentation for the shared responsive shell.

Purpose: Implements D-18 through D-20 by verifying real drawer/nav behavior, active route title state, touch target geometry, focus visibility/return, and no page-level overflow across the representative Phase 10 browser matrix.
Output: A reusable Playwright spec/helper extension and Phase 11 shell documentation.
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
@.planning/phases/11-shared-responsive-shell-and-navigation/11-CONTEXT.md
@.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-03-SUMMARY.md
@.planning/phases/11-shared-responsive-shell-and-navigation/11-shared-responsive-shell-and-navigation-01-SUMMARY.md
@.planning/phases/11-shared-responsive-shell-and-navigation/11-shared-responsive-shell-and-navigation-02-SUMMARY.md
@e2e/fixtures/mobile-smoke.ts
@e2e/phase-10-mobile-route-smoke.spec.ts
@playwright.config.ts
@docs/phase-10-mobile-baseline.md

<interfaces>
Existing Phase 10 Playwright patterns:
```typescript
await page.goto(route.path, { waitUntil: 'domcontentloaded' });
await expectStableSelectorVisible(page, `[data-route="${route.routeName}"]`);
await expectPrimaryContentOrActionVisible(page, route);
await expectNoPageHorizontalOverflow(page);
```

New shell selectors from Plans 01-02:
```typescript
'[data-shell="pi-responsive-shell"]'
'[data-shell-drawer-trigger]'
'[data-shell-drawer-close]'
'[data-nav="primary"]'
'[data-nav-item="admin/governance/registry"]'
'[data-nav-active="true"]'
'[data-page-title]'
```

Playwright research notes:
- Use `locator.boundingBox()` for 44px geometry assertions.
- Use `await expect(locator).toBeFocused()` for focus return.
- Use `await expect(locator).toHaveCSS(...)` or computed-style evaluation for focus-visible signal when browser support differs.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Extend mobile smoke helpers for shell touch and focus assertions</name>
  <files>e2e/fixtures/mobile-smoke.ts</files>
  <behavior>
    - `expectTapTargetAtLeast(locator, 44)` fails if visible element bounding box width or height is below 44px unless an explicit compact opt-out selector is present.
    - `expectFocusVisible(page, locator)` focuses a locator and verifies either outline/box-shadow/border focus style changes or a documented focus-visible class/token signal.
    - Existing Phase 10 helpers remain backward-compatible.
  </behavior>
  <action>Add typed helper functions for tap target geometry and focus visibility. Keep helpers generic and reusable for Phase 12-15. Do not remove or rename existing Phase 10 exports. Include assertion messages that name the sampled selector/control for easy debugging.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Helper file exports new tap/focus assertions while Phase 10 route smoke spec still loads/lists.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create all-route shell navigation browser gate</name>
  <files>e2e/phase-11-shell-navigation.spec.ts</files>
  <behavior>
    - For each of the eight current routes, direct navigation shows shell, route marker, active nav item, page title, primary content/action, and no page-level horizontal overflow.
    - On Mobile Chrome, drawer trigger opens nav, each nav item reaches its route, active state updates, and drawer close returns focus to trigger.
    - Representative sampled controls meet 44px tap target floor: drawer trigger, drawer close, at least one nav item, at least one primary action or key existing route action.
    - Focus-visible is sampled for drawer trigger, nav item, and at least one page action/control.
  </behavior>
  <action>Create `e2e/phase-11-shell-navigation.spec.ts` using the Phase 10 route table style. Cover all current Console/Admin routes from D-18. Reuse helper functions from `mobile-smoke.ts`. Keep tests deterministic/no-key and avoid mutating actions: focus/read controls and navigate, but do not start runs, approve/reject, refresh plugins/MCP, or cancel real runs. Use stable `data-*` hooks, not brittle text-only selectors, per D-17.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-11-shell-navigation.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Phase 11 spec lists all expected shell/navigation tests for Mobile Chrome and is ready for full browser execution in CI/dev environments.</done>
</task>

<task type="auto">
  <name>Task 3: Document Phase 11 shell contract and verification commands</name>
  <files>docs/phase-11-responsive-shell.md, docs/phase-10-mobile-baseline.md</files>
  <action>Create `docs/phase-11-responsive-shell.md` documenting: shared shell URL/route list, Console/Admin product grouping, selector contract, tap/focus contract, Java contract command, Playwright command, non-mutating/no-key behavior, and deferred Phase 12-15 items. Update `docs/phase-10-mobile-baseline.md` with a short handoff note that Phase 11 supersedes route-only navigation checks with shell/nav/touch/focus gates while keeping the same representative matrix. Do not add real-device/UAT release checklist here; that remains Phase 15.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-11-shell-navigation.spec.ts --list</automated>
  </verify>
  <done>Documentation records how to run and interpret Phase 11 shell/navigation gates and preserves Phase 15 UAT boundary.</done>
</task>

</tasks>

<verification>
Run list checks locally:
```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-11-shell-navigation.spec.ts --project="Mobile Chrome" --list
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-11-shell-navigation.spec.ts --list
```
If the local Vaadin/browser environment is stable, also run:
```bash
npm run e2e -- e2e/phase-11-shell-navigation.spec.ts --project="Mobile Chrome"
```
If full browser execution hits the known Vaadin dev-mode timeout documented in Phase 10, record it in the summary without weakening the spec/list gate.
</verification>

<success_criteria>
- Phase 11 Playwright spec covers all current Console/Admin routes through the shared shell.
- Drawer open/close, active nav state, page title, primary content/action visibility, no-overflow, 44px target samples, visible focus, and focus return are verified.
- Documentation explains commands and local/CI expectations.
- No deferred Console flow, runtime card, approval UX, full Admin card conversion, or release hardening scope is implemented.
</success_criteria>

<output>
After completion, create `.planning/phases/11-shared-responsive-shell-and-navigation/11-shared-responsive-shell-and-navigation-03-SUMMARY.md`.
</output>
