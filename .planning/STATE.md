---
gsd_state_version: 1.0
milestone: v1.2
milestone_name: Console 对话产品化
status: verifying
stopped_at: Phase 18 context gathered
last_updated: "2026-06-29T13:55:52.807Z"
last_activity: 2026-06-29
progress:
  total_phases: 6
  completed_phases: 2
  total_plans: 10
  completed_plans: 10
  percent: 0
---

# Project State: Pi Java Agent Platform

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-06-28)

**Core value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。  
**Current focus:** Phase 17 — console-session-restore-ux

## Current Position

Phase: 17 (console-session-restore-ux) — EXECUTING
Plan: 4 of 4
Status: Phase complete — ready for verification
Last activity: 2026-06-29

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**

- Total plans completed in v1.1: 3
- Average duration: N/A
- Total execution time in v1.1: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 16. Conversation Read Model and Recent Sessions | 0/TBD | - | - |
| 17. Console Session Restore UX | 0/TBD | - | - |
| 18. Streaming Bubble Lifecycle | 0/TBD | - | - |
| 19. Multi-Turn Runtime Context | 0/TBD | - | - |
| 20. Provider/Model and Local Profile Stability | 0/TBD | - | - |
| 21. Verification, Security, and Regression Hardening | 0/TBD | - | - |

**Recent Trend:**

- Last 5 plans: none in v1.1
- Trend: N/A

| Phase 17-console-session-restore-ux P02 | 9min | 2 tasks | 2 files |
| Phase 17-console-session-restore-ux P01 | 9min | 2 tasks | 4 files |
| Phase 17-console-session-restore-ux P03 | 9min | 3 tasks | 3 files |
| Phase 17-console-session-restore-ux P04 | 10m55s | 3 tasks | 8 files |
| Phase 17-console-session-restore-ux P05 | 5m15s | 2 tasks | 4 files |
| Phase 17-console-session-restore-ux P06 | 4m30s | 2 tasks | 2 files |

### v1.2 Roadmap

| Phase | Goal |
|-------|------|
| 16 | Conversation Read Model and Recent Sessions |
| 17 | Console Session Restore UX |
| 18 | Streaming Bubble Lifecycle |
| 19 | Multi-Turn Runtime Context |
| 20 | Provider/Model and Local Profile Stability |
| 21 | Verification, Security, and Regression Hardening |

| Phase 10-responsive-baseline-and-mobile-test-harness P02 | 5min | 3 tasks | 4 files |
| Phase 10-responsive-baseline-and-mobile-test-harness P01 | 364 | 3 tasks | 5 files |
| Phase 10-responsive-baseline-and-mobile-test-harness P03 | 37m09s | 3 tasks | 5 files |
| Phase 11-shared-responsive-shell-and-navigation P01 | 6min | 3 tasks | 14 files |
| Phase 11-shared-responsive-shell-and-navigation P02 | 4min | 3 tasks | 4 files |
| Phase 11-shared-responsive-shell-and-navigation P03 | 6min | 3 tasks | 4 files |
| Phase 12-console-mobile-first-flow P01 | 8m32s | 2 tasks | 6 files |
| Phase 12-console-mobile-first-flow P02 | 5m42s | 2 tasks | 5 files |
| Phase 12-console-mobile-first-flow P03 | 11m20s | 2 tasks | 5 files |
| Phase 12-console-mobile-first-flow P04 | 12min | 3 tasks | 7 files |
| Phase 12-console-mobile-first-flow P05 | 14min | 3 tasks | 6 files |
| Phase 12-console-mobile-first-flow P06 | 9m03s | 3 tasks | 5 files |
| Phase 13-runtime-cards-timeline-tool-and-approval-ux P01 | 9min | 2 tasks | 4 files |
| Phase 13-runtime-cards-timeline-tool-and-approval-ux P02 | 11m09s | 2 tasks | 4 files |
| Phase 13-runtime-cards-timeline-tool-and-approval-ux P03 | 5min | 2 tasks | 2 files |
| Phase 13-runtime-cards-timeline-tool-and-approval-ux P04 | 9m08s | 3 tasks | 5 files |
| Phase 13-runtime-cards-timeline-tool-and-approval-ux P05 | 9m38s | 2 tasks | 9 files |
| Phase 14-admin-governance-full-site-mobile-coverage P01 | 9min | 3 tasks | 6 files |
| Phase 14-admin-governance-full-site-mobile-coverage P03 | 4m55s | 1 tasks | 3 files |
| Phase 14-admin-governance-full-site-mobile-coverage P02 | 16m36s | 2 tasks | 4 files |
| Phase 14-admin-governance-full-site-mobile-coverage P04 | 17m02s | 2 tasks | 3 files |
| Phase 14-admin-governance-full-site-mobile-coverage P05 | 5min | 2 tasks | 2 files |
| Phase 15-cross-browser-orientation-accessibility-and-release-hardening P01 | 4m30s | 3 tasks | 2 files |
| Phase 15-cross-browser-orientation-accessibility-and-release-hardening P02 | 6m59s | 3 tasks | 1 files |
| Phase 15-cross-browser-orientation-accessibility-and-release-hardening P03 | 9m | 3 tasks | 3 files |
| Phase 15-cross-browser-orientation-accessibility-and-release-hardening P04 | 6m30s | 3 tasks | 2 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table. Recent decisions affecting current work:

- [Milestone v1.2]: Continue phase numbering after v1.1; new milestone starts at Phase 16.
- [Milestone v1.2]: Scope is Console conversation productization: Kimi-style chat IA, recent sessions, transcript restore, streaming assistant bubbles, multi-turn context, provider/local stability, and verification.
- [Milestone v1.2]: Preserve Java/Vaadin/COLA boundaries; add typed conversation read models and App-layer context assembly rather than Vaadin-only history or mobile-only APIs.

- [Milestone v1.1]: Continue phase numbering after v1.0; new milestone starts at Phase 10.
- [Milestone v1.1]: Keep 6 phases for full-site mobile H5 support.
- [Milestone v1.1]: Scope is existing Vaadin Web Console/Admin behavior plus verification; no new frontend stack, native app, mobile-only backend fork, or runtime capability expansion.
- [Milestone v1.1]: Preserve Java/Vaadin-first UI, public REST/SSE DTO boundaries, and adapter-web focused implementation.
- [Phase 10-responsive-baseline-and-mobile-test-harness]: Represent Mobile Firefox with Firefox engine plus mobile viewport/touch/user-agent flags because Playwright CI does not provide true Firefox for Android.
- [Phase 10-responsive-baseline-and-mobile-test-harness]: Use WebKit iPhone emulation as the Phase 10 Mobile Safari proxy and defer real-device Safari UAT to Phase 15.
- [Phase 10-responsive-baseline-and-mobile-test-harness]: Use a project-owned Vaadin Flow theme named pi-mobile as the Phase 10 responsive baseline owner.
- [Phase 10-responsive-baseline-and-mobile-test-harness]: Keep Phase 10 root hooks additive through stable data attributes; do not redesign Console/Admin layout in plan 10-01.
- [Phase 10-responsive-baseline-and-mobile-test-harness]: Keep Phase 10 route smoke focused on route load, stable selectors, primary content/actions, no page-level overflow, and one deterministic non-mutating interaction per route category.
- [Phase 10-responsive-baseline-and-mobile-test-harness]: Run the Vaadin E2E server from the adapter-web module base in development mode so the project-owned pi-mobile theme is discoverable without requiring a production frontend bundle in this local harness.
- [Phase 10-responsive-baseline-and-mobile-test-harness]: Limit CSS changes to targeted baseline overflow hotspots; final navigation, Console flow, runtime-card, approval UX, and Admin card/table migrations remain deferred.
- [Phase 11-shared-responsive-shell-and-navigation]: Use PiResponsiveShell as the single RouterLayout for Console and Admin Governance visual chrome.
- [Phase 11-shared-responsive-shell-and-navigation]: Keep route/navigation metadata in PiRouteNavRegistry so Java shell and browser tests share route truth.
- [Phase 11-shared-responsive-shell-and-navigation]: Apply touch/focus behavior through additive pi-mobile CSS and stable data hooks, not per-view inline styles.
- [Phase 11-shared-responsive-shell-and-navigation]: Keep Phase 11 browser gate deterministic/no-key and non-mutating.
- [Phase 12-console-mobile-first-flow]: Use route-local Vaadin panel state and segmented Buttons instead of new Console routes or mobile-only APIs.
- [Phase 12-console-mobile-first-flow]: Keep desktop three-column selectors and columnOrder while using mobile-only active panel hiding at phone breakpoints.
- [Phase 12-console-mobile-first-flow]: Expose mobile Agent/Session behavior through additive data attributes and CSS hooks rather than changing public REST/SSE DTOs.
- [Phase 12-console-mobile-first-flow]: Keep feed/composer behavior inside existing Vaadin Console components with additive wrappers and data hooks, not new routes or API contracts.
- [Phase 12-console-mobile-first-flow]: Use RunContextPanel as the backup cancel/status surface because shell private action slots are not required for D-08 in this plan.
- [Phase 12-console-mobile-first-flow]: Preserve public REST/SSE path behavior while synchronizing run state through existing ConsoleView seams.
- [Phase 12-console-mobile-first-flow]: Keep MVER-03 browser assertions on stable data-* selectors and tolerant cancel-or-terminal behavior to avoid Vaadin shadow DOM and fake-runtime timing brittleness.
- [Phase 12-console-mobile-first-flow]: Document Phase 13 runtime/tool/approval card interiors and Phase 15 real-device/accessibility hardening as explicit downstream handoffs.
- [Phase 12-console-mobile-first-flow]: Use route-local Vaadin callbacks instead of new routes or mobile-only APIs for Agent, Session, Send, and Cancel actions.
- [Phase 12-console-mobile-first-flow]: Initialize Console Agent Catalog from the existing AgentCatalogQueryService read model, keeping Vaadin concerns in adapter-web.
- [Phase 12-console-mobile-first-flow]: Keep historical Session population limited to existing/current flow seams; preserve explicit empty state until a history/list read model supplies rows.
- [Phase 12-console-mobile-first-flow]: Use an adapter-web ConsoleRunExecutionBridge so Vaadin UI code can call existing App-layer session/run/query use cases without introducing mobile-only APIs.
- [Phase 12-console-mobile-first-flow]: Keep a safe direct-construction demo bridge for no-Spring component tests while Spring construction delegates to AppConsoleRunExecutionBridge.
- [Phase 12-console-mobile-first-flow]: Treat terminal cancellation races as acceptable mobile feedback and harden double-click/no-active-run UI handlers to show status instead of throwing.
- [Phase 12-console-mobile-first-flow]: Use a Vaadin poll-backed bounded replay hook in adapter-web rather than new mobile-only REST/SSE APIs.
- [Phase 12-console-mobile-first-flow]: Make MVER-03 fail on empty-session fallbacks by requiring an active [data-role=session-card].
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Runtime cards stay inside RunEventRenderer and ChatEventStreamPanel appendEvent seams instead of adding a standalone timeline route.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Sensitive runtime diagnostics use a package-private adapter-web redaction utility for conservative mobile detail rendering.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Tool lifecycle rendering remains one ToolCallCard per runtime event; no toolCallId aggregation cache was introduced.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Tool payload-derived summary, structured detail, and advanced diagnostics now reuse RuntimeDetailRedactor rather than local ad-hoc redaction.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Keep approval decisions inline in ApprovalCard with no Dialog, ConfirmDialog, Notification, MenuBar, or ContextMenu confirmation layer.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Reuse the same ApprovalCard constructor for Console USER and Admin ADMIN roles while preserving public approval decision API paths.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Render approval preview, arguments, and eligible roles through RuntimeDetailRedactor so mobile summaries stay conservative.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Keep Phase 13 browser coverage as a representative Console runtime-card matrix using stable data-* selectors and fake-runtime hints.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Document full runtime/tool/approval selector and redaction contracts for Phase 14/15 handoff rather than introducing new mobile-only APIs or modal primitives.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Keep approval decision execution in adapter-web via ApprovalDecisionHandler/AppApprovalDecisionHandler instead of adding mobile-only REST DTOs or Vaadin dependencies outside the adapter layer.
- [Phase 13-runtime-cards-timeline-tool-and-approval-ux]: Preserve deterministic demo constructors with ApprovalDecisionHandler.demo() while Spring constructors bridge to ApprovalCommandService for live Console/Admin cards.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Keep Admin mobile primitives package-local inside adapter-web/admin so Vaadin/mobile concerns do not leak into App/Domain/client DTO contracts.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Render Governance Overview summaries as label/value card fields plus status chips while preserving renderedText() semantic compatibility for existing tests.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Treat metadata as structured collapsed Details and redact sensitive keys and values before rendering.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Operations metrics use AdminMobileCardSupport metric/detail primitives while preserving renderedText() semantic compatibility.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Operations warning and error discoverability is represented through data-status-severity hooks for abnormal values.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Keep Registry/MCP/Plugin/Extension mobile conversion inside adapter-web Vaadin components with no public Admin DTO or route changes.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Prioritize operator-relevant MCP and Plugin entries by abnormal status/error/reason before normal cards.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Preserve renderedText() semantic compatibility while moving visible summaries to label rows, chips, cards, and collapsed Details.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Preserve Policy/Audit renderedText and contextLinks semantics while converting visible Vaadin content to mobile card/detail layouts.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Keep Policy/Audit redacted context collapsed by default with data-admin-details values policy-context and audit-details.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Use AdminMobileRedactor plus sensitive-key handling for Policy/Audit detail keys and values before rendering.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Keep MVER-04 deterministic by using Playwright --list as the local no-key browser gate.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Use existing Admin public routes and stable data-* hooks for coverage rather than adding mobile-only routes or screenshot assertions.
- [Phase 14-admin-governance-full-site-mobile-coverage]: Document real-device/UAT, cross-browser/orientation hardening, and broader regression expansion as Phase 15 handoffs.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Use reusable named viewport cases in mobile-smoke.ts so future Phase 15 gates share portrait, landscape, and tablet dimensions.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Keep landscape coverage inside the existing Playwright project matrix with page.setViewportSize rather than adding dedicated landscape projects.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Assert structural shell, route, nav, critical control, and no-overflow contracts instead of screenshot visual baselines.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Keep Phase 15 critical-flow coverage in a new release spec rather than modifying Phase 05 desktop baseline or prior mobile phase specs.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Use stable data-* selectors and fake-runtime hints for Console run/runtime coverage; no screenshots or backend/API changes were introduced.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Represent desktop release confidence as a compact Console plus Admin route summary under chromium while preserving detailed desktop Phase 05 tests unchanged.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Keep Phase 15 accessibility checks representative and deterministic instead of introducing mandatory axe-core audits.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Keep accessibility hardening in the pi-mobile presentation layer and static/browser tests, with no backend DTO or API changes.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Use a bounded 641px-899px tablet bridge media query before the existing 900px desktop shell breakpoint.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Treat Phase 15 automated Playwright Mobile Safari and Mobile Firefox projects as proxies, not proof of true-device Safari/Firefox mobile behavior.
- [Phase 15-cross-browser-orientation-accessibility-and-release-hardening]: Use blocker, known limitation, and follow-up classifications as the release go/no-go vocabulary for unrun or failed true-device validation.
- [Phase 17-console-session-restore-ux]: Promoted replaceTranscriptForProof(...) to a compatibility delegator over formal replaceTranscript(...).
- [Phase 17-console-session-restore-ux]: Kept tool/error transcript items visible as compact secondary Vaadin cards instead of primary assistant prose.
- [Phase 17-console-session-restore-ux]: Mapped data-stream-state directly to persisted message status; live pending/delta/terminal mutation remains Phase 18.
- [Phase 17-console-session-restore-ux]: Recent history cards consume SessionSummaryDto directly in adapter-web and do not render provider/model metadata by default.
- [Phase 17-console-session-restore-ux]: Represent hasMore with lightweight data-role=session-more only; full history management/search UX remains deferred.
- [Phase 17-console-session-restore-ux]: Use adapter-web ConsoleView state for active session identity while preserving App/read-model access through ConsoleRunExecutionBridge.
- [Phase 17-console-session-restore-ux]: Treat New Conversation as the explicit reset path for selected-session continuation.
- [Phase 17-console-session-restore-ux]: Restore activeRunId and numeric transcript nextCursor only as a Phase 17 polling cursor seam; full delta aggregation remains Phase 18.
- [Phase 17-console-session-restore-ux]: Use Playwright --list as the no-key CI-safe product-path gate while encoding the live browser restore flow for server-backed runs.
- [Phase 17-console-session-restore-ux]: Keep Phase 17 transcript restore selector assertions focused on persisted message status; live delta mutation remains Phase 18.
- [Phase 17-console-session-restore-ux]: Document search/rename/archive/pin/delete and localStorage-only history as explicitly deferred rather than adding copy or UI affordances.
- [Phase 17-console-session-restore-ux]: Expose the existing Console panel switcher as the visible history/details affordance instead of adding routes or deferred conversation-management features.
- [Phase 17-console-session-restore-ux]: Normalize abnormal transcript status fallback labels to lowercase for selector/test stability while retaining bundle-backed copy.
- [Phase 17-console-session-restore-ux]: Browser restore tests must assert panel controls are visible and enabled before clicking them.
- [Phase 17-console-session-restore-ux]: Keep the automated Playwright plan gate as --list/no-key while documenting a separate live-server command for intentional browser execution.

### Pending Todos

None yet.

### Blockers/Concerns

- Phase 10 should inventory current Vaadin theme/bootstrap and existing Playwright harness shape before adding duplicate browser-test infrastructure.
- Phase 12 should inspect current Console fixed-width, scroll, and composer behavior before implementation planning.
- Phase 15 should document real iOS Safari/Android Chrome UAT gaps if CI cannot run true device browsers.

## Session Continuity

Last session: 2026-06-29T13:55:52.803Z
Stopped at: Phase 18 context gathered
Resume file: .planning/phases/18-streaming-bubble-lifecycle/18-CONTEXT.md

---
*State reset: 2026-06-28 after v1.2 roadmap creation*
