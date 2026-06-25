---
phase: 14-admin-governance-full-site-mobile-coverage
plan: 04
type: execute
wave: 2
depends_on:
  - 14-admin-governance-full-site-mobile-coverage-01
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java
autonomous: true
requirements:
  - MADM-06
  - MADM-07
must_haves:
  truths:
    - "Mobile admin can inspect Policy decisions with decision, reason, tool, run ID, session ID, and timestamp summaries."
    - "Mobile admin can inspect Audit summaries with actor/source/action/status/resource/timestamp-style summaries where DTO data exists."
    - "Policy and Audit redacted context/details are collapsed by default and do not expose raw sensitive payloads."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java"
      provides: "Policy decision cards with collapsed redacted context"
      contains: "data-policy-decision-card"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java"
      provides: "Audit summary cards with collapsed redacted details"
      contains: "data-audit-card"
  key_links:
    - from: "AdminPolicyDecisionsView"
      to: "AdminMobileRedactor"
      via: "redacted context details"
      pattern: "AdminMobileRedactor\."
    - from: "AdminAuditView"
      to: "Console session/run links"
      via: "existing /console/sessions paths"
      pattern: "/console/sessions/"
---

<objective>
Convert Policy Decisions and Audit Summaries into safe mobile cards with collapsed redacted context/details.

Purpose: Governance safety requires scan-friendly summaries while keeping sensitive policy/audit context hidden until deliberately expanded and still redacted.
Output: Card/detail conversions for `AdminPolicyDecisionsView` and `AdminAuditView` plus Java redaction/selector tests.
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
@docs/phase-13-runtime-cards.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java

<interfaces>
Existing Policy/Audit methods to preserve:
```java
public void showPolicyDecisions(List<PolicyDecisionSummaryDto> decisions)
public String policyDecisionsPath()
public List<String> contextLinks()
public String renderedText()

public void showAudits(List<AuditSummaryDto> audits)
public String auditsPath()
public List<String> contextLinks()
public String renderedText()
```

Phase 14 locked decisions implemented here: D-05, D-06, D-07, D-10, D-14, D-15, D-16, D-17, D-21.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Render Policy decision cards with collapsed redacted context</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileRedactor.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java
  </read_first>
  <behavior>
    - Test 1: each policy decision renders `data-policy-decision-card` and preserves `data-policy-decision-id`.
    - Test 2: summary fields include decision, reason, tool, toolCall, session, run, and decidedAt as label-value rows.
    - Test 3: redacted summary map is inside collapsed `data-admin-details` with `data-detail-layer="structured"` or `advanced`.
    - Test 4: raw secrets such as `sk-test-secret`, `rawSecret`, `apiKey`, `password`, and token-like values are absent.
  </behavior>
  <action>Refactor `AdminPolicyDecisionsView.addDecision` to create a `data-policy-decision-card` using `AdminMobileCardSupport`. Summary must include label-value rows for `decision`, `reason`, `tool`, `toolCall`, `session`, `run`, and `decidedAt`; add a `.pi-status-chip` / `data-status-chip` based on decision value (`ALLOW` normal, `DENY`/`BLOCK`/`REQUIRE_APPROVAL` warning/abnormal). Preserve existing Session and Run anchors and `contextLinks()`. Move `decision.redactedSummary()` into collapsed Details named `Redacted context` with `data-admin-details="policy-context"`; details must be collapsed by default per D-17. Use `AdminMobileRedactor` for every key/value rendered in details, per D-14/D-16. Keep `data-deferred-controls="search-filter-export"` but do not add actual search/filter/export controls.</action>
  <acceptance_criteria>
    - `AdminPolicyDecisionsView.java` contains `data-policy-decision-card` and `policy-context`.
    - `AdminPolicyDecisionsView.java` contains `AdminMobileRedactor`.
    - `AdminGovernanceViewsTest.java` asserts `data-policy-decision-card` and absence of raw sensitive strings.
    - `AdminGovernanceViewsTest.java` still asserts context links `/console/sessions/session-1` and `/console/sessions/session-1/runs/run-1`.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test</automated>
  </verify>
  <done>MADM-06 Policy decisions render as safe card summaries with collapsed redacted context and existing context links.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Render Audit summary cards with collapsed redacted details</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileRedactor.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java
  </read_first>
  <behavior>
    - Test 1: each audit renders `data-audit-card` and preserves `data-audit-id`.
    - Test 2: summary fields include action, resourceType, resourceId, session, run, and recordedAt as label-value rows.
    - Test 3: redactedDetails map is inside collapsed `data-admin-details="audit-details"`.
    - Test 4: search/filter/export controls remain absent and raw sensitive strings remain absent.
  </behavior>
  <action>Refactor `AdminAuditView.addAudit` to create a `data-audit-card` using `AdminMobileCardSupport`. Summary must include label-value rows for `action`, `resourceType`, `resourceId`, `session`, `run`, and `recordedAt`. If future DTO fields for actor/source/status are not present, do not invent backend fields; include only existing DTO fields and keep the plan summary truthful. Move `audit.redactedDetails()` into collapsed Details named `Redacted audit details` with `data-admin-details="audit-details"` and `data-detail-layer="structured"`. Use `AdminMobileRedactor` for every rendered detail key/value, per D-15/D-16/D-17. Preserve Session/Run anchors and `contextLinks()`. Do not add search/filter/export controls.</action>
  <acceptance_criteria>
    - `AdminAuditView.java` contains `data-audit-card` and `audit-details`.
    - `AdminAuditView.java` contains `AdminMobileRedactor`.
    - `AdminGovernanceViewsTest.java` asserts `data-audit-card` and absence of raw sensitive strings.
    - `AdminGovernanceViewsTest.java` keeps negative assertions for search/filter/export.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test</automated>
  </verify>
  <done>MADM-07 Audit summaries render as safe card summaries with collapsed redacted details and no deferred controls.</done>
</task>

</tasks>

<verification>
Run `grep -R "sk-test-secret\|rawSecret\|apiKey\|password" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java` and confirm no test secret marker is hardcoded into production rendering.
</verification>

<success_criteria>
- Policy and Audit pages are mobile card/detail surfaces.
- Sensitive context is collapsed and redacted by default.
- Existing public paths and context links remain stable.
</success_criteria>

<output>
After completion, create `.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-04-SUMMARY.md`
</output>
