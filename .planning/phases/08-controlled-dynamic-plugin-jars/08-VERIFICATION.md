---
phase: 08-controlled-dynamic-plugin-jars
verified: 2026-06-18T02:09:21Z
status: human_needed
score: 24/24 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 18/24
  gaps_closed:
    - "Disabled/quarantined plugin tools now disappear from current new registry resolution through DynamicPluginToolRegistry and state-aware PluginGovernanceCatalogAdapter."
    - "Admin refresh now re-runs controlled-directory rediscovery through Pf4jControlledPluginDiscoveryService and updates the live adapter snapshot."
    - "allowlist/selected plugin controls are enforced before contribution while rejected plugins remain visible with NOT_ALLOWLISTED/NOT_SELECTED governance metadata."
    - "Adapter Web production code no longer imports org.pf4j directly and the architecture gate now includes Adapter Web PF4J isolation."
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Open Admin Governance plugin view in a browser and inspect visual layout/action-copy clarity."
    expected: "Plugin metadata, refresh/disable/quarantine action plans, redacted errors, and not-a-sandbox warning are understandable and visually acceptable."
    why_human: "Automated component/browser tests verify content and routes, but visual fit/finish and operator comprehension require human judgment."
---

# Phase 8: Controlled Dynamic Plugin JARs Verification Report

**Phase Goal:** Support trusted dynamic plugin JARs as controlled enterprise extensions with lifecycle, compatibility, and operational controls.
**Verified:** 2026-06-18T02:09:21Z
**Status:** human_needed
**Re-verification:** Yes — after gap closure plans 08-09, 08-10, and 08-11.

## Goal Achievement

Phase 8 is automated-code verified. The previous blocker gaps are closed in the actual code path:

- `PluginGovernanceCatalogAdapter.contributionRegistry()` rebuilds from current discovered snapshot and state on each call.
- `DynamicPluginToolRegistry` delegates each `listTools()`/`resolve()` to the current contribution supplier.
- `refresh()` calls the discovery supplier and replaces the snapshot with `REFRESHED` metadata.
- `allowedPluginIds()` and `selectedPluginIds()` are enforced and surfaced as `NOT_ALLOWLISTED` / `NOT_SELECTED` governance metadata.
- `PluginGovernanceBeanConfiguration` uses `Pf4jControlledPluginDiscoveryService` and no longer imports `org.pf4j`.
- Product-path tests prove live Admin REST refresh/disable/quarantine affect `/api/tools` and new run resolution.

The only remaining item is human visual verification of the Admin UI presentation, not a code gap.

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | A developer can enable controlled plugin loading through typed configuration without Admin upload/install workflows. | ✓ VERIFIED | `PluginRegistryProperties`, `PluginGovernanceBeanConfiguration.PluginProperties`, docs `pi.plugins`; `PluginGovernanceApiTest` asserts unsupported CRUD routes are not present. |
| 2 | PF4J dependencies are isolated to a plugin infrastructure module. | ✓ VERIFIED | `Pf4jControlledPluginDiscoveryService` owns PF4J; grep found no `org.pf4j` in `pi-agent-adapter-web/src/main/java`; architecture test passes. |
| 3 | Plugin descriptor/lifecycle summaries can express controlled-directory, compatibility, and non-sandbox warnings. | ✓ VERIFIED | `PluginDescriptorSummary`, `PluginLifecycleSummary`, docs and tests cover compatibility, redacted path/error, non-sandbox warning. |
| 4 | A PF4J-discovered plugin can expose Pi ExtensionSource capabilities without replacing Phase 6 extension APIs. | ✓ VERIFIED | `Pf4jPluginSourceDiscovery` + `Pf4jPluginExtensionBridge`; sample extension implements Pi `ExtensionSource` and PF4J extension point. |
| 5 | Failed, incompatible, disabled, and quarantined plugins remain visible in governance and contribute no usable capabilities. | ✓ VERIFIED | `PluginGovernanceCatalogAdapterTest`, `PluginCapabilityDisablementTest`, and sample compatibility E2E pass. |
| 6 | Disable/quarantine affects new resolution only and does not promise guaranteed hot unload. | ✓ VERIFIED | Docs lines 112-114 state new-resolution semantics/no unload; `DynamicPluginToolRegistry` implements new lookup behavior. |
| 7 | Admin-facing plugin governance contracts are public DTOs/App ports, not PF4J or infrastructure objects. | ✓ VERIFIED | App `PluginGovernanceCatalog`, client `Plugin*Dto`, and controller use DTO/use-case boundaries. |
| 8 | Admin can request plugin refresh, disable, and quarantine through narrow use-case seams. | ✓ VERIFIED | `AdminGovernanceController` delegates `/plugins/refresh`, `/plugins/{id}/disable`, `/plugins/{id}/quarantine` to `GovernanceQueryService`. |
| 9 | Governance overview no longer reports plugins as a placeholder when a plugin catalog is present. | ✓ VERIFIED | `DefaultGovernanceQueryService` plugin mapping was established in 08-03 and no placeholder gap remains. |
| 10 | Cloud Server can bind controlled plugin directory configuration through Spring properties. | ✓ VERIFIED | `PluginProperties` binds `pi.plugins.*` and maps to `PluginRegistryProperties.requireValid()`. |
| 11 | Plugin-provided tool capabilities join the primary ToolRegistry after built-ins, extensions, and MCP without bypassing ToolExecutionGateway. | ✓ VERIFIED | `ToolGovernanceBeanConfiguration` appends `pluginToolRegistry`; `PluginToolRegistryWiringTest` verifies ordering; E2E verifies gateway events/audit. |
| 12 | Admin REST can view, refresh, disable, and quarantine plugins through public DTOs. | ✓ VERIFIED | `AdminGovernanceController`; `PluginGovernanceApiTest` passes. |
| 13 | Plugin tool execution goes through ToolExecutionGateway with policy, audit, events, and redaction. | ✓ VERIFIED | `PluginGovernedToolE2ETest`, `SamplePluginJarE2ETest`, `PluginSecurityRedactionE2ETest` cover policy/audit/events/redaction. |
| 14 | Disabled or quarantined plugin tools are unavailable for new runs and new registry resolution. | ✓ VERIFIED | `SamplePluginJarE2ETest` disables/quarantines via REST, asserts `/api/tools` absence and no `tool.completed` in a new REST-created run. |
| 15 | Raw plugin secrets/unsafe metadata do not appear in REST, events, audit, UI fixtures, or errors. | ✓ VERIFIED | `PluginSecurityRedactionE2ETest` asserts absence across REST, events, persisted events, audit, Admin, catalog, and UI fixture text. |
| 16 | Admin can see plugin metadata, lifecycle, health, compatibility, capability counts, and redacted errors in Web Console. | ✓ VERIFIED | `AdminRegistryStatusView`, `AdminPluginGovernanceViewTest`, Playwright spec, and docs. Human review still recommended for visual quality. |
| 17 | Admin UI exposes confirmed disable/quarantine action plans with optional reasons, not upload/install/delete/upgrade controls. | ✓ VERIFIED | `AdminPluginGovernanceViewTest` and browser smoke coverage. |
| 18 | UI copy explicitly warns that plugin classloader isolation is not a sandbox for untrusted code. | ✓ VERIFIED | Docs and Admin UI tests cover “not a sandbox” warning. |
| 19 | A deterministic sample plugin JAR can be built in the Maven reactor. | ✓ VERIFIED | `pi-sample-plugin-readonly`, `plugin.properties`, and focused Maven gate passed. |
| 20 | Cloud Server can load the sample plugin JAR from a controlled directory and register its safe read-only tool capability. | ✓ VERIFIED | `SamplePluginJarE2ETest` passed: 4 tests, 0 failures. |
| 21 | Compatibility failure, disable, and quarantine flows are validated with real sample plugin packaging. | ✓ VERIFIED | `SamplePluginJarCompatibilityE2ETest` passed: compatibility failure and live refresh; `SamplePluginJarE2ETest` covers live disable/quarantine. |
| 22 | Architecture gates prevent PF4J/plugin implementation leakage into forbidden modules. | ✓ VERIFIED | `PluginInfrastructureArchitectureTest` passed and includes Adapter Web direct PF4J check. |
| 23 | Documentation explains packaging, controlled directory config, lifecycle, compatibility, disable/quarantine, audit/redaction, sample usage, and non-sandbox warning. | ✓ VERIFIED | `docs/phase-08-controlled-dynamic-plugin-jars.md` covers all named topics plus deferrals. |
| 24 | Requirement traceability marks PLUG-01 through PLUG-06 and E2E-08 plugin portion complete with concrete evidence. | ✓ VERIFIED | `.planning/REQUIREMENTS.md` Dynamic Plugins and Traceability sections cite concrete classes/tests; OPS-01 remains pending. |

**Score:** 24/24 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `pi-agent-infrastructure-plugin/pom.xml` | Isolated PF4J infrastructure module | ✓ VERIFIED | Module exists; PF4J dependency local to plugin infra/sample scope. |
| `PluginRegistryProperties.java` | Controlled directory config incl. allowlist/selected/non-sandbox | ✓ VERIFIED | Validates directory/safety and selected-inside-allowlist. |
| `Pf4jControlledPluginDiscoveryService.java` | Infrastructure-owned PF4J manager/discovery | ✓ VERIFIED | Contains `DefaultPluginManager`, `loadPlugins()`, `startPlugins()`, `discover()`. |
| `Pf4jPluginSourceDiscovery.java` / `Pf4jPluginExtensionBridge.java` | PF4J-to-Pi ExtensionSource bridge | ✓ VERIFIED | Bridges plugin descriptors/extensions to Phase 6 contribution model. |
| `PluginGovernanceCatalogAdapter.java` | Refreshable/state-aware governance and contribution registry | ✓ VERIFIED | Uses `AtomicReference`, rebuilds contributions in `contributionRegistry()`, enforces state/selection. |
| `DynamicPluginToolRegistry.java` | Current contribution ToolRegistry wrapper | ✓ VERIFIED | Delegates each list/resolve to `new ExtensionToolRegistry(contributionSupplier.get())`. |
| `PluginGovernanceCatalog.java` and plugin DTOs | Public App/client contracts | ✓ VERIFIED | Controller/use-case boundary uses DTOs and App ports. |
| `PluginGovernanceBeanConfiguration.java` | Adapter composition root | ✓ VERIFIED | PF4J-free Adapter Web composition uses discovery service and dynamic registry. |
| `ToolGovernanceBeanConfiguration.java` | Primary registry includes plugins | ✓ VERIFIED | Builtins → extensions → MCP → plugin registry order. |
| `AdminGovernanceController.java` | Plugin REST endpoints | ✓ VERIFIED | GET/POST refresh/disable/quarantine endpoints exist and delegate. |
| `PluginGovernedToolE2ETest.java` | Governed plugin tool product-path E2E | ✓ VERIFIED | Proves gateway/policy/audit/events. |
| `PluginSecurityRedactionE2ETest.java` | Redaction E2E | ✓ VERIFIED | Proves sensitive plugin text absence across public surfaces. |
| `PluginCapabilityDisablementTest.java` | Same-adapter disable/quarantine filtering | ✓ VERIFIED | Same adapter + dynamic registry resolution becomes empty. |
| `AdminRegistryStatusView.java` / `ConsoleHttpClient.java` | Admin UI plugin rendering/actions | ✓ VERIFIED | Component/browser tests and path anchors exist. |
| `e2e/phase-08-plugin-governance.spec.ts` | Browser smoke for plugin governance | ✓ VERIFIED | Existing summary reports pass; UI visual quality remains human-check item. |
| `pi-sample-plugin-readonly` | Sample PF4J plugin module | ✓ VERIFIED | Descriptor and Java sample plugin exist. |
| `SamplePluginJarE2ETest.java` | Real sample JAR load/invoke/disable/quarantine E2E | ✓ VERIFIED | Product-path REST tests pass. |
| `SamplePluginJarCompatibilityE2ETest.java` | Compatibility and refresh sample tests | ✓ VERIFIED | Refresh after copy into empty controlled directory verified. |
| `PluginInfrastructureArchitectureTest.java` | PF4J isolation architecture gate | ✓ VERIFIED | Adapter Web direct PF4J rule included and passing. |
| `docs/phase-08-controlled-dynamic-plugin-jars.md` | Operator docs | ✓ VERIFIED | Covers config, packaging, lifecycle, refresh, disable/quarantine, redaction, non-sandbox, deferrals. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `pi.plugins.directory` | Controlled-directory PF4J discovery | `PluginProperties` → `PluginRegistryProperties` → `Pf4jControlledPluginDiscoveryService.discover()` | ✓ WIRED | Startup and refresh discovery flow through infrastructure service. |
| `PluginRegistryProperties.allowedPluginIds/selectedPluginIds` | Effective plugin contributions | `selectionStatus()` + `effectiveProperties()` disabled sources | ✓ WIRED | Not-allowlisted/unselected plugins are governance-visible but capability-inert. |
| Admin refresh endpoint | Controlled directory rediscovery | `AdminGovernanceController` → `GovernanceQueryService` → `PluginGovernanceCatalogAdapter.refresh()` → discovery supplier | ✓ WIRED | Sample E2E copies JAR after startup, POSTs refresh, observes tool. |
| Admin disable/quarantine | Current new-run resolution | state store → `adapter.contributionRegistry()` → `DynamicPluginToolRegistry.resolve()` | ✓ WIRED | REST tests prove `/api/tools` and new run resolution change without context rebuild. |
| Plugin registry | `ToolExecutionGateway` | composite primary `ToolRegistry` consumed by `DefaultToolExecutionGateway` | ✓ WIRED | Governed and sample plugin E2E prove audit/events/policy. |
| Adapter Web | PF4J implementation | no direct `org.pf4j` imports; infrastructure service owns PF4J | ✓ WIRED | Grep produced no Adapter Web production matches. |
| Architecture gate | Adapter Web production package | ArchUnit imports `io.github.pi_java.agent.adapter.web` and forbids `org.pf4j..` | ✓ WIRED | `PluginInfrastructureArchitectureTest` passed. |
| Docs/Requirements | Implemented gap closure | Evidence names `DynamicPluginToolRegistry`, `Pf4jControlledPluginDiscoveryService`, sample E2Es | ✓ WIRED | `.planning/REQUIREMENTS.md` and docs updated. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| --- | --- | --- | --- | --- |
| `PluginGovernanceCatalogAdapter` | `discoveredPlugins` snapshot | Initial discovery + refresh supplier | Yes — refresh replaces `AtomicReference` snapshot | ✓ FLOWING |
| `DynamicPluginToolRegistry` | current tool descriptors/resolution | `adapter::contributionRegistry` supplier | Yes — calls supplier for every list/resolve | ✓ FLOWING |
| `PluginGovernanceBeanConfiguration` | plugin discovery/catalog/registry beans | `PluginProperties` + `Pf4jControlledPluginDiscoveryService` | Yes — properties bind actual directory and discovery service | ✓ FLOWING |
| `AdminGovernanceController` | plugin REST responses/mutations | `GovernanceQueryService` public DTO mapping | Yes — endpoints delegate to use-case methods | ✓ FLOWING |
| `SamplePluginJarE2ETest` | sample plugin capability | built sample JAR copied to controlled directory | Yes — REST `/api/tools` and run events show capability | ✓ FLOWING |
| `AdminRegistryStatusView` | plugin DTO rendering | public `PluginGovernanceResponse` / fixture catalog | Yes in component/browser tests; visual polish needs human | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Focused Phase 8 Java smoke gate | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin,pi-sample-plugin-readonly,pi-agent-adapter-web -am -Dtest=PluginInfrastructureArchitectureTest,PluginGovernanceCatalogAdapterTest,PluginCapabilityDisablementTest,PluginRegistryPropertiesTest,PluginToolRegistryWiringTest,PluginGovernanceApiTest,SamplePluginJarE2ETest,SamplePluginJarCompatibilityE2ETest test` | Completed; Surefire reports show 0 failures/errors for inspected tests (`SamplePluginJarE2ETest` 4/0/0, `SamplePluginJarCompatibilityE2ETest` 2/0/0, `PluginInfrastructureArchitectureTest` 3/0/0, etc.). | ✓ PASS |
| Adapter Web direct PF4J isolation | `grep -R "org\.pf4j" -n pi-agent-adapter-web/src/main/java; test $? -eq 1` | No output; command passed. | ✓ PASS |
| Allowlist/selected enforcement presence | grep for `allowedPluginIds()/selectedPluginIds()/NOT_ALLOWLISTED/NOT_SELECTED` | Found in `PluginGovernanceCatalogAdapter.java` lines 175-181. | ✓ PASS |
| Product-path disable/quarantine coverage | Read `SamplePluginJarE2ETest` | REST POST disable/quarantine, `/api/tools` absence, new run no `tool.completed`. | ✓ PASS |
| Product-path refresh coverage | Read `SamplePluginJarCompatibilityE2ETest` | Empty directory at startup, copy sample JAR, POST refresh, asserts `REFRESHED` and tool visibility. | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| PLUG-01 | 08-01, 08-04, 08-07, 08-08, 08-09, 08-10, 08-11 | Admin can configure a controlled plugin directory for trusted dynamic plugin JARs. | ✓ SATISFIED | `pi.plugins.directory`, `PluginRegistryProperties`, `Pf4jControlledPluginDiscoveryService`, Adapter binding, sample load and live refresh E2E. |
| PLUG-02 | 08-01, 08-02, 08-04, 08-05, 08-07, 08-08, 08-09, 08-10, 08-11 | Load descriptors, validate compatibility, register capabilities through extension registry. | ✓ SATISFIED | PF4J bridge/discovery, compatibility tests, sample descriptor, contribution registry, Adapter PF4J isolation gate. |
| PLUG-03 | 08-01, 08-02, 08-03, 08-04, 08-07, 08-08 | Track lifecycle states discovered/loaded/started/disabled/failed/quarantined. | ✓ SATISFIED | Lifecycle summaries, public DTOs, state store overlay, disable/quarantine tests, failed/incompatible visibility. |
| PLUG-04 | 08-03, 08-04, 08-06, 08-08 | Admin can view metadata, capabilities, health, load and compatibility errors in GUI. | ✓ SATISFIED | REST DTOs, `AdminRegistryStatusView`, component tests, browser spec, docs. |
| PLUG-05 | 08-02, 08-03, 08-04, 08-05, 08-07, 08-08, 08-09, 08-10, 08-11 | Admin can disable/quarantine so capabilities are unavailable for new runs. | ✓ SATISFIED | `DynamicPluginToolRegistry`, state-aware adapter, same-adapter disablement tests, live REST sample disable/quarantine E2E. |
| PLUG-06 | 08-01, 08-02, 08-05, 08-06, 08-08 | Dynamic plugin isolation is lifecycle/dependency isolation, not a security sandbox. | ✓ SATISFIED | Config acknowledgement, Admin warning copy, redaction E2E, docs explicitly state not a sandbox. |
| E2E-08 | 08-05, 08-07, 08-08, 08-09, 08-10, 08-11 | Integration E2E verifies MCP and plugin JAR loading/disable flows through same gateway, policy, audit, event pipeline. | ✓ SATISFIED | Plugin governed E2E, sample JAR load/invoke/refresh/disable/quarantine product-path E2E, redaction tests; MCP portion was Phase 7. |

All requirement IDs declared across Phase 8 plan frontmatter are accounted for in `.planning/REQUIREMENTS.md`. No orphaned Phase 8 requirement IDs were found beyond PLUG-01..06 and E2E-08.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` | 15, 27, 43 | `PLACEHOLDER` input placeholder text | ℹ️ Info | Unrelated Phase 5 UI input placeholder; not a Phase 8 stub and not data-flow blocking. |

No Phase 8 blocker stubs were found in the verified implementation files. Empty lists/maps/strings observed in config and DTO paths are defensive defaults or test fixtures, not user-visible unfinished behavior.

### Human Verification Required

### 1. Admin Plugin Governance Visual Review

**Test:** Start the Cloud Server with the e2e/test plugin fixture or a sample controlled plugin directory, open Admin Governance plugin status, and inspect the rendered plugin rows and action plans.
**Expected:** Admin can clearly understand plugin ID/name/version/vendor, lifecycle, compatibility/health, redacted errors, capability counts, refresh/disable/quarantine action plans, and the not-a-sandbox warning.
**Why human:** Automated tests verify content, routing, absence of forbidden controls, and redaction; final visual clarity and operator comprehension require human judgment.

### Gaps Summary

No automated code gaps remain. The prior dynamic registry, refresh, allowlist/selected, Adapter Web PF4J isolation, docs, and traceability gaps are closed. Phase 8 is ready to proceed after optional/human UI visual acceptance.

---

_Verified: 2026-06-18T02:09:21Z_
_Verifier: the agent (gsd-verifier)_
