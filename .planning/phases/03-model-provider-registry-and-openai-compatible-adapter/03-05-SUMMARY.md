---
phase: 03-model-provider-registry-and-openai-compatible-adapter
plan: 05
subsystem: openai-provider-infrastructure
tags: [java, maven, infrastructure, spring-ai, resilience4j, provider-registry, secret-resolver, cola]

requires:
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: Provider/model Domain descriptors, CredentialRef/SecretRef, App ModelProviderRegistry and SecretResolver ports
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Maven module conventions and Infrastructure layer patterns
provides:
  - Isolated pi-agent-infrastructure-model-openai module for Spring AI OpenAI-compatible and Resilience4j dependencies
  - OpenAiProviderProperties config model with base URL, completions path, default model, credential reference, extra body/default params, timeout, retry, rate limiter, and circuit breaker settings
  - EnvironmentAndPropertySecretResolver supporting env: and config: references from injected maps without raw secret leakage
  - InMemoryModelProviderRegistry mapping OpenAI-compatible properties to Domain ProviderDescriptor/ModelDescriptor/ModelProviderResolution records
affects: [03-06-openai-streaming-adapter, 03-07-cloud-composition-provider-dispatch, 05-admin-governance]

tech-stack:
  added: [Spring AI BOM 1.1.5, spring-ai-starter-model-openai, Resilience4j BOM 2.2.0, resilience4j retry/ratelimiter/circuitbreaker/timelimiter]
  patterns: [isolated provider infrastructure module, reference-only credential configuration, injected secret-source maps for tests, redacted default string output]

key-files:
  created:
    - pi-agent-infrastructure-model-openai/pom.xml
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderProperties.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/EnvironmentAndPropertySecretResolver.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/InMemoryModelProviderRegistry.java
    - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderPropertiesTest.java
    - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/EnvironmentAndPropertySecretResolverTest.java
    - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/InMemoryModelProviderRegistryTest.java
  modified:
    - pom.xml

key-decisions:
  - "Isolate Spring AI OpenAI-compatible and Resilience4j dependencies in pi-agent-infrastructure-model-openai, leaving Domain/App free of provider SDK dependencies."
  - "Model provider configuration as plain Java records first so later Spring @ConfigurationProperties wiring can bind without changing registry behavior."
  - "Resolve env:/config: secrets from injected maps in tests and default resolver construction, with toString and exception paths exposing only scheme-level redaction."

patterns-established:
  - "Provider properties convert into ProviderDescriptor and ModelDescriptor records rather than becoming the internal registry contract."
  - "Resilience settings are present as typed config values before the streaming adapter applies concrete Resilience4j decorators."
  - "Secret resolver returns Optional at the SecretRef boundary while CredentialRef resolution uses the App port's redacted exception path."

requirements-completed: [MODEL-01, MODEL-04, MODEL-05]

duration: 5m 07s
completed: 2026-06-14
---

# Phase 03 Plan 05: OpenAI Provider Infrastructure Foundation Summary

**OpenAI-compatible provider infrastructure module with isolated Spring AI/Resilience4j dependencies, typed provider config, env/config secret resolution, and in-memory registry descriptors.**

## Performance

- **Duration:** 5m 07s
- **Started:** 2026-06-14T09:58:40Z
- **Completed:** 2026-06-14T10:03:47Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added `pi-agent-infrastructure-model-openai` as a dedicated Maven module after `pi-agent-infrastructure`, importing Spring AI and Resilience4j through root dependency management while keeping those dependencies outside Domain/App.
- Added `OpenAiProviderProperties` to model OpenAI-compatible endpoint configuration, default model/capabilities, credential references, extra/default parameters, and typed resilience settings.
- Added `EnvironmentAndPropertySecretResolver` with `env:` and `config:` support through injected maps, plus redaction tests for resolved secret display and missing-secret exceptions.
- Added `InMemoryModelProviderRegistry` to expose provider/model descriptors and registry resolution from provider properties using the App `ModelProviderRegistry` port and Domain descriptor records.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add dedicated provider module and dependency foundation** - `7c3bbda` (feat)
2. **Task 2: Implement provider config, secret resolver, and in-memory registry** - `a495357` (feat)

**Plan metadata:** pending final docs commit

_Note: Task 2 was executed TDD-style with failing tests first; the red tests failed before implementation because the provider classes did not exist and because a scoped non-reactor command could not see upstream module classes._

## Files Created/Modified

- `pom.xml` - Adds the new provider module and imports Spring AI 1.1.5 plus Resilience4j 2.2.0 BOMs.
- `pi-agent-infrastructure-model-openai/pom.xml` - Isolated OpenAI-compatible provider infrastructure module with Domain/App, Spring AI OpenAI starter, Resilience4j, JUnit, and AssertJ dependencies.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderProperties.java` - Plain Java provider configuration and resilience options with redacted credential display.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/EnvironmentAndPropertySecretResolver.java` - App `SecretResolver` implementation for injected environment and configuration property sources.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/InMemoryModelProviderRegistry.java` - In-memory App registry implementation that converts provider properties into Domain descriptors.
- `pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderPropertiesTest.java` - Config validation, resilience, and redaction tests.
- `pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/EnvironmentAndPropertySecretResolverTest.java` - Env/config secret resolution and missing-secret redaction tests.
- `pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/InMemoryModelProviderRegistryTest.java` - Registry descriptor/resolution and unknown model/provider tests.

## Decisions Made

- Spring AI/OpenAI-compatible and Resilience4j artifacts are isolated in `pi-agent-infrastructure-model-openai`; no provider dependencies were added to `pi-agent-domain`, `pi-agent-app`, or the core infrastructure module.
- Provider configuration is plain Java and testable before Spring binding, so later composition plans can add `@ConfigurationProperties` wiring without changing the registry contract.
- Secret resolution accepts injected source maps rather than reading real process environment in tests, keeping verification deterministic and avoiding accidental secret exposure.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Used reactor upstream build for provider module tests**
- **Found during:** Task 2 TDD red verification
- **Issue:** Running the provider module tests without `-am` could not resolve freshly-created upstream Domain/App classes from the local reactor.
- **Fix:** Used `-am` for implementation and final verification so Maven built upstream modules in the same reactor.
- **Files modified:** None.
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am test` passed.
- **Committed in:** `a495357` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed blocking verification issue
**Impact on plan:** No scope creep; changes remained within planned module, config, resolver, registry, and tests.

## Issues Encountered

- The working tree contained pre-existing/parallel planning artifacts from other phases before execution. Task commits staged only Plan 03-05 files.
- A test assertion initially expected a trailing parenthesis in an App-layer missing-secret exception message. The test was corrected to assert the actual safe redaction string (`env:***`) and absence of raw secret names/values.
- Maven emits SLF4J no-provider warnings during final verification due transitive test/runtime dependencies; this did not fail tests and did not require logging configuration for this infrastructure foundation plan.

## Known Stubs

None. Stub scan only matched defensive null/blank checks in production code; these are validation guards, not UI placeholders or mock data paths.

## User Setup Required

None - no external service configuration required.

## Verification

- Task 1: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am -DskipTests compile` passed.
- RED Task 2: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiProviderPropertiesTest,EnvironmentAndPropertySecretResolverTest,InMemoryModelProviderRegistryTest test` failed before implementation/upstream reactor inclusion.
- Final: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am test` passed.

## Next Phase Readiness

- Plan 03-06 can build the Spring AI-backed `StreamingModelClient` inside the isolated module without leaking provider SDK/Spring AI types into Domain/App.
- Plan 03-07 can wire `OpenAiProviderProperties`, `EnvironmentAndPropertySecretResolver`, and `InMemoryModelProviderRegistry` into the Cloud Server composition root.
- Admin Governance can later list provider/model descriptors from registry metadata while raw credentials remain resolvable only at infrastructure call boundaries.

## Self-Check: PASSED

- Found created/modified files: `pom.xml`, `pi-agent-infrastructure-model-openai/pom.xml`, `OpenAiProviderProperties.java`, `EnvironmentAndPropertySecretResolver.java`, `InMemoryModelProviderRegistry.java`, and `03-05-SUMMARY.md`.
- Found task commits in git history: `7c3bbda` and `a495357`.
- Final provider module verification passed with upstream reactor modules: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-model-openai -am test`.

---
*Phase: 03-model-provider-registry-and-openai-compatible-adapter*
*Completed: 2026-06-14*
