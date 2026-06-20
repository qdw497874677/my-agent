# Pitfalls Research: Mobile-First H5 Adaptation

**Domain:** Mobile-first H5 adaptation of an existing Java/Spring/Vaadin Agent Web Console and Admin Governance app  
**Project:** Pi Java Agent Platform  
**Researched:** 2026-06-20  
**Overall confidence:** HIGH for responsive layout, Vaadin component, mobile browser, accessibility, and Playwright testing pitfalls; MEDIUM for product-specific operator workflow risks until real mobile UAT.

## Executive Guidance

The highest risk in this milestone is treating mobile H5 support as “add media queries to a desktop UI.” The existing Web Console is a desktop-first Vaadin app with dense operational surfaces. A mobile-first milestone must change the presentation structure: shared responsive shell, single-column primary task flow, drawers/details for secondary context, card/detail patterns for dense admin data, and objective mobile browser verification.

The second major risk is broad scope. “Full-site mobile” can become an unbounded redesign unless the milestone keeps backend/runtime/API semantics stable and focuses on existing routes and user/admin behaviors. Mobile adaptation should stay in `pi-agent-adapter-web`, preserve `ConsoleHttpClient` / `EventStreamClient` / public DTO boundaries, and prove behavior through mobile Playwright smoke gates plus targeted Java view tests.

## Critical Pitfalls

### Pitfall 1: Adding responsive CSS without changing desktop-first information architecture

**What goes wrong:**  
The three-column Console and dense Admin pages shrink to phone width, but core actions are hidden, context panels compete with chat/run output, and users still need horizontal scrolling or desktop mental models.

**Warning signs:**
- Fixed-width columns, `min-width` leaks, or page-level horizontal overflow at 360–430px.
- Chat composer, cancel, approvals, or session controls are below unrelated context.
- Admin pages still depend on wide grids/tables.

**Prevention:**
- Make mobile the default layout; enhance for tablet/desktop.
- Put Chat/Run and active approvals/cancel first on mobile.
- Move sessions/catalog/run context to drawers, tabs, accordions, or details panels.
- Convert dense governance data to cards/details on mobile.

**Phase should address:** Mobile baseline/shell and Console refactor phases.

### Pitfall 2: Letting page-level horizontal overflow slip through

**What goes wrong:**  
One Vaadin Grid, long ID, JSON block, or fixed toolbar causes the entire page to scroll sideways. This makes H5 feel broken and often hides critical actions.

**Warning signs:**
- `document.documentElement.scrollWidth > clientWidth` in mobile tests.
- Long run/session/tool/plugin IDs do not wrap or truncate safely.
- JSON/log/code blocks are not internally scroll-contained.

**Prevention:**
- Add no-horizontal-overflow Playwright assertions early.
- Use `min-width: 0`, wrapping, truncation, and internal scroll only for unavoidable monospace/detail blocks.
- Prefer mobile card/detail layouts over full-width tables.

**Phase should address:** Baseline verification and every route conversion phase.

### Pitfall 3: Relying on hover, tiny links, or desktop pointer assumptions

**What goes wrong:**  
Controls are technically visible but hard to tap; hover-only metadata is inaccessible; approvals/cancel actions become unsafe or unreliable on touch devices.

**Warning signs:**
- Critical actions are icon-only without visible label/context.
- Tap targets are below WCAG 2.2 AA minimum or too close together.
- Buttons rely on hover tooltip for risk/context explanation.

**Prevention:**
- Use touch-safe sizing, spacing, visible labels, and explicit details sections.
- Prefer 44px primary touch targets where practical; never rely on hover.
- Add mobile tests for visible/enabled primary actions.

**Phase should address:** Shared mobile design primitives, cards/approvals phase.

### Pitfall 4: Breaking SSE/chat usability on mobile

**What goes wrong:**  
The chat composer disappears behind browser chrome/keyboard, event feed scroll traps the page, or users cannot monitor live output while retaining cancel/approval controls.

**Warning signs:**
- Nested scroll regions prevent returning to composer or latest event.
- Composer is not reachable after events stream in.
- Active run controls are not visible in phone viewport.

**Prevention:**
- Design Console as a mobile task flow: feed + composer + active controls.
- Use constrained scroll regions intentionally; avoid uncontrolled nested scroll traps.
- Keep cancel/approval affordances reachable during active runs.

**Phase should address:** Console mobile IA and runtime card/timeline phases.

### Pitfall 5: Treating Admin Governance as optional on mobile

**What goes wrong:**  
Only Chat/Run becomes mobile-friendly while operators still cannot inspect operations, MCP, plugin, extension, policy, and audit surfaces from a phone.

**Warning signs:**
- Admin routes load but show desktop tables, clipped columns, or unreadable metadata.
- Mobile tests cover `/console` only.
- “Mobile support” excludes governance because it is “admin.”

**Prevention:**
- Enumerate every existing Console/Admin route in requirements and tests.
- Use a shared admin mobile card/detail schema.
- Keep inspect-only governance semantics; avoid adding new admin mutations during adaptation.

**Phase should address:** Admin Governance mobile conversion phase.

### Pitfall 6: Over-expanding into a new frontend stack or native app

**What goes wrong:**  
The milestone becomes a React/Next.js rewrite, Hilla adoption, Capacitor/native app, PWA/offline project, or API redesign instead of H5 adaptation.

**Warning signs:**
- New mobile-specific DTOs or REST endpoints without measured need.
- Separate frontend module appears only for mobile.
- Service worker/offline cache stores governance/audit data.

**Prevention:**
- Keep implementation in Vaadin adapter-web and theme/test assets.
- Preserve public REST/SSE/read-model boundaries.
- Defer native app, push notifications, offline admin, and full PWA to future milestones.

**Phase should address:** Requirements scoping and roadmap constraints.

### Pitfall 7: Mobile browser matrix is claimed but not verified

**What goes wrong:**  
The app works in desktop Chrome narrow viewport but fails in mobile Safari/WebKit, Firefox, keyboard/viewport-height behavior, or orientation changes.

**Warning signs:**
- Tests use only desktop Chromium with resized viewport.
- No touch/mobile context (`isMobile`, `hasTouch`) in E2E.
- No documented real iOS Safari UAT if CI cannot run WebKit.

**Prevention:**
- Add Playwright mobile projects for Chromium/mobile, WebKit/iPhone proxy, Firefox/mobile viewport where supported, and tablet.
- Test portrait and landscape representative sizes.
- Document CI emulation vs real-device/UAT gaps honestly.

**Phase should address:** Baseline test harness and final hardening phase.

### Pitfall 8: Mobile changes regress desktop behavior

**What goes wrong:**  
Mobile-first CSS and component changes make existing desktop Console/Admin E2E fail or degrade the desktop workflow that v1.0 already validated.

**Warning signs:**
- Desktop three-column/two-pane enhancements disappear completely.
- Existing Playwright specs or Java view tests are removed instead of extended.
- CSS is global but not scoped to Pi UI classes/components.

**Prevention:**
- Keep desktop E2E as regression gates.
- Use mobile-first defaults with desktop `min-width` enhancements.
- Scope CSS with `pi-*` class names and stable data attributes.

**Phase should address:** All phases; final verification hardening must include desktop regression.

## Roadmap Prevention Summary

| Risk | Prevent In Phase |
|------|------------------|
| CSS-only adaptation | Baseline shell + Console IA phases |
| Horizontal overflow | Baseline test harness and all route conversion phases |
| Touch/hover issues | Shared primitives and cards/approval phase |
| SSE/chat scroll traps | Console mobile flow phase |
| Admin excluded | Admin Governance conversion phase |
| Scope creep into new stack/native/PWA | Requirements and roadmap constraints |
| Browser matrix unverified | Baseline harness + final hardening |
| Desktop regressions | Every phase with final smoke gate |

## What Not To Do

- Do not introduce React/Next.js/Hilla/native app just for mobile.
- Do not add mobile-only backend APIs unless existing DTOs measurably cannot support mobile summaries.
- Do not claim “Safari supported” solely from desktop Chrome responsive mode.
- Do not make horizontal scrolling tables the default mobile admin pattern.
- Do not cache sensitive Admin Governance/audit data offline.
- Do not expand Agent runtime/tool capabilities inside this UI adaptation milestone.
