---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
plan: 05
type: execute
wave: 5
depends_on: [13-runtime-cards-timeline-tool-and-approval-ux-03, 13-runtime-cards-timeline-tool-and-approval-ux-04]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalDecisionHandler.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppApprovalDecisionHandler.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java
  - e2e/phase-13-runtime-cards.spec.ts
autonomous: true
gap_closure: true
requirements: [MCARD-04]
must_haves:
  truths:
    - "Mobile user tapping Approve invokes the approval decision path."
    - "Mobile user tapping Reject invokes the rejection decision path."
    - "Approval card shows success or failure feedback after a decision attempt."
    - "Console USER and Admin ADMIN cards share click-action wiring while preserving actor role."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalDecisionHandler.java"
      provides: "Approval decision callback for ApprovalCard clicks"
      exports: ["ApprovalDecisionHandler"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppApprovalDecisionHandler.java"
      provides: "Vaadin-to-App approval decision bridge"
      exports: ["AppApprovalDecisionHandler"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java"
      provides: "Risk-first approval card with real click listeners and feedback"
  key_links:
    - from: "ApprovalCard buttons"
      to: "ApprovalDecisionHandler.decide"
      via: "Button.addClickListener calls planApprove/planReject"
      pattern: "addClickListener"
    - from: "AppApprovalDecisionHandler"
      to: "ApprovalCommandService.decide"
      via: "DecisionPlan session/run/approval/request"
      pattern: "approvalCommandService.decide"
---

<objective>
Close the Phase 13 verification gap where approval controls look actionable but do not execute approval decisions.

Purpose: Make MCARD-04 true end-to-end for existing Vaadin approval card surfaces. Tapping Approve or Reject must invoke a decision handler, preserve USER/ADMIN actor roles, and show visible feedback without introducing dialogs, modal confirmations, new REST DTOs, or mobile-only APIs.
Output: Adapter-web approval decision callback contract, App-layer bridge, wired ApprovalCard click listeners, Console/Admin renderer/container wiring, and Java/browser assertions for real click behavior.
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
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-VERIFICATION.md
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-03-SUMMARY.md
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-04-SUMMARY.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/ApprovalController.java
@pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ApprovalCommandService.java
@e2e/phase-13-runtime-cards.spec.ts

<interfaces>
Preserve existing approval API contract:

```java
public class ApprovalCard extends Div {
    public ApprovalCard(ApprovalSummaryDto approval, ConsoleHttpClient httpClient, String actorRole);
    public static ApprovalCard from(ApprovalSummaryDto approval, ConsoleHttpClient httpClient);
    public DecisionPlan planApprove(String reason);
    public DecisionPlan planReject(String reason);
    public String statusFeedback();
    public record DecisionPlan(String path, ApprovalDecisionRequest request, String sessionId, String runId, String approvalId, String toolCallId) { }
}

public interface ApprovalCommandService {
    ApprovalDecisionResponse decide(RequestContext context, String sessionId, String runId, String approvalId,
                                    ApprovalDecisionRequest request);
}
```

Create this adapter-web callback contract:

```java
@FunctionalInterface
public interface ApprovalDecisionHandler {
    ApprovalDecisionResponse decide(ApprovalCard.DecisionPlan plan);
}
```

Locked decisions preserved: D-07 inline risk-first card, D-08 no second dialog/long press/multi-step confirmation, D-09 USER and ADMIN reuse, D-13/D-14 no new modal primitives. Verification gap source: `13-VERIFICATION.md` failed truth #4 because `ApprovalCard` buttons have no click listeners or transport execution path.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Wire ApprovalCard buttons to a decision handler with visible feedback</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalDecisionHandler.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppApprovalDecisionHandler.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/ApprovalController.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ApprovalCommandService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultApprovalService.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java
    - .planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-VERIFICATION.md
  </read_first>
  <behavior>
    - Test 1: Clicking Approve calls `ApprovalDecisionHandler.decide(...)` exactly once with `DecisionPlan.request().decision() == APPROVE`, path `/api/sessions/session-1/runs/run-1/approvals/preview-1/decision`, actorRole `USER`, and original session/run/approval/toolCall IDs.
    - Test 2: Clicking Reject calls `ApprovalDecisionHandler.decide(...)` exactly once with `DecisionPlan.request().decision() == REJECT`, actorRole `USER`, and original IDs.
    - Test 3: After a successful click, `[data-role="approval-decision-feedback"]` contains `APPROVED` or `REJECTED`, card root has `data-decision-state="succeeded"`, and both action buttons are disabled.
    - Test 4: If the handler throws `RuntimeException("boom")`, feedback contains `Decision failed: boom`, root has `data-decision-state="failed"`, and buttons remain enabled.
    - Test 5: `AppApprovalDecisionHandler` delegates to `ApprovalCommandService.decide(...)` with `ROLE_ADMIN` when actorRole is `ADMIN`, otherwise `ROLE_USER`.
  </behavior>
  <action>Create `ApprovalDecisionHandler` in the adapter-web console package. It must expose `ApprovalDecisionResponse decide(ApprovalCard.DecisionPlan plan)` plus `static ApprovalDecisionHandler demo()` returning an `ApprovalDecisionResponse` with status `APPROVED` or `REJECTED`, principal `vaadin-demo`, actorRole from `plan.request().actorRole()`, reason from `plan.request().reason()`, and current `Instant.now()`.

Create `AppApprovalDecisionHandler` in the same package. It must hold `ApprovalCommandService approvalCommandService` and implement `decide(plan)` by constructing a `RequestContext` from `plan.request().actorRole()`: principal id/display `vaadin-approval`, authorities `Set.of("ROLE_ADMIN")` for `ADMIN` otherwise `Set.of("ROLE_USER")`, correlation/source `vaadin-approval`; then call `approvalCommandService.decide(context, plan.sessionId(), plan.runId(), plan.approvalId(), plan.request())`. Keep this bridge in adapter-web per COLA; do not import Vaadin into App/Domain.

Extend `ApprovalCard` with overload `ApprovalCard(ApprovalSummaryDto approval, ConsoleHttpClient httpClient, String actorRole, ApprovalDecisionHandler approvalDecisionHandler)` and make the existing 3-arg constructor delegate to it with `ApprovalDecisionHandler.demo()`. Store feedback `Span` and action `Button`s so click handlers can update them. Add click listeners: Approve executes `executeDecision(planApprove("Approved from mobile approval card"))`; Reject executes `executeDecision(planReject("Rejected from mobile approval card"))`. `executeDecision` must set root `data-decision-state="submitting"`, update feedback to `Submitting approval decision...`, call `approvalDecisionHandler.decide(plan)`, then on success set feedback to `Decision recorded: {response.status()} ({response.decision()})`, root `data-decision-state="succeeded"`, and disable both buttons. On `RuntimeException`, set feedback to `Decision failed: {exception.getMessage()}`, root `data-decision-state="failed"`, and leave buttons enabled. Preserve `DecisionPlan`, `summaryText`, `detailsText`, risk-first fields, `data-action`, `data-risk-action`, and `data-role="approval-decision-feedback"`. Do not add Dialog, ConfirmDialog, Notification, MenuBar, ContextMenu, new REST DTOs, `/mobile/*` APIs, or Domain/App Vaadin dependencies.</action>
  <acceptance_criteria>
    - `ApprovalDecisionHandler.java` exists and contains `ApprovalDecisionResponse decide(ApprovalCard.DecisionPlan plan)`.
    - `ApprovalDecisionHandler.java` contains `static ApprovalDecisionHandler demo()` and strings `APPROVED`, `REJECTED`, `vaadin-demo`.
    - `AppApprovalDecisionHandler.java` contains `ApprovalCommandService approvalCommandService`, `approvalCommandService.decide`, `ROLE_ADMIN`, and `ROLE_USER`.
    - `ApprovalCard.java` contains `ApprovalDecisionHandler approvalDecisionHandler`, at least two `addClickListener` calls, `planApprove("Approved from mobile approval card")`, and `planReject("Rejected from mobile approval card")`.
    - `ApprovalCard.java` contains `data-decision-state`, `submitting`, `succeeded`, `failed`, `Decision recorded:`, and `Decision failed:`.
    - `ApprovalCard.java` does not contain `new Dialog`, `new ConfirmDialog`, `Notification.show`, `new MenuBar`, or `new ContextMenu`.
    - `WebConsoleApprovalCardsTest.java` contains assertions for click-triggered `APPROVE`, click-triggered `REJECT`, `data-decision-state`, and `Decision failed: boom`.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest test</automated>
  </verify>
  <done>ApprovalCard controls are no longer hollow: component tests prove button clicks call a decision handler, update visible feedback, and preserve redacted risk-first UI without modal confirmation.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Connect Console, ApprovalPanel, Admin, and browser assertions to wired decisions</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java, e2e/phase-13-runtime-cards.spec.ts</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalPanel.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AppApprovalDecisionHandler.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java
    - e2e/phase-13-runtime-cards.spec.ts
    - e2e/fixtures/fake-runtime.ts
  </read_first>
  <behavior>
    - Test 1: `RunEventRenderer` created with a recording handler renders approval runtime events into cards whose clicks use that handler.
    - Test 2: `ApprovalPanel(ConsoleHttpClient, String, ApprovalDecisionHandler)` renders USER cards whose clicks use the supplied handler.
    - Test 3: `AdminApprovalQueueView` has a Spring constructor accepting `ApprovalCommandService`, creates `AppApprovalDecisionHandler`, constructs cards with actorRole `ADMIN`, and card clicks preserve `ADMIN` in the decision request.
    - Test 4: `ConsoleView` Spring constructor accepts `ApprovalCommandService` and passes `new AppApprovalDecisionHandler(approvalCommandService)` into `RunEventRenderer`; demo/test constructors remain deterministic.
    - Test 5: `e2e/phase-13-runtime-cards.spec.ts` clicks an approve or reject control and asserts feedback text `Decision recorded:` or `[data-decision-state="succeeded"]`.
  </behavior>
  <action>Thread the new decision handler through all Phase 13 approval renderers and containers. Update `RunEventRenderer` with constructors `RunEventRenderer()`, `RunEventRenderer(ConsoleHttpClient httpClient)`, and `RunEventRenderer(ConsoleHttpClient httpClient, ApprovalDecisionHandler approvalDecisionHandler)`, defaulting the first two to `ApprovalDecisionHandler.demo()`. In the approval-required branch, construct `new ApprovalCard(toApprovalSummary(event, payload), httpClient, "USER", approvalDecisionHandler)` so event-feed approval buttons use the renderer handler.

Update `ApprovalPanel` with overload `ApprovalPanel(ConsoleHttpClient httpClient, String actorRole, ApprovalDecisionHandler approvalDecisionHandler)`, defaulting existing constructors to `ApprovalDecisionHandler.demo()`. In `showApprovals`, construct `new ApprovalCard(approval, httpClient, actorRole, approvalDecisionHandler)`.

Update `ConsoleView` Spring `@Autowired` constructor to accept `ApprovalCommandService approvalCommandService` in addition to existing services. Pass `new RunEventRenderer(new ConsoleHttpClient(), new AppApprovalDecisionHandler(approvalCommandService))` into the main constructor so live Console event-feed cards execute App-layer approval decisions. Preserve existing no-arg/test constructors by using demo handler constructors; do not introduce REST calls from server-side Vaadin.

Update `AdminApprovalQueueView` with an `@Autowired` constructor accepting `ApprovalCommandService approvalCommandService` and delegating to a main constructor with `new ConsoleHttpClient()` and `new AppApprovalDecisionHandler(approvalCommandService)`. Preserve existing no-arg and `ConsoleHttpClient` constructors for tests by defaulting to `ApprovalDecisionHandler.demo()`. In `showPendingApprovals`, construct `new ApprovalCard(approval, httpClient, "ADMIN", approvalDecisionHandler)`.

Extend Java tests with a recording handler class or lambda capturing `DecisionPlan` values and returning `ApprovalDecisionResponse`. Assert renderer/panel/Admin click wiring rather than only `planApprove/planReject`. Update Playwright Phase 13 to click whichever approval control is visible, then assert `data-decision-state="succeeded"` and/or `Decision recorded:`. Keep browser spec deterministic/no-key; do not broaden into Phase 14 Admin conversion or Phase 15 real-device/accessibility hardening.</action>
  <acceptance_criteria>
    - `RunEventRenderer.java` contains `ApprovalDecisionHandler approvalDecisionHandler` and `new ApprovalCard(` with `approvalDecisionHandler` in the approval branch.
    - `ApprovalPanel.java` contains `ApprovalDecisionHandler approvalDecisionHandler` and `new ApprovalCard(approval, httpClient, actorRole, approvalDecisionHandler)`.
    - `ConsoleView.java` references `ApprovalCommandService` and contains `new AppApprovalDecisionHandler(approvalCommandService)`.
    - `AdminApprovalQueueView.java` references `ApprovalCommandService`, contains `new AppApprovalDecisionHandler(approvalCommandService)`, and constructs `new ApprovalCard(approval, httpClient, "ADMIN", approvalDecisionHandler)`.
    - `WebConsoleApprovalCardsTest.java` contains assertions proving renderer, panel, and Admin card clicks invoke a recording handler with the expected actor role.
    - `e2e/phase-13-runtime-cards.spec.ts` contains `.click()` on an approval action and assertion text `Decision recorded:` or selector `[data-decision-state="succeeded"]`.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleApprovalCardsTest,WebConsoleRuntimeCardsTest test</automated>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Console event-feed approvals, ApprovalPanel approvals, and Admin approval queue cards all render with wired click-action decision handlers; browser gate now fails if approval controls are merely visible but not actionable.</done>
</task>

</tasks>

<verification>
Run the Java approval/runtime contract tests and the Phase 13 Playwright list gate. Then verify static absence of `new Dialog`, `new ConfirmDialog`, `Notification.show`, `new MenuBar`, `new ContextMenu`, `/mobile/`, or new client approval DTO records in modified files. Confirm `13-VERIFICATION.md` gap items are explicitly closed: click handlers execute a decision path/callback, feedback updates after decision attempt, and Java/browser assertions click controls.
</verification>

<success_criteria>
MCARD-04 is fully satisfied: tapping Approve or Reject from a mobile risk-first card invokes the approval decision flow or explicit callback, user-visible feedback changes, duplicate clicks are prevented after success, retry remains possible after failure, Console USER and Admin ADMIN roles are preserved, and no deferred modal or mobile-only API work is introduced.
</success_criteria>

<output>
After completion, create `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-05-SUMMARY.md`.
</output>
