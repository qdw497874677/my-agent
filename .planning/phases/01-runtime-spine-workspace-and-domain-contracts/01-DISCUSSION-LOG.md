# Phase 1: Runtime Spine, Workspace, and Domain Contracts - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-13
**Phase:** 01-Runtime Spine, Workspace, and Domain Contracts
**Areas discussed:** Event and State Contracts, Runtime Input Model, Workspace Boundary, Module and API Exposure, Test Gates, Error Classification, pi Reference Boundary

---

## Event and State Contracts

| Option | Description | Selected |
|--------|-------------|----------|
| Full stable event contract | Phase 1 locks full envelope with event/run/session/workspace/trace/visibility/redaction fields | ✓ |
| Minimal event contract | Only eventId/runId/type/sequence/timestamp/payload | |
| Internal events first | No stable external contract in Phase 1 | |

**User's choice:** Full stable event contract.

| Option | Description | Selected |
|--------|-------------|----------|
| Include suspended/blocked | Run/Step states support suspended and policy-blocked future governance paths | ✓ |
| Minimal terminal model | queued/running/succeeded/failed/cancelled only | |
| Unified lifecycle | Same generic lifecycle enum for Run and Step | |

**User's choice:** Include suspended/blocked.

| Option | Description | Selected |
|--------|-------------|----------|
| Staged taxonomy | Reserve run/step/model/tool/policy/workspace/artifact/message event families | ✓ |
| Runtime-only types | Only run/step/model/tool/message now | |
| Open strings | No taxonomy/enum | |

**User's choice:** Staged taxonomy.

| Option | Description | Selected |
|--------|-------------|----------|
| Strong envelope, layered payload | Core payloads strongly typed, extension payload path available | ✓ |
| Fully strong typed | Every payload is a Java record | |
| JSON payload | Payload as JsonNode/Map | |

**User's choice:** Strong envelope, layered payload.

---

## Runtime Input Model

| Option | Description | Selected |
|--------|-------------|----------|
| Generic RunInput union | Chat/task/form/tool-driven/workflow placeholder modeled separately | ✓ |
| Message-first extension | Everything starts from messages plus metadata | |
| Command-style DTO | mode + payload interpreted outside Domain | |

**User's choice:** Generic RunInput union.

| Option | Description | Selected |
|--------|-------------|----------|
| Messages and work products separated | SessionContext has messages/artifacts/attachments/external refs/memory refs/workspace scope | ✓ |
| Messages with attachments | Main state remains message timeline | |
| Event replay derived | Session state derives primarily from RunEvents | |

**User's choice:** Messages and work products separated.

| Option | Description | Selected |
|--------|-------------|----------|
| Complete capability declaration | AgentDefinition includes instructions/model/tools/policies/limits/input modes/workspace/output policy | ✓ |
| Minimal runnable | instructions/model/tool allowlist/runtime limits only | |
| Metadata + runtime opts | Policies/capabilities outside AgentDefinition | |

**User's choice:** Complete capability declaration.

| Option | Description | Selected |
|--------|-------------|----------|
| Runnable fake loop | FakeModel + FakeToolInvoker prove model→tool→model | ✓ |
| Interfaces only | No loop implementation in Phase 1 | |
| Near-real loop | More complete streaming/provider-ready loop | |

**User's choice:** Runnable fake loop.

---

## Workspace Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Full domain abstraction | Workspace, WorkspaceSession, Scope, Snapshot, Resource, Mount, Artifact, Attachment, ExternalReference | ✓ |
| Minimal Workspace | WorkspaceId/Scope/Artifact/Attachment only | |
| Ports first | Mostly Gateway interfaces, fewer domain objects | |

**User's choice:** Full domain abstraction.

| Option | Description | Selected |
|--------|-------------|----------|
| Port + fake only | Define CommandExecutionGateway and fake; no real shell | ✓ |
| Local temp implementation | Provide test-only local command executor | |
| No command yet | Defer command concepts to Phase 4 | |

**User's choice:** Port + fake only.

| Option | Description | Selected |
|--------|-------------|----------|
| Contract first | Snapshot/restore/fingerprint/drift metadata, fake may simulate | ✓ |
| Full fake restore | In-memory state save/restore | |
| Placeholder type only | Record without restore semantics | |

**User's choice:** Contract first.

| Option | Description | Selected |
|--------|-------------|----------|
| Independent referenceable entities | Artifacts/Attachments can be referenced by Message/RunEvent/ToolResult | ✓ |
| Message attachments | Artifact/Attachment mainly attached to Message | |
| Event-created products | Artifact primarily created/referenced through RunEvent payloads | |

**User's choice:** Independent referenceable entities.

---

## Module and API Exposure

| Option | Description | Selected |
|--------|-------------|----------|
| Full skeleton modules | Parent POM plus client/domain/app/testkit and boundary-ready adapter/infra skeletons | ✓ |
| Domain/testkit only | Minimal modules | |
| Complete all modules | Full future module graph as empty skeletons | |

**User's choice:** Full skeleton modules.

| Option | Description | Selected |
|--------|-------------|----------|
| Layered public API | client DTOs, domain contracts/ports/value objects, internals hidden | ✓ |
| Domain all public | Most domain models/services public | |
| Minimal public | Only small runtime surface public | |

**User's choice:** Layered public API.

| Option | Description | Selected |
|--------|-------------|----------|
| JDK-first | Avoid Spring/Jackson/Jakarta annotations in Domain | ✓ |
| Allow Jackson annotations | Domain models may carry serialization annotations | |
| Allow Jakarta Validation | Domain/client records carry validation annotations | |

**User's choice:** JDK-first.

| Option | Description | Selected |
|--------|-------------|----------|
| `io.github.pi_java` | Use open-source style groupId/package | ✓ |
| `com.pi.agent` | Short product namespace | |
| Planner decides later | Do not lock namespace | |

**User's choice:** `io.github.pi_java`.

---

## Test Gates

| Option | Description | Selected |
|--------|-------------|----------|
| Complete contract gate | Unit + fake loop + event ordering + cancellation/budget + ArchUnit + serialization snapshot | ✓ |
| Core unit gate | Domain/app unit + ArchUnit only | |
| Compile skeleton | Compile and smoke tests only | |

**User's choice:** Complete contract gate.

| Option | Description | Selected |
|--------|-------------|----------|
| Full fake suite | FakeModelClient, FakeToolInvoker, FakePolicy, FakeWorkspaceGateway, FakeCommandExecutionGateway, EventCollector, ID/Clock fixtures | ✓ |
| Model/tool first | Only model/tool/EventCollector | |
| Local fakes only | No reusable pi-testkit fake suite | |

**User's choice:** Full fake suite.

| Option | Description | Selected |
|--------|-------------|----------|
| Strict boundaries | Domain forbidden deps and COLA direction/internal package checks | ✓ |
| Domain forbidden deps only | Narrower ArchUnit gate | |
| No ArchUnit yet | Manual review | |

**User's choice:** Strict boundaries.

| Option | Description | Selected |
|--------|-------------|----------|
| Sequence + snapshot + terminal | Monotonic sequence, unique last terminal, visibility/redaction, serialization snapshots | ✓ |
| Sequence only | Ordered events and terminal status only | |
| State machine only | Event serialization not tested | |

**User's choice:** Sequence + snapshot + terminal.

---

## Error Classification

| Option | Description | Selected |
|--------|-------------|----------|
| Cross-domain error taxonomy | Runtime/model/tool/policy/workspace/validation/cancellation/timeout/internal categories | ✓ |
| RuntimeError only | Only runtime failures now | |
| Exceptions pass through | No error contract in Phase 1 | |

**User's choice:** Cross-domain error taxonomy.

| Option | Description | Selected |
|--------|-------------|----------|
| Terminal failure summary | Terminal event and Run result carry normalized failure summary | ✓ |
| Events only | Error detail only in event payload | |
| Exceptions only | Outer layer catches exceptions | |

**User's choice:** Terminal failure summary.

| Option | Description | Selected |
|--------|-------------|----------|
| Markers only | retryable/recoverable/userActionRequired markers, no automatic retry | ✓ |
| Basic retry | Fake loop implements retry budget | |
| Fully deferred | Retry/recovery semantics later | |

**User's choice:** Markers only.

---

## pi Reference Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Concept reference only | Borrow concepts, do not migrate TS/CLI/TUI/Node assumptions | ✓ |
| Deep comparative mapping | Systematically read packages and map to Java | |
| Do not look at pi | Avoid migration bias | |

**User's choice:** Concept reference only.

| Option | Description | Selected |
|--------|-------------|----------|
| runtime/tool/session/events | Focus on loop, tool call intent/result, session harness, event stream, provider abstraction | ✓ |
| coding-agent workspace | Focus on coding-agent workspace/tool experience | |
| extension/skills | Focus on extension and skills | |

**User's choice:** runtime/tool/session/events.

| Option | Description | Selected |
|--------|-------------|----------|
| Reference only canonical ref | List `/root/workspace/pi-agent` as reference-only, not normative | ✓ |
| Do not list canonical | Mention only in specifics | |
| List concrete packages as required | Require reading specific package paths | |

**User's choice:** Reference only canonical ref.

**Notes:** User specifically asked what parts of pi are being referenced and how pi session storage is implemented. The answer clarified pi's append-only tree + leaf pointer + buildContext design and rejected JSONL/cwd/Node/CLI assumptions for pi-java.

---

## the agent's Discretion

- Exact Java class/record names, package subdivisions, and event subtype names remain planner discretion within the locked boundaries.
- Exact fake fixture DSL and assertion helper shape remain planner discretion.

## Deferred Ideas

- PostgreSQL persistence and Flyway migrations: Phase 2.
- Real shell/file workspace execution and provision/impact implementation: Phase 4.
- Real provider SDK/OpenAI-compatible streaming: Phase 3.
- MCP and dynamic plugin integrations: Phases 7 and 8.
