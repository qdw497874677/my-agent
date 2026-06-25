# Phase 14 Admin Governance Mobile Coverage

## Scope

Phase 14 completes the Admin Governance mobile-first coverage pass for existing Vaadin routes. It proves that mobile admins can inspect Governance Overview, Registry, Operations, MCP, Plugin, Extension, Policy Decision, Audit, and Approval Queue surfaces through stable card/detail selectors without introducing mobile-only backend APIs or a second frontend stack.

This phase is limited to existing public Admin routes and deterministic no-key verification. Real-device/UAT, broad cross-browser hardening, portrait/landscape sweeps, tablet matrix expansion, deeper accessibility audits, and final desktop regression expansion remain Phase 15.

## Requirement Traceability

- **MADM-01:** Governance Overview renders stacked status cards with runtime/provider/tool/extension/MCP/plugin health, counts, messages, and links.
- **MADM-02:** Registry and Operations data render as mobile cards or responsive row details without relying on page-level horizontal table scrolling.
- **MADM-03:** MCP server/tool status, refresh/status metadata, and unhealthy/disconnected states are inspectable in Registry card/detail sections.
- **MADM-04:** Plugin selected/disabled/quarantined/load-error state and metadata are inspectable in stacked card/detail layouts.
- **MADM-05:** Extension sources, contributions, providers, tools, listeners, status, and expandable metadata are inspectable in Registry card/detail sections.
- **MADM-06:** Policy decisions expose decision, reason, tool/run/session IDs, timestamp, and expandable redacted context.
- **MADM-07:** Audit summaries expose actor/source/action/status/resource/timestamp and expandable redacted details.
- **MVER-04:** `e2e/phase-14-admin-governance-mobile.spec.ts` opens the full Admin mobile route set and verifies mobile card/detail content, details expansion, redaction, tap/focus sampling, and no page-level horizontal overflow through the Mobile Chrome list gate.

## MVER-04 Route Coverage

| Route | Stable route marker | Required mobile content |
| --- | --- | --- |
| `/admin/governance` | `[data-route="admin-governance"]` | Admin landing surface and mobile-critical root |
| `/admin/governance/overview` | `[data-route="admin-governance-overview"]` | Governance overview cards |
| `/admin/governance/registry` | `[data-route="admin-registry-status"]` | Registry, MCP, Plugin, and Extension cards/details |
| `/admin/governance/operations` | `[data-route="admin-operations"]` | Operations metric/status cards |
| `/admin/governance/policy-decisions` | `[data-route="admin-policy-decisions"]` | Policy decision cards and redacted context Details |
| `/admin/governance/audits` | `[data-route="admin-audit-summaries"]` | Audit cards and redacted audit Details |
| `/admin/governance/approvals` | `[data-route="admin-approval-queue"]` | Existing ADMIN `ApprovalCard` content or empty-state marker |

## Selector Contract

Browser tests and downstream Phase 15 verification should use stable `data-*` selectors instead of generated Vaadin IDs or text-only assertions.

Core Admin card/detail selectors:

- `[data-admin-card]`
- `[data-admin-details]`
- `[data-admin-overview-card]`
- `[data-admin-registry-section]`
- `[data-status-severity]`

Registry, MCP, Plugin, and Extension selectors:

- `[data-mcp-server-card]`
- `[data-mcp-tool-card]`
- `[data-plugin-card]`
- `[data-plugin-capability-card]`
- `[data-extension-source-card]`
- `[data-extension-capability-card]`

Operations selectors:

- `[data-operations-card]`
- `[data-operations-warning-card]`

Policy and Audit selectors:

- `[data-policy-decision-card]`
- `[data-audit-card]`

Approval route selectors reuse Phase 13 approval-card hooks:

- `[data-event-category="approval"]`
- `[data-approval-actions="inline"]`
- `[data-action="approve-tool-call"]`
- `[data-action="reject-tool-call"]`
- `[data-state="empty-admin-approvals"]`

## Redaction and Detail Rules

Admin mobile pages follow the Phase 13 layered detail model: compact card summaries first, structured label/value rows second, and dense/raw-ish troubleshooting context only behind collapsed Details. Policy and Audit details are collapsed by default through `[data-admin-details="policy-context"]` and `[data-admin-details="audit-details"]`.

Sensitive keys and marker values must not render in default summaries or expanded details. The Phase 14 browser gate expands the first visible Admin Details/control where present and asserts that the body does not contain `sk-test-secret`, `PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK`, `PI_PHASE8_FAKE_SECRET_DO_NOT_LEAK`, `rawSecret`, or `raw-token-value`.

Long IDs, URLs, JSON-like metadata, stack/error text, and redacted payload blocks must wrap or scroll internally without creating page-level horizontal overflow. Details expanders and action controls inherit the shared 44px tap-target and visible focus contract documented in Phase 11.

## Verification Commands

Java Admin component contracts:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest,McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest,AdminOperationsViewTest test
```

Playwright Mobile Chrome no-key list gate:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-14-admin-governance-mobile.spec.ts --project="Mobile Chrome" --list
```

Required final local gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest,McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest,AdminOperationsViewTest test && PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-14-admin-governance-mobile.spec.ts --project="Mobile Chrome" --list
```

## Phase 15 Handoffs

- Real-device/UAT for Android Chrome, iOS Safari, Edge mobile, and Firefox mobile remains Phase 15.
- Broad cross-browser/orientation hardening remains Phase 15.
- Representative portrait, landscape, tablet, and desktop regression expansion remains Phase 15.
- Deeper accessibility audits and keyboard traversal hardening remain Phase 15.
