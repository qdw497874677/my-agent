# Phase 11 Responsive Shell and Navigation

Phase 11 adds the shared Vaadin responsive shell used by the Agent Console and Admin Governance surfaces. It keeps the Java/Vaadin-first architecture from earlier phases and does not add mobile-only backend routes, frontend frameworks, or new runtime capabilities.

## Route Coverage

The shell covers the same eight Phase 10 routes:

| Product area | Route | Navigation label | Stable route marker |
| --- | --- | --- | --- |
| Console | `/console` | Console | `[data-route="console"]` |
| Admin | `/admin/governance` | Admin | `[data-route="admin-governance"]` |
| Admin | `/admin/governance/overview` | Overview | `[data-route="admin-governance-overview"]` |
| Admin | `/admin/governance/registry` | Registry | `[data-route="admin-registry-status"]` |
| Admin | `/admin/governance/operations` | Operations | `[data-route="admin-operations"]` |
| Admin | `/admin/governance/policy-decisions` | Policy Decisions | `[data-route="admin-policy-decisions"]` |
| Admin | `/admin/governance/audits` | Audits | `[data-route="admin-audit-summaries"]` |
| Admin | `/admin/governance/approvals` | Approvals | `[data-route="admin-approval-queue"]` |

Console and Admin remain distinct top-level product areas. Admin Governance exposes grouped sub-navigation for Overview, Registry, Operations, Policy Decisions, Audits, and Approvals.

## Selector Contract

Phase 11 extends the Phase 10 stable selector contract with shell/navigation hooks:

- `[data-shell="pi-responsive-shell"]` — shared shell root.
- `[data-shell-drawer-trigger]` and `[data-shell-drawer-close]` — compact drawer controls.
- `[data-nav="primary"]` — primary shared navigation root.
- `[data-nav-item="<route>"]` — route-backed nav item from the Java registry.
- `[data-nav-active="true"]` — current active route item.
- `[data-page-title]` — current route title in shell/header primitives.
- `[data-primary-action]` — page or shell primary action slot.

Use these hooks for Playwright and contract tests instead of brittle text-only selectors.

## Touch and Focus Contract

The `pi-mobile` Vaadin theme defines `--pi-mobile-tap-target: 44px` and applies the mobile tap floor to shell nav items, drawer controls, links, buttons, details/expanders, primary actions, refresh controls, approval controls, cancel controls, and existing `data-action` hooks.

Keyboard and tablet users get a visible `:focus-visible` ring via theme tokens. The drawer close action returns focus to the drawer trigger. Phase 11 intentionally implements a basic usable focus order; broader keyboard traversal and accessibility hardening remain Phase 15.

## Verification Commands

Java contract tests:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest test
```

Playwright list gates:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-11-shell-navigation.spec.ts --project="Mobile Chrome" --list
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-11-shell-navigation.spec.ts --list
```

Full browser execution, for CI/dev machines with stable Vaadin frontend/dev-server startup:

```bash
npm run e2e -- e2e/phase-11-shell-navigation.spec.ts --project="Mobile Chrome"
```

The Playwright gate is deterministic/no-key. It opens routes, navigates through shell links, samples geometry/focus, and reads existing stable content. It does not start runs, approve/reject tool calls, refresh providers/plugins/MCP, cancel runs, or mutate governance state.

## Deferred Scope

- Full Console mobile-first flow and composer/session/event stream redesign: Implemented in Phase 12; see docs/phase-12-console-mobile-flow.md.
- Runtime event/tool/approval cards and final confirmation UX: Phase 13.
- Full Admin card/detail/table migration: Phase 14.
- Real-device UAT, orientation sweeps, and deeper accessibility hardening: Phase 15.
