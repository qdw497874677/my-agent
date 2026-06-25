---
phase: 14-admin-governance-full-site-mobile-coverage
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileRedactor.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java
  - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebMobileBaselineContractTest.java
autonomous: true
requirements:
  - MADM-01
must_haves:
  truths:
    - "Mobile admin can read Governance Overview as stacked cards for runtime, providers, tools, extensions, MCP, and plugins."
    - "Governance Overview summary cards show status/health, count, short message, and links without pipe-separated dense text."
    - "Long metadata and redacted diagnostic details are collapsed behind mobile-safe Details controls."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java"
      provides: "Shared Admin card/detail/label/chip helper for Phase 14"
      exports: ["AdminMobileCardSupport"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileRedactor.java"
      provides: "Conservative adapter-web Admin redaction helper"
      exports: ["AdminMobileRedactor"]
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java"
      provides: "Overview stacked status cards"
      contains: "data-admin-overview-card"
    - path: "pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css"
      provides: "Admin card/detail/chip/no-overflow/tap-target CSS"
      contains: ".pi-admin-card"
  key_links:
    - from: "AdminGovernanceOverviewView"
      to: "AdminMobileCardSupport"
      via: "shared helper methods"
      pattern: "AdminMobileCardSupport\."
    - from: "styles.css"
      to: "Admin card selectors"
      via: "classes and data hooks"
      pattern: "pi-admin-card|data-admin-details"
---

<objective>
Create the shared Admin mobile card/detail rendering foundation and convert Governance Overview to stacked mobile cards.

Purpose: Phase 14 needs one consistent card/detail/label/chip contract before Registry, Operations, Policy, and Audit views can be converted without duplicating brittle `Span`/pipe-separated rendering.
Output: Package-local Admin helper/redactor, Overview mobile cards, CSS primitives, and Java contract coverage for MADM-01.
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
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-RESEARCH.md
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-VALIDATION.md
@docs/phase-11-responsive-shell.md
@docs/phase-13-runtime-cards.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageSection.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java
@pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css

<interfaces>
From `PiPageSection.java`:
```java
public class PiPageSection extends Div {
    public enum Variant { CARD("pi-card"), DETAIL("pi-detail") }
    public PiPageSection(String sectionName, Variant variant, Component... children)
    public static PiPageSection card(String sectionName, Component... children)
    public static PiPageSection detail(String sectionName, Component... children)
}
```

Existing Overview contract:
```java
public void showOverview(GovernanceOverviewResponse overview)
public String overviewPath()
public String registryStatusPath()
public String operationsPath()
public String renderedText()
```

Phase 14 locked decisions implemented here: D-02, D-05, D-06, D-07, D-09, D-10, D-16.
Deferred ideas prohibited: new mobile-only Admin routes, search/filter/export, screenshot visual regression, React/Next/Hilla/native/PWA.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add shared Admin mobile card and redaction helper contracts</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileRedactor.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiPageSection.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java
    - .planning/phases/14-admin-governance-full-site-mobile-coverage/14-CONTEXT.md
  </read_first>
  <behavior>
    - Test 1: `AdminMobileRedactor` returns `[REDACTED]` for values containing `sk-`, `rawSecret`, `apiKey`, `password`, `bearer `, or `token=`.
    - Test 2: `AdminMobileCardSupport.statusCard(...)` creates a `PiPageSection`-based card with class `pi-admin-card`, attribute `data-admin-card`, section attribute, and child label/value rows with `data-admin-field`.
    - Test 3: `AdminMobileCardSupport.details(...)` creates a collapsed Vaadin `Details` component with `data-expandable="true"`, `data-admin-details`, and detail content marked with `data-detail-layer="structured"` or `advanced`.
  </behavior>
  <action>Create package-private helper classes in `io.github.pi_java.agent.adapter.web.ui.admin`. `AdminMobileRedactor` must provide static methods for safe text and redacted metadata rendering; use conservative matching equivalent to Phase 13 plus Admin terms: `sk-`, `rawsecret`, `apikey`, `api_key`, `password`, `bearer `, `authorization`, `token=`, `access_token`, `refresh_token`. `AdminMobileCardSupport` must expose static helpers for `page(String routeName, Component...)`, `statusCard(String section, String title, String status, String count, String message, Component... details)`, `metricCard`, `labelValue`, `statusChip`, `actionRow`, `details`, and `redactedBlock`. Use `PiPageSection.card`/`detail`, `.pi-admin-card`, `.pi-admin-field`, `.pi-status-chip`, `.pi-detail-block`, `.pi-redacted-json`, and stable attributes `data-admin-card`, `data-admin-section`, `data-admin-field`, `data-status-chip`, `data-admin-details`, `data-detail-layer`. Do not make these helpers public API outside adapter-web/admin; do not import Domain/App/persistence/runtime classes. Add focused tests in `AdminGovernanceViewsTest` for helper/redactor behavior before relying on it in views, per D-05/D-06/D-07/D-16.</action>
  <acceptance_criteria>
    - `AdminMobileCardSupport.java` contains `class AdminMobileCardSupport` and `data-admin-card`.
    - `AdminMobileRedactor.java` contains `class AdminMobileRedactor` and `[REDACTED]`.
    - `AdminGovernanceViewsTest.java` contains assertions for `data-admin-details`, `data-admin-field`, and raw secret strings not appearing.
    - No new file imports `io.github.pi_java.agent.domain`, `io.github.pi_java.agent.app`, `org.springframework.jdbc`, or `com.vaadin.hilla`.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test</automated>
  </verify>
  <done>Shared Admin helper/redactor compile, tests prove redaction and selector contracts, and no public API/domain boundary changes are introduced.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Convert Governance Overview to stacked status cards</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java
    - .planning/REQUIREMENTS.md
  </read_first>
  <behavior>
    - Test 1: `showOverview(sampleOverview())` renders six `[data-admin-overview-card]` cards for runtime, providers, toolRegistry, extensions, mcp, and plugins.
    - Test 2: each card summary contains title/status/count/message label-value fields and not a pipe-separated string.
    - Test 3: metadata appears only inside collapsed Details with `data-admin-details` and redacted values.
    - Test 4: Overview keeps `registryStatusPath()` and `operationsPath()` links for route continuity.
  </behavior>
  <action>Refactor `AdminGovernanceOverviewView.showOverview` and `addStatus` to render `PiPageHeader` or equivalent heading plus stacked `AdminMobileCardSupport.statusCard` components. For each `GovernanceStatusDto`, create a card with attributes `data-admin-overview-card="{area}"`, `data-governance-area="{status.area()}"`, `data-governance-status="{status.status()}"`, and fields `status`, `count`, and `message`. Add action-row links to `/admin/governance/registry`, `/admin/governance/operations`, `/admin/governance/policy-decisions`, and `/admin/governance/audits` without adding new routes, per D-01/D-02. Preserve `renderedText()` semantic strings for existing tests, but stop rendering the visible summary as a single `" | "` pipe-separated Span. Put sorted metadata behind collapsed Details named `Metadata`, using `AdminMobileRedactor` and `data-detail-layer="structured"`, per D-05/D-06/D-07/D-16.</action>
  <acceptance_criteria>
    - `AdminGovernanceOverviewView.java` contains `data-admin-overview-card` and `AdminMobileCardSupport.statusCard`.
    - `AdminGovernanceOverviewView.java` no longer contains `new Span(text)` inside `addStatus`.
    - `AdminGovernanceViewsTest.java` asserts card count or selectors for runtime/providers/toolRegistry/extensions/mcp/plugins.
    - `AdminGovernanceViewsTest.java` asserts `/admin/governance/registry` and `/admin/governance/operations` are preserved.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test</automated>
  </verify>
  <done>MADM-01 Overview reads as stacked cards with collapsed redacted metadata and stable selectors while preserving existing route paths.</done>
</task>

<task type="auto">
  <name>Task 3: Add Admin mobile card/detail CSS contract</name>
  <files>pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebMobileBaselineContractTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebMobileBaselineContractTest.java
    - docs/phase-11-responsive-shell.md
    - docs/phase-13-runtime-cards.md
  </read_first>
  <action>Extend the `pi-mobile` theme with Admin card/detail CSS that downstream plans can reuse without editing CSS again. Add selectors for `.pi-admin-card`, `.pi-admin-card-grid`, `.pi-admin-card-summary`, `.pi-admin-field`, `.pi-admin-field-label`, `.pi-admin-field-value`, `.pi-admin-details`, `.pi-admin-nested-card`, `.pi-admin-action-row`, and Admin-specific attributes `[data-admin-card]`, `[data-admin-details]`, `[data-admin-field]`, `[data-status-chip]`. Required CSS values: `width: 100%`, `max-width: 100%`, `min-width: 0`, `overflow-wrap: anywhere`, `word-break: break-word` for cards/fields/detail blocks; grid gap uses `var(--pi-mobile-space-sm)`; controls/details inherit the existing 44px tap target/focus-visible contract. Add status chip modifiers for abnormal states using attributes/classes: `[data-status-severity="abnormal"]`, `[data-status-severity="warning"]`, `[data-status-severity="normal"]`. Update `WebMobileBaselineContractTest` to assert the stylesheet contains `.pi-admin-card`, `[data-admin-details]`, and `data-status-severity` so later plans can rely on them.</action>
  <acceptance_criteria>
    - `styles.css` contains `.pi-admin-card`, `.pi-admin-field`, `.pi-admin-details`, `.pi-admin-nested-card`, and `data-status-severity`.
    - `styles.css` contains `overflow-wrap: anywhere` in the Admin card/detail selector block.
    - `WebMobileBaselineContractTest.java` contains string assertions for `.pi-admin-card` and `[data-admin-details]`.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebMobileBaselineContractTest,AdminGovernanceViewsTest test</automated>
  </verify>
  <done>Admin card/detail styling is available for all later Phase 14 view conversions and guarded by fast tests.</done>
</task>

</tasks>

<verification>
Run the task commands plus a grep boundary check: `grep -R "io.github.pi_java.agent.domain\|io.github.pi_java.agent.app\|com.vaadin.hilla" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobile*.java` should return no matches.
</verification>

<success_criteria>
- MADM-01 has a tested Overview card implementation.
- The shared helper/redactor and CSS contracts exist before wave 2 plans consume them.
- No deferred mobile-only route/API/frontend-stack ideas appear.
</success_criteria>

<output>
After completion, create `.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-01-SUMMARY.md`
</output>
