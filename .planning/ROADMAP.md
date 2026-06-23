# Roadmap: Pi Java Agent Platform — v1.1 适配移动端web

**Created:** 2026-06-20  
**Granularity:** Standard  
**Mode:** YOLO  
**Milestone:** v1.1 适配移动端web  
**Previous milestone:** v1.0 completed Phases 1-9  
**v1.1 Requirements:** 29  
**Mapped:** 29 / 29 ✓

## Overview

Milestone v1.1 converts the existing Java/Vaadin Agent Web Console and Admin Governance surfaces from desktop-first browser UI into a mobile-first, full-site H5 experience. The roadmap keeps work inside the existing Web Console/Admin presentation layer and verification harness: no React/Next.js rewrite, no native app, no mobile-only backend fork, and no new Agent runtime/model/tool capability scope. Phases continue after v1.0 and start at Phase 10.

## Milestones

- ✅ **v1.0 Agent Platform Foundation** - Phases 1-9 (completed before this milestone)
- 🚧 **v1.1 适配移动端web** - Phases 10-15 (current)

## Phases

**Phase Numbering:**
- Integer phases (10, 11, 12): Planned milestone work continuing from v1.0
- Decimal phases (10.1, 10.2): Urgent insertions only if needed later

- [x] **Phase 10: Responsive Baseline and Mobile Test Harness** - Establish mobile-first theme defaults, stable UI hooks, route smoke, browser contexts, and no-overflow gates before broad refactoring. (completed 2026-06-21)
- [x] **Phase 11: Shared Responsive Shell and Navigation** - Make Console and Admin navigation touch-friendly and responsive through shared shell/primitives. (completed 2026-06-22)
- [ ] **Phase 12: Console Mobile-First Flow** - Convert Agent Catalog, Chat/Run, SSE feed, sessions, and cancellation into a usable phone-first Console flow. (gap closure planned 2026-06-23)
- [ ] **Phase 13: Runtime Cards, Timeline, Tool, and Approval UX** - Make run events, tool cards, dense details, approvals, dialogs, and confirmations safe and readable on mobile.
- [ ] **Phase 14: Admin Governance Full-Site Mobile Coverage** - Convert every existing Admin Governance surface to mobile card/detail layouts without relying on desktop tables.
- [ ] **Phase 15: Cross-Browser, Orientation, Accessibility, and Release Hardening** - Validate final mobile/tablet/browser/orientation/accessibility behavior and preserve desktop regression coverage.

## Phase Details

### Phase 10: Responsive Baseline and Mobile Test Harness
**Goal**: Mobile users can open the existing Console/Admin site at representative phone viewports while automated gates detect route failures, horizontal overflow, and missing primary actions before broad UI conversion begins.
**Depends on**: Phase 9
**Requirements**: MH5-01, MH5-03, MVER-01, MVER-02
**Success Criteria** (what must be TRUE):
  1. Mobile user can open every existing Console and Admin route at representative phone viewports without blank screens, route errors, or desktop-only blockers.
  2. Mobile user does not encounter page-level horizontal overflow on the baseline routed pages covered by the smoke harness.
  3. Maintainer can run automated mobile browser smoke tests for representative Chromium, WebKit/Safari proxy, Firefox or Firefox-sized mobile, and tablet contexts supported by CI.
  4. Maintainer can see route-level smoke results that verify route load, no page-level horizontal overflow, visible primary actions, and at least one key interaction per route category.
**Plans**: 3 plans
Plans:
- [ ] 10-responsive-baseline-and-mobile-test-harness-01-PLAN.md — Vaadin responsive theme baseline and stable root selectors
- [ ] 10-responsive-baseline-and-mobile-test-harness-02-PLAN.md — Representative Playwright mobile/tablet project matrix and helper foundation
- [ ] 10-responsive-baseline-and-mobile-test-harness-03-PLAN.md — Route-level mobile smoke gates and targeted overflow fixes
**UI hint**: yes

### Phase 11: Shared Responsive Shell and Navigation
**Goal**: Mobile users can move through all Console and Admin sections with a touch-friendly responsive shell, readable layout container, keyboard/focus-safe navigation, and shared mobile UI primitives.
**Depends on**: Phase 10
**Requirements**: MH5-02, MH5-04, MH5-05
**Success Criteria** (what must be TRUE):
  1. Mobile user can navigate all Console and Admin sections through a compact header, drawer/tabs, or equivalent touch-friendly navigation without desktop-width assumptions.
  2. Touch user can reliably activate primary links, buttons, toggles, refresh controls, details expanders, approvals, and cancel controls with mobile-safe target sizes and spacing.
  3. Keyboard or tablet user retains visible focus indicators and a usable focus order across drawers, dialogs, navigation, and page content.
  4. User sees a consistent route title, content container, status/action placement, and card/detail styling across Console and Admin pages.
**Plans**: 3 plans
Plans:
- [x] 11-shared-responsive-shell-and-navigation-01-PLAN.md — Shared route navigation registry and responsive shell wiring
- [x] 11-shared-responsive-shell-and-navigation-02-PLAN.md — Mobile tap/focus theme contract and shared page primitives
- [x] 11-shared-responsive-shell-and-navigation-03-PLAN.md — Playwright all-route shell navigation, touch, and focus gate
**UI hint**: yes

### Phase 12: Console Mobile-First Flow
**Goal**: Mobile users can complete the existing Agent Console workflow end-to-end: browse/select an agent, start or continue chat/run sessions, watch live output, and cancel active runs from visible mobile controls.
**Depends on**: Phase 11
**Requirements**: MCON-01, MCON-02, MCON-03, MCON-04, MCON-05, MVER-03
**Success Criteria** (what must be TRUE):
  1. Mobile user can browse Agent Catalog as stacked cards and select/start the General Agent without a desktop-width layout.
  2. Mobile user can type a multi-line prompt, submit it, and understand active run/composer state in a mobile-first Chat/Run flow.
  3. Mobile user can observe live SSE run output/events in a vertical feed and scroll previous events without losing access to current run controls.
  4. Mobile user can open session history, select a past session, continue it, and clearly identify the active session.
  5. Mobile user can cancel an active run from a visible touch-safe control and see cancelling or terminal feedback in the UI.
**Plans**: 5 plans
Plans:
- [x] 12-console-mobile-first-flow-01-PLAN.md — Chat-first Console panel state, segmented switcher, stacked Agent/session cards
- [x] 12-console-mobile-first-flow-02-PLAN.md — Sticky bounded composer, vertical event feed, inline run state, dual Cancel controls
- [x] 12-console-mobile-first-flow-03-PLAN.md — Console mobile product-path E2E, desktop regression, and documentation
- [ ] 12-console-mobile-first-flow-04-PLAN.md — Gap closure for real Agent, Session, Send, and Cancel UI activation wiring
- [ ] 12-console-mobile-first-flow-05-PLAN.md — Gap closure for user-triggered run execution, event feed progression, and cancellation feedback
**UI hint**: yes

### Phase 13: Runtime Cards, Timeline, Tool, and Approval UX
**Goal**: Mobile users can safely inspect run timelines, tool execution, policy/audit-like details, approvals, and viewport-fitting dialogs without raw sensitive payload exposure or horizontal overflow.
**Depends on**: Phase 12
**Requirements**: MCARD-01, MCARD-02, MCARD-03, MCARD-04, MCARD-05
**Success Criteria** (what must be TRUE):
  1. Mobile user can inspect run timeline events as compact cards or accordions showing status, timestamp/type, summary, and expandable details.
  2. Mobile user can inspect tool cards with tool name, source, status, policy/approval state, duration, error, and redacted input/output summaries.
  3. Mobile user can expand dense run/tool/policy/audit details without exposing raw sensitive payloads or causing page-level horizontal overflow.
  4. Mobile user can approve or reject a pending tool approval from a risk-first card that clearly shows side-effect context and requires intentional action.
  5. Mobile user sees dialogs, drawers, notifications, and confirmations fit the viewport with safe scrolling and explicit close/action controls.
**Plans**: TBD
**UI hint**: yes

### Phase 14: Admin Governance Full-Site Mobile Coverage
**Goal**: Mobile admins can inspect every existing Governance, Operations, Registry, MCP, Plugin, Extension, Policy, and Audit surface through stacked mobile cards/details instead of desktop-only tables.
**Depends on**: Phase 13
**Requirements**: MADM-01, MADM-02, MADM-03, MADM-04, MADM-05, MADM-06, MADM-07, MVER-04
**Success Criteria** (what must be TRUE):
  1. Mobile admin can read Governance Overview as stacked status cards with runtime/provider/tool/extension/MCP/plugin health, counts, messages, and links.
  2. Mobile admin can inspect Registry and Operations data as cards or responsive row details without relying on page-level horizontal table scrolling.
  3. Mobile admin can inspect MCP, Plugin, and Extension status/metadata, including unhealthy, disconnected, selected, disabled, quarantined, and load-error states where already supported.
  4. Mobile admin can inspect Policy decisions and Audit summaries with key IDs, actors/sources/actions/statuses/timestamps, and expandable redacted context.
  5. Automated mobile Admin E2E opens overview, registry, operations, MCP, plugin, extension, policy, and audit pages and verifies mobile card/detail content.
**Plans**: TBD
**UI hint**: yes

### Phase 15: Cross-Browser, Orientation, Accessibility, and Release Hardening
**Goal**: The full mobile H5 milestone is release-ready across representative portrait, landscape, tablet, and desktop contexts, with documented real-device/UAT expectations and preserved desktop behavior.
**Depends on**: Phase 14
**Requirements**: MVER-05, MVER-06, MVER-07
**Success Criteria** (what must be TRUE):
  1. Mobile user can use navigation and critical flows in representative portrait, landscape, and tablet viewports with no page-level horizontal overflow.
  2. Desktop user can still complete existing Web Console and Admin browser regression paths after mobile-first changes.
  3. Keyboard/tablet user can complete final focus, drawer/dialog, visible label, and no-hover-only interaction checks across converted surfaces.
  4. Maintainer can review release documentation that records Android Chrome, iOS Safari, Edge mobile, and Firefox mobile UAT expectations plus any CI/emulation gaps.
**Plans**: TBD
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 10 → 11 → 12 → 13 → 14 → 15

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 10. Responsive Baseline and Mobile Test Harness | v1.1 | 3/3 | Complete   | 2026-06-21 |
| 11. Shared Responsive Shell and Navigation | v1.1 | 3/3 | Complete   | 2026-06-22 |
| 12. Console Mobile-First Flow | v1.1 | 3/5 | Gaps planned | - |
| 13. Runtime Cards, Timeline, Tool, and Approval UX | v1.1 | 0/TBD | Not started | - |
| 14. Admin Governance Full-Site Mobile Coverage | v1.1 | 0/TBD | Not started | - |
| 15. Cross-Browser, Orientation, Accessibility, and Release Hardening | v1.1 | 0/TBD | Not started | - |

## Coverage Validation

| Requirement Prefix | Count | Phase |
|--------------------|-------|-------|
| MH5 | 5 | Phase 10, 11 |
| MCON | 5 | Phase 12 |
| MCARD | 5 | Phase 13 |
| MADM | 7 | Phase 14 |
| MVER | 7 | Phase 10, 12, 14, 15 |

**Coverage map:**
- MH5-01 → Phase 10
- MH5-02 → Phase 11
- MH5-03 → Phase 10
- MH5-04 → Phase 11
- MH5-05 → Phase 11
- MCON-01 → Phase 12
- MCON-02 → Phase 12
- MCON-03 → Phase 12
- MCON-04 → Phase 12
- MCON-05 → Phase 12
- MCARD-01 → Phase 13
- MCARD-02 → Phase 13
- MCARD-03 → Phase 13
- MCARD-04 → Phase 13
- MCARD-05 → Phase 13
- MADM-01 → Phase 14
- MADM-02 → Phase 14
- MADM-03 → Phase 14
- MADM-04 → Phase 14
- MADM-05 → Phase 14
- MADM-06 → Phase 14
- MADM-07 → Phase 14
- MVER-01 → Phase 10
- MVER-02 → Phase 10
- MVER-03 → Phase 12
- MVER-04 → Phase 14
- MVER-05 → Phase 15
- MVER-06 → Phase 15
- MVER-07 → Phase 15

**Total mapped:** 29 / 29 ✓  
**Duplicates:** 0 ✓  
**Orphans:** 0 ✓

## Constraints Preserved

- Java/Vaadin-first Web Console/Admin adaptation only.
- Work remains focused on `pi-agent-adapter-web` UI, theme, and browser verification unless a general non-mobile read-model fix is proven necessary.
- Public REST/SSE DTO boundaries remain stable; no viewport flags in Domain/App and no `/mobile/*` API fork by default.
- No React/Next.js/Hilla React rewrite, no native app, no PWA/offline cache, and no new runtime/model/tool capability scope.

---
*Roadmap created: 2026-06-20 for milestone v1.1 适配移动端web*
