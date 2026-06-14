---
phase: 03-model-provider-registry-and-openai-compatible-adapter
plan: 01
subsystem: domain-model-provider-registry
tags: [java, domain, provider-registry, model-capabilities, credential-ref, secret-ref]

requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Framework-free Domain module, AgentDefinition.modelRef string contract, and architecture dependency gates
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: Phase 3 context decisions D-01, D-02, D-03, D-12, and D-14
provides:
  - ProviderDescriptor, ModelDescriptor, and ModelCapabilities Domain vocabulary for provider/model registry resolution
  - ProviderModelRef parser for explicit provider:model AgentDefinition.modelRef syntax
  - CredentialRef and SecretRef Domain contracts that store only references and redact string output
  - ModelProviderResolution linking provider, model, and credential reference without raw secret material
affects: [03-02-streaming-model-contracts, 03-03-app-provider-registry, 03-05-openai-provider-infrastructure, 05-admin-governance]

tech-stack:
  added: []
  patterns: [plain Java records, immutable defensive copies, redacted secret references, provider-neutral Domain contracts]

key-files:
  created:
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderDescriptor.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelDescriptor.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelCapabilities.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelProviderResolution.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/CredentialRef.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/SecretRef.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderModelRef.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/DomainModelValidation.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/model/ModelProviderRegistryContractsTest.java
  modified: []

key-decisions:
  - "Keep provider registry vocabulary in pi-agent-domain as Spring-free plain Java records with immutable collection copies."
  - "Represent credentials as CredentialRef/SecretRef reference identifiers only; toString emits scheme-level redaction rather than raw reference targets."
  - "Validate AgentDefinition.modelRef at registry boundaries with explicit provider:model ProviderModelRef parsing while leaving AgentDefinition.modelRef as String."

patterns-established:
  - "Domain provider descriptors copy List/Map inputs with List.copyOf/Map.copyOf and reject blank identities."
  - "Credential-bearing records override toString to avoid exposing env/config reference target names."
  - "ProviderModelRef.parse rejects leading/trailing whitespace, missing segments, whitespace segments, and ambiguous multiple separators."

requirements-completed: [MODEL-01, MODEL-04]

duration: 5m 23s
completed: 2026-06-14
---

# Phase 03 Plan 01: Provider/Model Registry Domain Contracts Summary

**Provider-neutral Domain vocabulary for model registry resolution with explicit provider:model parsing and redacted credential-reference boundaries.**

## Performance

- **Duration:** 5m 23s
- **Started:** 2026-06-14T09:39:25Z
- **Completed:** 2026-06-14T09:44:48Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Added `ProviderDescriptor`, `ModelDescriptor`, `ModelCapabilities`, and `ModelProviderResolution` as framework-free Domain contracts for Phase 3 provider/model registry resolution.
- Added `ProviderModelRef.parse(...)` so future registry boundaries can validate `AgentDefinition.modelRef` as explicit `provider:model` syntax without changing the existing `String modelRef` contract.
- Added `CredentialRef` and `SecretRef` as reference-only secret boundaries with redacted `toString()` output and tests asserting raw env/config targets are not printed.
- Covered descriptor immutability, non-blank validation, capability fields, resolution linking, parser rejection cases, and secret-reference redaction in `ModelProviderRegistryContractsTest`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add provider/model descriptor contracts** - `64ef497` (feat)
2. **Task 2: Add provider:model and secret-reference value objects** - `d0af1f1` (feat)

**Plan metadata:** pending final docs commit

_Note: TDD red tests were created and verified failing during execution, then implementation commits were made with `--no-verify` because this was a parallel executor run._

## Files Created/Modified

- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderDescriptor.java` - Provider identity, display metadata, optional credential reference, default capabilities, model descriptors, and immutable metadata.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelDescriptor.java` - Provider/model identity, display name, per-model capabilities, and immutable metadata.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelCapabilities.java` - Streaming text, tool-call intent, usage reporting, context window, max output, and provider extra-parameter support.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ModelProviderResolution.java` - Resolution result linking provider, model, and credential reference with redacted string output.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/CredentialRef.java` - Credential reference wrapper backed by `SecretRef` with redacted display.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/SecretRef.java` - Scheme-prefixed secret reference value object supporting env/config-style references.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderModelRef.java` - Validated parser/canonicalizer for `provider:model` strings.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/DomainModelValidation.java` - Package-private validation helper for Domain model records.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/model/ModelProviderRegistryContractsTest.java` - Contract coverage for the new registry and secret-reference vocabulary.

## Decisions Made

- Provider and model registry contracts remain in `pi-agent-domain` as serialization-neutral records with no Spring, Spring AI, OpenAI SDK, Jackson, Jakarta, DB, App, Infrastructure, or Adapter imports.
- `CredentialRef` composes `SecretRef` rather than carrying raw secret material; both expose reference identifiers for resolver boundaries but redact display/string output.
- `ProviderModelRef` is a boundary parser instead of changing `AgentDefinition.modelRef`, preserving the Phase 1 AgentDefinition contract while preparing Phase 3 registry resolution.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Isolated verification from concurrent parallel executor work**
- **Found during:** Task 2 verification
- **Issue:** The requested task-specific Maven command initially compiled unrelated untracked Phase 03 Plan 02 streaming tests from another parallel executor, causing failures outside this plan's file scope.
- **Fix:** Continued with this plan's scoped implementation, committed only Plan 03-01 files, and then reran full Domain verification once the parallel work had landed.
- **Files modified:** None for this plan beyond planned files.
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain test` passed.
- **Committed in:** `d0af1f1` (Task 2 commit)

---

**Total deviations:** 1 auto-handled blocking coordination issue
**Impact on plan:** No scope creep; this plan's code stayed within planned Domain registry/credential files.

## Issues Encountered

- The working tree contained pre-existing and parallel-agent planning changes before execution. Task commits staged only this plan's Domain files.
- Parallel Phase 03 Plan 02 work introduced streaming model tests/classes during execution. Full `pi-agent-domain` verification passed after those parallel commits were present.

## Known Stubs

None. The `description = description == null ? "" : description` default in `ProviderDescriptor` is display metadata normalization, not a UI-rendered placeholder or missing data source.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain -Dtest=ModelProviderRegistryContractsTest test` passed after Task 1 implementation.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain -DskipTests compile` passed after Task 2 implementation.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain test` passed as final plan verification.

## Next Phase Readiness

- Phase 03 Plan 02+ can build streaming model metadata and event payloads on top of the established provider/model/capability vocabulary.
- Phase 03 Plan 03 can introduce App-layer registry and secret resolver ports using `ProviderModelRef`, `CredentialRef`, and `ModelProviderResolution` without introducing raw secrets or provider SDK types into Domain.

## Self-Check: PASSED

- Found created files: `ProviderDescriptor.java`, `ModelDescriptor.java`, `ModelCapabilities.java`, `ModelProviderResolution.java`, `CredentialRef.java`, `SecretRef.java`, `ProviderModelRef.java`, `DomainModelValidation.java`, and `ModelProviderRegistryContractsTest.java`.
- Found task commits in git history: `64ef497` and `d0af1f1`.
- Final Domain module verification passed.

---
*Phase: 03-model-provider-registry-and-openai-compatible-adapter*
*Completed: 2026-06-14*
