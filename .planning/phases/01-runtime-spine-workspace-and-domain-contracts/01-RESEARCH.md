# Phase 1 Research: Runtime Spine, Workspace, and Domain Contracts

**Researched:** 2026-06-13  
**Status:** Ready for planning  
**Scope:** Phase-specific planning research distilled from project research, Phase 1 context, COLA constraints, and Java runtime/test patterns.

## Planning Recommendation

Phase 1 should create a Java 21 / Maven multi-module foundation with a Spring-free Domain module, a Client contracts module, an App orchestration module, Infrastructure and Adapter skeletons for COLA boundary enforcement, and a reusable Testkit module. The implementation should avoid Spring Boot, Vaadin, PF4J, MCP SDKs, provider SDKs, JDBC, Jackson annotations, and Jakarta validation in Domain.

The minimum useful Phase 1 exit is not a full cloud server; it is a runnable in-memory General Agent loop that proves:

- `AgentDefinition` can be constructed with instructions, model reference/configuration, allowed tool scopes, policies, runtime limits, supported input modes, workspace policy, and output/artifact policy.
- `Session`, append-only session entries, `Run`, `Step`, `Message`, `ToolCall`, `ToolResult`, `Artifact`, `Attachment`, `ExternalReference`, `Workspace*`, and `RunEvent` contracts exist as strongly typed Java records/classes.
- The event envelope includes `eventId`, tenant/user/session/run/step/workspace context, `sequence`, `timestamp`, `type`, `traceId`, `correlationId`, `causationId`, `payload`, `visibility`, and `redaction` metadata.
- A fake model → fake tool → fake model path can emit ordered events, respect max-step/deadline/cancellation hooks, and end with exactly one terminal run event.
- ArchUnit tests enforce COLA and Domain dependency rules.

## Standard Stack for This Phase

Use only framework-independent Java libraries in production modules:

- Java 21 language level.
- Maven multi-module parent with `maven-surefire-plugin` and `maven-compiler-plugin`.
- JUnit Jupiter, AssertJ, and ArchUnit in test scope.
- No Spring, Vaadin, PF4J, MCP, provider SDK, JDBC, Jackson annotation, or Jakarta annotation dependency in `pi-agent-domain`.

## Contract Shape Decisions

- Prefer Java records for immutable IDs/value objects and strongly typed event payloads.
- Use sealed interfaces for `RunInput`, `RunEventPayload`, `SessionEntryPayload`, `PiError`, and model/tool response variants where useful.
- Keep extension payloads explicit: for Phase 1, a small `ExtensionEventPayload(String schema, String version, Map<String, Object> attributes)` path is acceptable. Do not make all core payloads untyped maps.
- Use typed enums for run/step status, event type family, visibility, redaction classification, error category/severity, interaction mode, artifact/attachment kinds, and workspace scope.
- Use `java.time.Clock`, `Instant`, `Duration`, and deterministic ID generator ports in runtime/testkit rather than static clocks/random IDs.

## Suggested Package Ownership

```text
io.github.pi_java.agent.client
io.github.pi_java.agent.domain.agent
io.github.pi_java.agent.domain.error
io.github.pi_java.agent.domain.event
io.github.pi_java.agent.domain.model
io.github.pi_java.agent.domain.policy
io.github.pi_java.agent.domain.runtime
io.github.pi_java.agent.domain.session
io.github.pi_java.agent.domain.tool
io.github.pi_java.agent.domain.workspace
io.github.pi_java.agent.app
io.github.pi_java.agent.infrastructure
io.github.pi_java.agent.adapter
io.github.pi_java.agent.testkit
```

## Validation Architecture

Phase 1 validation should be contract-heavy and fast enough to run after every task:

- **Quick command:** `mvn -q test`
- **Full command:** `mvn test`
- **Boundary tests:** ArchUnit tests in `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java` and `pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java`.
- **Contract tests:** event ordering, event envelope required fields, exactly one terminal event last, redaction/visibility presence, state transition legality, session tree context reconstruction, workspace snapshot fake semantics.
- **Loop tests:** `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/runtime/FakeGeneralAgentLoopTest.java` proves fake model → fake tool → fake model, max-step budget, deadline/cancellation, and terminal failure summaries.

## Pitfalls to Avoid

- Do not implement real OpenAI/Spring AI/provider adapters in Phase 1.
- Do not implement real host filesystem or shell access; only ports plus fakes.
- Do not model session as a linear chat transcript; use append-only entries with parent IDs and a current leaf pointer.
- Do not allow the Agent Loop to execute tools directly without a `ToolInvoker` / future `ToolExecutionGateway` port.
- Do not expose Spring/Jackson/Jakarta/PF4J/MCP/provider types from Domain.

## References Used

- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md`
- `.planning/ROADMAP.md` Phase 1
- `.planning/REQUIREMENTS.md`
- `.planning/research/ARCHITECTURE.md`
- `.planning/research/SUMMARY.md`
- `.planning/research/PITFALLS.md`
- `README.md`
