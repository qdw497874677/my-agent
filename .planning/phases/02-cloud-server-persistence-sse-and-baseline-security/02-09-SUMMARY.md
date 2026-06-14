---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 09
subsystem: security
tags: [spring-boot, spring-security, oauth2-resource-server, correlation, actuator, tdd]

requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: App RequestContext, SecurityPrincipalContext, and CorrelationContext contracts from Plan 02-04
provides:
  - Runnable Spring Boot web adapter entrypoint
  - Dev/test authentication principal path with JWT-ready Resource Server security shape
  - Request correlation servlet filter with response header, request attributes, and MDC baseline
  - Security/correlation integration test coverage for CLOUD-05
affects: [phase-02-rest-controllers, phase-02-sse, phase-02-runtime-composition, cloud-security]

tech-stack:
  added: []
  patterns: [Spring MVC adapter boundary, dev-auth filter, JWT-ready security chain, correlation MDC filter]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/PiCloudServerApplication.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/PiPrincipal.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/SecurityConfig.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/DevAuthenticationFilter.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/correlation/CorrelationFilter.java
    - pi-agent-adapter-web/src/main/resources/application.yml
    - pi-agent-adapter-web/src/main/resources/application-test.yml
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SecurityAndCorrelationIntegrationTest.java
  modified: []

key-decisions:
  - "Keep the web adapter runnable while excluding DataSource/Flyway auto-configuration until Plan 02-12 owns the single runtime composition root."
  - "Use a dev/test-only safe-header authentication filter while retaining oauth2ResourceServer JWT configuration in every security chain for production readiness."

patterns-established:
  - "Adapter security maps safe dev headers to PiPrincipal and App RequestContext without exposing raw auth headers or JWT claims."
  - "CorrelationFilter owns X-Correlation-ID/X-Request-ID normalization, pi.* request attributes, response correlation header, and MDC keys."

requirements-completed: [CLOUD-01, CLOUD-05]

duration: 4m 30s
completed: 2026-06-14
---

# Phase 02 Plan 09: Spring Boot Shell, Security Baseline, and Correlation Summary

**Spring Boot web shell with authenticated `/api/**`, public actuator health/info, dev/test principals, JWT-ready Resource Server configuration, and request correlation headers/MDC.**

## Performance

- **Duration:** 4m 30s
- **Started:** 2026-06-14T05:34:51Z
- **Completed:** 2026-06-14T05:39:21Z
- **Tasks:** 1 TDD task
- **Files modified:** 8

## Accomplishments

- Added `PiCloudServerApplication` as the Spring Boot web adapter entrypoint with package scanning and scheduling enabled, without adding runtime/persistence/queue/SSE composition beans.
- Added `SecurityConfig` with public `/actuator/health` and `/actuator/info`, authenticated `/api/**`, deny-by-default fallback, dev/test auth filter registration, and `oauth2ResourceServer(...jwt...)` configuration.
- Added `DevAuthenticationFilter` and `PiPrincipal` for safe dev/test tenant/user principal creation and conversion into App-layer `RequestContext`.
- Added `CorrelationFilter` to normalize `X-Correlation-ID`/`X-Request-ID`, set `pi.traceId`, `pi.correlationId`, and `pi.causationId`, return `X-Correlation-ID`, and populate MDC keys.
- Added `application.yml` and `application-test.yml` baseline configuration for virtual threads, actuator exposure, and test auth behavior with no real secrets.
- Added integration coverage for public health, protected API authentication, dev header principal context, and response correlation headers.

## Task Commits

1. **Task 1 RED: Security/correlation integration tests** - `b6ddadc` (`test`)
2. **Task 1 GREEN: Web shell, security, and correlation implementation** - `72fbbb0` (`feat`)

_Note: This plan used TDD, so Task 1 intentionally has separate RED and GREEN commits._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/PiCloudServerApplication.java` - Runnable Spring Boot application class for the web adapter.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/PiPrincipal.java` - Adapter principal that converts tenant/user/authorities into App `RequestContext`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/SecurityConfig.java` - Dev/test and production security filter chains with JWT-ready Resource Server shape.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/DevAuthenticationFilter.java` - Safe-header dev/test authentication filter using `X-Pi-Dev-Tenant` and `X-Pi-Dev-User`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/correlation/CorrelationFilter.java` - Request correlation attribute/header/MDC filter.
- `pi-agent-adapter-web/src/main/resources/application.yml` - Virtual thread, actuator, and temporary web-shell auto-configuration baseline.
- `pi-agent-adapter-web/src/main/resources/application-test.yml` - Test profile auth configuration with no real secrets.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SecurityAndCorrelationIntegrationTest.java` - Integration tests for security and correlation behavior.

## Decisions Made

- Kept runtime dependency-chain composition out of this plan; no Spring beans were added for `RunQueue`, `CancellationRegistry`, `RunDispatcher`, `RunWorkerScheduler`, `EventSink`, or `RunEventFanout`.
- Excluded DataSource/Flyway auto-configuration in the web adapter baseline so the runnable shell can start before Plan 02-12 supplies the single composition root and concrete infrastructure wiring.
- Kept `oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))` configured even in dev/test security chains while using a dev/test `JwtDecoder` stub to avoid real issuer/key requirements during local tests.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Excluded premature DataSource/Flyway auto-configuration**
- **Found during:** Task 1 GREEN verification
- **Issue:** The web adapter depends on infrastructure, which brings JDBC/Flyway classes into the Spring Boot classpath. Without datasource settings, actuator health auto-configuration attempted to create a `DataSource` and blocked the security/correlation shell from starting.
- **Fix:** Added `spring.autoconfigure.exclude` for `DataSourceAutoConfiguration` and `FlywayAutoConfiguration` in `application.yml`. This matches the plan boundary: persistence/runtime composition belongs to Plan 02-12, not this web-shell plan.
- **Files modified:** `pi-agent-adapter-web/src/main/resources/application.yml`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=SecurityAndCorrelationIntegrationTest`
- **Committed in:** `72fbbb0`

---

**Total deviations:** 1 auto-fixed (Rule 3 blocking)
**Impact on plan:** The fix enforces the intended plan boundary and avoids premature runtime/persistence bean composition.

## Issues Encountered

- Initial TDD RED failed as expected because `PiCloudServerApplication` and related security/correlation classes did not exist.
- First GREEN run failed because JDBC/Flyway auto-configuration tried to initialize a datasource before composition-root wiring exists; resolved via the Rule 3 auto-fix above.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=SecurityAndCorrelationIntegrationTest` — PASSED

## Self-Check: PASSED

- Found summary file: `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-09-SUMMARY.md`
- Found application file: `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/PiCloudServerApplication.java`
- Found task commit: `b6ddadc`
- Found task commit: `72fbbb0`

## Next Phase Readiness

- Plan 02-10 can add session-centric REST controllers on top of the authenticated `/api/**` shell and reuse `PiPrincipal`/correlation request attributes to build App `RequestContext`.
- Plan 02-12 must revisit datasource/Flyway exclusions when it owns the single Spring runtime composition root and concrete infrastructure bean activation.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
