# Phase 14 Research — Admin Governance Full-Site Mobile Coverage

**Phase:** 14-admin-governance-full-site-mobile-coverage  
**Date:** 2026-06-25  
**Status:** Complete

## Executive Summary

Phase 14 is an adapter-web/Vaadin mobile conversion phase. The implementation should stay inside `pi-agent-adapter-web`, preserve public Admin REST/client DTOs, and convert existing Admin Governance views from dense `Span`/pipe-separated row text into stacked card/detail structures with stable `data-*` contracts.

Key technical findings:

- Vaadin `Details` is the right primitive for collapsed dense Admin context: its summary stays visible, and collapsed content is inaccessible to keyboard/screen readers until expanded. Important scan fields therefore must stay in the card summary; raw-ish metadata/context goes behind collapsed `Details`.
- Existing project primitives (`PiPageHeader`, `PiPageSection`, `.pi-card`, `.pi-detail`, `.pi-status-chip`, `.pi-risk-chip`, `.pi-action-row`) already provide the mobile shell and card foundation. Phase 14 should add Admin-specific helper methods/classes rather than new routes or a frontend stack.
- Playwright already has reusable mobile helpers for no horizontal overflow, stable selector visibility, 44px tap targets, and focus-visible checks. Phase 14 should add one Admin full-route spec using those helpers and existing project matrix names.
- The largest risk is `AdminRegistryStatusView`: it owns Registry, MCP, Plugin, and Extension rendering. It should be converted into sectioned card groups with nested details before browser verification asserts the full MVER-04 surface.

## Sources Consulted

### Project Context

- `.planning/phases/14-admin-governance-full-site-mobile-coverage/14-CONTEXT.md`
- `.planning/ROADMAP.md` Phase 14 requirements and success criteria
- `.planning/REQUIREMENTS.md` MADM-01..MADM-07 and MVER-04
- `.planning/STATE.md` accumulated v1.1 decisions
- `docs/phase-11-responsive-shell.md`
- `docs/phase-13-runtime-cards.md`
- `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css`
- Existing Admin views/tests under `pi-agent-adapter-web/src/main/java/.../ui/admin` and `pi-agent-adapter-web/src/test/java/...`

### External Docs

- Vaadin Flow Details docs: Details is for expandable content; summary is always visible; collapsed content is hidden/inaccessible by keyboard/screen reader; use Details to reduce clutter but do not hide important information by default.
- Playwright docs: device emulation provides viewport/isMobile/hasTouch; locator `boundingBox()` supports geometry checks; `tap()` requires touch-enabled browser context.

## Recommended Implementation Architecture

### Shared Admin Mobile Helper

Create a package-local helper in `io.github.pi_java.agent.adapter.web.ui.admin`, for example `AdminMobileCardSupport`, that centralizes repeated card/detail rendering:

- `card(String section, String role, Component... children)` returning `PiPageSection.card(...)` with classes `pi-admin-card` and attributes such as `data-admin-card`, `data-admin-section`.
- `labelValue(String label, String value)` emitting a row with class `pi-admin-field` and attributes `data-admin-field`, `data-label`, `data-value`.
- `statusChip(String status)` emitting `.pi-status-chip` plus `data-status-chip`.
- `details(String summary, Component... rows)` emitting `Details` collapsed by default with attributes `data-expandable="true"`, `data-admin-details`, and content layer attributes.
- `redactedBlock(...)` or use a dedicated redaction helper for Admin dense details.

This avoids five Admin views hand-rolling inconsistent Div/Span structures. Keep it package-private inside adapter-web/admin; do not introduce Domain/App/client changes.

### Redaction

Phase 13 introduced `RuntimeDetailRedactor` as package-private in `ui.console`. Phase 14 can either:

1. Generalize/move the pattern into a shared adapter-web package, or
2. Create an admin-local package-private redactor with the same conservative behavior.

Planning recommendation: use an admin-local `AdminMobileRedactor` unless moving Phase 13 code is trivial. Required behavior:

- Replace sensitive values containing API keys, bearer tokens, `sk-`, `password`, `rawSecret`, `apikey`, and token-like strings with `[REDACTED]`.
- Do not expose secrets in summary fields or advanced details.
- Keep collapsed detail content structured and bounded; never render raw unbounded JSON in summaries.

### View Conversion Priority

1. **Overview**: simplest; proves helper/CSS contracts and MADM-01.
2. **Registry**: largest; convert into explicit sectioned groups for Registry status, MCP servers/tools, Plugins/capabilities/actions, Extensions/capabilities. Sort or render abnormal states first where practical.
3. **Operations**: convert metrics and warnings into area cards with status chips and detail rows.
4. **Policy/Audit**: convert rows into redacted cards with summary fields visible and context/details collapsed by default.
5. **Verification**: add browser spec after selectors exist.

## Validation Architecture

### Automated Gates

Use focused Java component/contract tests after each Admin conversion:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminOperationsViewTest test
```

For full Admin mobile browser coverage:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-14-admin-governance-mobile.spec.ts --project="Mobile Chrome" --list
```

Optional full browser execution where Vaadin dev-mode/browser dependencies are available:

```bash
npm run e2e -- e2e/phase-14-admin-governance-mobile.spec.ts --project="Mobile Chrome"
```

### What Java Tests Should Sample

- Card structure, data hooks, section identifiers, status chip attributes.
- Default-collapsed `Details` for dense metadata/context.
- Broader DTO branch coverage for MCP disconnected/unhealthy, plugin selected/disabled/quarantined/load-error, extension status, policy decisions, and audit details.
- Redaction negative assertions for raw secret marker strings.

### What Playwright Should Sample

- Route load for landing, overview, registry, operations, policy decisions, audits, approvals.
- Card/detail content on Admin mobile routes.
- Registry explicitly includes MCP, Plugin, and Extension sections/details.
- Details expansion works and has a 44px/touch-safe control.
- No page-level horizontal overflow.
- Redaction negative checks after expanding details.

## Pitfalls to Avoid

- Do not add `/mobile/*` routes, mobile-only backend APIs, public DTO changes, or React/Hilla/Next/native/PWA behavior.
- Do not make horizontal scrolling tables the default Admin mobile solution.
- Do not hide required scan fields (status, health, counts, key IDs, timestamps) behind collapsed Details.
- Do not rely on screenshot visual regression as the primary gate.
- Do not require real MCP servers, real plugin JARs beyond existing controlled fixtures, model provider keys, Docker, or real-device UAT for Phase 14.
- Do not redesign `ApprovalCard`; reuse it for Admin approvals and only wrap/verify route inclusion.

## Plan Implications

- Plan 01 should establish helper/CSS contracts and Overview card conversion.
- Plan 02 should convert Registry/MCP/Plugin/Extension cards and branch tests.
- Plan 03 should convert Operations metric/warning cards.
- Plan 04 should convert Policy/Audit cards with redacted collapsed details.
- Plan 05 should add the MVER-04 Playwright/Admin documentation gate and include approvals route verification.
