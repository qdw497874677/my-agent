# Phase 13: Runtime Cards, Timeline, Tool, and Approval UX - Context

**Gathered:** 2026-06-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 13 makes the existing Vaadin Console runtime-event interiors safe and readable on mobile. Mobile users must be able to inspect run timeline events, tool execution cards, dense run/tool/policy/audit-like details, and pending approvals from the existing Chat/Event Feed flow without raw sensitive payload exposure or page-level horizontal overflow. Approval actions must be risk-first and intentional, and any dialogs/drawers/notifications/confirmations already present or necessary for the flow must fit the viewport with explicit close/action controls.

This phase stays inside `pi-agent-adapter-web`, the existing Vaadin Flow UI, `pi-mobile` theme, and deterministic fake-runtime browser verification harness. It does **not** introduce new Agent runtime/model/tool capabilities, new public REST/SSE DTOs, viewport-specific backend APIs, `/mobile/*` endpoints, React/Next/Hilla React, native app, PWA/offline behavior, or a separate non-chat Console timeline route.

</domain>

<decisions>
## Implementation Decisions

### Runtime Timeline Shape
- **D-01:** Render the mobile runtime timeline by **enhancing the existing Chat/Event Feed**, not by introducing a competing standalone `RunTimelinePanel` or separate route. Runtime events should appear as compact timeline-style cards/accordions inside the Phase 12 chat-first narrative.
- **D-02:** Each timeline card default view should show **status, timestamp/type, and short summary**. Additional IDs, payload schema details, policy/tool/audit-like context, and troubleshooting fields belong behind `Details`/expanders.
- **D-03:** Timeline work should preserve the current `RunEventRenderer` → `ChatEventStreamPanel.appendEvent(...)` seam and the existing poll-backed append/dedupe behavior unless research proves a small adapter is necessary for card rendering.

### Tool Card Structure
- **D-04:** Tool execution should remain **one rendered card per runtime event** rather than aggregating all lifecycle events for a tool call into one mutable/progress card. This preserves the existing event append/dedupe model and keeps Phase 13 scoped to card interiors.
- **D-05:** Tool cards should default to a key summary: tool name, source, status, policy/approval state, duration when available, and error when present. Redacted input/output summaries and dense fields should be expandable.
- **D-06:** Do not introduce mixed aggregation rules for some tools and event cards for others in this phase. Consistency and testability are more important than a more sophisticated lifecycle visualization.

### Approval Risk UX
- **D-07:** Pending tool approval should use an **inline risk-first ApprovalCard** in the existing feed/pending-approval surfaces, not a modal-first or separate approval workflow. The card should make risk, side-effect context, policy reason, expected consequence, provision preview, and redacted argument summary visible in a mobile-readable hierarchy.
- **D-08:** Approve/Reject actions should remain inline 44px touch-safe controls with clear primary/secondary styling and risk/confirmation wording near the action. Do **not** require a second dialog, long-press, or multi-step confirmation in Phase 13.
- **D-09:** The same risk-first card pattern should work for both Console USER approvals and Admin ADMIN approvals where the existing card is reused, without collapsing Admin Governance mobile conversion into this phase.

### Dense Details and Redaction
- **D-10:** Dense run/tool/policy/audit-like details should use a **layered detail model**: compact summary by default, structured detail expansion, and advanced detail when useful.
- **D-11:** Advanced details may show **redacted pretty/raw-like JSON** where it helps troubleshooting, but only after redaction, size/line wrapping, and mobile overflow safeguards are applied. Never expose raw sensitive payloads directly.
- **D-12:** Redaction must be conservative and reusable across runtime/tool/approval detail rendering. Any new renderer must protect common sensitive markers such as API keys, passwords, tokens, and secrets and must avoid horizontal overflow for long strings, JSON, stack traces, IDs, URLs, and command-like text.

### Dialog, Notification, and Feedback Boundary
- **D-13:** Phase 13 should **not introduce new Dialog, ConfirmDialog, Notification, MenuBar, or ContextMenu interactions** unless implementation discovers an existing unavoidable platform behavior. Prefer inline `Details`, cards, action rows, and status feedback.
- **D-14:** MCARD-05 should be satisfied through viewport-fitting inline/card/detail behavior and explicit close/action controls for any existing drawers/dialog-like surfaces encountered, rather than by adding new modal primitives.

### Verification Scope
- **D-15:** Browser E2E should cover a **Console representative runtime event matrix**, not every possible DTO/event schema exhaustively. The matrix should include representative status, model, tool, approval, policy, terminal, error, and dense-detail events.
- **D-16:** The Phase 13 browser gate should assert card interiors, Details expansion, redacted advanced detail behavior, approve/reject actions, no page-level horizontal overflow, 44px tap targets for actions/expanders, and focus-visible behavior for interactive controls.
- **D-17:** Java contract/component tests should cover more renderer/card branches than browser E2E where practical, especially `RunEventRenderer`, `ToolCallCard`, `ApprovalCard`, redaction, `DecisionPlan`, and DTO fixture variants. Keep the established Java contract + Playwright dual-gate pattern.
- **D-18:** Tests must stay deterministic/no-key using fake runtime/test fixtures, stable `data-*` selectors, and existing mobile smoke helpers. Do not require real model providers, real tools, external MCP servers, or real-device UAT in Phase 13.

### Folded Todos
- No pending todos matched Phase 13 scope.

### the agent's Discretion
- Exact visual styling, badge/chip names, timeline connector treatment, colors, typography, spacing, and breakpoint polish are planner/designer discretion as long as risk-first readability, tap/focus, and no-overflow contracts are met.
- Exact Java helper/class extraction for shared card primitives and redaction utilities is planner discretion, provided code remains inside adapter-web UI/theme/test boundaries and does not leak Vaadin concerns into Domain/App/public DTOs.
- Exact representative event fixture content is planner discretion, provided the browser matrix covers the categories in D-15 and Java contracts cover renderer/card branches sufficiently.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 13 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 13 — Phase goal, dependency on Phase 12, MCARD-01 through MCARD-05 mapping, and success criteria for timeline cards, tool cards, dense details, approval cards, and viewport-fitting confirmations.
- `.planning/REQUIREMENTS.md` §Runtime Cards, Timeline, Tools, and Approvals — MCARD-01 through MCARD-05 requirements for compact cards/accordions, tool card fields, dense detail safety, risk-first approval, and viewport-safe dialogs/drawers/notifications.
- `.planning/PROJECT.md` §Current Milestone: v1.1 适配移动端web — milestone boundary: existing Vaadin Web Console/Admin Governance converted to mobile-first H5 while preserving Java/Vaadin/public REST/SSE DTO boundaries.
- `.planning/STATE.md` — Current v1.1 state, Phase 12 completion handoff, and accumulated decisions for mobile theme, selectors, Console flow, and Phase 13 deferred runtime/tool/approval card interiors.

### Prior Mobile Foundation Decisions
- `.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-CONTEXT.md` — `pi-mobile` theme baseline, representative Playwright matrix, route smoke/no-overflow helpers, stable `data-*` selector contract, and explicit deferral of final runtime-card/approval UX to Phase 13.
- `.planning/phases/11-shared-responsive-shell-and-navigation/11-CONTEXT.md` — shared `PiResponsiveShell`, compact header/drawer, status/action slot, 44px tap target, focus-visible contract, base card/detail/action-row primitives, and shell/navigation Playwright patterns.
- `.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md` — Chat-first mobile Console flow, sticky composer/cancel state, vertical event feed, fake-runtime MVER-03 gate, and explicit deferral of runtime/tool/approval card interiors and dense details to Phase 13.

### Existing Product Documentation
- `docs/phase-05-web-console.md` — Existing Web Console/Admin public API boundary, Console run/session/catalog/tool/approval behavior, and UI/test patterns.
- `docs/phase-10-mobile-baseline.md` — Implemented mobile browser matrix, route smoke helpers, no-horizontal-overflow assertions, E2E commands, and Phase 15 UAT handoff.
- `docs/phase-11-responsive-shell.md` — Implemented shared shell/navigation behavior, selector/touch/focus contract, and deferred runtime event/tool/approval card UX.
- `docs/phase-12-console-mobile-flow.md` — Implemented Console mobile flow and Phase 13 deferred handoff for runtime event card interiors, tool cards, approval risk UX, dialogs/confirmations, and dense detail expansion.

### External Documentation to Research During Planning
- Vaadin Flow 24.x `Details`, layout/theme/component styling, focus-visible, and mobile responsive styling documentation — verify best practice for expandable mobile cards, keyboard behavior, and viewport-safe details.
- Vaadin Flow 24.x Dialog/ConfirmDialog/Notification documentation only as a boundary check — Phase 13 decision is to avoid introducing new modal primitives unless existing behavior requires it.
- Playwright documentation — mobile viewport assertions, focus/tap-target geometry, Details expansion assertions, JSON/text wrapping visibility, and multi-project mobile test stability.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java` — Central event-to-category/card dispatcher for model, approval, tool, policy, terminal, status, and generic events; the main seam for richer timeline card components.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` — Owns `[data-role="event-feed"]`, event append behavior, and the mobile feed into which runtime cards should render.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` — Owns the Phase 12 chat-first flow, poll-backed event replay, sequence dedupe, terminal propagation, and panel state.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java` — Existing expandable tool lifecycle card with `data-event-category="tool"`, `data-tool-status`, `data-expandable="true"`, summary/details text, and redaction heuristics.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java` — Existing approval card with approve/reject actions, `DecisionPlan`, redacted summaries, USER/ADMIN actor role support, and side-effect/risk-related DTO fields.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java` — Pending-approval list container in Console.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java` — Admin approval route reusing `ApprovalCard` with ADMIN actor context.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` — Canonical Vaadin-side REST path/DTO registry for events, approvals, approval decisions, runs, and cancellation.
- `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` — Existing mobile baseline tokens and primitives: `--pi-mobile-tap-target`, `--pi-mobile-focus-ring`, safe areas, overflow/wrap defaults, `.pi-card`, `.pi-detail`, `.pi-action-row`, feed/grid/page styling.
- `e2e/fixtures/fake-runtime.ts` — Deterministic fake runtime helpers for runs, approval runs, approval decisions, cancellation, events, and mobile tool/approval hints.
- `e2e/fixtures/mobile-smoke.ts` — Reusable no-overflow, tap-target, focus-visible, and selector helper functions.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java`, `WebConsoleCatalogAndToolCardsTest.java`, and `WebConsoleMobileFlowContractTest.java` — Existing Java component/contract fixture patterns for approval cards, tool cards, feed append/dedupe, terminal behavior, and renderer contracts.

### Established Patterns
- UI implementation belongs in `pi-agent-adapter-web`; Domain/App/client/public DTO contracts must stay free of Vaadin, Playwright, responsive theme, and viewport-specific concerns.
- Production frontend remains Vaadin Java/Flow plus `pi-mobile` theme; TypeScript Playwright is accepted only as browser test tooling.
- Dense Vaadin UI tests should use stable `data-*` selectors such as `data-role`, `data-action`, `data-event-category`, `data-tool-status`, `data-approval-id`, `data-field`, `data-state`, and `data-primary-action`, supplemented by accessibility selectors for controls.
- Existing card disclosure uses Vaadin `Details`; no Dialog/ConfirmDialog/Notification/MenuBar/ContextMenu pattern exists in adapter-web today.
- Browser E2E remains deterministic/no-key and should rely on fake runtime/test fixtures instead of real model providers, real remote tools, external MCP servers, or live credentials.
- Admin and Console surfaces are separated, but approval card rendering is already shared between USER and ADMIN contexts.
- Vaadin Console rendering currently updates via poll-backed replay (`UI` polling), not a browser-side SSE-driven renderer. Phase 13 should not assume a different live rendering architecture.

### Integration Points
- Extend `RunEventRenderer` and existing card classes to return richer card components for representative event categories while preserving append/dedupe semantics.
- Extend `ToolCallCard` and `ApprovalCard` from pipe-delimited strings toward structured summary fields, badges/chips if useful, Details sections, and redacted advanced details.
- Add `pi-mobile/styles.css` rules for `.pi-tool-call-card`, `.pi-approval-card`, `.pi-approval-panel`, timeline/event-card interiors, status/risk chips, dense detail blocks, wrapping, and action rows.
- Add/extend Java contract tests for card content, redaction, Details expansion structure, DecisionPlan, renderer category mapping, and mobile CSS selector contracts.
- Add `e2e/phase-13-runtime-cards.spec.ts` or equivalent, reusing Phase 12 Console flow, fake runtime fixtures, and mobile-smoke helpers to validate representative runtime event matrix and approval actions.

</code_context>

<specifics>
## Specific Ideas

- User selected all proposed Phase 13 discussion areas: timeline shape, tool card structure, approval risk UX, dense detail safety, dialog/feedback boundary, and test scope.
- User chose to enhance the existing Chat/Event Feed as the runtime timeline rather than creating a separate timeline panel or route.
- User chose status + timestamp/type + summary as the default card density.
- User chose one card per tool runtime event, not tool-call lifecycle aggregation.
- User chose default tool-card visibility of key fields, with redacted input/output summaries behind expansion.
- User chose inline risk-first approval cards with explicit risk/side-effect/consequence wording near Approve/Reject, and no second-step modal confirmation.
- User chose layered dense details and allowed redacted pretty/raw-like JSON in advanced detail sections when wrapped, bounded, and safely redacted.
- User chose not to introduce Dialog/ConfirmDialog/Notification in Phase 13 unless unavoidable.
- User initially wanted a full event matrix; this was scoped to a Console representative runtime event matrix for browser E2E, with Java contracts covering additional renderer/card branches.

</specifics>

<deferred>
## Deferred Ideas

- Standalone Run Timeline route/panel separate from the Chat/Event Feed — deferred unless a future phase changes Console information architecture.
- Tool-call lifecycle aggregation into a single mutable/progress card — deferred; Phase 13 preserves event-card semantics.
- New modal confirmation/dialog/notification framework for approvals and runtime feedback — deferred to a future UX/accessibility phase if inline feedback proves insufficient.
- Admin Governance full-site mobile card/detail conversion beyond shared approval card behavior — Phase 14.
- Broad orientation/cross-browser/accessibility hardening, final desktop/mobile regression expansion, and real-device/UAT release documentation — Phase 15.
- Native app, PWA/offline behavior, push/background monitoring, deep-linkable incident triage, event filtering/copy/share, and new mobile-only Agent capabilities — future/out of scope for v1.1 unless a later roadmap adds them.

</deferred>

---

*Phase: 13-runtime-cards-timeline-tool-and-approval-ux*
*Context gathered: 2026-06-24*
