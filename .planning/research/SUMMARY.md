# Project Research Summary

**Project:** Pi Java Agent Platform  
**Domain:** v1.1 mobile-first H5 adaptation of the existing Java/Spring/Vaadin Agent Web Console and Admin Governance app  
**Researched:** 2026-06-20  
**Confidence:** HIGH

## Executive Summary

v1.1 is a **mobile-first H5 adaptation milestone**, not a new mobile product, native app, React rewrite, backend expansion, or runtime architecture phase. The existing Pi Java Agent Platform already prioritizes a Java/Vaadin Web Console and Admin Governance surface over a TypeScript frontend. Expert implementation for this milestone is therefore to keep the same Vaadin Flow application and make every existing Console/Admin route usable on mainstream mobile browsers through responsive information architecture, shared mobile UI primitives, centralized theme CSS, and mobile browser E2E verification.

The recommended approach is an **adapter-web presentation refactor**. Keep COLA, REST/SSE DTOs, `ConsoleHttpClient`, `EventStreamClient`, runtime/domain/app/infrastructure modules, tool governance semantics, and public routes stable. Build mobile defaults in `pi-agent-adapter-web`: a responsive app shell, compact navigation, single-column Console task flow, card/detail patterns for dense run/admin data, safe sticky action/composer areas, and Playwright Java/mobile projects that assert route coverage, no horizontal overflow, visible key actions, touch context, and desktop regression.

The key risk is mistaking mobile support for “desktop UI plus media queries.” That will leave the three-column Console, wide admin grids, hover-dependent actions, long IDs/JSON payloads, and SSE scroll behavior unusable on phones. Mitigate by making mobile the default IA, progressively enhancing for tablet/desktop, converting dense data to cards/details, keeping approvals/cancel/composer reachable during active runs, and adding objective mobile gates before broad page conversion. Scope must remain tight: no React/Next.js, Vaadin 25/Spring Boot 4 migration, native wrapper, mobile-only APIs, offline admin cache, or new agent runtime capabilities in this milestone.

## Key Findings

### Recommended Stack

The stack research is explicit: **do not add a separate frontend stack**. Use the existing **Vaadin Flow + Spring Boot + Java test harness** and add a mobile design-system/verification layer inside `pi-agent-adapter-web`. Stay on **Vaadin 24.x** while the platform stays on **Spring Boot 3.5.x**. The repo currently pins Vaadin **24.8.4**; latest compatible Vaadin 24 patch observed was **24.10.7**, but implementation should validate the exact Maven patch. Vaadin 25 is not a mobile prerequisite and would drag in Spring Boot 4 migration risk.

**Core technologies:**
- **Vaadin Flow 24.x** — existing Java UI framework; supports responsive layouts, AppLayout behavior, FormLayout responsive steps, Grid details, Dialog behavior, and Flow theme integration.
- **Vaadin Lumo Theme + Lumo Utility Classes** — standard tokens/utilities for spacing, typography, visibility, responsive helpers, accessibility helpers, and touch sizing.
- **App-level Vaadin theme/CSS resources** — central place for mobile-first primitives: responsive shell, card lists, timeline cards, approval bars, drawers, no-overflow defaults, long-token wrapping.
- **CSS media queries and container queries** — phone layout as default; enhance at 640px, 768px, 1024px+ and adapt cards/panels based on container width.
- **`@media (pointer: coarse)` touch sizing** — increase Lumo size variables and spacing for touchscreen contexts.
- **Playwright Java 1.60.0+ candidate** — explicit test dependency for mobile browser contexts with `viewport`, `screenSize`, `deviceScaleFactor`, `isMobile(true)`, and `hasTouch(true)`.
- **JUnit Jupiter + Spring Boot Test** — keep E2E/test lifecycle inside Java/Maven; do not add Node Playwright Test as a second orchestration stack unless existing repo tooling already requires it.

**Avoid adding:** React/Next.js/Vite/Hilla React, Tailwind CSS, native iOS/Android wrappers, Capacitor/Cordova/Flutter/React Native, Vaadin 25 migration, mobile-specific REST/SSE DTOs by default, horizontal-scroll tables as the mobile admin strategy, and CSS-only patching without IA changes.

### Expected Features

The milestone should launch when the **same full site** is usable on phone/tablet widths, not when only Chat works. Every existing Console and Admin Governance route must load, navigate, render primary information, expose critical actions, and pass no-horizontal-overflow gates.

**Must have (table stakes):**
- **Full-site mobile route coverage** — every Console/Admin route opens at phone viewport without blank screens, route errors, or desktop-only blockers.
- **Shared responsive shell/navigation** — compact header, mobile drawer/section nav, route title, back/close affordances, touch-friendly links.
- **No page-level horizontal overflow** — validate representative widths: 360, 390/393, 412/430, 768, and landscape.
- **Touch/readability/accessibility baseline** — 44px-preferred primary targets, WCAG 2.2 AA minimum target/spacing, visible focus, no hover-only controls, readable spacing and wrapping.
- **Console mobile IA refactor** — Chat/Run first; sessions/catalog/context moved to drawer/tabs/collapsible details instead of fixed desktop three-column workbench.
- **Agent Catalog mobile cards** — stacked cards with clear selection/start affordance.
- **Chat/Run mobile composer and SSE feed** — multi-line prompt, submit/running states, readable live event feed, scrollable history, and composer/control access under mobile browser chrome.
- **Run timeline/tool/approval mobile cards** — vertical event cards, expandable details, redacted tool payload summaries, clear status/risk/provenance, thumb-safe approve/reject.
- **Session continuation and cancel affordances** — session history/active state and visible cancel/terminal-state feedback on mobile.
- **Admin Governance mobile cards/details** — overview, registry, operations, MCP, plugin, extension, policy, approval queue, and audit readable without desktop table dependence.
- **Automated mobile verification gate** — Playwright mobile Chrome, mobile Safari/WebKit proxy, tablet, route/action smoke, no-overflow assertions, and desktop regression preserved.

**Should have (differentiators / v1.1.x follow-up if baseline is stable):**
- **Run focus mode** — active mobile run prioritizes stream, latest tool activity, approvals, and cancel.
- **Mobile incident triage mode** — Admin overview shortcuts to unhealthy provider/tool/MCP/plugin/audit evidence.
- **Risk-first approval UX** — approval cards emphasize side-effect level, policy decision, source/tool, and redacted input before action.
- **Shared mobile card schema across governance surfaces** — consistent status badge/title/metadata/details/actions for registry/MCP/plugin/extension/policy/audit.
- **Deep-linkable expanded details** — links to run/session/audit/policy/MCP/plugin open directly to useful mobile detail state.
- **Mobile event filters/summarization** — client-side filtering by errors/tools/approvals/model output.
- **Poor-network/reconnect messaging** — clear stale/reconnecting/retry states for SSE/admin refresh.
- **Copy/share of redacted IDs/summaries** — incident handoff without exposing sensitive payloads.

**Defer (v2+ / out of milestone):**
- Native mobile app, app-store delivery, or native wrappers.
- Push notifications/background run monitoring.
- Full PWA/offline admin mode or service-worker caching of sensitive governance/audit data.
- Mobile-specific new Agent runtime capabilities.
- Mobile-only reduced product that excludes governance.
- React/Next.js mobile rewrite or separate mobile UI routes unless an isolated component proves impossible responsively.

### Architecture Approach

Architecturally, this is an **adapter-only mobile refactor**. All mobile work belongs in `pi-agent-adapter-web` plus root/browser E2E assets. The presentation structure changes; the runtime data flow does not. Existing public REST/SSE/read-model contracts and app/domain/infrastructure layers stay stable unless a measured performance issue requires a general read-model improvement usable by desktop/API/future CLI too.

**Major components:**
1. **Responsive Shell / Navigation** — shared app/admin shell with compact header, drawer/section navigation, route titles, safe content container, and desktop enhancement.
2. **Vaadin Routes / Views** — existing Console, Agent Catalog, Chat/Run, timeline, approval, and Admin Governance pages converted from desktop-first composition to mobile-first composition.
3. **Shared Mobile UI Primitives** — `PageScaffold`, `ResponsiveSection`, `ResponsiveCard`, `StatusBadge`, `MobileActionBar`, touch-safe buttons, collapsible details, and stable hooks.
4. **Theme and CSS Assets** — centralized `styles.css`, `tokens.css`, `responsive.css`, `console.css`, `admin.css`, `components.css` or equivalent existing Vaadin theme convention.
5. **Console Panels/Cards** — `ConsoleView`, `SessionListPanel`, `AgentCatalogPanel`, `ChatEventStreamPanel`, `RunContextPanel`, `ToolCallCard`, `ApprovalCard` made independently narrow-container safe.
6. **Admin Governance Views** — overview/operations/registry/MCP/plugin/extension/policy/audit/approval queue rendered as card/detail layouts on mobile, grids only as tablet/desktop enhancements.
7. **Playwright Mobile Projects/Specs** — mobile/tablet browser contexts, no-overflow assertions, route smoke, key Console/Admin interactions, screenshots/traces on failure.
8. **Stable UI Test Hooks** — `data-route`, `data-layout`, `data-action-*`, semantic `pi-*` classes become the contract between Java components, CSS, and Playwright.

**Key patterns to follow:**
- Mobile-first CSS with desktop `min-width` enhancements; Java exposes structure, CSS adapts it.
- Progressive disclosure for dense data: summary + status/severity/provenance + primary action + collapsed details.
- Adapter-only boundary: no viewport flags in App/Domain; no `/mobile/*` APIs by default.
- One route set and one public DTO/SSE contract; mobile state lives in Vaadin UI state.
- Desktop regression remains in the test matrix; mobile-first must not mean desktop-broken.
- Tool governance/redaction remains visible and preserved; mobile cards must not reveal hidden payloads.

### Critical Pitfalls

1. **CSS-only adaptation without IA change** — avoid by defining mobile task order first: Chat/Run and approvals/cancel first; sessions/catalog/context as drawers/details; admin data as cards.
2. **Horizontal overflow leaks** — avoid by adding Playwright `scrollWidth <= clientWidth + 1` assertions early, setting `min-width: 0`, wrapping long IDs/URLs/JSON, and avoiding wide tables on phones.
3. **Hover/tiny pointer assumptions** — avoid by using visible labels, explicit menus/details, touch-safe spacing, 44px-preferred primary targets, and tests for visible/enabled critical actions.
4. **SSE/chat mobile usability breaks** — avoid uncontrolled nested scroll traps; keep composer, cancel, approval, and terminal-state feedback reachable while events stream.
5. **Admin Governance treated as optional** — avoid by enumerating every Admin route in requirements/tests and converting registry/operations/MCP/plugin/extension/policy/audit to card/detail mobile layouts.
6. **Scope creep into new stack/native/PWA/API redesign** — avoid by constraining implementation to Vaadin adapter/theme/tests and preserving public REST/SSE/read-model boundaries.
7. **Browser matrix claimed but not verified** — avoid by testing real mobile contexts (`isMobile`, `hasTouch`) for Chromium/WebKit/tablet and documenting real iOS Safari UAT gaps.
8. **Desktop regressions from global CSS** — avoid with scoped `pi-*` classes, mobile defaults plus desktop enhancements, and existing desktop E2E retained.

## Implications for Roadmap

Based on research, the v1.1 roadmap should be dependency-driven: **verification/theme baseline → shell/navigation → Console flow → reusable run/tool/approval cards → Admin full-site conversion → cross-browser hardening**. This order creates objective gates before broad refactoring and converts the highest-value/riskiest user path before the broader governance surface.

### Phase 1: Responsive Baseline and Mobile Test Harness

**Rationale:** Establish the acceptance gate before touching many views. Mobile regressions are mostly visual/layout/action regressions, so Playwright mobile contexts and no-overflow assertions must exist first.

**Delivers:**
- Vaadin theme entrypoint/import structure or documented use of existing theme convention.
- Design tokens, mobile-first no-overflow defaults, long-token wrapping, touch sizing variables.
- Explicit Playwright Java dependency if missing; mobile Chrome, mobile Safari/WebKit proxy, tablet, and desktop projects/profiles.
- `mobile-h5` route smoke with no-horizontal-overflow checks and screenshots/traces on failure.
- Stable `data-route`/`data-layout` hooks on representative pages.

**Addresses:** MOBILE-H5-REQ-001, 003, 030, 031, 034; stack additions around Lumo/theme/Playwright.

**Avoids:** Horizontal overflow slipping through, unverified browser matrix, desktop regressions, visual-only/manual-only testing.

**Research flag:** Standard patterns; skip broad research. Confirm existing Vaadin theme bootstrap (`@Theme`/`AppShellConfigurator`/theme folder) and exact Playwright dependency/config style in the repo.

### Phase 2: Shared Responsive Shell and Navigation

**Rationale:** Every route depends on shell/navigation. Doing per-page conversions first will create duplicate mobile nav and rework.

**Delivers:**
- Responsive `MainConsoleLayout` and `AdminGovernanceLayout` with compact header, drawer/section navigation, route title, safe content container.
- Shared primitives: `PageScaffold`, `ResponsiveSection`, `MobileActionBar`, `ResponsiveCard`, `StatusBadge` or equivalent.
- Touch-friendly navigation across Console, Agent Catalog/session areas, and Admin overview/registry/operations/MCP/plugin/extension/policy/audit.

**Addresses:** MOBILE-H5-REQ-002, 004, 005 plus shared shell prerequisites for all route coverage.

**Avoids:** Unreachable navigation, inconsistent mobile layout per route, hover-only nav/actions, desktop shell squeezed onto phones.

**Research flag:** Standard Vaadin patterns. During implementation choose exact widgets (`AppLayout`, drawer, `Tabs`, `Details`, plain layouts) based on current component inventory.

### Phase 3: Console Workbench Mobile-First Flow

**Rationale:** Console is the primary user-facing flow and exercises catalog/session/chat/run/SSE/cancel state. It also has the highest IA risk because the current desktop mental model is a three-column workbench.

**Delivers:**
- `ConsoleView` refactored to mobile task order: chat/run feed first; active status/actions visible; sessions/catalog/context in drawer/tabs/collapsible sections.
- Agent Catalog stacked cards with start/select affordance.
- Mobile composer with multi-line input, submit/running state, keyboard/browser-chrome-aware placement.
- SSE feed as readable vertical stream with scroll behavior that does not trap the user away from controls.
- Session history/continuation and active run cancel available on phone.

**Addresses:** MOBILE-H5-REQ-010, 011, 012, 016, 017; supports full Console route coverage.

**Avoids:** CSS-only three-column shrink, chat composer disappearing, active controls hidden, nested scroll traps, session context inaccessible.

**Research flag:** Needs targeted implementation spike if current `ConsoleView` has fixed sizing or server-side state assumptions. No external research needed unless mobile keyboard/Safari viewport behavior becomes blocking.

### Phase 4: Runtime Cards, Timeline, Tool, and Approval UX

**Rationale:** Tool execution, approvals, policy/redaction, and event timelines are both differentiating and safety-sensitive. They should be reusable before Admin approval/governance conversion.

**Delivers:**
- Mobile event/timeline card schema with status, timestamp/type, summary, expandable details.
- `ToolCallCard` mobile summary/detail layout with tool/source/status/policy/approval/duration/error/redacted input-output.
- `ApprovalCard` / approval panel with risk-first context, large approve/reject actions, confirmation where needed, and terminal feedback.
- Long payload handling: wrapping, truncation, internal scroll only for code/JSON blocks, redaction markers always visible.

**Addresses:** MOBILE-H5-REQ-013, 014, 015; differentiators risk-first approval UX and run focus foundations.

**Avoids:** Secret exposure through raw payloads, tiny destructive actions, hover-only risk context, unreadable logs/IDs, unsafe approvals on mobile.

**Research flag:** Mostly standard Vaadin component work. Phase-specific validation should check current redaction metadata availability; add generic read-model fields only if risk/tool summaries cannot be produced from existing DTOs.

### Phase 5: Admin Governance Full-Site Mobile Coverage

**Rationale:** Full-site mobile means Admin is not optional. Admin has broad surface area and dense data, so it should happen after shell and shared card/detail primitives exist.

**Delivers:**
- Admin overview as stacked health/status cards with counts, severity, messages, links.
- Registry/operations/MCP/plugin/extension pages as mobile cards/details with status, capabilities, metadata, disabled/quarantined/load-error states.
- Policy decisions and audit pages as filterable/searchable stacked cards with expandable redacted context and wrapped identifiers.
- Approval queue using the same mobile approval cards.
- Playwright mobile coverage for each Admin route category and key inspect/refresh/disable/quarantine/approval action visibility where already supported.

**Addresses:** MOBILE-H5-REQ-020 through 026 and MOBILE-H5-REQ-033.

**Avoids:** “Chat-only mobile,” wide-table admin dependency, hidden governance context, excluding policy/audit from mobile acceptance.

**Research flag:** Potential phase research if lists are large enough to require Vaadin lazy data providers/virtualization or generic backend pagination/projection. Otherwise standard card/detail conversion.

### Phase 6: Cross-Browser, Orientation, Accessibility, and Release Hardening

**Rationale:** Browser/viewport quirks and desktop regressions are easiest to finish after all surfaces are converted. This phase validates the milestone as a release, not just a layout refactor.

**Delivers:**
- Final mobile Chrome, WebKit/iPhone proxy, tablet, landscape, and desktop regression gates.
- Manual/device-farm UAT note for real iOS Safari and Android Chrome if CI cannot run them reliably.
- Orientation/short-height tuning for sticky composer, bottom approval actions, dialogs/drawers, and virtual keyboard scenarios.
- Accessibility pass for focus order, visible focus, icon labels, screen-reader helper labels, and no hover-only interactions.
- Release checklist documenting supported mobile browsers, known gaps, and screenshots/traces retention.

**Addresses:** MOBILE-H5-REQ-005, 030, 031, 032, 034, 035 plus desktop preservation.

**Avoids:** Safari/WebKit surprises, landscape failures, keyboard/viewport chrome bugs, claimed support without evidence, desktop regressions.

**Research flag:** Needs targeted validation/UAT for real iOS Safari behavior and possibly accessibility tooling if current Playwright suite lacks a11y checks.

### Phase Ordering Rationale

- **Gates before refactors:** mobile browser projects, no-overflow assertions, and stable hooks must exist before widespread layout changes.
- **Shell before pages:** route navigation and content container rules are shared dependencies; page conversion before shell creates duplicate work.
- **Console before Admin:** Console validates the hardest live flow: catalog/session/chat/run/SSE/tool/approval/cancel.
- **Cards before broad governance:** tool/timeline/approval cards become the pattern Admin reuses for policy/audit/MCP/plugin/extension data.
- **Hardening last but not optional:** WebKit/iOS, orientation, touch targets, focus, and desktop regression need final full-surface validation.
- **No backend expansion by default:** any API/read-model changes should be justified by measured payload/performance needs and must be general, not mobile-only.

### Research Flags

Phases likely needing deeper or targeted research during planning:
- **Phase 1:** Confirm exact Vaadin theme/bootstrap convention and whether Playwright is Java/JUnit-based or existing root config is TypeScript-based; align without adding a second stack.
- **Phase 3:** Investigate current `ConsoleView` fixed-width/scroll/composer implementation before estimating refactor effort.
- **Phase 5:** Research Vaadin virtualization/lazy data providers only if admin audit/registry/events lists are large or mobile performance fails.
- **Phase 6:** Validate real iOS Safari/Android Chrome UAT path and optional accessibility automation depth.

Phases with standard patterns (skip broad `/gsd-research-phase`):
- **Phase 2:** Vaadin responsive shell/navigation and shared primitives are well-documented; implementation inventory is enough.
- **Phase 4:** Progressive disclosure cards, Lumo utilities, Vaadin details/dialog/buttons, and CSS wrapping are standard.
- **Most of Phase 1:** Playwright mobile context/device setup is documented; only repo-specific integration needs inspection.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Vaadin 24 responsive APIs, Lumo utilities, theme CSS, and Playwright mobile emulation are backed by official/Context7 docs. Exact patch versions should still be checked at implementation time. |
| Features | HIGH | Table stakes align with milestone constraints, WCAG/reflow/touch guidance, responsive H5 expectations, and existing Console/Admin surfaces. Differentiators are product-specific and medium confidence. |
| Architecture | HIGH | Boundary recommendation is strongly supported by existing COLA/project constraints and current adapter-web/API separation: mobile is presentation-layer work. Exact class/file names need final code inventory. |
| Pitfalls | HIGH | Critical pitfalls are consistent across responsive design, Vaadin component behavior, mobile browser testing, accessibility, and project-specific Console/Admin density risks. |

**Overall confidence:** HIGH

### Gaps to Address

- **Exact Vaadin patch target:** Current repo uses 24.8.4; latest Vaadin 24 patch observed was 24.10.7. Validate Maven compatibility before upgrading; shipping on 24.8.4 is acceptable if responsive APIs suffice.
- **Theme bootstrap convention:** Confirm whether the app uses a Vaadin theme folder, `@Theme`, `AppShellConfigurator`, `@StyleSheet`, or another established pattern; do not churn just to match docs.
- **Playwright integration shape:** Research says Playwright Java should be explicit if absent, but architecture examples mention `playwright.config.ts`. Inventory actual repo test harness and avoid duplicate browser-test orchestration.
- **Component inventory and fixed-width leaks:** Inspect all Console/Admin Vaadin classes for hard-coded widths, Grid usage, nested scroll regions, and hover-only controls before final estimates.
- **Large-list performance:** If audit/events/registry lists are heavy, add generic pagination/virtualization/projections; do not create mobile-only DTOs.
- **Real mobile browser parity:** Playwright WebKit/mobile emulation is strong but not a full replacement for real iOS Safari/Android Chrome UAT; document any release gap.
- **Accessibility depth:** Touch target and focus requirements are clear, but decide whether to add automated a11y checks or keep manual/Playwright assertions for this milestone.

## Sources

### Primary (HIGH confidence)
- `.planning/research/STACK.md` — Vaadin 24.x/Lumo/theme/Playwright Java stack recommendations and version posture.
- `.planning/research/FEATURES.md` — mobile H5 table stakes, candidate MOBILE-H5 requirement IDs, MVP/defer lists, feature dependency map.
- `.planning/research/ARCHITECTURE.md` — adapter-web-only architecture, component responsibilities, build order, data-flow preservation, boundaries.
- `.planning/research/PITFALLS.md` — critical mobile-first pitfalls and prevention strategies.
- Project constraints from `.planning/PROJECT.md` / `CLAUDE.md` — Java/Vaadin-first, COLA boundaries, public REST/SSE, Cloud Server + Web Console/Admin priority.
- Vaadin official docs and Context7 `/vaadin/flow`, `/websites/vaadin` — responsiveness, AppLayout, FormLayout, Grid details, dialogs, theme/CSS resources, Lumo utility classes.
- Playwright Java official docs and Context7 `/microsoft/playwright-java` — mobile context emulation, devices, viewport/screen/touch/user agent/color scheme.
- W3C WCAG 2.2 / WAI guidance — reflow, focus visibility, target size minimum/enhanced target size.

### Secondary (MEDIUM confidence)
- Vaadin roadmap/search evidence — Vaadin 24.10.7 as observed latest 24 line; Vaadin 25 requiring Spring Boot 4.0.4+ evidence should be revalidated before any platform upgrade decision.
- Maven Central search evidence — `com.microsoft.playwright:playwright` 1.60.0 observed on 2026-05-19; verify latest before final pin.
- Product-specific differentiator inference — mobile incident triage, run focus mode, risk-first approvals, and copy/share evidence are strong fit for Agent/Admin workflows but should be validated with users/operators.

---
*Research completed: 2026-06-20*  
*Ready for roadmap: yes*
