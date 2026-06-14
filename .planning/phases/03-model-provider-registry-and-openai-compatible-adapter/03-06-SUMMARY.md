---
phase: 03-model-provider-registry-and-openai-compatible-adapter
plan: 06
subsystem: openai-compatible-streaming-adapter
tags: [java21, infrastructure, spring-ai, openai-compatible, streaming, resilience4j, secret-redaction, cola]

requires:
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: Provider-neutral StreamingModelClient chunks, ProviderErrorSummary, ModelUsage, ModelFinishReason, provider properties, registry, and SecretResolver boundary
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: Isolated pi-agent-infrastructure-model-openai module with Spring AI and Resilience4j dependencies
provides:
  - Spring AI-backed OpenAI-compatible StreamingModelClient implementation behind Pi-owned Domain contracts
  - Adapter-local OpenAI tool-call fragment accumulator emitting only complete Domain ToolCall intents
  - Provider-neutral OpenAI error mapper with auth/rate-limit/5xx/context/safety/timeout/cancellation taxonomy and secret redaction
  - Resilience4j-backed pre-stream timeout, retry, rate-limiter, and circuit-breaker policy hooks
  - Injectable Spring AI stream source factory seam for no-key adapter contract tests
affects: [03-07-cloud-composition-provider-dispatch, 03-08-provider-contract-e2e, phase-04-tool-registry]

tech-stack:
  added: []
  patterns: [adapter-owned-fragment-aggregation, injectable-provider-stream-source, pre-stream-resilience-only, secret-safe-provider-errors]

key-files:
  created:
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamEvent.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiToolCallAccumulator.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapper.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/ProviderResiliencePolicy.java
    - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClientTest.java
    - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiToolCallAccumulatorTest.java
    - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapperTest.java
  modified: []

key-decisions:
  - "Keep OpenAI stream parsing behind an OpenAiStreamSource seam so production uses Spring AI while tests remain deterministic and key-free."
  - "Apply retry/rate-limiter/circuit-breaker decorators only before stream iteration begins; after chunks are emitted, failures become provider-neutral error chunks rather than retries."
  - "Resolve raw API keys only while creating the adapter's model configuration, and sanitize all provider messages with OpenAiProviderErrorMapper before exposing Domain error records."

patterns-established:
  - "OpenAI tool-call fragments are accumulated by index/id/name/arguments and become Domain ToolCall intents only after arguments parse as complete JSON object payloads."
  - "Provider resilience policy owns concrete Resilience4j objects inside Infrastructure while Domain/App continue seeing only StreamingModelClient semantics."
  - "Adapter tests inject fake OpenAiStreamEvent fixtures rather than requiring real OpenAI-compatible endpoints or API keys."

requirements-completed: [MODEL-02, MODEL-03, MODEL-04, MODEL-05]

duration: 9m 35s
completed: 2026-06-14
---

# Phase 03 Plan 06: OpenAI-Compatible Streaming Adapter Summary

**Spring AI-backed OpenAI-compatible streaming adapter with Pi-owned text/tool/usage/finish/error chunks, secret-safe provider taxonomy, and pre-stream resilience hooks.**

## Performance

- **Duration:** 9m 35s
- **Started:** 2026-06-14T10:06:21Z
- **Completed:** 2026-06-14T10:15:56Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments

- Implemented `OpenAiCompatibleStreamingModelClient` as an Infrastructure adapter that implements the Domain `StreamingModelClient` port and emits only Pi-owned `ModelStreamChunk` values.
- Added `OpenAiToolCallAccumulator` to normalize OpenAI-style incremental tool-call fragments into complete Domain `ToolCall` intents only after argument payloads parse as JSON objects.
- Added `OpenAiProviderErrorMapper` to normalize authentication, rate-limit, transient 5xx, context-length, safety/content-filter, bad-response, timeout, cancellation, and stream failures without leaking API keys or authorization material.
- Added `OpenAiSpringAiModelFactory`, `OpenAiStreamSource`, and `OpenAiStreamEvent` so production can use Spring AI `OpenAiChatModel` while tests inject fake streams with no real key or network dependency.
- Added `ProviderResiliencePolicy` wrapping Resilience4j retry, rate limiter, circuit breaker, and time limiter configuration in the provider infrastructure module.

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement tool-call aggregation and provider error mapping**
   - `9501424` test(03-06): add OpenAI normalization tests
   - `b90be04` feat(03-06): implement OpenAI normalization helpers
2. **Task 2: Implement Spring AI OpenAI-compatible StreamingModelClient**
   - `79d98f1` test(03-06): add streaming adapter contract tests
   - `f3c13ed` feat(03-06): implement OpenAI streaming adapter

**Plan metadata:** pending final docs commit

_Note: Both tasks were TDD tasks, so each has test then implementation commits._

## Files Created/Modified

- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java` - Pi `StreamingModelClient` adapter that resolves secrets, constructs stream source configuration, emits normalized chunks, handles cancellation, and maps stream errors.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java` - Production Spring AI `OpenAiChatModel` factory plus response-to-stream-event conversion seam.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamEvent.java` - Adapter-local event vocabulary for fake and Spring AI streams.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java` - Minimal stream source interface used by the adapter.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiToolCallAccumulator.java` - Fragment accumulator and JSON object argument parser for OpenAI tool-call deltas.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapper.java` - Provider-neutral error taxonomy and redaction helper.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/ProviderResiliencePolicy.java` - Resilience4j policy wrapper for pre-stream provider calls.
- `pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClientTest.java` - No-key adapter contract tests for config, chunk normalization, cancellation, and sanitized error emission.
- `pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiToolCallAccumulatorTest.java` - Unit tests for fragmented, incomplete, and invalid tool-call argument payloads.
- `pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapperTest.java` - Unit tests for provider error taxonomy and secret redaction.

## Decisions Made

- Kept Spring AI-specific response handling in Infrastructure by translating it to adapter-local `OpenAiStreamEvent` values before the Domain `StreamingModelClient` sees anything.
- Used a no-key fake stream source seam for contract tests rather than adding a fake HTTP server in this plan; broader provider contract/E2E coverage remains assigned to Plan 03-08.
- Chose conservative retry semantics: resilience decorators are applied to source creation/stream startup, while mid-stream failures are mapped to `ProviderError` chunks instead of being retried.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Handled standalone module verification needing upstream reactor modules**
- **Found during:** Overall verification
- **Issue:** The plan's bare `-pl pi-agent-infrastructure-model-openai test` command failed in this workspace because freshly changed upstream reactor classes were not built/installed for standalone test discovery.
- **Fix:** Verified with `-am` so Maven built upstream Domain/App modules in the same reactor, matching the module pattern from Plan 03-05.
- **Files modified:** None.
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am test` passed.
- **Committed in:** N/A (verification command adjustment only)

**2. [Rule 1 - Bug] Caught interrupted Spring AI stream waiting**
- **Found during:** Task 2 implementation verification
- **Issue:** The Spring AI stream bridge waited on a `CountDownLatch` without handling `InterruptedException`, causing compilation to fail.
- **Fix:** Caught interruption, restored thread interrupt state, disposed the subscription, and surfaced the interruption through the adapter error path.
- **Files modified:** `OpenAiSpringAiModelFactory.java`
- **Verification:** Full provider module reactor tests passed.
- **Committed in:** `f3c13ed`

---

**Total deviations:** 2 auto-handled (1 blocking verification issue, 1 implementation bug).
**Impact on plan:** No scope creep; both fixes were required to compile and verify the planned adapter behavior.

## Issues Encountered

- Maven still emits SLF4J no-provider warnings during provider module tests due transitive dependencies; this is pre-existing/non-blocking and did not require changes in this plan.
- The working tree contains unrelated pre-existing/parallel Phase 2 and Phase 3 planning artifacts. Task commits staged only Plan 03-06 source/test files.

## Known Stubs

None. Stub-pattern scan found defensive null/blank checks and safe fallback messages only; no UI-flowing placeholders, TODO/FIXME stubs, or mock data paths block the plan goal.

## User Setup Required

None - no external service configuration required. Tests run with fake stream fixtures and no real API key.

## Verification

- Task 1 RED: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am -Dtest=OpenAiToolCallAccumulatorTest,OpenAiProviderErrorMapperTest test` failed before implementation because planned classes did not exist.
- Task 1 final: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am -Dtest=OpenAiToolCallAccumulatorTest,OpenAiProviderErrorMapperTest test` passed.
- Task 2 RED: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am -Dtest=OpenAiCompatibleStreamingModelClientTest test` failed before implementation because planned adapter/factory classes did not exist.
- Overall: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am test` passed.

## Next Phase Readiness

- Plan 03-07 can wire `OpenAiCompatibleStreamingModelClient`, `OpenAiSpringAiModelFactory.springAi()`, provider properties, and secret resolver into the Cloud Server composition root.
- Plan 03-08 can add fake OpenAI-compatible endpoint/contract tests and Cloud Server E2E using the adapter's no-key stream seams and normalized error/tool-call behavior.
- Phase 4 can consume complete Domain tool-call intents without knowing OpenAI fragment semantics.

## Self-Check: PASSED

- Found created files: `OpenAiCompatibleStreamingModelClient.java`, `OpenAiSpringAiModelFactory.java`, `OpenAiStreamEvent.java`, `OpenAiStreamSource.java`, `OpenAiToolCallAccumulator.java`, `OpenAiProviderErrorMapper.java`, `ProviderResiliencePolicy.java`, and the three new provider adapter test files.
- Found task commits in git history: `9501424`, `b90be04`, `79d98f1`, and `f3c13ed`.
- Final provider module verification passed with upstream reactor modules: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am test`.

---
*Phase: 03-model-provider-registry-and-openai-compatible-adapter*
*Completed: 2026-06-14*
