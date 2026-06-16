---
phase: 08-controlled-dynamic-plugin-jars
plan: 05
subsystem: testing
tags: [plugins, e2e, tool-gateway, policy, audit, redaction, disablement, tdd]

requires:
  - phase: 08-controlled-dynamic-plugin-jars
    provides: Adapter Web plugin ToolRegistry composition and Admin plugin governance REST from Plan 08-04.
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: ToolExecutionGateway, policy, audit, redaction, and tool.lifecycle contracts.
  - phase: 07-mcp-client-bridge-and-governed-remote-tools
    provides: No-key product-path E2E patterns for external governed tool sources.
provides:
  - No-key plugin governed tool product-path E2E proving plugin tools use ToolExecutionGateway, policy, audit, and tool.lifecycle events.
  - Plugin disable/quarantine capability-inert regression coverage for new registry resolution.
  - Plugin redaction E2E across REST detail, event history, persisted events, audit strings, Admin DTOs, tool catalog text, and UI fixture text.
affects: [phase-08, phase-09, dynamic-plugins, admin-governance, tool-gateway, security-redaction, e2e]

tech-stack:
  added: []
  patterns:
    - No-key product-path E2E for plugin-provided tools mirrors MCP E2E while keeping fake plugin fixtures deterministic.
    - Disable/quarantine regression tests rebuild governance-derived registries to prove new resolutions are capability-inert.
    - Plugin redaction tests assert absence of raw secrets, paths, env names/values, and plugin metadata across all public surfaces.

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/PluginGovernedToolE2ETest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/PluginSecurityRedactionE2ETest.java
    - pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginCapabilityDisablementTest.java
  modified:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java

key-decisions:
  - "Use deterministic fake plugin tool descriptors and bindings for no-key product-path E2E, proving gateway behavior without launching subprocesses or requiring sample plugin packaging."
  - "Assert plugin disable/quarantine by rebuilding PluginGovernanceCatalogAdapter-derived ExtensionToolRegistry instances so tests match new-run/new-resolution semantics."
  - "Treat UI fixture text as a redaction public surface and add a sanitized plugin fixture summary for browser-oriented follow-up coverage."

patterns-established:
  - "Plugin external-source tests assert ToolExecutionGateway-driven tool.lifecycle and audit evidence, not direct plugin binding calls."
  - "Plugin security tests cover raw secret absence from REST, events, persisted events, audit, Admin DTOs, catalog text, and UI fixture text."
  - "Plugin lifecycle mutation tests verify disabled/quarantined tools are absent from new ToolRegistry resolution while governance remains visible with redacted reasons."

requirements-completed: [PLUG-02, PLUG-05, PLUG-06, E2E-08]

duration: 15m 20s
completed: 2026-06-16
---

# Phase 08 Plan 05: Plugin Governed E2E and Security Regression Summary

**Plugin tools now have no-key product-path tests proving governed ToolExecutionGateway execution, capability inertness after disable/quarantine, and raw-secret redaction across runtime and governance surfaces.**

## Performance

- **Duration:** 15m 20s
- **Started:** 2026-06-16T17:49:06Z
- **Completed:** 2026-06-16T18:04:26Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added `PluginGovernedToolE2ETest` with REST-created run coverage for plugin read-only success, approval-required, and deny branches through the existing General Agent and `ToolExecutionGateway` path.
- Asserted plugin provenance uses `ToolProvenance.SourceKind.PLUGIN`, plugin/source/capability identifiers, `tool.lifecycle` payloads, and audit records without invoking fake plugin bindings for policy gates.
- Added `PluginCapabilityDisablementTest` proving disabled and quarantined plugin tools cannot be listed or resolved by new `ExtensionToolRegistry` instances derived from governance state.
- Added `PluginSecurityRedactionE2ETest` proving raw fake plugin secrets, absolute paths, env names/values, raw exception text, and sensitive plugin metadata do not appear in REST details, event history, persisted events, audit strings, Admin plugin responses, tool catalog text, or UI fixture text.
- Extended `WebConsoleE2EFixtureConfiguration` with sanitized plugin fixture text for browser/UI smoke coverage without exposing raw plugin internals.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add no-key governed plugin tool E2E** - `0625932` (test)
2. **Task 1 GREEN: Add no-key governed plugin tool E2E** - `e92db82` (feat)
3. **Task 2 RED: Add disable/quarantine and redaction E2E coverage** - `288883a` (test)
4. **Task 2 GREEN: Add disable/quarantine and redaction E2E coverage** - `7a78d70` (feat)

**Plan metadata:** pending final docs commit

_Note: TDD tasks intentionally have RED then GREEN commits._

## Files Created/Modified

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/PluginGovernedToolE2ETest.java` - No-key plugin tool product-path E2E through REST run creation, fake model tool-call intent, `ToolExecutionGateway`, policy, audit, and events.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/PluginSecurityRedactionE2ETest.java` - Plugin-specific redaction E2E across REST, events, persisted event store, audit, Admin governance, catalog, and UI fixture text.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java` - Adds sanitized plugin fixture text for UI/browser-facing redaction assertions.
- `pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginCapabilityDisablementTest.java` - Focused infrastructure regression proving disabled/quarantined plugins are capability-inert for new registry lookups.

## Decisions Made

- Used deterministic in-memory/fake plugin descriptors and bindings for the E2E instead of sample plugin JAR packaging, matching the plan's no-key/no-Docker/no-subprocess requirement and leaving packaged sample plugin validation to later Phase 8 sample work.
- Verified disable/quarantine semantics at the governance-adapter-to-tool-registry boundary because v1 semantics apply to new capability resolution, not in-flight call interruption or JVM hot unload.
- Added explicit sanitized UI fixture text because plugin secrets can otherwise leak through browser fixtures even when REST/event/audit paths are safe.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Overrode plugin governance catalog in Adapter Web E2E fixtures**
- **Found during:** Task 1 GREEN
- **Issue:** The focused Spring Boot E2E context saw both the infrastructure adapter bean and public `PluginGovernanceCatalog` bean, causing ambiguous governance service wiring.
- **Fix:** Added a primary fake `PluginGovernanceCatalog` in plugin E2E test configurations so the test context remains deterministic while the tool execution path still uses the production `ToolExecutionGateway` bean.
- **Files modified:** `PluginGovernedToolE2ETest.java`, `PluginSecurityRedactionE2ETest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=PluginGovernedToolE2ETest test`; combined Task 2 verification command.
- **Committed in:** `e92db82`, `7a78d70`

**2. [Rule 3 - Blocking] Added plugin-scoped AgentDefinition for redaction E2E**
- **Found during:** Task 2 GREEN
- **Issue:** The default test agent did not pre-authorize `tool:plugin`, so the redaction failure fixture stopped at approval-required policy before plugin error normalization could be asserted.
- **Fix:** Added a primary `AgentDefinition` with `tool:plugin` scope and a focused allow policy in `PluginSecurityRedactionE2ETest`.
- **Files modified:** `PluginSecurityRedactionE2ETest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin,pi-agent-adapter-web -am -Dtest=PluginCapabilityDisablementTest,PluginSecurityRedactionE2ETest test`
- **Committed in:** `7a78d70`

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both fixes were necessary to exercise the planned product-path and redaction assertions in the current Spring Boot test context. No product scope was added.

## Issues Encountered

- A parallel Phase 08 Plan 06 executor committed UI work while this plan was running, so recent git history includes unrelated 08-06 commits interleaved between 08-05 task commits. This plan staged only its own files.
- Pre-existing unrelated working-tree changes under older Phase 02 planning artifacts, Phase 03 context files, and `bun.lock` were present before execution and left untouched.
- During one intermediate run, a concurrently edited `AdminPluginGovernanceViewTest` compile failure appeared; it was outside this plan's files and resolved by the parallel executor before final verification.

## Known Stubs

None. Fake plugin descriptors, fake bindings, and sanitized UI fixture strings are intentional no-key E2E fixtures for this verification plan and do not block the plan goal.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=PluginGovernedToolE2ETest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin,pi-agent-adapter-web -am -Dtest=PluginCapabilityDisablementTest,PluginSecurityRedactionE2ETest test` — passed
- Final parallel rerun of both focused commands — passed

## Auth Gates

None.

## User Setup Required

None - all tests use deterministic fake plugin/model/tool fixtures with in-memory state and require no model keys, Docker, external plugin JAR, network service, or subprocess launch.

## Next Phase Readiness

- Plan 08-06 can rely on sanitized plugin governance/UI fixture text and public DTO surfaces without weakening redaction boundaries.
- Plan 08-07 sample plugin packaging can add real sample JAR load coverage on top of these already-validated gateway, policy, disable/quarantine, audit, event, and redaction contracts.
- Phase 09 can add production telemetry around plugin tool calls while preserving the source-agnostic `ToolExecutionGateway` contract.

## Self-Check: PASSED

- Verified summary and key files exist: `08-05-SUMMARY.md`, `PluginGovernedToolE2ETest.java`, `PluginSecurityRedactionE2ETest.java`, and `PluginCapabilityDisablementTest.java`.
- Verified task commits exist in git history: `0625932`, `e92db82`, `288883a`, and `7a78d70`.

---
*Phase: 08-controlled-dynamic-plugin-jars*
*Completed: 2026-06-16*
