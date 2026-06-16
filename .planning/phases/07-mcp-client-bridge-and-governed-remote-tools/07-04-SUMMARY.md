---
phase: 07-mcp-client-bridge-and-governed-remote-tools
plan: 04
subsystem: infra
tags: [java, mcp, tool-registry, governance, json-schema, redaction]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: canonical ToolDescriptor, ToolRegistry, ToolExecutorBinding, and governed execution contracts
  - phase: 07-mcp-client-bridge-and-governed-remote-tools
    provides: MCP server configuration, governance App ports, client factory seam, credential boundary, and sanitized client errors
provides:
  - Thread-safe MCP server discovery registry retaining configured server health and failed discovery visibility
  - MCP tool descriptor mapper with server-qualified IDs, JSON Schema passthrough, provenance, scopes, and conservative safety metadata
  - ToolRegistry adapter for available MCP tools plus redacted MCP governance catalog status adapter
affects: [phase-07-mcp-execution, phase-07-admin-governance, phase-09-hardening]

tech-stack:
  added: []
  patterns: [immutable-discovery-snapshots, sdk-to-domain-descriptor-mapping, governance-status-adapter, deferred-executor-seam]

key-files:
  created:
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpServerRegistry.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpDiscoveryResult.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolDescriptorMapper.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistry.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpGovernanceCatalogAdapter.java
    - pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistryTest.java
  modified:
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactory.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientHandle.java
    - pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactoryTest.java

key-decisions:
  - "Keep MCP discovery state as replace-all immutable per-server snapshots so manual refresh can safely update tool availability while reads remain deterministic."
  - "Normalize MCP tools into Pi ToolDescriptor records with IDs `mcp.<serverId>.<toolName>` and scopes `tool:mcp`, `mcp:server:<serverId>`, and `mcp:tool:<serverId>:<toolName>`."
  - "Preserve failed configured servers in governance status with sanitized errors, while ToolRegistry resolves only currently available tools."
  - "Use a deferred failing ToolExecutorBinding seam for Plan 07-04 so remote execution cannot bypass ToolExecutionGateway before Plan 07-05 implements the governed MCP executor."

patterns-established:
  - "Discovery registry owns MCP SDK-facing refresh state; ToolRegistry and governance adapters are read-model projections over the same snapshots."
  - "MCP annotation hints are copied into descriptor metadata and mapped conservatively: read-only closed-world tools become LOW/READ_ONLY; destructive/open-world/unknown tools remain external and higher risk."
  - "Failed refreshes retain stale tool names only as unavailable governance state; stale tools are not resolvable/executable."

requirements-completed: [MCP-02, MCP-04, MCP-05]

duration: 10m 57s
completed: 2026-06-16
---

# Phase 07 Plan 04: MCP Discovery Registry and Governed Descriptor Summary

**MCP discovery snapshots now project remote tools into Pi ToolDescriptors and redacted Admin governance status without exposing failed/stale tools for execution.**

## Performance

- **Duration:** 10m 57s
- **Started:** 2026-06-16T08:59:13Z
- **Completed:** 2026-06-16T09:10:10Z
- **Tasks:** 3
- **Files modified:** 9

## Accomplishments

- Added `McpServerRegistry` and `McpDiscoveryResult` to refresh configured MCP servers, store immutable snapshots, preserve disabled/failed configured servers, redact diagnostics, and mark stale tools unavailable after failed refresh.
- Added `McpToolDescriptorMapper` to normalize MCP SDK tool metadata into canonical Pi `ToolDescriptor` records with JSON Schema passthrough, MCP provenance, explicit allowlist scopes, server timeouts, and MCP annotation metadata.
- Added `McpToolRegistry` backed by discovery snapshots so only currently available MCP tools are listed/resolved, with a deferred executor seam that cannot perform remote execution before the governed executor plan.
- Added `McpGovernanceCatalogAdapter` mapping server/tool snapshots into App `McpServerStatus`, `McpToolStatus`, and `McpRefreshStatus` records with redacted failure visibility.
- Extended the client handle seam with `listTools()` while keeping MCP SDK types contained inside the MCP infrastructure module.

## Task Commits

TDD tasks were committed with RED/GREEN commits while preserving atomic task grouping:

1. **Task 1 RED: Implement MCP discovery registry state test** - `9b6304f` (test)
2. **Task 1 GREEN: Implement MCP discovery registry state** - `3d8a275` (feat)
3. **Task 2 RED: Map MCP tools into canonical ToolDescriptor records test** - `614354b` (test)
4. **Task 2 GREEN: Map MCP tools into canonical ToolDescriptor records** - `1cbbd3d` (feat)
5. **Task 3 RED: Expose MCP ToolRegistry and governance catalog adapters test** - `a347460` (test)
6. **Task 3 GREEN: Expose MCP ToolRegistry and governance catalog adapters** - `3c7c299` (feat)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpServerRegistry.java` - Configured server discovery state, refresh snapshots, stale-tool-unavailable handling, and sanitized failure capture.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpDiscoveryResult.java` - Refresh result record with configured/refreshed/failed counts and redacted error summary.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolDescriptorMapper.java` - MCP SDK tool-to-ToolDescriptor normalization, schema document passthrough, provenance/scopes/risk mapping, and annotation metadata preservation.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistry.java` - App `ToolRegistry` adapter listing/resolving only available MCP descriptors.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpGovernanceCatalogAdapter.java` - App `McpGovernanceCatalog` adapter for server/tool status and refresh delegation.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactory.java` - Extended initialized client seam to support MCP tool listing.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientHandle.java` - Added listTools forwarding while preserving public handle boundaries.
- `pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactoryTest.java` - Updated fake initialized client for the new discovery seam.
- `pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistryTest.java` - No-network/no-key tests for discovery refresh state, descriptor normalization, registry resolution, governance status, and secret redaction.

## Decisions Made

- Used replace-all per-server snapshots in `McpServerRegistry`; successful refresh replaces tools deterministically, and failed refresh retains prior tool names only as unavailable governance data.
- Used `ToolProvenance.SourceKind.MCP`, source id as server id, and binding ref `mcp:<serverId>:<toolName>` so later execution/audit can trace every remote tool back to its MCP server.
- Required MCP descriptor scopes to include a general MCP scope plus server and server/tool scopes to support explicit Agent allowlists.
- Kept Plan 07-04 execution non-functional by design: the placeholder binding returns a failed `MCP_EXECUTION_DEFERRED` result and must still be invoked through consumers of `ToolExecutionGateway`; Plan 07-05 will replace it with governed remote execution.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Extended MCP client seam with listTools support**
- **Found during:** Task 1 (Implement MCP discovery registry state)
- **Issue:** Plan 03's `McpClientHandle` intentionally exposed only server id/transport/close and had no discovery method, blocking registry refresh from listing MCP tools through the existing client seam.
- **Fix:** Added `InitializedClient.listTools()` plus `McpClientHandle.listTools()` and implemented SDK `client.listTools().tools()` forwarding in `McpClientFactory`.
- **Files modified:** `McpClientFactory.java`, `McpClientHandle.java`, `McpClientFactoryTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -am -Dtest=McpToolRegistryTest test`
- **Committed in:** `3d8a275`

---

**Total deviations:** 1 auto-fixed (Rule 3 blocking issue)
**Impact on plan:** The seam extension was necessary for planned discovery and stayed inside `pi-agent-infrastructure-mcp`, preserving COLA and SDK leakage boundaries.

## Issues Encountered

- The exact narrow command `mvn -q -pl pi-agent-infrastructure-mcp -Dtest=McpToolRegistryTest test` could not compile App MCP governance port imports until reactor dependencies were included. Final verification used the plan-level `-am` command and passed.
- Existing unrelated uncommitted planning artifacts under Phase 2/Phase 3 and `bun.lock` were present before execution and left untouched.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -Dtest=McpToolRegistryTest test` — used during incremental work where possible; the final adapter step requires reactor dependency compilation.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -am -Dtest=McpToolRegistryTest test` — passed final plan verification.

## Known Stubs

- `McpToolRegistry.DeferredMcpToolExecutorBinding` intentionally returns a failed `MCP_EXECUTION_DEFERRED` result. This is the explicit Plan 07-04 placeholder seam allowed by the plan; Plan 07-05 owns real MCP remote tool execution through governed `ToolExecutorBinding` behavior. It does not make MCP tools silently executable.
- Empty-string normalization for optional descriptions/redacted errors is defensive status/metadata normalization, not UI placeholder data.

## Auth Gates

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 07-05 can replace the deferred MCP executor with a real remote call binding while preserving `McpToolRegistry` resolution and the ToolExecutionGateway-only execution path.
- Plan 07-06 can wire `McpGovernanceCatalogAdapter` into Cloud Server governance without changing App/client DTO contracts.
- Admin UI can display failed MCP server discovery status and available tool metadata without needing MCP SDK access.

## Self-Check: PASSED

- Found summary file: `.planning/phases/07-mcp-client-bridge-and-governed-remote-tools/07-04-SUMMARY.md`
- Found key files: `McpServerRegistry.java`, `McpToolDescriptorMapper.java`, `McpToolRegistry.java`, `McpGovernanceCatalogAdapter.java`
- Found task commits: `9b6304f`, `3d8a275`, `614354b`, `1cbbd3d`, `a347460`, `3c7c299`

---
*Phase: 07-mcp-client-bridge-and-governed-remote-tools*
*Completed: 2026-06-16*
