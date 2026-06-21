# Phase 11: Shared Responsive Shell and Navigation - Context

**Gathered:** 2026-06-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 11 delivers the shared responsive shell and navigation foundation for the existing Vaadin Console and Admin Governance surfaces. Mobile users must be able to move through all existing Console/Admin sections through a compact, touch-friendly shell; touch and keyboard/tablet users must get mobile-safe target sizes, visible focus, and usable navigation focus order; and all routes must begin sharing consistent title, content container, status/action placement, and base card/detail styling.

This phase does **not** implement the full Console mobile-first flow, runtime/tool/approval card UX, full Admin Governance card/detail migration, new backend/runtime/model/tool capabilities, a React/Next/Hilla rewrite, a native app, or mobile-only backend/API forks. Those remain Phase 12-15 or out of scope.

</domain>

<decisions>
## Implementation Decisions

### Shell Structure
- **D-01:** Use a **single shared Vaadin RouterLayout/shell** for all existing Console and Admin Governance routes, rather than separate Console/Admin shells or per-view header composition.
- **D-02:** Preserve Console and Admin as distinct product areas inside the shared shell: top-level navigation should expose **Console** and **Admin** as the two global entry areas, with Admin Governance sub-navigation below Admin.
- **D-03:** Replace or converge the current layout placeholders/zombie layouts. `/admin/governance` should remain an Admin landing route, but it should not continue as a pseudo-layout. The route-less `MainConsoleLayout` should be retired, replaced, or folded into the new shared shell rather than left as an unused competing pattern.
- **D-04:** Keep shell implementation in `pi-agent-adapter-web` and UI/theme/test surfaces only. Do not change Domain/App/client DTOs and do not add viewport-specific backend APIs, `/mobile/*` endpoints, or mobile-only server behavior for this phase.

### Mobile Navigation Pattern
- **D-05:** Use a **compact header + hamburger drawer** as the primary phone navigation pattern. This should fit the eight current routes and leave page content room on small screens.
- **D-06:** Render Admin Governance sub-navigation as grouped items inside the drawer: Admin should expose Overview, Registry, Operations, Policy Decisions, Audits, and Approvals as reachable section routes.
- **D-07:** Use one responsive source of truth for navigation. The same route/nav registry should drive phone drawer navigation and wider tablet/desktop presentation, where the shell may show an expanded or fixed navigation surface instead of a collapsed drawer.
- **D-08:** Show the current route title in the shell/header area and provide visible active-state highlighting for the corresponding nav item. Admin sub-pages should have clear second-level titles or labels.

### Touch and Focus Contract
- **D-09:** Standardize a 44px minimum touch target floor through a theme token such as `--pi-mobile-tap-target: 44px` for shell/nav items and key interactive controls.
- **D-10:** Implement tap-target sizing primarily through global `pi-mobile` CSS defaults with explicit exceptions. Links, buttons, toggles, drawer triggers, nav items, primary actions, refresh controls, details expanders, approvals, and cancel controls should get mobile-safe min-size/spacing unless a component deliberately opts out with a documented/tested compact variant.
- **D-11:** Add global `:focus-visible` styling tokens in the `pi-mobile` theme. Navigation, drawer triggers, links, buttons, details/expanders, and primary actions should have consistent visible focus indicators.
- **D-12:** Drawer keyboard behavior should meet a **basic usable** Phase 11 contract: opening the drawer exposes the trigger/close/nav items in a usable focus order, and closing the drawer returns focus to the drawer trigger. A full complex focus trap is not required in Phase 11 unless the chosen Vaadin component provides it naturally; broader accessibility hardening remains Phase 15.

### Shared UI Primitives
- **D-13:** Phase 11 should create the shared shell primitives needed by later mobile phases: route title, content container, status/action slot, and base card/detail surface styling.
- **D-14:** Introduce a unified content container pattern, e.g. `.pi-page` / `.pi-content`, with variants such as narrow/default/wide or admin-dense. Phones should get full-width safe-area-aware padding; wider screens may constrain readable content while still allowing dense Admin variants.
- **D-15:** Introduce a shared page header primitive or convention with title, optional subtitle/status, and primary action slot. Mobile layout should stack these affordances safely; wider layout may align them horizontally.
- **D-16:** Do **not** perform broad business-content rewrites in Phase 11. Provide base `card`, `detail`, `list`, and `action-row` classes/components and do only minimum integration necessary to satisfy consistent title/container/status/action/card styling. Full Console flow redesign belongs to Phase 12; full runtime/tool/approval card UX belongs to Phase 13; full Admin card/detail migration belongs to Phase 14.

### Selector and Test Contract
- **D-17:** Fully standardize shell/nav/page selector hooks by extending the Phase 10 `data-*` contract. Required stable hooks should include `data-shell`, `data-nav`, `data-nav-item`, `data-nav-active`, `data-page-title`, and `data-primary-action` where applicable.
- **D-18:** Add a Phase 11 browser navigation gate covering all current Console/Admin routes. On representative phone/tablet/desktop projects, tests should verify shell visibility, drawer open/close, each nav item reaching its route, active nav state, page title visibility, primary content/action visibility, and no page-level horizontal overflow.
- **D-19:** Add focused touch/focus assertions instead of only visual smoke. Playwright should sample key controls such as nav items, drawer trigger, primary actions, and key buttons/expanders for 44px target sizing, visible focus, and drawer close focus return.
- **D-20:** Use both fast Java contract tests and Playwright tests. Java contract tests should validate theme/shell/data-hook wiring quickly; Playwright should verify real drawer/nav behavior, responsive behavior, touch-target geometry, focus visibility/return, and reuse Phase 10 mobile smoke helpers.

### Folded Todos
- No pending todos matched Phase 11 scope.

### the agent's Discretion
- Exact Java class names, package names, CSS class names, and whether the shell uses Vaadin `AppLayout` or a custom free-component `RouterLayout` are planner/researcher discretion, provided the decisions above hold and licensing/dependency implications are checked.
- Exact breakpoint values and whether tablet/desktop show a fixed sidebar, expanded drawer, or header navigation are planner/designer discretion, provided the nav registry is shared and responsive rather than forked.
- Exact visual treatment, iconography, spacing tokens, and page-header typography are designer/planner discretion, provided touch/focus/selector/test contracts are met.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 11 Scope and Mobile Requirements
- `.planning/ROADMAP.md` §Phase 11 — Phase goal, dependency on Phase 10, MH5-02/MH5-04/MH5-05 mapping, and success criteria for compact navigation, touch targets, focus, and shared title/container/action/card primitives.
- `.planning/REQUIREMENTS.md` §Mobile Foundation — MH5-02, MH5-04, and MH5-05 requirements for responsive shell/navigation, mobile-safe target sizes, and visible focus/focus order.
- `.planning/PROJECT.md` §Current Milestone: v1.1 适配移动端web — mobile-first H5 milestone boundary, full-site Vaadin Console/Admin coverage, Java/Vaadin/public REST/SSE DTO preservation, and no new frontend/native/mobile-backend stack.
- `.planning/STATE.md` — Current v1.1 state and carried-forward Phase 10 decisions around pi-mobile theme, data hooks, route smoke, and deferred navigation/layout redesign.

### Prior Phase UI and Verification Decisions
- `.planning/phases/05-agent-web-console-and-runtime-cockpit/05-CONTEXT.md` — Console/Admin are separate product areas; Console is Chat-first; Admin Governance is a distinct surface; Vaadin must consume public APIs/read models; Playwright/no-key browser E2E is accepted as test-only tooling.
- `.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-CONTEXT.md` — Phase 10 route list, `pi-mobile` theme baseline, representative Playwright matrix, route-level smoke scope, no-overflow gates, and stable `data-*` selector contract.
- `.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-03-SUMMARY.md` — Implemented route smoke coverage for all eight routes, Phase 10 helper patterns, E2E server/theme discovery gotchas, and explicit handoff that Phase 11 builds shell/navigation on top of the route smoke gate.

### Existing Product Documentation
- `docs/phase-05-web-console.md` — Web Console/Admin public API boundary and UI/test patterns.
- `docs/phase-10-mobile-baseline.md` — Mobile browser matrix, route smoke helpers, no-horizontal-overflow assertions, E2E commands, local environment limitations, and Phase 15 UAT handoff.

### External Documentation to Research During Planning
- Vaadin Flow 24.8/24.x RouterLayout/AppLayout/navigation/theme documentation — verify best free-component approach for shared shell, drawer/header composition, route title handling, theme styles, and any licensing implications.
- Vaadin Flow keyboard/focus/accessibility documentation — focus-visible styling, drawer/dialog focus behavior, navigation component semantics, and keyboard usability expectations.
- Playwright documentation — locator geometry/bounding box assertions, focus assertions, mobile/tablet project reuse, and reliable drawer/navigation testing patterns.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiWebAppShell.java` — Current Vaadin `AppShellConfigurator` and `@Theme("pi-mobile")` owner. It should keep owning theme/meta bootstrap; the new RouterLayout should own visual shell/navigation chrome.
- `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` — Existing Phase 10 theme baseline with safe-area, spacing, content width, overflow/wrap rules, and mobile Console/Admin overflow fixes. Phase 11 should extend this file with shell/nav/tap/focus/page primitives.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` — Existing `/console` route and current three-column workbench surface; should be attached to the shared shell without doing full Phase 12 flow redesign.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/*View.java` — Existing Admin Governance routes for overview, registry, operations, policy decisions, audits, and approvals; these should become reachable through the shared shell/drawer and receive consistent page-header/container treatment.
- `e2e/fixtures/mobile-smoke.ts` — Existing helpers for no-overflow, stable selector visibility, primary content/action visibility, and route baseline checks. Phase 11 should extend or reuse these for shell/nav/touch/focus assertions.
- `e2e/phase-10-mobile-route-smoke.spec.ts` — Existing route table for all eight current routes; Phase 11 should derive or mirror this route metadata for all-route shell navigation gates.
- `playwright.config.ts` — Existing Desktop Chrome, Mobile Chrome, Mobile Safari/WebKit, Mobile Firefox-sized, and Tablet projects.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebMobileBaselineContractTest.java` — Existing fast Java contract test pattern for Vaadin theme/root hook assertions; Phase 11 should add a similar shell/nav contract test.

### Established Patterns
- UI implementation belongs in `pi-agent-adapter-web`; Domain/App/client/public DTO contracts must not depend on Vaadin, Playwright, theme CSS, or responsive shell implementation details.
- Vaadin views currently use plain Java component composition with `Div`/`Main`, CSS class names, and stable `data-*` attributes rather than a separate frontend framework.
- Phase 10 established the `pi-mobile` theme as the responsive/mobile CSS owner and established route smoke as a metadata-driven Playwright pattern.
- Browser E2E remains deterministic/no-key and should rely on fake runtime/test fixtures, not real model providers or external services.
- `src/main/frontend/` is ignored by repository defaults; intentional theme changes may need force-staging in execution, matching Phase 10 precedent.

### Integration Points
- Add a new shared Vaadin RouterLayout/shell class in `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/` and attach all eight existing routes through `@Route(..., layout = ...)` or an equivalent Vaadin routing pattern.
- Create a single Java route/nav registry for Console and Admin Governance entries so shell UI and tests avoid route drift.
- Extend `pi-mobile/styles.css` with shell/header/drawer/nav, tap target, focus-visible, page header, content container, and base card/detail/action-row styles.
- Extend Playwright with a Phase 11 shell/navigation spec and reuse Phase 10 route metadata/helpers for all-route navigation and no-overflow gates.
- Reconcile or remove `MainConsoleLayout` and converge `AdminGovernanceLayout` so no unused or competing layout abstraction remains.

</code_context>

<specifics>
## Specific Ideas

- User selected all proposed Phase 11 discussion areas: shell structure, mobile navigation pattern, touch/focus contract, shared UI primitives, and selector/test contract.
- User chose a single shared shell with Console/Admin as top-level areas and Admin sub-navigation grouped inside the mobile drawer.
- User chose compact header + drawer for phone navigation and a shared responsive nav source of truth for tablet/desktop presentation.
- User chose a 44px touch target floor, global CSS defaults with explicit exceptions, global focus-visible tokens, and basic drawer focus return rather than a full Phase 11 focus trap requirement.
- User chose to create foundational primitives only: route title, content container, status/action slot, and base card/detail styling. Full feature-specific content redesigns remain later phases.
- User chose full shell/nav selector standardization and Java contract + Playwright gates, including all-route navigation and focused touch/focus assertions.

</specifics>

<deferred>
## Deferred Ideas

- Full Console mobile-first flow, including Agent Catalog, Chat/Run composer, SSE feed, sessions, and cancellation UX — Phase 12.
- Final runtime event cards, tool cards, approval cards, dense details, dialogs, and confirmations UX — Phase 13.
- Full Admin Governance card/detail conversion across registry, operations, MCP, plugin, extension, policy, audit, and approval surfaces — Phase 14.
- Broad cross-browser/orientation/accessibility hardening, full keyboard traversal, real-device/UAT expectations, and release documentation — Phase 15.
- Full mobile/native/PWA/offline product enhancements and mobile-only Agent capabilities — out of scope for v1.1 unless future roadmap adds them.

</deferred>

---

*Phase: 11-shared-responsive-shell-and-navigation*
*Context gathered: 2026-06-21*
