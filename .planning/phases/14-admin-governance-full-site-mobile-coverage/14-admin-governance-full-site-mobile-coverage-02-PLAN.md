---
phase: 14-admin-governance-full-site-mobile-coverage
plan: 02
type: execute
wave: 2
depends_on:
  - 14-admin-governance-full-site-mobile-coverage-01
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPluginGovernanceViewTest.java
autonomous: true
requirements:
  - MADM-02
  - MADM-03
  - MADM-04
  - MADM-05
must_haves:
  truths:
    - "Mobile admin can inspect Registry data as sectioned cards instead of a single dense text stream."
    - "Mobile admin can inspect MCP server and MCP tool state, including disconnected/unhealthy examples, without desktop tables."
    - "Mobile admin can inspect Plugin lifecycle, health, selected/disabled/quarantined/load-error style states and metadata in stacked cards/details."
    - "Mobile admin can inspect Extension sources and capabilities as sectioned cards/details."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java"
      provides: "Sectioned Registry/MCP/Plugin/Extension mobile card rendering"
      contains: "data-admin-registry-section"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java"
      provides: "MCP mobile card branch coverage"
      contains: "data-mcp-server-card"
    - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPluginGovernanceViewTest.java"
      provides: "Plugin mobile card branch coverage"
      contains: "data-plugin-card"
  key_links:
    - from: "AdminRegistryStatusView"
      to: "ConsoleHttpClient Admin paths"
      via: "existing governance/refresh/action path helpers"
      pattern: "admin(Mcp|Plugin|Extension).*Path"
    - from: "AdminRegistryStatusView"
      to: "AdminMobileCardSupport"
      via: "cards, details, chips, label rows"
      pattern: "AdminMobileCardSupport\."
---

<objective>
Convert Admin Registry, MCP, Plugin, and Extension governance rendering into explicit sectioned mobile cards/details.

Purpose: Registry is the densest Admin page and carries MADM-02 through MADM-05. It must surface abnormal integration states without relying on table scrolling or pipe-separated rows.
Output: `AdminRegistryStatusView` card/detail conversion with Java tests covering registry, MCP, plugin, and extension branches.
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
@.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-01-SUMMARY.md
@docs/phase-07-mcp-client-bridge.md
@docs/phase-08-controlled-dynamic-plugin-jars.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java

<interfaces>
Existing `AdminRegistryStatusView` public methods to preserve:
```java
public String overviewPath()
public String extensionGovernancePath()
public String mcpGovernancePath()
public String mcpRefreshPath()
public String mcpRefreshActionText()
public String pluginGovernancePath()
public String pluginRefreshPath()
public String pluginRefreshActionText()
public String pluginDisableActionText(String pluginId)
public String pluginQuarantineActionText(String pluginId)
public void showOverview(GovernanceOverviewResponse overview)
public void showExtensions(ExtensionGovernanceResponse extensions)
public void showMcpGovernance(McpGovernanceResponse governance)
public void showPlugins(PluginGovernanceResponse governance)
public boolean mutationControlsPresent()
public String renderedText()
```

Phase 14 locked decisions implemented here: D-03, D-05, D-06, D-07, D-09, D-10, D-11, D-12, D-13, D-16, D-19, D-21.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Convert registry overview and extension sections to mobile cards</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java
    - .planning/phases/14-admin-governance-full-site-mobile-coverage/14-CONTEXT.md
  </read_first>
  <behavior>
    - Test 1: `showOverview(sampleOverview())` renders distinct section cards for model providers, tool registry, extensions, MCP, and plugins with `data-admin-registry-section`.
    - Test 2: `showExtensions(sampleExtensions())` renders `data-extension-source-card`, nested `data-extension-capability-card`, source kind/status/health chips, and collapsed metadata details.
    - Test 3: extension metadata detail redacts secret markers and no visible summary uses ` | ` pipe-separated row text.
  </behavior>
  <action>Refactor `showOverview`, `addStatus`, `showExtensions`, `addExtensionSource`, and `addExtensionCapability` to use `AdminMobileCardSupport`. Create explicit section wrappers with `data-admin-registry-section="registry|extensions|mcp|plugins"`. For extension sources, render summary fields `sourceId`, `name`, `kind`, `lifecycleStatus`, `healthStatus`, `compatibilityStatus`, `enabled`, and `capabilities` as label-value rows plus status chips. Render capabilities as nested cards with `data-extension-capability-card`, `data-extension-capability`, and `data-capability-type`. Put metadata and redacted error in collapsed Details, not the summary, per D-05/D-06/D-10/D-13/D-16. Preserve `renderedText()` entries for current semantic assertions but do not render those pipe-separated strings as the primary visible summary.</action>
  <acceptance_criteria>
    - `AdminRegistryStatusView.java` contains `data-admin-registry-section` and `data-extension-source-card`.
    - `AdminRegistryStatusView.java` contains `data-extension-capability-card`.
    - `AdminGovernanceViewsTest.java` asserts `data-extension-source-card` or `data-extension-capability-card` via element inspection.
    - `AdminRegistryStatusView.java` still contains `extensionGovernancePath()` returning the existing ConsoleHttpClient path.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test</automated>
  </verify>
  <done>Registry and Extension surfaces render as sectioned cards/details with stable selectors and unchanged public path helpers.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Convert MCP and Plugin governance to abnormal-first mobile cards</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPluginGovernanceViewTest.java</files>
  <read_first>
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPluginGovernanceViewTest.java
    - docs/phase-07-mcp-client-bridge.md
    - docs/phase-08-controlled-dynamic-plugin-jars.md
  </read_first>
  <behavior>
    - Test 1: `showMcpGovernance(sampleMcpGovernance())` renders each server as `data-mcp-server-card` with connection/discovery/health/status chips and tool-count summary.
    - Test 2: MCP tools render as nested cards or collapsed structured details with `data-mcp-tool-card`, availability, readOnly/destructive/openWorld, schema summary, and redacted error.
    - Test 3: `showPlugins(samplePluginGovernance())` renders plugin cards with lifecycle, health, compatibility, selected/enabled/disabled/quarantined/load-error style states where DTO data exists.
    - Test 4: abnormal MCP/plugin cards are rendered before normal cards when sample data contains unhealthy/down/quarantined/failed items.
  </behavior>
  <action>Refactor `showMcpGovernance`, `addMcpServer`, `addMcpTool`, `showPlugins`, `addPlugin`, and `addPluginCapability`. For MCP, render a refresh action row with existing `data-action-plan="POST"`, `data-action-path=mcpRefreshPath()`, and `data-read-only-refresh="true"`. Render server cards with `data-mcp-server-card`, `data-mcp-server`, `data-mcp-connection`, `data-mcp-discovery`, `data-status-severity`, and label-value rows for `transport`, `auth`, `tools`, `lastRefresh`, and short redacted message. MCP tools must be inspectable as `data-mcp-tool-card` nested cards or Details, per D-12. For plugins, render warning card `data-plugin-warning="not-a-sandbox"`, refresh row, and one `data-plugin-card` per plugin with summary fields: pluginId, lifecycle, enabled, health, compatibility, selected/disabled/quarantined/load-error where represented by lifecycle/health/compatibility/reason/redactedError, capabilityCount, lastUpdated. Add action row buttons for existing refresh/disable/quarantine path helpers only; do not add upload/install/delete/upgrade/search/export. Sort abnormal/operator-relevant plugin and MCP entries before normal entries using statuses containing `UNHEALTHY`, `FAILED`, `DOWN`, `WARN`, `DISCONNECTED`, `DISABLED`, `QUARANTINED`, `INCOMPATIBLE`, or nonblank redacted error/reason, per D-09/D-10/D-11/D-12. Put metadata/capability status counts/path summaries behind collapsed Details with redaction.</action>
  <acceptance_criteria>
    - `AdminRegistryStatusView.java` contains `data-mcp-server-card`, `data-mcp-tool-card`, `data-plugin-card`, and `data-plugin-capability-card`.
    - `AdminRegistryStatusView.java` contains abnormal-state sort/helper logic or comparator referencing `QUARANTINED` or `UNHEALTHY`.
    - `McpAdminGovernanceViewTest.java` asserts `data-mcp-server-card` and `data-mcp-tool-card`.
    - `AdminPluginGovernanceViewTest.java` asserts `data-plugin-card`, `data-plugin-action`, and absence of upload/install/delete/upgrade/search/export controls.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest test</automated>
  </verify>
  <done>MADM-03/MADM-04/MADM-05 registry sub-surfaces are mobile card/detail based, abnormal states are discoverable, and existing read-only/action boundaries remain intact.</done>
</task>

</tasks>

<verification>
Run the task commands and confirm no public Admin DTO files under `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin` changed unless a non-mobile bug is explicitly discovered and documented.
</verification>

<success_criteria>
- Registry, MCP, Plugin, and Extension Admin surfaces render through cards/details with stable selectors.
- Existing ConsoleHttpClient public Admin paths remain the only route/API anchors.
- Deferred advanced Admin search/filter/export and mobile-only routes are absent.
</success_criteria>

<output>
After completion, create `.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-02-SUMMARY.md`
</output>
