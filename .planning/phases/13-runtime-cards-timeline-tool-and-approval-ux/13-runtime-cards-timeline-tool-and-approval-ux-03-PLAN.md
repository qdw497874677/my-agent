---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
plan: 03
type: execute
wave: 3
depends_on: [13-runtime-cards-timeline-tool-and-approval-ux-01]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java
autonomous: true
requirements: [MCARD-04, MCARD-03, MCARD-05]
must_haves:
  truths:
    - "Mobile user can approve or reject a pending tool approval from an inline risk-first card."
    - "Approval cards show risk, side-effect context, policy reason, expected consequence, provision preview, and redacted argument summary before action."
    - "Approve and Reject remain inline touch-safe actions without a second modal confirmation."
    - "The same ApprovalCard pattern supports Console USER and Admin ADMIN actor roles without converting all Admin pages."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java"
      provides: "Risk-first reusable approval card"
      exports: ["ApprovalCard", "DecisionPlan"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java"
      provides: "Console pending-approval container using ApprovalCard"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java"
      provides: "Admin approval queue reusing ApprovalCard with ADMIN actor role"
  key_links:
    - from: "ApprovalCard.planApprove/planReject"
      to: "ConsoleHttpClient.approvalDecisionPath"
      via: "DecisionPlan path and ApprovalDecisionRequest actorRole"
      pattern: "approvalDecisionPath\\(approval.sessionId\\(\\), approval.runId\\(\\), approval.approvalId\\(\\)\\)"
    - from: "AdminApprovalQueueView"
      to: "ApprovalCard"
      via: "ADMIN actor role construction"
      pattern: "new ApprovalCard\\(.*ADMIN"
---

<objective>
Upgrade approval UX into an inline risk-first mobile approval card shared by Console and Admin approval surfaces.

Purpose: Satisfy MCARD-04 while preserving D-07/D-08/D-09: inline risk-first card, 44px touch-safe Approve/Reject controls, no second dialog, reusable USER/ADMIN behavior without Phase 14 Admin conversion.
Output: Refactored ApprovalCard, minimal container attributes if needed, and Java contracts.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/REQUIREMENTS.md
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-CONTEXT.md
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-RESEARCH.md
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-01-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java

<interfaces>
Preserve existing approval API contract:

```java
public class ApprovalCard extends Div {
    public ApprovalCard(ApprovalSummaryDto approval, ConsoleHttpClient httpClient, String actorRole);
    public static ApprovalCard from(ApprovalSummaryDto approval, ConsoleHttpClient httpClient);
    public DecisionPlan planApprove(String reason);
    public DecisionPlan planReject(String reason);
    public String summaryText();
    public String detailsText();
    public record DecisionPlan(String path, ApprovalDecisionRequest request, String sessionId, String runId, String approvalId, String toolCallId) { }
}
```

Locked decisions implemented here: D-07 inline risk-first card, D-08 no second dialog/long press/multi-step confirmation, D-09 USER and ADMIN reuse, D-12 redacted summaries.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Refactor ApprovalCard into risk-first hierarchy</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java
  </read_first>
  <behavior>
    - Test 1: Approval summary text contains tool name, policy reason, risk label, side-effect label, expected consequence, provision preview summary, redacted argument summary, and `Approve/Reject`.
    - Test 2: Card has stable attributes `data-event-category="approval"`, `data-risk-level`, `data-side-effect`, `data-approval-id`, `data-tool-call-id`, and `data-actor-role`.
    - Test 3: Raw sensitive values in preview/arguments are redacted through `RuntimeDetailRedactor`.
  </behavior>
  <action>Refactor `ApprovalCard` body into visible risk-first sections while preserving public methods and `DecisionPlan`. Add CSS classes `pi-approval-card pi-card`. Visible body must include labels `Approval required`, `Risk`, `Side effect`, `Policy reason`, `Expected consequence`, `Provision preview`, and `Arguments`. Use `RuntimeDetailRedactor.stringify/redact` for `provisionPreview`, `redactedArgumentSummary`, and eligible-role values. Add stable attributes `data-risk-level` and `data-side-effect` from `approval.riskLabel()` and `approval.sideEffectLabel()`. Keep `Details("Decision context", ...)` or replace with structured `Details` as long as default visible body contains risk/action information per Vaadin Details guidance. Do not add Dialog/ConfirmDialog/Notification or second-step confirmation.</action>
  <acceptance_criteria>
    - `ApprovalCard.java` contains labels `Approval required`, `Risk`, `Side effect`, `Policy reason`, `Expected consequence`, `Provision preview`, and `Arguments`.
    - `ApprovalCard.java` contains attributes `data-risk-level` and `data-side-effect`.
    - `ApprovalCard.java` contains `RuntimeDetailRedactor.stringify` or `RuntimeDetailRedactor.redact`.
    - `ApprovalCard.java` does not contain `new Dialog`, `new ConfirmDialog`, `Notification.show`, `new MenuBar`, or `new ContextMenu`.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest test</automated>
  </verify>
  <done>Approval card default view communicates risk and side effects before action, with redacted summaries and stable mobile selectors.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Harden inline approval actions and USER/ADMIN reuse</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java
    - docs/phase-12-console-mobile-flow.md
  </read_first>
  <behavior>
    - Test 1: Approve button has `data-action="approve-tool-call"`, Reject button has `data-action="reject-tool-call"`, both are children of an element with class `pi-action-row` or equivalent action row marker.
    - Test 2: `planApprove("...")` and `planReject("...")` keep original session/run/approval/toolCall IDs and set actorRole `USER` or `ADMIN` based on card construction.
    - Test 3: AdminApprovalQueueView continues to construct approval cards with ADMIN actor role and does not require Admin full-site conversion.
  </behavior>
  <action>Place Approve and Reject buttons inside an action row with class `pi-action-row` and attribute `data-approval-actions="inline"`. Set approve button attributes `data-action="approve-tool-call"` and `data-risk-action="approve"`; set reject button attributes `data-action="reject-tool-call"` and `data-risk-action="reject"`. Set primary/secondary theme names or class names so Approve and Reject are visually distinct, but do not add a dialog. Add status feedback element/attribute `data-role="approval-decision-feedback"` if one is not already browser-visible. Verify `ApprovalPanel` and `AdminApprovalQueueView` still reuse `ApprovalCard` and preserve USER/ADMIN actor role paths.</action>
  <acceptance_criteria>
    - `ApprovalCard.java` contains `data-approval-actions` value `inline`.
    - `ApprovalCard.java` contains `data-risk-action` values `approve` and `reject`.
    - `ApprovalCard.java` contains `data-role` value `approval-decision-feedback`.
    - `WebConsoleApprovalCardsTest.java` asserts USER and ADMIN actor roles in `DecisionPlan` requests.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest,WebConsoleRuntimeCardsTest test</automated>
  </verify>
  <done>Inline approval actions are stable, touch-target-ready, decision-plan-compatible, and shared by Console USER and Admin ADMIN contexts.</done>
</task>

</tasks>

<verification>
Run Java approval contracts. Confirm no new modal primitive imports/usages exist in `ApprovalCard`, `ApprovalPanel`, or `AdminApprovalQueueView`. Confirm Admin changes are limited to ApprovalCard reuse, not broad Phase 14 page conversion.
</verification>

<success_criteria>
MCARD-04 is satisfied through a risk-first inline ApprovalCard; MCARD-03/MCARD-05 safety boundaries are preserved; D-07 through D-09 are traceable in code and tests.
</success_criteria>

<output>
After completion, create `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-03-SUMMARY.md`.
</output>
