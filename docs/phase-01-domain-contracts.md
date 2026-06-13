# Phase 1 Domain Contracts

Phase 1 establishes the framework-independent Domain/runtime spine that later REST/SSE, persistence, provider, tool, MCP, plugin, Web Console, and governance phases consume. These contracts are intentionally Java/JDK-first and serialization-neutral.

## Module Boundaries

- `pi-agent-domain` contains the public Domain contracts: runtime state, event envelope, session tree, workspace boundary, model/tool/policy ports, and cross-domain errors. Per D-20 through D-23, it must not depend on Spring, Jakarta, Jackson annotations, database APIs, Vaadin, PF4J, MCP SDKs, provider SDKs, or outer COLA packages.
- `pi-agent-app` may orchestrate application use cases over Domain and client contracts, but Domain remains independent of App/Adapter/Infrastructure per D-26.
- `pi-agent-client` is reserved for external DTOs, commands, queries, and response envelopes; it must not become the Domain model.
- `pi-agent-adapter` and `pi-agent-infrastructure` are boundary-ready shells for later REST/SSE, persistence, provider, UI, MCP, and plugin implementations.
- `pi-testkit` provides deterministic fakes for contract tests and downstream E2E without model keys, host shell access, or real persistence, matching D-11, D-24, and D-25.

## Agent and Runtime Contracts

- `AgentDefinition` captures agent identity, instructions, `modelRef`, allowed tools/scopes, policy references, `RuntimeLimits`, supported `InteractionMode` values, workspace policy, and output/artifact policy, satisfying D-08 through D-10.
- `RunInput` is a sealed input union for `ChatInput`, `TaskInput`, `StructuredInput`, `ToolDrivenInput`, and `WorkflowInput`, keeping the runtime generic rather than Chat-only.
- `AgentRuntime` defines the minimal runtime port with `start(RunContext)` and `cancel(String runId, String reason)`.
- `RunContext` binds `AgentDefinition`, `RunInput`, `SessionContext`, `WorkspaceScope`, `RuntimeLimits`, `CancellationToken`, trace ID, and start time.
- `Run`, `Step`, `RunStatus`, `StepStatus`, and `RunHandle` define governance-ready lifecycle state, terminal results, and failure summaries per D-02, D-06, and D-07.
- `ModelClient`, `ModelRequest`, and `ModelResponse` form the provider-neutral model port. `ModelResponse` separates final text from `ToolCallIntent`, preserving the model→tool→model loop semantics from D-11 and D-30 without exposing real provider SDK types.
- `ToolCall`, `ToolResult`, `ToolInvoker`, and `PolicyDecision` are Phase 1 contracts only; the governed `ToolExecutionGateway`, registry, audit, and approval pipeline are deferred to later phases.

## Session Tree Contracts

- `Session` is an append-only entry tree with a current leaf pointer, following D-12 and rejecting pi's local JSONL/cwd assumptions from D-15 and D-30.
- `SessionEntry` stores stable entry ID, parent entry ID, timestamp, and a strongly typed `SessionEntryPayload`.
- `SessionEntryPayload` separates `MessageEntry`, `ArtifactEntry`, `AttachmentEntry`, `ExternalReferenceEntry`, `MemoryReferenceEntry`, `WorkspaceScopeEntry`, `CompactionSummaryEntry`, and `BranchSummaryEntry` so chat transcript is not the only runtime state model per D-09 and D-13.
- `SessionContextResolver` reconstructs the active context by walking the current leaf path and returns `SessionContext` with separate messages, artifacts, attachments, external references, memory references, optional workspace scope, and active path.
- `Message`, `Artifact`, `Attachment`, and `ExternalReference` are independent Domain concepts and may be referenced by sessions, events, tool results, or workspace state per D-19.

## Workspace Contracts

- `Workspace`, `WorkspaceSession`, `WorkspaceScope`, `WorkspaceSnapshot`, `Resource`, and `Mount` model Workspace as a logical cloud-safe runtime boundary rather than a host filesystem alias, satisfying D-16 and D-18.
- `WorkspaceGateway` is a Domain port for opening/closing sessions, finding resources, and snapshot/restore operations. Implementations belong to Infrastructure or Testkit and must not leak host filesystem assumptions into Domain.
- `CommandExecutionGateway` is a Domain port for command execution requests/results through a controlled workspace boundary. Phase 1 provides no unrestricted shell or process implementation, per D-17.
- `FakeWorkspaceGateway` and `FakeCommandExecutionGateway` in `pi-testkit` simulate workspace and command contracts for automated verification only.

## Event Envelope

- `RunEvent` is the stable event envelope required by D-01. It includes `eventId`, tenant/user/session/run/step/workspace context, monotonic `sequence`, `timestamp`, `RunEventType`, `TraceId`, `CorrelationId`, `CausationId`, strongly typed `RunEventPayload`, `EventVisibility`, and `RedactionMetadata`.
- `RunEventType` reserves stable wire-name families from D-03: `run.*`, `step.*`, `model.*`, `tool.*`, `policy.*`, `workspace.*`, `artifact.*`, and `message.*`.
- `RunEventPayload` is a sealed payload hierarchy for run lifecycle, step lifecycle, model deltas, tool proposals/results, policy decisions, workspace snapshots, artifacts, messages, and explicit `ExtensionPayload` for future adapter-specific data per D-04.
- `RedactionMetadata` and `EventVisibility` are mandatory on every event so REST/SSE, Admin UI, audit, and logs can honor visibility and redaction boundaries from D-06 and D-27.
- Terminal run events emitted by the fake runtime are exactly one of `run.completed`, `run.failed`, `run.cancelled`, or `run.policy_blocked`, and contract tests require the terminal event to be last.

## Testkit

- `GeneralAgentLoop` is a synchronous fake implementation of `AgentRuntime` that proves model→tool→model behavior, ordered `RunEvent` emission, cancellation, deadlines, max-step budgets, policy blocking, and terminal states without real providers or tools, satisfying D-11.
- `FakeModelClient`, `FakeToolInvoker`, and `FakePolicy` script deterministic model responses, tool results, and policy decisions.
- `EventCollector` collects events and exposes shared assertions for monotonic sequences and terminal event invariants.
- `DeterministicIds` and `DeterministicClock` make test runs repeatable for contract and downstream E2E verification.
- `FakeWorkspaceGateway` and `FakeCommandExecutionGateway` keep workspace tests inside controlled fakes rather than host resources.

## Verification Commands

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test`
- In environments where Maven already runs on Java 21, the equivalent commands are `mvn -q test` and `mvn test`.

## Explicitly Deferred

- real provider SDK adapters
- Spring Boot REST/SSE
- PostgreSQL/Flyway persistence
- Vaadin UI
- MCP clients
- PF4J/dynamic plugin loading
- unrestricted shell/file execution
- real host filesystem workspace access
- crash-resumable durable execution unless later roadmap promotes it
