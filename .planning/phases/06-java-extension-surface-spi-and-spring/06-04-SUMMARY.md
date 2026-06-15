---
phase: 06-java-extension-surface-spi-and-spring
plan: 04
subsystem: spring-boot-starter
tags: [java, spring-boot, autoconfiguration, extension-spi, serviceloader, applicationcontextrunner]

requires:
  - phase: 06-java-extension-surface-spi-and-spring
    provides: Framework-free extension API, App extension governance port, and ServiceLoader contribution registry from plans 06-01 through 06-03
provides:
  - Spring Boot 3.5 starter module for extension registration
  - AutoConfiguration.imports registration for Pi extension auto-configuration
  - pi.extensions configuration properties for enablement, platform API version, disabled sources/capabilities, and duplicate override opt-in
  - Auto-configured ServiceLoader discovery, deterministic Spring ExtensionSource bean merge, contribution registry, tool registry, model registry, and governance catalog beans
  - ApplicationContextRunner tests for starter behavior, bean overrides, duplicate capability failures, and disabled capability governance visibility
affects: [phase-06, cloud-server-composition, spring-extension-registration, admin-governance, phase-07-mcp, phase-08-plugins]

tech-stack:
  added: [spring-boot-autoconfigure, spring-boot-configuration-processor, spring-boot-test]
  patterns: [Boot 3.5 AutoConfiguration.imports, ConditionalOnMissingBean override seam, properties-driven extension filtering, deterministic Spring bean source merge]

key-files:
  created:
    - pi-agent-spring-boot-starter/pom.xml
    - pi-agent-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    - pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/PiAgentExtensionAutoConfiguration.java
    - pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/PiAgentExtensionProperties.java
    - pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/SpringExtensionSourceFactory.java
    - pi-agent-spring-boot-starter/src/test/java/io/github/pi_java/agent/spring/autoconfigure/PiAgentExtensionAutoConfigurationTest.java
  modified:
    - pom.xml
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscovery.java
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/DefaultExtensionContributionRegistry.java
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionToolRegistry.java
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionGovernanceCatalogAdapter.java

key-decisions:
  - "Keep the Spring integration in a dedicated starter module that depends outward on App/Extension/Infrastructure contracts while preserving Domain/App framework isolation."
  - "Merge Spring ExtensionSource beans into the existing ServiceLoader discovery output so all sources pass through one deterministic DefaultExtensionContributionRegistry."
  - "Preserve Spring bean provenance through tool and governance adapters using sourceKind metadata instead of creating a parallel Spring-only registry path."

patterns-established:
  - "External Boot applications opt into extension registration by including the starter and Boot imports metadata; no broad component scanning is required."
  - "Starter beans are override-friendly via @ConditionalOnMissingBean for discovery, contribution registry, ToolRegistry, ModelProviderRegistry, and ExtensionGovernanceCatalog."
  - "Disabled extension capabilities remain governance-visible while being excluded from usable registries."

requirements-completed: [EXT-02, EXT-03, EXT-05, WORK-06]

duration: 5m 46s
completed: 2026-06-15
---

# Phase 06 Plan 04: Spring Boot Starter Auto-Configuration Summary

**Boot 3.5 starter wiring that deterministically combines ServiceLoader and Spring Bean extension sources into governed registries.**

## Performance

- **Duration:** 5m 46s
- **Started:** 2026-06-15T23:33:48Z
- **Completed:** 2026-06-15T23:39:34Z
- **Tasks:** 3
- **Files modified:** 11

## Accomplishments

- Added `pi-agent-spring-boot-starter` as a reactor module after `pi-agent-infrastructure-extension` with explicit starter dependencies and Boot 3.5 auto-configuration imports metadata.
- Implemented `PiAgentExtensionAutoConfiguration`, `PiAgentExtensionProperties`, and `SpringExtensionSourceFactory` under the starter module with no component-scan magic.
- Auto-configured `ServiceLoaderExtensionDiscovery`, `DefaultExtensionContributionRegistry`, `ExtensionToolRegistry`, `ExtensionModelProviderRegistry`, and `ExtensionGovernanceCatalogAdapter` with override-friendly conditions.
- Extended ServiceLoader discovery to accept explicitly supplied Spring `ExtensionSource` beans, producing one ordered discovery list and one contribution registry.
- Added ApplicationContextRunner tests proving starter bean creation, user-provided bean override, duplicate capability failure, disabled capability filtering, and governance visibility.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Spring Boot starter module and imports file** - `262876b` (feat)
2. **Task 2: Auto-configure ServiceLoader and Spring Bean extension sources** - `f142420` (feat)
3. **Task 3: Test starter behavior with ApplicationContextRunner** - `3e2edfe` (test)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pom.xml` - Adds `pi-agent-spring-boot-starter` to the parent reactor after `pi-agent-infrastructure-extension`.
- `pi-agent-spring-boot-starter/pom.xml` - Defines the Boot starter module with Extension API, infrastructure-extension, App, autoconfigure, configuration processor, and test dependencies.
- `pi-agent-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` - Registers the starter auto-configuration class using Boot 3.5 metadata.
- `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/PiAgentExtensionAutoConfiguration.java` - Auto-configures extension discovery, contribution registry, tool/model registries, and governance catalog.
- `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/PiAgentExtensionProperties.java` - Binds `pi.extensions` enablement, platform API version, disabled sources/capabilities, and duplicate override settings.
- `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/SpringExtensionSourceFactory.java` - Collects ordered Spring `ExtensionSource` beans without component scanning.
- `pi-agent-spring-boot-starter/src/test/java/io/github/pi_java/agent/spring/autoconfigure/PiAgentExtensionAutoConfigurationTest.java` - ApplicationContextRunner coverage for starter behavior.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscovery.java` - Adds merge support for explicit extension sources alongside ServiceLoader results.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/DefaultExtensionContributionRegistry.java` - Retains source references for governance provenance mapping.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionToolRegistry.java` - Preserves Spring bean provenance when source metadata requests `SPRING_BEAN`.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionGovernanceCatalogAdapter.java` - Maps governance source/capability kind from source metadata instead of hardcoding SPI for Spring-sourced contributions.

## Decisions Made

- Kept Spring starter code in `pi-agent-spring-boot-starter` so Spring Boot applications and the future Cloud Server composition root can share the same integration path.
- Used `ObjectProvider<ExtensionSource>.orderedStream()` for deterministic Spring bean source collection, then merged those sources through ServiceLoader discovery sorting and the existing contribution registry.
- Preserved source provenance as metadata-driven `SPRING_BEAN` in adapters rather than forking the extension registry by source mechanism.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Preserved Spring Bean provenance in existing registry adapters**
- **Found during:** Task 3 (Test starter behavior with ApplicationContextRunner)
- **Issue:** The existing infrastructure adapters hardcoded extension provenance/governance kind as `SPI`, so Spring bean extension sources were usable but not distinguishable from ServiceLoader sources as required by the starter tests.
- **Fix:** Retained source references in contribution entries and mapped `sourceKind=SPRING_BEAN` metadata through `ExtensionToolRegistry` and `ExtensionGovernanceCatalogAdapter`.
- **Files modified:** `DefaultExtensionContributionRegistry.java`, `ExtensionToolRegistry.java`, `ExtensionGovernanceCatalogAdapter.java`, `PiAgentExtensionAutoConfigurationTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-spring-boot-starter -am -Dtest=PiAgentExtensionAutoConfigurationTest test`
- **Committed in:** `3e2edfe`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Required for correctness of the planned Spring Bean merge/provenance behavior; no extra feature scope was introduced.

## Issues Encountered

- The duplicate capability failure test intentionally logs a Spring context initialization warning because ApplicationContextRunner asserts that duplicate capability IDs fail by default.
- Maven emits existing SLF4J no-provider warnings from upstream test dependencies; tests pass and no logging dependency was added by this starter plan.
- Pre-existing unrelated planning-file changes and `bun.lock` remain in the working tree from other parallel work; they were not staged or modified by this plan.

## Known Stubs

None. The starter path is wired to concrete ServiceLoader/Spring source discovery and concrete extension registry adapters; no UI-blocking or mock-only data path was introduced.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-spring-boot-starter -am test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-spring-boot-starter -am -Dtest=PiAgentExtensionAutoConfigurationTest test`

## Next Phase Readiness

- Plan 06-05 can add annotation-based Spring extension registration on top of the starter and `ExtensionSource` bean merge seam.
- Plan 06-06 can wire the Cloud Server to consume this starter path instead of bespoke internal extension governance wiring.
- Later MCP/plugin work can continue publishing capabilities through the same contribution registry and governed ToolRegistry/GovernanceCatalog adapters.

## Self-Check: PASSED

- Verified key files exist: `06-04-SUMMARY.md`, `pi-agent-spring-boot-starter/pom.xml`, `PiAgentExtensionAutoConfiguration.java`, and `PiAgentExtensionAutoConfigurationTest.java`.
- Verified task commits exist: `262876b`, `f142420`, and `3e2edfe`.

---
*Phase: 06-java-extension-surface-spi-and-spring*
*Completed: 2026-06-15*
