# Phase 1: Runtime Spine, Workspace, and Domain Contracts - Context

**Gathered:** 2026-06-13
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1 establishes the framework-independent Java Agent Runtime kernel and first-class Workspace contracts in the COLA Domain layer. It must define stable domain models, runtime ports, event/state contracts, session context structure, workspace/resource abstractions, cancellation/budget hooks, and fake testkit support that later Cloud Server, persistence, provider, tool, MCP, plugin, Web Console, and Admin Governance phases build on.

This phase does **not** implement real provider SDK adapters, Spring Boot REST/SSE, PostgreSQL persistence, Vaadin UI, MCP clients, PF4J/dynamic plugin loading, unrestricted shell/file execution, or real host filesystem workspace access.

</domain>

<decisions>
## Implementation Decisions

### Event and State Contracts
- **D-01:** Phase 1 must define a complete, stable `RunEvent` envelope, not an internal-only event shape. The envelope should include at least `eventId`, tenant/user/session/run/step/workspace context, `sequence`, `timestamp`, `type`, `traceId`, `correlationId`/`causationId` or equivalent causality fields, `payload`, `visibility`, and `redaction` metadata. Later phases may extend but should not break this contract.
- **D-02:** Run and Step state machines must include governance-ready states from the start. Run status should include queued/running/suspended/cancelling/succeeded/failed/cancelled/policy-blocked semantics. Step status should include pending/running/suspended/succeeded/failed/cancelled/skipped semantics or equivalent strongly typed variants.
- **D-03:** Define a staged event taxonomy in Phase 1, even if the fake runtime only emits a subset. The taxonomy should reserve core families such as `run.*`, `step.*`, `model.*`, `tool.*`, `policy.*`, `workspace.*`, `artifact.*`, and `message.*` so later phases do not invent incompatible event names.
- **D-04:** `RunEvent` envelope should be strongly typed. Payload should be layered: core runtime event payloads are strongly typed Java records/sealed types, while an explicit extension payload path remains available for future adapters.
- **D-05:** Define a cross-domain error taxonomy in Phase 1. It should cover runtime, model, tool, policy, workspace, validation, cancellation, timeout, and internal errors with normalized category/code/severity/visibility semantics.
- **D-06:** Terminal run events and terminal run results must carry a normalized failure summary for failed/cancelled/policy-blocked outcomes. This summary must avoid raw secret or sensitive payload leakage.
- **D-07:** Error summaries should include `retryable`, `recoverable`, and `userActionRequired` flags or equivalent forward-compatible markers. Phase 1 should not implement automatic retry/recovery.

### Runtime Input Model and Agent Definition
- **D-08:** Runtime input must be modeled as a generic `RunInput` union/sealed hierarchy rather than as chat messages only. Include variants or placeholders for chat-style input, task/run input, structured/form input, tool-driven input, and future workflow/planner input.
- **D-09:** Session context must separate messages from artifacts, attachments, external references, memory references, and workspace scope. Chat transcript must not be the only runtime state model.
- **D-10:** `AgentDefinition` should include complete capability declarations: instructions, model reference/configuration, allowed tools/scopes, policies, runtime limits, supported input modes, workspace policy, and output/artifact policy.
- **D-11:** Phase 1 must implement a runnable fake General Agent loop using fake model and fake tool invoker/testkit support. It should prove a model→tool→model path, ordered events, state transitions, cancellation/budget handling, and terminal states without using real provider SDKs or the future governed tool gateway implementation.

### Session Storage and pi Reference Mapping
- **D-12:** Session should be modeled as an append-only entry tree, not a linear chat transcript. Borrow the concept from pi's `SessionTreeEntry` design: entries have stable IDs, parent entry IDs, timestamps, types, and payloads; a current leaf pointer identifies the active context path.
- **D-13:** Phase 1 should define a `SessionContextResolver` or equivalent domain service that rebuilds current runtime context from the active leaf path. It should account for messages, active model/tool settings, compaction summaries, branch summaries, artifacts, attachments, external references, memory refs, and workspace scope.
- **D-14:** Phase 1 should provide in-memory/fake session storage for contract tests. PostgreSQL implementation, JSONB persistence, tables, migrations, and query read models belong to Phase 2.
- **D-15:** Do not migrate pi's JSONL file persistence, cwd-encoded session directory, `parentSessionPath`, Node execution environment, CLI/TUI session manager, or local filesystem assumptions. Convert those concepts to cloud-safe IDs, tenant/workspace context, future PostgreSQL persistence, and Workspace/Policy boundaries.

### Workspace Boundary
- **D-16:** Phase 1 must define full Workspace domain abstractions: `Workspace`, `WorkspaceSession`, `WorkspaceScope`, `WorkspaceSnapshot`, `Resource`, `Mount`, `Artifact`, `Attachment`, and `ExternalReference` or equivalent value types.
- **D-17:** `WorkspaceGateway` and `CommandExecutionGateway` are Domain ports only in Phase 1. Provide fake/testkit implementations, but do not provide real shell or unrestricted host filesystem execution.
- **D-18:** Snapshot support is contract-first. Define snapshot/restore semantics, snapshot IDs, fingerprint/drift metadata, and replay-safety hooks. Fake workspace may simulate snapshot behavior, but Phase 1 must not promise production-grade snapshot consistency.
- **D-19:** Artifacts and Attachments are independent entities that can be referenced by Message, RunEvent, ToolResult, SessionContext, or Workspace state. They are not merely fields attached to a chat message.

### Module and API Exposure
- **D-20:** Phase 1 should create a full Maven multi-module skeleton rather than only a domain/testkit subset. Expected modules include parent POM plus client contracts, domain, app, testkit, and empty or boundary-ready adapter/infrastructure skeletons where useful for enforcing COLA dependencies.
- **D-21:** Public API is layered. Client module exposes external DTOs/commands/queries/response envelopes. Domain exposes stable contracts, ports, value objects, runtime models, and state/event types. Runtime implementation details should live in `internal` packages or equivalent non-public API areas.
- **D-22:** Domain must be JDK-first and framework-independent. Avoid Spring, Vaadin, PF4J, MCP SDK, DB, provider SDK, Jakarta validation annotations, and Jackson annotations in Domain. Serialization/validation concerns should live in client/adapter/test layers unless a later plan proves a narrow exception is necessary.
- **D-23:** Use `io.github.pi_java` as the initial groupId/package namespace unless planning discovers a stronger project-owned namespace.

### Test Gates and Verification
- **D-24:** Phase 1 minimum passing gate is a complete contract gate: `mvn test` should cover unit tests, fake General Agent loop, event ordering, terminal event behavior, cancellation/deadline/max-step budgets, ArchUnit boundaries, and event serialization/snapshot or contract tests.
- **D-25:** `pi-testkit` should provide a reusable fake suite: `FakeModelClient`, `FakeToolInvoker`, `FakePolicy`, `FakeWorkspaceGateway`, `FakeCommandExecutionGateway`, `EventCollector`, deterministic ID generator, deterministic clock, and assertion helpers.
- **D-26:** ArchUnit checks must be strict. Domain must not depend on Spring/Jakarta/Jackson annotations/DB/Vaadin/PF4J/MCP/provider SDKs. COLA direction must be enforced: Adapter → App → Domain, Infrastructure implements Domain ports, and internal packages should not be consumed inappropriately.
- **D-27:** Event contract tests should verify monotonic sequence ordering, exactly one terminal event that is last for a run, presence of redaction/visibility fields, and stable serialization snapshots/contracts for key event types.

### pi Reference Boundary
- **D-28:** `/root/workspace/pi-agent` is reference-only, not normative. Downstream agents may inspect it for runtime/tool/session/event/provider abstraction concepts, but must not copy TypeScript module structure or local CLI/TUI behavior into pi-java.
- **D-29:** If studying pi, focus on `packages/agent/src/agent-loop.ts`, `packages/agent/src/harness/types.ts`, `packages/agent/src/harness/session/session.ts`, `packages/agent/src/harness/session/jsonl-storage.ts`, `packages/agent/src/harness/session/memory-storage.ts`, and `packages/agent/src/harness/compaction/compaction.ts` for conceptual inspiration.
- **D-30:** The pi concepts to borrow are append-only session tree, current leaf pointer, leaf-to-root context reconstruction, compaction entry, branch summary, AgentLoop turn semantics, tool call intent/result separation, and execution environment abstraction. The pi concepts to reject are JSONL persistence, cwd-based session scoping, Node `fs`/`child_process`, unrestricted local shell/file access, and CLI/TUI session UX.

### the agent's Discretion
- Exact Java record/class names, package subdivisions, and builder/factory style are left to planner/researcher discretion as long as the above decisions and COLA boundaries are preserved.
- Exact event subtype names within the approved taxonomy are flexible if they remain consistent, testable, and documented.
- Exact fake model/tool scripts and test fixture DSLs are flexible, but must prove the contract gates above.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project and Phase Scope
- `.planning/ROADMAP.md` §Phase 1 — Phase goal, requirements, success criteria, and explicit exclusions.
- `.planning/REQUIREMENTS.md` — Phase 1 requirements: CORE-01..CORE-09, WORK-01, WORK-02, WORK-04, WORK-05, OPS-04, OPS-06.
- `.planning/PROJECT.md` — Product vision, constraints, out-of-scope items, reference boundary, and key platform decisions.
- `.planning/STATE.md` — Current phase focus and key planning concerns for Phase 1.
- `README.md` — Product positioning, architecture principles, Workspace boundary, Event-first, Tool Gateway-first, and v1 verification strategy.

### Architecture and Stack Research
- `.planning/research/ARCHITECTURE.md` — COLA module split, component boundaries, domain model, event envelope, runtime/session/tool/workspace patterns, and anti-patterns.
- `.planning/research/SUMMARY.md` — Research summary, Phase 1 deliverables, risks, and research flags.
- `.planning/research/STACK.md` — Java 21/Maven/Spring Boot/Spring AI/Testcontainers/ArchUnit stack guidance; Phase 1 should only use framework-independent portions where applicable.

### Reference Only: TypeScript pi
- `/root/workspace/pi-agent` — Reference only, not normative. Use for conceptual comparison of Agent Loop, session tree, buildContext, compaction, tool calls, and event concepts. Do not migrate TypeScript code, JSONL storage, cwd/session manager, local shell/filesystem, CLI/TUI, or Node assumptions.
- `/root/workspace/pi-agent/packages/agent/src/agent-loop.ts` — Reference for turn loop and model→tool→model semantics only.
- `/root/workspace/pi-agent/packages/agent/src/harness/types.ts` — Reference for session entry concepts, runtime hooks, tool result shape, execution environment abstraction, and event concepts only.
- `/root/workspace/pi-agent/packages/agent/src/harness/session/session.ts` — Reference for context reconstruction from session tree.
- `/root/workspace/pi-agent/packages/agent/src/harness/session/jsonl-storage.ts` — Reference to understand pi's append-only tree mechanics; do not migrate JSONL persistence.
- `/root/workspace/pi-agent/packages/agent/src/harness/session/memory-storage.ts` — Reference for test/in-memory session storage patterns.
- `/root/workspace/pi-agent/packages/agent/src/harness/compaction/compaction.ts` — Reference for compaction/cut-point concepts.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- No reusable Java/Maven source assets exist yet. The repository contains planning and research documents only.
- `/root/workspace/pi-agent` is available locally as a conceptual reference, but it is outside this repo and must not be treated as implementation source.

### Established Patterns
- COLA is the governing architecture: Adapter → App → Domain ← Infrastructure. Domain has zero outward dependencies.
- Runtime core must be framework-independent, Java-first, cloud-safe, and not shaped by Chat UI, CLI/TUI, provider SDKs, MCP, PF4J, Vaadin, DB, or Spring Boot.
- Event-first design is central: `RunEvent` becomes the future shared contract for REST/SSE, Admin GUI, audit, observability, and future TUI/CLI.
- Workspace is a first-class domain boundary, not a host filesystem alias.

### Integration Points
- Phase 1 will likely create the first Maven parent POM and module skeleton.
- Later Phase 2 Cloud Server and persistence will consume Phase 1 `RunEvent`, `Run`, `Session`, `SessionEntry`, and Workspace contracts.
- Later Phase 4 Tool Gateway must plug into the Phase 1 tool intent/result/event/state abstractions without bypassing policy/audit.
- Later Phase 5 Web Console must consume public APIs/read models/events rather than Domain internals.

</code_context>

<specifics>
## Specific Ideas

- User asked explicitly what parts of pi are being referenced and how pi session storage works. Context should guide downstream agents to borrow pi's append-only session tree and context reconstruction concepts, not its local JSONL storage implementation.
- Session in pi-java should support tree/leaf semantics so later continuation, branching, compaction, and replay/debug-friendly views are possible.
- Workspace design should preserve Mirage-like concepts noted in README: Resource, Mount, WorkspaceSession, Snapshot, Provision/impact estimation.

</specifics>

<deferred>
## Deferred Ideas

- Real PostgreSQL session/event persistence and Flyway migrations — Phase 2.
- Real host filesystem, shell, command execution, provision/impact implementation, and governed Workspace-backed tools — Phase 4.
- Real model provider SDK/OpenAI-compatible streaming — Phase 3.
- MCP resource/tool adapters — Phase 7.
- Dynamic plugin workspace/tool contributions — Phase 8.
- Full replay/crash-resumable durable execution — deferred unless later roadmap explicitly promotes it.

</deferred>

---

*Phase: 01-runtime-spine-workspace-and-domain-contracts*
*Context gathered: 2026-06-13*
