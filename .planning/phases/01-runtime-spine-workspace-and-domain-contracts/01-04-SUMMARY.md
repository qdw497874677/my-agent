---
phase: 01-runtime-spine-workspace-and-domain-contracts
plan: 04
subsystem: runtime-testkit
tags: [java21, domain-ports, testkit, agent-loop, events, cancellation]

requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: AgentDefinition, RunInput, RunStatus, RunEvent, ToolCall, ToolResult, SessionContext, WorkspaceGateway contracts
provides:
  - Framework-free runtime/model/tool/event/policy ports for fake and future production runtimes
  - Reusable pi-testkit fake model, tool, policy, workspace, command, deterministic ID, clock, and event collector utilities
  - Runnable fake General Agent loop proving model-to-tool-to-model execution with ordered terminal events
  - Cancellation, max-step, and deadline coverage for fake runtime behavior
affects: [phase-02-cloud-server, phase-03-model-provider, phase-04-tool-gateway, phase-05-web-console, phase-06-extension-fabric]

tech-stack:
  added: []
  patterns: [COLA Domain ports, provider-neutral events, deterministic fake runtime, TDD red-green commits]

key-files:
  created:
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/AgentRuntime.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunContext.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/CancellationToken.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelClient.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelResponse.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolInvoker.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/EventSink.java
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java
    - pi-testkit/src/test/java/io/github/pi_java/agent/testkit/runtime/FakeGeneralAgentLoopTest.java
  modified: []

key-decisions:
  - "Keep AgentRuntime, ModelClient, ToolInvoker, EventSink, and PolicyDecision as pure Domain contracts with no async framework, provider SDK, Spring, persistence, or host process dependency."
  - "Implement GeneralAgentLoop in pi-testkit as a synchronous fake runtime, not a production cloud executor, so later phases can validate contracts without real model keys or tools."
  - "Use RunEvent terminal events as the authoritative observable contract for fake loop outcomes, with exactly one terminal event last."

patterns-established:
  - "Runtime ports accept RunContext and CancellationToken explicitly so cancellation, deadline, workspace, trace, and tenant context stay visible at each boundary."
  - "Fake testkit components are deterministic and scriptable, enabling E2E-style contract tests without provider SDKs, shell execution, or host filesystem access."
  - "EventCollector assertion helpers verify monotonic sequence ordering and terminal-event invariants."

requirements-completed: [CORE-03, CORE-04, CORE-05, OPS-06]

duration: 7m20s
completed: 2026-06-13
---

# Phase 01 Plan 04: Runtime Ports and Fake General Agent Testkit Summary

**Framework-free runtime ports and a deterministic fake General Agent loop that executes model → tool → model with ordered terminal RunEvents, cancellation, max-step, and deadline coverage.**

## Performance

- **Duration:** 7m20s
- **Started:** 2026-06-13T18:57:09Z
- **Completed:** 2026-06-13T19:04:09Z
- **Tasks:** 3
- **Files modified:** 22

## Accomplishments

- Defined Domain runtime/model/tool/event/policy ports: `AgentRuntime`, `RunContext`, `RunHandle`, `CancellationToken`, `ModelClient`, `ModelRequest`, `ModelResponse`, `ToolInvoker`, `PolicyDecision`, `EventSink`, and `IdGenerator`.
- Added reusable `pi-testkit` fakes: scripted `FakeModelClient`, `FakeToolInvoker`, configurable `FakePolicy`, in-memory `FakeWorkspaceGateway`, shell-free `FakeCommandExecutionGateway`, `EventCollector`, `DeterministicIds`, and `DeterministicClock`.
- Implemented `GeneralAgentLoop` as a synchronous testkit runtime that emits provider-neutral `RunEvent` envelopes and proves model-tool-model execution without real providers, persistence, Spring, or host shell access.
- Added fake loop tests for ordered terminal events, max-step failure, pre-cancelled runs, cancellation before tool invocation, and expired deadline failure summaries.

## Task Commits

Each task was committed atomically. TDD tasks include red and green commits:

1. **Task 1: Define runtime/model/tool/event/policy ports**
   - `0b5413d` test: add failing runtime ports contracts
   - `f9ab9a1` feat: define runtime model tool event ports
2. **Task 2: Implement fake testkit components and GeneralAgentLoop**
   - `3165404` test: add failing fake general agent loop tests
   - `16bfba3` feat: implement fake general agent testkit loop
3. **Task 3: Add cancellation and deadline testkit coverage**
   - `2da0545` feat: add cancellation and deadline fake loop coverage

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/AgentRuntime.java` - Runtime start/cancel port.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunContext.java` - Agent, input, session, workspace, limits, cancellation, trace, and start timestamp context.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunHandle.java` - Run ID/status/failure summary result handle.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/CancellationToken.java` - Thread-safe cancellation signal with reason.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelClient.java` - Model invocation port.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelRequest.java` - Model request context plus accumulated tool results.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelResponse.java` - Sealed final-text or tool-call-intent response contract.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolInvoker.java` - Tool invocation port.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/policy/PolicyDecision.java` - Policy decision enum including approval and sandbox gates.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/EventSink.java` - Provider-neutral event publishing port.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/common/IdGenerator.java` - Deterministic/runtime ID generator contract.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/runtime/RuntimePortsContractTest.java` - Domain-only port and cancellation token contract tests.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/DeterministicIds.java` - Prefix-counter ID generator.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/DeterministicClock.java` - One-second advancing test clock.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/EventCollector.java` - Event sink plus monotonic/terminal assertions.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeModelClient.java` - Scripted fake model responses and cancellation hook.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolInvoker.java` - Tool-name keyed fake tool results and invocation tracking.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakePolicy.java` - Configurable fake policy decisions.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeWorkspaceGateway.java` - In-memory workspace session/snapshot/resource fake.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeCommandExecutionGateway.java` - Configured command-result fake with no process execution.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` - Runnable fake AgentRuntime implementation.
- `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/runtime/FakeGeneralAgentLoopTest.java` - End-to-end fake loop contract tests.

## Decisions Made

- Kept all new Domain contracts JDK-first and framework-independent, preserving COLA Domain zero outward dependency rules.
- Kept `GeneralAgentLoop` in `pi-testkit` instead of Domain/App because it is a fake contract harness, not the production runtime implementation.
- Modeled model responses as a sealed interface with final text and tool-call intent variants to avoid provider SDK leakage while supporting General Agent loop semantics.
- Treated policy decisions other than `ALLOW` as terminal policy-blocking behavior in the fake loop; future governed tool phases can implement approval/sandbox continuation flows.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Forced Maven verification to Java 21**
- **Found during:** Task 1 verification
- **Issue:** The shell Maven process initially used a Java toolchain that did not support `--release 21`, and stale Java 21-compiled test classes produced Java 17 runtime errors.
- **Fix:** Ran plan verification commands with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` so Maven compile/test used the project-required Java 21 runtime.
- **Files modified:** None
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` passed.
- **Committed in:** Not applicable (environment-only fix)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** No scope change; verification aligned with the project Java 21 baseline.

## Issues Encountered

- Running `mvn -q -pl pi-agent-domain test` without `JAVA_HOME` failed because Maven used an incompatible Java runtime/toolchain. Resolved by setting `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` for all verification commands.

## Known Stubs

None. Stub scan found only null checks/intentional nullable fields, not UI-facing placeholder data or unimplemented behavior that prevents this plan's goal.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` — passed.

## Next Phase Readiness

- Phase 1 Plan 05 can harden contract verification using the new runtime ports and fake testkit loop.
- Phase 2 can consume `AgentRuntime`, `RunContext`, `RunHandle`, and `EventSink` contracts for Cloud Server/runtime orchestration without introducing provider SDKs into Domain.
- Phase 3 and Phase 4 have stable seams for real model adapters and governed tool execution to plug in later.

## Self-Check: PASSED

- Verified key created files exist: `AgentRuntime.java`, `GeneralAgentLoop.java`, `FakeGeneralAgentLoopTest.java`, and this summary.
- Verified task commits exist in git history: `0b5413d`, `f9ab9a1`, `3165404`, `16bfba3`, and `2da0545`.
- Verified full test suite passed with Java 21: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test`.

---
*Phase: 01-runtime-spine-workspace-and-domain-contracts*
*Completed: 2026-06-13*
