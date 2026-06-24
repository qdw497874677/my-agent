# Phase 13 Runtime Cards, Timeline, Tool, and Approval UX

## Scope

Phase 13 completes the mobile Console runtime-card interior pass inside the existing Vaadin Chat/Event Feed. It keeps the Phase 12 product path and public REST/SSE DTO boundaries intact while making runtime events, governed tool events, dense diagnostics, and approval cards readable and safe under representative mobile viewport pressure.

This phase does not add a standalone timeline route, mobile-only backend API, new modal approval flow, React/Next/Hilla React frontend, native app, or new runtime/tool/model capabilities.

## Requirement Traceability

- **MCARD-01:** Runtime timeline events render as compact cards/accordions in `[data-role="event-feed"]` with status/category/type metadata, summaries, and expandable details.
- **MCARD-02:** Tool event cards use `[data-event-category="tool"]` and expose tool name/source/status/policy/approval/duration/error plus redacted input/output summaries.
- **MCARD-03:** Dense run/tool/policy details render behind `Details` / `[data-detail-layer="structured|advanced"]` and theme rules wrap long strings, JSON-like diagnostics, IDs, URLs, and error text without page-level horizontal overflow.
- **MCARD-04:** Pending approvals render as risk-first `[data-event-category="approval"]` cards with risk, side-effect context, policy reason, expected consequence, preview/arguments, and explicit approve/reject controls.
- **MCARD-05:** Phase 13 satisfies viewport-safe confirmation behavior through inline cards, details, action rows, and explicit approve/reject/status feedback; no new Dialog, ConfirmDialog, Notification, MenuBar, or ContextMenu interaction is introduced.

## Selector Contract

Browser tests and downstream phases should rely on stable `data-*` hooks and project-owned classes, not generated Vaadin IDs or shadow-DOM internals.

Core feed and category selectors:

- `[data-role="event-feed"]`
- `[data-event-category]`
- `[data-event-category="model"]`
- `[data-event-category="status"]`
- `[data-event-category="policy"]`
- `[data-event-category="terminal"]`
- `[data-event-category="event"]`
- `[data-event-category="tool"]`
- `[data-event-category="approval"]`

Card and detail selectors/classes:

- `.pi-runtime-event-card`
- `.pi-tool-call-card`
- `.pi-approval-card`
- `.pi-runtime-card-summary`
- `.pi-runtime-card-meta`
- `.pi-status-chip`
- `.pi-risk-chip`
- `.pi-detail-block`
- `.pi-redacted-json`
- `[data-expandable="true"]`
- `[data-detail-layer="structured"]`
- `[data-detail-layer="advanced"]`

Approval action selectors:

- `[data-approval-actions="inline"]`
- `[data-action="approve-tool-call"]`
- `[data-action="reject-tool-call"]`
- `[data-risk-action="approve"]`
- `[data-risk-action="reject"]`

## Redaction and Dense Details

Runtime, tool, and approval details reuse adapter-web redaction before rendering payload-derived text. The representative Phase 13 Playwright gate includes negative assertions that raw sensitive marker strings such as `sk-live-secret` and `raw-token-value` are not visible after details are expanded.

Dense details are layered:

1. compact summary on the card body;
2. structured details for input/output/decision summaries;
3. advanced redacted diagnostics through `[data-detail-layer="advanced"]`.

The `pi-mobile` theme applies `width: 100%`, `max-width: 100%`, `min-width: 0`, `overflow-wrap: anywhere`, and `word-break: break-word` to runtime/tool/approval cards and detail blocks so long payload-like text stays inside the viewport. Redacted JSON-like blocks may scroll internally only where appropriate and must not create page-level horizontal overflow.

## Approval UX Contract

Approvals remain inline and intentional. The card shows risk and side-effect context before the action row, then exposes approve/reject controls through stable action hooks. The action row uses `[data-approval-actions="inline"]`, wraps on narrow screens, and each action keeps at least `var(--pi-mobile-tap-target)` minimum size.

Phase 13 intentionally avoids a second-step modal confirmation. Status feedback remains on the card/pending approval surface so mobile users can see the outcome path without viewport-fitting dialog risk.

## Verification Commands

Java card and renderer contracts:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest test
```

Playwright Mobile Chrome list gate:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome" --list
```

Full representative browser execution, for CI/dev machines with stable Vaadin frontend/dev-server startup:

```bash
npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome"
```

## Known CI and Emulation Gaps

The required Phase 13 local verification gate is the Java contract command plus Playwright `--list` compatibility. Full browser execution depends on the same Vaadin development-mode and browser host dependencies documented in Phases 10-12. If local WebKit/Chromium host dependencies or Vaadin frontend startup are unavailable, keep the selector/list gate intact and run the full browser command on CI or a developer machine with stable Playwright browser support.

The representative browser matrix intentionally covers a deterministic fake/no-key event mix rather than every possible runtime DTO shape. Java component contracts cover additional card branches.

## Deferred Handoffs

- **Phase 14:** Full Admin Governance mobile conversion remains separate. Phase 13 only confirms the shared approval-card pattern is viewport-safe for the Console/admin card component contract.
- **Phase 15:** Real-device Android Chrome/Edge/Firefox and iOS Safari UAT, portrait/landscape sweeps, deeper accessibility audits, keyboard traversal hardening, and final desktop/mobile regression expansion remain release-hardening scope.
- **Future UX:** Standalone run timeline routes, tool-call lifecycle aggregation, event filtering/copy/share, and modal confirmation redesign remain deferred until product requirements justify a broader Console information-architecture change.
