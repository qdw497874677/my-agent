---
phase: 01-runtime-spine-workspace-and-domain-contracts
plan: 02
subsystem: domain-contracts
tags: [java21, domain, records, sealed-types, runtime-state, events, tdd]

requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Java 21 Maven/COLA skeleton and Spring-free Domain module gates
provides:
  - Spring-free AgentDefinition capability declaration and generic RunInput hierarchy
  - Explicit Run, Step, Message, ToolCall, ToolResult, PiError, and FailureSummary domain contracts
  - Provider-neutral RunEvent envelope with tenant/user/session/run/step/workspace/trace context
  - Reserved RunEvent taxonomy families and redaction metadata contract
affects: [runtime-core, cloud-api, persistence, sse, admin-governance, tool-governance, model-adapters]

tech-stack:
  added: []
  patterns: [Java records for immutable domain values, sealed interfaces for input and event payload unions, typed platform identifiers, TDD contract tests]

key-files:
  created:
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/common/PlatformIds.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/agent/AgentDefinition.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunInput.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/Run.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/error/PiError.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEvent.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventPayload.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventType.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/agent/AgentDefinitionTest.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/runtime/RuntimeStateModelTest.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/event/RunEventContractTest.java
  modified: []

key-decisions:
  - "Use nested JDK records in PlatformIds for tenant/user/agent/session/run/step/workspace/trace correlation identifiers."
  - "Represent generic runtime input and event payload variants with Java sealed interfaces so chat is not the only input or state model."
  - "Keep Domain contracts framework-free and serialization-neutral; adapter/client layers can map these contracts later."

patterns-established:
  - "Domain records validate non-null and non-blank required fields in canonical constructors."
  - "Collections stored by Domain records are defensively copied with Set.copyOf and Map.copyOf."
  - "RunEventType exposes stable lowercase dotted wire names while internal Java enum names remain conventional."

requirements-completed: [CORE-01, CORE-02, CORE-04, CORE-05, CORE-07, OPS-04]

duration: 7m 03s
completed: 2026-06-13
---

# Phase 01 Plan 02: Agent, Runtime State, Error, and RunEvent Contracts Summary

**Spring-free Java Domain contracts for generic Agents, explicit runtime state, governed failures, and redaction-aware RunEvents.**

## Performance

- **Duration:** 7m 03s
- **Started:** 2026-06-13T18:46:29Z
- **Completed:** 2026-06-13T18:53:32Z
- **Tasks:** 3
- **Files modified:** 22 plan files, plus this summary and planning metadata

## Accomplishments

- Defined `AgentDefinition`, `RuntimeLimits`, `InteractionMode`, and `RunInput` variants so agents can declare non-chat-only capabilities without Spring or provider SDK dependencies.
- Added explicit runtime state contracts for runs, steps, messages, tool-call intent/result separation, and normalized error/failure summaries with retry/recovery/user-action flags.
- Added a strongly typed `RunEvent` envelope carrying tenant, user, session, run, step, workspace, trace, correlation, causation, payload, visibility, and redaction context.
- Reserved the staged event taxonomy families (`run.*`, `step.*`, `model.*`, `tool.*`, `policy.*`, `workspace.*`, `artifact.*`, `message.*`) via `RunEventType.wireName()`.
- Wrote TDD contract tests for agent definitions, runtime state/error semantics, and RunEvent envelope/taxonomy behavior.

## Task Commits

Each task was committed atomically using TDD test and implementation commits:

1. **Task 1: Define AgentDefinition and generic RunInput contracts**
   - `94eac87` (test): add failing agent definition contract tests
   - `c770e6b` (feat): define agent input contracts
2. **Task 2: Define run, step, message, tool result, and failure contracts**
   - `865edbe` (test): add failing runtime state tests
   - `882c538` (feat): define runtime state contracts
3. **Task 3: Define strongly typed RunEvent envelope and taxonomy**
   - `3e12aa3` (test): add failing run event contract tests
   - `f70de92` (feat): define run event contracts

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/common/PlatformIds.java` - typed platform ID records for tenant/user/agent/session/run/step/workspace/trace/correlation/causation context.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/agent/AgentDefinition.java` - immutable agent capability declaration.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/agent/InteractionMode.java` - supported interaction mode taxonomy.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/agent/RuntimeLimits.java` - deadline, max-step, and max-tool-call limits.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunInput.java` - sealed generic runtime input hierarchy.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/Run.java` - run state contract.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunStatus.java` - run lifecycle and governance statuses.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/Step.java` - step state contract.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/StepStatus.java` - step lifecycle statuses.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/message/Message.java` - message contract with artifact and attachment references separated from content.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolCall.java` - tool invocation intent contract.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolResult.java` - tool execution result contract.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/error/PiError.java` - normalized platform error taxonomy.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/error/FailureSummary.java` - safe terminal failure summary without raw payload fields.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/EventVisibility.java` - user/admin/internal visibility taxonomy.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEvent.java` - contextual event envelope.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventPayload.java` - sealed typed payload hierarchy plus extension payload path.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventType.java` - stable event taxonomy and wire names.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RedactionMetadata.java` - redaction status, fields, and policy reference.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/agent/AgentDefinitionTest.java` - AgentDefinition and RunInput contract tests.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/runtime/RuntimeStateModelTest.java` - runtime status and failure summary contract tests.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/event/RunEventContractTest.java` - RunEvent envelope and taxonomy contract tests.

## Decisions Made

- Used nested records under `PlatformIds` instead of raw strings for core runtime context IDs, preserving lightweight JDK-only Domain types while reducing accidental ID mixups.
- Modeled `RunInput` and `RunEventPayload` as sealed hierarchies because Phase 1 needs explicit generic unions without adopting provider/framework serialization annotations in Domain.
- Kept `FailureSummary` intentionally small (`message`, `PiError`) and did not add raw payload or stack fields, preserving the redaction-by-default direction required by D-05/D-06/D-07.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Introduced `EventVisibility` during Task 2**
- **Found during:** Task 2 (Define run, step, message, tool result, and failure contracts)
- **Issue:** Task 2 required `PiError` to include a `visibility` field, but `EventVisibility` was listed under the Task 3 event files.
- **Fix:** Added the Spring-free `EventVisibility` enum with `USER`, `ADMIN`, and `INTERNAL` while implementing `PiError`, then reused it in Task 3.
- **Files modified:** `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/EventVisibility.java`, `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/error/PiError.java`
- **Verification:** `mvn -q -pl pi-agent-domain -DskipTests compile`, `mvn -q -pl pi-agent-domain test`, and `mvn -q test` passed under Java 21.
- **Committed in:** `882c538`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** The enum was already part of the plan's file list and was needed earlier to keep the error contract strongly typed. No scope beyond the plan was added.

## Issues Encountered

- Parallel execution interleaved unrelated Phase 01 Plan 03 commits and tests in the working tree history. This plan staged only its own task files and verification passed after those parallel changes landed.
- Maven emitted SLF4J no-provider warnings from ArchUnit/test dependencies; tests still passed and no production logging dependency was added to Domain.

## User Setup Required

None - no external service configuration required. Developers should run Maven with Java 21 selected.

## Known Stubs

None - no placeholder production behavior, TODO/FIXME markers, or UI data stubs were introduced in this plan's files.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH mvn -q -pl pi-agent-domain -DskipTests compile` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH mvn -q -pl pi-agent-domain test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH mvn -q test` — passed.

## Self-Check: PASSED

- Found created files: `AgentDefinition.java`, `RunInput.java`, `Run.java`, `PiError.java`, `RunEvent.java`, `RunEventPayload.java`, `RunEventType.java`, and all three plan test files.
- Found task commits: `94eac87`, `c770e6b`, `865edbe`, `882c538`, `3e12aa3`, `f70de92`.
- Verified no known stubs in this plan's created files.

## Next Phase Readiness

- Plan 01-03 can build Workspace, Artifact/Attachment, and Session tree contracts on the typed IDs, runtime state, Message references, and RunEvent envelope introduced here.
- Phase 2 persistence/SSE can depend on stable wire names and contextual event envelope fields without coupling Domain to Jackson or Spring.

---
*Phase: 01-runtime-spine-workspace-and-domain-contracts*
*Completed: 2026-06-13*
