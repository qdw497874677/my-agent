---
phase: 03-model-provider-registry-and-openai-compatible-adapter
plan: 07
subsystem: model-provider-runtime-wiring
tags: [java, spring-boot, cola, openai-compatible, model-provider-registry, secret-resolver, dispatcher]

requires:
  - phase: 03-03
    provides: App-layer ModelProviderRegistry and SecretResolver ports
  - phase: 03-05
    provides: OpenAI-compatible provider properties, registry implementation, and secret resolver
  - phase: 03-06
    provides: Spring AI-backed OpenAI-compatible StreamingModelClient
provides:
  - Adapter composition root wiring for provider registry, secret resolver, and conditional OpenAI-compatible streaming client/runtime beans
  - Web adapter dependency on the isolated OpenAI-compatible infrastructure module
  - DefaultRunDispatcher provider:model modelRef injection and fast validation before runtime dispatch
  - No-key wiring regression coverage that preserves the fake runtime path when provider execution is disabled
affects: [phase-03, phase-04, phase-05, cloud-runtime-composition, provider-governance]

tech-stack:
  added: [pi-agent-infrastructure-model-openai dependency in pi-agent-adapter-web]
  patterns: [Adapter-only provider composition, conditional provider runtime beans, explicit provider:model dispatch configuration]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ModelProviderWiringIntegrationTest.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherModelRefTest.java
  modified:
    - pi-agent-adapter-web/pom.xml
    - pi-agent-infrastructure-model-openai/pom.xml
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java

key-decisions:
  - "Keep provider wiring in Adapter configuration: ModelProviderBeanConfiguration owns Spring composition for registry, secrets, streaming client, and optional provider runtime without moving Spring/provider SDK types into Domain or App."
  - "Use pi.runtime.default-model-ref with an openai-compatible:<default-model-id> fallback so queued dispatch always receives explicit provider:model syntax."
  - "Use the non-starter Spring AI OpenAI dependency in the provider infrastructure module so Cloud Server tests without real OpenAI keys do not trigger Spring AI auto-configuration failures."

patterns-established:
  - "Provider registry and SecretResolver are always available for catalog/config resolution, while the OpenAI StreamingModelClient/AgentRuntime path is conditional on pi.providers.openai-compatible.enabled=true."
  - "DefaultRunDispatcher validates AgentDefinition.modelRef with ProviderModelRef.parse immediately before building RunContext, causing bare/invalid refs to fail as platform errors before provider SDK invocation."

requirements-completed: [MODEL-01, MODEL-02, MODEL-04, MODEL-05]

duration: 7m 46s
completed: 2026-06-14
---

# Phase 03 Plan 07: Provider Registry and OpenAI-Compatible Cloud Wiring Summary

**Cloud runtime provider composition with OpenAI-compatible registry/secrets wiring and explicit provider:model dispatch selection.**

## Performance

- **Duration:** 7m 46s
- **Started:** 2026-06-14T10:18:55Z
- **Completed:** 2026-06-14T10:26:41Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Added `ModelProviderBeanConfiguration` at the Adapter composition boundary to create `OpenAiProviderProperties`, `ModelProviderRegistry`, and `SecretResolver` beans without expanding `CloudRuntimeBeanConfiguration`.
- Added conditional OpenAI-compatible `StreamingModelClient` and fallback `AgentRuntime` beans that only activate when `pi.providers.openai-compatible.enabled=true`, preserving no-key fake runtime tests.
- Wired the web adapter to depend on `pi-agent-infrastructure-model-openai` only at the composition root and switched that infrastructure module to the non-autoconfiguring Spring AI OpenAI artifact.
- Replaced dispatcher hardcoded bare `default-model` with explicit `openai-compatible:<default>` modelRef wiring and fast validation of `provider:model` syntax before runtime dispatch.
- Added regression tests for provider registry/secret wiring and dispatcher modelRef validation.

## Task Commits

Each task was committed atomically. Because both tasks were marked TDD, each has RED and GREEN commits:

1. **Task 1 RED: Wire provider beans in Adapter composition root** - `8e9a5bf` (`test`) added failing provider wiring/no-key runtime test.
2. **Task 1 GREEN: Wire provider beans in Adapter composition root** - `d1b4747` (`feat`) added provider bean configuration and web dependency wiring.
3. **Task 2 RED: Replace dispatcher hardcoded default-model with provider:model selection** - `eae323c` (`test`) added failing dispatcher modelRef tests.
4. **Task 2 GREEN: Replace dispatcher hardcoded default-model with provider:model selection** - `433bc5a` (`feat`) added modelRef constructor/config wiring and validation.

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-adapter-web/pom.xml` - Adds the OpenAI-compatible infrastructure dependency at the Adapter composition root.
- `pi-agent-infrastructure-model-openai/pom.xml` - Uses `spring-ai-openai` instead of the Spring Boot starter to avoid unwanted no-key autoconfiguration.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java` - Registers OpenAI-compatible provider properties, registry, secret resolver, conditional streaming model client, and optional provider runtime.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` - Injects configured `pi.runtime.default-model-ref` into `DefaultRunDispatcher` with an OpenAI-compatible fallback.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` - Adds provider:model constructor wiring and validates the AgentDefinition modelRef before runtime dispatch.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ModelProviderWiringIntegrationTest.java` - Covers provider registry/secret resolution and disabled-provider fake runtime behavior.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherModelRefTest.java` - Covers valid provider:model dispatch and bare modelRef fast failure.

## Decisions Made

- Provider composition remains Adapter-owned: Domain/App only expose/use platform ports and records.
- Provider registry and SecretResolver are created even when provider execution is disabled so catalog/config paths can resolve provider metadata without requiring keys.
- OpenAI-compatible runtime execution is opt-in through `pi.providers.openai-compatible.enabled=true` to keep CI/no-key runs on fake runtime unless explicitly enabled.
- Dispatcher model selection is configuration-driven via `pi.runtime.default-model-ref` with a fallback to `openai-compatible:${pi.providers.openai-compatible.default-model-id:gpt-4.1-mini}`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Avoided Spring AI starter auto-configuration API-key failure**
- **Found during:** Task 1 (provider wiring GREEN verification)
- **Issue:** Adding the OpenAI provider infrastructure module to the web adapter classpath pulled in `spring-ai-starter-model-openai`, which attempted to create Spring AI OpenAI auto-configured beans and failed startup when no `spring.ai.openai.api-key` was present.
- **Fix:** Switched `pi-agent-infrastructure-model-openai` from `spring-ai-starter-model-openai` to the non-starter `spring-ai-openai` artifact. The project-owned `ModelProviderBeanConfiguration` now controls all provider wiring explicitly.
- **Files modified:** `pi-agent-infrastructure-model-openai/pom.xml`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ModelProviderWiringIntegrationTest test`
- **Committed in:** `d1b4747`

---

**Total deviations:** 1 auto-fixed (Rule 3 blocking)
**Impact on plan:** The fix preserves the plan's no-key CI requirement and keeps provider composition project-owned at the Adapter boundary.

## Issues Encountered

- The plan's task-level verification command omitted `-am`, so Maven could not resolve reactor SNAPSHOT modules when run against `pi-agent-adapter-web` alone. Verification was run with `-am` to include required reactor modules.
- Overall plan verification `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am test` reached pre-existing Testcontainers integration tests and failed because this execution environment has no Docker socket (`/var/run/docker.sock`). This matches prior Phase 2 environment gating; focused non-Docker plan tests passed.

## Verification

Passed:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ModelProviderWiringIntegrationTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure,pi-agent-adapter-web -am -Dtest=DefaultRunDispatcherModelRefTest,ModelProviderWiringIntegrationTest test
```

Environment-gated:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am test
```

This failed only on Docker/Testcontainers availability for existing `JdbcPersistenceIntegrationTest` and `PostgresRunQueueTest`.

## Known Stubs

None. The grep hits for `null`/empty maps are guard defaults, no-op test fakes, or existing run-input fallbacks and do not block provider wiring goals.

## User Setup Required

None for no-key verification. To enable the real OpenAI-compatible path later, set provider properties such as `pi.providers.openai-compatible.enabled=true`, `pi.providers.openai-compatible.api-key`, and optional base URL/model settings.

## Next Phase Readiness

- Phase 03-08 can build fake provider contract/E2E coverage against an Adapter-wired provider registry and explicit dispatcher modelRef path.
- Cloud Server composition now has the extension point for real provider runtime activation without exposing secrets or breaking no-key tests.

## Self-Check: PASSED

- Confirmed key created files exist: `ModelProviderBeanConfiguration.java`, `ModelProviderWiringIntegrationTest.java`, and `DefaultRunDispatcherModelRefTest.java`.
- Confirmed task commits exist: `8e9a5bf`, `d1b4747`, `eae323c`, and `433bc5a`.

---
*Phase: 03-model-provider-registry-and-openai-compatible-adapter*
*Completed: 2026-06-14*
