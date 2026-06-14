---
phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
plan: 04
subsystem: infrastructure-tool-governance
tags: [java, infrastructure, tool-registry, json-schema, networknt, policy, redaction, payload-limits, cola]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Framework-free ToolDescriptor, ToolSchema, risk/side-effect metadata, and ProvisionPreview contracts from Plan 04-01.
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Descriptor-first ToolRegistry and ToolExecutorBinding App ports from Plan 04-02.
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: ToolExecutionGateway collaborator ports for validation, policy, preview, redaction, and payload limiting from Plan 04-03.
provides:
  - Infrastructure-only JSON Schema argument validation behind the ToolArgumentValidator App port.
  - Descriptor-first in-memory ToolRegistry implementation for built-in/test registrations.
  - Conservative default policy evaluator, non-executing preview generator, schema/secret-aware redactor, and payload limiter helpers.
affects: [phase-04-tool-gateway-wiring, phase-04-built-in-tools, phase-05-agent-web-console, phase-06-extension-surface, phase-07-mcp-tools, phase-08-plugin-tools]

tech-stack:
  added: [com.networknt:json-schema-validator]
  patterns: [infrastructure-only-json-schema, descriptor-first-registry, conservative-default-policy, non-executing-preview, schema-driven-redaction, bounded-payload-summaries]

key-files:
  created:
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/InMemoryToolRegistry.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/NetworkntToolArgumentValidator.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/DefaultToolPolicyEvaluator.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/DefaultToolRedactor.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/DefaultToolPayloadLimiter.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/DefaultToolPreviewGenerator.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/tool/ToolInfrastructureGovernanceTest.java
  modified:
    - pom.xml
    - pi-agent-infrastructure/pom.xml

key-decisions:
  - "Keep networknt JSON Schema validation in Infrastructure only and expose only safe validation summaries through the App validator port."
  - "Use conservative default policy semantics: safe read-only allowed, side-effectful and unscoped tools require preview/approval, destructive or critical tools block by default."
  - "Generate provision previews as static impact estimates only; preview generation must not execute tool bindings, workspace writes, or processes."
  - "Return payload summaries and truncation metadata for oversized payloads instead of propagating unbounded raw arguments/results."

patterns-established:
  - "NetworkntToolArgumentValidator maps ToolSchema dialects to networknt specifications and redacts validation details to schema path/keyword summaries."
  - "InMemoryToolRegistry accepts normalized ToolDescriptor plus ToolExecutorBinding registrations and rejects duplicate descriptor IDs."
  - "DefaultToolRedactor combines ToolSchema.sensitiveFields, sensitive key names, and SecretRef/CredentialRef/token/password value patterns."
  - "DefaultToolPayloadLimiter computes approximate byte sizes and returns bounded preview metadata when payloads exceed configured/schema limits."

requirements-completed: [TOOL-03, TOOL-04, TOOL-05, OPS-03, OPS-05, WORK-08]

duration: 15m 18s
completed: 2026-06-14
---

# Phase 04 Plan 04: Tool Infrastructure Governance Summary

**Infrastructure-only JSON Schema validation plus conservative policy, preview, redaction, and payload-limit collaborators for the governed tool gateway.**

## Performance

- **Duration:** 15m 18s
- **Started:** 2026-06-14T19:05:52Z
- **Completed:** 2026-06-14T19:21:10Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Added `NetworkntToolArgumentValidator` behind the App `ToolArgumentValidator` port, with schema dialect selection and safe validation error summaries that do not echo raw input.
- Added `InMemoryToolRegistry` for source-agnostic descriptor-first tool registrations, including duplicate ID rejection and optional resolution by namespaced ID.
- Added default Infrastructure collaborators for conservative policy decisions, static provision previews, schema/secret-aware redaction, and bounded payload summaries.
- Extended `ToolInfrastructureGovernanceTest` to cover JSON Schema validation, COLA dependency isolation, registry behavior, policy defaults, redaction, payload limiting, and preview behavior.

## Task Commits

Each task was committed atomically. Task 2 followed TDD with a RED test commit and GREEN implementation commit:

1. **Task 1: Add JSON Schema validator and in-memory registry infrastructure**
   - `9ef1e24` feat: add tool schema validation infrastructure
2. **Task 2: Implement conservative policy, preview, redaction, and limits**
   - `3d08ccf` test: add failing tool governance collaborator tests
   - `e749d7a` feat: implement default tool governance helpers

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pom.xml` - Adds dependency management for `com.networknt:json-schema-validator`.
- `pi-agent-infrastructure/pom.xml` - Adds the Infrastructure-only networknt dependency.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/InMemoryToolRegistry.java` - Descriptor-first in-memory registry for built-in/test tool registrations.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/NetworkntToolArgumentValidator.java` - Networknt-backed ToolSchema input validation with safe error summaries.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/DefaultToolPolicyEvaluator.java` - Conservative default policy evaluator using descriptor risk, side-effect, scopes, and AgentDefinition policy metadata.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/DefaultToolRedactor.java` - Schema-sensitive-field and pattern-based redaction helper.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/DefaultToolPayloadLimiter.java` - Argument/result limit checks with truncation metadata and bounded previews.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/tool/DefaultToolPreviewGenerator.java` - Non-executing impact estimate generator for risky/side-effectful tool calls.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/tool/ToolInfrastructureGovernanceTest.java` - Non-Docker coverage for infrastructure tool governance collaborators.

## Decisions Made

- Kept all `com.networknt` imports in `pi-agent-infrastructure`; Domain and App remain JSON-schema-library-neutral and framework-free.
- Defaulted side-effectful tools to `REQUIRE_APPROVAL` with preview required, even when the only issue is an unapproved scope, to keep the platform conservative before later tenant/user policy engines exist.
- Treated previews as immutable impact summaries: they report risk/side-effect/scopes/policy metadata and explicitly mark `executesSideEffects=false`.
- Used approximate UTF-8 byte sizing and `payloadPreview` metadata for payload limits rather than serializing/storing full raw payloads.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Cleared corrupt Maven dependency cache**
- **Found during:** Task 1 verification
- **Issue:** Maven failed resolving `tools.jackson.dataformat:jackson-dataformat-yaml:3.1.1` with `Illegal packet size`, indicating a corrupt local artifact/cache transfer.
- **Fix:** Removed the corrupt local dependency directory and reran Maven so it could download a valid artifact.
- **Files modified:** None.
- **Verification:** Focused `ToolInfrastructureGovernanceTest` compile/test progressed past dependency resolution.
- **Committed in:** Not applicable; local cache repair only.

**2. [Rule 3 - Blocking] Used networknt 2.0.0 instead of the planned 3.x line in this Spring Boot 3.5/Jackson 2 reactor**
- **Found during:** Task 1 verification
- **Issue:** networknt 3.x compiled against the new `tools.jackson` stack and required `com.fasterxml.jackson.annotation.JsonSerializeAs`, which was not available in the Boot-managed Jackson 2.x dependency set or Maven Central as a compatible `jackson-annotations` version in this environment.
- **Fix:** Pinned `com.networknt:json-schema-validator` to `2.0.0`, which still supports the required JSON Schema dialect validation APIs while remaining compatible with the current Spring Boot 3.5/Jackson 2 reactor.
- **Files modified:** `pom.xml`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am -Dtest=ToolInfrastructureGovernanceTest test` passed.
- **Committed in:** `9ef1e24`

**3. [Rule 1 - Bug] Adapted tests to existing ToolExecutionResult and RequestContext signatures**
- **Found during:** Task 1 and Task 2 test compilation
- **Issue:** Initial tests assumed helper factories/signatures that do not exist in the current Domain/App records.
- **Fix:** Constructed `ToolExecutionResult`, `ToolDescriptor`, and `SecurityPrincipalContext` using their actual record constructors.
- **Files modified:** `ToolInfrastructureGovernanceTest.java`
- **Verification:** Focused tests passed after implementation.
- **Committed in:** `9ef1e24`, `3d08ccf`

---

**Total deviations:** 3 auto-fixed (2 blocking, 1 bug)
**Impact on plan:** Fixes were required for dependency compatibility and compilation. No architectural boundary changes or gateway contract changes were introduced.

## Known Stubs

None. Stub-pattern scan found only null checks/default safety values and empty Optional/Map values used for immutable record defaults, not UI-facing placeholders or unwired mock data.

## Issues Encountered

- The full plan verification command `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-app,pi-agent-infrastructure -am test` reached pre-existing Testcontainers integration tests and failed because this execution environment has no Docker socket (`/var/run/docker.sock`). This is an environment gate already tracked by prior phase summaries, not caused by this plan.
- The focused non-Docker verification command passed and covers all new Plan 04-04 behavior.
- The shell did not have `rg`; dedicated content-search tooling confirmed no `com.networknt` imports in Domain/App and no execution calls in the preview generator.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am -Dtest=ToolInfrastructureGovernanceTest test` — passed.
- Content search for `com.networknt` in `pi-agent-domain/src/main/java` — no matches.
- Content search for `com.networknt` in `pi-agent-app/src/main/java` — no matches.
- Content search for `ProcessBuilder|WorkspaceGateway|ToolExecutorBinding|\.execute\(` in `DefaultToolPreviewGenerator.java` — no matches.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-app,pi-agent-infrastructure -am test` — blocked by pre-existing Docker/Testcontainers environment gate; new focused tests passed.

## User Setup Required

None - no external service configuration required for the new non-Docker tests. Docker remains required only for pre-existing Testcontainers persistence/queue integration tests.

## Next Phase Readiness

- The App `DefaultToolExecutionGateway` can now be wired to real Infrastructure collaborators for validation, conservative policy, preview, redaction, and payload limiting.
- Later built-in tools, workspace tools, SPI/Spring tools, MCP tools, and plugin tools can register descriptor/binding pairs in the source-agnostic registry and rely on the same governance helpers.

## Self-Check: PASSED

- Found all key files on disk: `pom.xml`, `pi-agent-infrastructure/pom.xml`, all six Infrastructure tool implementation files, `ToolInfrastructureGovernanceTest.java`, and this `04-04-SUMMARY.md`.
- Verified commits exist in `git log --oneline --all`: `9ef1e24`, `3d08ccf`, and `e749d7a`.

---
*Phase: 04-governed-tool-registry-workspace-and-invocation-pipeline*
*Completed: 2026-06-14*
