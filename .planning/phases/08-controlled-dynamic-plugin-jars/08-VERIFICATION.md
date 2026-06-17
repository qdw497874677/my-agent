---
phase: 08-controlled-dynamic-plugin-jars
verified: 2026-06-17T17:05:02Z
status: gaps_found
score: 18/24 must-haves verified
gaps:
  - truth: "Disabled or quarantined plugin tools are unavailable for new runs and new registry resolution."
    status: failed
    reason: "Admin disable/quarantine mutates PluginStateStore, but the runtime plugin ToolRegistry is built once from PluginGovernanceCatalogAdapter.contributionRegistry(); that contribution registry is final and is not rebuilt after mutation, so existing Cloud Server new-run resolution can still resolve plugin tools."
    artifacts:
      - path: "pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapter.java"
        issue: "contributionRegistry is computed once in the constructor before later disable/quarantine state changes."
      - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java"
        issue: "pluginToolRegistry bean wraps adapter.contributionRegistry() once at startup; Admin mutation endpoints do not update it."
      - path: "pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SamplePluginJarCompatibilityE2ETest.java"
        issue: "disable/quarantine tests rebuild a new adapter manually instead of proving REST Admin mutation affects the live Cloud Server registry used for new runs."
    missing:
      - "Make plugin registry resolution consult current PluginStateStore on each resolve/list, or rebuild/swap plugin contributions after disable/quarantine."
      - "Add product-path test: load sample plugin, disable/quarantine via Admin REST, then prove /api/tools/new REST-created run cannot resolve/invoke the plugin tool."
  - truth: "Admin can request plugin refresh through a narrow audited use-case seam and controlled directory refresh rediscovery."
    status: failed
    reason: "refresh() returns REFRESH_REQUESTED metadata only; no code re-runs PF4J discovery or updates the adapter/catalog/registry from the controlled directory."
    artifacts:
      - path: "pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapter.java"
        issue: "refresh() is a static response and has no discovery callback or catalog update."
      - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java"
        issue: "discoverPlugins() is only called during bean construction; refresh endpoint has no path back to it."
    missing:
      - "Implement refresh to rediscover controlled-directory JARs and update governance status plus plugin tool registry for new resolution."
      - "Add a test that starts without a plugin, copies a sample JAR into the controlled directory, POSTs /api/admin/governance/plugins/refresh, and observes the capability become available."
  - truth: "Controlled plugin directory configuration enforces allowlist/selected controls."
    status: failed
    reason: "PluginRegistryProperties validates selected IDs are inside allowlist, but neither allowedPluginIds nor selectedPluginIds are used during PF4J discovery, governance, or registry construction; all JARs in the directory are discovered and started."
    artifacts:
      - path: "pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginRegistryProperties.java"
        issue: "properties define allowedPluginIds/selectedPluginIds but only validate relationship between lists."
      - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java"
        issue: "discoverPlugins() ignores allowlist/selected settings when loading/starting plugins."
    missing:
      - "Filter or reject plugin descriptors not allowed/selected before contributing capabilities."
      - "Add tests proving an unselected/not-allowlisted JAR remains unavailable and visible only as disabled/rejected governance state if appropriate."
  - truth: "Architecture gates prevent PF4J/plugin implementation leakage into forbidden modules."
    status: failed
    reason: "Adapter Web production code imports PF4J directly, while docs and must-haves claim PF4J is isolated to plugin infrastructure. The ArchUnit gate does not include adapter-web in the forbidden import set, so it does not catch this leakage."
    artifacts:
      - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java"
        issue: "imports org.pf4j.DefaultPluginManager and org.pf4j.PluginManager directly."
      - path: "pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginInfrastructureArchitectureTest.java"
        issue: "forbidden-module rule excludes Adapter Web, allowing production PF4J imports outside plugin infrastructure."
    missing:
      - "Move PF4J DefaultPluginManager construction/loading behind a plugin-infrastructure factory/service."
      - "Extend architecture gates to forbid Adapter Web production code from importing org.pf4j.., except for deliberate sample plugin packaging/tests."
---

# Phase 8: Controlled Dynamic Plugin JARs Verification Report

**Phase Goal:** Controlled Dynamic Plugin JARs — isolated PF4J plugin infrastructure, controlled directory loading, plugin governance/admin controls, governed tool execution path, sample plugin E2E, architecture gates, docs, and traceability.
**Verified:** 2026-06-17T17:05:02Z
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

Phase 8 has substantial implementation: plugin infrastructure module, PF4J discovery bridge, governance DTOs/REST/UI, governed plugin-tool E2E tests, a real sample plugin JAR, docs, and requirements traceability all exist. However, several goal-critical behaviors are not actually wired in the live product path:

1. Admin disable/quarantine does not update the already-constructed runtime plugin registry used for new runs.
2. Admin refresh does not rediscover the controlled directory.
3. allowlist/selected plugin controls are configuration-only and are not enforced during discovery/registration.
4. PF4J is not fully isolated to plugin infrastructure because Adapter Web production code imports PF4J directly and the architecture gate allows that leakage.

These gaps block full achievement of PLUG-01, PLUG-02 architecture-isolation expectations, PLUG-05, and the plugin half of E2E-08.

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | A developer can enable controlled plugin loading through typed configuration without Admin upload/install workflows. | ⚠️ PARTIAL | `PluginRegistryProperties` and `pi.plugins` binding exist; no upload/install routes found. But allowlist/selected controls are not enforced. |
| 2 | PF4J dependencies are isolated to a plugin infrastructure module. | ✗ FAILED | `PluginGovernanceBeanConfiguration.java` imports `org.pf4j.DefaultPluginManager`/`PluginManager`; architecture gate excludes Adapter Web. |
| 3 | Plugin descriptor/lifecycle summaries can express controlled-directory, compatibility, and non-sandbox warnings. | ✓ VERIFIED | `PluginDescriptorSummary`, `PluginLifecycleSummary`, docs, and tests cover redacted path/compatibility/non-sandbox fields. |
| 4 | A PF4J-discovered plugin can expose Pi ExtensionSource capabilities without replacing Phase 6 extension APIs. | ✓ VERIFIED | `Pf4jPluginSourceDiscovery` + `Pf4jPluginExtensionBridge` bridge PF4J wrappers to `ServiceLoaderExtensionDiscovery.DiscoveredSource`; sample plugin implements Pi `ExtensionSource`. |
| 5 | Failed, incompatible, disabled, and quarantined plugins remain visible in governance and contribute no usable capabilities. | ⚠️ PARTIAL | Governance adapter display and manually-rebuilt adapter tests pass, but live runtime registry is not updated after Admin disable/quarantine. |
| 6 | Disable/quarantine affects new resolution only and does not promise guaranteed hot unload. | ✗ FAILED | Hot-unload is not promised, but live new resolution still uses the startup `contributionRegistry`; Admin mutation does not affect the active `pluginToolRegistry`. |
| 7 | Admin-facing plugin governance contracts are public DTOs/App ports, not PF4J or infrastructure objects. | ✓ VERIFIED | App `PluginGovernanceCatalog`, client `Plugin*Dto` records, and `DefaultGovernanceQueryService` mapping use public boundaries. |
| 8 | Admin can request plugin refresh, disable, and quarantine through narrow use-case seams. | ⚠️ PARTIAL | REST/App methods exist, but refresh is only `REFRESH_REQUESTED` and disable/quarantine do not update active registry resolution. |
| 9 | Governance overview no longer reports plugins as a placeholder when a plugin catalog is present. | ✓ VERIFIED | `DefaultGovernanceQueryService` maps `pluginGovernanceCatalog.plugins()` and no plugin future placeholder was found in that service. |
| 10 | Cloud Server can bind controlled plugin directory configuration through Spring properties. | ✓ VERIFIED | `PluginGovernanceBeanConfiguration.PluginProperties` binds `pi.plugins.*` and validates required directory/non-sandbox acknowledgement. |
| 11 | Plugin-provided tool capabilities join the primary ToolRegistry after built-ins, extensions, and MCP without bypassing ToolExecutionGateway. | ✓ VERIFIED | `ToolGovernanceBeanConfiguration` appends `pluginToolRegistry`; `PluginGovernedToolE2ETest` and `SamplePluginJarE2ETest` show gateway events/audit. |
| 12 | Admin REST can view, refresh, disable, and quarantine plugins through public DTOs. | ⚠️ PARTIAL | Endpoints exist and return DTOs, but refresh/disable/quarantine semantics are incomplete as above. |
| 13 | Plugin tool execution goes through ToolExecutionGateway with policy, audit, events, and redaction. | ✓ VERIFIED | `PluginGovernedToolE2ETest` asserts policy allow/deny/approval branches, `tool.lifecycle`, and audit; sample JAR E2E invokes through `ToolExecutionGateway`. |
| 14 | Disabled or quarantined plugin tools are unavailable for new runs and new registry resolution. | ✗ FAILED | Tests prove this only after constructing a new adapter; live Cloud Server registry is fixed at startup and not state-aware. |
| 15 | Raw plugin secrets/unsafe metadata do not appear in REST, events, audit, UI fixtures, or errors. | ✓ VERIFIED | `PluginSecurityRedactionE2ETest`, DTO sanitization, and UI fixture checks assert absence of fake secret/path/env strings. |
| 16 | Admin can see plugin metadata, lifecycle, health, compatibility, capability counts, and redacted errors in Web Console. | ✓ VERIFIED | `AdminRegistryStatusView.showPlugins`, `AdminPluginGovernanceViewTest`, and Playwright spec exist. |
| 17 | Admin UI exposes confirmed disable/quarantine action plans with optional reasons, not upload/install/delete/upgrade controls. | ✓ VERIFIED | UI tests assert action-plan text and absence of deferred controls. |
| 18 | UI copy explicitly warns that plugin classloader isolation is not a sandbox for untrusted code. | ✓ VERIFIED | UI test and docs contain explicit “not a sandbox” warning. |
| 19 | A deterministic sample plugin JAR can be built in the Maven reactor. | ✓ VERIFIED | `pi-sample-plugin-readonly` module and built `target/pi-sample-plugin-readonly-0.1.0-SNAPSHOT.jar` exist. |
| 20 | Cloud Server can load the sample plugin JAR from a controlled directory and register its safe read-only tool capability. | ✓ VERIFIED | `SamplePluginJarE2ETest` copies JAR to temp controlled directory and asserts Admin/tools visibility. |
| 21 | Compatibility failure, disable, and quarantine flows are validated with real sample plugin packaging. | ⚠️ PARTIAL | Compatibility and manually rebuilt disable/quarantine tests exist; product-path Admin mutation to live registry is not proven. |
| 22 | Architecture gates prevent PF4J/plugin implementation leakage into forbidden modules. | ✗ FAILED | Gate exists but omits Adapter Web; Adapter Web imports PF4J directly. |
| 23 | Documentation explains packaging, controlled directory config, lifecycle, compatibility, disable/quarantine, audit/redaction, sample usage, and non-sandbox warning. | ✓ VERIFIED | `docs/phase-08-controlled-dynamic-plugin-jars.md` covers all named topics and deferrals. |
| 24 | Requirement traceability marks PLUG-01 through PLUG-06 and E2E-08 plugin portion complete with concrete evidence. | ✓ VERIFIED | `.planning/REQUIREMENTS.md` contains concrete Phase 8 evidence for PLUG-01..06 and E2E-08, though some claims are overstated given the gaps. |

**Score:** 18/24 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `pi-agent-infrastructure-plugin/pom.xml` | Isolated PF4J infrastructure module | ✓ VERIFIED | Exists and declares `pf4j`; module is in root reactor. |
| `PluginRegistryProperties.java` | Controlled directory config | ⚠️ PARTIAL | Exists and validates enable/directory/non-sandbox; allowlist/selected are not enforced downstream. |
| `Pf4jPluginSourceDiscovery.java` | PF4J discovery to Pi source bridge | ✓ VERIFIED | Uses PF4J `PluginManager` and bridges plugin `ExtensionSource` instances. |
| `Pf4jPluginExtensionBridge.java` | Descriptor/lifecycle/compatibility bridge | ✓ VERIFIED | Enriches source/capability metadata and preserves `ToolExtensionCapability`. |
| `PluginGovernanceCatalogAdapter.java` | Plugin read model and state overlay | ⚠️ PARTIAL | Displays state overlay, but final `contributionRegistry` is not recomputed after mutations. |
| `PluginGovernanceCatalog.java` and plugin DTOs | App/client public contracts | ✓ VERIFIED | App port and DTO records exist and are mapped by governance service. |
| `PluginGovernanceBeanConfiguration.java` | Adapter composition root | ⚠️ PARTIAL | Wires discovery/catalog/registry, but owns PF4J construction directly and only discovers at bean creation. |
| `ToolGovernanceBeanConfiguration.java` | Primary registry includes plugins | ✓ VERIFIED | Adds plugin registry after built-ins/extensions/MCP. |
| `AdminGovernanceController.java` | Plugin REST endpoints | ⚠️ PARTIAL | Endpoints exist; mutation/refresh semantics incomplete in live registry path. |
| `PluginGovernedToolE2ETest.java` | Product-path governed plugin execution E2E | ✓ VERIFIED | Verifies gateway, policy, audit, event, provenance behavior for fake plugin tools. |
| `PluginSecurityRedactionE2ETest.java` | Redaction E2E | ✓ VERIFIED | Verifies raw fake secret/path/env/metadata absence across public surfaces. |
| `PluginCapabilityDisablementTest.java` | Disable/quarantine filtering regression | ⚠️ PARTIAL | Proves manually rebuilt adapter filtering, not live Admin mutation effect on existing registry. |
| `AdminRegistryStatusView.java` / `ConsoleHttpClient.java` | Admin UI plugin rendering/actions | ✓ VERIFIED | UI paths/rendering/action-plan tests exist. |
| `e2e/phase-08-plugin-governance.spec.ts` | Browser smoke | ✓ VERIFIED | File exists and summary reports passing. |
| `pi-sample-plugin-readonly` | Sample PF4J plugin module | ✓ VERIFIED | Module, descriptor, plugin class, extension source, and built JAR exist. |
| `SamplePluginJarE2ETest.java` | Real sample JAR load/invoke E2E | ✓ VERIFIED | Loads from temp controlled directory and invokes via REST-created run. |
| `SamplePluginJarCompatibilityE2ETest.java` | Compatibility/disable/quarantine sample tests | ⚠️ PARTIAL | Tests compatibility and rebuilt adapter disable/quarantine; not REST/live-registry mutation. |
| `PluginInfrastructureArchitectureTest.java` | PF4J isolation architecture gate | ✗ FAILED | Gate exists but omits Adapter Web, allowing production PF4J imports there. |
| `docs/phase-08-controlled-dynamic-plugin-jars.md` | Operator docs | ✓ VERIFIED | Exists and covers requirements, config, lifecycle, endpoints, safety, verification, deferrals. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `PluginRegistryProperties` | Spring `pi.plugins` binding | `PluginProperties.toInfrastructure()` | ✓ WIRED | Adapter Web maps properties to infrastructure config. |
| `pi.plugins.directory` | PF4J directory loading | `DefaultPluginManager(List.of(directory))` | ✓ WIRED | Startup discovery loads configured directory. |
| `allowedPluginIds/selectedPluginIds` | PF4J discovery/registry | expected filtering | ✗ NOT_WIRED | No downstream references outside validation/tests. |
| `AdminGovernanceController` | `GovernanceQueryService` | plugin methods | ✓ WIRED | Controller delegates plugin status/refresh/disable/quarantine. |
| Admin disable/quarantine | live plugin `ToolRegistry` | state store → registry resolution | ✗ NOT_WIRED | Mutations update state store only; active contribution registry remains unchanged. |
| Admin refresh | controlled directory rediscovery | endpoint → PF4J reload | ✗ NOT_WIRED | `refresh()` returns static status only. |
| Plugin registry | `ToolExecutionGateway` | primary composite registry | ✓ WIRED | Plugin registry is included in primary tool registry consumed by `DefaultToolExecutionGateway`. |
| Sample plugin JAR | controlled directory load | copy target jar → PF4J discovery | ✓ WIRED | Sample E2E copies built JAR into temp directory and loads it. |
| Architecture gate | PF4J isolation | ArchUnit forbidden packages | ✗ PARTIAL | Gate protects core/App/client/API/starter/MCP/model, not Adapter Web production. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| --- | --- | --- | --- | --- |
| `PluginGovernanceBeanConfiguration` | discovered plugin list | `discoverPlugins()` calls PF4J `loadPlugins/startPlugins` at bean creation | Yes at startup | ⚠️ STATIC after startup; no refresh data flow |
| `PluginGovernanceCatalogAdapter` | `contributionRegistry` | constructor builds from discovered sources + initial state | Yes initially | ⚠️ STATIC after disable/quarantine |
| `AdminGovernanceController` | plugin REST responses | `GovernanceQueryService` → `PluginGovernanceCatalog` | Yes | ⚠️ mutation responses do not update active runtime registry |
| `AdminRegistryStatusView` | plugin DTOs | public `PluginGovernanceResponse` passed to `showPlugins` | Yes in tests/fixtures | ✓ FLOWING |
| `SamplePluginJarE2ETest` | sample plugin capability | built sample JAR copied to temp plugin directory | Yes | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Previous verification exists | glob `.planning/phases/08-controlled-dynamic-plugin-jars/*-VERIFICATION.md` | none | ✓ PASS |
| Required Phase 8 files exist | glob/read across plugin infra, adapter tests, sample plugin, docs | expected files found | ✓ PASS |
| PF4J import isolation | grep `org\.pf4j` in Java | Adapter Web production import found | ✗ FAIL |
| allowlist/selected enforcement | grep downstream references for `allowedPluginIds/selectedPluginIds` | only config validation/tests; no discovery use | ✗ FAIL |
| Admin mutation affects registry | code trace from controller → state store → registry | no recompute/dynamic registry path | ✗ FAIL |
| Docs coverage | read `docs/phase-08-controlled-dynamic-plugin-jars.md` | covers PLUG-01..06, E2E-08, not-a-sandbox, verification | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| PLUG-01 | 08-01, 08-04, 08-07, 08-08 | Admin can configure controlled plugin directory for trusted dynamic plugin JARs. | ⚠️ PARTIAL | `pi.plugins.directory` loads at startup, but allowlist/selected controls are not enforced and refresh is not real rediscovery. |
| PLUG-02 | 08-01, 08-02, 08-04, 08-05, 08-07, 08-08 | Load descriptors, validate compatibility, register capabilities through extension registry. | ⚠️ PARTIAL | Descriptor/compatibility/registration works; PF4J implementation leaks into Adapter Web despite isolation goal. |
| PLUG-03 | 08-01, 08-02, 08-03, 08-04, 08-07, 08-08 | Track lifecycle states discovered/loaded/started/disabled/failed/quarantined. | ✓ SATISFIED | Lifecycle summaries, DTOs, governance adapter, tests, and docs cover state visibility. |
| PLUG-04 | 08-03, 08-04, 08-06, 08-08 | Admin can view metadata, capabilities, health, load and compatibility errors in GUI. | ✓ SATISFIED | REST DTOs, `AdminRegistryStatusView`, component test, Playwright spec, docs. |
| PLUG-05 | 08-02, 08-03, 08-04, 08-05, 08-07, 08-08 | Admin can disable/quarantine so capabilities are unavailable for new runs. | ✗ BLOCKED | Mutations update state store/read model but not the live startup plugin registry used by `ToolExecutionGateway`. |
| PLUG-06 | 08-01, 08-02, 08-05, 08-06, 08-08 | Dynamic plugin isolation is lifecycle/dependency isolation, not a security sandbox. | ✓ SATISFIED | Config acknowledgement, UI warning, docs, and redaction tests exist. |
| E2E-08 | 08-05, 08-07, 08-08 | E2E verifies plugin JAR loading/disable flows through same gateway, policy, audit, event pipeline. | ⚠️ PARTIAL | Gateway/audit/event and sample JAR load E2E exist; disable/quarantine product path is tested by rebuilding adapter, not by live Admin mutation → new-run resolution. |

No Phase 8 requirement IDs from the user prompt are missing from plan frontmatter or `.planning/REQUIREMENTS.md`. No additional Phase 8 PLUG/E2E requirement IDs were found orphaned in `.planning/REQUIREMENTS.md` beyond PLUG-01..06 and E2E-08.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `PluginGovernanceCatalogAdapter.java` | 25, 33 | final cached `contributionRegistry` built once | 🛑 Blocker | Disable/quarantine state changes do not affect active new-run registry resolution. |
| `PluginGovernanceCatalogAdapter.java` | 54-57 | static refresh response | 🛑 Blocker | Admin refresh endpoint does not rediscover controlled-directory plugins. |
| `PluginGovernanceBeanConfiguration.java` | 47-55 | one-time startup discovery | 🛑 Blocker | Refresh and runtime changes cannot update plugin catalog/registry. |
| `PluginGovernanceBeanConfiguration.java` | 16-17 | direct PF4J imports in Adapter Web | ⚠️ Warning/Blocker for isolation | Violates claimed PF4J infrastructure isolation. |
| `PluginRegistryProperties.java` + `PluginGovernanceBeanConfiguration.java` | 15-16 / discovery path | config not consumed | 🛑 Blocker | allowlist/selected controls do not control loaded capabilities. |
| `PluginInfrastructureArchitectureTest.java` | 22-32 | forbidden package list omits Adapter Web | ⚠️ Warning | Architecture gate cannot catch current Adapter Web PF4J leakage. |

The `ChatEventStreamPanel` placeholder string found by grep is unrelated Phase 5 UI input placeholder text and not a Phase 8 stub.

### Human Verification Required

None for current status. Automated code inspection found blocker gaps before human-only visual verification would be meaningful.

### Gaps Summary

Phase 8 is close but not fully goal-complete. The delivered code demonstrates startup plugin loading and governed execution, but dynamic governance controls are not actually dynamic in the live Cloud Server path. Disable/quarantine and refresh need to affect the active plugin catalog/tool registry for subsequent runs. Controlled loading also needs to enforce allowlist/selected IDs, and PF4J construction should move out of Adapter Web to satisfy the isolation goal and architecture gate.

---

_Verified: 2026-06-17T17:05:02Z_
_Verifier: the agent (gsd-verifier)_
