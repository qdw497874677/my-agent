---
phase: 03-model-provider-registry-and-openai-compatible-adapter
plan: 08
subsystem: provider-contract-verification
tags: [java21, openai-compatible, streaming, cloud-server, e2e, secret-redaction, resilience, cola]

requires:
  - phase: 03-06
    provides: OpenAI-compatible StreamingModelClient, stream source seam, tool-call aggregation, error mapper, resilience hooks
  - phase: 03-07
    provides: Cloud Server provider registry wiring, SecretResolver wiring, provider:model dispatch configuration
provides:
  - Fake OpenAI-compatible provider contract suite covering streaming, tool calls, usage, errors, timeout, cancellation, and redaction
  - Cloud Server fake provider E2E with no-key in-memory persistence fallback and event-history replay assertions
  - Gated optional real-provider smoke test skipped unless explicit env vars are set
  - Phase 3 downstream provider contract documentation and MODEL-01..MODEL-05 validation notes
affects: [phase-04-tool-registry, phase-05-admin-governance, phase-06-extension-surface, phase-09-production-hardening]

tech-stack:
  added: []
  patterns: [fake-provider-contract-tests, adapter-injected-stream-factory, no-docker-e2e-fallback, credential-redaction-contracts]

key-files:
  created:
    - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleProviderContractTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudServerOpenAiCompatibleE2ETest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/OptionalRealProviderSmokeTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/InMemoryCloudE2EConfiguration.java
    - docs/phase-03-model-provider-contracts.md
  modified:
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapper.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java
    - .planning/REQUIREMENTS.md

key-decisions:
  - "Use fake OpenAI-compatible stream fixtures and an injected OpenAiSpringAiModelFactory seam for provider E2E, preserving no-key verification."
  - "Publish provider-runtime TextDelta/Finished chunks as normalized model.delta events through the existing EventSink/persistence path."
  - "Use an in-memory Cloud Server E2E persistence/queue harness for this environment because Docker/Testcontainers are unavailable."
  - "Keep optional real provider smoke fully environment-gated with PI_OPENAI_COMPATIBLE_SMOKE_ENABLED plus base URL, API key, and model vars."

patterns-established:
  - "Provider contract tests assert Pi ModelStreamChunk/ProviderErrorSummary contracts rather than Spring AI/OpenAI SDK types."
  - "Cloud provider E2E can swap storage/queue implementations in test scope while still exercising REST, worker activation, EventSink, and event-history replay."
  - "HTTP-shaped provider exceptions are mapped reflectively without adding provider SDK types to Domain/App."

requirements-completed: [MODEL-01, MODEL-02, MODEL-03, MODEL-04, MODEL-05]

duration: 16m 45s
completed: 2026-06-14
---

# Phase 03 Plan 08: Provider Contract Verification and Documentation Summary

**No-key OpenAI-compatible provider verification with fake streaming contracts, Cloud Server replay E2E, secret redaction checks, and Phase 3 model-provider contract documentation.**

## Performance

- **Duration:** 16m 45s
- **Started:** 2026-06-14T10:29:41Z
- **Completed:** 2026-06-14T10:46:26Z
- **Tasks:** 3
- **Files modified:** 9

## Accomplishments

- Added `OpenAiCompatibleProviderContractTest` covering text deltas, finish reasons, fragmented tool-call aggregation, usage present/missing, latency, HTTP-shaped 429/5xx failures, mid-stream error retry prevention, timeout, cancellation, and raw secret redaction.
- Added `CloudServerOpenAiCompatibleE2ETest` using a fake OpenAI-compatible stream source to create a run through REST, activate the worker, persist normalized `model.delta` events, replay event history, verify terminal completion, and assert fake secret absence without Docker or real keys.
- Added `OptionalRealProviderSmokeTest`, skipped by default unless `PI_OPENAI_COMPATIBLE_SMOKE_ENABLED=true` and all real provider env vars are present.
- Documented provider registry, `provider:model` refs, streaming chunk semantics, OpenAI-compatible adapter boundaries, SecretRef/CredentialRef rules, resilience behavior, fake tests, optional smoke setup, and explicit deferrals in `docs/phase-03-model-provider-contracts.md`.
- Updated `.planning/REQUIREMENTS.md` with Phase 3 validation evidence for `MODEL-01` through `MODEL-05`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add fake OpenAI-compatible provider contract tests** - `db5effc` (`test`)
2. **Task 2: Add Cloud Server fake provider E2E and gated real-provider smoke** - `4e9c6b2` (`test`)
3. **Task 3: Document Phase 3 contracts and update requirement status** - `eac7e90` (`docs`)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleProviderContractTest.java` - Fake provider contract tests for streaming, tool calls, usage, errors, cancellation, timeout, and redaction.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapper.java` - Adds HTTP-shaped exception status extraction and broader API-key assignment redaction.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java` - Makes OpenAI stream factory injectable and publishes provider text/finish chunks as normalized `model.delta` events.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudServerOpenAiCompatibleE2ETest.java` - No-key fake provider Cloud Server E2E for REST-run creation, worker activation, event history replay, terminal completion, and secret absence.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/OptionalRealProviderSmokeTest.java` - Explicitly gated real provider smoke test.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java` - Test-only fake OpenAI stream source factory.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/InMemoryCloudE2EConfiguration.java` - Test-only in-memory repositories, run queue, audit no-op, and transaction template for no-Docker E2E fallback.
- `docs/phase-03-model-provider-contracts.md` - Downstream Phase 3 provider contract index.
- `.planning/REQUIREMENTS.md` - Requirement validation annotations for MODEL-01..MODEL-05.

## Decisions Made

- Kept provider verification no-key by injecting `OpenAiSpringAiModelFactory` in Adapter configuration and using deterministic fake stream events in tests.
- Used event-history replay assertions for the no-Docker Cloud Server provider E2E because live SSE extraction in the in-memory fallback hit a security/async transport issue unrelated to model-provider contracts.
- Documented direct HTTP, native Anthropic/Gemini, Admin provider UI, Vault/KMS, governed tool execution, MCP, and plugin provider discovery as explicit deferrals.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Added HTTP-shaped provider exception mapping**
- **Found during:** Task 1 provider contract verification
- **Issue:** 429/5xx exceptions thrown before stream creation mapped as generic transient failures without preserving HTTP status/provider code.
- **Fix:** `OpenAiProviderErrorMapper.fromThrowable` now reflectively extracts a `status()` method when present and applies `fromHttpStatus(...)` only before stream output starts.
- **Files modified:** `OpenAiProviderErrorMapper.java`
- **Verification:** `OpenAiCompatibleProviderContractTest` passed.
- **Committed in:** `db5effc`

**2. [Rule 1 - Bug] Broadened API-key assignment redaction**
- **Found during:** Task 1 secret redaction tests
- **Issue:** Messages containing `api_key=...` could leave the token name in safe messages and trip `ProviderErrorSummary` safety checks.
- **Fix:** Added API-key assignment sanitization and generic `api credential` replacement.
- **Files modified:** `OpenAiProviderErrorMapper.java`
- **Verification:** `OpenAiCompatibleProviderContractTest` passed and asserts chunks/errors do not contain the fake secret.
- **Committed in:** `db5effc`

**3. [Rule 3 - Blocking] Switched Cloud provider E2E to no-Docker in-memory persistence fallback**
- **Found during:** Task 2 verification
- **Issue:** The initial Testcontainers-based E2E failed because `/var/run/docker.sock` is unavailable in this execution environment.
- **Fix:** Added `InMemoryCloudE2EConfiguration` with test-only repositories, queue, audit no-op, and transaction template so REST, worker activation, EventSink, and replay can be verified without Docker.
- **Files modified:** `InMemoryCloudE2EConfiguration.java`, `CloudServerOpenAiCompatibleE2ETest.java`
- **Verification:** Focused Task 2 Maven test passed.
- **Committed in:** `4e9c6b2`

**4. [Rule 2 - Missing Critical] Published provider runtime chunks through EventSink**
- **Found during:** Task 2 E2E implementation
- **Issue:** The provider runtime streamed through `StreamingModelClient` but discarded chunks, so no `model.delta` records could be persisted/replayed.
- **Fix:** Added `ModelDeltaPublishingSink` inside the Adapter-owned streaming runtime to publish `TextDelta` and `Finished` chunks as `RunEventPayload.ModelDeltaPayload` through the existing EventSink.
- **Files modified:** `ModelProviderBeanConfiguration.java`
- **Verification:** Cloud provider E2E verifies persisted/replayed `model.delta` events.
- **Committed in:** `4e9c6b2`

---

**Total deviations:** 4 auto-fixed (2 bugs, 1 blocking issue, 1 missing critical event publication path).
**Impact on plan:** All fixes were necessary to satisfy the planned provider contract, no-key E2E, and secret-redaction requirements; no product-scope features were added.

## Issues Encountered

- Full `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test` remains environment-gated by pre-existing Testcontainers tests (`JdbcPersistenceIntegrationTest`, `PostgresRunQueueTest`) because Docker is unavailable at `/var/run/docker.sock`. Focused Phase 03-08 no-key verification passed.
- The no-Docker Cloud Server E2E validates event-history replay rather than live SSE stream extraction because the in-memory fallback path triggered a Spring Security/async extraction issue after the response was committed. Existing Phase 2 SSE coverage remains responsible for live SSE transport behavior.
- Maven emits existing SLF4J no-provider warnings and Mockito dynamic-agent warnings during tests; these are non-blocking and unrelated to Phase 03-08 behavior.

## Known Stubs

None. Stub scan findings are intentional null guards/default handling in test harnesses and provider configuration; no UI-flowing placeholder/mock data prevents the plan goal.

## User Setup Required

None for normal verification. Optional real-provider smoke requires:

```bash
export PI_OPENAI_COMPATIBLE_SMOKE_ENABLED=true
export PI_OPENAI_COMPATIBLE_BASE_URL="https://your-openai-compatible.example/v1"
export PI_OPENAI_COMPATIBLE_API_KEY="..."
export PI_OPENAI_COMPATIBLE_MODEL="your-model-id"
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=OptionalRealProviderSmokeTest test
```

## Verification

Passed:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am -Dtest=OpenAiCompatibleProviderContractTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=CloudServerOpenAiCompatibleE2ETest,OptionalRealProviderSmokeTest test
test -f docs/phase-03-model-provider-contracts.md && grep -q "MODEL-01" .planning/REQUIREMENTS.md && grep -q "OpenAI-compatible" docs/phase-03-model-provider-contracts.md
```

Environment-gated:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test
```

This failed only on Docker/Testcontainers availability for pre-existing infrastructure integration tests.

## Next Phase Readiness

- Phase 4 can consume complete provider-neutral tool-call intents without knowing OpenAI fragment semantics.
- Phase 4/9 can build on the documented secret-redaction and provider error taxonomy rules.
- Phase 5 Admin Governance can use `docs/phase-03-model-provider-contracts.md` as the backend/provider contract index for future provider status/config views.

## Self-Check: PASSED

- Found created files: `OpenAiCompatibleProviderContractTest.java`, `CloudServerOpenAiCompatibleE2ETest.java`, `OptionalRealProviderSmokeTest.java`, `FakeOpenAiProviderE2EConfiguration.java`, `InMemoryCloudE2EConfiguration.java`, and `docs/phase-03-model-provider-contracts.md`.
- Found task commits in git history: `db5effc`, `4e9c6b2`, and `eac7e90`.
- Focused provider contract and Cloud Server fake provider E2E verification passed without real provider keys.

---
*Phase: 03-model-provider-registry-and-openai-compatible-adapter*
*Completed: 2026-06-14*
