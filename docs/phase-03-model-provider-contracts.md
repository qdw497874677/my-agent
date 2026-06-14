# Phase 03 Model Provider Contracts

Phase 03 validates `MODEL-01` through `MODEL-05` for the Pi Java Agent Platform. It establishes a provider-neutral model registry, an OpenAI-compatible streaming adapter, credential boundaries, resilience semantics, and no-key contract/E2E verification.

## Requirement Coverage

| Requirement | Phase 03 validation |
|-------------|---------------------|
| `MODEL-01` | Provider/model registry resolves provider IDs, model IDs, capabilities, and `CredentialRef` values through App ports and Infrastructure implementations. |
| `MODEL-02` | OpenAI-compatible streaming chat adapter implements Pi `StreamingModelClient` and can be wired into Cloud Server runtime execution. |
| `MODEL-03` | Text deltas, complete tool-call intents, finish reasons, optional usage, latency, cancellation, timeout, and provider errors normalize into Pi-owned chunks/events. |
| `MODEL-04` | `SecretRef`/`CredentialRef` plus `SecretResolver` keep raw secrets at the provider boundary; tests assert secrets do not appear in chunks, errors, event history, or public responses. |
| `MODEL-05` | Provider calls include timeout/cancellation hooks, pre-stream retry/rate-limit/circuit-breaker extension points, and focused fake-provider contract tests. |

## Provider and Model Registry

Model references use explicit `provider:model` syntax, for example:

```text
openai-compatible:gpt-4.1-mini
```

The canonical parser is `ProviderModelRef.parse(...)`. Registry implementations resolve this reference into:

- `ProviderDescriptor` — provider identity, display metadata, credential reference, and provider capabilities.
- `ModelDescriptor` — model ID, model reference, provider ID, and `ModelCapabilities`.
- `ModelProviderResolution` — combined provider/model resolution for downstream dispatch.

The App layer owns the `ModelProviderRegistry` port. Infrastructure owns concrete config-backed implementations such as `InMemoryModelProviderRegistry`. Adapter Web wires these beans at the Spring composition boundary.

## Streaming Model Client Semantics

`StreamingModelClient` is the provider-neutral Domain port for streaming model calls. It accepts a `ModelRequest`, a `CancellationToken`, and a callback sink for `ModelStreamChunk` values.

Supported chunk types are:

- `TextDelta` — ordered text delta with provider ID, model ID, modelRef, sequence, and latency.
- `ToolCallIntent` — complete provider-neutral `ToolCall` intent. OpenAI incremental fragments are aggregated before Domain sees them.
- `Usage` — optional token usage. Providers may omit usage entirely.
- `Finished` — terminal model stream metadata including `ModelFinishReason` and optional usage.
- `Cancelled` / `TimedOut` — provider-neutral cancellation/timeout signals.
- `ProviderError` — normalized `ProviderErrorSummary` with Pi `MODEL` error category and safe messages.

Cloud Server provider runtime publishes normalized `model.delta` events to the existing `EventSink`, so provider output flows through the same persistence and replay path as earlier runtime events.

## OpenAI-Compatible Adapter Boundary

The OpenAI-compatible implementation lives in `pi-agent-infrastructure-model-openai` and is isolated from Domain/App. It uses Spring AI behind an `OpenAiSpringAiModelFactory` seam, but Domain/App only see Pi-owned contracts.

Important boundaries:

- Spring AI/OpenAI classes do not appear in Domain or App contracts.
- `OpenAiStreamSource` lets tests provide deterministic fake streams without real network/API keys.
- `OpenAiToolCallAccumulator` owns OpenAI fragmented `tool_calls[].function.arguments` semantics and emits only complete `ToolCall` intents.
- Direct HTTP/SSE adapter remains a documented future escape hatch for provider quirks; it is not part of Phase 03 default implementation.

## Credential and Secret Rules

Provider configuration stores references, not raw secrets:

- `SecretRef` describes where a secret lives, such as `env:PI_OPENAI_COMPATIBLE_API_KEY` or `config:pi.providers.openai-compatible.api-key`.
- `CredentialRef` wraps the secret reference for provider descriptors/config.
- `SecretResolver` resolves raw values only at the Infrastructure provider boundary.
- `ResolvedSecret.toString()` and provider descriptors must remain redacted.

Raw API keys must not appear in:

- provider descriptors or public registry views,
- model chunks/events/API responses,
- provider exceptions exposed through `ProviderErrorSummary`,
- future Admin/Web Console visible payloads.

## Resilience Behavior

Phase 03 uses conservative resilience semantics:

- Retry/rate-limit/circuit-breaker decorators apply only before stream iteration starts.
- HTTP-shaped 429 and 5xx provider failures are retryable only before any output has emitted.
- Mid-stream provider failures are normalized as non-retried `provider_stream_failed` chunks.
- Timeout/cancellation hooks are mandatory, and cancellation is observed through Pi `CancellationToken`.
- Resilience4j details remain in Infrastructure via `ProviderResiliencePolicy`.

## Verification

Normal CI/local verification requires no real provider credentials.

Focused commands:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am -Dtest=OpenAiCompatibleProviderContractTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=CloudServerOpenAiCompatibleE2ETest,OptionalRealProviderSmokeTest test
```

The fake provider contract suite covers text streaming, fragmented tool calls, usage present/missing, finish reasons, latency, HTTP-shaped 429/5xx mapping, mid-stream error retry prevention, timeout, cancellation, and secret redaction.

The Cloud Server fake provider E2E uses an injected fake OpenAI-compatible stream source and in-memory test persistence fallback so it runs without Docker or real keys in this environment. It validates REST-created runs, worker activation, persisted/replayed normalized `model.delta` events, terminal completion, and fake secret absence in public responses.

## Optional Real Provider Smoke

The real provider smoke test is skipped by default. To run it explicitly:

```bash
export PI_OPENAI_COMPATIBLE_SMOKE_ENABLED=true
export PI_OPENAI_COMPATIBLE_BASE_URL="https://your-openai-compatible.example/v1"
export PI_OPENAI_COMPATIBLE_API_KEY="..."
export PI_OPENAI_COMPATIBLE_MODEL="your-model-id"
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=OptionalRealProviderSmokeTest test
```

Absence of these environment variables must never fail normal Maven test runs.

## Explicit Deferrals

- Native Anthropic/Gemini/provider-specific adapters.
- Direct HTTP OpenAI-compatible adapter implementation.
- Admin UI provider management screens.
- Vault/KMS/database-backed credential storage and rotation.
- Full Phase 04 governed ToolExecutionGateway behavior for model-proposed tool execution.
- MCP model/tool bridge and dynamic plugin provider discovery.
