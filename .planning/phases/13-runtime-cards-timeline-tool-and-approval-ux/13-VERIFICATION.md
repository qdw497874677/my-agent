---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
verified: 2026-06-24T06:45:00Z
status: gaps_found
score: 4/5 must-haves verified
gaps:
  - truth: "Mobile user can approve or reject a pending tool approval from a risk-first card that clearly shows side-effect context and requires intentional action."
    status: failed
    reason: "ApprovalCard renders Approve/Reject buttons and can create DecisionPlan objects in tests, but the buttons are not wired with click listeners or any transport execution path, so a mobile user tapping them cannot actually approve or reject."
    artifacts:
      - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java"
        issue: "Approve/Reject buttons only set data attributes; no addClickListener or backend decision call is attached."
      - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java"
        issue: "Renders ApprovalCard instances but provides no action callback/transport wiring for approval decisions."
      - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java"
        issue: "Exposes testable planApprove/planReject helpers, but rendered ADMIN ApprovalCards still have no click-action wiring."
      - path: "e2e/phase-13-runtime-cards.spec.ts"
        issue: "Browser gate checks visibility/tap target/focus for approval controls, but does not click approve/reject or verify decision feedback/API invocation."
    missing:
      - "Wire Approve and Reject button click handlers to execute the approval decision path or a provided decision callback."
      - "Update card feedback after decision attempt/success/failure so the user sees the result of intentional action."
      - "Add Java/component and browser assertions that clicking Approve/Reject invokes the decision flow, not just that buttons are visible."
---

# Phase 13: Runtime Cards, Timeline, Tool, and Approval UX Verification Report

**Phase Goal:** Mobile users can safely inspect run timelines, tool execution, policy/audit-like details, approvals, and viewport-fitting dialogs without raw sensitive payload exposure or horizontal overflow.
**Verified:** 2026-06-24T06:45:00Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Mobile user can inspect run timeline events as compact cards/accordions showing status, timestamp/type, summary, and expandable details. | ✓ VERIFIED | `RunEventRenderer.render(...)` routes model/status/policy/terminal/generic branches to `RuntimeEventCard.from(...)`; `RuntimeEventCard` renders `summaryText`, `Details`, and `Advanced redacted detail` with `data-event-category`, `data-event-type`, `data-event-status`, and `data-expandable`. Covered by `WebConsoleRuntimeCardsTest`. |
| 2 | Mobile user can inspect tool cards with tool name, source, status, policy/approval state, duration, error, and redacted input/output summaries. | ✓ VERIFIED | `ToolCallCard.from(...)` builds visible labels `Tool`, `Source`, `Status`, `Policy`, `Approval`, `Duration`, `Error`, `Summary`, plus structured `Input / output summary` and advanced details. `RunEventRenderer` keeps `ToolCallCard.from(event)` for tool lifecycle events. |
| 3 | Mobile user can expand dense run/tool/policy/audit details without exposing raw sensitive payloads or causing page-level horizontal overflow. | ✓ VERIFIED | `RuntimeDetailRedactor` redacts API keys/password/secret/token/authorization/bearer/`sk-live-*`/`raw-token-value`; runtime/tool/approval cards call it for payload-derived text. `styles.css` applies `max-width: 100%`, `min-width: 0`, `overflow-wrap: anywhere`, and `word-break: break-word` to runtime/tool/approval/detail selectors. Java tests assert raw sensitive markers are absent. |
| 4 | Mobile user can approve or reject a pending tool approval from a risk-first card that clearly shows side-effect context and requires intentional action. | ✗ FAILED | Risk-first display exists, but the actual action is unwired: `ApprovalCard` creates Approve/Reject buttons with data attributes but no `addClickListener` or backend decision execution. Only `planApprove/planReject` helpers create a `DecisionPlan`; tapping buttons cannot call them. |
| 5 | Mobile user sees dialogs, drawers, notifications, and confirmations fit the viewport with safe scrolling and explicit close/action controls. | ✓ VERIFIED | Phase 13 intentionally adds no new `Dialog`, `ConfirmDialog`, `Notification`, `MenuBar`, or `ContextMenu` primitives; confirmations are inline card/details/action rows. CSS covers inline approval action wrapping/tap targets. Existing shell/drawer behavior is outside the new card implementation and remains governed by earlier phases. |

**Score:** 4/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java` | Reusable conservative redaction and bounded formatting | ✓ VERIFIED | Exists, substantive, used by runtime/tool/approval cards. Redacts required sensitive markers and bounds long diagnostics. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeEventCard.java` | Compact timeline-style card for non-tool events | ✓ VERIFIED | Exists, substantive, wired through `RunEventRenderer.runtimeEvent(...)`. Provides summary and Details layers. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java` | Existing renderer seam enhanced for card components | ✓ VERIFIED | Preserves `RenderedEvent` and routes non-tool branches to `RuntimeEventCard`, tools to `ToolCallCard`, approvals to `ApprovalCard`. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java` | Structured mobile tool execution card | ✓ VERIFIED | Exists, substantive, wired via `RunEventRenderer`; visible fields and redacted details implemented. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java` | Risk-first reusable approval card | ⚠️ HOLLOW ACTION | Display and `DecisionPlan` construction are substantive, but rendered buttons do not invoke the decision plan or backend. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java` | Console pending-approval container using ApprovalCard | ⚠️ PARTIAL | Renders USER cards from approval DTOs; no action wiring around card button clicks. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java` | Admin approval queue reusing ApprovalCard with ADMIN role | ⚠️ PARTIAL | Reuses `ApprovalCard` with ADMIN and exposes helper methods for tests; UI button clicks remain unwired. |
| `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` | Mobile card/detail/action overflow, tap, and focus styling | ✓ VERIFIED | Contains Phase 13 runtime/tool/approval selectors, wrapping, max-width/min-width, redacted JSON, detail-layer, and inline approval action tap target rules. |
| `e2e/phase-13-runtime-cards.spec.ts` | Representative Console runtime-card mobile browser gate | ⚠️ PARTIAL | Exists and covers selectors/overflow/details/control visibility, but misses the approval-click behavior required by MCARD-04. |
| `docs/phase-13-runtime-cards.md` | Selector, verification, and handoff documentation | ✓ VERIFIED | Documents MCARD traceability, selector contract, redaction, approval UX, commands, CI gaps, and handoffs. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `RunEventRenderer.render(RunEventDto)` | `RuntimeEventCard.from(...)` | `runtimeEvent(...)` returns `RenderedEvent(..., card)` | ✓ WIRED | Non-tool model/status/policy/terminal/generic events get `RuntimeEventCard` components. |
| `RuntimeEventCard` | `RuntimeDetailRedactor` | summary/details/advanced detail formatting | ✓ WIRED | Uses `shorten`, `stringify`, and `boundedStringify`. |
| `RunEventRenderer.java` | `ToolCallCard.from(event)` | tool lifecycle branch | ✓ WIRED | Tool schema/type branch returns `RenderedEvent("tool", ..., card)`. |
| `ToolCallCard.java` | `RuntimeDetailRedactor.java` | payload-derived summary/details | ✓ WIRED | Uses `RuntimeDetailRedactor.stringify`/`redact` for all helper rendering paths. |
| `ApprovalCard.planApprove/planReject` | `ConsoleHttpClient.approvalDecisionPath` | `DecisionPlan` path/request | ⚠️ PARTIAL | Helper methods build correct paths/requests, but no UI click path calls them. |
| `ApprovalCard` buttons | Approval decision backend | expected click listener/action callback | ✗ NOT_WIRED | Static scan found no `addClickListener` in `ApprovalCard`; buttons only have data attributes. |
| `AdminApprovalQueueView` | `ApprovalCard` | ADMIN actor role construction | ✓ WIRED DISPLAY / ⚠️ PARTIAL ACTION | Cards are constructed with `ADMIN`, but same missing click-action wiring applies. |
| `e2e/phase-13-runtime-cards.spec.ts` | Console event feed | `[data-role="event-feed"]`, `[data-event-category]` | ✓ WIRED | Spec targets stable feed/card selectors and imports mobile helpers. |
| `styles.css` | Runtime/tool/approval card classes | `.pi-runtime-event-card`, `.pi-tool-call-card`, `.pi-approval-card` | ✓ WIRED | CSS selectors match Java card classes. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| --- | --- | --- | --- | --- |
| `RuntimeEventCard` | `RunEventDto event`, `payload`, `summary` | `RunEventRenderer.render(...)` receives public runtime event DTOs from existing feed/replay path | Yes | ✓ FLOWING |
| `ToolCallCard` | `RunEventDto.payload` fields | `RunEventRenderer` tool lifecycle branch | Yes | ✓ FLOWING |
| `ApprovalCard` | `ApprovalSummaryDto approval` | `RunEventRenderer.toApprovalSummary(...)`, `ApprovalPanel.showApprovals(...)`, `AdminApprovalQueueView.showPendingApprovals(...)` | Display data flows; action data does not execute | ⚠️ HOLLOW ACTION |
| `styles.css` | Card/detail classes and data selectors | Java components emit matching classes/attributes | Yes | ✓ FLOWING |
| `e2e/phase-13-runtime-cards.spec.ts` | Runtime card DOM from Console event feed | `/console` fake-runtime prompt and stable selectors | Listed successfully; full execution not rerun here | ? PARTIAL |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Java card/renderer/approval contracts | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest,WebConsoleMobileFlowContractTest test` | User-provided validation: PASSED, 48 tests | ✓ PASS |
| Playwright Phase 13 list/discovery gate | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome" --list` | User-provided validation: PASSED, 1 test listed | ✓ PASS |
| Approval card click performs decision | Static scan for `ApprovalCard` click listener/action execution | No `addClickListener` or transport execution found in `ApprovalCard`; only `planApprove/planReject` helper methods | ✗ FAIL |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| MCARD-01 | Plans 01, 04 | Mobile user can inspect run timeline events as compact cards/accordions with status, timestamp/type, summary, and expandable details. | ✓ SATISFIED | `RuntimeEventCard`, `RunEventRenderer`, tests, CSS, and docs implement compact timeline cards in `[data-role="event-feed"]`. |
| MCARD-02 | Plans 02, 04 | Mobile user can inspect tool cards with tool name, source, status, policy/approval state, duration, error, and redacted input/output summaries. | ✓ SATISFIED | `ToolCallCard` visible summary labels, redacted Details layers, renderer tool branch, Java tests. |
| MCARD-03 | Plans 01, 02, 03, 04 | Mobile user can expand dense details without raw sensitive payloads or page-level horizontal overflow. | ✓ SATISFIED | `RuntimeDetailRedactor`, redaction tests, `data-detail-layer`, and `styles.css` overflow wrapping rules. |
| MCARD-04 | Plan 03, 04 | Mobile user can approve or reject a pending tool approval from a risk-first card showing side-effect context and requiring intentional action. | ✗ BLOCKED | Risk-first card and controls exist, but Approve/Reject controls do not execute the decision. |
| MCARD-05 | Plans 01, 03, 04 | Mobile user sees dialogs, drawers, notifications, and confirmations fit viewport with safe scrolling and explicit close/action controls. | ✓ SATISFIED WITH INLINE SCOPE | No new modal primitives introduced; inline card/details/action-row CSS handles viewport safety. Phase 15 still owns broader real-device/orientation/accessibility hardening. |

**Requirement accounting:** All IDs requested by the user and declared in PLAN frontmatter are accounted for: MCARD-01, MCARD-02, MCARD-03, MCARD-04, MCARD-05. REQUIREMENTS.md maps all five to Phase 13; no orphaned Phase 13 MCARD requirements found.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `ApprovalCard.java` | 55-64 | Buttons are created with labels/data attributes only | 🛑 Blocker | User-visible Approve/Reject controls do not perform approval/rejection. |
| `ApprovalCard.java` | 79-120 | Decision logic is only exposed as helper methods | 🛑 Blocker | Tests can plan decisions, but UI cannot trigger them. |
| `e2e/phase-13-runtime-cards.spec.ts` | 65-80 | Approval controls verified for visibility/tap/focus only | ⚠️ Warning | Browser gate can pass even with unwired approval actions. |

No TODO/FIXME/placeholder/new modal primitive patterns were found in the verified Phase 13 UI files. Null/empty fallbacks in card renderers are defensive display defaults, not blocking stubs.

### Human Verification Required

No additional human-only checks are required before gap closure because the blocking failure is statically verifiable. After fixing the approval click wiring, a human/mobile smoke pass should still confirm final touch behavior if desired.

### Gaps Summary

Phase 13 substantially achieves runtime cards, tool cards, redaction, dense-detail wrapping, selector documentation, and representative test discovery. The remaining blocker is MCARD-04 action wiring: `ApprovalCard` presents a risk-first approval UI, but tapping Approve or Reject cannot approve/reject because the buttons have no click listeners or backend decision invocation. This means the phase goal is not fully achieved even though the visual card and DecisionPlan helper tests pass.

---

_Verified: 2026-06-24T06:45:00Z_
_Verifier: the agent (gsd-verifier)_
