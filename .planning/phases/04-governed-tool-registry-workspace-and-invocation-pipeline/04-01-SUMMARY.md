---
phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
plan: 01
subsystem: domain
tags: [java, domain, tool-registry, run-events, redaction, cola]

requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Framework-free Domain contracts, RunEvent envelope, ToolCall/ToolResult legacy contracts, and COLA dependency gates.
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: Descriptor-first provider pattern and provider-neutral event extension approach mirrored for tools.
provides:
  - Canonical framework-free ToolDescriptor, ToolSchema, ToolProvenance, risk/side-effect classification, and timeout/default metadata contracts.
  - Normalized ToolExecutionRequest, ToolExecutionResult, ToolExecutionStatus, and ProvisionPreview contracts with redacted summaries.
  - Complete tool lifecycle RunEventType wire names and a redacted ToolLifecyclePayload for future tool cards/audit mapping.
affects: [phase-04-tool-registry, phase-05-agent-web-console, phase-06-extension-surface, phase-07-mcp-tools, phase-08-plugin-tools]

tech-stack:
  added: []
  patterns: [framework-free-domain-records, descriptor-first-tool-metadata, redacted-tool-lifecycle-payloads, tdd-contract-tests]

key-files:
  created:
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolDescriptor.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolSchema.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolProvenance.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolRiskLevel.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolSideEffect.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolExecutionRequest.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolExecutionResult.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ProvisionPreview.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolExecutionStatus.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolValidation.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/tool/ToolDescriptorContractTest.java
  modified:
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventType.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventPayload.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/event/RunEventContractTest.java

key-decisions:
  - "Use plain JDK Map/Set/Optional/Duration records for tool schema, descriptor, preview, and execution results so Domain remains framework-free and JSON-schema-library-neutral."
  - "Preserve legacy ToolCall/ToolResult and add ToolExecutionRequest/ToolExecutionResult as gateway-facing contracts for later ToolExecutionGateway work."
  - "Represent tool lifecycle events with stable tool.* wire names plus one ToolLifecyclePayload carrying descriptor ref, provenance/version, redacted summaries, policy decision, preview, execution status, and error category."

patterns-established:
  - "Tool descriptors are descriptor-first and source-agnostic: provenance names the source kind without exposing Spring, MCP, plugin, or provider SDK types."
  - "Tool results default to redacted summaries; raw output is optional and absent from the normal contract path."
  - "Lifecycle event payloads carry Optional governance fields so proposed/policy/preview/start/update/terminal states can share one public-friendly Domain payload."

requirements-completed: [TOOL-01, TOOL-04, TOOL-06, OPS-05]

duration: 5m 00s
completed: 2026-06-14
---

# Phase 04 Plan 01: Governed Tool Domain Contracts Summary

**Framework-free governed tool descriptors, normalized execution outcomes, redacted summaries, previews, and tool lifecycle event contracts for the future single ToolExecutionGateway.**

## Performance

- **Duration:** 5m 00s
- **Started:** 2026-06-14T18:39:43Z
- **Completed:** 2026-06-14T18:44:43Z
- **Tasks:** 2
- **Files modified:** 14

## Accomplishments

- Added canonical tool metadata contracts covering descriptor identity, schemas, provenance/source, version, scopes, risk, side effects, timeout defaults, sensitive fields, and metadata.
- Added gateway-facing execution contracts covering requests, statuses, previews, redacted input/output summaries, error categories, optional raw output, and latency.
- Expanded RunEvent taxonomy to include complete public-friendly tool lifecycle wire names and a ToolLifecyclePayload suitable for future audit, SSE, REST mapping, and Web Console tool cards.
- Verified Domain remains Spring/Jackson/networknt/framework-free through the full `pi-agent-domain` test suite including ArchUnit gates.

## Task Commits

Each task was committed atomically. TDD tasks have RED and GREEN commits:

1. **Task 1: Add canonical tool descriptor and execution contracts**
   - `cd4abae` test: add failing test for governed tool contracts
   - `7f64df9` feat: implement governed tool domain contracts
2. **Task 2: Expand tool lifecycle event contracts**
   - `936ec80` test: add failing test for tool lifecycle events
   - `eea765e` feat: expand governed tool lifecycle events

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolDescriptor.java` - Canonical descriptor-first tool metadata record.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolSchema.java` - Plain Java schema document metadata with dialect, sensitive fields, and payload limit.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolProvenance.java` - Source-agnostic provenance for built-in, testkit, SPI, Spring Bean, MCP, plugin, and remote future sources.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolRiskLevel.java` - Tool risk taxonomy.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolSideEffect.java` - Side-effect classification for read-only, external, workspace write, and destructive actions.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolExecutionRequest.java` - Gateway-facing execution request metadata.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolExecutionResult.java` - Normalized execution result with redaction, preview, optional error category, status, and optional raw output.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ProvisionPreview.java` - Preview/impact estimate contract for risky or side-effectful actions.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolExecutionStatus.java` - Success, failure, deny, approval, sandbox, cancellation, timeout, and preview-only statuses.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolValidation.java` - Package-local validation helper for tool contracts.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventType.java` - Added complete `tool.*` lifecycle wire names.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventPayload.java` - Added `ToolLifecyclePayload` carrying redacted summaries and governance metadata.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/tool/ToolDescriptorContractTest.java` - Contract tests for descriptor validation and execution statuses.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/event/RunEventContractTest.java` - Contract tests for tool lifecycle wire names and payload metadata.

## Decisions Made

- Used plain Java collections and records for schema documents instead of Jackson/networknt/Spring types; schema validation remains an Infrastructure/App concern in later plans.
- Kept legacy `ToolCall`/`ToolResult` source-compatible while adding `ToolExecutionRequest` and `ToolExecutionResult` for the forthcoming gateway pipeline.
- Added a shared lifecycle payload rather than replacing the existing RunEvent envelope, preserving earlier event compatibility while enabling full TOOL-06 coverage.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None. The grep hit for `description = description == null ? "" : description` in `ToolDescriptor` is normalization for optional prose, not a UI/data stub.

## Issues Encountered

- TDD RED commits intentionally failed compilation before contracts existed.
- The initial Green implementation exposed an ambiguous `null` constructor call in the TDD test because `ToolDescriptor` supports both nullable `ToolSchema` and `Optional<ToolSchema>` forms. The test was disambiguated with `(ToolSchema) null` casts and verified green.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain -Dtest=ToolDescriptorContractTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain -Dtest=RunEventContractTest,ToolDescriptorContractTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain test` — passed; includes DomainDependencyArchTest framework dependency gate.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 04-02 can now build `ToolRegistry`, executor binding, query use cases, and public catalog DTOs on top of stable Domain descriptors.
- Plan 04-03 can use `ToolExecutionRequest`, `ToolExecutionResult`, `ProvisionPreview`, and lifecycle payloads when implementing gateway ordering, audit, events, limits, and redaction.

## Self-Check: PASSED

- Found `ToolDescriptor.java`, `ToolExecutionResult.java`, `RunEventType.java`, and this `04-01-SUMMARY.md` on disk.
- Verified commits exist in `git log --oneline --all`: `cd4abae`, `7f64df9`, `936ec80`, and `eea765e`.

---
*Phase: 04-governed-tool-registry-workspace-and-invocation-pipeline*
*Completed: 2026-06-14*
