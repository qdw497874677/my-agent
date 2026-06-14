---
phase: 03-model-provider-registry-and-openai-compatible-adapter
plan: 02
subsystem: domain-model-contracts
tags: [java21, domain, model-provider, streaming, run-events, cola]

requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Framework-free Domain model, runtime event, cancellation, and model client contracts
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: Plan 01 provider/model descriptor and provider:model modelRef contracts
provides:
  - Provider-neutral StreamingModelClient contract using Pi-owned chunks and sink callback
  - Provider-neutral ModelStreamChunk hierarchy for text deltas, complete tool-call intents, usage, finish, cancellation, timeout, and provider errors
  - Normalized ModelUsage, ModelFinishReason, and ProviderErrorSummary value records
  - Backward-compatible ModelResponse and RunEventPayload.ModelDeltaPayload metadata enrichment
affects: [phase-03-openai-compatible-adapter, phase-03-fake-runtime-streaming, phase-04-tool-registry]

tech-stack:
  added: []
  patterns: [sealed-interface-domain-events, callback-streaming-port, constructor-backward-compatibility, secret-safe-error-summary]

key-files:
  created:
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelStreamChunk.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelUsage.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelFinishReason.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderErrorSummary.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/StreamingModelClient.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/model/ModelStreamingContractsTest.java
  modified:
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelResponse.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventPayload.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/event/RunEventContractTest.java

key-decisions:
  - "Use a callback-style StreamingModelClient with a Pi-owned ModelStreamSink instead of Flow.Publisher/Reactor to keep Domain framework-free."
  - "Represent provider tool-call streaming only as complete ToolCall intents in Domain; provider fragment aggregation remains adapter-owned."
  - "Keep legacy ModelResponse.FinalText, ModelResponse.ToolCallIntent, and ModelDeltaPayload constructors so existing fake runtime/testkit code stays source-compatible."

patterns-established:
  - "Provider-neutral stream chunks carry providerId, modelId, modelRef, sequence, and latency metadata on every variant."
  - "ProviderErrorSummary wraps PiError.Category.MODEL and rejects obvious secret-bearing safe messages."

requirements-completed: [MODEL-02, MODEL-03, MODEL-05]

duration: 5m 24s
completed: 2026-06-14
---

# Phase 03 Plan 02: Provider-Neutral Streaming Model Contract Summary

**Provider-neutral streaming model chunks with complete tool-call intents, usage/finish/error metadata, and enriched run-event deltas without provider SDK leakage.**

## Performance

- **Duration:** 5m 24s
- **Started:** 2026-06-14T09:39:32Z
- **Completed:** 2026-06-14T09:44:56Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments

- Added a framework-free `StreamingModelClient` port with Pi-owned `ModelStreamSink` callback semantics.
- Added a sealed `ModelStreamChunk` vocabulary for text deltas, complete tool-call intents, optional usage, finish reasons, cancellation, timeout, and normalized provider errors.
- Enriched synchronous `ModelResponse` and `RunEventPayload.ModelDeltaPayload` with optional provider/model, finish, usage, and latency metadata while preserving existing constructors.
- Added contract tests proving streaming chunks remain provider-neutral and event payloads can carry normalized streaming metadata.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define provider-neutral streaming chunks and metadata**
   - `6d48c1d` test(03-02): add streaming model contract tests
   - `8606ef2` feat(03-02): define streaming model chunks
2. **Task 2: Enrich model response and event payload normalization**
   - `6955bf5` test(03-02): add model metadata contract tests
   - `c21859d` feat(03-02): enrich model response metadata

_Note: Both tasks were TDD tasks, so each has test then implementation commits._

## Files Created/Modified

- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelStreamChunk.java` - Sealed provider-neutral streaming chunk hierarchy.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelUsage.java` - Optional input/output/total token usage value record.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelFinishReason.java` - Provider-neutral finish reason enum.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderErrorSummary.java` - Secret-safe provider error summary mapped to `PiError.Category.MODEL`.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/StreamingModelClient.java` - Framework-free streaming model port.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelResponse.java` - Backward-compatible metadata enrichment for sync responses.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventPayload.java` - Backward-compatible metadata enrichment for model delta events.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/model/ModelStreamingContractsTest.java` - Streaming model contract tests.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/event/RunEventContractTest.java` - Event payload metadata contract coverage.
- `.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/deferred-items.md` - Parallel-execution contamination note for out-of-scope Plan 01 test file.

## Decisions Made

- Use a callback-style streaming API rather than `Flow.Publisher` or Reactor to keep Domain simple and free of provider/framework dependencies.
- Keep tool-call streaming fragments out of Domain. Adapters must aggregate fragments and emit only complete `ToolCall` intents.
- Preserve constructor compatibility for existing fake runtime/testkit code instead of requiring unrelated updates in this plan.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Documented parallel workspace contamination from Plan 01 test file**
- **Found during:** Task 1 verification
- **Issue:** Maven test compilation included an untracked `ModelProviderRegistryContractsTest` from parallel Plan 01 work that referenced provider registry classes outside this plan's scope.
- **Fix:** Confirmed focused 03-02 contract and architecture tests pass, then documented the out-of-scope blocking item in `deferred-items.md` rather than modifying unrelated Plan 01 work.
- **Files modified:** `.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/deferred-items.md`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain -Dtest=ModelStreamingContractsTest,RunEventContractTest,DomainDependencyArchTest test`
- **Committed in:** `8606ef2`

---

**Total deviations:** 1 auto-handled blocking issue.
**Impact on plan:** No scope creep; unrelated parallel work was documented and 03-02 verification passed after parallel Plan 01 commits landed.

## Issues Encountered

- During early Task 1 verification, the workspace contained untracked Plan 01 provider registry tests. This was documented as deferred/out-of-scope. After Plan 01 commits appeared in history, full `pi-agent-domain` tests passed.

## Known Stubs

None. Stub-pattern scan of the created/modified Domain model files found no UI-flowing placeholder data or TODO/FIXME stubs.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain -Dtest=ModelStreamingContractsTest,RunEventContractTest,DomainDependencyArchTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain test` — passed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 03 can define App provider registry and secret resolver ports using the normalized Domain stream vocabulary.
- Plan 04 can evolve the fake General Agent loop to consume `StreamingModelClient` while maintaining sync `ModelClient` compatibility.
- Plan 06 OpenAI-compatible adapter can aggregate Spring AI/provider fragments into `ModelStreamChunk` without leaking provider types into Domain.

## Self-Check: PASSED

- Created files exist: `ModelStreamChunk.java`, `ModelUsage.java`, `ModelFinishReason.java`, `ProviderErrorSummary.java`, `StreamingModelClient.java`, `ModelStreamingContractsTest.java`.
- Modified files exist: `ModelResponse.java`, `RunEventPayload.java`, `RunEventContractTest.java`.
- Task commits exist in git history: `6d48c1d`, `8606ef2`, `6955bf5`, `c21859d`.

---
*Phase: 03-model-provider-registry-and-openai-compatible-adapter*
*Completed: 2026-06-14*
