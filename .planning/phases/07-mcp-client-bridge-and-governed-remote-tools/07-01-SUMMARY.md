---
phase: 07-mcp-client-bridge-and-governed-remote-tools
plan: 01
subsystem: infra
tags: [java, maven, mcp, spring-ai, configuration, security]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Governed tool registry, credential/redaction conventions, and policy/audit safety boundaries
  - phase: 06-java-extension-surface-spi-and-spring
    provides: Source/capability/provenance governance language for external capability sources
provides:
  - Dedicated pi-agent-infrastructure-mcp Maven module with MCP dependencies isolated from Domain/App/client/starter public APIs
  - Typed configuration-file-first MCP trusted server model for Streamable HTTP, stdio, and legacy SSE
  - Static credential/header reference contract with redacted public summaries
  - Conservative MCP safety validator for URL schemes, hosts, URL credentials, stdio commands, and secret-reference-only auth
affects: [phase-07-mcp-discovery, phase-07-mcp-execution, phase-07-admin-governance, phase-09-hardening]

tech-stack:
  added: [spring-ai-starter-mcp-client]
  patterns: [isolated-infrastructure-module, config-file-first-trusted-server-contract, redacted-governance-summary, sanitized-validation-errors]

key-files:
  created:
    - pi-agent-infrastructure-mcp/pom.xml
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpTransportKind.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpAuthProperties.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpServerProperties.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpSafetyValidator.java
    - pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/config/McpServerPropertiesTest.java
  modified:
    - pom.xml

key-decisions:
  - "Keep Spring AI MCP client dependencies only in pi-agent-infrastructure-mcp; no root dependencyManagement pin was needed because the existing Spring AI BOM manages the starter."
  - "Represent all MCP auth material as refs (credentialRef, bearerTokenRef, apiKeySecretRef, customHeaderSecretRefs, envSecretRefs) and expose only ref schemes/counts in public summaries."
  - "Make Streamable HTTP the default transport while requiring stdio/SSE to be explicitly selected by McpTransportKind."
  - "Limit Plan 07-01 safety validation to pre-connection configuration checks; production egress policy, redirect behavior, and runtime network hardening remain Phase 9/transport implementation concerns."

patterns-established:
  - "MCP module isolation: MCP/Spring AI MCP dependencies live in pi-agent-infrastructure-mcp and do not leak into Domain/App/client/starter POMs."
  - "Redacted summaries: governance-facing configuration summaries contain transport/auth/header/env counts and ref schemes, never raw ref targets or secret-like values."
  - "Sanitized validator errors: validation failures name the server id and safe reason without echoing raw URLs with credentials, header values, token names, or env values."

requirements-completed: [MCP-01, MCP-05]

duration: 6m 50s
completed: 2026-06-16
---

# Phase 07 Plan 01: MCP Infrastructure Module and Trusted Server Configuration Summary

**Isolated MCP infrastructure module with configuration-file-first trusted server definitions, static credential refs, redacted summaries, and SSRF-sensitive safety validation.**

## Performance

- **Duration:** 6m 50s
- **Started:** 2026-06-16T08:38:53Z
- **Completed:** 2026-06-16T08:45:43Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments

- Added `pi-agent-infrastructure-mcp` to the reactor after extension infrastructure and before starter/model/adapter modules.
- Isolated the Spring AI MCP client starter dependency in the MCP infrastructure module; no Domain/App/client/extension/starter module received MCP dependencies.
- Added plain Java MCP configuration records for server identity, enabled flag, display name, explicit transport, HTTP/SSE endpoint, stdio command/args/env refs, timeout, static credential refs, and metadata.
- Added governance-safe redacted summaries that expose auth mode and counts without raw secret/header/ref targets.
- Added `McpSafetyValidator` to reject unsafe URL schemes, missing hosts, URL-embedded credentials, invalid stdio configuration, and raw-looking auth/header/env values before any connection attempt.

## Task Commits

Each task was committed atomically. TDD tasks include separate RED/GREEN commits where applicable:

1. **Task 1: Add isolated MCP infrastructure Maven module** - `6e05ec5` (feat)
2. **Task 2 RED: Define typed MCP server, transport, and auth configuration tests** - `970a5f3` (test)
3. **Task 2 GREEN: Define typed MCP server, transport, and auth configuration** - `408a325` (feat)
4. **Task 3 RED: Add MCP configuration safety validation tests** - `37e4e8e` (test)
5. **Task 3 GREEN: Add MCP configuration safety validation** - `31b905a` (feat)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pom.xml` - Adds `pi-agent-infrastructure-mcp` to the parent reactor after `pi-agent-infrastructure-extension`.
- `pi-agent-infrastructure-mcp/pom.xml` - New infrastructure module with module-scoped Spring AI MCP client and optional configuration processor dependency.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpTransportKind.java` - Explicit `STREAMABLE_HTTP`, `STDIO`, and `SSE` transport enum.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpAuthProperties.java` - Static credential/header reference contract with redacted summaries.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpServerProperties.java` - Trusted MCP server configuration model with HTTP/SSE, stdio, timeout, env refs, metadata, and public summary helpers.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpSafetyValidator.java` - Conservative pre-connection safety validator with sanitized errors.
- `pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/config/McpServerPropertiesTest.java` - No-key tests for configuration representation, redaction, and validation failures/success.

## Decisions Made

- Used direct module-scoped `spring-ai-starter-mcp-client` rather than adding new global Maven management; the existing Spring AI BOM already manages Spring AI artifacts.
- Kept configuration records in Infrastructure instead of Domain/App so MCP details remain outside framework-free core contracts.
- Allowed raw-looking custom header strings to be constructed but rejected by `McpSafetyValidator`, making validation the explicit safety gate before connection.
- Returned `ValidationResult` for successful validation and threw sanitized `IllegalArgumentException` for unsafe configuration, matching the plan allowance of either result records or exceptions.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Adjusted custom header ref construction so raw-value validation reaches the safety validator**
- **Found during:** Task 3 (Add MCP configuration safety validation)
- **Issue:** `McpAuthProperties` initially rejected non-`scheme:target` custom header values in its constructor, preventing `McpSafetyValidator` from producing the planned sanitized safety error for raw secret-looking values.
- **Fix:** Let `McpAuthProperties` carry nonblank custom header values, then require safe ref schemes and reject raw-looking values in `McpSafetyValidator`.
- **Files modified:** `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpAuthProperties.java`, `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/config/McpSafetyValidator.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -Dtest=McpServerPropertiesTest test`
- **Committed in:** `31b905a`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** The adjustment keeps validation behavior aligned with the plan and improves sanitized error coverage without adding scope.

## Issues Encountered

- Initial `mvn -q -pl pi-agent-infrastructure-mcp -am test` during Task 1 encountered unrelated untracked parallel-agent test code under `pi-agent-app`. The plan-owned module compile was checked with test skipping for the new module, and the final full requested verification later passed.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -Dtest=McpServerPropertiesTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -am test` — passed.

## Known Stubs

None. The grep hits for `null`/empty-string handling are intentional optional-field defaults or redacted summary blanks, not UI/data-source stubs.

## Self-Check: PASSED

- Found summary file: `.planning/phases/07-mcp-client-bridge-and-governed-remote-tools/07-01-SUMMARY.md`
- Found key files: `pi-agent-infrastructure-mcp/pom.xml`, `McpSafetyValidator.java`
- Found task commits: `6e05ec5`, `970a5f3`, `408a325`, `37e4e8e`, `31b905a`

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 07-02 can define App/client governance read models against these redacted MCP server/auth/transport summaries.
- Plan 07-03 can build concrete transport/client factories using the validated `McpServerProperties` contract.
- Later transport code still needs same-host/no-credential redirect behavior and production network egress controls as documented in Phase 7/Phase 9 hardening scope.

---
*Phase: 07-mcp-client-bridge-and-governed-remote-tools*
*Completed: 2026-06-16*
