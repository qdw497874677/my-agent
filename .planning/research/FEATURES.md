# Feature Research: Mobile-First H5 Adaptation

**Domain:** Mobile-first H5 support for Java/Vaadin Agent Web Console and Admin Governance  
**Project:** Pi Java Agent Platform  
**Researched:** 2026-06-20  
**Confidence:** HIGH for responsive/mobile/accessibility/testing expectations; MEDIUM-HIGH for Vaadin-specific implementation patterns; MEDIUM for product differentiators because they are inferred from this product's Agent Console/Governance domain rather than broad public competitor evidence.

## Research Scope

This research covers **only the new mobile-first H5 milestone** for an already-built Java/Vaadin Agent Web Console and Admin Governance app. Existing product capabilities are assumed to exist: Agent Catalog, Chat/Run Console, Run timeline/events, tool cards, approval cards, session history/continuation/cancel, Admin Governance overview/registry/operations/MCP/plugin/extension/policy/audit views, REST/SSE API boundary, and Playwright E2E.

The goal is not to add a separate mobile product. The goal is to make the **same full site** usable and testable on mainstream mobile browsers, while keeping Java/Vaadin/public REST/SSE boundaries and avoiding React/Next.js or native mobile apps.

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist in a credible mobile H5 admin/console experience. Missing these means the product still feels “desktop site squeezed onto phone,” not mobile-first.

| Feature | Why Expected | Complexity | Dependencies / Notes |
|---------|--------------|------------|----------------------|
| **Full-site mobile route coverage** | Users expect every route that exists on desktop to at least be usable on mobile, especially for operational incident triage. Partial mobile support creates dead ends. | MEDIUM | Depends on existing Vaadin routes: `/console`, Agent Catalog area, Chat/Run, Run context, Admin overview, registry, operations, MCP, plugin, extension, policy, approval, audit views. Acceptance must enumerate every route and verify no mobile-only 404/blank/blocked page. |
| **Mobile-first information architecture** | Phone users cannot effectively operate a desktop three-column workbench. Primary task flow must be visible first, secondary context must become drawers/sheets/details. | HIGH | Depends on existing `ConsoleView` three-column layout (`sessions`, `chat-event-stream`, `run-context`). Convert to task-first order: chat/run primary, sessions/catalog as navigation drawer or top switcher, run context as collapsible/details panel. |
| **Responsive app shell/navigation** | Admin and console users need predictable navigation between many pages on narrow screens. Vaadin `AppLayout` is documented as responsive and supports navbar/drawer regions. | MEDIUM | Introduce or standardize a shared mobile shell: compact header, drawer navigation, current route title, back/close affordances. Admin pages should not rely on wide horizontal nav. |
| **No horizontal page overflow at common phone widths** | Horizontal scrolling is one of the clearest signs of failed responsive design. WCAG reflow guidance and responsive design basics emphasize adapting content to viewport width. | MEDIUM | Validate at widths such as 360, 390/393, 412, 430, 768, and landscape. Some inner code/log panels may scroll internally, but the document/body must not overflow horizontally. |
| **Touch-friendly interactive controls** | Mobile users tap with fingers, not mouse pointers. WCAG 2.2 target-size guidance defines 24×24 CSS px minimum, while 44×44 is a safer enhanced target. | MEDIUM | Buttons, links, drawer toggles, tabs, cards, approval actions, cancel actions, grid detail toggles, refresh buttons, and filters should be at least 44px where practical and never below WCAG 2.2 AA minimum/spacing. |
| **Readable typography and spacing on H5** | Dense admin tables and desktop cards become unreadable on phones unless text, wrapping, and spacing are mobile-tuned. | MEDIUM | Use shared mobile CSS/theme tokens. Avoid tiny status badges or dense inline metadata. Wrap long IDs and tool names. Use monospace blocks with internal scrolling only when necessary. |
| **Agent Catalog mobile cards** | Catalog discovery is a core entry point. On mobile, cards should stack and show enough metadata to choose an agent without opening desktop grids. | LOW-MEDIUM | Depends on existing Agent Catalog API/cards. Acceptance: user can view catalog, identify default/general agent, and select/start chat from a phone viewport. |
| **Chat/Run mobile composer** | Chat is the primary user entry point. Mobile users expect a bottom-safe, thumb-accessible composer that does not disappear behind browser UI or virtual keyboard. | HIGH | Depends on existing run/session APIs and SSE. Composer should remain usable after scrolling, support multi-line text, submit, disabled/running state, and safe-area/keyboard-friendly layout. |
| **Live SSE event stream readable on mobile** | Agent runs are long-running and streaming. Users must understand model deltas, step transitions, tool calls, approvals, completion/failure on mobile. | HIGH | Depends on existing public SSE and event DTOs. Acceptance: mobile user starts a run, sees streamed output/events, can scroll history without losing composer/control access. |
| **Run timeline/event mobile presentation** | Timeline is critical for observability. Desktop timeline/table layouts must become a vertical feed or accordion on mobile. | MEDIUM-HIGH | Depends on existing run timeline/events and tool cards. Use event cards with timestamp/status/type summary and expandable details. Avoid dense multi-column timeline tables. |
| **Tool cards usable on mobile** | Tool execution details are a differentiating part of the console. Users must inspect tool name, status, policy, inputs/outputs summary, duration, errors. | MEDIUM | Depends on governed tool cards and audit/redaction boundaries. Use collapsed summaries by default; details expand into readable stacked sections. Preserve redaction; do not expose raw sensitive payloads because mobile is less private. |
| **Approval cards optimized for thumb use** | Human approval can block a run; mobile users often approve/reject while away from desktop. Actions must be clear and safe. | HIGH | Depends on existing user/admin approval APIs/cards. Acceptance: approval card shows risk/side-effect summary, required context, approve/reject actions, confirmation for high-risk actions, and success/failure state at mobile width. |
| **Session history/continuation mobile flow** | Existing sessions must be discoverable and resumable on mobile. Users should not need a desktop sidebar to continue work. | MEDIUM | Depends on session history/continuation/cancel. Provide drawer/list/search-ish compact navigation; selected session state remains obvious; continuation does not reset the selected agent/session accidentally. |
| **Cancel/stop run mobile affordance** | Long-running or risky runs need an accessible cancel path on mobile. | MEDIUM | Depends on existing cancel API. The cancel action should be visible when a run is active, not buried in an off-screen context column. Include confirmation or reason where existing behavior requires it. |
| **Admin Governance mobile overview cards** | Operators need at-a-glance health on phone. Overview should stack status cards with clear severity and links. | MEDIUM | Depends on existing Admin Governance overview DTOs. Acceptance: runtime/provider/tool/extension/MCP/plugin statuses are visible as cards with counts/messages and route links. |
| **Registry/Operations/MCP/Plugin/Extension mobile details** | Governance pages often contain grids. On phone, users need inspectability via card lists and row details, not wide tables. | HIGH | Depends on existing admin DTOs. Vaadin Grid can use item details renderers, but hidden columns may still send data to client; use mobile-specific projections/cards for heavy data where needed. |
| **Policy and audit mobile inspection** | Policy/audit views are core safety surfaces. Mobile users must inspect decisions and audit summaries without horizontal table dependence. | HIGH | Depends on policy/audit APIs and redaction. Use filters stacked above list, compact decision/audit cards, expandable JSON/details, and strong wrapping for IDs. |
| **Mobile-safe dialogs, drawers, overlays, notifications** | Vaadin overlays can feel broken if too wide/tall or unscrollable on phones. | MEDIUM | All approval confirmations, details dialogs, error notifications, and drawer panels must fit viewport, scroll internally if needed, and close by explicit controls/backdrop/escape-equivalent where applicable. |
| **Orientation and tablet behavior** | Users rotate phones and use tablets. Mobile support should not be portrait-only. | MEDIUM | Acceptance across portrait/landscape and tablet widths. Landscape can use two-pane layout if enough width; portrait should remain single-column. Do not lock orientation. |
| **Keyboard and focus accessibility retained** | Mobile-first must not regress desktop or accessibility. WCAG 2.2 focus guidance expects visible focus indicators. | MEDIUM | Preserve keyboard navigation and `:focus-visible`; ensure drawer/menu/modal focus order is sensible. This is also useful for external keyboards on tablets. |
| **Mobile browser target matrix** | Project explicitly targets Android/iOS Chrome, Safari, Edge, Firefox. Users expect mainstream mobile browser compatibility. | MEDIUM | Automated local Playwright can cover Chromium/WebKit/Firefox device emulation where available; real iOS Safari should be human/UAT if CI cannot run it. Document browser support target. |
| **Automated mobile verification gate** | The milestone explicitly requires mobile viewport/browser smoke. Without tests, mobile regressions will recur. | MEDIUM-HIGH | Depends on existing Playwright E2E. Add projects such as Mobile Chrome/Pixel, Mobile Safari/iPhone, tablet, and narrow custom viewport with `hasTouch`. Assertions: route loads, no horizontal overflow, key actions complete, touch targets visible/enabled. |
| **Desktop behavior preserved** | This is an adaptation milestone, not a rewrite that breaks existing desktop E2E. | MEDIUM | Existing desktop Playwright and unit tests remain required. Mobile CSS/layout changes should be additive and responsive, not desktop-only regressions. |

### Differentiators (Competitive Advantage)

These are not required for minimum mobile viability, but they match the Agent Console/Admin Governance domain and can make the product feel purpose-built rather than generically responsive.

| Feature | Value Proposition | Complexity | Dependencies / Notes |
|---------|-------------------|------------|----------------------|
| **Mobile incident triage mode** | Operators can diagnose a bad run or unhealthy extension from a phone quickly: status → latest failures → affected tool/provider/plugin → audit link. | MEDIUM-HIGH | Depends on Admin overview, operations, registry, audit. Implement as ordering/shortcuts on existing surfaces, not new backend semantics. |
| **Run “focus mode” on mobile** | During an active run, the UI prioritizes stream, approvals, cancel, and latest tool activity while hiding secondary navigation. | MEDIUM | Depends on Chat/Run, SSE, approvals, cancel. Could be a responsive state in ConsoleView. Acceptance: active run has no distracting sidebars and all critical controls remain reachable. |
| **Risk-first approval UX** | Approval card emphasizes side-effect level, tool/source, policy decision, and redacted input summary before approve/reject. | MEDIUM | Depends on existing policy/tool metadata. Especially valuable on mobile where users make faster decisions and need guardrails. |
| **Mobile card schema shared across governance surfaces** | Registry, MCP, plugin, extension, policy, and audit pages feel consistent: status badge, primary title, metadata chips, expandable details, actions. | MEDIUM | Depends on common UI components/theme classes. Reduces per-page layout drift and testing burden. |
| **Deep-linkable mobile details** | Links to a run, session, audit event, MCP server, plugin, or policy decision open directly to an expanded mobile-friendly detail state. | MEDIUM-HIGH | Depends on route/query state. Useful for alerts, chatops, and future CLI/TUI links. |
| **Mobile event summarization controls** | Dense event streams can be filtered by errors/tools/approvals/model output on a phone. | MEDIUM | Depends on existing event taxonomy. Keep client-side initially; avoid adding backend query complexity unless data volume requires it. |
| **Offline/poor-network resilience messaging** | Mobile networks are less reliable. Clear reconnect/loading/stale-state indicators improve trust for SSE/admin refresh. | MEDIUM | Depends on EventStreamClient and HTTP client behavior. Do not promise offline operation; show reconnecting/stale and allow retry. |
| **Installable/PWA-like polish without offline claims** | Add viewport/theme-color/manifest-ish polish so the H5 app feels good when pinned to home screen, but avoid full PWA scope creep. | LOW-MEDIUM | Optional after core mobile passes. Should not introduce service-worker caching of sensitive admin data unless explicitly designed. |
| **Mobile evidence capture for audits** | A mobile operator can copy/share a run ID, audit ID, policy decision ID, or redacted summary quickly. | LOW-MEDIUM | Depends on existing IDs and redacted summaries. Useful for incident workflows; avoid sharing raw payloads. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem attractive but would distract from the current milestone or violate project constraints.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| **Native iOS/Android app** | Better mobile feel, push notifications, app-store presence. | Out of scope; duplicates H5 surfaces; introduces new client stack and release process. | Make full-site H5 excellent first; keep REST/SSE boundaries suitable for future clients. |
| **React/Next.js mobile frontend rewrite** | More frontend ecosystem patterns for responsive UI. | Violates Java/Vaadin-first constraint and creates parallel UI architecture. | Use Vaadin Flow with shared responsive CSS/components and targeted client-side enhancements only where necessary. |
| **Mobile-only reduced product** | Faster to ship only Chat on phones. | Milestone requires full-site support; operators need Admin Governance on mobile for triage. | Prioritize task-specific mobile layouts for every existing route, even if some admin pages are inspect-only. |
| **Just add CSS media queries to desktop layout** | Looks cheap and fast. | Existing console is a three-column desktop workbench; CSS alone can leave wrong IA, hidden actions, overflow, and unusable tables. | Refactor layout hierarchy: mobile shell, drawers, cards, details, responsive component composition. |
| **Horizontal scroll tables as default** | Preserves all desktop columns with little work. | Poor mobile UX and fails “no horizontal overflow” expectation. Also makes touch actions hard. | Convert tables/grids to card lists or primary-column + expandable details; allow internal scroll only for unavoidable code/JSON blocks. |
| **Hide governance fields to fit mobile** | Cleaner mobile screens. | Can remove safety/audit context needed for decisions. | Show summaries first and move full metadata into expandable details; preserve access to all critical inspect fields. |
| **Gesture-heavy custom interactions** | Feels app-like. | Hard to discover, hard to test, and can conflict with browser scrolling/back gestures. | Prefer explicit buttons, drawers, accordions, and standard links. |
| **Offline admin mode with cached sensitive data** | Helpful on unreliable mobile networks. | Risky for governance/audit/tool payload privacy; complex cache invalidation and auth. | Show stale/reconnect states; do not cache sensitive admin payloads offline in this milestone. |
| **Push notifications / background run monitoring** | Mobile operators want alerts. | Requires notification permission, service worker, auth/session handling, and product policy decisions. | Defer; expose copyable/deep-linkable run/admin URLs and clear live/reconnect states. |
| **Agent execution feature expansion during mobile milestone** | Tempting to add new mobile-specific agent actions. | Scope creep; milestone is adaptation of existing capabilities. | Keep backend/API semantics unchanged; only add UI affordances and tests unless a tiny API field is required for mobile display. |
| **Device-specific layouts for every phone model** | Teams may try to perfect individual devices. | Unmaintainable and brittle. | Use mobile-first breakpoints and test representative viewport classes. |
| **Viewport zoom disabling as a blanket fix** | Prevents layout shifts. | Hurts accessibility and is discouraged for responsive sites. | Build layouts that reflow and remain usable with browser zoom/text scaling where practical. |

## Acceptance Behaviors and Candidate REQ-IDs

These are written so they can be copied into phase requirements as user-centric, testable acceptance criteria.

### Mobile Shell and Navigation

| Candidate ID | Acceptance Behavior | Complexity | Depends On |
|--------------|---------------------|------------|------------|
| **MOBILE-H5-REQ-001** | As a mobile user, I can open every existing Console and Admin Governance route at a 390×844 phone viewport without blank screens, route errors, or desktop-only blocking messages. | MEDIUM | All existing Vaadin routes. |
| **MOBILE-H5-REQ-002** | As a mobile user, I can navigate from Console to Agent Catalog/session areas and from Admin overview to registry/operations/MCP/plugin/extension/policy/audit pages through a touch-friendly drawer or compact navigation. | MEDIUM | Shared app shell/navigation. |
| **MOBILE-H5-REQ-003** | As a mobile user, the page body has no horizontal overflow at 360, 390/393, 412/430, 768, and landscape representative viewports. | MEDIUM | Responsive CSS/theme and page layouts. |
| **MOBILE-H5-REQ-004** | As a touch user, primary links/buttons/toggles on mobile are large enough to tap reliably and meet or exceed WCAG 2.2 AA target-size minimum/spacing, with 44px target preferred for primary controls. | MEDIUM | Shared button/link/card styles. |
| **MOBILE-H5-REQ-005** | As a keyboard/tablet user, focus indicators remain visible and navigation/drawer/dialog focus order remains usable after mobile layout changes. | MEDIUM | Accessibility styles and Vaadin component configuration. |

### Console, Catalog, Chat, Run, and Session Flow

| Candidate ID | Acceptance Behavior | Complexity | Depends On |
|--------------|---------------------|------------|------------|
| **MOBILE-H5-REQ-010** | As a mobile user, I can browse the Agent Catalog as stacked cards and start/select the general agent without needing a desktop-width grid. | LOW-MEDIUM | Existing Agent Catalog API/cards. |
| **MOBILE-H5-REQ-011** | As a mobile user, I can type a multi-line chat prompt, submit it, and see the composer state change while the run is active. | HIGH | Existing create session/run APIs. |
| **MOBILE-H5-REQ-012** | As a mobile user, I can observe live SSE run output/events in a vertical feed and scroll previous events without losing access to current run controls. | HIGH | Existing EventStreamClient/SSE DTOs. |
| **MOBILE-H5-REQ-013** | As a mobile user, I can inspect run timeline events as compact cards/accordions with status, timestamp/type, summary, and expandable details. | MEDIUM-HIGH | Existing event/timeline model. |
| **MOBILE-H5-REQ-014** | As a mobile user, I can inspect tool cards with tool name, source, status, policy/approval state, duration, error, redacted input/output summary, and expandable details. | MEDIUM | Existing governed tool cards/audit redaction. |
| **MOBILE-H5-REQ-015** | As a mobile user, I can approve or reject a pending tool approval from a card that clearly shows risk/side-effect context and requires an intentional tap. | HIGH | Existing approval APIs/cards and policy metadata. |
| **MOBILE-H5-REQ-016** | As a mobile user, I can open session history, select a past session, continue it, and see which session is active. | MEDIUM | Existing session history/continuation APIs. |
| **MOBILE-H5-REQ-017** | As a mobile user, I can cancel an active run from a visible control and see cancelling/cancelled/failed/completed terminal state feedback. | MEDIUM | Existing cancel API and run status display. |

### Admin Governance Full-Site Support

| Candidate ID | Acceptance Behavior | Complexity | Depends On |
|--------------|---------------------|------------|------------|
| **MOBILE-H5-REQ-020** | As a mobile admin, I can read the Governance Overview as stacked status cards with runtime/provider/tool/extension/MCP/plugin health, counts, messages, and links. | MEDIUM | Existing Governance Overview DTO. |
| **MOBILE-H5-REQ-021** | As a mobile admin, I can inspect Registry and Operations data as cards or responsive row details without relying on horizontal table scrolling. | HIGH | Existing registry/operations views and DTOs. |
| **MOBILE-H5-REQ-022** | As a mobile admin, I can inspect MCP server/tool status, refresh/status metadata where already supported, and identify unhealthy/disconnected states. | MEDIUM-HIGH | Existing MCP governance APIs/views. |
| **MOBILE-H5-REQ-023** | As a mobile admin, I can inspect Plugin state, selected/disabled/quarantined/load errors, and available plugin metadata in a stacked card/detail layout. | MEDIUM-HIGH | Existing plugin governance APIs/views. |
| **MOBILE-H5-REQ-024** | As a mobile admin, I can inspect Extension contributions/providers/tools/listeners with source/type/status and expandable metadata. | MEDIUM-HIGH | Existing extension governance APIs/views. |
| **MOBILE-H5-REQ-025** | As a mobile admin, I can inspect Policy decisions with decision, reason, tool/run/session IDs, timestamp, and expandable redacted context. | HIGH | Existing policy decisions API/view. |
| **MOBILE-H5-REQ-026** | As a mobile admin, I can inspect Audit summaries with actor/source/action/status/timestamp and expandable redacted details. | HIGH | Existing audit API/view. |

### Browser, Orientation, and Verification

| Candidate ID | Acceptance Behavior | Complexity | Depends On |
|--------------|---------------------|------------|------------|
| **MOBILE-H5-REQ-030** | As a QA gate, Playwright runs mobile projects for representative Mobile Chrome and Mobile Safari/WebKit device profiles, plus at least one tablet profile or custom tablet viewport. | MEDIUM | Existing `playwright.config.ts`; Playwright devices registry. |
| **MOBILE-H5-REQ-031** | As a QA gate, mobile smoke tests verify route load, no body horizontal overflow, visible primary action, and at least one key interaction per route category. | MEDIUM-HIGH | E2E page objects/selectors/data attributes. |
| **MOBILE-H5-REQ-032** | As a QA gate, the Console mobile E2E starts a fake/no-key run, observes streamed event UI, opens a tool/approval/session area, and cancels or reaches terminal status. | HIGH | Existing no-key Playwright product path and fake model/tool/MCP/plugin setup. |
| **MOBILE-H5-REQ-033** | As a QA gate, Admin mobile E2E opens overview, registry, operations, MCP, plugin, extension, policy, and audit pages and verifies each has mobile card/detail content. | HIGH | Existing admin routes and stable selectors. |
| **MOBILE-H5-REQ-034** | As a QA gate, representative portrait and landscape viewports pass no-horizontal-overflow and primary-navigation checks. | MEDIUM | Playwright viewport/device configs. |
| **MOBILE-H5-REQ-035** | As a release note/UAT gate, real mobile browser coverage is documented for Android Chrome and iOS Safari; browser gaps are tracked explicitly if CI can only emulate. | LOW-MEDIUM | Human UAT or device farm. |

## Feature Dependencies

```text
Existing Vaadin routes + REST/SSE DTOs
    └──requires──> Shared mobile shell/navigation
                       └──requires──> Full-site route coverage tests

ConsoleView three-column workbench
    └──requires──> Mobile IA refactor: primary chat/run feed + secondary drawers/details
                       └──requires──> Chat composer + SSE feed + run controls mobile acceptance

Existing Agent Catalog API/cards
    └──requires──> Mobile stacked catalog cards
                       └──enables──> Mobile start-chat path

Existing Run events/tool cards/approval cards
    └──requires──> Mobile event card schema + expandable details
                       └──enables──> Run focus mode and risk-first approval UX

Existing Admin Governance DTOs/views
    └──requires──> Responsive card/detail layouts replacing wide-table dependence
                       └──enables──> Mobile incident triage mode

Shared responsive CSS/theme tokens
    └──requires──> No body horizontal overflow + touch targets + readable typography
                       └──enables──> Playwright mobile smoke gate

Existing Playwright desktop E2E
    └──requires──> Mobile device projects + viewport assertions + touch-enabled checks
                       └──must preserve──> Desktop regression coverage
```

### Dependency Notes

- **Mobile IA must precede per-page polish:** The current console is explicitly a three-column workbench. If this remains the mental model on phones, individual CSS fixes will not make Chat/Run usable.
- **Shared mobile card/detail patterns should precede Admin page-by-page conversion:** Registry, Operations, MCP, Plugin, Extension, Policy, and Audit pages all have the same problem: dense operational metadata. A shared pattern reduces duplicated layout and tests.
- **No-horizontal-overflow gate should be implemented early:** It is objective, easy to regress, and catches most desktop-grid leakage.
- **Playwright mobile projects depend on stable selectors:** Before writing many tests, ensure mobile elements have route/category data attributes and reliable labels.
- **Public REST/SSE boundary should remain unchanged:** Most work should be Vaadin layout/component adaptation. Add API fields only if a mobile summary cannot be produced from existing DTOs.

## MVP Definition

### Launch With (v1.1 Mobile H5)

Minimum viable mobile-first adaptation for this milestone.

- [ ] **Full-site mobile route coverage** — every existing Console/Admin route loads and is usable at phone viewport.
- [ ] **Shared responsive app shell/navigation** — mobile drawer/compact nav with route title and touch-friendly links.
- [ ] **Console mobile IA refactor** — chat/run feed first; sessions/catalog/context move to drawers, tabs, or collapsible panels.
- [ ] **Mobile Chat/Run path** — user can select/browse agent, submit chat, observe SSE events, inspect run/tool/approval cards, continue session, cancel active run.
- [ ] **Admin Governance mobile cards/details** — overview, registry, operations, MCP, plugin, extension, policy, audit are readable without page-level horizontal overflow.
- [ ] **Touch/readability/accessibility baseline** — touch targets, visible focus, readable typography, safe dialogs/drawers.
- [ ] **Playwright mobile smoke gate** — representative phone/tablet projects and key route/action assertions; desktop E2E remains passing.

### Add After Validation (v1.1.x)

Useful once the full-site mobile baseline is stable.

- [ ] **Run focus mode** — active run layout that prioritizes stream, approvals, cancel, and latest tool activity.
- [ ] **Mobile incident triage shortcuts** — overview links to latest unhealthy provider/tool/MCP/plugin/audit items.
- [ ] **Event filters/summarization on mobile** — filter run feed by errors/tools/approvals/model output.
- [ ] **Deep-linkable expanded details** — direct links to run/audit/policy/plugin/MCP detail cards open expanded on mobile.
- [ ] **Mobile evidence copy/share** — copy IDs/redacted summaries for incident handoff.
- [ ] **Poor-network/reconnect polish** — explicit stale/reconnecting state for SSE and admin refresh.

### Future Consideration (v2+)

Defer beyond the H5 adaptation milestone.

- [ ] **Native mobile app** — only after H5 proves mobile workflows and API contract needs.
- [ ] **Push notifications/background monitoring** — requires separate auth/session/notification/service-worker decisions.
- [ ] **Full PWA/offline admin mode** — sensitive governance/audit data caching needs security design.
- [ ] **Mobile-specific new Agent capabilities** — should be product-driven, not hidden inside responsive adaptation.

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Full-site mobile route coverage | HIGH | MEDIUM | P1 |
| Shared mobile shell/navigation | HIGH | MEDIUM | P1 |
| No horizontal overflow gates | HIGH | MEDIUM | P1 |
| Console mobile IA refactor | HIGH | HIGH | P1 |
| Chat composer + SSE feed mobile support | HIGH | HIGH | P1 |
| Tool/approval/session/cancel mobile support | HIGH | MEDIUM-HIGH | P1 |
| Admin Governance mobile cards/details | HIGH | HIGH | P1 |
| Touch target/readability/focus baseline | HIGH | MEDIUM | P1 |
| Playwright mobile projects and smoke tests | HIGH | MEDIUM-HIGH | P1 |
| Run focus mode | MEDIUM-HIGH | MEDIUM | P2 |
| Mobile incident triage mode | MEDIUM-HIGH | MEDIUM-HIGH | P2 |
| Event filters/summarization | MEDIUM | MEDIUM | P2 |
| Deep-linkable mobile details | MEDIUM | MEDIUM-HIGH | P2 |
| PWA-like polish without offline cache | LOW-MEDIUM | LOW-MEDIUM | P3 |
| Push notifications | MEDIUM | HIGH | P3 / defer |
| Native app | MEDIUM | VERY HIGH | P3 / anti-feature now |

**Priority key:**
- **P1:** Must have for mobile H5 milestone acceptance.
- **P2:** Should have after baseline if time remains or as v1.1.x follow-up.
- **P3:** Nice-to-have/future; avoid in initial milestone.

## Existing Surface Dependency Map

| Existing Surface | Mobile Adaptation Expected | API Changes Expected? | Test Focus |
|------------------|----------------------------|-----------------------|------------|
| Agent Catalog | Stacked cards; clear agent selection/start affordance. | No. | Catalog route visible; select/start path. |
| Chat/Run Console | Single-column run feed; bottom/visible composer; context in drawer/details. | No. | Submit prompt; observe run; no overflow. |
| Run timeline/events | Vertical event feed/cards; expandable details. | No. | Event cards render model/tool/terminal states. |
| Tool cards | Compact summary + expandable redacted payload/error/details. | No. | Tool card readable and expandable. |
| Approval cards | Risk-first card; clear approve/reject actions; touch-safe. | No unless missing risk metadata. | Approve/reject visible and intentional. |
| Session history/continuation/cancel | Drawer/list; active state; visible cancel during run. | No. | Select/continue/cancel at mobile viewport. |
| Admin overview | Stacked health cards and links. | No. | Health cards and links visible. |
| Admin registry/operations | Cards or grid row details instead of wide table dependence. | No, unless server-side projection needed for very heavy grids. | Route loads; row/card details readable. |
| Admin MCP/plugin/extension | Status cards with metadata/details/actions already supported. | No. | Status/disabled/quarantine/error states visible. |
| Admin policy/audit | Filter/search controls stacked; decision/audit cards; expandable redacted context. | No. | Decision/audit summary and details accessible. |
| Playwright E2E | Add mobile projects and route/action smoke. | No. | Devices, touch, viewport, overflow assertions. |

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Mobile responsive/table-stakes expectations | HIGH | Verified with MDN responsive design/viewport guidance, W3C WCAG 2.2 target/focus/reflow guidance, and product milestone constraints. |
| Vaadin implementation direction | MEDIUM-HIGH | Context7 and official Vaadin docs confirm AppLayout responsiveness, full-height layout patterns, and Grid row details. Specific codebase refactor effort depends on current component implementation and CSS baseline. |
| Playwright mobile verification | HIGH | Playwright docs confirm device profiles, mobile Safari/Chrome emulation, viewport and touch options. Real iOS Safari remains UAT/device-farm dependent. |
| Admin governance mobile feature mapping | MEDIUM-HIGH | Directly derived from existing project surfaces and DTO boundaries; less dependent on external market evidence. |
| Differentiators | MEDIUM | Product-specific recommendations inferred from Agent/Admin operational use cases; validate with user/operator feedback if available. |

## Sources

- Project context: `.planning/PROJECT.md`, current milestone v1.1 mobile H5 adaptation, existing validated surfaces and constraints. Confidence: HIGH.
- Vaadin Flow docs via Context7 `/vaadin/flow` and `/websites/vaadin`: AppLayout is responsive for desktop/tablet/mobile; full-height app/grid examples; Grid item details renderers for row expansion. Confidence: HIGH for capability, MEDIUM-HIGH for exact implementation fit.
- Vaadin official search results: App Layout docs and API state responsive drawer/navbar behavior; Grid docs and renderer/details guidance. Confidence: MEDIUM-HIGH.
- W3C WCAG 2.2 docs via Context7 `/websites/w3_wai_wcag22`: target size examples, focus appearance, `:focus-visible`, responsive reflow techniques, responsive images. Confidence: HIGH.
- W3C WAI target-size official pages: WCAG 2.5.8 target size minimum and 2.5.5 enhanced target size. Confidence: HIGH.
- MDN/Web.dev official responsive design and viewport guidance found via web search: responsive design adapts to screen sizes/resolutions and mobile viewport behavior. Confidence: HIGH.
- Playwright docs via Context7 `/microsoft/playwright`: devices registry, Mobile Safari/Chrome projects, mobile context, `hasTouch`, viewport/device emulation. Confidence: HIGH.

---
*Feature research for: mobile-first H5 adaptation of Pi Java Agent Web Console/Admin Governance*  
*Researched: 2026-06-20*
