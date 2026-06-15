---
phase: 06-java-extension-surface-spi-and-spring
plan: 06
subsystem: cloud-server-extension-governance
tags: [java, spring-boot-starter, admin-governance, vaadin, extension-spi, read-only-api]

requires:
  - phase: 06-java-extension-surface-spi-and-spring
    provides: Spring Boot starter, ServiceLoader/Spring extension registry, and annotation-derived extension source support from plans 06-04 and 06-05
provides:
  - Cloud Server Adapter Web dependency on pi-agent-spring-boot-starter
  - Deterministic composite built-in plus extension ToolRegistry and ModelProviderRegistry wiring
  - Starter-provided ExtensionGovernanceCatalog consumption by GovernanceQueryService
  - Authenticated read-only /api/admin/governance/extensions coverage with real fixture extension source data
  - Vaadin Admin Registry extension status rendering through ConsoleHttpClient public API anchors
affects: [phase-06, cloud-server-composition, admin-governance, extension-ui, phase-07-mcp, phase-08-plugins]

tech-stack:
  added: [pi-agent-spring-boot-starter dependency in adapter-web]
  patterns: [starter-consumer-product-path, deterministic composite registries, read-only extension governance DTO rendering, public REST client anchors]

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ExtensionGovernanceApiTest.java
  modified:
    - pi-agent-adapter-web/pom.xml
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java

key-decisions:
  - "Make Adapter Web consume the same pi-agent-spring-boot-starter extension path external Spring applications use."
  - "Keep built-in tools/providers ahead of extension contributions in deterministic composite registries without silently overriding existing built-ins."
  - "Expose extension status only through authenticated GET DTOs and read-only Vaadin labels; enable/disable remains configuration-driven."

patterns-established:
  - "Cloud Server product wiring validates the external starter integration path instead of using a bespoke extension governance fallback."
  - "Vaadin Admin Governance views add new extension data through ConsoleHttpClient public API type/path anchors only."
  - "Extension source/capability status rows include source kind, lifecycle, health, compatibility, enabled state, redacted errors, and capability metadata without mutation controls."

requirements-completed: [EXT-02, EXT-04, EXT-05]

duration: 6m 55s
completed: 2026-06-15
---

# Phase 06 Plan 06: Cloud Server Starter and Extension Governance Summary

**Cloud Server now consumes the Spring extension starter path and displays real read-only extension source/capability status through public Admin Governance APIs and Vaadin views.**

## Performance

- **Duration:** 6m 55s
- **Started:** 2026-06-15T23:51:05Z
- **Completed:** 2026-06-15T23:58:00Z
- **Tasks:** 3
- **Files modified:** 8

## Accomplishments

- Added `pi-agent-spring-boot-starter` to Adapter Web so the product Cloud Server validates the same extension starter/autoconfiguration path intended for enterprise Spring Boot applications.
- Replaced the adapter-local empty extension governance fallback with starter-provided `ExtensionGovernanceCatalog` injection, while retaining `@Configuration(proxyBeanMethods = false)` and `@ConditionalOnMissingBean` composition patterns.
- Composed built-in and extension tool/model registries in deterministic order so built-ins continue to work and extension contributions become visible/resolvable through the same App ports.
- Added focused authenticated API coverage for `GET /api/admin/governance/extensions`, including source kind, capability status, health/compatibility, enabled state, redacted error metadata, and GET-only semantics.
- Extended `ConsoleHttpClient` and `AdminRegistryStatusView` so Vaadin renders extension source/capability status via public DTO boundaries with no enable/disable/delete mutation controls.

## Task Commits

Each task was committed atomically:

1. **Task 1: Make Adapter Web consume extension starter beans** - `889b8cf` (feat)
2. **Task 2: Add read-only extension governance REST endpoint** - `a835604` (test)
3. **Task 3: Render extension status in Admin Governance UI** - `fade212` (feat)

**Plan metadata:** pending final docs commit

_Note: Existing endpoint/controller behavior was already present from earlier Phase 6 work; Task 2 added the focused product-path API fixture test required by this plan._

## Files Created/Modified

- `pi-agent-adapter-web/pom.xml` - Adds the Cloud Server dependency on `pi-agent-spring-boot-starter`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java` - Removes the adapter-local `EmptyExtensionGovernanceCatalog` fallback so starter-provided catalog beans are consumed.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java` - Composes built-in and extension `ToolRegistry` instances in deterministic order.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java` - Composes OpenAI-compatible and extension `ModelProviderRegistry` instances in deterministic order.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ExtensionGovernanceApiTest.java` - Adds authenticated extension governance API coverage using a Spring `ExtensionSource` fixture.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` - Adds public REST path/type anchors for extension governance DTOs.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java` - Adds read-only extension source and capability rendering.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java` - Covers public API path anchoring and inspect-only extension status rendering.

## Decisions Made

- Adapter Web now validates D-03 by depending on `pi-agent-spring-boot-starter` directly rather than maintaining an internal extension-only composition path.
- Built-in registries remain first in composite lookup order; extension contributions are appended and cannot silently replace existing built-ins through Adapter Web composition.
- Admin extension governance remains read-only: REST exposes only GET, and Vaadin renders labels/rows without mutation affordances.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added explicit composite registries for product starter consumption**
- **Found during:** Task 1 (Make Adapter Web consume extension starter beans)
- **Issue:** The starter auto-configures extension-only `ToolRegistry`/`ModelProviderRegistry` only when no bean exists, while Adapter Web must preserve built-ins and also consume extension contributions. Without explicit composition, the product path would either keep built-ins only or replace them with extension-only registries.
- **Fix:** Added deterministic Adapter Web composite registries that list/resolve built-in registries first and extension registries second when a `DefaultExtensionContributionRegistry` is available.
- **Files modified:** `ToolGovernanceBeanConfiguration.java`, `ModelProviderBeanConfiguration.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -DskipTests compile`
- **Committed in:** `889b8cf`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Required to satisfy D-11 and preserve built-in Cloud Server behavior while enabling extension contributions; no feature scope creep.

## Issues Encountered

- Task 2 controller method already existed from prior Phase 6 work, so this plan added the missing focused `ExtensionGovernanceApiTest` instead of duplicating controller logic.
- Maven test output includes existing Mockito dynamic-agent and Vaadin/Atmosphere startup warnings; tests passed and no dependency/runtime changes were needed.
- Pre-existing unrelated planning-file changes and `bun.lock` remain in the working tree from other parallel work; this plan did not stage or modify them.

## Known Stubs

None for the Plan 06 extension governance goal. MCP and dynamic plugin rows remain intentional Phase 7/8 placeholders from prior Admin Governance scope and were not expanded here.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -DskipTests compile`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ExtensionGovernanceApiTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AdminGovernanceViewsTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ExtensionGovernanceApiTest test && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -DskipTests compile`

## Next Phase Readiness

- Plan 06-07 can use the product Cloud Server starter path and extension governance API fixture as the basis for SPI/Spring conformance tests.
- Later MCP and plugin phases can reuse the same Admin Registry read-only source/capability rendering language while replacing their current placeholders with real governance catalogs.

## Self-Check: PASSED

- Verified key files exist: `06-06-SUMMARY.md` and `ExtensionGovernanceApiTest.java`.
- Verified task commits exist: `889b8cf`, `a835604`, and `fade212`.

---
*Phase: 06-java-extension-surface-spi-and-spring*
*Completed: 2026-06-15*
