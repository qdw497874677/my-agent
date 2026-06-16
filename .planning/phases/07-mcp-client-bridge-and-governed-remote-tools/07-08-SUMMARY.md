---
phase: 07-mcp-client-bridge-and-governed-remote-tools
plan: 08
subsystem: testing
tags: [java, mcp, archunit, e2e, playwright, governance, redaction]

requires:
  - phase: 07-mcp-client-bridge-and-governed-remote-tools
    provides: MCP transport/client, discovery, ToolRegistry, ToolExecutorBinding, Admin REST/UI, and governance DTO wiring from Plans 07-01 through 07-07
provides:
  - MCP architecture boundary gate preventing MCP SDK/Spring AI MCP leakage into forbidden modules
  - No-key governed MCP product-path and redaction E2E tests through REST-created runs, ToolExecutionGateway, audit, events, policy, and Admin governance
  - Phase 07 MCP contract documentation and requirement traceability for MCP-01 through MCP-05 plus partial E2E-08 evidence
affects: [phase-07, phase-08, mcp, dynamic-plugins, admin-governance, tool-gateway, e2e]

tech-stack:
  added: [ArchUnit test dependency in pi-agent-infrastructure-mcp]
  patterns:
    - MCP SDK/Spring AI MCP types remain isolated to MCP infrastructure and Adapter Web composition/test surfaces
    - MCP product-path E2E uses fake model tool-call intent and fake MCP seams to prove no-key gateway/audit/event/redaction behavior
    - Requirement traceability may mark the MCP half of E2E-08 complete while leaving plugin JAR flows pending Phase 8

key-files:
  created:
    - pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/McpInfrastructureArchitectureTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpGovernedToolE2ETest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpSecurityRedactionE2ETest.java
    - docs/phase-07-mcp-client-bridge.md
  modified:
    - pi-agent-infrastructure-mcp/pom.xml
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java
    - .planning/REQUIREMENTS.md

key-decisions:
  - "Keep the architecture gate package-focused and allow MCP SDK/Spring AI MCP only in MCP infrastructure plus Adapter Web composition/test surfaces."
  - "Use deterministic in-memory/fake MCP seams for product-path E2E so tests remain no-key, no-Docker, no-network, and no arbitrary stdio launch."
  - "Record E2E-08 as pending overall because the MCP portion is complete in Phase 7 but sample plugin JAR loading/disable flows remain Phase 8 scope."

patterns-established:
  - "MCP closeout tests assert ToolExecutionGateway-driven tool.lifecycle events and audits rather than direct MCP client calls."
  - "Security E2E checks raw fake secrets across REST, event history, in-memory persisted events, audits, Admin DTOs, UI/catalog text, and normalized errors."
  - "Phase contract docs include exact verification commands and explicit deferrals for Phase 8/9 follow-up."

requirements-completed: [MCP-01, MCP-02, MCP-03, MCP-04, MCP-05]

duration: 10m 56s
completed: 2026-06-16
---

# Phase 07 Plan 08: MCP Closeout Gates, E2E, and Traceability Summary

**No-key MCP bridge closeout with architecture isolation, governed fake remote-tool E2E, redaction coverage, browser smoke, and Phase 7 contract traceability.**

## Performance

- **Duration:** 10m 56s
- **Started:** 2026-06-16T09:44:08Z
- **Completed:** 2026-06-16T09:55:04Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments

- Added `McpInfrastructureArchitectureTest` and an ArchUnit test dependency in `pi-agent-infrastructure-mcp` to prevent MCP SDK/Spring AI MCP leakage into Domain, App, Client, Extension API, and Spring starter modules, while also preventing MCP infrastructure from depending on Adapter Web, Vaadin, PF4J, or plugin packages.
- Added no-key `McpGovernedToolE2ETest` proving fake MCP descriptor naming, schema/provenance, read-only allowed calls, approval/deny paths, auth/timeout error categories, server-down governance status, stdio/http descriptor presence, `ToolExecutionGateway`, audit, and `tool.lifecycle` events.
- Added `McpSecurityRedactionE2ETest` covering raw `PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK` absence from REST detail, event history, in-memory persisted events, audit strings, Admin governance responses, tool catalog/UI-facing strings, and normalized errors.
- Extended `WebConsoleE2EFixtureConfiguration` with deterministic fake MCP governance snapshots for the Phase 7 Playwright smoke path.
- Created `docs/phase-07-mcp-client-bridge.md` as the downstream contract index covering configuration-file-first setup, transports, credential refs, safety defaults, discovery/refresh, descriptor normalization, gateway-only invocation, governance API/UI, verification commands, architecture boundaries, and explicit deferrals.
- Updated `.planning/REQUIREMENTS.md` with concrete Phase 7 evidence for `MCP-01` through `MCP-05` and partial `E2E-08` traceability while leaving plugin flows pending Phase 8.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add MCP architecture and dependency boundary gates** - `8a03b94` (test)
2. **Task 2: Add no-key governed MCP product-path E2E and security redaction E2E** - `145f02c` (test)
3. **Task 3: Document Phase 7 contracts and update requirement traceability** - `52afc7f` (docs)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-infrastructure-mcp/pom.xml` - Added ArchUnit test dependency for the MCP infrastructure architecture gate.
- `pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/McpInfrastructureArchitectureTest.java` - Added package-level MCP SDK/Spring AI MCP isolation and infrastructure dependency boundary assertions.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpGovernedToolE2ETest.java` - Added no-key product-path REST run E2E for fake MCP discovery/execution/status through gateway, policy, audit, events, and governance.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpSecurityRedactionE2ETest.java` - Added MCP-specific secret absence and normalized-error redaction E2E.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java` - Added deterministic e2e-profile fake MCP governance fixtures and redacted unhealthy server state.
- `docs/phase-07-mcp-client-bridge.md` - Added Phase 7 MCP contract and verification index.
- `.planning/REQUIREMENTS.md` - Updated MCP requirement evidence and E2E-08 partial status.

## Decisions Made

- Kept the architecture gate package-based rather than implementation-class-based so it catches future leaks without overfitting current class names.
- Used deterministic fake MCP tool registry/executor seams in Adapter Web E2E instead of launching real network/process MCP services; this satisfies the no-key/no-Docker plan intent while proving the product path from REST-created run to `ToolExecutionGateway`.
- Left `E2E-08` unchecked because the requirement explicitly includes Phase 8 plugin JAR load/disable flows even though the Phase 7 MCP portion is now validated.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added missing ArchUnit test dependency to MCP infrastructure**
- **Found during:** Task 1 (Add MCP architecture and dependency boundary gates)
- **Issue:** `pi-agent-infrastructure-mcp` did not declare `archunit-junit5`, so the new architecture test could not compile in that module.
- **Fix:** Added the test-scoped ArchUnit dependency to `pi-agent-infrastructure-mcp/pom.xml`.
- **Files modified:** `pi-agent-infrastructure-mcp/pom.xml`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -am -Dtest=McpInfrastructureArchitectureTest test`
- **Committed in:** `8a03b94`

**2. [Rule 3 - Blocking] Extended browser E2E fixture MCP data instead of modifying already-present spec logic**
- **Found during:** Task 2 (Add no-key governed MCP product-path E2E and security redaction E2E)
- **Issue:** `e2e/phase-07-mcp-governance.spec.ts` already existed from Plan 07-07, but its server data needed deterministic e2e-profile MCP snapshots for the final Phase 7 smoke gate.
- **Fix:** Added fake healthy/unhealthy MCP servers to `WebConsoleE2EFixtureConfiguration`; the existing browser spec then validates discovered tools, redacted unhealthy state, refresh, and no CRUD controls.
- **Files modified:** `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java`
- **Verification:** `npm run e2e -- e2e/phase-07-mcp-governance.spec.ts`
- **Committed in:** `145f02c`

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both changes were necessary to run the planned gates in the current repository. They did not add product scope beyond no-key MCP verification and traceability closeout.

## Issues Encountered

- Pre-existing unrelated working-tree changes were present under older Phase 02 planning artifacts, Phase 03 planning context files, and `bun.lock`; they were left unstaged and untouched.
- During E2E test implementation, assertions were adjusted to current gateway payload shapes (`sourceId=fake` in provenance and terminal timeout mapping through the existing policy-blocked run status) while preserving checks for `MCP_TIMEOUT`, redaction, and gateway events.

## Known Stubs

None. The new tests and docs use deterministic fake seams intentionally for no-key E2E validation; they do not block Phase 7 goals and explicitly document real external service/plugin deferrals.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -am -Dtest=McpInfrastructureArchitectureTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=McpGovernedToolE2ETest,McpSecurityRedactionE2ETest test`
- `npm run e2e -- e2e/phase-07-mcp-governance.spec.ts`
- `test -f docs/phase-07-mcp-client-bridge.md && grep -q "MCP-01" .planning/REQUIREMENTS.md && grep -q "ToolExecutionGateway" docs/phase-07-mcp-client-bridge.md && grep -q "Phase 8" docs/phase-07-mcp-client-bridge.md`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp,pi-agent-adapter-web -am -Dtest=McpInfrastructureArchitectureTest,McpToolRegistryTest,McpToolExecutorBindingTest,McpGovernanceApiTest,McpGovernedToolE2ETest,McpSecurityRedactionE2ETest test`
- `npm run e2e -- e2e/phase-07-mcp-governance.spec.ts`

## Next Phase Readiness

- Phase 7 MCP client bridge is closed with contract docs, architecture isolation, no-key product-path verification, redaction E2E, and Admin/browser smoke coverage.
- Phase 8 dynamic plugin JAR work can reuse the same governance language: source/provenance, inspect-only Admin status until mutations are planned, no bypass around `ToolExecutionGateway`, and partial `E2E-08` tracking.
- Phase 9 can add production telemetry around MCP calls without changing the gateway-only invocation contract.

## Self-Check: PASSED

- Verified key created/modified files exist.
- Verified task commits exist: `8a03b94`, `145f02c`, `52afc7f`.

---
*Phase: 07-mcp-client-bridge-and-governed-remote-tools*
*Completed: 2026-06-16*
