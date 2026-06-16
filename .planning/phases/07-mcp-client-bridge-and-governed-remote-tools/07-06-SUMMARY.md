---
phase: 07-mcp-client-bridge-and-governed-remote-tools
plan: 06
subsystem: adapter-web
tags: [mcp, cloud-server, admin-governance, tool-registry, spring]

requires:
  - phase: 07-mcp-client-bridge-and-governed-remote-tools
    provides: MCP configuration, discovery registry, governance catalog adapter, and governed remote executor binding from Plans 07-01 through 07-05
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: primary ToolRegistry and ToolExecutionGateway path for all tool sources
provides:
  - Adapter Web dependency and Spring composition root for MCP server properties, discovery registry, tool registry, and governance catalog
  - Primary Cloud Server ToolRegistry composition including built-ins, extensions, and MCP tools in deterministic order
  - Admin Governance REST endpoints for MCP status and read-only refresh action
affects: [phase-07-mcp-e2e, phase-05-admin-governance, phase-08-plugin-tool-sources]

tech-stack:
  added:
    - pi-agent-infrastructure-mcp dependency in pi-agent-adapter-web
  patterns:
    - Spring Adapter composition over Infrastructure MCP bridge with App/client DTO boundaries
    - Primary composite ToolRegistry with first-registry-wins duplicate handling
    - Read-only Admin governance refresh endpoint without configuration mutation

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/McpGovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpToolRegistryWiringTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpGovernanceApiTest.java
  modified:
    - pi-agent-adapter-web/pom.xml
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpServerRegistry.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpGovernanceCatalogAdapter.java

key-decisions:
  - "Keep MCP SDK/client types in pi-agent-infrastructure-mcp while Adapter Web only composes typed properties, registry beans, and App/client governance DTOs."
  - "Make the primary ToolRegistry compose built-ins first, extension tools second, and MCP tools third so remote tools cannot silently override built-ins."
  - "Expose Admin MCP refresh as POST /api/admin/governance/mcp/refresh because it triggers rediscovery state only, not server configuration CRUD."

patterns-established:
  - "MCP Cloud Server wiring mirrors extension wiring: source-specific Infrastructure registry behind one App ToolRegistry and one governance catalog."
  - "Tests use fake MCP discovery handles and dev/test auth headers, requiring no network, model keys, or MCP server process."

requirements-completed: [MCP-01, MCP-02, MCP-03, MCP-04]

duration: 9m 37s
completed: 2026-06-16
---

# Phase 07 Plan 06: MCP Cloud Server Wiring and Admin Governance Summary

**Cloud Server now composes configured MCP discovery into the governed tool registry and exposes read-only MCP Admin status plus refresh endpoints.**

## Performance

- **Duration:** 9m 37s
- **Started:** 2026-06-16T09:24:08Z
- **Completed:** 2026-06-16T09:33:45Z
- **Tasks:** 3
- **Files modified:** 8

## Accomplishments

- Added `pi-agent-infrastructure-mcp` as an Adapter Web dependency and introduced `McpGovernanceBeanConfiguration` as the Spring composition root for MCP properties, secret resolution, server registry, tool descriptor mapper, MCP tool registry, and MCP governance catalog.
- Bound typed MCP configuration under `pi.mcp.servers` plus `pi.mcp.discovery.startup`, with a safe empty registry/catalog when no MCP servers are configured and test-controlled startup discovery.
- Updated the primary `ToolRegistry` bean to compose built-ins first, extensions second, and MCP third, preserving existing first-registry-wins duplicate semantics for listing and resolution.
- Exposed `GET /api/admin/governance/mcp` and `POST /api/admin/governance/mcp/refresh` through the thin Admin Governance controller, delegating to `GovernanceQueryService` and returning `pi-agent-client` DTOs.
- Added fake MCP wiring/API tests proving no-network startup, fake MCP tool provenance through the final registry, MCP overview health, read-only refresh behavior, and absence of fake secret text in JSON.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Adapter Web MCP bean configuration** - `4439162` (feat)
2. **Task 2: Compose MCP tools into the governed ToolRegistry** - `6b15391` (feat)
3. **Task 3: Add Admin MCP governance REST endpoints** - `a872361` (feat)

## Files Created/Modified

- `pi-agent-adapter-web/pom.xml` - Adds Adapter Web dependency on `pi-agent-infrastructure-mcp`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/McpGovernanceBeanConfiguration.java` - Binds `pi.mcp` server/discovery properties and creates MCP registry/governance beans.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java` - Composes MCP registry into the primary governed `ToolRegistry` after built-ins and extensions.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java` - Adds MCP status and refresh endpoints.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpServerRegistry.java` - Makes the fakeable discovery-client constructor public for Adapter Web wiring tests.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpGovernanceCatalogAdapter.java` - Fixes refresh status count mapping to match public DTO semantics.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpToolRegistryWiringTest.java` - Verifies bean wiring, composite registry routing, and MCP provenance with fake discovery.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpGovernanceApiTest.java` - Verifies Admin MCP status/refresh REST behavior and redaction.

## Decisions Made

- Kept MCP secret resolution injected via the existing App `SecretResolver` seam and translated to `McpSecretHeaderResolver` only inside Adapter Web/Infrastructure boundaries.
- Made the composite `ToolRegistry` bean `@Primary` because the concrete `McpToolRegistry` is also a ToolRegistry bean and App/controller consumers must receive the composed product registry.
- Chose read-only `POST /api/admin/governance/mcp/refresh` for manual rediscovery; PUT/PATCH/DELETE on `/api/admin/governance/mcp` remain unsupported.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Made `McpServerRegistry` fakeable from Adapter Web tests**
- **Found during:** Task 2
- **Issue:** The existing fakeable `McpServerRegistry(List, DiscoveryClientFactory, Clock)` constructor was package-private inside the infrastructure module, preventing Adapter Web wiring tests from supplying no-network fake discovery.
- **Fix:** Made the constructor public while keeping the SDK-facing types inside `pi-agent-infrastructure-mcp`.
- **Files modified:** `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpServerRegistry.java`
- **Commit:** `6b15391`

**2. [Rule 1 - Bug] Fixed MCP refresh DTO count ordering**
- **Found during:** Task 3 API verification
- **Issue:** `McpGovernanceCatalogAdapter.refresh()` passed refreshed/failed/configured counts to `McpRefreshStatus` in the wrong order, making public JSON report `refreshedServerCount: 0` for a successful single-server refresh.
- **Fix:** Reordered constructor arguments to configured, refreshed, failed.
- **Files modified:** `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpGovernanceCatalogAdapter.java`
- **Commit:** `a872361`

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both fixes were required to complete the planned Adapter Web wiring/API verification and did not alter the public architecture or governance boundaries.

## Known Stubs

- None. Empty lists/maps/null checks in the new MCP configuration are defensive optional-configuration defaults, not UI-facing placeholder data. Empty-string redacted-error normalization in MCP registry projections is existing safe status normalization.

## Issues Encountered

- The exact non-reactor Task 1 command could not resolve the newly added `pi-agent-infrastructure-mcp` SNAPSHOT from the local Maven cache. The required plan-level reactor verification with `-am` was used and passed.
- Pre-existing unrelated uncommitted planning artifacts under Phase 02/Phase 03 and `bun.lock` were present before execution and left untouched.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=McpToolRegistryWiringTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=McpGovernanceApiTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=McpToolRegistryWiringTest,McpGovernanceApiTest test` — passed

## Auth Gates

None.

## User Setup Required

None - tests use fake MCP discovery and dev/test auth headers with no external MCP server, network, or model key requirement.

## Next Phase Readiness

- Plan 07-07 can add Web Console/Admin UI display for the real MCP governance DTOs without changing REST contracts.
- Plan 07-08 can build no-key fake MCP E2E around the same composed ToolRegistry and ToolExecutionGateway path.

## Self-Check: PASSED

- Found summary file: `.planning/phases/07-mcp-client-bridge-and-governed-remote-tools/07-06-SUMMARY.md`
- Found key files: `McpGovernanceBeanConfiguration.java`, `McpToolRegistryWiringTest.java`, `McpGovernanceApiTest.java`, `AdminGovernanceController.java`
- Found task commits: `4439162`, `6b15391`, `a872361`

---
*Phase: 07-mcp-client-bridge-and-governed-remote-tools*
*Completed: 2026-06-16*
