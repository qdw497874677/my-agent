---
phase: 08-controlled-dynamic-plugin-jars
plan: 02
subsystem: infra
tags: [pf4j, plugins, extension-api, governance, lifecycle]

requires:
  - phase: 06-java-extension-surface-spi-and-spring
    provides: Framework-free ExtensionSource, capability, lifecycle, compatibility, and contribution registry semantics.
  - phase: 08-controlled-dynamic-plugin-jars
    provides: Isolated PF4J plugin infrastructure module, registry configuration, and redacted descriptor/lifecycle summaries from Plan 08-01.
provides:
  - PF4J plugin wrapper to Pi ExtensionSource bridge with sanitized descriptor provenance.
  - PF4J plugin source discovery read model that mirrors ServiceLoader discovered source semantics.
  - In-memory plugin disable/quarantine state seam and governance catalog adapter.
  - Capability filtering so failed, incompatible, disabled, and quarantined plugins remain visible but contribute no usable capabilities.
affects: [phase-08-plugin-loading, admin-governance, extension-integration, plugin-lifecycle]

tech-stack:
  added: []
  patterns:
    - Keep PF4J types isolated to `pi-agent-infrastructure-plugin` while exposing Pi ExtensionSource and App plugin governance records outward.
    - Use PluginStateStore to affect new capability resolution only; no hot unload guarantee is implied.

key-files:
  created:
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jPluginExtensionBridge.java
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jPluginSourceDiscovery.java
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginStateStore.java
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/InMemoryPluginStateStore.java
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapter.java
    - pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jPluginSourceDiscoveryTest.java
    - pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapterTest.java
  modified:
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscovery.java

key-decisions:
  - "Bridge PF4J plugins into existing `ServiceLoaderExtensionDiscovery.DiscoveredSource` semantics so `DefaultExtensionContributionRegistry` remains authoritative for compatibility, disablement, ordering, and usable capability filtering."
  - "Model plugin disable/quarantine as an infrastructure state overlay that affects new resolution only and does not promise JVM hot unload or interruption of active calls."
  - "Expose plugin provenance through sanitized metadata (`sourceKind=PLUGIN`, plugin id/version/provider/path summary) without leaking raw PF4J objects outside plugin infrastructure."

patterns-established:
  - "PF4J-to-Pi bridge: PF4J owns descriptor/classloader/lifecycle inputs while Pi ExtensionSource remains the capability contract."
  - "Plugin governance state overlay: disabled/quarantined plugins remain visible in governance and are capability-inert for new registry resolution."
  - "Sanitized plugin diagnostics: raw secrets and absolute host paths are redacted before governance/status records."

requirements-completed: [PLUG-02, PLUG-03, PLUG-05, PLUG-06]

duration: 9m 03s
completed: 2026-06-16
---

# Phase 08 Plan 02: PF4J Extension Bridge and Plugin Governance State Summary

**PF4J-discovered plugin JARs now translate into Pi ExtensionSource contributions with disabled/quarantined governance filtering and sanitized failure visibility.**

## Performance

- **Duration:** 9m 03s
- **Started:** 2026-06-16T17:22:09Z
- **Completed:** 2026-06-16T17:31:12Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added a PF4J plugin extension bridge that converts plugin wrapper descriptor/lifecycle data plus plugin-provided `ExtensionSource` instances into Pi discovered source records.
- Enriched plugin-provided sources/capabilities with sanitized plugin provenance metadata while preserving existing `DefaultExtensionContributionRegistry` compatibility and duplicate semantics.
- Preserved failed and incompatible plugins as governance-visible, capability-inert discovered sources with sanitized diagnostics.
- Added an in-memory `PluginStateStore` seam for disable/quarantine state and a governance adapter that maps plugin discovery plus state into App-layer plugin source/capability status records.
- Proved disabled and quarantined plugins remain visible, set `enabled=false`, require explicit operator action for quarantine, and contribute no usable capabilities for new resolution.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Bridge PF4J plugins into Pi ExtensionSource discovery** - `d06b4e4` (test)
2. **Task 1 GREEN: Bridge PF4J plugins into Pi ExtensionSource discovery** - `2a49b7a` (feat)
3. **Task 2 RED: Add disable/quarantine state and plugin governance adapter** - `70258df` (test)
4. **Task 2 GREEN: Add disable/quarantine state and plugin governance adapter** - `04c6441` (feat)

**Plan metadata:** pending final docs commit

_Note: Both tasks were TDD and therefore have separate failing-test and implementation commits._

## Files Created/Modified

- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jPluginExtensionBridge.java` - Bridges PF4J wrapper descriptor/lifecycle information and plugin `ExtensionSource` instances into Pi extension discovery semantics.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jPluginSourceDiscovery.java` - Discovers plugin-provided `ExtensionSource` objects through PF4J and records plugin descriptor/lifecycle context alongside the normalized discovered source.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginStateStore.java` - Defines the disable/quarantine state seam for plugin lifecycle governance.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/InMemoryPluginStateStore.java` - Provides v1 in-memory plugin state with sanitized actor/reason storage.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapter.java` - Converts plugin discovery plus state into App-layer plugin governance records and filters disabled/quarantined sources from new usable capability resolution.
- `pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jPluginSourceDiscoveryTest.java` - Verifies plugin bridge metadata enrichment, usable capability contribution, failed plugin visibility, and sanitized failure handling.
- `pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginGovernanceCatalogAdapterTest.java` - Verifies disabled/quarantined/failed/incompatible plugin states, capability filtering, and mutation status redaction.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscovery.java` - Makes sanitized discovery helpers public so plugin infrastructure can reuse the existing discovered-source contract rather than inventing a parallel one.

## Decisions Made

- Reused `ServiceLoaderExtensionDiscovery.DiscoveredSource` as the bridge output to keep the Phase 6 contribution registry as the source of truth for compatibility, duplicate capability, disabled source, ordering, and usable-capability rules.
- Used plugin state as an overlay on source resolution rather than PF4J unload semantics; `DISABLED` and `QUARANTINED` stop new usable capability resolution only.
- Kept governance records App-layer/public-boundary oriented (`PluginSourceStatus`, `PluginCapabilityStatus`) and did not expose PF4J wrapper/descriptor/classloader objects.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Reused ServiceLoader discovered-source helpers across module boundary**
- **Found during:** Task 1 (Bridge PF4J plugins into Pi ExtensionSource discovery)
- **Issue:** The existing discovery helper factory/sanitizer methods were package-private, which would force plugin infrastructure to duplicate discovered-source semantics and sanitization.
- **Fix:** Made `ServiceLoaderExtensionDiscovery.sanitize`, `DiscoveredSource.discovered`, and `DiscoveredSource.failed` public so PF4J bridge output mirrors Phase 6 discovery semantics directly.
- **Files modified:** `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscovery.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin -am -Dtest=Pf4jPluginSourceDiscoveryTest test`
- **Committed in:** `2a49b7a`

---

**Total deviations:** 1 auto-fixed (Rule 2 missing critical)
**Impact on plan:** The adjustment preserves the planned “reuse/mirror ServiceLoader semantics” requirement and avoids a parallel plugin-only discovery contract.

## Issues Encountered

- Because this agent ran in parallel with other executors, the initial `-am` test invocation observed unrelated in-flight App/client plugin governance test artifacts before their main classes were installed. Installing the upstream App module and rerunning the focused plugin tests resolved the local Maven reactor visibility issue. No plan code was changed for this.
- Pre-existing unrelated uncommitted planning files and `bun.lock` were present before/alongside this execution. They were left untouched and excluded from all task commits.

## Known Stubs

None. Empty strings in plugin status records represent intentional “no error/no reason/no previous state” values, and `<unknown>`/redacted path fallbacks are safety redaction behavior rather than UI/data stubs.

## Validation

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin -am -Dtest=Pf4jPluginSourceDiscoveryTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin -am -Dtest=PluginGovernanceCatalogAdapterTest test`

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Later Phase 8 plans can wire plugin governance into Adapter Web and Admin surfaces using the App-layer plugin status records without exposing PF4J types.
- Later sample plugin/product-path E2E can use `Pf4jPluginSourceDiscovery` to prove controlled plugin JAR load through existing extension and governed tool registry paths.
- Disable/quarantine semantics are ready for audited Admin operations while keeping hot unload guarantees explicitly out of scope.

## Self-Check: PASSED

- Verified expected files exist: PF4J bridge, PF4J source discovery, plugin state store, in-memory state store, plugin governance adapter, and this SUMMARY file.
- Verified task commits exist in git history: `d06b4e4`, `2a49b7a`, `70258df`, and `04c6441`.

---
*Phase: 08-controlled-dynamic-plugin-jars*
*Completed: 2026-06-16*
