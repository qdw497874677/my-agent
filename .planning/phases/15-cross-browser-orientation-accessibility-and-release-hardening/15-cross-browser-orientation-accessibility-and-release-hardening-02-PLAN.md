---
phase: 15-cross-browser-orientation-accessibility-and-release-hardening
plan: 02
type: execute
wave: 2
depends_on:
  - 15-01
files_modified:
  - e2e/phase-15-critical-flow-regression.spec.ts
autonomous: true
requirements:
  - MVER-05
  - MVER-06
must_haves:
  truths:
    - "Console critical flow is exercised deeper than route smoke on stable browser projects."
    - "Admin critical inspection flow is exercised deeper than route smoke across converted card/detail surfaces."
    - "Desktop Console/Admin release summary routes still load primary content/actions and avoid horizontal overflow after mobile changes."
  artifacts:
    - path: "e2e/phase-15-critical-flow-regression.spec.ts"
      provides: "Layered critical-flow and desktop regression release gate"
      contains: "Phase 15"
  key_links:
    - from: "e2e/phase-15-critical-flow-regression.spec.ts"
      to: "e2e/fixtures/fake-runtime.ts"
      via: "deterministic no-key Console run/cancel/runtime helpers"
      pattern: "mobileToolApprovalHint|phase13RuntimeCardMatrixHint"
    - from: "e2e/phase-15-critical-flow-regression.spec.ts"
      to: "e2e/fixtures/mobile-smoke.ts"
      via: "overflow, tap target, focus, and viewport helpers from Plan 01"
      pattern: "expectNoPageHorizontalOverflow"
---

<objective>
Create the Phase 15 critical-flow and desktop regression release summary gate for MVER-05/MVER-06.

Purpose: Preserve product confidence by proving more than route load: Console run/chat/session/cancel/runtime-card behavior, Admin card/detail inspection, and desktop route regressions remain covered after mobile-first changes.
Output: `e2e/phase-15-critical-flow-regression.spec.ts`.
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
@docs/phase-12-console-mobile-flow.md
@docs/phase-13-runtime-cards.md
@docs/phase-14-admin-governance-mobile.md
@e2e/fixtures/mobile-smoke.ts
@e2e/fixtures/fake-runtime.ts
@e2e/phase-05-web-console.spec.ts
@e2e/phase-12-console-mobile-flow.spec.ts
@e2e/phase-13-runtime-cards.spec.ts
@e2e/phase-14-admin-governance-mobile.spec.ts

<interfaces>
Existing helper contracts:
```typescript
export function mobileToolApprovalHint(): string;
export function phase13RuntimeCardMatrixHint(): string;
export async function expectNoPageHorizontalOverflow(page: Page, tolerance = 1): Promise<void>;
export async function expectTapTargetAtLeast(locator: Locator, minimum = 44, label = 'tap target'): Promise<void>;
export async function expectFocusVisible(page: Page, locator: Locator, label = 'focused control'): Promise<void>;
```

Phase 15 locked decisions implemented here: D-02, D-04, D-06, D-07, D-08, D-09.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add stable Console critical path release checks</name>
  <files>e2e/phase-15-critical-flow-regression.spec.ts</files>
  <behavior>
    - Console test opens `/console`, selects General Agent, submits a deterministic fake-runtime prompt, observes event feed growth, inspects runtime/tool/approval cards where present, opens Sessions, and cancels or accepts terminal status.
    - Test samples portrait and tablet/desktop-bridge dimensions, not every landscape/browser permutation, matching D-04/D-07.
    - Assertions use stable `data-*` hooks and no screenshot baseline.
  </behavior>
  <action>Create the spec and implement a Console `describe` block by reusing patterns from Phase 12/13. Use `mobileToolApprovalHint()` or `phase13RuntimeCardMatrixHint()` to trigger deterministic no-key runtime/tool/approval surfaces. Include no-overflow checks after panel changes and after event/detail expansion. Keep timing-sensitive run progression on stable project(s) by using test naming/annotations that can be listed for all projects but executed primarily where stable; do not silently skip browser-family smoke coverage per D-08.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Console critical-flow tests are discoverable and cover chat/run/session/cancel/runtime inspection beyond route smoke.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add Admin card/detail inspection release checks</name>
  <files>e2e/phase-15-critical-flow-regression.spec.ts</files>
  <behavior>
    - Admin release check opens representative converted surfaces: landing/overview, registry, operations, policy decisions, audits, and approvals or a representative subset with explicit route names.
    - Each sampled route expands a visible Details/card control, checks redaction markers are absent, verifies focus/tap usability on a control, and checks no horizontal overflow.
    - Admin coverage is structural selector-based, not screenshot-based.
  </behavior>
  <action>Add an Admin `describe` block to the same spec using the route selector matrix from `phase-14-admin-governance-mobile.spec.ts`. It should inspect card/detail density for tablet bridge and mobile portrait where cheap. Reuse redaction markers and first-visible-control logic from Phase 14. This implements D-02 deeper Admin coverage while staying within existing public Admin routes and avoiding new DTO/API/backend work.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Admin critical inspection tests are discoverable and prove representative card/detail inspection, redaction, focus/tap, and no-overflow behavior.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Add desktop release summary regression checks</name>
  <files>e2e/phase-15-critical-flow-regression.spec.ts</files>
  <behavior>
    - Desktop Chrome summary check preserves the existing `phase-05-web-console.spec.ts` baseline by not modifying it.
    - New Phase 15 summary opens key desktop Console and Admin routes, verifies primary content/actions, and checks no page-level horizontal overflow.
    - The spec is discoverable under the `chromium` Playwright project for MVER-06.
  </behavior>
  <action>Add a desktop regression `describe` block in the same spec. It should not replace or weaken `e2e/phase-05-web-console.spec.ts` per D-06; instead it provides a compact Phase 15 release summary gate for `/console` and representative Admin routes using existing selectors. Keep assertions deterministic/no-key and structural. If any desktop-only issue is found, prefer low-risk selector/CSS fixes in Plan 03 rather than changing public APIs.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="chromium" --list</automated>
  </verify>
  <done>Desktop release summary tests list under `chromium`, existing Phase 05 desktop spec remains untouched, and MVER-06 has a Phase 15-specific gate.</done>
</task>

</tasks>

<verification>
Automated gates:
- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="Mobile Chrome" --list`
- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="chromium" --list`
</verification>

<success_criteria>
- D-02 deeper Console/Admin critical paths are represented.
- D-06 desktop regression baseline is preserved and supplemented.
- D-07 layered matrix is respected without forcing timing-sensitive flows into every browser family.
- No deferred screenshot regression or exhaustive permutation suite is introduced.
</success_criteria>

<output>
After completion, create `.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-02-SUMMARY.md`.
</output>
