---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 04
status: executing
stopped_at: Completed 04-02-PLAN.md
last_updated: "2026-06-14T18:53:16.198Z"
progress:
  total_phases: 9
  completed_phases: 3
  total_plans: 34
  completed_plans: 28
---

# Project State: Pi Java Agent Platform

**Initialized:** 2026-06-13  
**Status:** Executing Phase 04
**Current Phase:** 04

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-06-13)

**Core value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。  
**Current focus:** Phase 04 — governed-tool-registry-workspace-and-invocation-pipeline

## Workflow Configuration

- Mode: YOLO
- Granularity: Standard
- Parallelization: Enabled
- Research before phase planning: Enabled
- Plan checking: Enabled
- Phase verification: Enabled
- Planning docs committed to git: Yes

## Current Roadmap

| Phase | Name | Status |
|-------|------|--------|
| 1 | Runtime Spine, Workspace, and Domain Contracts | Complete |
| 2 | Cloud Server, Persistence, SSE, and Baseline Security | Complete |
| 3 | Model Provider Registry and OpenAI-Compatible Adapter | Complete |
| 4 | Governed Tool Registry and Invocation Pipeline | In Progress |
| 5 | Agent Web Console and Runtime Cockpit | Pending |
| 6 | Java Extension Surface: SPI and Spring | Pending |
| 7 | MCP Client Bridge and Governed Remote Tools | Pending |
| 8 | Controlled Dynamic Plugin JARs | Pending |
| 9 | Observability, Policy, Tenancy, and Production Hardening | Pending |

## Phase 1 Summary

**Goal:** Establish a framework-independent Java Agent Runtime kernel and first-class Workspace contracts that all cloud, GUI, provider, tool, MCP, and plugin work will build on.

**Requirements:** CORE-01, CORE-02, CORE-03, CORE-04, CORE-05, CORE-06, CORE-07, CORE-08, CORE-09, WORK-01, WORK-02, WORK-04, WORK-05, OPS-04, OPS-06

**Key concerns for planning:**

- Keep runtime core independent of Spring Boot, Vaadin, PF4J, MCP, and provider SDKs.
- Use COLA boundaries: Adapter → App → Domain ← Infrastructure; Domain has zero outward dependencies.
- Keep runtime core generic: no Chat-only, Coding-only, single-provider, single-tool-protocol, or UI-driven assumptions.
- Define Workspace as a first-class domain concept instead of a thin wrapper over host filesystem.
- Define stable domain models before persistence/API/UI.
- Define event envelope carefully because REST/SSE/Admin/TUI/CLI/audit all depend on it.
- Use fake model/fake tool testkit to validate Agent loop without real providers.
- Use fake workspace and fake command executor so later E2E can validate file/command/resource actions without host shell dependencies.
- Design Phase 1 testkit so later E2E can validate Run lifecycle without real model keys.

## Research Summary

See `.planning/research/SUMMARY.md`.

Key findings:

- Build modular monolith first, distributed-capable later.
- Runtime core should be framework-independent; Spring, Spring AI, MCP, PF4J, Vaadin are adapters.
- Every tool source must normalize into one Tool Registry + Governed Tool Invocation Pipeline.
- Do not start with MCP or dynamic plugins; they depend on stable tool/policy/event contracts.
- Web GUI is an Agent Web Console first: Agent Catalog, Chat entry, Run timeline, Tool cards, Approval cards, Session history, plus Admin Governance. It is not a full visual workflow builder in v1.

## Open Questions

- Does v1 require crash-resumable durable execution, or persisted history/cancellation only?
- Which MCP transport/auth scope is mandatory for launch?
- Should dynamic plugins be restart-required for unload in v1?
- Which policy engine approach should be adopted beyond the default Java interface?
- Which E2E tool stack should be standard for Web Console: Playwright, Selenium, or Vaadin TestBench?

## Execution Progress

- [x] 01-01-PLAN.md — Java 21 Maven/COLA skeleton and architecture gates completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-01-SUMMARY.md`).
- [x] 01-02-PLAN.md — Define AgentDefinition, runtime state, error, and RunEvent contracts completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-02-SUMMARY.md`).
- [x] 01-03-PLAN.md — Define Workspace, Artifact/Attachment, and append-only Session tree contracts completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-03-SUMMARY.md`).
- [x] 01-04-PLAN.md — Implement runtime ports and reusable fake General Agent testkit loop completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-04-SUMMARY.md`).
- [x] 01-05-PLAN.md — Harden Phase 1 verification and write downstream contract index completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-05-SUMMARY.md`).
- [x] 03-08-PLAN.md — Phase 3 provider contract tests, Cloud Server fake provider E2E, optional smoke gate, and downstream docs completed (`.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/03-08-SUMMARY.md`).

## Decisions

- 2026-06-13 (Phase 01 Plan 01): Use a Maven parent with explicit dependency/plugin management and no framework BOM in Phase 1 foundation.
- 2026-06-13 (Phase 01 Plan 01): Keep `pi-agent-domain` production dependencies empty while allowing only test-scoped JUnit, AssertJ, and ArchUnit.
- 2026-06-13 (Phase 01 Plan 01): Codify COLA dependency direction immediately with ArchUnit before domain implementation starts.
- [Phase 01]: Model Workspace as a logical runtime boundary using resource and mount IDs instead of host filesystem paths.
- [Phase 01]: Keep command execution in Domain as a request/result port only; no shell or process implementation belongs in Phase 1 Domain.
- [Phase 01]: Represent session history as an append-only tree with current leaf reconstruction and separated non-message context lists.
- [Phase 01]: Use nested JDK records in PlatformIds for runtime context IDs.
- [Phase 01]: Represent generic runtime input and event payload variants with Java sealed interfaces.
- [Phase 01]: Keep Domain contracts framework-free and serialization-neutral for adapter/client mapping later.
- [Phase 01]: Keep runtime/model/tool/event/policy ports pure Domain contracts with no async framework, provider SDK, Spring, persistence, or host process dependency.
- [Phase 01]: Implement GeneralAgentLoop in pi-testkit as a synchronous fake runtime for contract verification without real model keys or tools.
- [Phase 01]: Use exactly one terminal RunEvent as the last observable fake loop outcome.
- [Phase 01]: Use Java 21 explicitly for Maven verification in this environment because the system Maven launcher defaults to Java 17.
- [Phase 01]: Keep Phase 1 contract documentation as a downstream boundary document that names deferred provider, persistence, UI, MCP, plugin, shell, filesystem, and durable execution scopes.
- [Phase 02]: Plan 01 imported Spring Boot and Testcontainers through root dependencyManagement while keeping module dependencies explicit.
- [Phase 02]: Plan 01 kept Spring/JDBC/Flyway/Security dependencies only in outer Client/Infrastructure/Adapter modules; Domain and App production dependencies remain framework-isolated.
- [Phase 02]: Plan 03 keeps event history REST and SSE on one shared RunEventDto envelope in pi-agent-client.
- [Phase 02]: Plan 03 keeps client event DTOs provider-neutral and free of Domain imports so adapter-web owns Domain-to-public mapping later.
- [Phase 02]: Plan 03 exposes event replay through run-scoped sequence cursors afterSequence and nextAfterSequence.
- [Phase 02]: Plan 02 kept pi-agent-client DTOs as plain Java records without Domain imports or Spring/Jakarta annotations.
- [Phase 02]: Plan 02 represented create-run input generically with inputType plus Map input instead of chat-transcript-only API shape.
- [Phase 02]: Plan 04 uses plain App-layer records for security and correlation context without Spring or servlet dependencies.
- [Phase 02]: Plan 04 keeps run use-case methods session-centric by requiring sessionId alongside runId for commands and queries.
- [Phase 02]: Plan 04 configures Surefire to allow filtered reactor builds where upstream modules have no matching tests.
- [Phase 02]: Plan 05 keeps persistence and execution contracts in pi-agent-app as plain Java ports depending only on App, Client, and Domain types.
- [Phase 02]: Plan 05 models queued cancellation as Optional<QueuedRun> cancelQueuedAndReturn(...) so App services can publish exactly one durable terminal event with original queued context.
- [Phase 02]: Plan 05 makes terminal publishing methods publish*IfAbsent to reserve durable hasTerminalEvent idempotency guards.
- [Phase 02]: Plan 06 kept concrete App use-case services as plain Java constructor-injected classes with no Spring/JDBC/Servlet/SSE imports.
- [Phase 02]: Plan 06 routes App-created terminal run events through DefaultRunTerminalEventPublisher guarded by RunEventStore.hasTerminalEvent.
- [Phase 02]: Plan 06 uses queued-run context for queued cancellation terminal events to preserve tenant/user/session/run/workspace/trace/correlation IDs.
- [Phase 02]: Plan 07 stores RunEvent payloads as JSONB with explicit payload_schema/payload_version instead of Java class-name serialization.
- [Phase 02]: Plan 07 routes live event delivery through Infrastructure RunEventFanout only after TransactionTemplate persistence succeeds.
- [Phase 02]: Plan 09 kept the web adapter runnable while excluding DataSource/Flyway auto-configuration until Plan 02-12 owns the single runtime composition root.
- [Phase 02]: Plan 09 uses a dev/test-only safe-header authentication filter while retaining oauth2ResourceServer JWT configuration in every security chain for production readiness.
- [Phase 02]: Plan 08 keeps worker and scheduler classes as plain Infrastructure classes; Spring bean registration remains deferred to the later composition-root plan.
- [Phase 02]: Plan 08 extends run_queue with queued-run context and payload columns so cancellation and worker execution can publish terminal events from original run context.
- [Phase 02]: Plan 08 enforces AgentRuntime.start(context) with a bounded worker Future so timeout handling calls AgentRuntime.cancel and marks TIMED_OUT consistently.
- [Phase 02]: Plan 10 keeps REST controllers thin: they build RequestContext from PiPrincipal plus CorrelationFilter attributes and immediately delegate to App use cases.
- [Phase 02]: Plan 10 exposes run activation as RunActivationTrigger instead of wiring concrete worker/dispatcher behavior in the web adapter.
- [Phase 02]: Plan 10 maps Domain RunEvent to client RunEventDto explicitly using RunEventType.wireName and payload schema/version fields.
- [Phase 02]: Plan 11 keeps live SSE fanout in Adapter Web as an in-memory subscriber registry while durable replay remains owned by RunQueryService/event persistence.
- [Phase 02]: Plan 11 uses bare per-run RunEvent.sequence as the SSE id and parses Last-Event-ID only when it is a positive long.
- [Phase 02]: Plan 11 closes SSE subscriptions from completion, timeout, error, send failure, and terminal event paths.
- [Phase 02]: Plan 12 owns active runtime bean registration through CloudRuntimeBeanConfiguration instead of component scanning infrastructure fanout beans.
- [Phase 02]: Plan 12 keeps RunController activation abstracted behind RunActivationTrigger, with production wiring delegating to RunWorkerScheduler.triggerAsync().
- [Phase 02]: Plan 12 provides scheduled polling through a small adapter bean so RunWorkerScheduler remains a plain infrastructure class.
- [Phase 02]: Plan 13 uses TestCloudRuntimeConfiguration to provide a test-only no-key AgentRuntime built from pi-testkit GeneralAgentLoop, FakeModelClient, FakeToolInvoker, and FakePolicy.
- [Phase 02]: Plan 13 treats Docker absence as an environment gate for Testcontainers validation while keeping non-container regressions green locally.
- [Phase 02]: Plan 13 honors AgentRuntime RunHandle terminal status in DefaultRunDispatcher so max-step, policy, and runtime failures are not incorrectly marked completed.
- [Phase 03]: Plan 02 uses callback-style StreamingModelClient with Pi-owned ModelStreamSink instead of Flow.Publisher/Reactor to keep Domain framework-free.
- [Phase 03]: Plan 02 represents provider tool-call streaming only as complete ToolCall intents in Domain; adapter owns fragment aggregation.
- [Phase 03]: Plan 02 preserves legacy ModelResponse and ModelDeltaPayload constructors so existing fake runtime/testkit code remains source-compatible.
- [Phase 03]: Plan 03 keeps model provider registry and secret resolution as App ports over Domain records, with no Spring, provider SDK, or persistence types in App.
- [Phase 03]: Plan 03 exposes ResolvedSecret.rawValue only from the SecretResolver boundary while default string/error paths carry redacted metadata only.
- [Phase 03]: Plan 03 uses Optional for unknown provider/model registry resolution so callers never receive null and Adapter can map API errors later.
- [Phase 03]: Plan 04 keeps FakeStreamingModelClient deterministic and callback-based with scripted actions instead of sleeps, networking, Reactor, or provider SDK dependencies.
- [Phase 03]: Plan 04 exposes streaming support through a GeneralAgentLoop constructor overload so existing synchronous ModelClient tests remain source-compatible.
- [Phase 03]: Plan 04 publishes finish/usage metadata as an empty text MODEL_DELTA payload because existing Domain model metadata is carried on ModelDeltaPayload.
- [Phase 03]: Plan 05 isolates Spring AI OpenAI-compatible and Resilience4j dependencies in pi-agent-infrastructure-model-openai, leaving Domain/App free of provider SDK dependencies.
- [Phase 03]: Plan 05 models provider configuration as plain Java records first so later Spring @ConfigurationProperties wiring can bind without changing registry behavior.
- [Phase 03]: Plan 05 resolves env:/config: secrets from injected maps with default string and exception paths exposing only scheme-level redaction.
- [Phase 03]: Plan 06 keeps OpenAI stream parsing behind an OpenAiStreamSource seam so production uses Spring AI while tests remain deterministic and key-free.
- [Phase 03]: Plan 06 applies retry/rate-limiter/circuit-breaker decorators only before stream iteration begins; after chunks are emitted, failures become provider-neutral error chunks rather than retries.
- [Phase 03]: Plan 06 resolves raw API keys only while creating adapter model configuration and sanitizes all provider messages before exposing Domain error records.
- [Phase 03]: Plan 07 keeps provider registry, secret resolver, streaming client, and optional OpenAI-compatible runtime wiring in Adapter configuration only.
- [Phase 03]: Plan 07 dispatches queued runs with explicit provider:model refs using pi.runtime.default-model-ref and an openai-compatible default-model fallback.
- [Phase 03]: Plan 07 uses spring-ai-openai instead of the Spring Boot OpenAI starter to avoid no-key auto-configuration failures.
- [Phase 03]: Plan 08 uses fake OpenAI-compatible stream fixtures and injected OpenAiSpringAiModelFactory for no-key provider E2E.
- [Phase 03]: Plan 08 publishes provider TextDelta and Finished chunks as normalized model.delta events through the existing EventSink path.
- [Phase 03]: Plan 08 uses in-memory Cloud Server E2E persistence/queue fallback when Docker/Testcontainers are unavailable.
- [Phase 04]: Plan 01 uses plain JDK Map/Set/Optional/Duration records for tool schema, descriptor, preview, and execution results so Domain remains framework-free and JSON-schema-library-neutral.
- [Phase 04]: Plan 01 preserves legacy ToolCall/ToolResult and adds ToolExecutionRequest/ToolExecutionResult as gateway-facing contracts for later ToolExecutionGateway work.
- [Phase 04]: Plan 01 represents tool lifecycle events with stable tool.* wire names plus ToolLifecyclePayload carrying descriptor ref, provenance/version, redacted summaries, policy decision, preview, execution status, and error category.
- [Phase 04]: Plan 02 keeps ToolExecutorBinding in App as a low-level executor seam behind ToolRegistry resolution so future ToolExecutionGateway can be the only governance caller.
- [Phase 04]: Plan 02 returns public tool catalog data as pi-agent-client records instead of Domain records so REST/Admin/Web Console surfaces do not leak Domain or executor implementation types.
- [Phase 04]: Plan 02 exposes source provenance as string metadata in client DTOs while preserving descriptor-first normalization and avoiding source-specific registry methods.

## Performance Metrics

| Phase | Plan | Duration | Tasks | Files |
|-------|------|----------|-------|-------|
| 01-runtime-spine-workspace-and-domain-contracts | 01 | 11m 46s | 2 | 16 |
| Phase 01-runtime-spine-workspace-and-domain-contracts P03 | 6m 34s | 3 tasks | 19 files |
| Phase 01-runtime-spine-workspace-and-domain-contracts P02 | 7m 03s | 3 tasks | 22 files |
| Phase 01-runtime-spine-workspace-and-domain-contracts P04 | 7m 20s | 3 tasks | 22 files |
| Phase 01-runtime-spine-workspace-and-domain-contracts P05 | 4m 06s | 3 tasks | 4 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P01 | 3m 06s | 1 tasks | 4 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P03 | 2m 24s | 1 tasks | 4 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P02 | 2m 42s | 1 tasks | 13 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P04 | 3m 30s | 1 tasks | 9 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P05 | 3m 01s | 1 tasks | 11 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P06 | 7m 14s | 2 tasks | 9 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P07 | 9m 55s | 2 tasks | 10 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P09 | 4m 30s | 1 tasks | 8 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P08 | 7m 22s | 2 tasks | 9 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P10 | 10m 52s | 2 tasks | 6 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P11 | 5m 38s | 2 tasks | 5 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P12 | 7m 01s | 1 tasks | 4 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P13 | 9m 06s | 2 tasks | 11 files |
| Phase 03-model-provider-registry-and-openai-compatible-adapter P02 | 5m 24s | 2 tasks | 10 files |
| Phase 03-model-provider-registry-and-openai-compatible-adapter P03 | 5m 13s | 2 tasks | 7 files |
| Phase 03-model-provider-registry-and-openai-compatible-adapter P04 | 7m 05s | 2 tasks | 4 files |
| Phase 03-model-provider-registry-and-openai-compatible-adapter P05 | 5m 07s | 2 tasks | 8 files |
| Phase 03-model-provider-registry-and-openai-compatible-adapter P06 | 9m 35s | 2 tasks | 10 files |
| Phase 03-model-provider-registry-and-openai-compatible-adapter P07 | 7m 46s | 2 tasks | 7 files |
| Phase 03-model-provider-registry-and-openai-compatible-adapter P08 | 16m 45s | 3 tasks | 9 files |
| Phase 04-governed-tool-registry-workspace-and-invocation-pipeline P01 | 5m 00s | 2 tasks | 14 files |
| Phase 04-governed-tool-registry-workspace-and-invocation-pipeline P02 | 4m 52s | 2 tasks | 8 files |

## Last Session

- **Updated:** 2026-06-14T18:53:16Z
- **Stopped At:** Completed 04-02-PLAN.md

## Next Action

Run:

```text
/gsd-verify-work 1
```

Context captured:

```text
.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md
```

Optional shortcuts:

```text
/gsd-plan-phase 1 --skip-research
```

## Session Notes

- 2026-06-13: Phase 1 context gathered. Resume from `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md`.

---
*State initialized: 2026-06-13 after roadmap creation*
