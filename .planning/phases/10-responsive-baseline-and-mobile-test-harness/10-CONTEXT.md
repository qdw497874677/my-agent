# Phase 10: Responsive Baseline and Mobile Test Harness - Context

**Gathered:** 2026-06-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 10 establishes the mobile H5 baseline before broad UI conversion begins. It must make the existing Vaadin Console/Admin routes open at representative mobile/tablet viewports, add stable mobile browser smoke infrastructure, create project-owned responsive theme defaults, and add route-level no-horizontal-overflow/primary-content gates. It may apply targeted fixes to high-risk overflow surfaces only when needed for the baseline gates.

This phase does **not** redesign the shared navigation shell, fully convert Console workflows, rebuild runtime/tool/approval cards, or convert all Admin Governance tables/details into final mobile card layouts. Those belong to Phases 11-15.

</domain>

<decisions>
## Implementation Decisions

### Mobile Browser Test Matrix
- **D-01:** Phase 10 should use a **representative enforced Playwright matrix**: keep existing Desktop Chrome regression coverage and add Mobile Chrome/Pixel, Mobile Safari via WebKit/iPhone, Mobile Firefox or a Firefox mobile-sized/touch viewport where CI support permits, and a tablet/iPad-style context.
- **D-02:** CI-supported projects in that representative matrix are mandatory smoke gates. If a true mobile Firefox/WebKit/device behavior cannot be supported in CI, document the exact emulation/proxy gap rather than blocking on real-device infrastructure.
- **D-03:** Do not explode Phase 10 into every orientation/device/browser combination. Broad portrait/landscape/tablet/browser hardening remains Phase 15; Phase 10 creates the baseline harness and representative coverage.

### Route Smoke Coverage
- **D-04:** Phase 10 mobile smoke tests should be **route-level baseline tests**, not full product-flow E2E. For every existing Console/Admin route, verify route load, stable route marker visibility, no page-level horizontal overflow, visible primary content or primary action, and at least one light key interaction per route category.
- **D-05:** The route set should include all currently existing Vaadin UI routes: `console`, `admin/governance`, `admin/governance/overview`, `admin/governance/registry`, `admin/governance/operations`, `admin/governance/policy-decisions`, `admin/governance/audits`, and `admin/governance/approvals`.
- **D-06:** Keep deep Console run-flow E2E, Admin governance conversion E2E, cross-browser/orientation hardening, and full desktop/mobile regression expansion in later phases unless a light interaction is required to prove the route is not inert.

### Responsive Theme Baseline
- **D-07:** Phase 10 should create a project-owned Vaadin responsive theme/bootstrap baseline. It should cover viewport/safe-area assumptions, `box-sizing`, full-width/flexible containers, text/code wrapping, mobile-safe overflow defaults, and page-level no-horizontal-scroll behavior.
- **D-08:** Apply **targeted high-risk overflow fixes** only where needed to pass the Phase 10 gates, especially the existing Console three-column workbench and dense Admin Registry/MCP/Plugin-style views. These fixes should make current routes usable/openable at phone widths without doing the final mobile information architecture redesign.
- **D-09:** Avoid premature Phase 11-14 work: no full drawer/nav shell redesign, no complete Console chat/mobile flow conversion, no final runtime-card/approval UX redesign, and no full Admin table-to-card migration in Phase 10 unless strictly necessary for route-level baseline viability.

### Stable Test Selector Contract
- **D-10:** Phase 10 should standardize a stable `data-*` UI test contract for mobile smoke and future phases. Use predictable markers such as `data-route`, `data-layout`, `data-panel` or `data-surface`, `data-action`, `data-primary-action`, and `data-mobile-critical` where useful.
- **D-11:** Mobile smoke tests should prefer these stable hooks over brittle body-text selectors. Accessibility selectors remain valuable for user-facing controls, but the project should not rely only on role/name matching for dense Vaadin governance views.
- **D-12:** Existing `data-*` hooks should be reused and normalized rather than replaced wholesale. Old specs may remain behavior-first, but new Phase 10 smoke tests should establish the contract downstream phases can extend.

### CI, Emulation, and UAT Boundary
- **D-13:** Phase 10 should record Playwright/CI coverage and known emulation gaps, especially WebKit-as-Mobile-Safari and any Firefox mobile limitations. Real Android/iOS/Edge/Firefox device UAT is not required in Phase 10.
- **D-14:** Real-device/UAT expectations and final release notes belong to Phase 15. Phase 10 should leave a clear handoff note for Phase 15 rather than adding manual device checks as a blocking gate now.

### Folded Todos
- No pending todos matched Phase 10 scope.

### the agent's Discretion
- Exact Playwright device names, viewport dimensions, project names, and whether Firefox is represented by a named device descriptor or custom mobile viewport are planner/researcher discretion, as long as D-01 through D-03 hold.
- Exact CSS token names, theme folder name, class names, and Vaadin `@Theme`/AppShell wiring details are planner discretion, provided a project-owned responsive baseline is created and no outer-layer dependencies leak into Domain/App.
- Exact light interaction per route category is planner discretion, but interactions should be low-risk, deterministic, no-key, and should not create broad product-flow scope.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 10 Scope and Mobile Requirements
- `.planning/ROADMAP.md` §Phase 10 — Phase goal, dependencies, MH5-01/MH5-03/MVER-01/MVER-02 mapping, and success criteria for route load, no overflow, browser contexts, and smoke results.
- `.planning/REQUIREMENTS.md` §Mobile Foundation — MH5-01 and MH5-03 requirements for route accessibility and page-level no-horizontal-overflow.
- `.planning/REQUIREMENTS.md` §Mobile Verification and Release Gates — MVER-01 and MVER-02 requirements for mobile browser contexts and route smoke coverage.
- `.planning/PROJECT.md` §Current Milestone: v1.1 适配移动端web — milestone boundary: existing Vaadin Web Console/Admin Governance converted to mobile-first H5 without a new frontend stack or native app.
- `.planning/STATE.md` — Current Phase 10 state and concern to inventory existing Vaadin theme/bootstrap and Playwright harness before adding duplicate infrastructure.

### Prior Phase UI and Verification Decisions
- `.planning/phases/05-agent-web-console-and-runtime-cockpit/05-CONTEXT.md` — Vaadin Web Console/Admin separation, Chat-first Console, three-column workbench, tool/approval card direction, public API-only UI boundary, and Playwright/no-key browser E2E decisions.
- `.planning/phases/07-mcp-client-bridge-and-governed-remote-tools/07-CONTEXT.md` — Admin MCP status/refresh visibility, sanitized errors, and public governance DTO/UI boundary relevant to mobile smoke coverage of MCP governance surfaces.
- `.planning/phases/08-controlled-dynamic-plugin-jars/08-CONTEXT.md` — Admin plugin status/actions, disable/quarantine confirmation, audit/redaction, and public DTO/UI boundary relevant to dense registry/mobile overflow risks.
- `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-CONTEXT.md` — Admin operations summary/detail surfaces, telemetry/redaction boundaries, and regression/no-key E2E expectations.

### Existing Product Documentation
- `docs/phase-05-web-console.md` — Web Console/Admin public API boundary and UI/test patterns, if present during planning.
- `docs/phase-07-mcp-client-bridge.md` — MCP Admin governance/status behavior, if present during planning.
- `docs/phase-08-controlled-dynamic-plugin-jars.md` — Plugin Admin governance/status/action behavior, if present during planning.

### External Documentation to Research During Planning
- Vaadin Flow 24.8 theme/AppShell/responsive styling documentation — project-owned theme structure, global styles, viewport/meta/AppShell behavior, and component/container styling conventions.
- Playwright 1.57+ device/project/browser documentation — configuring Mobile Chrome, WebKit/iPhone, Firefox/mobile-sized contexts, tablet contexts, browser installation, and CI-supported projects.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` — Existing `@Route("console")` Console workbench with `data-route="console"` and `data-layout="three-column-workbench"`; high-risk mobile overflow surface.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java`, `RunContextPanel.java`, `AgentCard.java`, and `ApprovalCard.java` — Existing Console components with useful `data-column` and `data-action` hooks such as send/cancel/choose/approve/reject.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java`, `AdminRegistryStatusView.java`, `AdminOperationsView.java`, `AdminPolicyDecisionsView.java`, `AdminAuditView.java`, and `AdminApprovalQueueView.java` — Existing Admin route/views to include in Phase 10 smoke route coverage.
- `e2e/fixtures/fake-runtime.ts` — Existing Playwright helper functions and dev headers for no-key product-path tests.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java` — Existing `e2e` profile fake runtime/readiness fixture for deterministic browser tests.
- `scripts/e2e-web-server.sh` — Existing Playwright web server bootstrap that builds/runs the Vaadin Spring Boot app with production-mode Vaadin assets.

### Established Patterns
- UI lives in `pi-agent-adapter-web`; Domain/App/client contracts must not depend on Vaadin, Playwright, or frontend/theme implementation details.
- Browser E2E is currently root-level TypeScript Playwright, accepted as test-only tooling despite Java-first production constraints.
- Default browser verification is no-key/deterministic and should use fake runtime/test fixtures, not real model providers or external services.
- Existing Vaadin views already use several `data-*` hooks; Phase 10 should normalize and extend this pattern instead of inventing a parallel selector system.
- Public REST/SSE/read-model boundaries remain stable; Phase 10 should not add viewport-specific backend APIs or `/mobile/*` forks.

### Integration Points
- `playwright.config.ts` — Add mobile/tablet projects and keep Desktop Chrome regression project.
- `scripts/e2e-install.sh` — Extend browser installation beyond Chromium where Phase 10 matrix requires WebKit/Firefox.
- `package.json` — Existing `e2e` and `e2e:install` scripts are the entry points to preserve or extend.
- `pi-agent-adapter-web/src/main/frontend/index.html` — Existing Vaadin-generated viewport meta already includes `width=device-width, initial-scale=1, viewport-fit=cover`; verify whether AppShell/theme wiring should own or preserve this.
- `pi-agent-adapter-web/src/main/frontend/` — No project-owned `themes/` folder exists yet; Phase 10 likely creates one and wires it through Vaadin theme configuration.
- `e2e/phase-09-operations-governance.spec.ts` — Example of using `data-route`/`data-admin-surface` selectors in current tests.

</code_context>

<specifics>
## Specific Ideas

- User selected all proposed Phase 10 discussion areas: browser matrix, route smoke scope, responsive baseline strength, and selector contract.
- User chose a representative forced browser matrix instead of a minimal or exhaustive matrix.
- User chose route-level baseline smoke instead of either load-only checks or full product-flow smoke in Phase 10.
- User chose global responsive baseline plus high-risk overflow fixes, explicitly avoiding a broad early mobile redesign.
- User chose to standardize a `data-*` selector contract for future mobile tests.
- User chose to record CI/emulation gaps only in Phase 10 and leave real-device/UAT expectations to Phase 15.

</specifics>

<deferred>
## Deferred Ideas

- Full shared responsive shell/navigation redesign — Phase 11.
- Full Console mobile-first flow, including agent selection, chat/run flow, SSE feed behavior, session continuation, and cancellation UX — Phase 12.
- Final runtime event cards, tool cards, approval cards, dialogs, drawers, and dense details UX — Phase 13.
- Full Admin Governance mobile card/detail conversion across all surfaces — Phase 14.
- Broad cross-browser/orientation/accessibility hardening and real-device/UAT release documentation — Phase 15.

</deferred>

---

*Phase: 10-responsive-baseline-and-mobile-test-harness*
*Context gathered: 2026-06-20*
