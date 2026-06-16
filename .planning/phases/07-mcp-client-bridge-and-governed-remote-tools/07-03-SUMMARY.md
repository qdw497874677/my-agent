---
phase: 07-mcp-client-bridge-and-governed-remote-tools
plan: 03
subsystem: infra
tags: [java, mcp, spring-ai, transport, secrets, redaction]

requires:
  - phase: 07-mcp-client-bridge-and-governed-remote-tools
    provides: Trusted MCP server configuration, transport enum, auth ref model, and safety validation
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: App SecretResolver/ResolvedSecret conventions and raw-secret redaction boundary
provides:
  - Infrastructure MCP client factory seam with fakeable transport/client builders
  - Streamable HTTP, legacy SSE, and stdio transport selection from validated server configuration
  - Infrastructure-only static credential reference resolution for HTTP headers and stdio env values
  - Categorized sanitized MCP initialization errors suitable for governance status surfaces
affects: [phase-07-mcp-discovery, phase-07-mcp-execution, phase-07-admin-governance, phase-09-hardening]

tech-stack:
  added: []
  patterns: [infrastructure-only-sdk-seam, transport-factory-boundary, secret-ref-final-boundary-resolution, sanitized-exception-boundary]

key-files:
  created:
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactory.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientHandle.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpTransportFactory.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpSecretHeaderResolver.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientErrorSanitizer.java
    - pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientException.java
    - pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactoryTest.java
  modified: []

key-decisions:
  - "Keep MCP SDK client and transport types inside pi-agent-infrastructure-mcp; public handles expose only server id and transport kind."
  - "Use fakeable TransportFactory and ClientBuilder seams so no-key unit tests can assert transport selection and initialization behavior without network/process side effects."
  - "Resolve raw static credential refs only in McpSecretHeaderResolver immediately before transport creation, while redacted summaries expose counts only."
  - "Normalize initialization failures into McpClientException with a small category enum and safe operator hints instead of propagating raw remote bodies or headers."

patterns-established:
  - "MCP factory seam: validation, secret resolution, transport creation, SDK client initialization, and closeable handle creation happen in one infrastructure boundary."
  - "Transport selection tests use fakes and McpTransportKind assertions rather than real MCP servers, sockets, or subprocesses."
  - "MCP credential-boundary tests use PI_PHASE7_FAKE_SECRET_DO_NOT_LEAK and assert public toString/summary/error surfaces remain redacted."

requirements-completed: [MCP-01, MCP-05]

duration: 6m 57s
completed: 2026-06-16
---

# Phase 07 Plan 03: MCP Client Factory, Credential Boundary, and Sanitized Error Summary

**MCP transport/client creation seam with infrastructure-only secret injection and categorized redacted initialization failures.**

## Performance

- **Duration:** 6m 57s
- **Started:** 2026-06-16T08:49:30Z
- **Completed:** 2026-06-16T08:56:27Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments

- Added `McpClientFactory` and `McpClientHandle` so validated `McpServerProperties` can create initialized closeable MCP client handles without leaking MCP SDK types above infrastructure.
- Added `McpTransportFactory` that selects Streamable HTTP, legacy SSE, or stdio transport from the explicit Phase 7 transport config and applies endpoint/timeout/stdout env/header boundaries.
- Added `McpSecretHeaderResolver` to adapt existing `SecretRef`/`ResolvedSecret` conventions and materialize raw header/env values only at the final infrastructure transport boundary.
- Added `McpClientErrorSanitizer` and `McpClientException` to map initialization failures to stable categories: `CONFIG_INVALID`, `AUTH_FAILED`, `SERVER_UNAVAILABLE`, `TIMEOUT`, `TRANSPORT_ERROR`, and `UNKNOWN`.
- Expanded `McpClientFactoryTest` with TDD coverage for all transport kinds, minimal tools-only capability requests, close behavior, fake-secret redaction, and categorized sanitized auth failures.

## Task Commits

Each task was committed atomically. TDD tasks include separate RED/GREEN commits:

1. **Task 1 RED: Create client and transport factory seam tests** - `6ab5f05` (test)
2. **Task 1 GREEN: Create client and transport factory seams** - `86980bc` (feat)
3. **Task 2 RED: Resolve static credential refs inside MCP infrastructure** - `ba36b1b` (test)
4. **Task 2 GREEN: Resolve MCP static credential refs** - `6c1b196` (feat)
5. **Task 3 RED: Normalize and sanitize MCP connection errors** - `0e638c8` (test)
6. **Task 3 GREEN: Sanitize MCP client initialization errors** - `1f3f80d` (feat)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactory.java` - Validates server configuration, resolves secrets, builds transport handles, initializes sync clients with minimal capabilities, and wraps failures through the sanitizer.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientHandle.java` - Closeable infrastructure handle exposing only server id and transport kind publicly.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpTransportFactory.java` - Concrete SDK transport builder for Streamable HTTP, SSE, and stdio with request headers/env applied at the final boundary.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpSecretHeaderResolver.java` - Infrastructure secret lookup adapter and redacted transport-secret summary object.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientErrorSanitizer.java` - Category selection and safe governance/operator hint generation for connection/init failures.
- `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientException.java` - Sanitized runtime exception carrying server id and category without raw remote details.
- `pi-agent-infrastructure-mcp/src/test/java/io/github/pi_java/agent/infrastructure/mcp/client/McpClientFactoryTest.java` - No-network/no-process tests for transport selection, secrets, redaction, and error sanitization.

## Decisions Made

- Kept MCP SDK objects behind package/private infrastructure seams; `McpClientHandle` does not expose the SDK client publicly, preserving the COLA boundary.
- Used `McpSchema.ClientCapabilities.builder().build()` and no roots/sampling/elicitation/prompts/resources callbacks to keep client capabilities minimal/tools-only for Phase 7.
- Added fakes for `TransportFactory` and `ClientBuilder` instead of launching stdio processes or connecting to real MCP services, matching the plan's no-key/no-network verification intent.
- Treated raw secret material as valid only inside `ResolvedTransportSecrets`; its `toString()` and `redactedSummary()` expose counts only.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Existing unrelated uncommitted planning artifacts under Phase 2 and `bun.lock` were present before this plan. They were left untouched and excluded from all plan commits.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -Dtest=McpClientFactoryTest test` — passed after each GREEN task.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -am -Dtest=McpClientFactoryTest test` — passed as final plan verification.

## Known Stubs

None. Stub-pattern scan found only intentional null/blank defensive checks and the default `/mcp` endpoint fallback used when an HTTP endpoint is omitted; no placeholder UI/data-source stubs were introduced.

## Auth Gates

None.

## Self-Check: PASSED

- Found summary file: `.planning/phases/07-mcp-client-bridge-and-governed-remote-tools/07-03-SUMMARY.md`
- Found key files: `McpClientFactory.java`, `McpTransportFactory.java`, `McpSecretHeaderResolver.java`, `McpClientErrorSanitizer.java`
- Found task commits: `6ab5f05`, `86980bc`, `ba36b1b`, `6c1b196`, `0e638c8`, `1f3f80d`

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 07-04 can use `McpClientFactory` handles as the connection seam for discovery and server status capture.
- Plan 07-05 can execute remote calls through an infrastructure adapter while preserving ToolExecutionGateway as the only governed execution path.
- Phase 9 can harden HTTP redirect/egress policies around `McpTransportFactory` without changing Domain/App contracts.

---
*Phase: 07-mcp-client-bridge-and-governed-remote-tools*
*Completed: 2026-06-16*
