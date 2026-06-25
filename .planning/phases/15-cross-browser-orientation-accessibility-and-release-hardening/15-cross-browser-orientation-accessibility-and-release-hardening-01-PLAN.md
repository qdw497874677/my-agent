---
phase: 15-cross-browser-orientation-accessibility-and-release-hardening
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - e2e/fixtures/mobile-smoke.ts
  - e2e/phase-15-orientation-release-smoke.spec.ts
autonomous: true
requirements:
  - MVER-05
must_haves:
  truths:
    - "Maintainer can run one Phase 15 browser spec that covers all eight Console/Admin routes at portrait, landscape, and tablet viewports."
    - "Mobile/tablet user can open every existing route with shared shell navigation, primary content or action visibility, and no page-level horizontal overflow."
    - "Landscape coverage uses in-test viewport switching instead of adding dedicated Playwright landscape projects."
  artifacts:
    - path: "e2e/fixtures/mobile-smoke.ts"
      provides: "Reusable viewport/orientation helpers and route baseline assertions for Phase 15"
      exports: ["expectNoPageHorizontalOverflow", "expectPrimaryContentOrActionVisible"]
    - path: "e2e/phase-15-orientation-release-smoke.spec.ts"
      provides: "All-route portrait/landscape/tablet release smoke matrix"
      contains: "page.setViewportSize"
  key_links:
    - from: "e2e/phase-15-orientation-release-smoke.spec.ts"
      to: "e2e/fixtures/mobile-smoke.ts"
      via: "shared no-overflow and selector helpers"
      pattern: "expectNoPageHorizontalOverflow"
    - from: "e2e/phase-15-orientation-release-smoke.spec.ts"
      to: "playwright.config.ts"
      via: "existing project matrix; no new landscape projects"
      pattern: "test.describe"
---

<objective>
Create the Phase 15 all-route orientation release smoke gate for MVER-05.

Purpose: Prove final mobile H5 routes survive representative portrait, landscape, and tablet viewport changes across the existing Playwright browser projects without adding a costly landscape project explosion.
Output: Shared viewport helper contract plus `e2e/phase-15-orientation-release-smoke.spec.ts`.
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
@docs/phase-10-mobile-baseline.md
@docs/phase-11-responsive-shell.md
@docs/phase-14-admin-governance-mobile.md
@playwright.config.ts
@e2e/fixtures/mobile-smoke.ts
@e2e/phase-10-mobile-route-smoke.spec.ts
@e2e/phase-11-shell-navigation.spec.ts

<interfaces>
Existing route/test contracts the executor should reuse:

From `playwright.config.ts`:
```typescript
projects: ['chromium', 'Mobile Chrome', 'Mobile Safari', 'Mobile Firefox', 'Tablet']
```

From `e2e/fixtures/mobile-smoke.ts`:
```typescript
export type MobileSmokeRoute = {
  path: string;
  routeName: string;
  primaryContent?: MobileSmokeSelector[];
  primaryActions?: MobileSmokeSelector[];
};
export async function expectNoPageHorizontalOverflow(page: Page, tolerance = 1): Promise<void>;
export async function expectStableSelectorVisible(page: Page, selector: string): Promise<Locator>;
export async function expectPrimaryContentOrActionVisible(page: Page, route: Pick<MobileSmokeRoute, 'routeName' | 'primaryContent' | 'primaryActions'>): Promise<void>;
```

Phase 15 locked decisions implemented here: D-01, D-03, D-04, D-05, D-07, D-09.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add reusable Phase 15 viewport/orientation helpers</name>
  <files>e2e/fixtures/mobile-smoke.ts</files>
  <behavior>
    - Helper exposes named portrait, landscape, and tablet viewport sizes suitable for representative Phase 15 coverage.
    - Helper switches viewport with `page.setViewportSize(...)`, waits for route stability, and does not modify `playwright.config.ts` projects per D-03.
    - Helper can assert shared shell marker, route marker, primary content/action, and no page-level horizontal overflow after each switch.
  </behavior>
  <action>Extend `mobile-smoke.ts` with small exported Phase 15 helper types/functions only. Add a `Phase15ViewportCase` type and named cases such as phone portrait, phone landscape, and tablet bridge. Add an assertion helper that accepts a `MobileSmokeRoute`, sets viewport, navigates or revalidates the route, then checks `[data-shell="pi-responsive-shell"]`, `[data-route="..."]`, `expectPrimaryContentOrActionVisible`, and `expectNoPageHorizontalOverflow`. Keep helpers generic and deterministic; do not add axe-core, screenshot comparison, new Playwright projects, or app code.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>`mobile-smoke.ts` exports Phase 15 viewport cases/helper(s), existing Phase 10 list gate still discovers tests, and no dedicated landscape project is added.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create all-route portrait/landscape/tablet orientation smoke spec</name>
  <files>e2e/phase-15-orientation-release-smoke.spec.ts</files>
  <behavior>
    - All eight routes from D-01 are represented: `/console`, `/admin/governance`, `/admin/governance/overview`, `/admin/governance/registry`, `/admin/governance/operations`, `/admin/governance/policy-decisions`, `/admin/governance/audits`, `/admin/governance/approvals`.
    - Each route is validated in portrait, landscape, and tablet viewport cases with no page-level horizontal overflow.
    - Landscape checks include shell/drawer navigation markers, route primary content/action visibility, and critical control visibility per D-04.
  </behavior>
  <action>Create `phase-15-orientation-release-smoke.spec.ts` using the existing route metadata style from Phase 10/11. Reuse stable `data-*` selectors from prior specs/docs. For every route and viewport case, call the new helper; for landscape, also sample drawer trigger/nav visibility on phone-width projects when visible and ensure at least one critical control/action selector remains usable. Do not make screenshots the pass/fail contract per D-09; screenshots may remain Playwright failure artifacts only.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-orientation-release-smoke.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Spec lists deterministic tests for all eight routes across portrait/landscape/tablet cases and uses in-test viewport switching per D-03.</done>
</task>

<task type="auto">
  <name>Task 3: Prove the layered browser matrix discovers Phase 15 orientation coverage</name>
  <files>e2e/phase-15-orientation-release-smoke.spec.ts</files>
  <action>Run and, if needed, minimally adjust test annotations/names so the new spec is discoverable under the existing core projects: `chromium`, `Mobile Chrome`, `Mobile Safari`, `Mobile Firefox`, and `Tablet` per D-07. Keep the deepest assertions structural and low-noise. If a project has an unavoidable list/discovery issue, fix the test shape rather than skipping; runtime/browser execution failures will be handled by later execution/verification and documented by Plan 04 if environment-specific per D-08.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-orientation-release-smoke.spec.ts --project="chromium" --project="Mobile Chrome" --project="Mobile Safari" --project="Mobile Firefox" --project="Tablet" --list</automated>
  </verify>
  <done>One command lists Phase 15 orientation smoke coverage under every existing Playwright project without adding new projects.</done>
</task>

</tasks>

<verification>
Automated gate: `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-orientation-release-smoke.spec.ts --project="chromium" --project="Mobile Chrome" --project="Mobile Safari" --project="Mobile Firefox" --project="Tablet" --list`.
</verification>

<success_criteria>
- MVER-05 has all-route portrait/landscape/tablet smoke coverage.
- D-01/D-03/D-04/D-05/D-07/D-09 are traceable in the spec/helper implementation.
- No deferred full screenshot baseline, exhaustive permutation suite, or new landscape project matrix is introduced.
</success_criteria>

<output>
After completion, create `.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-01-SUMMARY.md`.
</output>
