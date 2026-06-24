# Phase 14: Admin Governance Full-Site Mobile Coverage - Context

**Gathered:** 2026-06-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 14 converts every existing Admin Governance surface into mobile-readable stacked card/detail layouts. Mobile admins must be able to inspect Governance Overview, Registry, Operations, MCP, Plugin, Extension, Policy decisions, Audit summaries, and the existing Admin approval queue without relying on desktop-only tables, pipe-separated dense text, or page-level horizontal scrolling.

This phase stays inside `pi-agent-adapter-web`, the existing Vaadin Flow UI, the `pi-mobile` theme, Java component/contract tests, and deterministic Playwright browser verification. It does **not** add new Agent runtime/model/tool/plugin/MCP capabilities, does **not** change public REST/SSE/client DTO boundaries, does **not** add viewport-specific backend APIs or `/mobile/*` endpoints, does **not** introduce React/Next.js/Hilla React/native/PWA/offline behavior, and does **not** make horizontal-scroll tables the default Admin mobile solution.

</domain>

<decisions>
## Implementation Decisions

### Admin Information Architecture
- **D-01:** Preserve the existing Admin route structure. Keep `/admin/governance`, `/admin/governance/overview`, `/admin/governance/registry`, `/admin/governance/operations`, `/admin/governance/policy-decisions`, `/admin/governance/audits`, and `/admin/governance/approvals` as the mobile full-site coverage targets; convert each page internally rather than adding mobile-only routes.
- **D-02:** Governance Overview should render as a stacked/mobile-responsive health status card set: runtime, providers, tools, extensions, MCP, and plugins each need readable health/count/message/link-style summaries where data exists.
- **D-03:** Registry should use explicit sectioned card groups for Tools/Registry data, MCP, Plugin, and Extension status rather than one undifferentiated stream. This matches the current `AdminRegistryStatusView` responsibility split and keeps governance concepts clear on phone screens.
- **D-04:** Operations should use metric/area cards for Runs, Models, Tools, Policies, MCP, Plugins, Errors, and Warnings. Preserve existing operations meaning while making mobile scanning and abnormal-state discovery easier.

### Card Density and Detail Model
- **D-05:** Admin cards should default to key-field summaries, not verbose raw rows: name/title, status/health, count, last-refresh or timestamp where relevant, and a short message/reason. IDs, long metadata, JSON-like payloads, capabilities, and troubleshooting detail belong behind `Details` expanders.
- **D-06:** Use the same layered detail model established in Phase 13: compact summary by default, structured detail expansion for label/value rows, and advanced redacted detail only where it materially helps governance troubleshooting.
- **D-07:** Replace pipe-separated `Span` text with consistent label-value rows, semantic chips, and action rows inside cards. Keep stable `data-*` hooks so Java tests and Playwright can assert structure without brittle text-only matching.
- **D-08:** `AdminApprovalQueueView` should keep reusing the Phase 13 `ApprovalCard` for ADMIN approvals. Phase 14 should include this route in full-site mobile verification and only add wrapper/spacing/test coverage if needed; do not redesign the approval card again.

### Governance State Presentation
- **D-09:** Within each Admin section, abnormal or operator-relevant states should be prioritized visually and, where practical, sorted before normal entries. Examples: `unhealthy`, `disconnected`, `quarantined`, `load-error`, and `disabled` should be easier to find than healthy/default rows.
- **D-10:** Use semantic status/risk chips plus a short message for health/lifecycle/connection/decision states. Error details, load errors, disconnected diagnostics, and raw-ish metadata should be expandable and redacted rather than placed in the default summary.
- **D-11:** Plugin cards should surface all key governance states in the summary layer where data is available: lifecycle, health, selected, disabled, quarantined, and load-error. Plugin metadata and capabilities should be structured details.
- **D-12:** MCP server cards should default to connection state, health, tool count, and refresh/status metadata. MCP tool details should be visible either as nested/child cards or structured Details, but the mobile admin must be able to inspect tool status without a desktop table.
- **D-13:** Extension status should follow the same sectioned-card pattern: source/contribution/provider/tool/listener identity and status in the summary, with expandable metadata/capabilities.

### Policy and Audit Safety
- **D-14:** Policy decision cards should default to decision, reason, tool, run ID, session ID, and timestamp. Decision context must be available only through expandable redacted details.
- **D-15:** Audit summary cards should default to actor, source, action, status, resource type/id, and timestamp. Audit details must be available only through expandable redacted details.
- **D-16:** Reuse or generalize the Phase 13 conservative redaction utility/pattern for Admin dense details. API keys, passwords, bearer tokens, raw secrets, provider keys, and token-like values must not be exposed in card summaries or advanced details.
- **D-17:** Policy/Audit detail sections should be collapsed by default, including abnormal rows. The default view should remain scan-friendly and safe; users deliberately expand context/details when needed.

### Verification Scope
- **D-18:** Add an Admin mobile E2E gate covering the full Admin route set: landing, overview, registry, operations, policy decisions, audits, and approvals. Registry coverage must verify MCP, Plugin, and Extension card/detail sections so MVER-04's overview/registry/operations/MCP/plugin/extension/policy/audit coverage is explicit.
- **D-19:** Browser fixtures should cover a representative state matrix, not exhaustive permutations: healthy/default plus representative unhealthy, disconnected, selected, disabled, quarantined, and load-error states where current test seams support them.
- **D-20:** Playwright assertions should verify card/section visibility, Details expansion, redaction, no page-level horizontal overflow, and mobile-safe tap/focus behavior for key controls/expanders. Do not rely on screenshot visual regression as the primary Phase 14 gate.
- **D-21:** Java component/contract tests should cover broader branch detail than browser E2E: card fields, data hooks, selector contracts, redaction rules, status mappings, and representative DTO variants. Playwright should prove real mobile layout/product-path coverage using stable selectors.
- **D-22:** Tests must remain deterministic/no-key and should use existing fake/test fixtures and public Admin paths. Do not require real model providers, remote MCP servers, real plugin jars beyond existing controlled fixtures, external credentials, or real-device UAT in Phase 14.

### Folded Todos
- No pending todos matched Phase 14 scope.

### the agent's Discretion
- Exact Java helper/component extraction is planner discretion, provided it stays in `pi-agent-adapter-web` and does not leak Vaadin/mobile concerns into Domain/App/client DTOs.
- Exact chip colors, card spacing, typography, icon usage, section headings, and breakpoint polish are planner/designer discretion as long as summary/detail readability, abnormal-state discoverability, 44px target, focus-visible, and no-overflow contracts are met.
- Exact fixture data values and which representative abnormal states are exercised in Java vs Playwright are planner discretion, provided D-18 through D-22 are satisfied.
- Whether MCP tools render as nested child cards or structured Details is implementation discretion, provided tool status is inspectable on mobile without desktop tables.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 14 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 14 — Phase goal, dependency on Phase 13, MADM-01 through MADM-07 plus MVER-04 mapping, and success criteria for full Admin Governance card/detail coverage.
- `.planning/REQUIREMENTS.md` §Admin Governance Mobile Coverage — MADM-01 through MADM-07 requirements for overview, registry, operations, MCP, plugin, extension, policy, and audit mobile inspection.
- `.planning/REQUIREMENTS.md` §Mobile Verification and Release Gates — MVER-04 requirement for Admin mobile E2E coverage of overview, registry, operations, MCP, plugin, extension, policy, and audit pages.
- `.planning/PROJECT.md` §Current Milestone: v1.1 适配移动端web — milestone boundary: existing Vaadin Web Console/Admin Governance converted to mobile-first H5 while preserving Java/Vaadin/public REST/SSE DTO boundaries.
- `.planning/STATE.md` — Current v1.1 state and accumulated decisions for `pi-mobile`, shared shell/navigation, Console mobile flow, runtime cards, reusable redaction, and Admin full-site handoff.

### Prior Mobile Foundation Decisions
- `.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-CONTEXT.md` — `pi-mobile` theme baseline, representative browser matrix, route smoke/no-overflow helpers, stable `data-*` selector contract, and explicit deferral of final Admin table/card migration to Phase 14.
- `.planning/phases/11-shared-responsive-shell-and-navigation/11-CONTEXT.md` — `PiResponsiveShell`, compact header/drawer, Admin sub-navigation, route title/content/action primitives, 44px tap target, focus-visible contract, and base card/detail styling.
- `.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md` — adapter-web/mobile-only API boundary decisions and Phase 14 deferred Admin Governance full-site conversion.
- `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-CONTEXT.md` — layered card/detail model, reusable redaction direction, `ApprovalCard` reuse for ADMIN approvals, and stable browser/card verification patterns.

### Existing Product Documentation
- `docs/phase-05-web-console.md` — Existing Web Console/Admin public API boundary, Admin Governance UI/API behavior, and no-key Playwright patterns.
- `docs/phase-07-mcp-client-bridge.md` — MCP Admin governance/status/refresh behavior and redaction/public DTO boundary, if present during planning.
- `docs/phase-08-controlled-dynamic-plugin-jars.md` — Plugin Admin governance status/actions, selected/disabled/quarantined/load-error semantics, and controlled plugin UI/test behavior, if present during planning.
- `docs/phase-09-observability-policy-tenancy-and-production-hardening.md` — Operations, policy, audit, observability, tenancy, and redaction/governance context, if present during planning.
- `docs/phase-10-mobile-baseline.md` — Implemented mobile browser matrix, route smoke helpers, no-horizontal-overflow assertions, E2E commands, and Phase 15 UAT handoff.
- `docs/phase-11-responsive-shell.md` — Shared shell/navigation behavior, selector/touch/focus contract, page primitives, and Admin route navigation.
- `docs/phase-13-runtime-cards.md` — Runtime/tool/approval selector and redaction contracts for reusable card/detail patterns, if present during planning.

### External Documentation to Research During Planning
- Vaadin Flow 24.x `Details`, layout, component styling, theme CSS, accessibility/focus-visible, and mobile responsive documentation — verify best practice for expandable Admin cards and keyboard/touch behavior.
- Playwright documentation — mobile viewport assertions, locator geometry/tap target checks, Details expansion, focus-visible, no-horizontal-overflow, and multi-project mobile test stability.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java` — Shared RouterLayout for Console and Admin Governance; Phase 14 should keep all Admin routes inside this shell.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageHeader.java` — Shared page-header primitive for title/subtitle/status/actions; Admin views should converge on this where useful.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageSection.java` — Shared card/detail primitive; a natural base for Admin status cards and sectioned details.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiRouteNavRegistry.java` — Route/navigation truth source for Console plus seven Admin routes; Phase 14 should not add separate mobile-only nav truth.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLandingView.java` — Admin landing route; already lightweight/mobile-ready and mainly needs full-site E2E inclusion or optional section link polish.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java` — Current overview uses dense text summaries; primary MADM-01 conversion target for health status cards.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java` — Largest Phase 14 target. Currently owns Registry, MCP, Plugin, and Extension rendering with dense `Span`/string composition; should become sectioned cards/details.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java` — Current operations metrics summary; should become grouped metric/status cards.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java` — Current policy rows; should become decision cards with collapsed redacted context.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java` — Current audit rows; should become audit cards with collapsed redacted details.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java` — Already reuses `ApprovalCard` with ADMIN role; include in verification without redesigning card behavior.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java` — Reusable risk-first approval card for USER/ADMIN contexts.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java` — Phase 13 conservative redaction utility/pattern; planning should decide whether to adjust visibility/package or extract a shared adapter-web utility for Admin details.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` — Vaadin-side public REST/Admin DTO path boundary; Phase 14 should consume existing paths rather than add viewport-specific endpoints.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/*.java` — Public Admin DTOs consumed by Vaadin; treat as stable unless a non-mobile bug is proven.
- `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` — Existing mobile theme tokens/classes, including admin surface classes, card/detail/action-row/status chip patterns, tap target, focus-visible, safe-area, and no-overflow rules.
- `e2e/fixtures/mobile-smoke.ts` — Reusable no-overflow, tap-target, focus-visible, selector, and route baseline helpers.
- `e2e/phase-10-mobile-route-smoke.spec.ts`, `e2e/phase-11-shell-navigation.spec.ts`, and `e2e/phase-13-runtime-cards.spec.ts` — Existing route/shell/card browser gate patterns to extend.

### Established Patterns
- UI implementation belongs in `pi-agent-adapter-web`; Domain/App/client DTO contracts must stay free of Vaadin, Playwright, responsive theme, and viewport-specific behavior.
- Production frontend remains Vaadin Java/Flow plus the project-owned `pi-mobile` theme; TypeScript is accepted only for Playwright browser testing.
- Stable `data-*` selectors are the preferred test contract for dense Vaadin Admin surfaces, supplemented by accessibility selectors for interactive controls.
- Mobile content should use additive wrappers, classes, and data hooks rather than public API forks or mobile-specific backend responses.
- Browser E2E remains deterministic/no-key and should use fake/runtime/test fixtures, not real provider credentials, real external MCP servers, or real-device UAT.
- Dense details should be redacted, bounded, wrapped, and collapsed by default; avoid raw secrets and horizontal overflow for long strings, JSON, IDs, URLs, stack traces, and error messages.

### Integration Points
- Convert `AdminGovernanceOverviewView`, `AdminRegistryStatusView`, `AdminOperationsView`, `AdminPolicyDecisionsView`, and `AdminAuditView` from dense `Span`/pipe-separated rows to card/detail components using `PiPageHeader`, `PiPageSection`, shared CSS classes, semantic chips, and stable data attributes.
- Keep `AdminApprovalQueueView` on `ApprovalCard`; update CSS/tests only if required for full-site mobile consistency.
- Extend `styles.css` for Admin section/card/detail classes, abnormal-state chips, label-value rows, nested MCP/plugin/extension details, wrapped redacted blocks, and mobile/touch/focus rules.
- Update Java tests such as `AdminGovernanceViewsTest`, `AdminPluginGovernanceViewTest`, `McpAdminGovernanceViewTest`, `AdminOperationsViewTest`, `WebMobileBaselineContractTest`, and `WebResponsiveShellContractTest` to assert card structure and selector contracts.
- Add `e2e/phase-14-admin-governance-mobile.spec.ts` or equivalent, reusing mobile-smoke helpers and the existing Playwright project matrix to validate full Admin mobile coverage.

</code_context>

<specifics>
## Specific Ideas

- User selected all proposed Phase 14 discussion areas: Admin information architecture, card density, abnormal/governance state expression, Policy/Audit safety, and MVER-04 verification scope.
- User chose to preserve existing Admin routes and convert each page internally rather than introducing mobile-only routes or a new navigation model.
- User chose Overview health status cards, sectioned Registry card groups, and Operations metric/area cards.
- User chose key-field summaries, label-value rows, semantic chips, default-collapsed Details, and Phase 13-style layered redacted details.
- User chose abnormal-state prioritization, semantic chip + message status expression, full key Plugin state summaries, and MCP server cards with connection/health/tool-count/refresh metadata.
- User chose Policy decision summaries with tool/run/session/timestamp and redacted collapsed context, and Audit summaries with actor/source/action/status/resource/timestamp plus redacted collapsed details.
- User chose full Admin route Playwright coverage with a representative state matrix and Java/component tests for broader branch coverage.

</specifics>

<deferred>
## Deferred Ideas

- New mobile-only Admin routes, deep-linkable expanded Admin details, incident-triage shortcuts, mobile evidence copy/share, advanced Admin search/filter/export UX, and offline/PWA governance cache — future/out of scope unless a later roadmap adds them.
- Exhaustive browser E2E permutations for every MCP/plugin/extension/policy/audit DTO state — use Java/component tests for branch breadth and keep Playwright representative.
- Screenshot-based visual regression baseline — not selected as the Phase 14 primary gate.
- Broad cross-browser/orientation/accessibility hardening, final desktop/mobile regression expansion, and real-device/UAT release documentation — Phase 15.
- Native app, React/Next.js/Hilla React rewrite, PWA/offline behavior, push/background monitoring, and new Agent runtime/model/tool capabilities — out of scope for v1.1.

</deferred>

---

*Phase: 14-admin-governance-full-site-mobile-coverage*
*Context gathered: 2026-06-24*
