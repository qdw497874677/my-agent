---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 06
status: executing
stopped_at: Completed 06-07-PLAN.md
last_updated: "2026-06-16T00:13:58.626Z"
progress:
  total_phases: 9
  completed_phases: 5
  total_plans: 51
  completed_plans: 50
---

# Project State: Pi Java Agent Platform

**Initialized:** 2026-06-13  
**Status:** Executing Phase 06
**Current Phase:** 06

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-06-13)

**Core value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。  
**Current focus:** Phase 06 — java-extension-surface-spi-and-spring

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
- [Phase 04]: Plan 03 keeps validation, policy evaluation, preview generation, payload limiting, and redaction as App-layer ports so concrete implementations remain replaceable.
- [Phase 04]: Plan 03 preserves REQUIRE_APPROVAL and REQUIRE_SANDBOX as non-executing gateway outcomes instead of converting them to generic deny.
- [Phase 04]: Plan 03 emits and audits redacted summary-level lifecycle data from the gateway while leaving raw tool output out of default events and audits.
- [Phase 04]: Keep networknt JSON Schema validation in Infrastructure only and expose only safe validation summaries through the App validator port.
- [Phase 04]: Use conservative default policy semantics: safe read-only allowed, side-effectful and unscoped tools require preview/approval, destructive or critical tools block by default.
- [Phase 04]: Generate provision previews as static impact estimates only; preview generation must not execute tool bindings, workspace writes, or processes.
- [Phase 04]: Return payload summaries and truncation metadata for oversized payloads instead of propagating unbounded raw arguments/results.
- [Phase 04]: Plan 05 keeps legacy FakeToolInvoker source-compatible but wraps it behind FakeToolExecutorBinding for gateway-aware paths.
- [Phase 04]: Plan 05 uses DefaultToolExecutionGateway in the testkit fake gateway so fake E2E observes governed lifecycle events.
- [Phase 04]: Plan 05 normalizes gateway-emitted event sequence numbers in GeneralAgentLoop to keep a single monotonic run event stream.
- [Phase 04]: Plan 06 treats local-temp workspace and ProcessBuilder command execution as deterministic dev/test infrastructure only, documented as not a production sandbox.
- [Phase 04]: Plan 06 exposes built-in examples as ordinary ToolDescriptor plus ToolExecutorBinding registrations rather than special registry APIs.
- [Phase 04]: Plan 06 marks workspace write and command examples as side-effectful with previewRequired and approvalRecommended metadata.
- [Phase 04]: Plan 07 exposes the tool catalog through a thin read-only /api/tools controller returning client DTOs only.
- [Phase 04]: Plan 07 maps ToolLifecyclePayload explicitly as tool.lifecycle schema v1 with redacted public payload maps.
- [Phase 04]: Plan 07 owns tool governance composition in Adapter configuration and requires the runtime composition to include ToolExecutionGateway.
- [Phase 05]: Plan 01 keeps Vaadin dependency management in the parent POM while declaring the Vaadin starter only in pi-agent-adapter-web.
- [Phase 05]: Plan 01 permits Vaadin Console/Admin routes and static resources without weakening authenticated /api/** security.
- [Phase 05]: Plan 01 establishes ConsoleHttpClient/EventStreamClient as the Vaadin public API/SSE boundary anchored to pi-agent-client DTOs.
- [Phase 05]: Plan 02 keeps Agent Catalog DTOs in pi-agent-client as plain Java records with string-based model/tool/risk metadata so UI and API clients do not import Domain or Spring types.
- [Phase 05]: Plan 02 exposes Agent Catalog as a read-only App query service plus thin Adapter controller; Agent Studio create/edit/publish remains out of scope.
- [Phase 05]: Plan 02 uses one default Cloud General Agent catalog entry sourced through App service and public API rather than Vaadin hardcoding.
- [Phase 05]: Plan 03 derives approval IDs from approval-required lifecycle preview IDs with toolCallId fallback.
- [Phase 05]: Plan 03 records approval decisions as audit entries plus same-run events while preserving a future-compatible resume seam.
- [Phase 05]: Plan 03 allows USER and ADMIN approval actors in dev/test through an explicit App-layer role seam.
- [Phase 05]: Plan 04 keeps Admin Governance contracts in pi-agent-client as plain redacted records with no Domain/Spring/Jakarta imports.
- [Phase 05]: Plan 04 exposes extension, MCP, and plugin governance as read-only FUTURE_ENABLED/UNCONFIGURED placeholders rather than configuration surfaces in Phase 5.
- [Phase 05]: Plan 04 keeps /api/admin/governance inspect-only with GET mappings only; mutations remain out of scope for Phase 5.
- [Phase 05]: Plan 05 makes ConsoleView the concrete /console route while MainConsoleLayout remains non-routed to avoid duplicate Vaadin route ownership.
- [Phase 05]: Plan 05 represents Console UI actions as public REST/SSE action plans so Vaadin stays behind ConsoleHttpClient/EventStreamClient and pi-agent-client DTO boundaries.
- [Phase 05]: Plan 05 renders model deltas, run status, policy/tool lifecycle, and terminal events into one integrated chat/event stream narrative.
- [Phase 05]: Plan 06 renders Agent Catalog cards from public AgentCatalogResponse data while keeping Catalog secondary to the Chat-first Console.
- [Phase 05]: Plan 06 renders governed tool lifecycle events as expandable redacted ToolCallCard components from public RunEventDto payload maps.
- [Phase 05]: Plan 06 ignores Vaadin dev-mode generated frontend output so test-generated runtime artifacts do not enter source commits.
- [Phase 05]: Plan 07 approval UI components build backend decision plans instead of mutating local-only approval state.
- [Phase 05]: Plan 07 approval-required tool lifecycle events are promoted to dedicated approval cards before generic tool cards.
- [Phase 05]: Plan 07 admin approval decisions reuse ApprovalCard behavior with an explicit ADMIN actor role under a separated Admin Governance route.
- [Phase 05]: Plan 08 keeps Admin Governance Vaadin views inspect-only with no provider/tool/policy/plugin/MCP/extension mutation controls.
- [Phase 05]: Plan 08 uses ConsoleHttpClient as the Vaadin public API anchor for governance overview, policy decision, and audit endpoints.
- [Phase 05]: Plan 08 renders extension/MCP/plugin as read-only placeholder status metadata until Phases 6/7/8.
- [Phase 05]: Plan 09 uses Playwright as a test-only browser E2E harness while production Web Console/Admin remains Java-first.
- [Phase 05]: Plan 09 runs browser E2E through Spring Boot test/e2e profiles with fake runtime fixtures, in-memory state, no Docker, and no model keys.
- [Phase 05]: Plan 09 documents Phase 5 public REST/SSE/read-model contracts as the downstream boundary for extension, MCP, plugin, and governance work.
- [Phase 06]: Plan 01 keeps pi-agent-extension-api framework-free with compile dependencies only on pi-agent-domain and pi-agent-app.
- [Phase 06]: Plan 01 models tool extensions as descriptor-first capabilities carrying ToolDescriptor plus ToolExecutorBinding so future sources register through governed tool contracts.
- [Phase 06]: Plan 01 represents memory provider support as metadata-only capability; runtime/RAG wiring remains deferred.
- [Phase 06]: Expose extension governance as read-only public DTOs with Map<String, String> metadata only; Admin mutation controls remain disabled/deferred.
- [Phase 06]: Keep extension governance catalog in App using App-owned string statuses so App does not depend on extension SDK or discovery implementations.
- [Phase 06]: Wire Adapter Web with EmptyExtensionGovernanceCatalog as the safe fallback while concrete SPI/Spring discovery modules provide real catalog data later.
- [Phase 06]: Plan 03 keeps SPI discovery in a separate infrastructure module with no Spring/PF4J/MCP dependencies.
- [Phase 06]: Plan 03 represents failed ServiceLoader providers as failed source statuses with sanitized errors for governance visibility.
- [Phase 06]: Plan 03 exposes extension tools only through ToolRegistry.resolve with ToolExecutorBinding to preserve the governed execution path.
- [Phase 06]: Plan 04 keeps Spring integration in a dedicated starter module while preserving Domain/App framework isolation.
- [Phase 06]: Plan 04 merges Spring ExtensionSource beans into existing ServiceLoader discovery output so all sources pass through one deterministic contribution registry.
- [Phase 06]: Plan 04 preserves Spring bean provenance through sourceKind metadata instead of creating a parallel Spring-only registry path.
- [Phase 06]: Plan 05 limits Spring annotations to lightweight tools and event listener metadata; complex providers, policies, workspace, and memory extensions still require explicit ExtensionSource beans.
- [Phase 06]: Plan 05 discovers annotations from already-registered Spring beans through explicit starter factories rather than component-scan magic.
- [Phase 06]: Plan 05 represents annotated tools as ordinary ToolExtensionCapability entries with ToolDescriptor plus ToolExecutorBinding so execution remains behind ToolRegistry and ToolExecutionGateway consumers.
- [Phase 06]: Make Adapter Web consume the same pi-agent-spring-boot-starter extension path external Spring applications use.
- [Phase 06]: Keep built-in tools/providers ahead of extension contributions in deterministic composite registries without silently overriding existing built-ins.
- [Phase 06]: Expose extension status only through authenticated GET DTOs and read-only Vaadin labels; enable/disable remains configuration-driven.
- [Phase 06]: Plan 07 uses duplicated lightweight conformance fixtures in module tests while keeping canonical extension-api testFixtures source to avoid invasive Maven test-fixtures wiring.
- [Phase 06]: Plan 07 treats extension provider credential references as CredentialRef values in ExtensionModelProviderRegistry so provider descriptors stay registry-bound and redacted.

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
| Phase 04-governed-tool-registry-workspace-and-invocation-pipeline P03 | 8m 42s | 2 tasks | 8 files |
| Phase 04-governed-tool-registry-workspace-and-invocation-pipeline P04 | 15m 18s | 2 tasks | 9 files |
| Phase 04-governed-tool-registry-workspace-and-invocation-pipeline P05 | 7m 16s | 2 tasks | 7 files |
| Phase 04-governed-tool-registry-workspace-and-invocation-pipeline P06 | 7m 10s | 2 tasks | 8 files |
| Phase 04-governed-tool-registry-workspace-and-invocation-pipeline P07 | 9m 41s | 2 tasks | 8 files |
| Phase 05-agent-web-console-and-runtime-cockpit P01 | 12m 20s | 2 tasks | 8 files |
| Phase 05-agent-web-console-and-runtime-cockpit P02 | 8m | 2 tasks | 9 files |
| Phase 05-agent-web-console-and-runtime-cockpit P03 | 12m 08s | 2 tasks | 9 files |
| Phase 05-agent-web-console-and-runtime-cockpit P04 | 6m 41s | 2 tasks | 9 files |
| Phase 05-agent-web-console-and-runtime-cockpit P05 | 6m 34s | 2 tasks | 7 files |
| Phase 05-agent-web-console-and-runtime-cockpit P06 | 7m 08s | 2 tasks | 8 files |
| Phase 05-agent-web-console-and-runtime-cockpit P07 | 7m 52s | 2 tasks | 8 files |
| Phase 05-agent-web-console-and-runtime-cockpit P08 | 6m 41s | 2 tasks | 6 files |
| Phase 05-agent-web-console-and-runtime-cockpit P09 | 2m 43s | 3 tasks | 12 files |
| Phase 06-java-extension-surface-spi-and-spring P01 | 14m | 3 tasks | 18 files |
| Phase 06-java-extension-surface-spi-and-spring P02 | 6m 33s | 3 tasks | 12 files |
| Phase 06-java-extension-surface-spi-and-spring P03 | 7m 22s | 3 tasks | 12 files |
| Phase 06-java-extension-surface-spi-and-spring P04 | 5m 46s | 3 tasks | 11 files |
| Phase 06-java-extension-surface-spi-and-spring P05 | 6m | 3 tasks | 7 files |
| Phase 06-java-extension-surface-spi-and-spring P06 | 6m 55s | 3 tasks | 8 files |
| Phase 06-java-extension-surface-spi-and-spring P07 | 11m 54s | 3 tasks | 6 files |

## Last Session

- **Updated:** 2026-06-14T19:04:29Z
- **Stopped At:** Completed 06-07-PLAN.md

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
