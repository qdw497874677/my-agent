---
phase: 06-java-extension-surface-spi-and-spring
plan: 02
subsystem: governance
tags: [java, cola, extension-governance, spi, spring, admin-api]

requires:
  - phase: 05-agent-web-console-and-runtime-cockpit
    provides: Admin Governance read-only overview API and Phase 5 extension placeholder surface
  - phase: 06-java-extension-surface-spi-and-spring
    provides: Extension API contracts from Plan 01 for downstream SPI/Spring discovery
provides:
  - Public client extension governance DTO envelope, source DTO, and capability DTO
  - Framework-free App extension governance catalog port and status records
  - GovernanceQueryService extension detail query and catalog-derived overview status
affects: [phase-06-spi-spring-discovery, phase-07-mcp-governance, phase-08-plugin-governance, admin-governance]

tech-stack:
  added: []
  patterns: [plain Java records for public read models, App-layer read-only governance port, constructor-injected catalog fallback]

key-files:
  created:
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/ExtensionGovernanceResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/ExtensionSourceDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/ExtensionCapabilityDto.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/ExtensionGovernanceCatalog.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/ExtensionSourceStatus.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/ExtensionCapabilityStatus.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/EmptyExtensionGovernanceCatalog.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryServiceExtensionTest.java
  modified:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java

key-decisions:
  - "Expose extension governance as read-only public DTOs with Map<String, String> metadata only; Admin mutation controls remain disabled/deferred."
  - "Keep extension governance catalog in App using App-owned string statuses so App does not depend on extension SDK or discovery implementations."
  - "Wire Adapter Web with EmptyExtensionGovernanceCatalog as the safe fallback while concrete SPI/Spring discovery modules provide real catalog data later."

patterns-established:
  - "Extension governance data flows ExtensionGovernanceCatalog -> DefaultGovernanceQueryService -> pi-agent-client DTOs."
  - "Governance overview status uses source/capability counts and redacted health metadata instead of Phase 5 placeholders."

requirements-completed: [EXT-03, EXT-04]

duration: 6m 33s
completed: 2026-06-15
---

# Phase 06 Plan 02: Extension Governance Read Model Summary

**Read-only extension governance DTOs and App catalog port replace the Phase 5 extension placeholder with redacted source/capability status.**

## Performance

- **Duration:** 6m 33s
- **Started:** 2026-06-15T23:14:10Z
- **Completed:** 2026-06-15T23:20:43Z
- **Tasks:** 3
- **Files modified:** 12

## Accomplishments

- Added public client records for extension governance responses, sources, and capabilities without Domain/App/Spring/Jakarta/SDK imports.
- Added a framework-free App-layer `ExtensionGovernanceCatalog` plus status records and `EmptyExtensionGovernanceCatalog` fallback.
- Integrated extension governance into `GovernanceQueryService`, including a read-only `extensions(RequestContext)` detail query and catalog-derived overview status/count metadata.
- Added Adapter Web fallback wiring and `/api/admin/governance/extensions` read-only endpoint so the Admin API can expose the new public read model.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define public extension governance DTOs** - `f07eab4` (feat)
2. **Task 2: Add App extension governance port and default empty catalog** - `5ca90cc` (feat)
3. **Task 3: Integrate extension governance into GovernanceQueryService** - `9651c44` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/ExtensionGovernanceResponse.java` - Public response envelope for extension governance sources.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/ExtensionSourceDto.java` - Redacted extension source public status record.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/ExtensionCapabilityDto.java` - Redacted extension capability public status record with `Map<String, String>` metadata.
- `pi-agent-client/src/test/java/io/github/pi_java/agent/client/admin/ExtensionGovernanceDtoContractTest.java` - DTO contract coverage for public records and metadata shape.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/ExtensionGovernanceCatalog.java` - Read-only App port for extension governance source status.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/ExtensionSourceStatus.java` - App-owned source status record.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/ExtensionCapabilityStatus.java` - App-owned capability status record.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/EmptyExtensionGovernanceCatalog.java` - Safe fallback preserving unconfigured behavior until discovery is wired.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/port/ExtensionGovernanceCatalogContractTest.java` - App port contract coverage for read-only/framework-free behavior.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java` - Added `extensions(RequestContext)` query.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java` - Maps catalog statuses to public DTOs and derives extension overview status.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryServiceExtensionTest.java` - Verifies redacted mapping and failed/incompatible/disabled counts.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java` - Wires empty catalog fallback and injects the catalog into governance service.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java` - Adds read-only extension governance endpoint.

## Decisions Made

- Expose extension governance as read-only public DTOs with `Map<String, String>` metadata only; Admin mutation controls remain disabled/deferred.
- Keep extension governance catalog in App using App-owned string statuses so App does not depend on extension SDK or discovery implementations.
- Wire Adapter Web with `EmptyExtensionGovernanceCatalog` as the safe fallback while concrete SPI/Spring discovery modules provide real catalog data later.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added Adapter Web fallback bean and read-only endpoint**
- **Found during:** Task 3 (Integrate extension governance into GovernanceQueryService)
- **Issue:** Constructor-injecting `ExtensionGovernanceCatalog` into `DefaultGovernanceQueryService` would leave the Spring composition root without a catalog bean and no HTTP detail route for the new read-only `extensions(RequestContext)` query.
- **Fix:** Added `EmptyExtensionGovernanceCatalog` fallback bean in `GovernanceBeanConfiguration` and exposed `/api/admin/governance/extensions` as a GET-only endpoint.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java`, `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -DskipTests compile`
- **Committed in:** `9651c44`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Required to keep the application composition root runnable after adding the new App port; no mutation controls or extra governance features were introduced.

## Known Stubs

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/EmptyExtensionGovernanceCatalog.java` returns `List.of()` intentionally as the safe unconfigured fallback until concrete SPI/Spring discovery catalogs are wired in later Phase 6 plans.
- `ExtensionSourceStatus` and `ExtensionCapabilityStatus` normalize optional version/redacted-error fields to empty strings intentionally for DTO stability; these defaults do not block the plan goal.

## Issues Encountered

- Pre-existing unrelated working-tree changes were present in `.planning/STATE.md`, Phase 02 planning files, and `bun.lock`; they were not modified or staged by this plan.
- Maven emitted SLF4J no-provider warnings during App tests; tests passed and the warning is pre-existing/out of scope.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=DefaultGovernanceQueryServiceExtensionTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -DskipTests compile`

## Next Phase Readiness

- Concrete SPI and Spring discovery modules can now publish source/capability statuses through `ExtensionGovernanceCatalog` without changing public Admin DTOs.
- Admin Governance can show extension details through the new read-only response while MCP/plugin surfaces remain deferred placeholders.

## Self-Check: PASSED

- Verified summary and key created files exist.
- Verified task commits exist: `f07eab4`, `5ca90cc`, `9651c44`.

---
*Phase: 06-java-extension-surface-spi-and-spring*
*Completed: 2026-06-15*
