---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 10
subsystem: api
tags: [spring-mvc, rest, security-context, run-events, dto-mapping, tdd]

requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: App session/run use-case interfaces, client REST/event DTOs, security principal, correlation filter
provides:
  - Session-centric REST controllers for sessions and runs
  - Run activation trigger seam for later worker scheduling composition
  - Explicit Domain RunEvent to public RunEventDto mapping
affects: [phase-02-sse, phase-02-composition-root, phase-05-web-console, api-clients]

tech-stack:
  added: []
  patterns: [Spring MVC controllers delegate to App use cases, client DTO boundary, activation hook seam, explicit event DTO mapping]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/RunController.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/NoopRunActivationTrigger.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/mapper/RunEventDtoMapper.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunApiIntegrationTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunQueryIntegrationTest.java
  modified: []

key-decisions:
  - "Plan 02-10 keeps REST controllers thin: they build RequestContext from PiPrincipal plus CorrelationFilter attributes and immediately delegate to App use cases."
  - "Plan 02-10 exposes run activation as RunActivationTrigger instead of wiring concrete worker/dispatcher behavior in the web adapter."
  - "Plan 02-10 maps Domain RunEvent to client RunEventDto explicitly using RunEventType.wireName and payload schema/version fields."

patterns-established:
  - "Controller RequestContext pattern: Principal + servlet correlation attributes -> PiPrincipal.toRequestContext(...)."
  - "REST-created runs call a small activation hook after createRun returns 202 Accepted; concrete scheduling remains deferred to the composition-root plan."
  - "Adapter event mapping preserves public DTO identity without Jackson polymorphic @class leakage from Domain."

requirements-completed: [CLOUD-01, CLOUD-03, CLOUD-04, CLOUD-05]

duration: 10m 52s
completed: 2026-06-14
---

# Phase 02 Plan 10: Session-Centric REST Controllers and Event Mapper Summary

**Session/run REST API controllers with App-use-case delegation, activation hook seam, and explicit provider-neutral RunEvent DTO mapping.**

## Performance

- **Duration:** 10m 52s
- **Started:** 2026-06-14T05:45:11Z
- **Completed:** 2026-06-14T05:51:43Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added `SessionController` for `POST /api/sessions`, `GET /api/sessions/{sessionId}`, and `GET /api/sessions/{sessionId}/history`.
- Added `RunController` for session-centric run creation, detail/status, cancellation, event history, steps, messages, tool calls, and result resources.
- Preserved the client DTO boundary: controller signatures return `pi-agent-client` DTOs or `ResponseEntity` wrappers, not Domain records.
- Added `RunActivationTrigger` and default no-op bean so REST-created runs have an activation seam now, while concrete worker scheduling remains for Plan 02-12.
- Added `RunEventDtoMapper` preserving `wireName`, sequence, trace/correlation IDs, redaction metadata, `payloadSchema`, `payloadVersion`, and payload attributes.
- Added Spring MVC integration tests for authentication, delegation, activation hook invocation, query resource endpoints, event cursor parameters, and mapper behavior.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Controller API tests** - `94f175f` (test)
2. **Task 1 GREEN: Session/run REST controllers** - `17ab935` (feat)
3. **Task 2: RunEvent DTO mapper** - `d93de81` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java` - Session-centric REST endpoints and shared `RequestContext` conversion.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/RunController.java` - Session-centric run command/query endpoints plus activation hook call.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/NoopRunActivationTrigger.java` - Default no-op trigger bean until Plan 02-12 wires worker scheduling.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/mapper/RunEventDtoMapper.java` - Explicit Domain `RunEvent` to client `RunEventDto` mapper.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunApiIntegrationTest.java` - Command endpoint/auth/delegation/activation tests.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunQueryIntegrationTest.java` - Query endpoint/event cursor/mapper tests.

## Decisions Made

- Used Spring MVC controllers only in Adapter; business behavior stays in App interfaces to preserve COLA boundaries.
- Kept `RunActivationTrigger` as a controller-package port and provided a no-op bean so the web adapter is runnable before Plan 02-12 supplies the concrete scheduler trigger.
- Used explicit reflection over Java record components for non-extension Domain payloads and direct attributes for `ExtensionPayload`; no Domain Jackson polymorphism or `@class` output was introduced.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Explicit Spring MVC binding names added**
- **Found during:** Task 1 (REST controller implementation)
- **Issue:** Tests failed because this Maven build does not expose Java parameter names to Spring MVC, causing unnamed `@PathVariable`/`@RequestParam` binding failures.
- **Fix:** Added explicit names to all controller `@PathVariable` and `@RequestParam` annotations.
- **Files modified:** `SessionController.java`, `RunController.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='RunApiIntegrationTest,RunQueryIntegrationTest'`
- **Committed in:** `17ab935`

---

**Total deviations:** 1 auto-fixed (Rule 1 bug)
**Impact on plan:** Required for controller correctness in the current Maven compiler environment; no scope expansion.

## Issues Encountered

- Spring test output includes Mockito dynamic-agent warnings from the existing test stack; tests pass and no production behavior is affected.

## Known Stubs

- `NoopRunActivationTrigger.java` intentionally provides a no-op default activation trigger. It is an explicit seam required by this plan; Plan 02-12 owns concrete wiring to `RunWorkerScheduler.triggerAsync()`.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='RunApiIntegrationTest,RunQueryIntegrationTest'` — passed.

## Next Phase Readiness

- Plan 02-11 can reuse `RunEventDtoMapper` for REST/SSE envelope consistency.
- Plan 02-12 can replace the no-op activation trigger with concrete `RunWorkerScheduler.triggerAsync()` composition without changing controller code.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*


## Self-Check: PASSED

- Verified created controller, mapper, test, and summary files exist.
- Verified task commits exist: `94f175f`, `17ab935`, `d93de81`.
