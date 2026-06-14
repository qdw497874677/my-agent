---
phase: 03-model-provider-registry-and-openai-compatible-adapter
verified: 2026-06-14T10:52:29Z
status: passed
score: 5/5 must-haves verified
---

# Phase 3: Model Provider Registry and OpenAI-Compatible Adapter Verification Report

**Phase Goal:** Add real model streaming and tool-call intent normalization without leaking provider-specific types into core.  
**Verified:** 2026-06-14T10:52:29Z  
**Status:** passed  
**Re-verification:** No — initial verification

## Goal Achievement

Phase 3's goal is achieved. The codebase contains provider/model registry contracts in Domain/App, an isolated OpenAI-compatible infrastructure module, provider-neutral streaming contracts, fake streaming runtime loop support, adapter-web composition wiring, no-key contract/E2E tests, gated optional real-provider smoke coverage, and downstream documentation. Focused regression and web adapter Phase 3 gates were run successfully during verification.

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Provider registry resolves model IDs, provider IDs, capability descriptors, and credential references. | ✓ VERIFIED | `ProviderDescriptor`, `ModelDescriptor`, `ModelCapabilities`, `ProviderModelRef`, `ModelProviderRegistry`, `InMemoryModelProviderRegistry`, and `ModelProviderQueryService` exist and are wired. `ProviderModelRef.parse` enforces explicit `provider:model`; registry `resolve` returns `ModelProviderResolution`. |
| 2 | OpenAI-compatible streaming chat adapter can run through the General Agent loop / Cloud runtime path. | ✓ VERIFIED | `OpenAiCompatibleStreamingModelClient implements StreamingModelClient`; `GeneralAgentLoop` has a streaming constructor and consumes `ModelStreamChunk`; `ModelProviderBeanConfiguration` wires `StreamingModelClient` and `StreamingOnlyAgentRuntime`; `CloudServerOpenAiCompatibleE2ETest` creates a REST run and verifies persisted `model.delta` events. |
| 3 | Text deltas, tool-call intents, finish reasons, usage/tokens, latency, and provider errors are normalized into platform records/events. | ✓ VERIFIED | `ModelStreamChunk` sealed hierarchy covers `TextDelta`, `ToolCallIntent`, `Usage`, `Finished`, `Cancelled`, `TimedOut`, and `ProviderError`; `OpenAiToolCallAccumulator` aggregates fragments into complete Domain `ToolCall`; `OpenAiProviderErrorMapper` maps sanitized provider errors; E2E verifies replayed `model.delta`. |
| 4 | Raw secrets are not written to visible provider records/events/errors by default. | ✓ VERIFIED | `SecretRef`/`CredentialRef` redacted `toString`; `ResolvedSecret` boundary; `EnvironmentAndPropertySecretResolver`; `OpenAiProviderErrorMapper.sanitize`; `OpenAiCompatibleProviderContractTest` and `CloudServerOpenAiCompatibleE2ETest` assert fake secrets are absent from chunks/errors/API event history. |
| 5 | Provider calls support timeout, cancellation, retry/rate-limit/circuit-breaker hooks, and contract tests. | ✓ VERIFIED | `OpenAiProviderProperties.ResilienceOptions` models timeout/retry/rate limiter/circuit breaker; `ProviderResiliencePolicy` creates Resilience4j `Retry`, `RateLimiter`, `CircuitBreaker`, and `TimeLimiter`; adapter checks `CancellationToken`; provider contract tests cover timeout, cancellation, pre-stream retryable errors, and mid-stream no-retry behavior. |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderDescriptor.java` | Provider-neutral provider descriptor | ✓ VERIFIED | Exists, immutable record, validates provider/model consistency, holds `CredentialRef` only. |
| `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelDescriptor.java` | Provider-neutral model descriptor | ✓ VERIFIED | Exists and is used by provider registry and properties-backed registry. |
| `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderModelRef.java` | Explicit `provider:model` parser | ✓ VERIFIED | Exists, rejects blank/whitespace/missing or multiple separators, returns canonical format. |
| `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/StreamingModelClient.java` | Provider-neutral streaming model port | ✓ VERIFIED | Exists as Domain functional interface with Pi-owned sink and no Spring AI/Reactor/OpenAI imports. |
| `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelStreamChunk.java` | Provider-neutral chunk hierarchy | ✓ VERIFIED | Exists as sealed interface with text/tool/usage/finish/cancel/timeout/error chunks and latency/sequence metadata. |
| `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventPayload.java` | Normalized model event payload | ✓ VERIFIED | Contains enriched `ModelDeltaPayload` used by testkit and adapter runtime to publish `MODEL_DELTA`/`model.delta`. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/ModelProviderRegistry.java` | App provider registry port | ✓ VERIFIED | Lists providers/models and resolves `ProviderModelRef` into `ModelProviderResolution` without provider SDK details. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/SecretResolver.java` | Secret resolution boundary | ✓ VERIFIED | App boundary exists; raw value is represented only by `ResolvedSecret` at boundary. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ModelProviderQueryService.java` | Provider query use case | ✓ VERIFIED | Exists with default implementation returning provider/model metadata only. |
| `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java` | Deterministic no-key fake streaming model provider | ✓ VERIFIED | Exists and is covered by focused regression gate. |
| `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` | Streaming-aware fake runtime loop | ✓ VERIFIED | Consumes `StreamingModelClient`, publishes `MODEL_DELTA`, handles tool intents, cancellation, timeout, and provider errors. |
| `pi-agent-infrastructure-model-openai/pom.xml` | Isolated OpenAI-compatible provider module | ✓ VERIFIED | Module exists in root `pom.xml`; Spring AI and Resilience4j dependencies isolated here, not in Domain/App. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderProperties.java` | Provider config model | ✓ VERIFIED | Models base URL, completions path, model ID, credential ref, params, extra body, timeout/retry/rate-limit/circuit-breaker options. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/EnvironmentAndPropertySecretResolver.java` | `env:`/`config:` secret resolver | ✓ VERIFIED | Resolves env/config references from injected maps, returns redacted boundary types, no raw secret in `toString`. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java` | OpenAI-compatible `StreamingModelClient` | ✓ VERIFIED | Implements Pi streaming port, resolves secret at provider boundary, maps fake/Spring AI stream events into Pi chunks. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiToolCallAccumulator.java` | Adapter-local fragmented tool-call aggregation | ✓ VERIFIED | Accumulates OpenAI-style fragments and emits Domain `ToolCall` only after complete JSON object arguments are available. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapper.java` | Provider-neutral error taxonomy/sanitization | ✓ VERIFIED | Maps auth/rate-limit/5xx/context/safety/bad-response/timeout/stream failures and redacts secrets/auth headers. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java` | Spring composition for model provider beans | ✓ VERIFIED | Wires properties, registry, secret resolver, conditional Spring AI factory/client/runtime when provider enabled. |
| `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` | Dispatcher modelRef integration | ✓ VERIFIED | Default is explicit `openai-compatible:gpt-4.1-mini`; validates model refs with `ProviderModelRef.parse` before runtime dispatch. |
| `pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleProviderContractTest.java` | Fake provider contract suite | ✓ VERIFIED | Covers text, tool-call aggregation, usage present/missing, finish, 429/5xx, mid-stream error no-retry, timeout, cancellation, redaction. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudServerOpenAiCompatibleE2ETest.java` | Cloud Server fake provider E2E | ✓ VERIFIED | Creates REST run, waits terminal completion, verifies persisted/replayed `model.delta`, and asserts fake secret absence. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/OptionalRealProviderSmokeTest.java` | Gated optional real smoke | ✓ VERIFIED | Uses JUnit assumptions on `PI_OPENAI_COMPATIBLE_SMOKE_ENABLED` and required env vars; skipped by default. |
| `docs/phase-03-model-provider-contracts.md` | Contract documentation | ✓ VERIFIED | Documents MODEL-01..MODEL-05, provider registry, streaming chunks, OpenAI adapter boundary, credential rules, resilience, test commands, smoke env. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `AgentDefinition.modelRef` / dispatcher | `ProviderModelRef` | Validation before runtime dispatch | ✓ WIRED | `DefaultRunDispatcher.validateModelRef` calls `ProviderModelRef.parse`; default and config value use `openai-compatible:<model>`. |
| `ModelProviderRegistry` | Domain provider/model contracts | `ProviderModelRef` → `ModelProviderResolution` | ✓ WIRED | Registry port imports and resolves Domain model descriptors and credential ref. |
| `InMemoryModelProviderRegistry` | `OpenAiProviderProperties` | Properties create provider/model descriptors | ✓ WIRED | `fromProperties` maps OpenAI-compatible config into `ProviderDescriptor` and `ModelDescriptor`. |
| `OpenAiCompatibleStreamingModelClient` | `StreamingModelClient` | Implements Domain port | ✓ WIRED | Class explicitly `implements StreamingModelClient` and exposes `stream(...)`. |
| `OpenAiToolCallAccumulator` | `ModelStreamChunk.ToolCallIntent` | Adapter completes fragments before Domain sees tool call | ✓ WIRED | Adapter calls accumulator and emits `ModelStreamChunk.ToolCallIntent` only when complete `ToolCall` exists. |
| `ProviderResiliencePolicy` | `OpenAiCompatibleStreamingModelClient` | Pre-stream resilience decoration | ✓ WIRED | Client calls `resiliencePolicy.executeBeforeStream`; policy wires Retry/RateLimiter/CircuitBreaker/TimeLimiter. |
| `ModelProviderBeanConfiguration` | provider infrastructure module | Spring composition root | ✓ WIRED | Adapter Web imports provider infrastructure classes and conditionally creates `StreamingModelClient`/runtime beans. |
| `CloudServerOpenAiCompatibleE2ETest` | REST/run/event persistence/replay | Create run, worker execution, query event history | ✓ WIRED | Test verifies REST run terminal completion and persisted/replayed `model.delta` events. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `OpenAiCompatibleStreamingModelClient` | `ModelStreamChunk` stream | `OpenAiSpringAiModelFactory.create(config).stream(prompt, cancellationToken)` | Yes — fake tests inject deterministic stream events; Spring AI source maps `ChatResponse` to events | ✓ FLOWING |
| `OpenAiToolCallAccumulator` | complete `ToolCall` | accumulated `OpenAiStreamEvent.ToolCallFragment` arguments | Yes — parses complete JSON object arguments before emitting | ✓ FLOWING |
| `GeneralAgentLoop` | `MODEL_DELTA` events | `StreamingModelClient.stream` chunks | Yes — text and finish/usage chunks publish `RunEventPayload.ModelDeltaPayload` | ✓ FLOWING |
| `ModelProviderBeanConfiguration.StreamingOnlyAgentRuntime` | persisted model delta events | `StreamingModelClient` sink → `EventSink.publish` | Yes — text/finish chunks become `MODEL_DELTA` events through the normal event sink | ✓ FLOWING |
| `CloudServerOpenAiCompatibleE2ETest` | replayed `model.delta` history | REST-created run → worker → event store → events API | Yes — test asserts terminal completion and at least two `model.delta` events | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Focused Phase 3 domain/app/testkit/OpenAI module regression | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-app,pi-testkit,pi-agent-infrastructure-model-openai -am test` | Exited 0; only SLF4J no-provider warnings | ✓ PASS |
| Adapter Web provider wiring, Cloud Server fake E2E, gated real smoke | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=CloudServerOpenAiCompatibleE2ETest,OptionalRealProviderSmokeTest,ModelProviderWiringIntegrationTest test` | Exited 0; Cloud Server fake E2E started, completed run, and tests passed | ✓ PASS |
| Domain/App provider SDK leakage scan | Search for `org.springframework.ai`, `com.openai`, `reactor` under Domain/App source | Only Domain architecture test references forbidden packages; no App matches | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| MODEL-01 | 03-01, 03-03, 03-05, 03-07, 03-08 | Developer can register and resolve model providers through provider registry using model IDs, provider IDs, capabilities, and credential references. | ✓ SATISFIED | Domain descriptors and `ProviderModelRef`; App `ModelProviderRegistry`; Infrastructure `InMemoryModelProviderRegistry`; Spring wiring; docs mark validated. |
| MODEL-02 | 03-02, 03-04, 03-06, 03-07, 03-08 | Platform provides an OpenAI-compatible streaming chat adapter usable by the General Agent loop. | ✓ SATISFIED | `StreamingModelClient`; `OpenAiCompatibleStreamingModelClient`; streaming `GeneralAgentLoop`; conditional Cloud runtime wiring; fake Cloud Server E2E. |
| MODEL-03 | 03-02, 03-04, 03-06, 03-08 | Model adapter normalizes text deltas, tool-call intents, finish reasons, usage/tokens, latency, and provider errors into platform events and records. | ✓ SATISFIED | `ModelStreamChunk` hierarchy; accumulator; error mapper; `RunEventPayload.ModelDeltaPayload`; contract tests and persisted replay E2E. |
| MODEL-04 | 03-01, 03-03, 03-05, 03-06, 03-07, 03-08 | Provider configuration uses SecretRef/CredentialRef boundaries so raw secrets are not exposed in logs, prompts, events, or Admin GUI. | ✓ SATISFIED | `SecretRef`, `CredentialRef`, `SecretResolver`, `ResolvedSecret`, env/config resolver, sanitizer, redaction tests, E2E no-secret assertions. Admin GUI is not implemented in this phase, but no Admin-visible provider payload exists yet and docs carry the future rule. |
| MODEL-05 | 03-02, 03-04, 03-05, 03-06, 03-07, 03-08 | Provider calls support timeout, cancellation, retry/rate-limit/circuit-breaker hooks, and provider contract tests. | ✓ SATISFIED | `CancellationToken` propagation, timeout/cancel chunk contracts, `OpenAiProviderProperties.ResilienceOptions`, `ProviderResiliencePolicy`, dispatcher timeout compatibility, contract tests. |

**Orphaned requirements:** None. All Phase 3 requirement IDs in `.planning/REQUIREMENTS.md` (`MODEL-01` through `MODEL-05`) are declared in Phase 3 plan frontmatter and accounted for above.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapper.java` | 94 | `return null` | ℹ️ Info | Used as sentinel in private HTTP-status extraction helper; not a stub and does not affect goal. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiToolCallAccumulator.java` | 182 | `return null` | ℹ️ Info | JSON parser represents JSON `null` values; not an empty implementation. |

No blocker stub/placeholder patterns were found in Phase 3 production provider artifacts. `default-model` remains only in one negative test fixture; production dispatcher defaults to explicit `openai-compatible:gpt-4.1-mini` and validates `provider:model`.

### Human Verification Required

None required for Phase 3 goal achievement. Optional real-provider smoke testing is intentionally gated by environment variables and is not required for normal no-key verification.

### Gaps Summary

No gaps found. The phase goal is met with executable evidence, provider SDK boundaries preserved, all MODEL requirements covered, and no-key tests passing.

---

_Verified: 2026-06-14T10:52:29Z_  
_Verifier: the agent (gsd-verifier)_
