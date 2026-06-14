---
phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
plan: 07
subsystem: adapter-web-tool-governance
tags: [java, spring-boot, tool-registry, tool-gateway, rest-api, event-mapping, flyway, cola]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Descriptor-first ToolRegistry, ToolRegistryQueryService, client tool catalog DTOs, DefaultToolExecutionGateway, infrastructure governance helpers, and built-in tool catalog from Plans 04-02 through 04-06.
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Cloud Server controller/security/correlation patterns, PersistingEventSink, AuditRepository, REST/SSE event DTO mapping, and Flyway migration baseline.
provides:
  - Read-only `/api/tools` Cloud Server endpoint returning ToolCatalogResponse client DTOs without executor or secret leakage.
  - Public tool lifecycle RunEventDto mapping with stable `tool.lifecycle` schema and redacted lifecycle payload maps for REST/SSE replay.
  - Adapter composition root for built-in tools, InMemoryToolRegistry, ToolRegistryQueryService, DefaultToolExecutionGateway, validation, policy, preview, redaction, payload limiting, audit, and persist-then-emit events.
  - Tool governance Flyway indexes for tool lifecycle event replay and tool audit lookup.
affects: [phase-04-tool-e2e, phase-05-agent-web-console, phase-06-extension-surface, phase-07-mcp-tools, phase-08-plugin-tools]

tech-stack:
  added: []
  patterns: [thin-read-only-controller, client-dto-boundary, redacted-tool-lifecycle-event-mapping, adapter-composition-root, single-governed-tool-gateway]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/ToolRegistryController.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java
    - pi-agent-infrastructure/src/main/resources/db/migration/V2__tool_governance_indexes.sql
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ToolRegistryControllerTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ToolGovernanceWiringTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/mapper/RunEventDtoMapper.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java

key-decisions:
  - "Expose tool catalog through a thin `/api/tools` read-only Adapter controller that delegates immediately to ToolRegistryQueryService and returns client DTOs only."
  - "Map ToolLifecyclePayload explicitly instead of relying on reflection so public event payloads use stable wire names, enum strings, redacted summaries, and filtered provenance metadata."
  - "Own tool governance composition in Adapter configuration while keeping Domain/App contracts Spring-free and Infrastructure implementations behind App ports."
  - "Wire the OpenAI-compatible runtime bean to require the single ToolExecutionGateway so production runtime composition cannot exist without the governed gateway."

patterns-established:
  - "ToolRegistryController follows RunController/SessionController correlation and authentication context patterns while remaining read-only for Phase 4."
  - "ToolGovernanceBeanConfiguration is the single Cloud Server composition point for built-ins, registry, query service, gateway collaborators, audit, and event sink."
  - "Tool lifecycle event DTOs expose `tool.lifecycle` schema version 1 and omit raw executor/class/secret metadata from public payload maps."

requirements-completed: [TOOL-01, TOOL-02, TOOL-06, TOOL-07, OPS-02, OPS-05]

duration: 9m 41s
completed: 2026-06-14
---

# Phase 04 Plan 07: Cloud Tool Governance Wiring and Catalog API Summary

**Cloud Server read-only tool catalog API with governed built-in tool gateway wiring and redacted tool lifecycle event DTO mapping.**

## Performance

- **Duration:** 9m 41s
- **Started:** 2026-06-14T19:35:08Z
- **Completed:** 2026-06-14T19:44:49Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added `GET /api/tools` as a read-only Tool Registry REST endpoint backed by `ToolRegistryQueryService`, preserving client DTO boundaries and excluding executor/raw secret details.
- Added explicit public mapping for tool lifecycle `RunEventPayload.ToolLifecyclePayload` to stable `tool.lifecycle` payload maps for event history and SSE replay.
- Wired Cloud Server tool governance through `ToolGovernanceBeanConfiguration`: built-in tool catalog, in-memory registry, catalog query service, JSON Schema validator, conservative policy, redactor, payload limiter, preview generator, and `DefaultToolExecutionGateway`.
- Connected the gateway to `AuditRepository` and `PersistingEventSink`, and added Flyway V2 indexes for tool lifecycle event and audit queries.

## Task Commits

Each task was committed atomically. TDD tasks have RED and GREEN commits:

1. **Task 1: Map tool lifecycle events and add catalog REST endpoint**
   - `a0b2064` test: add failing tool registry controller tests
   - `80d6645` feat: expose governed tool catalog API
2. **Task 2: Wire governed gateway, built-in tools, and audit/event path**
   - `09b41bd` test: add failing tool governance wiring tests
   - `f786484` feat: wire governed tool gateway in cloud server

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/ToolRegistryController.java` - Thin read-only controller for `GET /api/tools` using existing `RequestContext` construction.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/mapper/RunEventDtoMapper.java` - Adds explicit tool lifecycle payload mapping, stable `tool.lifecycle` schema, enum wire strings, preview/provenance maps, and sensitive metadata filtering.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java` - Cloud Server composition root for built-ins, registry, query service, governance collaborators, and `DefaultToolExecutionGateway`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java` - Requires `ToolExecutionGateway` in the OpenAI-compatible runtime bean so runtime composition is gateway-aware.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java` - Adds a diagnostic `toString()` exposing collaborator types for wiring verification without changing behavior.
- `pi-agent-infrastructure/src/main/resources/db/migration/V2__tool_governance_indexes.sql` - Adds indexes for tool lifecycle run events and tool audit records.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ToolRegistryControllerTest.java` - REST and event mapping tests for catalog metadata, read-only controller shape, and public redaction.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ToolGovernanceWiringTest.java` - Spring wiring tests for single gateway bean, built-in registry visibility, PersistingEventSink/AuditRepository collaborators, and gateway-aware runtime wiring.

## Decisions Made

- Reused existing generic `RunEventDto` shape instead of adding tool-specific client DTO variants; stable wire semantics are provided by `payloadSchema=tool.lifecycle`, `type=tool.*`, and explicit payload map fields.
- Implemented tool catalog REST as list-only in Phase 4; create/update/delete remains deferred to later Admin Governance/extension phases.
- Kept built-in workspace execution local-temp/dev-test oriented and documented by the underlying Infrastructure classes as not a production sandbox.
- Added only query indexes in V2; no V1 table rewrites or new governance tables were necessary for this plan.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrected over-specific read-only test expectations**
- **Found during:** Task 1 GREEN verification.
- **Issue:** The initial RED test asserted `PUT/PATCH /api/tools/{id}` returned 405, but no item route exists in a list-only read API, so Spring correctly returned 404 for an unknown path.
- **Fix:** Kept the `POST /api/tools` 405 assertion and added reflection checks that `ToolRegistryController` declares no POST/PUT/PATCH/DELETE mappings.
- **Files modified:** `ToolRegistryControllerTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ToolRegistryControllerTest test` passed.
- **Committed in:** `80d6645`

**2. [Rule 1 - Bug] Adjusted redaction assertion to match filtered public payload semantics**
- **Found during:** Task 1 GREEN verification.
- **Issue:** The test expected `[REDACTED]` placeholder values in the DTO, while the implemented public mapper filtered sensitive key/value pairs out of nested tool lifecycle maps entirely.
- **Fix:** Asserted redaction metadata is true and sensitive executor/token/raw secret values are absent from the serialized DTO.
- **Files modified:** `ToolRegistryControllerTest.java`
- **Verification:** Focused ToolRegistryControllerTest passed.
- **Committed in:** `80d6645`

**3. [Rule 3 - Blocking] Added missing ToolRegistryQueryService bean required by the new controller**
- **Found during:** Task 2 RED context-load verification.
- **Issue:** `ToolRegistryController` required `ToolRegistryQueryService`, but Cloud Server composition had not yet registered the tool registry/query-service path.
- **Fix:** Added `ToolGovernanceBeanConfiguration` with `ToolRegistry`, `ToolRegistryQueryService`, built-ins, and gateway collaborators.
- **Files modified:** `ToolGovernanceBeanConfiguration.java`
- **Verification:** `ToolGovernanceWiringTest` context loaded and passed.
- **Committed in:** `f786484`

**4. [Rule 2 - Missing Critical] Made runtime provider composition require the governed gateway**
- **Found during:** Task 2 wiring review.
- **Issue:** The OpenAI-compatible runtime bean could be constructed with only a model client and event sink, which would allow a production runtime path to exist without proving the governed tool gateway is present.
- **Fix:** Added `ToolExecutionGateway` as a required collaborator of the runtime bean and exposed it in diagnostic wiring output.
- **Files modified:** `ModelProviderBeanConfiguration.java`
- **Verification:** `ToolGovernanceWiringTest` and `CloudRuntimeWiringIntegrationTest` passed.
- **Committed in:** `f786484`

---

**Total deviations:** 4 auto-fixed (3 bugs/blocking correctness, 1 missing critical wiring guard).
**Impact on plan:** All fixes were directly required to complete the planned read-only API and single governed gateway wiring. No architectural scope expansion beyond Adapter composition.

## Known Stubs

None. Stub-pattern scanning found only null checks/default safety values in existing web/security/config code and the gateway's internal optional preview variable. No UI-facing placeholder/mock data or unwired empty defaults were introduced by this plan.

## Issues Encountered

- `RunEventDto.java` did not need modification because the existing generic event envelope already supports stable `payloadSchema`, `payloadVersion`, and map payloads; Plan 04-07 requirements were satisfied by mapper changes.
- `CloudRuntimeBeanConfiguration.java` did not need a structural change for the model runtime path because the relevant OpenAI-compatible runtime bean is owned by `ModelProviderBeanConfiguration`; Task 2 updated that composition point instead.
- The repository still contains unrelated pre-existing/parallel planning doc changes under Phase 02/03. This plan staged and committed only Plan 04-07 files.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ToolRegistryControllerTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ToolGovernanceWiringTest,CloudRuntimeWiringIntegrationTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ToolRegistryControllerTest,ToolGovernanceWiringTest,RunSseIntegrationTest,RunQueryIntegrationTest test` — passed.
- Content search for `FakeToolInvoker` in `pi-agent-adapter-web/src/main/java` — no matches.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 04-08 can run Cloud Server E2E through `/api/tools`, built-in descriptors, governed gateway lifecycle events, audit records, and SSE/event history replay.
- Phase 5 Web Console/Admin can consume the same read-only catalog DTOs and `tool.lifecycle` event payloads for tool cards and governance views.

## Self-Check: PASSED

- Found key files on disk: `04-07-SUMMARY.md`, `ToolRegistryController.java`, `ToolGovernanceBeanConfiguration.java`, and `V2__tool_governance_indexes.sql`.
- Verified commits exist in `git log --oneline --all`: `a0b2064`, `80d6645`, `09b41bd`, and `f786484`.

---
*Phase: 04-governed-tool-registry-workspace-and-invocation-pipeline*
*Completed: 2026-06-14*
