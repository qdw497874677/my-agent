---
phase: 07-mcp-client-bridge-and-governed-remote-tools
plan: 05
subsystem: mcp-infrastructure
tags: [java, mcp, tool-execution-gateway, remote-tools, redaction]

requires:
  - phase: 07-mcp-client-bridge-and-governed-remote-tools
    provides: MCP server configuration, SDK client handle seam, tool discovery snapshots, and normalized ToolDescriptor registration from Plans 07-01 through 07-04
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: ToolRegistry, ToolExecutorBinding, ToolExecutionGateway, ToolExecutionRequest, ToolExecutionResult, timeout/cancellation/redaction contract
provides:
  - MCP remote tool invocation as ordinary ToolExecutorBinding instances resolved from McpToolRegistry
  - MCP CallToolResult success/error mapping into source-neutral ToolExecutionResult values
  - Safe MCP invocation failure categorization for auth, unavailable, timeout, cancellation, and generic failures
affects: [phase-07-cloud-wiring, phase-07-admin-governance, phase-07-e2e, phase-08-plugin-tool-sources]

tech-stack:
  added: []
  patterns:
    - Infrastructure-only MCP SDK invocation behind App ToolExecutorBinding seam
    - Summary-only MCP result mapping with raw remote error and binary payload redaction
    - Stable MCP invocation error categories for existing gateway/audit/event handling

key-files:
  created:
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/invocation/McpToolExecutorBinding.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/invocation/McpToolResultMapper.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/invocation/McpInvocationErrorMapper.java
    - pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/invocation/McpToolExecutorBindingTest.java
  modified:
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactory.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientHandle.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistry.java
    - pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactoryTest.java
    - pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistryTest.java

key-decisions:
  - "Keep MCP remote invocation in pi-agent-infrastructure-mcp; ToolExecutionGateway remains the only governance/audit/policy entry point."
  - "Capture server id and MCP tool name from discovery snapshots in McpToolExecutorBinding so request arguments cannot redirect to arbitrary MCP endpoints or tool names."
  - "Map MCP errors to stable categories and redacted summaries instead of returning raw remote headers, request bodies, or error bodies."

patterns-established:
  - "MCP invocation binding: registry resolution creates per-server/per-tool ToolExecutorBinding values that call MCP SDK handles only in Infrastructure."
  - "MCP output mapping: text/structured/resource metadata is summarized, binary data is redacted, and remote error content is category-only."
  - "MCP failure mapping: cancellation is checked before remote calls, timeout/auth/unavailable/generic failures are normalized, and non-idempotent unknown tools are not retried."

requirements-completed: [MCP-03, MCP-04, MCP-05]

duration: 8m
completed: 2026-06-16
---

# Phase 07 Plan 05: MCP Governed Remote Tool Invocation Summary

**MCP remote tools now execute as ToolExecutorBinding instances behind the existing governed gateway with safe result summaries and redacted failure categories.**

## Performance

- **Duration:** 8 min
- **Started:** 2026-06-16T09:13:14Z
- **Completed:** 2026-06-16T09:21:13Z
- **Tasks:** 3
- **Files modified:** 9

## Accomplishments

- Added `McpToolExecutorBinding`, which checks cancellation before invocation, opens MCP clients only in Infrastructure, and calls the configured MCP tool name captured from discovery.
- Wired `McpToolRegistry.resolve()` to return a concrete MCP binding instead of the previous deferred failure binding, preserving the App `ToolRegistry.ToolResolution` seam used by `ToolExecutionGateway`.
- Added `McpToolResultMapper` to map successful text, structured, image, and resource-link MCP responses into source-neutral `ToolExecutionResult` summaries suitable for existing payload limiting/redaction flow.
- Added `McpInvocationErrorMapper` to normalize cancellation, auth, timeout, unavailable, remote MCP error, and generic failures without exposing raw headers, bodies, credential values, or fake secret markers.
- Extended MCP client handle abstractions with `callTool` while keeping MCP SDK types contained inside the infrastructure MCP module.

## Task Commits

Each task was committed atomically:

1. **TDD RED: MCP executor binding expectations** - `5c9af96` (test)
2. **Task 1: Implement MCP ToolExecutorBinding** - `c43a29b` (feat)
3. **Task 2: Map MCP call results to Pi ToolExecutionResult** - `fa9cada` (test)
4. **Task 3: Normalize invocation/auth/timeout failures safely** - `babeabf` (test)

_Note: Plan tasks were TDD-scoped. Task 1 introduced the implementation needed by all tests; Tasks 2 and 3 added focused mapping/failure coverage on top of that implementation._

## Files Created/Modified

- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/invocation/McpToolExecutorBinding.java` - ToolExecutorBinding for configured remote MCP tool invocation.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/invocation/McpToolResultMapper.java` - Converts MCP `CallToolResult` success/error content into Pi `ToolExecutionResult` values.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/invocation/McpInvocationErrorMapper.java` - Categorizes and redacts invocation failures and cancellations.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactory.java` - Adds `callTool` to initialized client seam.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientHandle.java` - Exposes safe handle-level `callTool` delegation.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistry.java` - Resolves available discovered MCP tools into concrete executor bindings.
- `pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/invocation/McpToolExecutorBindingTest.java` - Covers configured tool invocation, cancellation, success mapping, MCP errors, auth redaction, timeout redaction, and no retry.
- `pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactoryTest.java` - Updates fake initialized client for the new callTool seam.
- `pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistryTest.java` - Updates registry resolution expectations from deferred failure to concrete successful binding.

## Decisions Made

- Kept all MCP SDK invocation inside `pi-agent-infrastructure-mcp`; Domain/App contracts still only see `ToolExecutorBinding` and `ToolExecutionResult`.
- Registry resolution captures the discovered `server` and MCP `tool.name()` when creating `McpToolExecutorBinding`; request arguments are passed as arguments only and cannot override endpoint/tool selection.
- MCP result raw output is limited to sanitized content/structured summaries; remote error content and binary image data are intentionally not exposed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Installed upstream App module to local Maven cache for non-reactor test command**
- **Found during:** Task 2 verification
- **Issue:** The plan's exact `mvn -pl pi-agent-infrastructure-mcp -Dtest=... test` command can fail in a stale local Maven cache because `pi-agent-app` classes from Plan 07-02 are needed by MCP governance tests.
- **Fix:** Ran `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -DskipTests install`, then reran the exact plan test command successfully.
- **Files modified:** None
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -Dtest=McpToolExecutorBindingTest test`
- **Committed in:** N/A (environment/cache fix only)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** No scope change. The fix only prepared the verification environment for the plan's specified non-reactor module test command.

## Issues Encountered

- The first non-reactor verification surfaced missing cached App MCP governance classes. This was resolved by installing upstream modules once; final plan verification used the reactor form from the plan and passed.
- Pre-existing unrelated uncommitted planning artifacts under Phase 02 and `bun.lock` were present at execution start/end. They were not modified or staged by this plan.

## Known Stubs

None. Null/empty-string patterns found in modified MCP files are defensive normalization/default guards, not UI-facing stubs or mock data paths.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -Dtest=McpToolExecutorBindingTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -am -Dtest=McpToolExecutorBindingTest test` — passed

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 07 Plan 06 can wire `McpToolRegistry` and MCP Admin governance REST into Cloud Server using the concrete invocation binding.
- Phase 07 Plan 08 can build Fake MCP execution E2E through `ToolExecutionGateway` and assert policy/audit/event/redaction lifecycle around these remote bindings.

## Self-Check: PASSED

- Found created files: `McpToolExecutorBinding.java`, `McpToolResultMapper.java`, `McpInvocationErrorMapper.java`, and `McpToolExecutorBindingTest.java`.
- Found task commits: `5c9af96`, `c43a29b`, `fa9cada`, and `babeabf`.

---
*Phase: 07-mcp-client-bridge-and-governed-remote-tools*
*Completed: 2026-06-16*
