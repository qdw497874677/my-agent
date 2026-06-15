---
phase: 06-java-extension-surface-spi-and-spring
plan: 03
subsystem: extension-infrastructure
tags: [java-spi, serviceloader, extension-registry, tool-registry, model-provider-registry, governance]

requires:
  - phase: 06-java-extension-surface-spi-and-spring
    provides: Framework-free extension API and App governance read models from plans 06-01 and 06-02
provides:
  - ServiceLoader-based ExtensionSource discovery with immutable deterministic results and failed-source visibility
  - Deterministic extension contribution registry with compatibility checks, disablement, duplicate handling, and safe error summaries
  - ToolRegistry, ModelProviderRegistry, and ExtensionGovernanceCatalog adapters over normalized extension capabilities
affects: [phase-06, phase-07-mcp, phase-08-plugins, extension-governance, tool-governance]

tech-stack:
  added: [Java ServiceLoader, Maven module pi-agent-infrastructure-extension]
  patterns: [descriptor-first tool adapter, source/capability contribution registry, governance read-model adapter]

key-files:
  created:
    - pi-agent-infrastructure-extension/pom.xml
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscovery.java
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/DefaultExtensionContributionRegistry.java
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionRegistrationProperties.java
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionCompatibilityChecker.java
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionToolRegistry.java
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionModelProviderRegistry.java
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionGovernanceCatalogAdapter.java
    - pi-agent-infrastructure-extension/src/test/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscoveryTest.java
    - pi-agent-infrastructure-extension/src/test/java/io/github/pi_java/agent/infrastructure/extension/ExtensionContributionRegistryTest.java
  modified:
    - pom.xml

key-decisions:
  - "Keep SPI discovery in a separate infrastructure module with no Spring/PF4J/MCP dependencies."
  - "Represent failed ServiceLoader providers as failed source statuses so governance retains safe error visibility."
  - "Expose extension tools only through ToolRegistry.resolve with ToolExecutorBinding, preserving the governed execution path."

patterns-established:
  - "Extension sources and capabilities are ordered by metadata order, then source/capability id for deterministic registration."
  - "Disabled, incompatible, and failed extension contributions stay visible in governance but are omitted from usable registries."
  - "Extension tool provenance is normalized to ToolProvenance.SourceKind.SPI with source and capability IDs in metadata."

requirements-completed: [EXT-01, EXT-03, EXT-05, WORK-06]

duration: 7m 22s
completed: 2026-06-15
---

# Phase 06 Plan 03: Java ServiceLoader Discovery and Normalized Registration Summary

**Java SPI extension discovery with deterministic contribution registration and governed Tool/Model/Governance adapters.**

## Performance

- **Duration:** 7m 22s
- **Started:** 2026-06-15T23:23:51Z
- **Completed:** 2026-06-15T23:31:13Z
- **Tasks:** 3
- **Files modified:** 12

## Accomplishments

- Added `pi-agent-infrastructure-extension` to the Maven reactor as the non-Spring/non-PF4J Java extension infrastructure module.
- Implemented ServiceLoader discovery for `ExtensionSource`, including deterministic ordering, immutable output, and failed-provider status capture with redacted error summaries.
- Implemented a contribution registry that sorts, disables, compatibility-checks, duplicate-checks, and separates governance-visible contributions from usable runtime registrations.
- Added registry adapters that normalize extension tool capabilities into `ToolRegistry`, model provider capabilities into `ModelProviderRegistry`, and source/capability status into `ExtensionGovernanceCatalog`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add infrastructure extension module and ServiceLoader discovery** - `7031f8a` (feat)
2. **Task 2: Build compatibility, disablement, ordering, and duplicate registration core** - `9f82097` (feat)
3. **Task 3: Adapt extension contributions to existing registries and governance** - `29ad375` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pom.xml` - Adds `pi-agent-infrastructure-extension` after the base infrastructure module in the reactor.
- `pi-agent-infrastructure-extension/pom.xml` - Declares explicit dependencies on Domain, App, and Extension API only, plus test libraries.
- `ServiceLoaderExtensionDiscovery.java` - Discovers `ExtensionSource` implementations via Java `ServiceLoader`, sorts sources, returns immutable status entries, and preserves failed-load visibility.
- `DefaultExtensionContributionRegistry.java` - Builds deterministic source/capability entries, applies disablement and compatibility checks, and enforces duplicate capability policy.
- `ExtensionRegistrationProperties.java` - Defines disabled source/capability sets, override opt-in, and platform API version.
- `ExtensionCompatibilityChecker.java` - Parses exact and inclusive/exclusive range compatibility strings and checks platform API support.
- `ExtensionToolRegistry.java` - Adapts usable `ToolExtensionCapability` entries into the existing `ToolRegistry` contract and normalizes SPI provenance.
- `ExtensionModelProviderRegistry.java` - Adapts usable model provider extension metadata into provider/model descriptors without raw secrets or invocation paths.
- `ExtensionGovernanceCatalogAdapter.java` - Maps all visible source/capability entries into App governance statuses.
- `ServiceLoaderExtensionDiscoveryTest.java` - Covers ServiceLoader fixtures, deterministic discovery, immutability, and failed-provider visibility.
- `ExtensionContributionRegistryTest.java` - Covers ordering, duplicate failures/overrides, disablement, compatibility, and failed-source retention.

## Decisions Made

- Kept SPI discovery in a separate infrastructure module with no Spring/PF4J/MCP dependencies so Plan 06-03 validates the pure Java extension path before starter/plugin work.
- Chose safe failed-source statuses instead of throwing discovery failures away; governance can show exception class and sanitized message without stack traces.
- Kept model provider extension support descriptor-only in this plan; no provider invocation path was introduced.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrected ServiceLoader test assumptions for ordering and classloader parent behavior**
- **Found during:** Task 1 (ServiceLoader discovery verification)
- **Issue:** Initial tests expected alpha before beta despite explicit order metadata and expected a child classloader to see only failed fixtures despite parent test resources also contributing providers.
- **Fix:** Updated assertions to match deterministic order-by-metadata behavior and to filter failed entries while allowing inherited discovered fixtures.
- **Files modified:** `pi-agent-infrastructure-extension/src/test/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscoveryTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-extension -am -Dtest=ServiceLoaderExtensionDiscoveryTest test`
- **Committed in:** `7031f8a`

**2. [Rule 1 - Bug] Qualified nested record order helper calls**
- **Found during:** Task 2 (contribution registry verification)
- **Issue:** Nested record accessor methods shadowed the static `order(Map)` helper, causing compilation failure.
- **Fix:** Qualified helper calls as `DefaultExtensionContributionRegistry.order(...)`.
- **Files modified:** `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/DefaultExtensionContributionRegistry.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-extension -am -Dtest=ExtensionContributionRegistryTest test`
- **Committed in:** `9f82097`

---

**Total deviations:** 2 auto-fixed (2 Rule 1 bugs)
**Impact on plan:** Both fixes were required for correctness and did not change planned scope.

## Issues Encountered

- Final module verification emits SLF4J no-provider warnings from upstream test dependencies; tests pass and no runtime dependency was added to this non-Spring module.
- Pre-existing unrelated planning files and `bun.lock` are present in the working tree from other agents; they were not modified or committed by this plan.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. Empty strings in governance DTO mapping are intentional App DTO defaults for optional version/error fields, not UI-blocking placeholders.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-extension -am -Dtest=ServiceLoaderExtensionDiscoveryTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-extension -am -Dtest=ExtensionContributionRegistryTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-extension -am test`

## Next Phase Readiness

- Plan 06-04 can build Spring Boot starter registration on top of `DefaultExtensionContributionRegistry` and the registry/governance adapters.
- Plan 06-06 can replace the Adapter Web empty extension governance fallback with the concrete `ExtensionGovernanceCatalogAdapter` through starter wiring.

## Self-Check: PASSED

- Found created module and key implementation files: `pi-agent-infrastructure-extension/pom.xml`, `ServiceLoaderExtensionDiscovery.java`, `DefaultExtensionContributionRegistry.java`, and `ExtensionToolRegistry.java`.
- Found task commits in git history: `7031f8a`, `9f82097`, and `29ad375`.

---
*Phase: 06-java-extension-surface-spi-and-spring*
*Completed: 2026-06-15*
