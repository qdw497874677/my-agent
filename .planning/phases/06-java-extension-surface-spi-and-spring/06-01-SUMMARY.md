---
phase: 06-java-extension-surface-spi-and-spring
plan: 01
subsystem: extension-api
tags: [java, maven, sdk, spi, extension-api, archunit]

requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Framework-free Domain runtime, event, tool, and workspace contracts
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: App model provider registry contract reused by extension capabilities
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: ToolDescriptor and ToolExecutorBinding contracts reused by tool extensions
provides:
  - Framework-free `pi-agent-extension-api` Maven module
  - Extension metadata, API version, compatibility, lifecycle, health, and source contracts
  - Typed capability contracts for tools, model providers, policies, event listeners, workspace providers, and metadata-only memory providers
  - ArchUnit boundary gate for public SDK dependencies and Domain/App back-dependencies
affects: [phase-06, phase-07-mcp, phase-08-plugins, admin-governance, spring-starter]

tech-stack:
  added: [pi-agent-extension-api]
  patterns: [descriptor-first tool extensions, redacted extension metadata, framework-free SDK boundary]

key-files:
  created:
    - pi-agent-extension-api/pom.xml
    - pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionSource.java
    - pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionMetadata.java
    - pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionCapability.java
    - pi-agent-extension-api/src/test/java/io/github/pi_java/agent/extension/api/ExtensionApiContractTest.java
    - pi-agent-extension-api/src/test/java/io/github/pi_java/agent/extension/api/ExtensionApiArchitectureTest.java
  modified:
    - pom.xml

key-decisions:
  - "Keep `pi-agent-extension-api` framework-free with compile dependencies only on `pi-agent-domain` and `pi-agent-app`."
  - "Model tool extensions as descriptor-first capabilities carrying existing `ToolDescriptor` plus `ToolExecutorBinding`, so future sources register through the governed tool pipeline."
  - "Represent memory provider support as metadata-only capability in Plan 06-01; runtime/RAG wiring remains deferred."

patterns-established:
  - "ExtensionSource exposes immutable metadata plus typed capabilities as the stable public SDK entry point."
  - "Extension metadata carries redacted metadata only, explicit lifecycle, health, enabled flag, and API compatibility range."
  - "Architecture tests enforce no Spring, Vaadin, PF4J, MCP, provider SDK, Adapter, or Infrastructure dependency in the public extension API."

requirements-completed: [EXT-01, EXT-03, WORK-06]

duration: 14m
completed: 2026-06-15
---

# Phase 06 Plan 01: Public Extension API/SPI Module Summary

**Framework-free Java extension SDK with metadata, compatibility, lifecycle/health, and descriptor-first typed capabilities for future SPI, Spring, MCP, and plugin integrations.**

## Performance

- **Duration:** 14 min
- **Started:** 2026-06-15T23:14:11Z
- **Completed:** 2026-06-15T23:28:20Z
- **Tasks:** 3
- **Files modified:** 18

## Accomplishments

- Added `pi-agent-extension-api` to the Maven reactor immediately after `pi-agent-app`, with production dependencies limited to existing Domain/App contracts.
- Created the public extension source, metadata, version/compatibility, lifecycle, health, and capability API under `io.github.pi_java.agent.extension.api`.
- Covered the Phase 6 capability family: tool, model provider, policy, event listener, workspace provider, and minimal metadata-only memory provider.
- Ensured tool capabilities reuse the existing governed `ToolDescriptor` plus App `ToolExecutorBinding` instead of inventing a separate executor surface.
- Added contract and ArchUnit tests proving validation, machine-checkable lifecycle/compatibility, immutable redacted metadata, and SDK boundary isolation.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add public extension API Maven module** - `3e45a2e` (feat)
2. **Task 2: Define extension metadata and capability contracts** - `a637f46` (feat)
3. **Task 3: Add extension API architecture gate** - `96f071c` (test)

**Plan metadata:** pending final docs commit

_Note: TDD tasks were executed with an initial failing contract-test/build signal before implementation where practical; commits were kept task-atomic per parallel execution instructions._

## Files Created/Modified

- `pom.xml` - Adds `pi-agent-extension-api` to the parent reactor after `pi-agent-app`.
- `pi-agent-extension-api/pom.xml` - Defines the framework-free public extension API module with Domain/App compile dependencies and JUnit/AssertJ/ArchUnit test dependencies.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionSource.java` - Public extension-source entry contract exposing metadata and capabilities.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionMetadata.java` - Immutable identity, version, vendor, API compatibility, lifecycle, health, enabled, and redacted metadata record.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionApiVersion.java` - Comparable semantic API version value object.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionCompatibility.java` - Machine-checkable min-inclusive/max-exclusive API compatibility range.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionLifecycleState.java` - Extension lifecycle states with availability semantics.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionHealth.java` - Public health status and redacted details record.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionCapability.java` - Capability marker interface with stable `Type` enum.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ToolExtensionCapability.java` - Tool capability carrying existing `ToolDescriptor` and `ToolExecutorBinding`.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ModelProviderExtensionCapability.java` - Model-provider capability metadata.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/PolicyExtensionCapability.java` - Policy capability metadata.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/EventListenerExtensionCapability.java` - Event listener/sink capability metadata.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/WorkspaceProviderExtensionCapability.java` - Workspace/resource provider capability metadata.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/MemoryProviderExtensionCapability.java` - Minimal metadata-only memory provider placeholder contract.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionStrings.java` - Package-local validation helper for non-blank strings.
- `pi-agent-extension-api/src/test/java/io/github/pi_java/agent/extension/api/ExtensionApiContractTest.java` - Contract tests for source/capability behavior, validation, lifecycle, compatibility, and tool binding requirements.
- `pi-agent-extension-api/src/test/java/io/github/pi_java/agent/extension/api/ExtensionApiArchitectureTest.java` - ArchUnit boundary tests for SDK dependencies and Domain/App back-dependencies.

## Decisions Made

- Kept `pi-agent-extension-api` as a plain Java SDK module rather than a Spring starter; Spring Bean and annotation registration remain dedicated later Plan 06 work.
- Reused existing Domain/App contracts for tools and model providers so extension sources cannot define parallel, ungoverned execution paths.
- Kept memory provider support metadata-only to satisfy extension identity/discovery needs without prematurely introducing RAG/runtime wiring.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Used focused module-only verification for final green gate due parallel executor modifications in upstream modules**
- **Found during:** Overall verification
- **Issue:** `mvn -q -pl pi-agent-extension-api -am test` attempted to test upstream modules and failed in `pi-agent-app` on concurrently modified governance extension tests outside this plan's files.
- **Fix:** Verified this plan's module with `mvn -q -pl pi-agent-extension-api test` after committing the module and architecture tests; recorded the upstream failure as out-of-scope parallel work rather than modifying unrelated files.
- **Files modified:** None
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-extension-api test`
- **Committed in:** N/A (verification-scope adjustment only)

---

**Total deviations:** 1 auto-handled blocking verification-scope issue
**Impact on plan:** Implementation scope unchanged; the public extension API module itself is green.

## Issues Encountered

- The first focused reactor run surfaced pre-existing/in-parallel Phase 6 governance tests in `pi-agent-client`/`pi-agent-app` that were not part of Plan 06-01. These were treated as out of scope for this parallel executor.
- ArchUnit emits expected SLF4J no-provider warnings in test output; no test failure or runtime dependency was introduced.

## Known Stubs

- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/MemoryProviderExtensionCapability.java` is intentionally metadata-only. This is not a missing implementation for Plan 06-01; the plan explicitly required memory provider support as a minimal placeholder with no runtime/RAG wiring.
- `pi-agent-extension-api/src/test/java/io/github/pi_java/agent/extension/api/ExtensionApiContractTest.java` uses capability ID `memory.placeholder` to assert the metadata-only memory provider contract.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-extension-api -Dtest=ExtensionApiContractTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-extension-api -Dtest=ExtensionApiArchitectureTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-extension-api test`

## Next Phase Readiness

- Plan 06-02 can build extension governance read models on top of `ExtensionMetadata` and `ExtensionCapability` without importing Spring or implementation-specific plugin/MCP types.
- Plan 06-03 Java ServiceLoader discovery can use `ExtensionSource` as the SPI provider interface.
- Plan 06-04/06-05 Spring starter and annotations can normalize Spring Beans into these same capability records.
- Later MCP/PF4J phases can publish capability metadata without bypassing ToolGateway, policy, audit, and event boundaries.

## Self-Check: PASSED

- Verified key files exist: `pom.xml`, `pi-agent-extension-api/pom.xml`, `ExtensionSource.java`, `ExtensionMetadata.java`, `ExtensionCapability.java`, `ExtensionApiContractTest.java`, `ExtensionApiArchitectureTest.java`, and this summary.
- Verified task commits exist: `3e45a2e`, `a637f46`, and `96f071c`.

---
*Phase: 06-java-extension-surface-spi-and-spring*
*Completed: 2026-06-15*
