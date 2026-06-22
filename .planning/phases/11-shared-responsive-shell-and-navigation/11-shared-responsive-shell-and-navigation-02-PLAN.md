---
phase: 11-shared-responsive-shell-and-navigation
plan: 02
type: execute
wave: 2
depends_on: [11-shared-responsive-shell-and-navigation-01]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageHeader.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageSection.java
  - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java
autonomous: true
requirements: [MH5-04, MH5-05]
must_haves:
  truths:
    - "Touch user can activate shell navigation, drawer controls, links, buttons, refresh controls, expanders, approvals, and cancel controls with mobile-safe target sizing."
    - "Keyboard/tablet user sees consistent focus-visible indicators on navigation and key interactive controls."
    - "Console and Admin pages share route title, content container, status/action placement, and base card/detail styling without feature-specific rewrites."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css"
      provides: "Tap target token, focus-visible styles, shell/nav/page/card/detail/action-row primitives"
      contains: "--pi-mobile-tap-target: 44px"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageHeader.java"
      provides: "Shared page header primitive with title/subtitle/status/action slot convention"
      min_lines: 30
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageSection.java"
      provides: "Shared base card/detail section primitive for later mobile phases"
      min_lines: 25
  key_links:
    - from: "PiResponsiveShell.java"
      to: "styles.css"
      via: "shell CSS classes and data hooks styled by pi-mobile theme"
      pattern: "pi-shell|pi-shell-header|pi-shell-drawer|pi-page"
    - from: "styles.css"
      to: "interactive controls"
      via: "global selector defaults and documented compact opt-out"
      pattern: "--pi-mobile-tap-target"
    - from: "styles.css"
      to: "keyboard focus"
      via: ":focus-visible token styling"
      pattern: ":focus-visible"
---

<objective>
Add shared mobile UI primitives, 44px tap-target defaults, and visible focus styling to the `pi-mobile` theme.

Purpose: Implements D-09 through D-16 by making the new shell and existing key controls touch-safe and focus-visible while creating only foundational page/card/detail primitives for later phases.
Output: Theme tokens/rules and small Vaadin primitives that the shared shell and later mobile plans can reuse.
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
@.planning/phases/11-shared-responsive-shell-and-navigation/11-shared-responsive-shell-and-navigation-01-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java
@pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css

<interfaces>
Shell hooks created by Plan 01 and styled here:
```java
data-shell="pi-responsive-shell"
data-nav="primary"
data-nav-item="..."
data-nav-active="true"
data-page-title="..."
data-shell-drawer-trigger
data-shell-drawer-close
```

Theme contract to implement:
```css
:root {
  --pi-mobile-tap-target: 44px;
  --pi-mobile-focus-ring: ...;
}

.pi-page { ... safe-area-aware content container ... }
.pi-page-header { ... title/status/action placement ... }
.pi-card, .pi-detail, .pi-action-row { ... base primitives only ... }
```

Vaadin/Playwright research notes:
- Vaadin Flow theme CSS should keep full-width layout constraints on `html`, `body`, and `#outlet`.
- Playwright can later assert computed CSS with `toHaveCSS`, focus with `toBeFocused`, and geometry through `boundingBox()`.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add page header and section primitives</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageHeader.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageSection.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java</files>
  <behavior>
    - `PiPageHeader` exposes `.pi-page-header`, `data-page-title`, optional subtitle/status, and optional action slot semantics.
    - `PiPageSection` exposes base `.pi-card` or `.pi-detail` class variants and stable `data-section` hook.
    - `PiResponsiveShell` wraps routed content with `.pi-page` / `.pi-content` and includes a header/status/action-slot convention without forcing each view to rewrite content.
  </behavior>
  <action>Add the page primitive contract test first, then create minimal reusable Vaadin Java components for Phase 11 primitives per D-13 through D-15 until it passes. Keep them adapter-web-only. Wire the shell to use the page header/content container convention. Do not convert Console workbench, runtime tool cards, approval cards, or full Admin lists into final mobile designs; that is deferred by D-16.</action>
  <verify>
    <automated>./mvnw -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest#pagePrimitivesExposeTitleContainerAndActionHooks test</automated>
  </verify>
  <done>Shared page/header/section primitives exist, shell uses the content container convention, and no feature-specific page migration is introduced.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add tap-target and focus-visible theme contract</name>
  <files>pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java</files>
  <behavior>
    - `--pi-mobile-tap-target: 44px` exists in `:root` per D-09.
    - Shell nav items, drawer trigger/close, links, buttons, toggles, details/expanders, primary actions, approvals, cancel controls, and refresh controls get min-size/spacing defaults per D-10.
    - `.pi-compact-control` or equivalent opt-out exists and is documented in CSS comments for deliberate compact variants.
    - `:focus-visible` styling is visible and consistent for shell/nav/key controls per D-11.
  </behavior>
  <action>Add the CSS token/focus/page primitive contract test first, then extend `pi-mobile/styles.css` with tokens and selectors for tap targets, focus rings, shell/header/drawer/nav, page containers, page headers, card/detail surfaces, and action rows until it passes. Use additive CSS classes and data hooks; do not rely on hover-only affordances. Preserve Phase 10 overflow/wrap rules. Since `src/main/frontend/` is ignored, note in the summary that execution may need `git add -f` for this intentional theme file.</action>
  <verify>
    <automated>./mvnw -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest#themeDefinesTapTargetFocusAndPagePrimitiveRules test</automated>
  </verify>
  <done>Theme contains tap-target/focus/page/card/detail/action-row rules and preserves existing no-overflow baseline selectors.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Apply minimum integration to key existing controls</name>
  <files>pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java</files>
  <behavior>
    - Existing `data-primary-action`, `data-action`, and link/button controls inherit tap/focus styles without page rewrites.
    - Drawer open/close controls and nav items meet the 44px floor by CSS class/data hook, not per-control inline styles.
    - No new Console flow, runtime-card, approval UX, or Admin card/detail migration appears in this phase.
  </behavior>
  <action>Add the existing-action-hook inheritance contract test first, then ensure the shell and global CSS integrate with existing Phase 10 selectors (`data-primary-action`, `data-action`, `data-mobile-critical`, `data-admin-surface`) so later Playwright geometry assertions can sample real controls. Add only narrow class names or attributes needed to let CSS apply; avoid broad changes inside business content views.</action>
  <verify>
    <automated>./mvnw -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest#existingActionHooksInheritMobileInteractionContract test</automated>
  </verify>
  <done>Key shell and existing action hooks are covered by global tap/focus rules without expanding into Phase 12-14 redesign scope.</done>
</task>

</tasks>

<verification>
Run the focused contract tests:
```bash
./mvnw -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest test
```
Confirm CSS still contains Phase 10 no-overflow baseline plus Phase 11 tokens.
</verification>

<success_criteria>
- 44px tap target token and defaults exist for shell/nav/key controls.
- Visible focus styles exist for keyboard/tablet navigation and controls.
- Shared page/header/content/card/detail/action-row primitives exist without performing deferred feature rewrites.
- Phase 10 route overflow baseline rules are preserved.
</success_criteria>

<output>
After completion, create `.planning/phases/11-shared-responsive-shell-and-navigation/11-shared-responsive-shell-and-navigation-02-SUMMARY.md`.
</output>
