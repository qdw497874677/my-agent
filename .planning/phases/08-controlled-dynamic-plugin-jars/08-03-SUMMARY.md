---
phase: 08-controlled-dynamic-plugin-jars
plan: 03
subsystem: app-governance
tags: [java, cola, plugin-governance, admin-dto, tdd]

requires:
  - phase: 08-controlled-dynamic-plugin-jars
    provides: Isolated PF4J/plugin infrastructure boundary and controlled-directory configuration from 08-01
  - phase: 06-java-extension-surface-spi-and-spring
    provides: Extension lifecycle, capability, health, compatibility, and governance vocabulary reused for plugins
provides:
  - App-layer PluginGovernanceCatalog port for plugin status, refresh, disable, and quarantine operations
  - Public pi-agent-client plugin governance DTOs with defensive collection copies and redacted string-only metadata
  - GovernanceQueryService plugin query/mutation methods and overview status mapping without the Phase 5 placeholder
affects: [adapter-web, admin-rest, admin-ui, plugin-infrastructure, phase-08]

tech-stack:
  added: []
  patterns: [COLA App port, public client DTO boundary, TDD, narrow audited mutation seam]

key-files:
  created:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/PluginGovernanceCatalog.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/PluginSourceStatus.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/PluginCapabilityStatus.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/PluginMutationStatus.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/EmptyPluginGovernanceCatalog.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginGovernanceResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginSourceDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginCapabilityDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginMutationRequest.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginMutationResponse.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/PluginGovernanceQueryServiceTest.java
  modified:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryServiceExtensionTest.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/GovernanceQueryServiceMcpTest.java

key-decisions:
  - "Plugin governance uses a dedicated App port and public client DTO set so PF4J/plugin infrastructure remains outside App/client APIs."
  - "Plugin mutations are intentionally limited to refresh, disable, and quarantine; upload/install/delete/upgrade remain out of scope."
  - "Overview plugin status is derived from PluginGovernanceCatalog and reports plugin/capability, disabled, quarantined, incompatible, and failed counts instead of a placeholder."

patterns-established:
  - "Plugin governance mirrors extension/MCP governance: App-owned status records map explicitly into pi-agent-client DTO records."
  - "Mutation request/response contracts are operation-scoped and redacted, carrying actor/reason only through narrow use-case seams."

requirements-completed: [PLUG-03, PLUG-04, PLUG-05]

duration: 7m 10s
completed: 2026-06-16
---

# Phase 08 Plan 03: Plugin Governance Contracts Summary

**App/client plugin governance contracts with refresh, disable, quarantine seams and real Admin overview status mapping**

## Performance

- **Duration:** 7m 10s
- **Started:** 2026-06-16T17:22:05Z
- **Completed:** 2026-06-16T17:29:15Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments

- Added framework-free App plugin governance records and `PluginGovernanceCatalog` for plugin status, capability status, refresh, disable, and quarantine.
- Added public `pi-agent-client` plugin governance DTOs using only strings, booleans, instants, lists, and maps with defensive copies.
- Extended `GovernanceQueryService` and `DefaultGovernanceQueryService` with plugin query/mutation methods and replaced the Phase 5 plugin placeholder in overview status.
- Updated adapter-web governance bean wiring to provide `EmptyPluginGovernanceCatalog` as the safe fallback until concrete plugin infrastructure wiring is active.
- Added focused TDD coverage for contract defensive copying, empty/fake catalog mapping, status counts, mutation delegation, and existing extension/MCP constructor regressions.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add App plugin governance port and client DTO contracts tests** - `940ec37` (test)
2. **Task 1 GREEN: Add App plugin governance port and client DTO contracts** - `eed874f` (feat)
3. **Task 2 RED: Map plugin governance through GovernanceQueryService tests** - `572805f` (test)
4. **Task 2 GREEN: Map plugin governance through GovernanceQueryService** - `b3ca3dd` (feat)

**Plan metadata:** `5053b11` (docs)

## Files Created/Modified

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/PluginGovernanceCatalog.java` - App port for plugin status and refresh/disable/quarantine governance operations.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/PluginSourceStatus.java` - App read model for plugin identity, source kind, lifecycle, health, compatibility, counts, redacted error, relative path, reason, capabilities, and metadata.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/PluginCapabilityStatus.java` - App read model for plugin capability status and metadata.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/PluginMutationStatus.java` - App mutation result for refresh/disable/quarantine.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/plugin/EmptyPluginGovernanceCatalog.java` - Safe no-plugin fallback catalog.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginGovernanceResponse.java` - Public plugin governance response boundary.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginSourceDto.java` - Public plugin source status DTO.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginCapabilityDto.java` - Public plugin capability status DTO.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginMutationRequest.java` - Public mutation request limited to `refresh`, `disable`, and `quarantine`.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PluginMutationResponse.java` - Public redacted mutation response DTO.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java` - Added plugin governance query/mutation use-case methods.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java` - Injects plugin catalog, maps plugin DTOs, delegates mutations, and reports overview counts.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java` - Wires optional plugin catalog with empty fallback.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/PluginGovernanceQueryServiceTest.java` - Focused TDD tests for contracts and service mapping.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryServiceExtensionTest.java` - Updated constructor wiring for plugin catalog dependency.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/GovernanceQueryServiceMcpTest.java` - Updated constructor wiring for plugin catalog dependency.

## Decisions Made

- Plugin governance has its own App port rather than extending extension governance, keeping plugin operational controls explicit while still reusing Phase 6 lifecycle/health/compatibility language.
- Public DTOs intentionally expose only redacted summary fields and simple Java types; PF4J, classloaders, Domain, Spring, Vaadin, and infrastructure types do not cross the boundary.
- `DISABLED` is counted separately from `QUARANTINED`; overview metadata includes both, plus incompatible and failed counts for Admin Governance readiness.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated adapter-web composition for new plugin catalog dependency**
- **Found during:** Task 2 (Map plugin governance through GovernanceQueryService)
- **Issue:** Adding `PluginGovernanceCatalog` to `DefaultGovernanceQueryService` required the Spring composition root to provide a fallback when concrete plugin infrastructure is not yet wired.
- **Fix:** Updated `GovernanceBeanConfiguration` to accept `Optional<PluginGovernanceCatalog>` and fall back to `EmptyPluginGovernanceCatalog`.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client,pi-agent-app -am -Dtest=PluginGovernanceQueryServiceTest,DefaultGovernanceQueryServiceExtensionTest,GovernanceQueryServiceMcpTest test`
- **Committed in:** `b3ca3dd`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Required for compilation and runtime composition safety. No scope creep beyond the planned plugin governance boundary.

## Issues Encountered

- TDD RED for Task 1 failed as expected because plugin App ports and public DTOs did not exist.
- TDD RED for Task 2 failed as expected because `GovernanceQueryService` had no plugin methods and `DefaultGovernanceQueryService` still used the plugin placeholder.
- One implementation iteration counted quarantined plugins as disabled because both were not enabled; corrected the overview metadata so `disabledPlugins` counts only `DISABLED` lifecycle status.

## Known Stubs

None. Empty strings and empty collections in the new records are defensive defaults for optional redacted summaries/reasons and do not feed UI as mock data.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client,pi-agent-app -am -Dtest=PluginGovernanceQueryServiceTest,DefaultGovernanceQueryServiceExtensionTest,GovernanceQueryServiceMcpTest test`
- Confirmed `DefaultGovernanceQueryService.java` no longer contains `FUTURE_ENABLED` or `Dynamic plugin governance arrives`.

## Next Phase Readiness

- Adapter REST/UI and plugin infrastructure plans can consume `GovernanceQueryService.plugins()`, `refreshPlugins()`, `disablePlugin()`, and `quarantinePlugin()` without leaking PF4J or infrastructure types.
- The App/client mutation seam is ready for audited infrastructure implementation in later Phase 8 plans.

## Self-Check: PASSED

- Verified summary and key files exist: `08-03-SUMMARY.md`, `PluginGovernanceCatalog.java`, `PluginGovernanceResponse.java`, and `DefaultGovernanceQueryService.java`.
- Verified task commits exist: `940ec37`, `eed874f`, `572805f`, and `b3ca3dd`.

---
*Phase: 08-controlled-dynamic-plugin-jars*
*Completed: 2026-06-16*
