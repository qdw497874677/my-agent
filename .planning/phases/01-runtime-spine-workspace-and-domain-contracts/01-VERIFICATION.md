---
phase: 01-runtime-spine-workspace-and-domain-contracts
verified: 2026-06-13T19:14:59Z
status: passed
score: 7/7 must-haves verified
---

# Phase 1: Runtime Spine, Workspace, and Domain Contracts Verification Report

**Phase Goal:** Establish a framework-independent and UI-agnostic Java Agent Runtime kernel plus first-class Workspace contracts in the COLA Domain layer that all cloud, GUI, provider, tool, MCP, and plugin work will build on.
**Verified:** 2026-06-13T19:14:59Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

Phase 1 goal is achieved. The codebase contains a Java 21 Maven multi-module COLA skeleton, Spring-free Domain contracts for agent/runtime/session/workspace/event/tool/model/policy concerns, reusable deterministic testkit fakes, a runnable fake General Agent loop, architecture gates, contract tests, and downstream contract documentation. The full Maven test gate passes under Java 21.

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Developer can construct an `AgentDefinition` with model config, instructions, tool allowlist, policies, interaction modes, and runtime limits without Spring dependencies. | ✓ VERIFIED | `AgentDefinition.java` is a JDK record with `modelRef`, `instructions`, `allowedToolScopes`, `policyRefs`, `RuntimeLimits`, and `supportedInputModes`; `AgentDefinitionTest` covers construction; `pi-agent-domain/pom.xml` has only test-scoped JUnit/AssertJ/ArchUnit. |
| 2 | Runtime domain model includes `Session`, `Run`, `Step`, `Message`, `ToolCall`, `ToolResult`, `Artifact`, `Attachment`, `Workspace`, `WorkspaceSession`, `WorkspaceScope`, `WorkspaceSnapshot`, and `RunEvent` with tenant/user/session/run/step/trace/workspace context. | ✓ VERIFIED | All named source files exist under `pi-agent-domain/src/main/java`; `RunEvent.java` contains tenant/user/session/run/step/workspace IDs plus trace/correlation/causation IDs and redaction metadata; `PlatformIds.java` defines typed IDs. |
| 3 | Runtime supports chat-style input, task/run input, structured form input, tool-driven execution, and future workflow/planner execution without using chat transcript as the only state model. | ✓ VERIFIED | `RunInput.java` is a sealed interface with `ChatInput`, `TaskInput`, `StructuredFormInput`, `ToolDrivenInput`, and `WorkflowPlannerInput`; `SessionEntryPayload` separates messages, artifacts, attachments, external refs, memory refs, workspace scopes, compaction summaries, and branch summaries. |
| 4 | Domain defines `WorkspaceGateway`, `CommandExecutionGateway`, Resource/Mount abstractions, and snapshot contracts without host filesystem assumptions. | ✓ VERIFIED | `WorkspaceGateway.java`, `CommandExecutionGateway.java`, `Resource.java`, `Mount.java`, and `WorkspaceSnapshot.java` exist; source scan found no `java.io.File`, `java.nio.file.Path`, `ProcessBuilder`, or host process APIs in Domain workspace contracts. |
| 5 | Fake model, fake tool, and fake workspace testkit can execute a complete General Agent loop and emit ordered events. | ✓ VERIFIED | `GeneralAgentLoop.java` uses `ModelClient`, `ToolInvoker`, `FakePolicy`, and `EventSink`; `FakeGeneralAgentLoopTest.model_tool_model_path_emits_ordered_terminal_events` validates model → tool → model path, monotonic sequence, terminal event last, and tool invocation. |
| 6 | Runtime supports cancellation, max-step/deadline budget hooks, and terminal run states. | ✓ VERIFIED | `CancellationToken.java`, `RunStatus.java`, and `GeneralAgentLoop.java` implement cancellation/deadline/max-step handling; `FakeGeneralAgentLoopTest` covers pre-cancelled, cancellation before tool call, max-step failure, and expired deadline failure. |
| 7 | Architecture tests verify COLA boundaries: Adapter depends on App, App depends on Domain/Gateways, Infrastructure implements Domain ports, and Domain/core modules do not depend on Spring Boot, Vaadin, PF4J, MCP, DB, or provider SDKs. | ✓ VERIFIED | `DomainDependencyArchTest.java` forbids Spring/Jakarta/Jackson annotations/DB/Vaadin/PF4J/MCP/provider SDKs and app/infrastructure/adapter packages; `AppDependencyArchTest.java` restricts App dependencies to Java/test/app/domain/client packages. Full `mvn -q test` passes. |

**Score:** 7/7 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `pom.xml` | Java 21 Maven parent and module skeleton | ✓ VERIFIED | Contains `<maven.compiler.release>21</maven.compiler.release>` and modules `pi-agent-client`, `pi-agent-domain`, `pi-agent-app`, `pi-agent-infrastructure`, `pi-agent-adapter-web`, `pi-testkit`. |
| `pi-agent-domain/pom.xml` | Spring-free Domain dependency baseline | ✓ VERIFIED | Only test-scoped JUnit, AssertJ, and ArchUnit dependencies; no production Spring/Jakarta/Jackson/Vaadin/PF4J/MCP/JDBC/OpenAI dependencies. |
| `DomainDependencyArchTest.java` | Domain forbidden dependency guard | ✓ VERIFIED | Forbids external frameworks/SDKs and outer project packages; executed by Maven test gate. |
| `AppDependencyArchTest.java` | App COLA direction guard | ✓ VERIFIED | Uses `should().onlyDependOnClassesThat()` for App → App/Domain/Client/Java/test dependencies. |
| `AgentDefinition.java` | Agent capability declaration | ✓ VERIFIED | Substantive immutable record with required capability fields and validation. |
| `RunInput.java` | Non-chat-only input hierarchy | ✓ VERIFIED | Sealed input union includes chat, task, structured form, tool-driven, workflow/planner variants. |
| `Run.java`, `Step.java`, `Message.java`, `ToolCall.java`, `ToolResult.java` | Explicit runtime state contracts | ✓ VERIFIED | All present with typed IDs or explicit intent/result fields and validation. |
| `RunEvent.java`, `RunEventPayload.java`, `RunEventType.java`, `RedactionMetadata.java`, `EventVisibility.java` | Provider-neutral event envelope and taxonomy | ✓ VERIFIED | Typed envelope includes context, sequence, timestamp, payload, visibility, redaction; tests cover wire names, redaction, extension payload. |
| `Workspace.java`, `WorkspaceSession.java`, `WorkspaceScope.java`, `WorkspaceSnapshot.java`, `Resource.java`, `Mount.java` | First-class workspace/resource contracts | ✓ VERIFIED | Logical workspace and resource boundary contracts exist; snapshot includes fingerprint, drift metadata, replay-safe flag. |
| `WorkspaceGateway.java`, `CommandExecutionGateway.java` | Workspace and command execution Domain ports | ✓ VERIFIED | Ports exist; command request carries workspace session, command args, environment, timeout, cancellation token; no shell implementation. |
| `Artifact.java`, `Attachment.java`, `ExternalReference.java` | Non-message work product contracts | ✓ VERIFIED | Independent records exist and are integrated into session context. |
| `Session.java`, `SessionEntry.java`, `SessionEntryPayload.java`, `SessionContext.java`, `SessionContextResolver.java` | Append-only session tree and active context resolver | ✓ VERIFIED | Resolver walks parent links from current leaf to root and accumulates separate messages/artifacts/attachments/external refs/memory/workspace context. |
| `AgentRuntime.java`, `RunContext.java`, `RunHandle.java`, `CancellationToken.java` | Runtime ports and cancellation signal | ✓ VERIFIED | Pure Domain contracts exist and compile under architecture tests. |
| `ModelClient.java`, `ModelRequest.java`, `ModelResponse.java`, `ToolInvoker.java`, `PolicyDecision.java`, `EventSink.java`, `IdGenerator.java` | Model/tool/policy/event runtime ports | ✓ VERIFIED | Framework-independent ports exist; fake loop uses them. |
| `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/*` | Reusable fake model/tool/policy/workspace/command/event/id/clock utilities | ✓ VERIFIED | Fake classes exist, are substantive, and are used by `FakeGeneralAgentLoopTest`. |
| `GeneralAgentLoop.java` | Runnable fake General Agent loop | ✓ VERIFIED | Implements `AgentRuntime`, calls `modelClient.next`, `toolInvoker.invoke`, and `eventSink.publish`, with terminal event handling. |
| `FakeGeneralAgentLoopTest.java` | End-to-end fake loop contract tests | ✓ VERIFIED | Covers model-tool-model, monotonic sequences, exactly-one-terminal-last, visibility/redaction, max-step, deadline, cancellation. |
| `docs/phase-01-domain-contracts.md` | Downstream Phase 1 contract index | ✓ VERIFIED | Exists and includes module boundaries, runtime/session/workspace/event/testkit contracts, verification commands, and explicit deferred non-goals. Minor documentation naming drift noted below. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `pom.xml` | `pi-agent-domain/pom.xml` | parent/module declaration | ✓ WIRED | Root POM declares `<module>pi-agent-domain</module>`. |
| `pi-agent-app/pom.xml` | `pi-agent-domain` | Maven dependency | ✓ WIRED | App module depends on Domain and Client; App ArchUnit guard passes. |
| `RunEvent.java` | `RunEventPayload.java` | strongly typed payload | ✓ WIRED | `RunEvent` has `RunEventPayload payload` and constructor requires non-null payload. |
| `AgentDefinition.java` | `RunInput.java` / interaction modes | supported interaction modes | ✓ WIRED | `AgentDefinition` exposes `Set<InteractionMode> supportedInputModes`; `RunInput` exposes corresponding input variants. |
| `SessionContextResolver.java` | `SessionEntry.java` | leaf-to-root traversal | ✓ WIRED | Resolver follows `entry.parentEntryId()` from `currentLeafEntryId`, reverses to root-to-leaf, and accumulates context. |
| `WorkspaceSession.java` | `WorkspaceScope.java` | run-scoped workspace constraints | ✓ WIRED | `WorkspaceSession` carries `WorkspaceScope scope`; `WorkspaceScope` contains tenant/user/session/run/workspace and allowed resources/mounts. |
| `GeneralAgentLoop.java` | `EventSink` / `EventCollector` | provider-neutral event sink | ✓ WIRED | `GeneralAgentLoop.publish` creates `RunEvent` and calls `eventSink.publish(event)`; tests pass with `EventCollector`. |
| `GeneralAgentLoop.java` | `ToolInvoker.java` | tool intent execution port | ✓ WIRED | `GeneralAgentLoop` calls `toolInvoker.invoke(toolCall, context, cancellationToken)` after policy allow; tests assert tool invocation. |
| `docs/phase-01-domain-contracts.md` | Domain/testkit contracts | downstream contract index | ✓ WIRED | Documentation references `RunEvent`, `SessionContextResolver`, `WorkspaceGateway`, and `GeneralAgentLoop`, plus deferred scopes. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| --- | --- | --- | --- | --- |
| `GeneralAgentLoop.java` | `ModelResponse response` | `modelClient.next(new ModelRequest(context, toolResults), cancellationToken)` | Yes, via injected `FakeModelClient` scripts in tests | ✓ FLOWING |
| `GeneralAgentLoop.java` | `ToolResult toolResult` | `toolInvoker.invoke(toolCall, context, cancellationToken)` | Yes, via registered `FakeToolInvoker` tool results in tests | ✓ FLOWING |
| `GeneralAgentLoop.java` | `RunEvent event` | `publish(...)` constructs events from `RunContext`, IDs, clock, payloads | Yes, `EventCollector` receives ordered event list in tests | ✓ FLOWING |
| `SessionContextResolver.java` | `SessionContext` lists and active path | `Session.entries()` and `currentLeafEntryId` traversal | Yes, contract tests build session entries and verify reconstructed context | ✓ FLOWING |
| `FakeWorkspaceGateway.java` | workspace sessions/snapshots/resources | in-memory fake maps/counters | Yes, testkit fake returns deterministic sessions/snapshots/resources without host filesystem | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Full Phase 1 Maven gate passes | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH mvn -q test` | Exited 0; only SLF4J no-provider warnings from test dependencies | ✓ PASS |
| Domain has no forbidden framework/SDK/host dependency imports in production source | Content scan for `java.io.File`, `java.nio.file.Path`, `ProcessBuilder`, Spring/Jakarta/Jackson/Vaadin/PF4J/MCP/OpenAI patterns under `pi-agent-domain/src/main/java` | No matches | ✓ PASS |
| Testkit has no host shell/filesystem execution in fake command/workspace code | Content scan for `ProcessBuilder`, `Runtime.getRuntime`, `java.io.File`, `java.nio.file.Path` under `pi-testkit/src/main/java` | No matches | ✓ PASS |
| Stub/placeholder scan | Content scan for `TODO`, `FIXME`, `XXX`, `HACK`, `PLACEHOLDER`, `placeholder`, `coming soon`, `not yet implemented`, `not available`, `return null`, empty returns | No matches in Domain/Testkit production sources | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| CORE-01 | 01-02 | Developer can define an Agent with instructions, model configuration, tool allowlist, policies, and runtime limits. | ✓ SATISFIED | `AgentDefinition.java`, `RuntimeLimits.java`, `InteractionMode.java`, `AgentDefinitionTest`. |
| CORE-02 | 01-02, 01-03 | System represents execution with explicit Session, Run, Step, Message, ToolCall, ToolResult, and RunEvent models. | ✓ SATISFIED | All named domain models exist under runtime/message/tool/session/event packages. |
| CORE-03 | 01-04 | Agent Runtime can execute General Agent loop sending messages to model, receiving tool-call intents, executing tools, appending results, continuing to completion/failure. | ✓ SATISFIED | `GeneralAgentLoop.java` with `ModelClient`, `ToolInvoker`, `EventSink`; `FakeGeneralAgentLoopTest` model-tool-model scenario passes. |
| CORE-04 | 01-02, 01-04, 01-05 | Runtime emits ordered provider-neutral RunEvents with IDs, sequence numbers, timestamps, trace IDs, context, event type, payload, redaction metadata. | ✓ SATISFIED | `RunEvent.java`, `RunEventType.java`, `RedactionMetadata.java`, `RunEventContractTest`, fake loop terminal/sequence assertions. |
| CORE-05 | 01-02, 01-04, 01-05 | Runtime supports status transitions, cancellation, deadlines, max-step budgets, terminal states. | ✓ SATISFIED | `RunStatus.java`, `StepStatus.java`, `CancellationToken.java`, `GeneralAgentLoop.java`, cancellation/deadline/max-step tests. |
| CORE-06 | 01-01, 01-05 | Core runtime contracts remain framework-independent and avoid Spring Boot, Vaadin, PF4J, MCP, provider SDK types. | ✓ SATISFIED | Domain POM has no production dependencies; `DomainDependencyArchTest` forbids framework/SDK packages; full tests pass. |
| CORE-07 | 01-02 | Core supports multiple interaction modes and does not treat chat transcript as only state model. | ✓ SATISFIED | `InteractionMode` and `RunInput` variants cover chat/task/structured form/tool-driven/workflow planner; session payloads separate non-message context. |
| CORE-08 | 01-03 | Core models artifacts, attachments, intermediate outputs, and external references separately from messages. | ✓ SATISFIED | `Artifact.java`, `Attachment.java`, `ExternalReference.java`, message references, `SessionContext` separate lists. |
| CORE-09 | 01-01, 01-05 | Codebase follows COLA layer boundaries. | ✓ SATISFIED | Multi-module skeleton and ArchUnit tests for Domain/App boundaries; Maven test gate passes. |
| WORK-01 | 01-03 | Domain defines Workspace, WorkspaceSession, WorkspaceScope, WorkspaceSnapshot, Artifact, Attachment, Resource/Mount abstractions. | ✓ SATISFIED | All listed classes/records exist in domain workspace/artifact packages. |
| WORK-02 | 01-03 | WorkspaceGateway abstracts file/resource/artifact operations without host filesystem assumptions to Domain. | ✓ SATISFIED | `WorkspaceGateway.java` is a pure Domain port; no host filesystem imports found. |
| WORK-04 | 01-03 | ToolContext and RunContext include workspaceId/session/resource scope for per-run constraints. | ✓ SATISFIED | Phase 1 defines `RunContext` carrying `WorkspaceScope`; `WorkspaceScope` contains workspace/session/run/resource/mount scope. No separate `ToolContext` exists yet, but `ToolInvoker.invoke(ToolCall, RunContext, CancellationToken)` passes the scoped runtime context to tool execution. |
| WORK-05 | 01-03 | Workspace supports snapshot/restore contracts and room for fingerprint/drift detection/replay-safe execution. | ✓ SATISFIED | `WorkspaceSnapshot` has `fingerprint`, `driftMetadata`, `replaySafe`; `WorkspaceGateway` exposes `createSnapshot` and `restoreSnapshot`. |
| OPS-04 | 01-02 | Platform models tenant ID, user ID, session ID, run ID, workspace ID, and trace ID in runtime context. | ✓ SATISFIED | `PlatformIds.java`, `Run.java`, `RunEvent.java`, `WorkspaceScope.java`, `RunContext.java`. |
| OPS-06 | 01-04, 01-05 | Platform exposes testkit utilities including fake model providers, fake tools, fake policies, and conformance tests for extensions. | ✓ SATISFIED | `pi-testkit` provides fake model/tool/policy/workspace/command/event/id/clock and fake loop contract tests. Extension conformance tests are deferred to extension phase; Phase 1 provides testkit foundation. |

No orphaned Phase 1 requirements were found: the Phase 1 IDs in `.planning/REQUIREMENTS.md` are all claimed by one or more Plan frontmatter `requirements` arrays and accounted for above.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `docs/phase-01-domain-contracts.md` | 16 | Documentation names `StructuredInput` and `WorkflowInput`, while code implements `StructuredFormInput` and `WorkflowPlannerInput`. | ℹ️ Info | Documentation naming drift only; not a goal blocker because source/tests and plan contract use the implemented names. Consider correcting in a docs cleanup. |

No blocker or warning anti-patterns were found in Domain/Testkit production source scans. No TODO/FIXME/placeholders, host shell execution, real filesystem coupling, or framework/provider SDK leakage was detected.

### Human Verification Required

None. This phase delivers contracts, ports, tests, and documentation only; there is no UI, external service, or live realtime behavior requiring manual verification.

### Gaps Summary

No blocking gaps found. Phase 1 achieved its goal and is ready for Phase 2 consumption.

---

_Verified: 2026-06-13T19:14:59Z_
_Verifier: the agent (gsd-verifier)_
