# Phase 10: Responsive Baseline and Mobile Test Harness - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-20
**Phase:** 10-Responsive Baseline and Mobile Test Harness
**Areas discussed:** Test browser matrix, Baseline route smoke, Responsive baseline, Selector contract, CI/UAT boundary

---

## Gray Areas Selected

The user selected all proposed Phase 10 gray areas for discussion:

| Area | Why it mattered |
|------|-----------------|
| Test browser matrix | Defines representative mobile/tablet Playwright projects and CI expectations for MVER-01. |
| Baseline route smoke | Defines route/category-level smoke coverage for MVER-02 without expanding into later product-flow E2E. |
| Responsive baseline | Defines how much CSS/theme/layout work belongs in Phase 10 vs later mobile conversion phases. |
| Selector contract | Defines stable test hooks for Phase 10 and downstream mobile E2E. |

---

## Test Browser Matrix

| Option | Description | Selected |
|--------|-------------|----------|
| Representative enforced matrix | Keep Desktop Chrome; add Mobile Chrome/Pixel, Mobile Safari/WebKit/iPhone, Mobile Firefox or Firefox mobile viewport, and Tablet/iPad. CI-supported projects become forced gates. | ✓ |
| Minimal matrix | Add only Mobile Chrome and Mobile Safari/WebKit; defer Firefox/tablet to Phase 15. | |
| Full matrix | Add Chrome/WebKit/Firefox across phone portrait, phone landscape, and tablet in Phase 10. | |

**User's choice:** Representative enforced matrix.
**Notes:** This balances MVER-01 coverage with Phase 10 scope. Full orientation/device hardening stays in Phase 15.

---

## Baseline Route Smoke

| Option | Description | Selected |
|--------|-------------|----------|
| Route-level baseline | Every current Console/Admin route verifies load, data-route marker, no page-level horizontal overflow, visible primary action/content, and one light key interaction per route category. | ✓ |
| Load/overflow only | Only verify route load and no horizontal overflow. | |
| Key-flow smoke | Run deeper Console/Admin product flows already in Phase 10. | |

**User's choice:** Route-level baseline.
**Notes:** Deep Console run-flow/Admin conversion E2E remains in later phases. Phase 10 establishes route health gates.

---

## Responsive Baseline

| Option | Description | Selected |
|--------|-------------|----------|
| Global baseline + high-risk fixes | Create project theme/global mobile defaults and only patch obvious high-risk overflow surfaces like Console three-column workbench and dense Admin views. | ✓ |
| Global CSS only | Add only reset/no-overflow defaults and do not touch route-specific layout. | |
| Early mobile redesign | Convert main Console/Admin pages to mobile-first cards/drawers in Phase 10. | |

**User's choice:** Global baseline + high-risk fixes.
**Notes:** User wants the baseline gates to pass without pulling Phase 11-14 redesign scope forward.

---

## Selector Contract

| Option | Description | Selected |
|--------|-------------|----------|
| Standardize data-* contract | Normalize stable `data-route`, `data-layout`, `data-panel`/`data-surface`, `data-action`, `data-primary-action`, and `data-mobile-critical` hooks. | ✓ |
| Reuse existing hooks only | Use current hooks and text selectors without formalizing a contract. | |
| Accessibility selectors only | Use role/name/label selectors and avoid data hooks. | |

**User's choice:** Standardize data-* contract.
**Notes:** Accessibility selectors remain useful, but dense Vaadin governance views need stable data hooks for reliable mobile smoke tests.

---

## CI/UAT Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Record CI gaps only | Phase 10 documents Playwright emulation/CI coverage and limitations; real-device Android/iOS/Edge/Firefox UAT stays Phase 15. | ✓ |
| Add light UAT | Phase 10 adds manual checks on 1-2 real devices. | |
| Do not record | Phase 10 only creates automation and does not document CI/emulation gaps. | |

**User's choice:** Record CI gaps only.
**Notes:** This matches roadmap traceability: MVER-07 belongs to Phase 15, not Phase 10.

---

## the agent's Discretion

- Exact Playwright device descriptors/project names/viewport dimensions.
- Exact CSS/theme structure and Vaadin wiring details.
- Exact light interaction chosen for each route category.

## Deferred Ideas

- Real-device/UAT release checklist — Phase 15.
- Full mobile navigation shell — Phase 11.
- Full Console/Admin mobile conversion flows — Phases 12-14.
