# Phase 13: Runtime Cards, Timeline, Tool, and Approval UX — Research

**Date:** 2026-06-24  
**Status:** Complete  
**Scope:** Planning research for mobile Vaadin runtime event cards, tool cards, approval cards, dense details, and deterministic verification.

## Research Question

What do we need to know to plan Phase 13 well while honoring the locked user decisions in `13-CONTEXT.md`?

## Key Findings

### Vaadin Details for layered mobile disclosure

- Vaadin `Details` is an expandable panel with an always-visible summary and a collapsible content area.
- The summary supports rich components, which fits Phase 13's requirement that status, timestamp/type, summary, and risk/status chips remain visible by default.
- Collapsed content is hidden and not keyboard/screen-reader reachable, so important risk/action information must not be hidden behind Details.
- Best practice is to use Details to group related content and reduce clutter; avoid hiding important information unless expanded by default.

**Planning implication:** Use `Details` for structured/advanced redacted details only. Keep status, type/timestamp, tool name, policy/approval state, risk, side effect, and Approve/Reject actions visible in card bodies.

### Vaadin Dialog/ConfirmDialog/Notification boundary

- Vaadin Dialogs are modal by default and disruptive; docs recommend using them sparingly.
- Dialogs are not constrained to the viewport by default; `setKeepInViewport(true)` is required if a dialog is unavoidable.
- ConfirmDialog should contain an informative title/message and explicit action labels, but Phase 13 locked decision D-08 explicitly rejects a second confirmation dialog for approvals.

**Planning implication:** Do not introduce new Dialog, ConfirmDialog, Notification, MenuBar, or ContextMenu interactions. Satisfy MCARD-05 through inline card/detail viewport safety and CSS overflow controls. If an executor discovers unavoidable existing Dialog usage, it must set viewport-fitting behavior and document it, but no plan should add new modal primitives.

### Playwright mobile assertions

- `locator.boundingBox()` returns element dimensions relative to the viewport and returns `null` if the element is not visible, which supports 44px tap-target sampling.
- `locator.focus()` calls focus on the element; the existing `expectFocusVisible` helper samples computed outline/box-shadow/border/focus-visible state.
- Existing project helper `expectNoPageHorizontalOverflow(page)` checks document/body scroll width against client width.

**Planning implication:** Extend the Phase 12 product-path spec instead of adding a separate route. Use existing helpers for overflow, tap target, and focus-visible checks. Keep browser E2E representative rather than exhaustive; use Java component tests for branch coverage.

## Existing Code Pattern Summary

- `RunEventRenderer` is the central event-to-component dispatcher and already returns `ApprovalCard`/`ToolCallCard` components through `RenderedEvent`.
- `ChatEventStreamPanel.appendEvent(...)` appends either a component or fallback text into `[data-role="event-feed"]`, preserving Phase 12 poll-backed append/dedupe behavior.
- `ToolCallCard` and `ApprovalCard` already perform basic redaction and use `Details`, but their text is pipe-delimited and needs structured summary/detail/advanced layers.
- `pi-mobile/styles.css` already defines `--pi-mobile-tap-target`, `.pi-card`, `.pi-detail`, `.pi-action-row`, feed spacing, overflow wrapping, and focus rings.
- Existing tests `WebConsoleCatalogAndToolCardsTest`, `WebConsoleApprovalCardsTest`, and `WebConsoleMobileFlowContractTest` are the right Java contract-test homes for card renderer behavior; a new `WebConsoleRuntimeCardsTest` can isolate new runtime-card/redaction contracts.
- Existing E2E `phase-12-console-mobile-flow.spec.ts` is the product-path base; Phase 13 should create `phase-13-runtime-cards.spec.ts` reusing the same selectors and fake runtime hints.

## Recommended Plan Architecture

1. Create a reusable adapter-web-only redaction/detail utility and generic runtime event/timeline card component.
2. Refactor `ToolCallCard` to structured summary fields plus expandable redacted input/output/detail/advanced sections.
3. Refactor `ApprovalCard` to risk-first inline hierarchy and shared USER/ADMIN decision semantics.
4. Add browser representative event-matrix proof, docs, and final mobile CSS contract.

## Validation Architecture

Use dual gates:

- **Java component/contract gate:** fast Maven tests covering renderer branches, redaction, structured Details, tool fields, approval `DecisionPlan`, and Admin reuse.
- **Playwright representative browser gate:** deterministic/no-key Console product path covering card interiors, Details expansion, redacted advanced detail, approve/reject controls, no page overflow, 44px tap targets, and focus-visible behavior.

Quick commands:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest test
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome" --list
```

Full browser execution remains environment-dependent in the current container, consistent with Phase 10/12 notes; CI or a prepared runner should execute the actual Mobile Chrome/Mobile Safari/Tablet projects.

## Constraints and Non-Goals

- No new runtime/model/tool/backend capability.
- No new public REST/SSE DTOs and no `/mobile/*` APIs.
- No standalone timeline route/panel.
- No tool lifecycle aggregation.
- No new modal confirmation workflow.
- No Admin Governance full-site card conversion beyond shared approval-card reuse.
