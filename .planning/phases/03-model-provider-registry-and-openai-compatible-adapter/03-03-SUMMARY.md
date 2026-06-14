---
phase: 03-model-provider-registry-and-openai-compatible-adapter
plan: 03
subsystem: app-model-provider-registry
tags: [java, app-layer, provider-registry, secret-resolver, redaction, cola]

requires:
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: ProviderDescriptor, ModelDescriptor, ModelCapabilities, ProviderModelRef, CredentialRef, SecretRef, and ModelProviderResolution Domain contracts from Plan 03-01
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Framework-free App use-case style, RequestContext, and App architecture gates
provides:
  - Framework-free App ModelProviderRegistry port for listing providers/models and resolving provider:model references
  - SecretResolver App boundary returning raw material only through ResolvedSecret while keeping display/toString redacted
  - ModelProviderQueryService and DefaultModelProviderQueryService for future REST/Admin provider catalog consumption without raw secrets
affects: [03-05-openai-provider-infrastructure, 03-07-cloud-composition-provider-dispatch, 05-admin-governance]

tech-stack:
  added: []
  patterns: [plain Java App ports, Optional-based unknown model resolution, redacted secret material boundary, constructor-injected App query service]

key-files:
  created:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/ModelProviderRegistry.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/SecretResolver.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/ResolvedSecret.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ModelProviderQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultModelProviderQueryService.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/port/ModelProviderAppPortContractTest.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultModelProviderQueryServiceTest.java
  modified: []

key-decisions:
  - "Keep model provider registry and secret resolution as App ports over Domain records, with no Spring, provider SDK, or persistence types in App."
  - "Use Optional for unknown provider/model registry resolution so callers never receive null and can choose API-specific error mapping later."
  - "Expose ResolvedSecret.rawValue only from the SecretResolver boundary and ensure default string/error paths carry redacted metadata only."

patterns-established:
  - "Registry ports provide convenience defaults for listModels and resolve while concrete infrastructure only needs to supply provider descriptors."
  - "App query service returns provider/model/capability/credential-reference metadata and overrides toString on credential-bearing records to redact reference targets."
  - "TDD App-layer contract tests use in-memory port implementations to keep behavior executable without infrastructure dependencies."

requirements-completed: [MODEL-01, MODEL-04]

duration: 5m 13s
completed: 2026-06-14
---

# Phase 03 Plan 03: App Provider Registry and Secret Boundary Summary

**Framework-free App provider registry and query boundaries that resolve provider:model metadata while keeping secret material redacted by default.**

## Performance

- **Duration:** 5m 13s
- **Started:** 2026-06-14T09:48:30Z
- **Completed:** 2026-06-14T09:53:43Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Added `ModelProviderRegistry` in the App layer to list providers, list provider models, and resolve `ProviderModelRef`/`String modelRef` into `ModelProviderResolution` without returning null for unknown providers/models.
- Added `SecretResolver` and `ResolvedSecret` as the App secret boundary, including raw value access for infrastructure callers and redacted display/toString safety for logs, errors, Admin, and API consumers.
- Added `ModelProviderQueryService` plus `DefaultModelProviderQueryService` so future REST/Admin surfaces can list provider catalogs and resolve model refs through metadata-only App records.
- Extended App architecture verification coverage by running the existing ArchUnit gate against the new App ports/use cases.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define App provider registry and secret resolver ports** - `67306f0` (feat)
2. **Task 2: Add provider query use case for future API/Admin consumers** - `ba4a209` (feat)

**Plan metadata:** pending final docs commit

_Note: TDD red tests were created and verified failing during execution, then implementation commits were made with `--no-verify` because this was a parallel executor run._

## Files Created/Modified

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/ModelProviderRegistry.java` - App port for provider catalog listing, model listing, and Optional-based provider:model resolution.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/SecretResolver.java` - App boundary for resolving `SecretRef`/`CredentialRef` with redacted missing-secret exception messages.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/ResolvedSecret.java` - Secret material wrapper with raw value access and redacted display validation/toString.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ModelProviderQueryService.java` - Metadata-only provider catalog and provider-model detail query contract.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultModelProviderQueryService.java` - Constructor-injected plain Java query service backed by `ModelProviderRegistry`.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/port/ModelProviderAppPortContractTest.java` - Contract tests for registry resolution, unknown model handling, and secret redaction.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultModelProviderQueryServiceTest.java` - Query service tests for catalog listing, metadata-only resolution, and unknown model refs.

## Decisions Made

- Model provider registry and secret resolution remain App-layer ports over Domain records; no Spring, Spring AI, OpenAI SDK, persistence, Vault, KMS, or config-framework types entered App.
- Unknown provider/model resolution uses `Optional.empty()` rather than null or an App-specific checked exception, preserving simple use-case semantics and leaving HTTP error mapping to Adapter Web later.
- Credential-bearing query records intentionally expose `CredentialRef` reference metadata but override string output so raw secret values and reference targets are not emitted through default display paths.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Ran App verification with reactor upstream modules**
- **Found during:** Task 1 verification
- **Issue:** The plan's `mvn -pl pi-agent-app ...` command could not compile against just-created Phase 03 Domain classes when they were not installed in the local Maven repository for this isolated module build.
- **Fix:** Used `-am` for App verification so Maven also built required upstream modules in the same reactor. No production scope was changed.
- **Files modified:** None.
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=ModelProviderAppPortContractTest,AppDependencyArchTest test` passed.
- **Committed in:** `67306f0` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed blocking verification issue
**Impact on plan:** No scope creep; the implementation stayed within planned App ports, use cases, tests, and architecture gates.

## Issues Encountered

- The working tree contained pre-existing/parallel-agent planning changes and testkit changes before and during execution. Task commits staged only this plan's App files.
- The literal grep-based stub scan matched null checks and `"<none>"` redaction/display text. These are defensive validation and safe redaction labels, not UI stubs or missing data sources.

## Known Stubs

None. No placeholder or mock data paths were added to production code. `"<none>"` appears only as safe display text for absent credential references.

## User Setup Required

None - no external service configuration required.

## Verification

- RED Task 1: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -Dtest=ModelProviderAppPortContractTest test` failed before App provider ports existed.
- Task 1: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=ModelProviderAppPortContractTest,AppDependencyArchTest test` passed.
- RED Task 2: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=DefaultModelProviderQueryServiceTest test` failed before query service classes existed.
- Final: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am test` passed.

## Next Phase Readiness

- Phase 03 Plan 05 can implement infrastructure configuration and secret resolution behind `ModelProviderRegistry` and `SecretResolver` without changing App contracts.
- Phase 03 Plan 07 can wire provider dispatch by resolving `AgentDefinition.modelRef` through the App registry/query boundary.
- Admin Governance can later consume `ModelProviderQueryService` metadata without receiving raw secret values.

## Self-Check: PASSED

- Found created files: `ModelProviderRegistry.java`, `SecretResolver.java`, `ResolvedSecret.java`, `ModelProviderQueryService.java`, and `DefaultModelProviderQueryService.java`.
- Found task commits in git history: `67306f0` and `ba4a209`.
- Final App module verification passed with upstream reactor modules: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am test`.

---
*Phase: 03-model-provider-registry-and-openai-compatible-adapter*
*Completed: 2026-06-14*
