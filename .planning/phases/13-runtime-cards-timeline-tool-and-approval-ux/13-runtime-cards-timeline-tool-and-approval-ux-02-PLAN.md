---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
plan: 02
type: execute
wave: 2
depends_on: [13-runtime-cards-timeline-tool-and-approval-ux-01]
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java
autonomous: true
requirements: [MCARD-02, MCARD-03]
must_haves:
  truths:
    - "Mobile user can inspect one tool card per tool runtime event with visible tool name, source, status, policy/approval state, duration, and error."
    - "Mobile user can expand redacted input/output and diagnostics without raw sensitive payload exposure."
    - "Tool card rendering preserves existing event append/dedupe semantics and does not aggregate lifecycle events."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java"
      provides: "Structured mobile tool execution card"
      exports: ["ToolCallCard"]
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java"
      provides: "Tool-card contract coverage"
  key_links:
    - from: "RunEventRenderer.java"
      to: "ToolCallCard.from(event)"
      via: "tool.lifecycle branch"
      pattern: "ToolCallCard\\.from\\(event\\)"
    - from: "ToolCallCard.java"
      to: "RuntimeDetailRedactor.java"
      via: "redacted summaries and advanced detail"
      pattern: "RuntimeDetailRedactor\\."
---

<objective>
Upgrade tool lifecycle cards into structured, mobile-readable, redacted tool execution cards.

Purpose: Satisfy MCARD-02/MCARD-03 while honoring D-04/D-05/D-06: one card per runtime event, no lifecycle aggregation, visible key summary fields, expandable redacted input/output and dense fields.
Output: Refactored `ToolCallCard` and strengthened Java contracts.
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
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java

<interfaces>
Use interfaces created by Plan 01:

```java
final class RuntimeDetailRedactor {
    static String redact(String value);
    static String stringify(Object value);
    static String shorten(String value, int maxChars);
}

public class ToolCallCard extends Div {
    public static ToolCallCard from(RunEventDto event);
    public String summaryText();
    public String detailsText();
}
```

Locked decisions implemented here: D-04 one card per runtime event, D-05 visible tool fields with expandable redacted summaries, D-06 no mixed aggregation rules, D-12 redaction protects secrets and long strings.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Refactor ToolCallCard into structured summary and detail layers</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java
  </read_first>
  <behavior>
    - Test 1: A completed tool event with `toolName`, `source`, `status`, `policyDecision`, `approvalState`, `durationMs`, `resultSummary`, and `errorCategory` shows those values in default `summaryText()`.
    - Test 2: The card has stable attributes `data-event-category="tool"`, `data-tool-status`, `data-tool-name`, `data-tool-source`, `data-policy-state`, and `data-expandable="true"`.
    - Test 3: The default visible text does not require a Details expansion to see tool name, source, status, policy/approval state, duration, and error.
  </behavior>
  <action>Refactor `ToolCallCard` away from one pipe-delimited `Span` into a mobile card hierarchy still extending `Div`. Keep `ToolCallCard.from(RunEventDto)` and `summaryText()`/`detailsText()` public methods. The visible summary must include concrete labels: `Tool`, `Source`, `Status`, `Policy`, `Approval`, `Duration`, `Error`, and `Summary`. Populate from payload keys: tool name from `toolName|tool|toolId|descriptorRef`; source from `source|provider|registrySource|toolSource` with fallback `runtime`; status from `status|decision|phase`; policy from `policyDecision|policyState|policyReason|decision`; approval from `approvalState|approvalStatus|requiresApproval|previewId`; duration from `durationMs|durationMillis|elapsedMs|duration`; error from `errorCategory|error|errorSummary`; summary from `purpose|summary|reason|resultSummary|outputSummary|redactedResultSummary`. Add CSS class `pi-tool-call-card pi-card` and data attributes listed in behavior. Use `RuntimeDetailRedactor` for all user-visible payload-derived text.</action>
  <acceptance_criteria>
    - `ToolCallCard.java` contains labels `Tool`, `Source`, `Status`, `Policy`, `Approval`, `Duration`, `Error`, and `Summary`.
    - `ToolCallCard.java` contains attributes `data-tool-name`, `data-tool-source`, and `data-policy-state`.
    - `ToolCallCard.java` contains `RuntimeDetailRedactor.stringify` or `RuntimeDetailRedactor.redact`.
    - `WebConsoleCatalogAndToolCardsTest.java` asserts `source`, `policyDecision` or `Policy`, `approvalState` or `Approval`, and `durationMs` or `Duration` behavior.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleCatalogAndToolCardsTest test</automated>
  </verify>
  <done>Tool cards expose all MCARD-02 default fields in a stable structured hierarchy without lifecycle aggregation.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add redacted input/output and advanced diagnostics sections</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeEventCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java
  </read_first>
  <behavior>
    - Test 1: Payload keys `inputSummary`, `argumentSummary`, `outputSummary`, `resultSummary`, `preview`, and `diagnostics` appear only redacted inside `Details` sections.
    - Test 2: Advanced details include sequence, type, payloadSchema, eventSequence, policyReason, previewId, and redacted diagnostics.
    - Test 3: Details text does not contain raw `sk-live-secret`, `raw-token-value`, `password=hunter2`, or `authorization=Bearer abc123`.
  </behavior>
  <action>Add two expandable Vaadin `Details` sections to `ToolCallCard`: summary `Input / output summary` for redacted input/output/argument/result/preview summaries, and summary `Advanced redacted detail` for sequence/type/payloadSchema/eventSequence/policyReason/previewId/diagnostics. Set `data-detail-layer="structured"` on the first details element and `data-detail-layer="advanced"` on the second. Preserve one card per event per D-04/D-06; do not add any static map keyed by toolCallId and do not mutate prior cards. Ensure `detailsText()` returns the same redacted advanced text used by the advanced Details so existing tests remain useful.</action>
  <acceptance_criteria>
    - `ToolCallCard.java` contains text `Input / output summary` and `Advanced redacted detail`.
    - `ToolCallCard.java` contains attributes `data-detail-layer` with values `structured` and `advanced`.
    - `ToolCallCard.java` does not contain `Map<String, ToolCallCard>` or a `toolCallId` aggregation cache.
    - `WebConsoleCatalogAndToolCardsTest.java` asserts no raw secret strings in `detailsText()`.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleCatalogAndToolCardsTest,WebConsoleRuntimeCardsTest test</automated>
  </verify>
  <done>Tool details provide structured and advanced redacted inspection without exposing raw payloads or changing lifecycle semantics.</done>
</task>

</tasks>

<verification>
Run the Java targeted tests. Inspect `ToolCallCard.java` to confirm no lifecycle aggregation map/cache was introduced and all payload-derived strings go through `RuntimeDetailRedactor`.
</verification>

<success_criteria>
MCARD-02 and MCARD-03 are covered for tool events: visible key summary fields, expandable redacted input/output/diagnostics, no raw sensitive payloads, no lifecycle aggregation.
</success_criteria>

<output>
After completion, create `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-02-SUMMARY.md`.
</output>
