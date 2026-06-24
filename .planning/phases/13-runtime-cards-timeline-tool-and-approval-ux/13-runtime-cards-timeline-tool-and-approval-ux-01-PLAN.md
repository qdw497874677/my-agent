---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeEventCard.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java
autonomous: true
requirements: [MCARD-01, MCARD-03, MCARD-05]
must_haves:
  truths:
    - "Mobile user can inspect non-tool run timeline events as compact cards with visible status, timestamp/type, and summary."
    - "Mobile user can expand structured and advanced details without seeing raw API keys, passwords, tokens, or secrets."
    - "Runtime cards stay inside the existing Chat/Event Feed instead of introducing a standalone timeline route."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java"
      provides: "Reusable conservative redaction and bounded text formatting for runtime/tool/approval details"
      exports: ["RuntimeDetailRedactor"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeEventCard.java"
      provides: "Compact timeline-style card for status/model/policy/terminal/generic run events"
      exports: ["RuntimeEventCard"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java"
      provides: "Existing renderer seam enhanced to return RuntimeEventCard components"
      exports: ["RunEventRenderer", "RenderedEvent"]
  key_links:
    - from: "RunEventRenderer.render(RunEventDto)"
      to: "RuntimeEventCard.from(RunEventDto, category, status, summary)"
      via: "component returned in RenderedEvent"
      pattern: "new RenderedEvent\\(\"(model|policy|terminal|status|event)\".*RuntimeEventCard"
    - from: "RuntimeEventCard"
      to: "RuntimeDetailRedactor"
      via: "redacted structured/advanced detail text"
      pattern: "RuntimeDetailRedactor\\."
---

<objective>
Create the Phase 13 foundation for mobile runtime timeline cards inside the existing Console event feed.

Purpose: Satisfy MCARD-01/MCARD-03/MCARD-05 without violating D-01/D-03/D-10/D-13: enhance `RunEventRenderer` and `ChatEventStreamPanel.appendEvent(...)` seams rather than adding a new route, modal, or public DTO.
Output: Reusable redaction utility, generic runtime event card, renderer wiring, and Java contracts.
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
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-VALIDATION.md
@.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-06-SUMMARY.md
@docs/phase-12-console-mobile-flow.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java

<interfaces>
Existing contracts executors must preserve:

```java
// RunEventRenderer.java
public class RunEventRenderer {
    public RenderedEvent render(RunEventDto event);
    public record RenderedEvent(String category, String text, boolean terminal, Component component) { }
}

// ChatEventStreamPanel.java
public void appendEvent(RunEventRenderer.RenderedEvent event) {
    append(event.category(), event.text(), event.component());
}
```

Phase 13 locked decisions implemented here:
- D-01: enhance existing Chat/Event Feed, no standalone RunTimelinePanel or route.
- D-02: default card view shows status, timestamp/type, summary.
- D-03: preserve `RunEventRenderer` → `ChatEventStreamPanel.appendEvent(...)` seam.
- D-10/D-11/D-12: layered detail model with redacted advanced detail only.
- D-13/D-14: no new Dialog/ConfirmDialog/Notification/MenuBar/ContextMenu.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add reusable redaction and runtime-card contracts</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeEventCard.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleApprovalCardsTest.java
  </read_first>
  <behavior>
    - Test 1: A runtime event with `status=RUNNING`, type `run.status`, and timestamp `2026-06-15T05:00:00Z` produces a card whose summary text contains `RUNNING`, `run.status`, `2026-06-15T05:00:00Z`, and a short summary.
    - Test 2: A payload containing `api_key=sk-live-secret`, `password=hunter2`, `token=raw-token-value`, `secret=my-secret`, and a 180-character URL/string returns detail text containing `[REDACTED]` and not containing the raw sensitive values.
    - Test 3: `RuntimeEventCard` exposes stable attributes `data-event-category`, `data-event-type`, `data-event-status`, `data-expandable="true"`, and `data-layered-detail="true"`.
  </behavior>
  <action>Create `RuntimeDetailRedactor` as a package-private/final adapter-web utility with static methods: `redact(String value)`, `stringify(Object value)`, and `shorten(String value, int maxChars)`. Redact case-insensitive markers `api_key=`, `api-key=`, `password=`, `secret=`, `token=`, `authorization=`, `bearer `, `sk-live-`, and `raw-token-value` by replacing the sensitive value with `[REDACTED]`. Create `RuntimeEventCard extends Div` with constructor/factory accepting a `RunEventDto`, category, status, and summary. The default visible body must include status, timestamp/type, and summary per D-02. Add two Vaadin `Details`: summary text `Details` for structured detail and `Advanced redacted detail` for redacted pretty/raw-like diagnostics per D-10/D-11. Do not import or instantiate Dialog, ConfirmDialog, Notification, MenuBar, or ContextMenu per D-13. Write RED tests in `WebConsoleRuntimeCardsTest` first, then implement until green.</action>
  <acceptance_criteria>
    - `RuntimeDetailRedactor.java` contains `final class RuntimeDetailRedactor` and static methods `redact(`, `stringify(`, and `shorten(`.
    - `RuntimeEventCard.java` contains `class RuntimeEventCard extends Div`.
    - `RuntimeEventCard.java` contains exact attribute names `data-event-category`, `data-event-type`, `data-event-status`, `data-expandable`, and `data-layered-detail`.
    - `RuntimeEventCard.java` contains summary labels `Details` and `Advanced redacted detail`.
    - `WebConsoleRuntimeCardsTest.java` asserts raw strings `sk-live-secret`, `raw-token-value`, `hunter2`, and `my-secret` are not present in card/detail output.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest test</automated>
  </verify>
  <done>Runtime cards and redaction utility exist, tests prove layered details and conservative redaction, and no modal primitives are introduced.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Wire runtime event cards through RunEventRenderer</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java
  </read_first>
  <behavior>
    - Test 1: `RunEventRenderer.render(model.delta event)` still returns category `model`, terminal `false`, text containing model content, and now a `RuntimeEventCard` component.
    - Test 2: policy/status/terminal/generic events return `RuntimeEventCard` components with categories `policy`, `status`, `terminal`, and `event` respectively.
    - Test 3: tool lifecycle and approval-required events continue returning `ToolCallCard` and `ApprovalCard` respectively, not `RuntimeEventCard`.
  </behavior>
  <action>Update `RunEventRenderer.render(...)` so non-tool and non-approval event branches construct `RuntimeEventCard` components while preserving the existing category strings, terminal boolean behavior, and `RenderedEvent` record signature per D-03. Keep tool lifecycle routing to `ToolCallCard.from(event)` and approval routing to `ApprovalCard.from(...)` unchanged except for using `RuntimeDetailRedactor` helpers if needed later. The model/status/policy/terminal/generic visible summary text must keep existing user-readable prefixes (`Policy:`, `Run terminal:`, `Run status:` where applicable) so Phase 12 feed text assertions remain compatible.</action>
  <acceptance_criteria>
    - `RunEventRenderer.java` still contains `public RenderedEvent render(RunEventDto event)` and `public record RenderedEvent`.
    - `RunEventRenderer.java` contains `RuntimeEventCard` in model, policy, terminal, status, or generic event branches.
    - `RunEventRenderer.java` still contains `ToolCallCard.from(event)`.
    - `RunEventRenderer.java` still contains `ApprovalCard.from(toApprovalSummary(event, payload), httpClient)`.
    - `WebConsoleRuntimeCardsTest.java` contains assertions for categories `model`, `policy`, `terminal`, `status`, and `event` producing `RuntimeEventCard`.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleMobileFlowContractTest test</automated>
  </verify>
  <done>Existing feed append/dedupe seam is preserved while representative non-tool runtime events render as compact timeline cards.</done>
</task>

</tasks>

<verification>
Run targeted Java contracts after each task. Confirm no files outside `pi-agent-adapter-web` production/test code are modified in this plan. Confirm `grep -R "new Dialog\|new ConfirmDialog\|Notification.show\|new MenuBar\|new ContextMenu" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console` returns no Phase 13 additions.
</verification>

<success_criteria>
MCARD-01 foundation exists for compact runtime cards, MCARD-03 redaction is reusable, MCARD-05 modal avoidance boundary is preserved, and downstream plans can reuse `RuntimeDetailRedactor` for tool and approval card details.
</success_criteria>

<output>
After completion, create `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-01-SUMMARY.md`.
</output>
