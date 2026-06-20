# Architecture Research: Mobile-First H5 Adaptation

**Project:** Pi Java Agent Platform  
**Domain:** Java/Vaadin Agent Web Console and Admin Governance mobile-first H5 adaptation  
**Researched:** 2026-06-20  
**Confidence:** HIGH for architecture boundaries and Vaadin/Playwright integration; MEDIUM for exact class/file names until implementation inventory is finalized.

## Executive Recommendation

Integrate mobile-first H5 support as a **Vaadin adapter-layer refactor**, not as a new frontend product, not as public API expansion, and not as runtime/domain work. The existing COLA boundary should remain unchanged: `pi-agent-domain`, `pi-agent-app`, `pi-agent-client`, infrastructure modules, REST DTOs, SSE event envelopes, `ConsoleHttpClient`, and `EventStreamClient` are stable inputs. The milestone should modify the Vaadin view/layout/component composition in `pi-agent-adapter-web`, add a project theme/CSS asset set, and extend Playwright E2E with mobile viewport/browser projects.

The primary architectural change is **presentation structure**, not data flow. Existing pages are desktop-first and class-driven (`pi-console-workbench`, `pi-console-sessions`, `pi-console-chat`, `pi-console-run-context`, `pi-admin-*`). Mobile-first adaptation should introduce a shared responsive shell, reusable mobile primitives, and CSS breakpoints that transform wide multi-column views into single-column, drawer/tabs/accordion/card flows. REST/SSE/read-model contracts should not change unless a specific page cannot render efficiently from existing DTOs; even then, prefer read-model projection changes behind existing client boundaries rather than UI-specific domain changes.

The recommended build order is: baseline responsive theme and test harness first, shared shell/navigation second, console workbench third, cards/timeline/approval interaction fourth, admin governance pages fifth, and finally cross-device hardening. This order creates a measurable mobile acceptance gate before high-volume page work, then tackles the most user-critical path before broad admin coverage.

## Standard Architecture

### System Overview

```text
┌─────────────────────────────────────────────────────────────────────┐
│                         Browser / H5 Clients                         │
│  Mobile Chrome / Mobile Safari / Edge / Firefox / Tablet / Desktop   │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ HTTP + Vaadin client engine + SSE
┌───────────────────────────────▼─────────────────────────────────────┐
│ pi-agent-adapter-web                                                 │
│                                                                       │
│  ┌──────────────────────┐   ┌─────────────────────────────────────┐  │
│  │ Responsive Shell      │   │ Vaadin Routes / Views               │  │
│  │ - app header          │   │ - ConsoleView                       │  │
│  │ - mobile nav drawer   │   │ - Agent Catalog                     │  │
│  │ - admin nav           │   │ - Chat/Run/Timeline/Approvals       │  │
│  └──────────┬───────────┘   │ - Admin Governance pages             │  │
│             │               └─────────────────┬───────────────────┘  │
│  ┌──────────▼─────────────────────────────────▼───────────────────┐  │
│  │ Shared Mobile UI Primitives                                      │  │
│  │ - PageScaffold, ResponsiveSection, MobileActionBar               │  │
│  │ - cards, status chips, collapsible details, touch-safe buttons    │  │
│  └──────────┬──────────────────────────────────────────────────────┘  │
│             │                                                         │
│  ┌──────────▼──────────────────────────────────────────────────────┐  │
│  │ Theme and CSS Assets                                             │  │
│  │ frontend/themes/pi-agent/styles.css                              │  │
│  │ responsive.css, console.css, admin.css, components.css            │  │
│  └──────────┬──────────────────────────────────────────────────────┘  │
│             │                                                         │
│  ┌──────────▼───────────┐       ┌──────────────────────────────────┐  │
│  │ ConsoleHttpClient     │       │ EventStreamClient                │  │
│  │ REST/read-model paths │       │ SSE run event stream paths       │  │
│  └──────────┬───────────┘       └──────────────┬───────────────────┘  │
└─────────────┼──────────────────────────────────┼──────────────────────┘
              │                                  │
┌─────────────▼──────────────────────────────────▼──────────────────────┐
│ Existing public REST/SSE and COLA application/runtime layers            │
│ pi-agent-client DTOs → App services → Domain runtime → Infrastructure   │
└────────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Status | Responsibility | Typical Implementation |
|-----------|--------|----------------|------------------------|
| `pi-agent-adapter-web` Vaadin routes | **Modified** | Convert existing Console/Admin pages from desktop-first composition to mobile-first H5 composition. | Java Vaadin Flow views/components plus CSS class hooks. |
| `MainConsoleLayout` / admin layout shell | **Modified** | Become a responsive application shell: compact header, mobile drawer/tabs, desktop navigation, safe content container. | Vaadin `AppLayout`/drawer-style composition or equivalent Flow layout; keep Java-first. |
| `ConsoleView` | **Modified** | Replace fixed three-column workbench with responsive mode: mobile order `chat → run context → sessions/catalog` with drawer/sections; desktop can keep three columns. | Keep `ConsoleHttpClient` and `EventStreamClient`; change layout and CSS hooks only. |
| Console panels (`SessionListPanel`, `AgentCatalogPanel`, `ChatEventStreamPanel`, `RunContextPanel`) | **Modified** | Make panels independently usable in narrow containers; avoid assumptions that sibling columns are visible. | Add semantic class names, compact states, collapsible sections, sticky input/action regions. |
| Cards (`AgentCard`, `ToolCallCard`, `ApprovalCard`) | **Modified** | Become touch-safe, readable mobile cards; long payloads wrap or collapse; destructive actions are reachable but guarded. | Vaadin `Div`, `Button`, `Details`/custom collapsible sections, Lumo utility classes, CSS. |
| Admin Governance views (`Admin*View`) | **Modified** | Render operational data as stacked cards/sections on mobile, tables/grids on wide screens where useful. | Shared admin page scaffold plus card/detail components. |
| Vaadin theme assets | **New** | Central mobile-first responsive design system for this app. | `src/main/frontend/themes/pi-agent/` or Vaadin-supported theme resource location, with `styles.css` imports. |
| Responsive Java helper primitives | **New** | Reduce repeated mobile layout code and make tests target stable hooks. | `ui/component/PageScaffold`, `ResponsiveCard`, `StatusBadge`, `MobileActionBar`, etc. |
| Playwright mobile projects/specs | **Modified + New** | Verify mobile viewports/browser emulation, no horizontal overflow, touch interactions, critical user/admin paths. | Extend `playwright.config.ts`; add `e2e/mobile-h5.spec.ts` or per-surface specs. |
| `pi-agent-client` DTOs | **Unchanged by default** | Remain public REST/read-model contract. | No mobile-specific DTOs unless proven necessary. |
| `ConsoleHttpClient` / `EventStreamClient` | **Unchanged by default** | Remain the only UI data access boundaries for REST/SSE. | May add path helpers only if existing route coverage is missing, not for layout. |
| App/Domain/Infrastructure modules | **Unchanged** | Agent runtime, governance, tool/MCP/plugin/model functionality. | No mobile-specific dependencies or conditionals. |

## Recommended Project Structure

Keep all mobile-first work in `pi-agent-adapter-web` plus root E2E config/specs:

```text
pi-agent-adapter-web/
├── src/main/java/io/github/pi_java/agent/adapter/web/ui/
│   ├── MainConsoleLayout.java                 # modify into responsive shell
│   ├── AdminGovernanceLayout.java             # modify into responsive admin shell/nav
│   ├── component/                             # new shared mobile/responsive primitives
│   │   ├── PageScaffold.java
│   │   ├── ResponsiveSection.java
│   │   ├── ResponsiveCard.java
│   │   ├── StatusBadge.java
│   │   └── MobileActionBar.java
│   ├── console/                               # modify existing panels/cards
│   │   ├── ConsoleView.java
│   │   ├── SessionListPanel.java
│   │   ├── AgentCatalogPanel.java
│   │   ├── ChatEventStreamPanel.java
│   │   ├── RunContextPanel.java
│   │   ├── ToolCallCard.java
│   │   └── ApprovalCard.java
│   └── admin/                                 # modify existing governance views
│       ├── AdminGovernanceOverviewView.java
│       ├── AdminOperationsView.java
│       ├── AdminRegistryStatusView.java
│       ├── AdminPolicyDecisionsView.java
│       ├── AdminAuditView.java
│       └── AdminApprovalQueueView.java
├── src/main/frontend/themes/pi-agent/          # new Vaadin theme assets
│   ├── styles.css                              # imports global theme files
│   ├── tokens.css                              # spacing, touch target, breakpoints
│   ├── responsive.css                          # shell and generic responsive rules
│   ├── console.css                             # console workbench/panels/cards
│   ├── admin.css                               # governance pages/cards/lists
│   └── components.css                          # status chips, badges, action bars
└── src/test/java/...                           # modify/add unit tests for classes/hooks

playwright.config.ts                            # add mobile/tablet projects
e2e/
├── phase-05-web-console.spec.ts                # keep desktop regression
├── phase-07-mcp-governance.spec.ts             # keep existing governance smoke
├── phase-08-plugin-governance.spec.ts          # keep existing governance smoke
├── phase-09-operations-governance.spec.ts      # keep existing operations smoke
└── mobile-h5.spec.ts                           # new cross-surface mobile acceptance
```

### Structure Rationale

- **No new frontend module:** The project explicitly avoids React/Next.js or a separate mobile app. Vaadin Flow supports Java-first UI, global/theme CSS, component CSS imports, Lumo utility classes, and responsive layouts.
- **Theme assets centralized:** Responsive behavior must be consistent across Console and Admin. Scattered inline styles or per-view one-off media queries will regress quickly.
- **Shared primitives in Java:** A `ResponsiveCard`/`PageScaffold` layer keeps Java/Vaadin composition readable and makes unit tests assert stable class names and data attributes.
- **E2E outside adapter module stays:** Current Playwright setup is root-level; extend it rather than adding a second browser-test system.

## Architectural Patterns

### Pattern 1: Adapter-Only Mobile Refactor

**What:** Mobile adaptation changes Vaadin layout/component composition and theme CSS; it does not change runtime, tool governance, MCP/plugin infrastructure, or public event semantics.

**When to use:** This milestone. The product already has validated public REST/SSE/read-model contracts and `ConsoleHttpClient`/`EventStreamClient` boundaries.

**Trade-offs:** This limits opportunities for mobile-specific backend shortcuts, but protects future CLI/TUI/API clients and avoids UI-driven domain drift.

**Boundary rule:** If a mobile page needs less data, collapse or lazy-render sections in the UI first. Add backend pagination/projection only when a measured mobile performance issue exists and expose it as a general read-model improvement, not a phone-only endpoint.

### Pattern 2: Mobile-First CSS With Desktop Enhancement

**What:** Default CSS targets narrow screens; wider breakpoints enhance to multi-column desktop/tablet layouts.

**When to use:** All new CSS for Console/Admin routes.

**Example:**

```css
.pi-console-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--pi-space-m);
  max-width: 100%;
  overflow-x: hidden;
}

.pi-console-chat,
.pi-console-run-context,
.pi-console-sessions {
  min-width: 0;
}

@media (min-width: 768px) {
  .pi-console-workbench {
    grid-template-columns: minmax(14rem, 18rem) minmax(0, 1fr);
  }
}

@media (min-width: 1100px) {
  .pi-console-workbench {
    grid-template-columns: minmax(14rem, 18rem) minmax(0, 1fr) minmax(18rem, 24rem);
  }
}
```

**Trade-offs:** CSS handles layout efficiently without Java-side viewport branching, but Java components must expose stable semantic class names and avoid fixed widths.

### Pattern 3: Progressive Disclosure for Dense Agent/Governance Data

**What:** Replace wide rows and always-open details with cards, status chips, and collapsible details on mobile.

**When to use:** Run timeline events, tool call payloads, approval cards, audit rows, MCP/plugin/extension capability lists, operations summaries.

**Trade-offs:** Mobile users see less at once, but they get a reliable task flow. Desktop can still show expanded grids or side-by-side sections.

**Recommended rule:** Every dense entity should have:

1. A one-line summary visible on mobile.
2. Status/severity/provenance chip.
3. Primary action reachable within thumb range.
4. Details collapsed by default if payloads are long.
5. Copy/debug affordances only where safe and redacted.

### Pattern 4: Stable Test Hooks as Architecture Contract

**What:** Existing `data-route`, `data-layout`, `data-action-*`, and class names should become an explicit UI architecture contract for Playwright and unit tests.

**When to use:** All modified/new UI components.

**Example Java hook:**

```java
addClassNames("pi-responsive-page", "pi-console-workbench");
getElement().setAttribute("data-route", "console");
getElement().setAttribute("data-layout", "responsive-workbench");
```

**Trade-offs:** Test hooks add small markup overhead, but prevent brittle CSS/text selectors and make mobile regressions easier to localize.

### Pattern 5: Playwright Device Projects as Acceptance Gate

**What:** Add mobile/tablet projects to `playwright.config.ts` using Playwright `devices` definitions, while preserving desktop Chromium regression.

**When to use:** From the first phase of this milestone, before large UI refactors.

**Example:**

```ts
projects: [
  { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  { name: 'mobile-chrome', use: { ...devices['Pixel 5'] } },
  { name: 'mobile-safari', use: { ...devices['iPhone 13'] } },
  { name: 'tablet', use: { ...devices['iPad Pro 11'] } },
]
```

**Trade-offs:** Browser matrix increases execution time. Keep smoke coverage focused on critical paths and reserve exhaustive route-by-route checks for CSS/layout invariants.

## Data Flow

### Request/Data Flow: No Architectural Change Expected

```text
Mobile browser user action
    ↓
Vaadin route/component in pi-agent-adapter-web
    ↓
ConsoleHttpClient path helper / EventStreamClient stream helper
    ↓
Existing REST/SSE endpoints and pi-agent-client DTOs
    ↓
App services → Domain runtime/governance → Infrastructure
    ↓
Existing read models/events
    ↓
Vaadin component state + responsive CSS rendering
```

### Key Data Flow Changes

1. **No REST/SSE contract change by default:** Mobile H5 consumes the same public DTOs and event envelopes as desktop.
2. **No new mobile-specific backend state:** Viewport, drawer open/closed state, selected tab/section, and collapsible detail state live in Vaadin component/UI state only.
3. **Optional lazy UI rendering:** For long timelines/audit lists, defer rendering hidden details in Vaadin components, but keep source data boundaries unchanged.
4. **Optional read-model improvement:** If mobile tests reveal payload/performance problems, add generic pagination/filter/summarization to public read models, usable by desktop/API/future CLI too.
5. **SSE stream unchanged:** `EventStreamClient` should continue to describe run event stream paths; mobile only changes how deltas/events are presented.

### State Management

```text
Server-side Vaadin UI instance
├── selected route/page
├── selected session/run/agent
├── active mobile section/drawer/tabs
├── expanded/collapsed details
└── rendered event/card state

Persistent platform state
├── sessions/runs/events/tool calls/audit
└── unchanged app/domain/infrastructure model
```

## Modified Components

### Console Surface

| Component | Required Mobile Change | Boundary to Preserve |
|-----------|------------------------|----------------------|
| `ConsoleView` | Replace `data-layout="three-column-workbench"` with responsive layout semantics; reorder/nest sections for mobile; expose hooks for no-overflow tests. | Continue using `ConsoleHttpClient` and `EventStreamClient`; keep run/session planning semantics. |
| `SessionListPanel` | Fit as drawer/collapsible section on phone; avoid fixed widths; include tap-friendly session rows. | Must not query backend directly. |
| `AgentCatalogPanel` / `AgentCard` | Render catalog as stacked cards; compact model/tool metadata; avoid horizontal card overflow. | Keep agent catalog path/DTO access through existing client boundary. |
| `ChatEventStreamPanel` | Prioritize message stream and input; support long model deltas/tool text wrapping; avoid viewport-jumping where possible. | Event semantics and rendering order remain based on SSE/read events. |
| `RunContextPanel` | Convert side panel to mobile section/bottom summary/card stack; keep status and cancel reachable. | Cancel still planned through existing REST path/request. |
| `ToolCallCard` | Collapse raw arguments/results; show status/provenance/approval state first; ensure redaction remains visible. | Do not expose raw secrets or bypass existing redaction. |
| `ApprovalPanel` / `ApprovalCard` | Touch-safe approve/reject controls; confirmation/role context readable; no tiny adjacent destructive actions. | Approval API and actor role semantics unchanged. |

### Admin Governance Surface

| Component | Required Mobile Change | Boundary to Preserve |
|-----------|------------------------|----------------------|
| `AdminGovernanceLayout` | Mobile admin nav drawer/section index; desktop can remain broad navigation. | Admin pages remain inspect/governance adapter views. |
| `AdminGovernanceOverviewView` | Stack health/status summaries as cards; make counts/severity visible first. | Use existing overview DTO/path. |
| `AdminOperationsView` | Render operations metrics as compact cards; avoid wide dashboards. | Observability/read-model source unchanged. |
| `AdminRegistryStatusView` | MCP/plugin/extension/provider/tool entries become responsive cards with collapsible capability details. | Refresh/disable/quarantine action paths remain existing `ConsoleHttpClient` helpers. |
| `AdminPolicyDecisionsView` | Policy decisions as timeline/card list; reason/details collapsed. | Existing policy read model unchanged unless generic pagination needed. |
| `AdminAuditView` | Audit rows as searchable/filterable card list; wrap identifiers safely; preserve redaction indicators. | Existing audit DTO/path unchanged unless generic pagination needed. |
| `AdminApprovalQueueView` | Queue items as approval cards with large actions and status. | Approval governance semantics unchanged. |

### Tests and Build Assets

| Asset | Change | Purpose |
|-------|--------|---------|
| `playwright.config.ts` | Add mobile/tablet projects via Playwright `devices`; keep desktop Chromium. | Validate mobile H5 against emulated Pixel/iPhone/tablet profiles. |
| `e2e/mobile-h5.spec.ts` | New smoke/invariant tests. | Console critical path, admin navigation, governance cards, no horizontal overflow, touch action visibility. |
| Existing E2E specs | Keep; optionally tag/structure for desktop vs mobile. | Prevent desktop regression while mobile refactor proceeds. |
| Java component tests | Add assertions for responsive class names, data hooks, action path preservation. | Catch accidental removal of architecture/test hooks without browser startup. |
| Theme files | New Vaadin frontend theme. | Centralize tokens, breakpoints, mobile-first layout rules. |

## New Theme/CSS Assets

Recommended asset split:

| File | Purpose | Key Content |
|------|---------|-------------|
| `styles.css` | Theme entrypoint. | Imports `tokens.css`, `responsive.css`, `console.css`, `admin.css`, `components.css`. |
| `tokens.css` | Mobile-first design tokens. | `--pi-space-*`, `--pi-touch-target-min`, `--pi-content-max`, breakpoint custom properties/comments. |
| `responsive.css` | App shell/page layout primitives. | `.pi-responsive-page`, `.pi-page-header`, `.pi-mobile-nav`, safe viewport rules, no-overflow defaults. |
| `console.css` | Console-specific responsive layout. | Workbench grid, chat stream, session/catalog drawer/stack, run context behavior. |
| `admin.css` | Governance-specific responsive layout. | Admin card lists, registry capability wrapping, operations summaries, audit/policy cards. |
| `components.css` | Shared cards/badges/action bars. | Status chips, risk badges, approval action bars, long-token wrapping. |

CSS architectural rules:

- Default styles are phone/narrow viewport styles.
- Add enhancements at tablet and desktop breakpoints; do not make desktop the base.
- Set `min-width: 0` on grid/flex children that contain event text, JSON, identifiers, URLs, or tool payloads.
- Use `overflow-wrap: anywhere` for IDs, plugin names, MCP server URLs, trace IDs, and JSON fragments.
- Avoid fixed pixel widths except minimum touch target and known icons.
- Prefer CSS/layout over Java viewport detection. Java should expose structure; CSS should adapt it.
- Keep Vaadin component shadow DOM styling explicit if needed via supported theme/component CSS mechanisms.

## Boundaries to Preserve

| Boundary | Must Preserve | Why |
|----------|---------------|-----|
| Java/Vaadin-first UI | Do not introduce React/Next.js, separate mobile app, or TypeScript product UI. | Project constraint and stack strategy. |
| `pi-agent-client` public contracts | No mobile-specific DTO forks. | Future CLI/TUI/API clients rely on stable public contracts. |
| `ConsoleHttpClient` / `EventStreamClient` | UI data access goes through these helpers. | Prevents Vaadin views from coupling to controllers or runtime internals. |
| COLA layering | No mobile code in Domain/App/Infrastructure. | Mobile H5 is presentation/adaptation work. |
| Tool governance/redaction | Mobile cards must not reveal hidden payloads or bypass approval semantics. | Cloud safety remains primary risk. |
| SSE event ordering/replay | Responsive rendering must not reorder persisted run events semantically. | Debuggability/audit depends on chronological event evidence. |
| Existing desktop behavior | Desktop E2E stays in matrix. | Mobile-first does not mean desktop regression is acceptable. |
| Route stability | Keep current routes unless a migration plan is explicit. | Existing docs/tests/users may rely on `/console` and `/admin/governance/*`. |

## Anti-Patterns

### Anti-Pattern 1: “Responsive CSS Patch” Without Information Architecture

**What people do:** Add a few media queries to shrink the three-column desktop workbench.

**Why it's wrong:** The milestone explicitly requires mobile-first IA. A three-column run cockpit does not become usable on phones by narrowing columns.

**Do this instead:** Define mobile task order and progressive disclosure per surface: chat first, status/actions second, context/catalog/history in drawer/sections.

### Anti-Pattern 2: Mobile-Specific Backend Forks

**What people do:** Add `/mobile/*` endpoints or mobile-only DTOs because a page feels dense.

**Why it's wrong:** It fragments public contracts and violates the existing REST/SSE/read-model boundary.

**Do this instead:** Recompose the UI first. If data volume is truly excessive, add general pagination/projection to existing read models.

### Anti-Pattern 3: Viewport Logic in Domain/App

**What people do:** Pass mobile flags into application services or alter runtime behavior for H5.

**Why it's wrong:** Runtime must remain general and not bind to Chat/UI/mobile clients.

**Do this instead:** Keep viewport/layout state in Vaadin UI and CSS.

### Anti-Pattern 4: Wide Tables as Admin Default

**What people do:** Keep desktop table/grid layouts for audit, policy, registry, MCP, and plugin pages.

**Why it's wrong:** Wide operational data is the fastest path to horizontal overflow and unusable mobile admin.

**Do this instead:** Use mobile card lists with collapsed details; enhance to grids only on wider screens.

### Anti-Pattern 5: Browser Matrix Without Assertions

**What people do:** Add mobile Playwright projects but only load pages.

**Why it's wrong:** Smoke navigation alone misses horizontal overflow, hidden action buttons, and inaccessible critical paths.

**Do this instead:** Assert layout invariants and complete key flows: create run, view stream/tool card, cancel/approval action visibility, admin registry/plugin/MCP status visibility.

## Integration Points

### Internal Boundaries

| Boundary | Communication | Mobile Integration Rule |
|----------|---------------|-------------------------|
| Vaadin views ↔ `ConsoleHttpClient` | Path helper/object boundary | Continue to plan REST actions through helper methods; do not call controllers/services directly. |
| Vaadin views ↔ `EventStreamClient` | SSE connection spec boundary | Keep stream URL/replay semantics unchanged; mobile only affects rendering. |
| Vaadin UI ↔ `pi-agent-client` DTOs | Public DTO consumption | Reuse existing DTOs. Add general read-model fields only when necessary and documented. |
| `pi-agent-adapter-web` ↔ App/Domain | Existing controllers/services | No mobile-specific use cases in App/Domain. |
| Theme CSS ↔ Java components | Class names/data attributes | Java components expose semantic hooks; CSS implements responsive behavior. |
| Playwright ↔ UI | Stable `data-*` hooks and routes | Treat hooks as test contract; avoid text-only selectors for layout checks. |

### External Services

No new external runtime service is required. Playwright browser dependencies already exist for E2E and should be extended with mobile emulation projects. Real-device manual UAT remains useful for iOS Safari quirks, but the automated gate should use Playwright-supported Chromium/WebKit/Firefox profiles.

## Suggested Build Order

### Phase 1 — Responsive Baseline and Test Harness

**Build:**

- Add Vaadin theme folder and `styles.css` import structure.
- Add design tokens and global no-overflow defaults.
- Extend `playwright.config.ts` with `mobile-chrome`, `mobile-safari`, and tablet projects.
- Add `mobile-h5.spec.ts` with route smoke and no-horizontal-overflow assertions.
- Add Java unit tests for stable responsive class/data hooks on representative components.

**Why first:** Establishes the acceptance gate before broad UI changes.

**Exit criteria:** Existing desktop E2E still passes; mobile projects can load `/console` and admin routes; no baseline horizontal overflow on simple pages.

### Phase 2 — Shared Responsive Shell and Navigation

**Build:**

- Refactor `MainConsoleLayout` and `AdminGovernanceLayout` into responsive shells.
- Add shared `PageScaffold`, `ResponsiveSection`, `MobileActionBar`, and navigation primitives.
- Preserve existing routes and page titles.

**Why second:** Every page depends on a usable shell/nav; doing page-by-page layout before shell creates rework.

**Exit criteria:** Mobile users can navigate Console and Admin Governance routes without horizontal scroll or unreachable nav.

### Phase 3 — Console Workbench Mobile-First Flow

**Build:**

- Refactor `ConsoleView` from fixed three-column semantics to responsive workbench.
- Prioritize chat/run status/actions on phone; move sessions/catalog into drawer/collapsible sections.
- Keep `planChatSubmission`, `selectSession`, `planCancelRunningRun`, and stream planning behavior intact.

**Why third:** Console is the primary user surface and exercises REST + SSE + session/run state.

**Exit criteria:** Mobile E2E can select/see agent context, submit chat/run, observe event stream area, inspect run status, and see cancel action without horizontal overflow.

### Phase 4 — Runtime Cards, Timeline, Tool, and Approval UX

**Build:**

- Refactor `ToolCallCard`, `ApprovalCard`, `ApprovalPanel`, run event/timeline rendering, status badges.
- Add progressive disclosure for tool inputs/results and long event payloads.
- Ensure redaction/approval/risk indicators remain prominent.

**Why fourth:** These components are reused in Console and Admin approval/governance views; they also carry safety semantics.

**Exit criteria:** Mobile tests verify tool/approval cards fit viewport, actions are touch-safe, redaction markers are visible, and long identifiers/payloads wrap.

### Phase 5 — Admin Governance Full-Site Coverage

**Build:**

- Convert overview, operations, registry, MCP/plugin/extension, policy decisions, audit, and approval queue pages to responsive card/section layouts.
- Keep inspect-only and mutation-control semantics visible through data attributes and text.
- Add mobile E2E coverage for each admin route category.

**Why fifth:** Admin has broad surface area; shared shell/cards should exist first.

**Exit criteria:** Every existing Admin Governance route is reachable and readable on phone/tablet; key refresh/disable/quarantine/approval action plans remain visible and tested.

### Phase 6 — Cross-Browser Hardening and Documentation

**Build:**

- Tune Safari/WebKit quirks, orientation/tablet breakpoints, keyboard/input behavior.
- Add final mobile acceptance checklist to project docs.
- Ensure screenshots/traces are retained on mobile failures in CI.

**Why last:** Hardening is most useful after all surfaces are converted.

**Exit criteria:** Desktop + mobile Chrome + mobile Safari/WebKit + tablet Playwright smoke pass; no route has horizontal overflow; critical Console/Admin actions complete under mobile viewport.

## Dependency Map

```text
Theme/test harness
  → Responsive shell/navigation
    → Console workbench layout
      → Shared runtime cards/tool/approval components
        → Admin governance route conversion
          → Cross-browser/orientation hardening
```

Critical dependency rules:

- Page conversion depends on shared theme and shell.
- Admin conversion depends on reusable cards/details patterns.
- Mobile E2E should exist before refactoring high-risk pages.
- DTO/API changes, if any, must be justified by generic read-model needs and come after UI-only options are exhausted.

## Scaling and Performance Considerations

| Concern | Mobile Risk | Recommended Architecture Response |
|---------|-------------|-----------------------------------|
| Long SSE timelines | Phone DOM becomes heavy and scroll janky. | Render compact event summaries; collapse details; consider generic event pagination/virtualization later. |
| Large tool payloads | Horizontal overflow or secret exposure through raw JSON. | Wrap/collapse payloads; preserve redaction; show summaries first. |
| Admin audit/registry lists | Dense tables unusable on phones. | Card lists with progressive disclosure; desktop grid enhancement only at wide breakpoints. |
| Vaadin server-side UI state | More collapsible/drawer state per user session. | Keep state lightweight; do not store large payload copies solely for mobile. |
| E2E matrix time | Mobile projects increase CI duration. | Keep mobile tests smoke/invariant-focused; run exhaustive checks only on changed surfaces or nightly. |

## Roadmap Implications

1. **This milestone is adapter-layer modernization.** It should not reopen runtime/provider/tool/MCP/plugin architecture.
2. **Mobile acceptance must be a first-class gate.** Add the Playwright mobile matrix before page refactors so every later phase can prove progress.
3. **Console before Admin.** Console validates the hardest user-facing flow: catalog/session/chat/run/SSE/tool/approval rendering. Admin then reuses the same shell/cards.
4. **Cards and progressive disclosure are architectural primitives.** Treat them as shared components, not one-off CSS patches.
5. **Preserve protocol future-proofing.** Future TUI/CLI and external API clients should see no mobile-specific contract drift.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| COLA/API boundary preservation | HIGH | Directly from `.planning/PROJECT.md` and existing module layout. |
| Vaadin theme/CSS integration | HIGH | Vaadin docs verify theme/global CSS, CSS imports, Lumo utility classes, responsive FormLayout/utilities. Exact project theme annotation/name may need implementation check. |
| Playwright mobile emulation | HIGH | Playwright docs verify `devices` registry and mobile projects for Pixel/iPhone/WebKit/Chromium. |
| Existing component inventory | MEDIUM-HIGH | Verified key classes/routes in repository; final implementation should inventory all UI classes before coding. |
| Data flow unchanged | HIGH | Existing `ConsoleHttpClient`/`EventStreamClient` and public DTO architecture already isolate UI from backend. |
| Build order | MEDIUM-HIGH | Derived from dependency analysis; should be validated after baseline mobile E2E spike. |

## Open Questions / Phase Research Flags

- **Theme bootstrap detail:** Confirm whether the current app already has an `AppShellConfigurator`/`@Theme` class or needs one added for `pi-agent` theme loading.
- **Vaadin component choice:** Decide during implementation whether to use `AppLayout`, `Tabs`, `Details`, `Grid`, or plain layouts for each surface. Architecture recommendation is about boundaries and behavior, not mandatory widgets.
- **Large-list strategy:** If audit/events/registry lists are large, research Vaadin virtualization/lazy data providers in a phase-specific task. Do not block initial mobile layout on this.
- **Real iOS Safari UAT:** Playwright WebKit mobile emulation is valuable but not a perfect substitute for real-device Safari validation.
- **Accessibility gate:** Consider adding keyboard/focus/ARIA assertions after basic mobile layout passes; touch usability and accessibility overlap but are not identical.

## Sources

- Project context: `/root/workspace/pi-java/.planning/PROJECT.md` — milestone goal, target features, constraints, validated phases, and Java/Vaadin/public REST/SSE boundary. HIGH confidence.
- Existing repository inventory: `pi-agent-adapter-web` Vaadin routes/components, `ConsoleHttpClient`, `EventStreamClient`, root `playwright.config.ts`, current E2E specs. HIGH confidence for current shape inspected on 2026-06-20.
- Vaadin documentation via Context7 (`/vaadin/docs`) — Lumo utility classes, responsive visibility utilities, responsive `FormLayout`, CSS theme folder/global styling, component CSS imports. HIGH confidence.
- Playwright documentation via Context7 (`/microsoft/playwright`) — device emulation, `devices` registry, multi-project config for Mobile Chrome/Mobile Safari/Desktop browsers. HIGH confidence.

---
*Architecture research for: mobile-first H5 adaptation of Pi Java Agent Web Console/Admin Governance*  
*Researched: 2026-06-20*
