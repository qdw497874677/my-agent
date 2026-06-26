# Phase 3: Model Provider Registry and OpenAI-Compatible Adapter - Context

**Gathered:** 2026-06-14
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 3 adds real model streaming and tool-call intent normalization without leaking provider-specific types into core. It must deliver a provider/model registry, OpenAI-compatible streaming chat adapter, normalized text deltas/tool-call intents/finish reasons/usage/latency/provider errors, CredentialRef/SecretRef boundaries, conservative resilience hooks, and provider contract tests.

This phase does **not** implement additional native provider families beyond OpenAI-compatible, Admin UI provider configuration screens, the Phase 4 governed ToolExecutionGateway, MCP, dynamic plugins, or a full credential vault/KMS/DB-backed credential store.

</domain>

<decisions>
## Implementation Decisions

### Provider Registry Shape
- **D-01:** Use a **layered provider/model registry**, not a flat modelRef-to-client map. Model provider registration should distinguish `ProviderDescriptor`, `ModelDescriptor`, capabilities, provider config, and credential references.
- **D-02:** `AgentDefinition.modelRef` should use the explicit `provider:model` format, for example `openai-compatible:gpt-4.1-mini`. Registry resolution maps this string to provider config, model descriptor, capabilities, and credential references.
- **D-03:** Keep provider/model/capability descriptor contracts provider-neutral and framework-free. Domain may define pure value objects and resolution result contracts; App may define registry ports/use cases; Infrastructure implements provider config loading and adapter wiring.

### Streaming Model Contract
- **D-04:** Extend the Pi-owned model contract for streaming rather than having the adapter publish events as a side channel. Introduce a provider-neutral streaming callback/chunk contract around `ModelClient` or a successor interface so the runtime/event layer remains responsible for emitting `MODEL_REQUESTED`, `MODEL_DELTA`, terminal, and error events.
- **D-05:** The OpenAI-compatible adapter should aggregate provider stream fragments into platform-neutral chunks and complete intents. Runtime core should not handle provider-specific stream fragment details.
- **D-06:** Tool-call streaming fragments must be aggregated by the adapter into complete provider-neutral `ToolCallIntent` records before the General Agent loop sees them. Do not push OpenAI incremental `tool_calls[].function.arguments` fragment semantics into Domain/App.
- **D-07:** Normalized model chunks/records must be able to carry text delta, complete tool-call intent, finish reason, optional usage/tokens, latency metadata, model/provider identity, and provider-neutral error summaries. Usage must be optional because OpenAI-compatible servers vary in whether and when they return token usage.

### OpenAI-Compatible Adapter Dependency
- **D-08:** Build the default OpenAI-compatible adapter using **Spring AI 1.1.x `OpenAiChatModel`** behind Pi-owned model contracts. Do not use Spring AI `ChatClient` advisor/tool-calling loop as the Agent loop; Pi owns the loop and only uses Spring AI as an Infrastructure adapter.
- **D-09:** Do not use `OpenAiSdkChatModel` as the Phase 3 default because its API surface is actively shifting toward Spring AI 2.0. Prefer the classic `OpenAiChatModel` path for OpenAI-compatible endpoints and `extra-body` support.
- **D-10:** Keep direct HTTP/SSE parsing as a documented Infrastructure-layer escape hatch for future provider quirks, custom gateway headers/auth flows, or Spring AI incompatibilities. Do not implement parallel Spring AI and direct-HTTP adapters by default in Phase 3.
- **D-11:** Add a dedicated provider infrastructure module, e.g. `pi-agent-infrastructure-model-openai`, to isolate Spring AI, Reactor/WebFlux, OpenAI-compatible, WireMock/MockWebServer, and Resilience4j dependencies from Domain/App and from the core Infrastructure module where practical.

### Credential and Secret Boundary
- **D-12:** Use `CredentialRef`/`SecretRef` plus a `SecretResolver` boundary. Domain/App/registry records store only references and redacted display metadata; raw secrets are resolved only inside Infrastructure at provider adapter construction/call boundaries.
- **D-13:** Phase 3 default `SecretResolver` supports environment-variable and Spring configuration-property references such as `env:OPENAI_API_KEY` and `config:pi.providers.<id>.api-key`. Vault/KMS/database-backed credential stores are extension points, not Phase 3 scope.
- **D-14:** Raw secrets must never appear in logs, prompts, persisted RunEvents, provider descriptors, public API DTOs, exception messages, or future Admin-visible payloads. Tests should assert this boundary for provider config, errors, and event payloads.

### Resilience and Cancellation
- **D-15:** Use conservative resilience defaults: timeout and cancellation support are mandatory; retry only for connection failures, 429, and 5xx responses **before any streamed output has started**; avoid retrying after text/tool-call deltas have been emitted.
- **D-16:** Rate limiter and circuit breaker hooks should be present and configurable per provider/model, but defaults should avoid surprising behavior. Resilience4j is the preferred implementation in Infrastructure.
- **D-17:** Cancellation must propagate from Pi `CancellationToken`/run cancellation into the streaming subscription/provider call. Streaming cancellation should stop upstream HTTP/Reactor work and surface a provider-neutral cancellation/timeout error when applicable.

### Provider Contract Tests
- **D-18:** Default CI/provider contract tests must use a fake OpenAI-compatible SSE endpoint, preferably WireMock or MockWebServer, and must not require real model API keys.
- **D-19:** Fake SSE tests must cover text deltas, fragmented tool-call argument aggregation, finish reasons, optional/missing usage, latency metadata, 429/5xx mapping, mid-stream errors, timeout, cancellation, and secret redaction.
- **D-20:** Add optional real-provider smoke tests behind an explicit profile/env gate. These tests may validate a configured OpenAI-compatible endpoint, but absence of API keys must never fail normal CI or local verification.

### the agent's Discretion
- Exact Java interface/record names, package names, and whether the streaming contract is `ModelClient.stream(...)`, `StreamingModelClient`, or a callback parameter are left to planner/researcher discretion as long as Spring AI/provider types stay out of Domain/App.
- Exact capability enum fields are flexible, but must cover at least streaming text, tool calls, usage reporting availability, context window/token limits where known, and provider-specific extra parameter support.
- Exact provider config property names are flexible, but must clearly model base URL, chat completions path, model ID, credential reference, extra-body/default parameters, timeouts, and resilience settings.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 3 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 3 — Phase goal, MODEL-01..MODEL-05 mapping, success criteria, and research-needed note.
- `.planning/REQUIREMENTS.md` §Model Providers — MODEL-01 through MODEL-05 details for provider registry, OpenAI-compatible streaming adapter, normalization, CredentialRef boundary, and resilience/contract tests.
- `.planning/PROJECT.md` — Product constraints: Java-first, COLA boundaries, cloud safety, extensibility, verification, OpenAI-compatible first, and provider SDK isolation.
- `.planning/STATE.md` — Current Phase 3 state and accumulated Phase 1/2 boundary decisions.

### Prior Phase Contracts
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md` — Locked decisions for provider-neutral runtime/model/tool/event/error contracts, strict Domain boundaries, fake General Agent loop, and deferred real provider SDK work.
- `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-CONTEXT.md` — Locked decisions for provider-neutral API/Event DTOs, persist-then-emit SSE, app/infrastructure ports, JSONB payload schema discipline, and single composition root.
- `docs/phase-01-domain-contracts.md` — Concrete downstream contract index for modules, `ModelClient`, `AgentRuntime`, `RunContext`, `RunEvent`, and fake testkit contracts.

### Architecture and Stack Guidance
- `.planning/research/STACK.md` §Agent Runtime and AI Integration — Spring AI 1.1.x recommendation, OpenAI-compatible first provider, and warning to keep provider types behind project-owned abstractions.
- `.planning/research/STACK.md` §Cloud Server API Layer — Spring MVC first with reactive boundaries only where required for streaming/MCP.
- `.planning/research/STACK.md` §Resilience, Safety, and Governance — Resilience4j guidance for model providers and remote calls.
- `.planning/research/STACK.md` §Testing and Quality — WireMock/MockWebServer and Testcontainers/fake-provider expectations.

### External Documentation to Research During Planning
- Spring AI 1.1.x OpenAI Chat documentation — `OpenAiChatModel`, streaming, OpenAI-compatible base URL/completions path, and `extra-body` behavior.
- Spring AI 1.1.x ChatClient/streaming documentation — streaming requires Reactor/Flux; do not rely on ChatClient tool-calling advisor loop for Pi's runtime loop.
- Resilience4j Spring Boot 3 documentation — timeout, retry, rate limiter, circuit breaker, and cancellation integration patterns.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelClient.java` — Current synchronous model port: `ModelResponse next(ModelRequest, CancellationToken)`. Phase 3 should extend or supersede this with Pi-owned streaming semantics.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelRequest.java` — Carries `RunContext` and prior `ToolResult` list into model calls.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelResponse.java` — Current sealed terminal response: `FinalText` or `ToolCallIntent`; needs normalization additions or companion records for streaming, usage, finish reason, latency, and errors.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/agent/AgentDefinition.java` — Contains `String modelRef`; Phase 3 registry resolves the chosen `provider:model` format.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventType.java` and `RunEventPayload.java` — Already reserve `MODEL_REQUESTED`, `MODEL_DELTA`, and a minimal `ModelDeltaPayload(modelRef, textDelta)`.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/error/PiError.java` — Already includes `Category.MODEL` and retryable/recoverable/userActionRequired flags for normalized provider errors.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` — Existing synchronous fake runtime loop that Phase 3 should keep compatible with or evolve through a successor streaming loop.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeModelClient.java` — Pattern for no-key deterministic model contract tests.

### Established Patterns
- `pi-agent-domain` production dependencies are intentionally empty; strict ArchUnit tests forbid Spring, Spring AI, OpenAI SDK, LangChain4j, MCP SDK, Jakarta, Jackson annotations, DB, app, infrastructure, and adapter dependencies.
- `pi-agent-app` also stays framework-light and only depends on Java/App/Domain/Client packages; provider adapter implementation must not live there.
- `pi-agent-infrastructure` implements App/Domain ports with Spring/JDBC/Flyway patterns. Provider adapter implementation should follow the same port/implementation split while isolating heavier provider dependencies in a dedicated module.
- Phase 2 established persist-then-emit event handling through Infrastructure `PersistingEventSink`; streaming model events should flow through runtime/event contracts rather than adapter side effects.
- `pi-agent-client` owns external DTOs; Domain records are not public JSON contracts.

### Integration Points
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` — Current composition root for runtime beans. Phase 3 should wire provider registry, secret resolver, OpenAI-compatible provider adapter, and model client/registry integration here or through imported configuration.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` — Currently builds a default `AgentDefinition` with hardcoded `modelRef`; Phase 3 should replace or route this through provider registry resolution/config.
- `pi-agent-infrastructure/pom.xml` and root `pom.xml` — Need dependencyManagement/module additions for Spring AI, provider module, Reactor/WebFlux boundary, Resilience4j, and WireMock/MockWebServer.
- `pi-agent-adapter-web/src/test/java/.../TestCloudRuntimeConfiguration.java` and `CloudServerHeadlessE2ETest.java` — Existing no-key fake runtime E2E pattern; Phase 3 should preserve no-key CI and add fake provider contract tests similarly.

</code_context>

<specifics>
## Specific Ideas

- The registry should be future-proof for Phase 6 SPI/Spring providers and Phase 5 Admin Governance provider views, but Phase 3 only needs the backend registry/contracts and OpenAI-compatible default adapter.
- `provider:model` was chosen over URI-style model refs or alias-only refs because it is explicit, short, and transparent for early backend phases.
- Use Spring AI for maintenance leverage around OpenAI-compatible streaming, chunk parsing, tool-call aggregation, `extra-body`, and Micrometer compatibility, but keep Pi's model abstraction authoritative.
- Optional real-provider smoke tests are useful for manual confidence but must be disabled unless a profile/API key is provided.

</specifics>

<deferred>
## Deferred Ideas

- Native Anthropic/Gemini/provider-specific adapters beyond OpenAI-compatible — future provider adapter phases or extensions.
- Direct HTTP OpenAI-compatible adapter — keep as documented escape hatch, not default Phase 3 implementation.
- Full Admin UI for provider/credential management — Phase 5 governance views and later hardening.
- Vault/KMS/database-backed credential store, secret rotation, and advanced credential audit workflows — future production hardening.
- Full governed ToolExecutionGateway behavior for executing tool calls — Phase 4.
- MCP tool/model bridge and remote provider/tool discovery — Phase 7.

</deferred>

---

*Phase: 03-model-provider-registry-and-openai-compatible-adapter*
*Context gathered: 2026-06-14*
