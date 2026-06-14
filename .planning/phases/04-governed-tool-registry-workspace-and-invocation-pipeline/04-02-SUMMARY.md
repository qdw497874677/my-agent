---
phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
plan: 02
subsystem: app-client-contracts
tags: [java, app, client-dto, tool-registry, catalog, cola]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Framework-free ToolDescriptor, ToolSchema, ToolProvenance, ToolExecutionRequest, ToolExecutionResult, and tool lifecycle contracts from Plan 04-01.
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: Descriptor-first App registry/query-service pattern mirrored for tools.
provides:
  - Descriptor-first App ToolRegistry port that lists ToolDescriptor records and resolves tool IDs to descriptor plus hidden executor binding.
  - Low-level ToolExecutorBinding contract accepting ToolExecutionRequest and CancellationToken without policy/audit responsibilities.
  - Read-only ToolRegistryQueryService and client ToolCatalogResponse/ToolDescriptorDto records for future Adapter REST/Admin/Web Console mapping.
affects: [phase-04-tool-execution-gateway, phase-05-agent-web-console, phase-06-extension-surface, phase-07-mcp-tools, phase-08-plugin-tools]

tech-stack:
  added: []
  patterns: [descriptor-first-registry-port, hidden-executor-binding, client-domain-separation, tdd-contract-tests]

key-files:
  created:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolRegistry.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolExecutorBinding.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ToolRegistryQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolRegistryQueryService.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/tool/ToolCatalogResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/tool/ToolDescriptorDto.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/port/ToolRegistryAppPortContractTest.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultToolRegistryQueryServiceTest.java
  modified: []

key-decisions:
  - "Keep ToolExecutorBinding in App as a low-level executor seam behind ToolRegistry resolution so future ToolExecutionGateway can be the only governance caller."
  - "Return public tool catalog data as pi-agent-client records instead of Domain records so future REST/Admin/Web Console surfaces do not leak Domain or executor implementation types."
  - "Expose source provenance as string metadata in client DTOs while preserving descriptor-first normalization and avoiding source-specific registry methods."

patterns-established:
  - "ToolRegistry mirrors ModelProviderRegistry but resolves tool IDs to a ToolResolution containing descriptor plus executor binding."
  - "DefaultToolRegistryQueryService maps Domain descriptors into client DTOs and never includes executor bindings, raw secrets, or implementation class names."
  - "Tool catalog DTOs are plain Java records free of Domain, Spring, and Jakarta imports."

requirements-completed: [TOOL-01, TOOL-02]

duration: 4m 52s
completed: 2026-06-14
---

# Phase 04 Plan 02: Descriptor-First Tool Registry and Catalog Contracts Summary

**Descriptor-first App registry contracts and read-only client catalog DTOs that expose governed tool metadata without leaking executor bindings or implementation details.**

## Performance

- **Duration:** 4m 52s
- **Started:** 2026-06-14T18:47:17Z
- **Completed:** 2026-06-14T18:52:09Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added the source-agnostic `ToolRegistry` App port with descriptor listing and `Optional` resolution of a tool ID to a descriptor/executor pair.
- Added `ToolExecutorBinding` as a low-level execution seam that accepts `ToolExecutionRequest` plus `CancellationToken`, with no Spring/MCP/plugin/Jackson or policy/audit responsibilities.
- Added `ToolRegistryQueryService`, `DefaultToolRegistryQueryService`, and client-side `ToolCatalogResponse`/`ToolDescriptorDto` records for future read-only REST and Admin/Web Console catalog surfaces.
- Verified client DTOs include required metadata — id/name, description, schemas, provenance, version, scopes, risk, side-effect classification, timeout default, and metadata — while excluding executor and secret details.

## Task Commits

Each task was committed atomically. TDD tasks have RED and GREEN commits:

1. **Task 1: Define descriptor-first ToolRegistry and executor binding ports**
   - `5b2bc06` test: add failing test for tool registry ports
   - `75fcc56` feat: define descriptor-first tool registry ports
2. **Task 2: Add read-only tool catalog query service and client DTOs**
   - `98fe878` test: add failing test for tool catalog query
   - `297e3b3` feat: add read-only tool catalog query DTOs

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolRegistry.java` - Descriptor-first App registry port with list and resolve operations plus hidden `ToolResolution` executor binding.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolExecutorBinding.java` - Low-level executor binding contract for future gateway orchestration.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ToolRegistryQueryService.java` - Read-only App use-case interface for tool catalog visibility.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolRegistryQueryService.java` - Domain descriptor to client DTO mapper backed by `ToolRegistry.listTools()`.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/tool/ToolCatalogResponse.java` - Public client catalog response record.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/tool/ToolDescriptorDto.java` - Public client descriptor DTO with schema/provenance nested records and no Domain/Spring/Jakarta imports.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/port/ToolRegistryAppPortContractTest.java` - Contract tests for descriptor listing, optional resolution, and absence of source-specific registry APIs.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultToolRegistryQueryServiceTest.java` - Query-service tests for complete catalog metadata and implementation-detail exclusion.

## Decisions Made

- Kept executor binding in App rather than Domain because it is an application-layer port that future gateway orchestration owns; Domain remains descriptor/value-object focused.
- Mapped descriptors to client records inside the App query service so Adapter REST can remain thin and future Web Console/Admin consumers receive stable public DTOs.
- Used enum `.name()` values for risk, side-effect, and provenance kind in client DTOs to avoid client dependency on Domain enum types while preserving stable catalog semantics.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None. Stub-pattern scanning found only pre-existing null/blank validation and fallback normalization in unrelated App files; no new placeholder/mock data or UI-facing empty defaults were introduced by this plan.

## Issues Encountered

- The initial TDD RED for Task 1 referenced `ToolExecutionStatus.SUCCEEDED`; Plan 04-01 established the enum value as `SUCCESS`, so the failing test was corrected before committing the RED contract.
- Java 21 does not provide `Optional.copyOf`; `ToolDescriptorDto` normalizes optional output schema with `outputSchema.map(schema -> schema)` instead.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=ToolRegistryAppPortContractTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app,pi-agent-client -am -Dtest=DefaultToolRegistryQueryServiceTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app,pi-agent-client -am -Dtest=DefaultToolRegistryQueryServiceTest,ToolRegistryAppPortContractTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app,pi-agent-client -am test` — passed; existing App architecture and use-case tests remain green.
- `grep -R "registerSpringBean\|registerMcpTool\|registerPluginTool" pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool` — no matches.
- Searched `pi-agent-client/src/main/java/io/github/pi_java/agent/client/tool` for Domain/Spring/Jakarta imports — no matches.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 04-03 can implement `ToolExecutionGateway` orchestration using `ToolRegistry.resolve(...)` and `ToolExecutorBinding.execute(...)` as the single controlled executor seam.
- Plan 04-07 can expose the read-only catalog through Adapter REST by returning the client DTOs added here without changing Domain/App contracts.

## Self-Check: PASSED

- Found `ToolRegistry.java`, `ToolExecutorBinding.java`, `DefaultToolRegistryQueryService.java`, `ToolDescriptorDto.java`, and this `04-02-SUMMARY.md` on disk.
- Verified commits exist in `git log --oneline --all`: `5b2bc06`, `75fcc56`, `98fe878`, and `297e3b3`.

---
*Phase: 04-governed-tool-registry-workspace-and-invocation-pipeline*
*Completed: 2026-06-14*
