---
phase: 07-mcp-client-bridge-and-governed-remote-tools
plan: 02
subsystem: governance
tags: [mcp, admin-governance, app-port, client-dto, cola]

requires:
  - phase: 05-agent-web-console-and-runtime-cockpit
    provides: inspect-only Admin Governance overview contracts and placeholder MCP status slot
  - phase: 06-java-extension-surface-spi-and-spring
    provides: extension governance source/capability/read-only mapping pattern reused for MCP governance
provides:
  - Framework-free App MCP governance catalog with server, tool, refresh, and empty fallback contracts
  - Public Admin MCP governance and refresh DTO records with defensive copy/validation behavior
  - GovernanceQueryService MCP read and refresh seams with overview status derived from a catalog
affects: [phase-07-mcp-client-bridge, phase-05-admin-governance, phase-08-plugin-governance]

tech-stack:
  added: []
  patterns:
    - Constructor-injected App governance ports with empty safe fallback
    - Public client DTO records over App port records with explicit redacted metadata

key-files:
  created:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/mcp/McpGovernanceCatalog.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/mcp/McpServerStatus.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/mcp/McpToolStatus.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/mcp/McpRefreshStatus.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/McpGovernanceResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/McpServerDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/McpToolDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/McpRefreshResponse.java
    - pi-agent-client/src/test/java/io/github/pi_java/agent/client/admin/McpGovernanceDtoContractTest.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/GovernanceQueryServiceMcpTest.java
  modified:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryServiceExtensionTest.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java

key-decisions:
  - "Use App-owned string MCP statuses and redacted metadata so App/client contracts do not leak MCP SDK, Spring, Vaadin, or provider types."
  - "Keep MCP Admin governance read-only-plus-refresh: DTOs expose status and refresh result only, with no create/edit/delete configuration fields."
  - "Inject McpGovernanceCatalog into DefaultGovernanceQueryService and use EmptyMcpGovernanceCatalog as adapter fallback until concrete bridge wiring arrives."

patterns-established:
  - "MCP governance mirrors extension governance: App port records first, client DTO records second, use-case mapping owns public boundary."
  - "Overview status computes server/tool/disabled/unhealthy counts from catalog state instead of hardcoded Phase 5 placeholders."

requirements-completed: [MCP-01, MCP-04]

duration: 7m 33s
completed: 2026-06-16
---

# Phase 07 Plan 02: MCP Governance Contracts Summary

**MCP Admin governance read/refresh contracts with framework-free App ports, public DTOs, and catalog-derived overview health.**

## Performance

- **Duration:** 7m 33s
- **Started:** 2026-06-16T08:38:47Z
- **Completed:** 2026-06-16T08:46:20Z
- **Tasks:** 3
- **Files modified:** 14

## Accomplishments

- Added `McpGovernanceCatalog` plus MCP server, tool, and refresh status records under the App port layer with JDK-only types and an empty safe fallback.
- Added public Admin DTO records for MCP governance and refresh responses in `pi-agent-client`, including validation and defensive copies for lists/maps.
- Extended `GovernanceQueryService` and `DefaultGovernanceQueryService` with `mcp(context)` and `refreshMcp(context)`, and replaced the MCP overview placeholder with catalog-derived health/count metadata.
- Added contract and mapping tests proving healthy/failed MCP server visibility, tool metadata mapping, refresh response mapping, and absence of raw fake secret strings.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add App MCP governance catalog contracts** - `855e2b8` (feat)
2. **Task 2: Add public MCP governance DTO records** - `5fb5b30` (feat)
3. **Task 3: Extend GovernanceQueryService mapping for MCP** - `ff6dadc` (feat)

**Plan metadata:** pending final docs commit

_Note: TDD tasks were executed with failing tests first, then implementation commits. Test and implementation changes were committed together per task to preserve the plan's atomic task-commit requirement._

## Files Created/Modified

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/mcp/McpGovernanceCatalog.java` - App MCP governance catalog interface, overall status derivation, and empty fallback.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/mcp/McpServerStatus.java` - Framework-free MCP server health/discovery/read model.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/mcp/McpToolStatus.java` - Framework-free MCP tool availability and safety metadata read model.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/mcp/McpRefreshStatus.java` - App refresh result record for Admin-triggered refresh status.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/McpGovernanceResponse.java` - Public MCP governance response wrapper.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/McpServerDto.java` - Public MCP server DTO.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/McpToolDto.java` - Public MCP tool DTO.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/McpRefreshResponse.java` - Public MCP refresh response DTO.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java` - Added MCP read and refresh methods.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java` - Maps MCP App port status to public DTOs and overview status.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java` - Supplies `EmptyMcpGovernanceCatalog` when no concrete MCP catalog bean exists.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/GovernanceQueryServiceMcpTest.java` - App contract and mapping tests for MCP governance.
- `pi-agent-client/src/test/java/io/github/pi_java/agent/client/admin/McpGovernanceDtoContractTest.java` - Public DTO contract tests.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryServiceExtensionTest.java` - Updated constructor usage after adding the MCP catalog dependency.

## Decisions Made

- Used App-owned string MCP statuses and plain JDK records to keep MCP SDK/Spring/Vaadin/provider types out of App and client contracts.
- Preserved Admin's inspect-only boundary while adding refresh: no DTO contains create/edit/delete/configuration mutation fields.
- Kept concrete MCP bridge/adapters deferred; `EmptyMcpGovernanceCatalog` keeps current adapter wiring safe and unconfigured until later Phase 7 plans provide real catalog data.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

- `DefaultGovernanceQueryService.futureStatus(...)` still uses placeholder metadata for the `plugins` overview area. This is pre-existing Phase 5/8 scope and does not block Plan 07-02; MCP no longer uses that placeholder.
- Empty-string normalization in MCP DTO/status records (`authSummary`, `schemaSummary`, `redactedError`) is intentional optional-field normalization, not UI stub data.

## Issues Encountered

- The exact planned command `mvn -q -pl pi-agent-app -Dtest=GovernanceQueryServiceMcpTest test` initially loaded a stale installed `pi-agent-client` artifact without the new MCP DTOs. Installed the updated client module locally and verified the app test and full reactor command. The final required verification passed with `-am`.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client -am test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=GovernanceQueryServiceMcpTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-client,pi-agent-app -am test`

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Later Phase 7 plans can wire real MCP discovery/refresh adapters behind `McpGovernanceCatalog` without changing public Admin DTOs.
- Admin API/UI work can add endpoints/views for `mcp(context)` and `refreshMcp(context)` while preserving no-CRUD governance boundaries.

## Self-Check: PASSED

- Verified `07-02-SUMMARY.md` exists in the phase directory.
- Verified key App/client MCP governance files exist.
- Verified task commits are present in git history: `855e2b8`, `5fb5b30`, `ff6dadc`.

---
*Phase: 07-mcp-client-bridge-and-governed-remote-tools*
*Completed: 2026-06-16*
