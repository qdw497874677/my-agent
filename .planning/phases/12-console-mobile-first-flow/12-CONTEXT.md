# Phase 12: Console Mobile-First Flow - Context

**Gathered:** 2026-06-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 12 converts the existing Vaadin Agent Console workflow into a phone-first H5 flow. Mobile users must be able to browse/select the General Agent, start or continue chat/run sessions, type multi-line prompts, observe live SSE output in a vertical feed, open session history, identify the active session, and cancel active runs from visible mobile controls.

This phase stays inside the existing Console UI, theme, and deterministic browser verification harness. It does **not** redesign runtime/tool/approval card interiors, dense details, dialogs, or approval risk UX beyond keeping them reachable in the Console flow; those belong to Phase 13. It does **not** add runtime/model/tool capabilities, new public REST/SSE DTOs, mobile-only backend APIs, React/Next/Hilla React, native app, or PWA/offline behavior.

</domain>

<decisions>
## Implementation Decisions

### Chat-First Mobile Layout
- **D-01:** On phones, the default Console view must be **Chat Feed first**. Users should land in the chat/event stream as the primary workflow, not in Catalog or Session history.
- **D-02:** Agents, Sessions, and Run Context should be reachable through a touch-safe in-page segmented switcher/tab pattern, rather than relying only on the global shell drawer or desktop-style side columns.
- **D-03:** When a secondary panel is opened, the implementation may use either overlay/sheet behavior or inline/down-push expansion, but it must preserve Chat state. Returning to Chat must keep the current feed/session/input context rather than resetting the flow.
- **D-04:** Wider tablet/desktop layouts should restore a responsive multi-column Console where appropriate, such as Sessions/Catalog, Chat/Event Feed, and Run Context columns. Phase 12 should not degrade desktop Console efficiency while making the phone flow single-primary.

### Composer and Run Controls
- **D-05:** The mobile Chat composer must be bottom-sticky or otherwise persistently reachable while users scroll the event feed. The current normal-flow composer behavior is not sufficient for Phase 12.
- **D-06:** The composer must support multi-line prompts with bounded/limited auto-growth. When input exceeds the maximum comfortable height, the TextArea should scroll internally instead of pushing the feed and controls off screen.
- **D-07:** Active run state should be shown inline near the composer, covering queued/running/cancelling/terminal feedback so users understand what Send/Cancel will do without scanning the whole page.
- **D-08:** Active run cancellation should use a **dual-position** pattern: a primary touch-safe Cancel affordance near the composer/run controls plus a backup visible affordance in the shell/page status/action area where practical. Users should not lose access to Cancel while scrolling live output.

### Agent Catalog and Session Flow
- **D-09:** Mobile Agent Catalog should render as full-width stacked Agent cards. Reuse the existing AgentCard/Catalog model where possible, but the mobile presentation must not depend on desktop-width columns.
- **D-10:** The General Agent card should expose a prominent Start/Continue primary CTA. Other entry actions may remain available but should be visually secondary on phones.
- **D-11:** Mobile Session history should show enough information to choose safely: short title/summary, recent run/session status, updated time, and a clear active-session highlight.
- **D-12:** Selecting a historical session should return the user to Chat, load/show the session history, and make the active session identity clear. Users should be able to continue from that state without an extra route or desktop-only detail page.

### SSE Feed and Event Scope
- **D-13:** The event stream should be a vertical mobile feed that can show live/incremental SSE output and prior events while preserving access to composer and cancellation controls.
- **D-14:** Phase 12 should verify and improve feed placement/scroll behavior, but it should avoid a full redesign of individual runtime/tool/approval/dense-detail cards. Detailed runtime cards, tool cards, approval cards, dialogs, and safe detail expansion remain Phase 13.
- **D-15:** Streamed event UI should prove at least meaningful status/model/terminal or cancellation progression in mobile flow. It does not need to assert every event category or tool-card interior in this phase.

### Verification and Regression Gates
- **D-16:** Phase 12 must add a full Console mobile product-path E2E: open Console, select/start General Agent, enter a multi-line prompt, submit, observe streamed event UI, open/use session/history surfaces, and either cancel an active run or reach terminal status.
- **D-17:** The MVER-03 browser gate should run the main Console path on a representative mobile/tablet matrix: at minimum Mobile Chrome, Mobile Safari/WebKit proxy, and Tablet. Mobile Firefox may run smoke/viewport/no-overflow coverage if full SSE/composer behavior is too flaky in CI.
- **D-18:** E2E should assert event feed visibility and incremental behavior, plus that scrolling prior events does not remove practical access to composer and Cancel. It should not require all Phase 13 card details.
- **D-19:** Preserve or update desktop Console regression coverage after the mobile-first refactor. Phase 12 changes Console core behavior, so waiting until Phase 15 for all desktop regression would be too risky.
- **D-20:** Keep tests deterministic/no-key using the existing fake runtime, public REST/SSE APIs, stable `data-*` selectors, and Playwright helper patterns from Phases 10-11.

### Folded Todos
- No pending todos matched Phase 12 scope.

### the agent's Discretion
- Exact segmented control styling, labels, iconography, breakpoint values, and whether secondary panels are implemented as sheets, drawers, details, tabs, or inline regions are planner/designer discretion as long as D-01 through D-04 hold.
- Exact sticky composer CSS mechanics, TextArea max-height, and safe-area handling are implementation discretion, provided the composer remains usable at representative phone/tablet viewports.
- Exact distribution of the dual Cancel controls between composer, page status slot, and shell action slot is implementation discretion, provided mobile users have visible touch-safe cancellation while a run is cancellable.
- Exact fake run fixture timing/event sequence is planner discretion, provided MVER-03 proves a real browser-visible Console loop and remains deterministic/no-key.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 12 Scope and Mobile Requirements
- `.planning/ROADMAP.md` §Phase 12 — Phase goal, dependency on Phase 11, MCON-01 through MCON-05 plus MVER-03 mapping, and success criteria for Agent Catalog, Chat/Run, SSE feed, sessions, and cancellation.
- `.planning/REQUIREMENTS.md` §Console Mobile Experience — MCON-01 through MCON-05 requirements for Catalog, prompt submission, live event feed, session history, and cancellation.
- `.planning/REQUIREMENTS.md` §Mobile Verification and Release Gates — MVER-03 requirement for Console mobile E2E covering fake/no-key run, streamed event UI, tool/approval/session areas, and cancel or terminal status.
- `.planning/PROJECT.md` §Current Milestone: v1.1 适配移动端web — milestone boundary: existing Vaadin Web Console/Admin Governance converted to mobile-first H5 while preserving Java/Vaadin/public REST/SSE DTO boundaries.
- `.planning/STATE.md` — Current v1.1 state and concern that Phase 12 should inspect current Console fixed-width, scroll, and composer behavior before implementation planning.

### Prior Mobile Foundation Decisions
- `.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-CONTEXT.md` — `pi-mobile` theme baseline, representative Playwright matrix, route smoke/no-overflow helpers, and stable `data-*` selector contract.
- `.planning/phases/11-shared-responsive-shell-and-navigation/11-CONTEXT.md` — shared `PiResponsiveShell`, compact header/drawer, status/action slot, 44px tap target, focus-visible contract, page primitives, and shell/navigation Playwright patterns.
- `docs/phase-10-mobile-baseline.md` — Implemented mobile browser matrix, route smoke helpers, no-horizontal-overflow assertions, E2E commands, and known emulation gaps.
- `docs/phase-11-responsive-shell.md` — Implemented shared shell/navigation behavior and explicit handoff that full Console mobile-first flow, composer/session/event stream redesign belongs to Phase 12.

### Existing Console Product Boundary
- `.planning/phases/05-agent-web-console-and-runtime-cockpit/05-CONTEXT.md` — Console is Chat-first, Vaadin consumes public APIs/read models, tool/approval cards are visible in Console, and Playwright/no-key browser E2E is accepted as test-only tooling.
- `docs/phase-05-web-console.md` — Existing Web Console/Admin public API boundary, Console run/session/catalog/tool/approval behavior, and UI/test patterns.

### External Documentation to Research During Planning
- Vaadin Flow 24.x layout/theme/component styling documentation — sticky/footer composer patterns, mobile safe-area CSS, tabs/segmented controls, Details/sheet alternatives, and TextArea sizing behavior.
- Playwright documentation — mobile keyboard/input behavior, SSE/event UI assertions, scroll/visibility assertions, project-specific retries, and stable multi-browser mobile testing patterns.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` — Existing `/console` route with three-panel workbench composition: sessions/catalog, chat/event stream, and run context. It already provides plan seams such as `planChatSubmission`, `selectSession`, `planCancelRunningRun`, and `applyRunStatus` that should be preserved or extended.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java` and `AgentCard.java` — Existing Agent Catalog and Agent card rendering from `/api/agents`, with `data-agent-id`, `data-action="choose-agent"`, and entry action hooks that can become stacked mobile cards.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` — Existing stream container, TextArea (`data-role="chat-input"`), and Send button (`data-action="send-chat"`). Current normal-flow composer is a known Phase 12 hotspot.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java` — Existing recent/selected session rows with `role="button"`; should become touch-safe active-session cards/rows with real mobile flow behavior.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java` — Existing run status and Cancel button (`data-action="cancel-run"`) with cancellable state visibility; likely source for composer/shell cancel wiring.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java`, `ToolCallCard.java`, `ApprovalCard.java`, and `ApprovalPanel.java` — Existing event/tool/approval renderers to keep reachable in the feed while deferring detailed card redesign to Phase 13.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` — Vaadin-side public REST path/DTO boundary for agents, sessions, runs, events, cancellation, and approvals.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/EventStreamClient.java` — SSE connection spec builder for `/api/sessions/{sessionId}/runs/{runId}/stream` with replay support.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java`, `PiPageHeader.java`, `PiPageSection.java`, and `PiRouteNavRegistry.java` — Shared shell, page primitives, route registry, status/action slots, and layout hooks created in Phase 11.
- `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` — Project-owned responsive theme with tap-target token, focus-visible rules, overflow/wrap defaults, safe-area support, and existing console breakpoint hooks.
- `e2e/fixtures/mobile-smoke.ts` — Reusable no-overflow, tap-target, focus-visible, stable selector, and route baseline helpers.
- `e2e/fixtures/fake-runtime.ts` — Existing deterministic fake runtime helpers for createRun, cancelRun, decideApproval, waitForTerminal, listEvents, and dev headers.
- `e2e/phase-05-web-console.spec.ts`, `e2e/phase-10-mobile-route-smoke.spec.ts`, and `e2e/phase-11-shell-navigation.spec.ts` — Existing Console product flow, route smoke, and shell/navigation patterns to extend rather than duplicate.

### Established Patterns
- UI work belongs in `pi-agent-adapter-web`; Domain/App/public DTOs must stay free of Vaadin, Playwright, responsive theme, and viewport-specific concerns.
- Production frontend remains Vaadin Java/Flow plus `pi-mobile` theme; TypeScript Playwright is accepted only as browser test tooling.
- Stable `data-*` selectors are the preferred contract for dense Vaadin UI tests, supplemented by role/accessibility selectors for interactive controls.
- Browser E2E should be no-key/deterministic and rely on fake runtime/test fixtures instead of real model providers or external services.
- Phase 13 owns final runtime cards, tool cards, approval cards, dense details, dialogs, and safe detail expansion; Phase 12 owns their placement/reachability within the Console flow.

### Integration Points
- Extend `ConsoleView`/console components so phone layout becomes Chat-first with secondary Agents/Sessions/Run panels and wider responsive multi-column behavior.
- Use Phase 11 shell/page status/action slots for backup active-run status and Cancel affordances where practical.
- Extend `pi-mobile/styles.css` for segmented Console controls, sticky composer, bounded TextArea behavior, feed scroll containment, responsive Console grid, and mobile panel behavior.
- Add or update fast Java contract tests for Console mobile hooks/CSS contracts alongside existing `WebMobileBaselineContractTest`, `WebResponsiveShellContractTest`, and Console component tests.
- Add `e2e/phase-12-console-mobile-flow.spec.ts` or equivalent, reusing Playwright mobile projects and fake-runtime/mobile-smoke helpers.

</code_context>

<specifics>
## Specific Ideas

- User selected all proposed Phase 12 discussion areas: Chat-first layout, composer/run controls, Catalog/Session flow, and mobile E2E scope.
- User chose Chat Feed as the phone default view.
- User chose an in-page segmented switcher for Agents, Sessions, and Run Context.
- User allowed either overlay/sheet or inline/down-push secondary panels as long as Chat state is preserved.
- User chose responsive multi-column behavior for tablet/desktop rather than all-size single-column or phone-only changes.
- User chose bottom sticky composer, inline run state near composer, dual-position Cancel, and bounded multi-line TextArea growth.
- User chose stacked mobile Agent cards, prominent General Agent Start/Continue CTA, session cards with summary/status/time, and automatic return to Chat after selecting a session.
- User chose a full mobile Console E2E loop with representative browser matrix, meaningful streamed event assertions, preserved composer/cancel access while scrolling, and desktop Console regression preservation.

</specifics>

<deferred>
## Deferred Ideas

- Full runtime timeline event card redesign, tool card detail redesign, approval risk-first card redesign, dialogs/drawers/notification confirmation UX, and dense redacted detail expansion — Phase 13.
- Full Admin Governance mobile card/detail conversion across overview, registry, operations, MCP, plugin, extension, policy, and audit — Phase 14.
- Broad orientation/cross-browser/accessibility hardening, final desktop/mobile regression expansion, and real-device/UAT release documentation — Phase 15.
- Native app, PWA/offline behavior, push/background monitoring, deep-linkable mobile incident triage, event filtering/copy/share, and new mobile-only Agent capabilities — future/out of scope for v1.1 unless a later roadmap adds them.

</deferred>

---

*Phase: 12-console-mobile-first-flow*
*Context gathered: 2026-06-23*
