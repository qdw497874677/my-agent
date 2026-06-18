---
phase: 08-controlled-dynamic-plugin-jars
plan: 09
subsystem: infrastructure-plugin
tags: [java, pf4j, plugins, governance, tool-registry, cola]

# Dependency graph
requires:
  - phase: 08-controlled-dynamic-plugin-jars
    provides: Controlled PF4J plugin bridge, governance catalog, state store, and plugin admin contracts from prior Phase 8 plans
provides:
  - Infrastructure-owned PF4J controlled-directory discovery service
  - Refreshable plugin governance catalog backed by current plugin state and discovery snapshots
  - Dynamic plugin ToolRegistry wrapper for current contribution resolution
  - Allowlist and selected-plugin controls that keep rejected plugins visible but capability-inert
affects: [adapter-web-plugin-wiring, admin-governance, plugin-tool-registry, phase-08-gap-closure]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Infrastructure service owns PF4J manager construction and discovery
    - Contribution registry rebuilt per lookup from current state snapshot
    - Selection policy disables contribution without hiding governance status

key-files:
  created:
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jControlledPluginDiscoveryService.java
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/DynamicPluginToolRegistry.java
  modified:
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapter.java
    - pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapterTest.java
    - pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginCapabilityDisablementTest.java
    - pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginRegistryPropertiesTest.java

key-decisions:
  - "Keep PF4J discovery in pi-agent-infrastructure-plugin through Pf4jControlledPluginDiscoveryService so later Adapter Web wiring can consume a PF4J-free service boundary."
  - "Rebuild DefaultExtensionContributionRegistry from the current discovery snapshot and PluginStateStore on each contributionRegistry() call so disable/quarantine affect new resolutions on the same adapter instance."
  - "Model allowlist/selected rejections as disabled contribution sources with governance metadata selectionStatus rather than hiding plugins from admin status."

patterns-established:
  - "Dynamic registry supplier: DynamicPluginToolRegistry delegates each list/resolve call to a fresh ExtensionToolRegistry built from adapter::contributionRegistry."
  - "Refresh snapshot: PluginGovernanceCatalogAdapter uses an AtomicReference for discovered plugin sources and replaces it on successful refresh."
  - "Selection visibility: NOT_ALLOWLISTED and NOT_SELECTED plugins remain visible in plugins() while contribution registry excludes usable capabilities."

requirements-completed: [PLUG-01, PLUG-02, PLUG-05, E2E-08]

# Metrics
duration: 5m 56s
completed: 2026-06-18
---

# Phase 08 Plan 09: Dynamic Plugin Infrastructure Gap Closure Summary

**PF4J controlled-directory discovery, refreshable plugin governance snapshots, and state-aware dynamic plugin tool resolution.**

## Performance

- **Duration:** 5m 56s
- **Started:** 2026-06-18T01:33:52Z
- **Completed:** 2026-06-18T01:39:48Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments

- Added `Pf4jControlledPluginDiscoveryService` in the plugin infrastructure module to own `DefaultPluginManager` construction, `loadPlugins()`, `startPlugins()`, and `Pf4jPluginSourceDiscovery.discover()` behind startup/discovery gates.
- Made `PluginGovernanceCatalogAdapter` refreshable and state-aware by storing discovered plugin sources in an `AtomicReference`, rebuilding contribution registries from the latest state on each lookup, and supporting refresh suppliers with `REFRESHED` / `REFRESH_DISABLED` statuses.
- Added `DynamicPluginToolRegistry` so plugin tool list/resolve calls always consult the latest governance-derived contribution registry.
- Enforced `allowedPluginIds` and `selectedPluginIds` before contribution while keeping rejected plugins visible in governance with `selectionStatus` metadata (`NOT_ALLOWLISTED`, `NOT_SELECTED`).

## Task Commits

Each task was committed atomically:

1. **Task 1: Move PF4J controlled-directory discovery behind an infrastructure service** - `217e964` (feat)
2. **Task 2: Make plugin governance contributions refreshable and state-aware** - `dce458a` (feat)
3. **Task 3: Enforce allowlist and selected plugin controls before contribution** - `8f976ae` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jControlledPluginDiscoveryService.java` - Infrastructure-owned PF4J discovery service with disabled/no-directory/no-startup no-op behavior.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/DynamicPluginToolRegistry.java` - ToolRegistry wrapper that delegates every lookup to a current contribution registry supplier.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapter.java` - Refreshable plugin snapshot, dynamic contribution registry rebuilding, refresh status handling, disable/quarantine state propagation, and allowlist/selected filtering.
- `pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginRegistryPropertiesTest.java` - Discovery gate coverage for disabled, missing-directory, and startup-disabled states.
- `pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginCapabilityDisablementTest.java` - Same-adapter disable/quarantine regression coverage through `DynamicPluginToolRegistry`.
- `pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapterTest.java` - Refresh supplier replacement, manual-refresh-disabled, NOT_ALLOWLISTED, and NOT_SELECTED governance tests.

## Decisions Made

- Keep PF4J manager creation out of Adapter Web by introducing an infrastructure service now; Plan 08-10 can wire this service without adding new PF4J semantics to web adapters.
- Preserve the legacy list-based `PluginGovernanceCatalogAdapter` constructor for existing tests while adding a constructor with `PluginRegistryProperties` and discovery supplier for live refresh.
- Treat allowlist/selected rejection as contribution disablement rather than discovery failure, because the plugin is still known and should remain visible to Admin Governance.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The initial self-check command attempted to use `rg`, which is not installed in this environment. Re-ran commit existence verification with `git cat-file -e`; all task commits were found.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. Stub scan found only intentional optional-null constructor fields and empty-string DTO defaults used for backward-compatible status records; no UI-facing placeholder data or unwired mock sources were introduced.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin -am -Dtest=PluginRegistryPropertiesTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin -am -Dtest=PluginGovernanceCatalogAdapterTest,PluginCapabilityDisablementTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin -am -Dtest=PluginGovernanceCatalogAdapterTest,PluginRegistryPropertiesTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin -am test`

## Next Phase Readiness

- Plan 08-10 can replace Adapter Web's direct PF4J construction with `Pf4jControlledPluginDiscoveryService` and use `DynamicPluginToolRegistry` for live plugin registry behavior.
- Plugin admin refresh/disable/quarantine flows now have infrastructure semantics that affect new registry resolution without reconstructing the adapter.

## Self-Check: PASSED

- Found created/modified files: `Pf4jControlledPluginDiscoveryService.java`, `DynamicPluginToolRegistry.java`, `PluginGovernanceCatalogAdapter.java`, `PluginGovernanceCatalogAdapterTest.java`, `PluginCapabilityDisablementTest.java`, `PluginRegistryPropertiesTest.java`.
- Found task commits: `217e964`, `dce458a`, `8f976ae`.

---
*Phase: 08-controlled-dynamic-plugin-jars*
*Completed: 2026-06-18*
