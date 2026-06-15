---
phase: 05-agent-web-console-and-runtime-cockpit
plan: 04
subsystem: api
tags: [admin-governance, rest-api, dto, spring-mvc, cola, redaction]

requires:
  - phase: 03-model-provider-registry-and-openai-compatible-adapter
    provides: ModelProviderRegistry for provider governance status
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: ToolRegistry and governed tool audit/policy concepts
  - phase: 05-agent-web-console-and-runtime-cockpit
    provides: authenticated web adapter and Admin UI public API boundary
provides:
  - Public admin governance DTO contracts for overview, status, policy decisions, and audit summaries
  - Read-only GovernanceQueryService with runtime/provider/tool/extension/MCP/plugin overview
  - Authenticated inspect-only /api/admin/governance REST endpoints
affects: [phase-05-admin-ui, phase-06-extensions, phase-07-mcp, phase-08-plugins, governance]

tech-stack:
  added: []
  patterns:
    - Plain pi-agent-client records for public Admin API contracts
    - App-layer read query service delegating from thin Adapter REST controller
    - Future extension/MCP/plugin areas exposed as read-only placeholder statuses only

key-files:
  created:
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/GovernanceOverviewResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/GovernanceStatusDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PolicyDecisionSummaryDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/AuditSummaryDto.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceControllerTest.java
  modified: []

key-decisions:
  - "Keep Admin Governance contracts in pi-agent-client as plain redacted records with no Domain/Spring/Jakarta imports."
  - "Expose extension, MCP, and plugin governance as read-only FUTURE_ENABLED/UNCONFIGURED placeholders rather than configuration surfaces in Phase 5."
  - "Keep /api/admin/governance inspect-only with GET mappings only; mutations remain out of scope for Phase 5."

patterns-established:
  - "Admin governance API boundary: Controller converts authenticated request context and delegates immediately to GovernanceQueryService."
  - "Governance summaries use string statuses and redacted metadata so UI clients do not depend on Domain enums or secret-bearing config objects."

requirements-completed: [GUI-07, GUI-08]

duration: 6m 41s
completed: 2026-06-15
---

# Phase 05 Plan 04: Admin Governance Read APIs Summary

**Authenticated inspect-only Admin Governance APIs with redacted DTOs for runtime, providers, tools, policy decisions, audits, and future extension/MCP/plugin status.**

## Performance

- **Duration:** 6m 41s
- **Started:** 2026-06-15T05:29:23Z
- **Completed:** 2026-06-15T05:36:04Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Added `pi-agent-client.admin` public records for governance overview, status, policy decision summaries, and audit summaries.
- Added `GovernanceQueryService` and `DefaultGovernanceQueryService` in the App layer to aggregate runtime, provider registry, tool registry, and future extension/MCP/plugin statuses without exposing Domain objects to public API clients.
- Added authenticated read-only `/api/admin/governance`, `/overview`, `/policy-decisions`, and `/audits` endpoints.
- Added regression tests that enforce DTO redaction, authenticated GET behavior, and absence of POST/PUT/PATCH/DELETE mappings under the governance API.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Define admin governance DTOs and query service tests** - `24d32f0` (test)
2. **Task 1 GREEN: Define admin governance DTOs and query service** - `5ae912c` (feat)
3. **Task 2 GREEN: Expose inspect-only admin governance REST endpoints** - `541936b` (feat)

_Note: Plan tasks used TDD, so Task 1 produced a failing test commit followed by implementation commits._

## Files Created/Modified

- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/GovernanceOverviewResponse.java` - Public overview response aggregating all governance areas.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/GovernanceStatusDto.java` - Redacted status item for runtime/provider/tool/future extension areas.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/PolicyDecisionSummaryDto.java` - Public policy decision summary with run/session/tool context links.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/AuditSummaryDto.java` - Public audit summary with redacted details and run/session context links.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/GovernanceQueryService.java` - App-layer read-only governance query contract.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java` - Default overview aggregation from model provider registry, tool registry, optional runtime, and future placeholders.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java` - Thin authenticated GET-only REST controller.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/GovernanceBeanConfiguration.java` - Spring composition for governance query service.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceControllerTest.java` - TDD coverage for DTOs, redaction, endpoints, and read-only boundary.

## Decisions Made

- Keep Admin Governance contracts in `pi-agent-client` as plain Java records so Vaadin and other clients consume only public DTOs.
- Represent extension/MCP/plugin areas as read-only `FUTURE_ENABLED`/`UNCONFIGURED` placeholder statuses to satisfy Phase 5 visibility without introducing configuration or mutation surfaces before later phases.
- Preserve the admin role seam by requiring authenticated `/api/**` access through the existing security chain and dev/test headers; no governance endpoint was made public.

## Deviations from Plan

None - plan executed as written. The future extension/MCP/plugin placeholders were explicitly required by the plan.

## Known Stubs

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java:51` and `:57` return empty policy decision and audit lists by default because current Phase 5 App ports expose write-only `AuditRepository` and run-scoped event queries, not tenant-wide recent governance query ports. The REST/API contract and controller support inspect-only lists now; later persistence query ports can populate them without changing public DTOs.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java:92` intentionally marks future extension/MCP/plugin surfaces as placeholder metadata (`surface=placeholder`, `mutation=disabled`) per D-14 and GUI-08.

## Issues Encountered

- Initial RED test failed at compilation because governance DTOs and service did not exist, as expected for TDD.
- Before `AdminGovernanceController` existed, governance paths were forwarded to Vaadin fallback and produced `springServlet` errors; adding the controller resolved the routing issue.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AdminGovernanceControllerTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AdminGovernanceControllerTest,ToolRegistryControllerTest test` — passed.

## Self-Check: PASSED

- Found summary and key files: `05-04-SUMMARY.md`, `GovernanceOverviewResponse.java`, `AdminGovernanceController.java`.
- Found task commits in git history: `24d32f0`, `5ae912c`, `541936b`.

## Next Phase Readiness

- Vaadin Admin Governance views can now consume public authenticated APIs instead of Domain/App internals.
- Phase 6/7/8 can replace placeholder statuses with SPI/Spring extension, MCP, and plugin governance read models while preserving the Phase 5 API boundary.

---
*Phase: 05-agent-web-console-and-runtime-cockpit*
*Completed: 2026-06-15*
