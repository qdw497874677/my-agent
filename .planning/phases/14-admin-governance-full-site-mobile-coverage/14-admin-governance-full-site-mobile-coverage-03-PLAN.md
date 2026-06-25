---
phase: 14-admin-governance-full-site-mobile-coverage
plan: 03
type: execute
wave: 2
depends_on:
  - 14-admin-governance-full-site-mobile-coverage-01
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminOperationsViewTest.java
autonomous: true
requirements:
  - MADM-02
must_haves:
  truths:
    - "Mobile admin can inspect Operations data as metric/area cards for Runs, Models, Tools, Policies, MCP, Plugins, Errors, and Warnings."
    - "Operations abnormal/error/warning states are scan-friendly and do not rely on desktop-width rows."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java"
      provides: "Operations metric/area mobile cards"
      contains: "data-operations-card"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminOperationsViewTest.java"
      provides: "Operations card contract tests"
      contains: "data-operations-card"
  key_links:
    - from: "AdminOperationsView"
      to: "AdminMobileCardSupport"
      via: "metric cards and Details"
      pattern: "AdminMobileCardSupport\."
---

<objective>
Convert Admin Operations metrics and warnings into mobile metric/area cards.

Purpose: MADM-02 requires Operations data to be readable as cards or responsive details without desktop table/row assumptions.
Output: `AdminOperationsView` metric-card conversion and Java contract coverage.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@.planning/STATE.md
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-CONTEXT.md
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-01-SUMMARY.md
@docs/phase-09-production-hardening.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java

<interfaces>
Existing `AdminOperationsView` methods to preserve:
```java
public String operationsPath()
public void showOperations(OperationsSummaryResponse response)
public String renderedText()
public OperationsSummaryResponse operations()
public boolean explorerControlsPresent()
```

Phase 14 locked decisions implemented here: D-04, D-05, D-06, D-07, D-09, D-10.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Render Operations metrics and warnings as cards</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminOperationsViewTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminOperationsViewTest.java
    - .planning/phases/14-admin-governance-full-site-mobile-coverage/14-CONTEXT.md
  </read_first>
  <behavior>
    - Test 1: `showOperations(sampleOperations())` renders sections for Runs, Models, Tools, Policies, MCP, Plugins, Errors, and Warnings with `data-operations-section` preserved.
    - Test 2: each non-empty metric renders a `data-operations-card` with fields `area`, `name`, `status`, `value`, `unit`, and collapsed metadata details.
    - Test 3: warning cards render `data-operations-warning-card` with severity chip and message summary.
    - Test 4: explorer/export/query-builder/delete/disable controls remain absent.
  </behavior>
  <action>Refactor `AdminOperationsView` to use `AdminMobileCardSupport` for metric and warning sections. Keep sections exactly `runs`, `models`, `tools`, `policies`, `mcp`, `plugins`, `errors`, and `warnings` through `data-operations-section`. For each `OperationMetricDto`, create a `data-operations-card` with `data-operations-area`, `data-operations-status`, `data-status-severity`, and label-value rows for `area`, `name`, `status`, `value`, `unit`; put metadata behind collapsed Details. For each `OperationalWarningDto`, create `data-operations-warning-card` with `severity`, `area`, and `message` summary plus metadata Details. Use abnormal-state severity for statuses/severities containing `ERROR`, `FAILED`, `DOWN`, `WARN`, `WARNING`, `UNHEALTHY`, or nonzero Errors values; otherwise normal. Preserve `renderedText()` semantic strings for existing assertions. Do not introduce charts, query builders, time range controls, export, mutation controls, or public DTO changes, per D-04/D-05/D-09.</action>
  <acceptance_criteria>
    - `AdminOperationsView.java` contains `data-operations-card` and `data-operations-warning-card`.
    - `AdminOperationsView.java` contains `data-status-severity`.
    - `AdminOperationsViewTest.java` asserts all eight section identifiers and at least one metric card selector.
    - `AdminOperationsViewTest.java` keeps negative assertions for export/query builder/chart editor/delete/disable.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminOperationsViewTest test</automated>
  </verify>
  <done>MADM-02 Operations data is card/detail based, abnormal states are represented with chips/severity attributes, and deferred controls stay absent.</done>
</task>

</tasks>

<verification>
Also run `grep -R "vaadin-grid\|Grid<" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java` and confirm no table/grid dependency was introduced.
</verification>

<success_criteria>
- Operations has metric/area cards for all required areas.
- No desktop table or deferred analytics/search/export surface is introduced.
</success_criteria>

<output>
After completion, create `.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-03-SUMMARY.md`
</output>
