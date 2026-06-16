---
phase: 06-java-extension-surface-spi-and-spring
plan: 07
subsystem: testing
tags: [java, maven, extension-spi, spring-boot-starter, conformance, tool-governance, credentials]

requires:
  - phase: 06-java-extension-surface-spi-and-spring
    provides: SPI discovery, Spring extension registration, annotation contribution, and Adapter Web extension governance wiring from plans 06-03 through 06-06
provides:
  - No-key conformance tests proving SPI and Spring extension sources normalize to equivalent governed metadata
  - Product-path REST E2E proving extension tools execute only through ToolExecutionGateway lifecycle, policy, audit, events, and redaction
  - Provider boundary regression proving extension provider credentials remain SecretRef/CredentialRef values behind ModelProviderRegistry
affects: [phase-07-mcp-client-bridge-and-governed-remote-tools, phase-08-controlled-dynamic-plugin-jars, extension-governance, tool-governance]

tech-stack:
  added: []
  patterns: [no-key extension conformance fixtures, gateway-only tool execution E2E, registry-bound provider credential assertions]

key-files:
  created:
    - pi-agent-extension-api/src/testFixtures/java/io/github/pi_java/agent/extension/api/fixtures/ConformanceExtensionSources.java
    - pi-agent-infrastructure-extension/src/test/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderConformanceTest.java
    - pi-agent-spring-boot-starter/src/test/java/io/github/pi_java/agent/spring/autoconfigure/SpringExtensionConformanceTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ExtensionConformanceE2ETest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ExtensionConformanceFixtureConfiguration.java
  modified:
    - pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionModelProviderRegistry.java

key-decisions:
  - "Use duplicated lightweight conformance fixtures in module tests while keeping a canonical testFixtures source in extension-api to avoid invasive Maven test-fixtures wiring."
  - "Treat extension provider credential references as CredentialRef values in ExtensionModelProviderRegistry so provider descriptors stay registry-bound and never expose raw secrets."

patterns-established:
  - "Extension safety tests create runs only through public REST and observe ToolExecutionGateway output through lifecycle events and audit records."
  - "Extension conformance fixtures carry fake secret markers only in test configuration and assert REST/events/audit/provider descriptors never leak them."

requirements-completed: [EXT-01, EXT-02, EXT-05, WORK-06]

duration: 11m 54s
completed: 2026-06-16
---

# Phase 06 Plan 07: Extension Conformance Safety Summary

**No-key conformance gates proving SPI/Spring extensions remain normalized, gateway-governed, audited, redacted, workspace-bounded, and credential-safe.**

## Performance

- **Duration:** 11m 54s
- **Started:** 2026-06-16T00:00:10Z
- **Completed:** 2026-06-16T00:12:04Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments

- Added SPI and Spring conformance tests that assert identical source/capability IDs, status, compatibility, health, and provenance/source-kind metadata across extension paths.
- Added Adapter Web product-path E2E where REST-created runs invoke extension tools through `ToolExecutionGateway`, producing `tool.proposed`, `tool.policy_decided`, `tool.started`, terminal lifecycle events, and audit records.
- Proved approval-required extension workspace tools do not execute side effects before approval and that sensitive extension values are absent from REST event history, persisted event strings, and audit details.
- Extended `ExtensionModelProviderRegistry` to preserve `credentialRef` metadata as a Domain `CredentialRef`, then verified provider descriptors expose only redacted refs through `ModelProviderRegistry`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add shared conformance extension fixtures** - `9555884` (test)
2. **Task 2: Add product-path extension ToolGateway conformance E2E** - `e3df909` (test)
3. **Task 3: Assert credential and provider boundary conformance** - `12bb392` (test)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-extension-api/src/testFixtures/java/io/github/pi_java/agent/extension/api/fixtures/ConformanceExtensionSources.java` - canonical no-key fixture contract for safe tool, workspace tool, event listener, provider, policy, workspace, and memory metadata capabilities.
- `pi-agent-infrastructure-extension/src/test/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderConformanceTest.java` - SPI-style registry conformance coverage for normalized source/capability semantics.
- `pi-agent-spring-boot-starter/src/test/java/io/github/pi_java/agent/spring/autoconfigure/SpringExtensionConformanceTest.java` - Spring Bean conformance coverage for equivalent governance source/capability metadata.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ExtensionConformanceFixtureConfiguration.java` - no-key E2E fixture source, fake runtime, policy evaluator, and side-effect probe.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ExtensionConformanceE2ETest.java` - REST-created run E2E for gateway-only extension tool execution, approval blocking, redaction, audit, and provider credential boundaries.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionModelProviderRegistry.java` - maps extension provider `credentialRef` metadata into redacted Domain `CredentialRef` descriptors.

## Decisions Made

- Use duplicated lightweight fixture definitions for module-local SPI/Spring tests, while still adding the canonical extension-api testFixtures class requested by the plan. This avoided an invasive Maven test-fixtures dependency change while keeping stable fixture IDs for future reuse.
- Preserve extension provider credentials as `CredentialRef` in provider descriptors rather than string-only metadata. This keeps the boundary compatible with existing `ModelProviderRegistry.resolve(...)` semantics and verifies redacted secret handling through Domain value objects.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Preserved extension provider credential refs in registry descriptors**
- **Found during:** Task 3 (Assert credential and provider boundary conformance)
- **Issue:** `ExtensionModelProviderRegistry` previously dropped provider credential refs by constructing `ProviderDescriptor` with `credentialRef` set to `null`, so Task 3 could not prove SecretRef/CredentialRef boundaries.
- **Fix:** Mapped redacted `credentialRef` metadata into Domain `CredentialRef` and propagated extension `sourceKind` metadata from the capability.
- **Files modified:** `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionModelProviderRegistry.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ExtensionConformanceE2ETest test`
- **Committed in:** `12bb392`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** The fix was required to satisfy the credential-boundary conformance objective; no new architecture or external services were introduced.

## Issues Encountered

- Initial Spring conformance test tried to reference an infrastructure module test class from another module; fixed by duplicating the lightweight fixture locally, matching the plan's allowed fallback.
- Task 3 assertion initially expected `env:<redacted>` while the Domain `SecretRef` redaction convention returns `env:***`; updated the test to assert the existing platform redaction behavior.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-extension,pi-agent-spring-boot-starter -am -Dtest=ServiceLoaderConformanceTest,SpringExtensionConformanceTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ExtensionConformanceE2ETest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-extension,pi-agent-spring-boot-starter,pi-agent-adapter-web -am -Dtest=ServiceLoaderConformanceTest,SpringExtensionConformanceTest,ExtensionConformanceE2ETest test`

## Next Phase Readiness

- Phase 7 MCP remote tool work can reuse these assertions: remote tool sources must normalize into the same registry semantics and execute only through `ToolExecutionGateway`.
- Phase 8 dynamic plugin work should add plugin-source variants to the same conformance pattern, preserving policy/audit/event/redaction/credential checks.

## Self-Check: PASSED

- Verified created files exist: `ConformanceExtensionSources.java`, `ServiceLoaderConformanceTest.java`, `SpringExtensionConformanceTest.java`, `ExtensionConformanceE2ETest.java`, and `ExtensionConformanceFixtureConfiguration.java`.
- Verified task commits exist in git history: `9555884`, `e3df909`, and `12bb392`.

---
*Phase: 06-java-extension-surface-spi-and-spring*
*Completed: 2026-06-16*
