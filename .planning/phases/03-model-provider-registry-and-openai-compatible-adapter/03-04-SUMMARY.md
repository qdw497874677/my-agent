---
phase: 03-model-provider-registry-and-openai-compatible-adapter
plan: 04
subsystem: testkit-runtime-streaming
tags: [java21, testkit, streaming, model-provider, agent-loop, cola]

requires:
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: Provider-neutral StreamingModelClient, ModelStreamChunk, ModelUsage, ModelFinishReason, and ProviderErrorSummary contracts
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: General Agent runtime contracts, RunEvent payloads, cancellation token, fake model/tool/policy testkit patterns
provides:
  - Deterministic no-key FakeStreamingModelClient for provider-neutral streaming chunks
  - Streaming-aware GeneralAgentLoop path preserving synchronous ModelClient compatibility
  - Runtime contract tests for ordered model deltas, complete tool-call intents, usage/finish metadata, cancellation, and provider errors
affects: [phase-03-openai-compatible-adapter, phase-04-tool-registry, runtime-contract-tests]

tech-stack:
  added: []
  patterns: [deterministic-fake-streaming-client, callback-stream-consumption, terminal-event-last, provider-neutral-runtime-events]

key-files:
  created:
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java
    - pi-testkit/src/test/java/io/github/pi_java/agent/testkit/FakeStreamingModelClientTest.java
    - pi-testkit/src/test/java/io/github/pi_java/agent/testkit/GeneralAgentLoopStreamingTest.java
  modified:
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java

key-decisions:
  - "Keep FakeStreamingModelClient deterministic and callback-based, with scripted actions instead of sleeps, networking, Reactor, or provider SDK dependencies."
  - "Expose streaming support through a GeneralAgentLoop constructor overload so existing synchronous ModelClient tests remain source-compatible."
  - "Publish finish/usage metadata as an empty text MODEL_DELTA payload because the existing Domain event vocabulary carries model metadata on ModelDeltaPayload."

patterns-established:
  - "Streaming testkit clients script provider-neutral ModelStreamChunk values and can split scripts across agent loop steps with nextStream()."
  - "The fake runtime consumes only complete provider-neutral tool-call intents; provider fragment semantics remain adapter-owned."
  - "Streaming cancellation and provider failures produce exactly one terminal run event last."

requirements-completed: [MODEL-02, MODEL-03, MODEL-05]

duration: 7m 05s
completed: 2026-06-14
---

# Phase 03 Plan 04: Fake Runtime Streaming Contract Summary

**Deterministic fake streaming model provider and streaming-aware General Agent loop validating ordered deltas, complete tool-call intents, metadata, cancellation, and model failures without real provider keys.**

## Performance

- **Duration:** 7m 05s
- **Started:** 2026-06-14T09:48:28Z
- **Completed:** 2026-06-14T09:55:33Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added `FakeStreamingModelClient`, a no-key deterministic `StreamingModelClient` testkit fake that scripts text deltas, complete tool-call intents, usage, finish, provider errors, cancellation, latency metadata, and step boundaries.
- Added a streaming constructor path to `GeneralAgentLoop` while preserving the existing synchronous `ModelClient` constructor and tests.
- Added runtime streaming tests proving ordered `MODEL_DELTA` events, tool proposal/tool execution flow from complete provider-neutral intents, finish/usage metadata capture, cancellation terminal behavior, and secret-safe provider error failures.
- Verified full `pi-testkit` tests, including existing synchronous runtime tests, with upstream modules included in the reactor.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add deterministic fake streaming model client**
   - `f8b070e` test(03-04): add fake streaming model tests
   - `ff5f2bc` feat(03-04): add fake streaming model client
2. **Task 2: Make GeneralAgentLoop consume streaming chunks**
   - `c96db6a` test(03-04): add streaming agent loop tests
   - `8de1bc9` feat(03-04): stream chunks through testkit agent loop

_Note: Both tasks were TDD-scoped tasks, so test and implementation changes were committed separately._

## Files Created/Modified

- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java` - Deterministic scripted streaming model fake for provider-neutral chunks and cancellation behavior.
- `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/FakeStreamingModelClientTest.java` - Unit tests for scripted text, tool, usage, finish, error, latency, and cancellation actions.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` - Added streaming model consumption path, metadata delta publishing, complete tool-call handling, cancellation/timeout/error mapping, and synchronous compatibility.
- `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/GeneralAgentLoopStreamingTest.java` - Runtime streaming contract tests for model deltas, tool calls, usage/finish metadata, cancellation, and provider errors.

## Decisions Made

- Used a constructor overload rather than replacing `ModelClient`, preserving all existing fake runtime callers and Phase 2 E2E wiring.
- Kept streaming consumption provider-neutral: the loop accepts only Pi `ModelStreamChunk` variants and never handles OpenAI fragment semantics.
- Used a metadata-only `MODEL_DELTA` with empty `textDelta` for finish/usage metadata because `RunEventPayload.ModelDeltaPayload` is the Domain payload currently carrying model metadata.
- Added `FakeStreamingModelClient.nextStream()` to script multi-step model/tool/model loops deterministically without asynchronous queues or sleeps.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Ran pi-testkit verification with upstream modules in the Maven reactor**
- **Found during:** Task 1 verification
- **Issue:** `mvn -q -pl pi-testkit -Dtest=FakeStreamingModelClientTest test` compiled against the locally installed `pi-agent-domain` artifact, which did not include the just-created Phase 03-02 streaming contracts in this parallel workspace.
- **Fix:** Used `-am` for plan verification so Maven built required upstream modules from the working tree before compiling `pi-testkit`.
- **Files modified:** None.
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am test` passed.
- **Committed in:** Not applicable; verification command adjustment only.

---

**Total deviations:** 1 auto-handled blocking verification issue.
**Impact on plan:** No scope creep; the implementation and tests remain within the planned testkit/runtime streaming scope.

## Issues Encountered

- The workspace contained unrelated parallel-execution planning and Phase 03-03 state changes. These were not modified by task commits and were left for their owning agents.

## Known Stubs

None. Stub-pattern scan of the created/modified testkit files found no TODO/FIXME placeholders or UI-flowing hardcoded empty data.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am -Dtest=FakeStreamingModelClientTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am test` — passed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 06/OpenAI-compatible adapter can now validate adapter output by feeding provider-neutral chunks into a fake runtime loop.
- Phase 4 tool registry work can rely on tool-call execution still being driven by complete `ToolCall` intents and the existing policy/tool invoker path.
- Cancellation and provider-error behavior are covered without real provider keys or network access.

## Self-Check: PASSED

- Created files exist: `FakeStreamingModelClient.java`, `FakeStreamingModelClientTest.java`, and `GeneralAgentLoopStreamingTest.java`.
- Modified file exists: `GeneralAgentLoop.java`.
- Task commits exist in git history: `f8b070e`, `ff5f2bc`, `c96db6a`, `8de1bc9`.

---
*Phase: 03-model-provider-registry-and-openai-compatible-adapter*
*Completed: 2026-06-14*
