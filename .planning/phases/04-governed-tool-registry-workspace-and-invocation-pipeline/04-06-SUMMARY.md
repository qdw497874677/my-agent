---
phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
plan: 06
subsystem: infrastructure-workspace-builtins
tags: [java, infrastructure, workspace, tools, command-allowlist, governance, cola]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: ToolDescriptor, ToolExecutionRequest/Result, risk/side-effect metadata, and ToolExecutorBinding contracts from Plans 04-01 and 04-02.
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Infrastructure InMemoryToolRegistry and governance helper patterns from Plan 04-04.
provides:
  - Bounded local-temp workspace implementation for deterministic dev/test workspace resources.
  - Allowlisted command execution gateway constrained to workspace roots, sanitized environment, timeouts, and bounded summaries.
  - Minimum safe built-in example tool catalog with read-only info, workspace resource write, and allowlisted workspace command descriptor/binding pairs.
affects: [phase-04-cloud-wiring, phase-04-e2e, phase-05-tool-cards, phase-06-extension-surface, phase-07-mcp-tools]

tech-stack:
  added: []
  patterns: [local-temp-not-production-sandbox, command-allowlist, descriptor-first-builtins, preview-approval-metadata, workspace-scoped-side-effects]

key-files:
  created:
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/workspace/LocalTempWorkspaceGateway.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/workspace/AllowlistedCommandExecutionGateway.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/BuiltinToolCatalog.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/ReadOnlyInfoTool.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/WorkspaceResourceWriteTool.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/WorkspaceCommandTool.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/workspace/LocalTempWorkspaceBoundaryTest.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/tool/BuiltinWorkspaceToolsTest.java
  modified: []

key-decisions:
  - "Treat local-temp workspace and ProcessBuilder command execution as deterministic dev/test infrastructure only, documented as not a production sandbox."
  - "Expose built-in examples as ordinary ToolDescriptor plus ToolExecutorBinding registrations rather than special registry APIs."
  - "Mark workspace write and workspace command examples as side-effectful with previewRequired and approvalRecommended metadata so the gateway can gate them before side effects."

patterns-established:
  - "LocalTempWorkspaceGateway resolves logical resource paths under a per-session root and rejects absolute paths or traversal outside the workspace boundary."
  - "AllowlistedCommandExecutionGateway checks the executable allowlist before ProcessBuilder, sets the working directory to the workspace root, clears host environment values, applies a bounded timeout, and truncates output/error summaries."
  - "BuiltinToolCatalog returns exactly the minimum three category registrations and can be converted directly to an InMemoryToolRegistry."

requirements-completed: [WORK-03, WORK-07, WORK-08, TOOL-07, TOOL-03, TOOL-04, TOOL-05]

duration: 7m 10s
completed: 2026-06-14
---

# Phase 04 Plan 06: Bounded Workspace and Built-in Tools Summary

**Bounded dev/test workspace execution with safe built-in descriptor/binding examples for read-only info, workspace writes, and allowlisted commands.**

## Performance

- **Duration:** 7m 10s
- **Started:** 2026-06-14T19:24:38Z
- **Completed:** 2026-06-14T19:31:48Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added `LocalTempWorkspaceGateway`, a root-constrained dev/test workspace gateway that creates per-session local-temp directories, rejects `..` and absolute path escapes, supports text read/write helpers for built-in tools, and cleans session roots on close.
- Added `AllowlistedCommandExecutionGateway`, a narrow command executor that rejects non-allowlisted executables, runs approved commands only with the workspace root as working directory, sanitizes the process environment, enforces timeouts, and returns bounded `CommandResult` summaries.
- Added `BuiltinToolCatalog` and three ordinary descriptor/binding built-ins: injected read-only info, workspace resource write/append, and allowlisted workspace command.
- Added focused tests covering workspace boundary escape rejection, command allowlist behavior, timeout summaries, built-in catalog shape, side-effect metadata, and workspace-scoped built-in execution.

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement bounded local-temp workspace and allowlisted commands**
   - `7c1752d` feat(04-06): add bounded local temp workspace
2. **Task 2: Add three-category built-in example tools**
   - `0dd3735` feat(04-06): add built-in workspace tool catalog

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/workspace/LocalTempWorkspaceGateway.java` - Bounded local-temp `WorkspaceGateway` implementation with per-session roots, safe path resolution, and cleanup helper.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/workspace/AllowlistedCommandExecutionGateway.java` - Workspace-rooted `CommandExecutionGateway` implementation with explicit allowlist, sanitized env, timeout, cancellation/interruption handling, and summary truncation.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/workspace/LocalTempWorkspaceBoundaryTest.java` - Tests for path escape rejection, command allowlist enforcement, workspace-root execution, and timeout summaries.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/BuiltinToolCatalog.java` - Descriptor-first catalog for the three minimum built-in tool categories.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/ReadOnlyInfoTool.java` - Injected read-only info built-in that does not read host env or files.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/WorkspaceResourceWriteTool.java` - Workspace resource write/append built-in with side-effectful, preview/approval-friendly descriptor metadata.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/WorkspaceCommandTool.java` - Allowlisted workspace command built-in backed by `CommandExecutionGateway`.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/tool/BuiltinWorkspaceToolsTest.java` - Tests for catalog categories, descriptor metadata, workspace-scoped execution, and boundary failure handling.

## Decisions Made

- Kept local-temp workspace storage in Infrastructure as a test/dev implementation only and documented both local-temp classes as **not a production sandbox**.
- Used injected maps for the read-only info tool so built-in examples do not read arbitrary host environment variables, host files, or machine-specific sensitive values.
- Modeled workspace write and command examples as side-effectful `WORKSPACE_WRITE` tools with `previewRequired=true` and `approvalRecommended=true` metadata; actual approval gating remains owned by `ToolExecutionGateway`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Resolved command lookup after environment sanitization**
- **Found during:** Task 1 GREEN verification
- **Issue:** Clearing the process environment removed `PATH`, so allowlisted commands could fail before timeout/summary assertions.
- **Fix:** Set a minimal safe `PATH` and resolve common `/usr/bin` or `/bin` executables after the allowlist check, while preserving the logical command allowlist.
- **Files modified:** `AllowlistedCommandExecutionGateway.java`
- **Commit:** `7c1752d`

**2. [Rule 1 - Bug] Avoided reading process streams after forcible timeout destroy**
- **Found during:** Task 1 GREEN verification
- **Issue:** Reading process streams after `destroyForcibly()` could throw `IOException: Stream closed`, causing timeout results to be reported as generic failures.
- **Fix:** Return a bounded timeout summary immediately after forcible destroy instead of attempting unbounded stream reads from a terminated process.
- **Files modified:** `AllowlistedCommandExecutionGateway.java`
- **Commit:** `7c1752d`

**3. [Rule 1 - Bug] Adapted built-in command tool to the existing CancellationToken API**
- **Found during:** Task 2 compile verification
- **Issue:** Initial implementation assumed a cancellation token ID accessor and static `none()` helper that do not exist on the current `CancellationToken` class.
- **Fix:** Used the tool call ID as the command request cancellation token ID and instantiated `new CancellationToken()` in tests.
- **Files modified:** `WorkspaceCommandTool.java`, `BuiltinWorkspaceToolsTest.java`
- **Commit:** `0dd3735`

## Known Stubs

None. Stub-pattern scanning found no TODO/FIXME/placeholder text and no hardcoded empty UI-facing data. Empty maps/sets in descriptors and results are immutable default metadata/result values, not unwired stubs.

## Issues Encountered

- Full `pi-agent-infrastructure -am test` reaches pre-existing Docker/Testcontainers tests (`JdbcPersistenceIntegrationTest`, `PostgresRunQueueTest`) and fails because this execution environment has no Docker socket. This is the same environment gate documented by prior phase summaries; focused non-Docker tests for this plan pass.
- The repository had unrelated pre-existing/parallel changes in planning docs and testkit files. Per the parallel execution instruction, this plan staged and committed only the files in its scope.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am -Dtest=LocalTempWorkspaceBoundaryTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am -Dtest=BuiltinWorkspaceToolsTest,ToolInfrastructureGovernanceTest test` — passed.
- Content search for `System.getenv|/etc/|user.home` in `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool` — no matches.
- Stub search for `TODO|FIXME|placeholder|coming soon|not available` in new Infrastructure tool/workspace code — no matches.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am test` — blocked by pre-existing Docker/Testcontainers environment gate; all focused non-Docker plan tests passed.

## User Setup Required

None for the new non-Docker verification. Docker remains required only for pre-existing Testcontainers-backed infrastructure tests.

## Next Phase Readiness

- Plan 04-07 can wire `BuiltinToolCatalog` registrations into Cloud Server composition and expose read-only tool catalog APIs.
- Plan 04-08 can use the three built-in categories to prove success, preview/approval, deny, redaction, audit, and workspace-bound command/file E2E paths through the governed gateway.

## Self-Check: PASSED

- Found all key files on disk: both workspace implementation files, all four built-in tool files, both new tests, and this `04-06-SUMMARY.md`.
- Verified commits exist in `git log --oneline --all`: `7c1752d` and `0dd3735`.

---
*Phase: 04-governed-tool-registry-workspace-and-invocation-pipeline*
*Completed: 2026-06-14*
