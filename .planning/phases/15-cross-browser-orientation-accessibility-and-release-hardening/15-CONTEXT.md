# Phase 15: Cross-Browser, Orientation, Accessibility, and Release Hardening - Context

**Gathered:** 2026-06-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 15 is the final v1.1 mobile H5 release hardening phase. It validates that the existing Vaadin Agent Web Console and Admin Governance surfaces are release-ready across representative portrait, landscape, tablet, and desktop contexts; preserves desktop regression behavior after the mobile-first changes; deepens keyboard/focus/accessibility checks across converted surfaces; and documents real-device/UAT expectations plus CI/emulation gaps for Android Chrome, iOS Safari, Edge mobile, and Firefox mobile.

This phase stays inside the existing Java/Vaadin Web UI, `pi-mobile` theme, Playwright verification harness, release documentation, and low-risk UI/theme/accessibility fixes. It does **not** add new Agent runtime/model/tool capabilities, new public REST/SSE DTOs, viewport-specific backend APIs, `/mobile/*` endpoints, React/Next.js/Hilla React, native mobile apps, PWA/offline behavior, or new mobile-only product features.

</domain>

<decisions>
## Implementation Decisions

### Viewport, Orientation, and Tablet Matrix
- **D-01:** Phase 15 should use a **critical-flows plus all-routes** orientation strategy. All eight existing routes (`console`, `admin/governance`, `admin/governance/overview`, `admin/governance/registry`, `admin/governance/operations`, `admin/governance/policy-decisions`, `admin/governance/audits`, and `admin/governance/approvals`) must get representative portrait, landscape, and tablet navigation/no-horizontal-overflow coverage.
- **D-02:** Console and Admin critical paths should receive deeper interaction coverage than simple route smoke. Console coverage should exercise the mobile run/chat/session/cancel/runtime-card path where stable; Admin coverage should exercise representative card/detail inspection across the converted governance surfaces.
- **D-03:** Do **not** add a large set of dedicated landscape Playwright projects by default. Reuse the existing Playwright browser projects and switch portrait/landscape/tablet dimensions inside the Phase 15 spec with `page.setViewportSize(...)` or equivalent helpers. This keeps reports and CI cost manageable while still proving MVER-05.
- **D-04:** Landscape coverage must verify shell/drawer navigation, route primary content/action visibility, no page-level horizontal overflow, and critical control usability. It does not need to run the full Console fake-run path in every landscape browser unless planning finds a cheap and stable way to do so.
- **D-05:** Tablet validation should be treated as the bridge between phone and desktop. It must catch both failure modes: tablet layouts that remain overly cramped like phone single-column layouts, and tablet layouts that prematurely assume full desktop width and overflow. Focus areas are navigation, Console responsive columns, and Admin card/detail density.

### Desktop and Cross-Browser Regression Gates
- **D-06:** Preserve the existing desktop Console regression baseline, especially `e2e/phase-05-web-console.spec.ts`, and add a Phase 15 release-hardening summary gate that confirms key Console and Admin desktop routes still load, expose primary content/actions, and avoid regressions after mobile-first changes.
- **D-07:** Use a layered cross-browser gate. The core matrix — Desktop Chrome, Mobile Chrome, Mobile Safari via WebKit proxy, Mobile Firefox proxy, and Tablet — should all run Phase 15 smoke/orientation coverage. Put the deepest, most timing-sensitive interactions on the most stable projects, while ensuring every representative browser family still receives meaningful release smoke.
- **D-08:** Browser-specific failures must be handled explicitly: fix low-risk CSS/UI/test issues when feasible; when caused by CI/emulation/engine limitations or unavailable true devices, document the exact gap in the release/UAT docs. Do not silently skip flaky failures without recording the reason and release impact.
- **D-09:** Screenshot visual regression should **not** be the primary Phase 15 gate. Continue to use structural selectors, route/content assertions, no-overflow checks, tap-target/focus checks, keyboard/accessibility assertions, and deterministic interactions. Screenshots may be attached for debugging or UAT evidence, but not as the main pass/fail contract.

### Accessibility and Keyboard Hardening
- **D-10:** Phase 15 should deepen accessibility beyond the existing 44px tap-target and `focus-visible` helpers. Add keyboard traversal samples, semantic/label/heading/landmark checks where practical, drawer/dialog/details focus usability checks, and assertions that important interactions are not hover-only.
- **D-11:** Keyboard traversal should use representative samples per surface category, not exhaustive full-page Tab chains on every route. Cover: shared shell drawer/navigation; Console composer, panel switcher, run/cancel controls; runtime Details and approval actions; Admin card Details and primary controls.
- **D-12:** Add a lightweight `prefers-reduced-motion` / no-hover-only contract. Confirm or add CSS rules so reduced-motion users are not forced through unnecessary animation, and ensure critical controls/actions remain available without hover.
- **D-13:** Accessibility fixes should stay low-risk and presentation-layer focused: data hooks, `aria-label`/`aria-current`/`aria-pressed`/labels, focus order/return, CSS focus/reduced-motion/hover fallback, and Vaadin component attributes. Do not broaden Phase 15 into business-flow rewrites, new component systems, or backend/API changes unless a severe blocker proves no smaller fix is possible.
- **D-14:** Adding `axe-core` or a similar automated accessibility audit is not locked as mandatory for Phase 15. Research/planning may evaluate it, but the locked requirement is keyboard + semantic + focus hardening using reliable, low-noise checks that fit Vaadin/Playwright and the existing harness.

### Release Documentation and Real-Device/UAT Expectations
- **D-15:** Create a concentrated Phase 15 release hardening document, not just scattered updates. It should include CI/browser coverage, viewport/orientation matrix, real-device UAT matrix, scripted manual steps, known CI/emulation gaps, and release-go/no-go criteria.
- **D-16:** The UAT matrix must explicitly list Android Chrome, iOS Safari, Edge mobile, and Firefox mobile. Even if the team cannot execute every true-device/browser combination, the document must distinguish true-device coverage from Playwright proxies such as WebKit-as-Mobile-Safari and Firefox-engine mobile viewport emulation.
- **D-17:** UAT steps should be scripted as critical-path checklists rather than vague high-level reminders or exhaustive per-control manuals. Each target browser/device should guide reviewers through Console run/chat/session/cancel, Admin inspection/card details, orientation switch, keyboard/focus checks where applicable, and no-horizontal-overflow inspection.
- **D-18:** Release readiness must classify uncompleted or failed true-device validation explicitly as `blocker`, `known limitation`, or `follow-up`. CI green is required for automated release confidence, but manual/device gaps must not be ambiguously presented as passed.

### Folded Todos
- No pending todos matched Phase 15 scope.

### the agent's Discretion
- Exact viewport dimensions for portrait, landscape, and tablet are planner/researcher discretion, provided they are representative and mapped to MVER-05.
- Exact test file names, helper extraction, and whether Phase 15 introduces a dedicated orientation helper module are implementation discretion, provided existing `mobile-smoke.ts` helpers and stable `data-*` contracts are reused where possible.
- Exact depth split between Mobile Chrome/WebKit/Firefox/Tablet for timing-sensitive Console/Admin interactions is planner discretion, provided the layered gate in D-07 is preserved and weaker coverage is explicitly justified.
- Exact release document file name and whether it is paired with a `15-HUMAN-UAT.md` file are planner discretion, provided the concentrated release hardening/UAT content in D-15 through D-18 exists and is easy for downstream agents and maintainers to find.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 15 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 15 — Phase goal, dependency on Phase 14, MVER-05/MVER-06/MVER-07 mapping, and success criteria for orientation, desktop regression, accessibility/focus, and release documentation.
- `.planning/REQUIREMENTS.md` §Mobile Verification and Release Gates — MVER-05, MVER-06, and MVER-07 requirements for portrait/landscape/tablet, desktop regression, and real-device/UAT release documentation.
- `.planning/PROJECT.md` §Current Milestone: v1.1 适配移动端web — milestone boundary: existing Vaadin Web Console/Admin Governance mobile-first H5, no new frontend stack/native app, and public REST/SSE DTO preservation.
- `.planning/STATE.md` — Current v1.1 state, accumulated decisions, and explicit concern that Phase 15 should document real iOS Safari/Android Chrome UAT gaps if CI cannot run true device browsers.

### Prior Mobile Decisions
- `.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-CONTEXT.md` — Representative browser matrix, CI/emulation gap policy, route smoke/no-overflow helpers, and deferral of broad orientation/UAT to Phase 15.
- `.planning/phases/11-shared-responsive-shell-and-navigation/11-CONTEXT.md` — Shared shell/navigation, drawer focus-return, touch/focus contract, and deferral of broader keyboard/accessibility hardening to Phase 15.
- `.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md` — Console chat-first flow, sticky composer, session/run/cancel behavior, fake-runtime MVER-03 gate, and desktop Console regression preservation.
- `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-CONTEXT.md` — Runtime/tool/approval card selector, Details, redaction, tap/focus, and representative event matrix decisions.
- `.planning/phases/14-admin-governance-full-site-mobile-coverage/14-CONTEXT.md` — Full Admin mobile card/detail coverage, redaction/detail contracts, stable selector usage, and Phase 15 handoffs.

### Implemented Product and Test Documentation
- `docs/phase-10-mobile-baseline.md` — Implemented Playwright browser matrix, known WebKit/Safari and Firefox mobile proxy gaps, route smoke helpers, no-overflow gate, and Phase 15 real-device/orientation/accessibility handoffs.
- `docs/phase-11-responsive-shell.md` — Implemented shared shell/navigation selector, touch/focus contract, drawer behavior, and accessibility hardening handoffs.
- `docs/phase-12-console-mobile-flow.md` — Console selector contract, mobile run/chat/session/cancel flow, and desktop regression hooks.
- `docs/phase-13-runtime-cards.md` — Runtime/tool/approval selector and redaction contracts, Details behavior, and Phase 15 hardening handoffs.
- `docs/phase-14-admin-governance-mobile.md` — Latest Admin selector/card/detail/redaction contract and explicit Phase 15 handoffs for real-device/UAT, cross-browser/orientation, accessibility, and desktop regression.

### Existing Test and Theme Integration Points
- `playwright.config.ts` — Existing browser projects: Desktop Chrome, Mobile Chrome, Mobile Safari/WebKit proxy, Mobile Firefox proxy, and Tablet.
- `e2e/fixtures/mobile-smoke.ts` — Reusable no-horizontal-overflow, stable selector, tap-target, and focus-visible helpers to extend for orientation/accessibility hardening.
- `e2e/fixtures/fake-runtime.ts` — Deterministic no-key fake-runtime helpers for Console run, cancellation, approval, and event assertions.
- `e2e/phase-05-web-console.spec.ts` — Existing desktop Console regression baseline for MVER-06.
- `e2e/phase-10-mobile-route-smoke.spec.ts`, `e2e/phase-11-shell-navigation.spec.ts`, `e2e/phase-12-console-mobile-flow.spec.ts`, `e2e/phase-13-runtime-cards.spec.ts`, and `e2e/phase-14-admin-governance-mobile.spec.ts` — Existing mobile route, shell, Console, runtime-card, and Admin mobile gates to preserve/reuse.
- `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` — Theme owner for viewport, safe-area, focus, tap-target, no-overflow, responsive layout, reduced-motion, and hover fallback fixes.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java` and `PiRouteNavRegistry.java` — Shared shell/navigation and route truth used by full-site orientation/navigation checks.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `playwright.config.ts` — Already defines the representative browser matrix needed for Phase 15; Phase 15 should extend test behavior before expanding projects.
- `e2e/fixtures/mobile-smoke.ts` — Provides `expectNoPageHorizontalOverflow`, `expectStableSelectorVisible`, `expectTapTargetAtLeast`, `expectFocusVisible`, and route selector types. This is the primary helper seam for orientation and accessibility hardening.
- `e2e/fixtures/fake-runtime.ts` — Provides no-key API helpers and Console shell assertions for deterministic product-path coverage.
- `e2e/phase-05-web-console.spec.ts` — Desktop Console regression baseline to preserve and reference for MVER-06.
- `e2e/phase-10-mobile-route-smoke.spec.ts` — Route table and all-route smoke structure that Phase 15 can reuse for all-route portrait/landscape/tablet no-overflow/navigation coverage.
- `e2e/phase-11-shell-navigation.spec.ts` — Existing drawer/nav/tap/focus assertions and focus-return pattern to extend for keyboard traversal.
- `e2e/phase-12-console-mobile-flow.spec.ts` — Console critical path for agent selection, prompt, streamed event UI, session card, and cancel/terminal behavior.
- `e2e/phase-13-runtime-cards.spec.ts` — Runtime/tool/approval card matrix and redaction/detail/tap/focus patterns.
- `e2e/phase-14-admin-governance-mobile.spec.ts` — Full Admin route mobile card/detail route matrix and sensitive marker redaction checks.
- `.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-HUMAN-UAT.md` and `.planning/phases/12-console-mobile-first-flow/12-HUMAN-UAT.md` — Existing UAT template examples for Phase 15 release/UAT docs.

### Established Patterns
- UI changes belong in `pi-agent-adapter-web`; Domain/App/client/public DTO contracts must remain free of Vaadin, Playwright, responsive theme, and viewport concerns.
- Production UI remains Vaadin Flow plus the project-owned `pi-mobile` theme. TypeScript remains test-only Playwright tooling.
- Stable `data-*` hooks are the preferred selector contract for dense Vaadin UI, supplemented by accessibility selectors for user-facing controls.
- Browser E2E remains deterministic/no-key and relies on fake runtime/test fixtures, not real model providers, real remote MCP servers, external credentials, or required true-device infrastructure.
- Mobile card/detail surfaces should be structurally asserted through selectors, Details expansion, redaction, no-overflow, tap-target, and focus/keyboard checks rather than screenshot-only visual assertions.
- Prior phases intentionally deferred broad real-device/UAT, orientation, deeper keyboard/accessibility, and final desktop/mobile regression expansion to Phase 15.

### Integration Points
- Add a Phase 15 Playwright spec or specs that reuse the existing route matrix, browser projects, and mobile-smoke helpers while adding portrait/landscape/tablet viewport switching.
- Extend `mobile-smoke.ts` or add a nearby helper for keyboard traversal samples, semantic checks, hover-independent interaction checks, and reduced-motion context assertions if planning determines helper extraction is useful.
- Extend `pi-mobile/styles.css` with any low-risk orientation, reduced-motion, hover fallback, focus, wrapping, and tablet bridge fixes discovered by the new gates.
- Add or update fast Java contract tests only for static theme/selector/accessibility contracts that are cheaper and more stable than browser verification.
- Add a Phase 15 release hardening/UAT document under `docs/` and/or `.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/`, using prior `*-HUMAN-UAT.md` templates where useful.

</code_context>

<specifics>
## Specific Ideas

- User selected all proposed Phase 15 discussion areas: viewport/orientation matrix, regression gates, accessibility depth, and release/UAT documentation.
- User chose all-route portrait/landscape/tablet no-overflow/navigation coverage plus deeper Console/Admin critical flow checks.
- User chose in-test viewport switching over adding dedicated landscape Playwright projects by default.
- User chose landscape checks focused on navigation, no-overflow, and critical controls rather than full fake-run flow in every landscape browser.
- User chose tablet validation as a bridge between mobile and desktop, specifically checking navigation, Console responsive columns, and Admin card density.
- User chose existing desktop Console regression plus a new Phase 15 desktop summary gate, not screenshot-based visual regression or all-spec desktop expansion.
- User chose a layered cross-browser gate: all core projects run meaningful smoke/orientation coverage, with deeper interactions concentrated where stable.
- User chose to fix or explicitly document browser-specific failures/gaps, not silently skip them.
- User chose keyboard + semantic + focus accessibility hardening, representative surface sampling, lightweight reduced-motion/no-hover-only contracts, and low-risk presentation-layer fixes.
- User chose a concentrated release hardening document with a four-browser real-device/UAT matrix, scripted critical-path steps, CI/emulation gap explanations, and explicit blocker/known-limitation/follow-up classification.

</specifics>

<deferred>
## Deferred Ideas

- Native mobile app, React/Next.js/Hilla React rewrite, PWA/offline behavior, push/background monitoring, and new mobile-only Agent capabilities remain out of scope for v1.1.
- Deep-linkable expanded mobile details, incident-triage shortcuts, event filtering, and mobile evidence copy/share remain future product enhancements, not Phase 15 release hardening.
- Exhaustive device/browser/orientation permutation testing and full screenshot visual regression baseline are not selected as Phase 15 primary gates.
- Mandatory `axe-core` gate is not locked for Phase 15; it may be researched, but reliable keyboard/semantic/focus checks are the required direction.

</deferred>

---

*Phase: 15-cross-browser-orientation-accessibility-and-release-hardening*
*Context gathered: 2026-06-25*
