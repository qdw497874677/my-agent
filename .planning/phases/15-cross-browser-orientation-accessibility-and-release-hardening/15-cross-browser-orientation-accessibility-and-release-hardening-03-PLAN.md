---
phase: 15-cross-browser-orientation-accessibility-and-release-hardening
plan: 03
type: execute
wave: 2
depends_on:
  - 15-01
files_modified:
  - e2e/phase-15-accessibility-hardening.spec.ts
  - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebPhase15AccessibilityContractTest.java
autonomous: true
requirements:
  - MVER-05
  - MVER-06
must_haves:
  truths:
    - "Keyboard/tablet user can traverse representative shell, Console, runtime/approval, and Admin controls with visible focus."
    - "Important controls are reachable without hover and retain visible labels/semantic state samples."
    - "Reduced-motion users are not forced through unnecessary shell/UI animation."
  artifacts:
    - path: "e2e/phase-15-accessibility-hardening.spec.ts"
      provides: "Representative keyboard, semantic, focus, no-hover, and reduced-motion browser checks"
      contains: "prefers-reduced-motion"
    - path: "pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css"
      provides: "Low-risk presentation-layer focus, reduced-motion, hover fallback, and tablet bridge CSS fixes"
      contains: "prefers-reduced-motion"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebPhase15AccessibilityContractTest.java"
      provides: "Fast static contract checks for Phase 15 accessibility CSS/selectors"
  key_links:
    - from: "e2e/phase-15-accessibility-hardening.spec.ts"
      to: "pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css"
      via: "browser assertions for reduced motion, focus, and hover-independent controls"
      pattern: "reduced-motion|focus-visible"
    - from: "WebPhase15AccessibilityContractTest.java"
      to: "styles.css"
      via: "static CSS contract assertions"
      pattern: "prefers-reduced-motion"
---

<objective>
Deepen Phase 15 accessibility, keyboard, reduced-motion, and no-hover hardening through low-risk presentation-layer changes.

Purpose: Close the final release hardening gap for keyboard/tablet users and mobile browsers without broadening into new component systems, backend APIs, or noisy mandatory axe audits.
Output: Accessibility Playwright spec, targeted `pi-mobile` CSS hardening, and a fast Java static contract test.
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
@docs/phase-11-responsive-shell.md
@docs/phase-13-runtime-cards.md
@docs/phase-14-admin-governance-mobile.md
@e2e/fixtures/mobile-smoke.ts
@e2e/phase-11-shell-navigation.spec.ts
@e2e/phase-13-runtime-cards.spec.ts
@e2e/phase-14-admin-governance-mobile.spec.ts
@pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebMobileBaselineContractTest.java
@pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java

<interfaces>
Established contracts:
```typescript
export async function expectFocusVisible(page: Page, locator: Locator, label = 'focused control'): Promise<void>;
export async function expectTapTargetAtLeast(locator: Locator, minimum = 44, label = 'tap target'): Promise<void>;
```

Existing CSS tokens:
```css
--pi-mobile-tap-target: 44px;
--pi-mobile-focus-color: #2563eb;
--pi-mobile-focus-ring: 0 0 0 3px color-mix(in srgb, var(--pi-mobile-focus-color) 30%, transparent);
```

Phase 15 locked decisions implemented here: D-10, D-11, D-12, D-13, D-14.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add representative keyboard and semantic accessibility checks</name>
  <files>e2e/phase-15-accessibility-hardening.spec.ts</files>
  <behavior>
    - Spec samples keyboard traversal for shared shell drawer/navigation, Console composer/panel/run/cancel controls, runtime Details/approval actions, and Admin card Details/primary controls per D-11.
    - Spec checks visible focus, useful labels or text for representative controls, active/pressed/current state attributes where already established, and no page-level overflow after keyboard interactions.
    - Spec does not introduce mandatory axe-core or exhaustive full-page Tab chains per D-14.
  </behavior>
  <action>Create `phase-15-accessibility-hardening.spec.ts`. Use `page.keyboard.press('Tab')` only for representative samples; prefer direct locators with `expectFocusVisible` for deterministic checks. Verify shell nav uses `data-nav-active` and/or `aria-current` if present; Console panel switcher uses visible labels and `aria-pressed`; Details controls can be focused/opened; approval/admin controls are not hover-only. Stay within existing Vaadin selectors and low-noise Playwright assertions.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-accessibility-hardening.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Accessibility spec is discoverable and covers representative shell, Console, runtime/approval, and Admin keyboard/focus/semantic samples.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add reduced-motion, hover fallback, and tablet bridge CSS hardening</name>
  <files>pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css</files>
  <behavior>
    - `@media (prefers-reduced-motion: reduce)` disables or minimizes shell drawer transitions and any mobile animation/scroll behavior found in the theme.
    - `@media (hover: none)` and/or base styles ensure critical actions do not rely on hover-only affordances.
    - Tablet bridge styles reduce cramped phone-only layout or premature desktop overflow for shell, Console columns, and Admin card density per D-05.
  </behavior>
  <action>Make targeted additions to `styles.css` only. Add a reduced-motion media block for `.pi-shell-drawer` and any transition/animation-bearing mobile primitives. Add hover-independent visible affordances for `[data-action]`, `[data-primary-action]`, `[data-risk-action]`, Details summaries, nav items, and Admin action links if needed. Add a tablet bridge media range (for example 641px-899px) that keeps content bounded, avoids desktop overflow, and improves Console/Admin density without hiding panels incorrectly. Do not redesign components or introduce new APIs per D-13.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebMobileBaselineContractTest,WebResponsiveShellContractTest test</automated>
  </verify>
  <done>Theme contains explicit reduced-motion/no-hover/tablet bridge rules while existing mobile baseline and shell Java contracts still pass.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Add fast Phase 15 accessibility CSS contract test</name>
  <files>pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebPhase15AccessibilityContractTest.java</files>
  <behavior>
    - Test fails if `styles.css` lacks `prefers-reduced-motion`, focus-visible contract, hover fallback selectors, or tablet bridge media rule.
    - Test stays static/no-browser and does not require external services.
    - Test documents D-10 through D-13 as code-level contract names or assertion messages.
  </behavior>
  <action>Create a JUnit test modeled after `WebMobileBaselineContractTest`/`WebResponsiveShellContractTest`. Read the theme CSS file as text and assert the presence of the Phase 15 release-hardening contracts: `prefers-reduced-motion`, `.pi-shell-drawer`, `focus-visible`, `hover: none` or equivalent hover fallback, `[data-risk-action]`/`[data-action]`, and a tablet-range media query. Keep assertions specific enough to prevent accidental removal but not brittle to formatting.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebPhase15AccessibilityContractTest test</automated>
  </verify>
  <done>Fast Java test locks the Phase 15 accessibility CSS contract and passes.</done>
</task>

</tasks>

<verification>
Automated gates:
- `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-accessibility-hardening.spec.ts --project="Mobile Chrome" --list`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebPhase15AccessibilityContractTest,WebMobileBaselineContractTest,WebResponsiveShellContractTest test`
</verification>

<success_criteria>
- D-10/D-11/D-12/D-13 accessibility direction is implemented with representative reliable checks.
- D-14 is respected: axe-core remains optional, not mandatory.
- Changes remain presentation-layer focused in `pi-agent-adapter-web` and `pi-mobile` theme.
</success_criteria>

<output>
After completion, create `.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-03-SUMMARY.md`.
</output>
