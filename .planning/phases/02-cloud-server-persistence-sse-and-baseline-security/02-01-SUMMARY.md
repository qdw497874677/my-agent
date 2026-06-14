---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 01
subsystem: infra
tags: [maven, spring-boot, spring-security, jdbc, flyway, postgresql, testcontainers, cola]

# Dependency graph
requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Framework-free Domain/App boundaries and Java 21 Maven module skeleton
provides:
  - Spring Boot 3.5.9 dependency management for outer cloud-server modules
  - Testcontainers 2.0.3 dependency management for future PostgreSQL integration gates
  - JDBC/Flyway/PostgreSQL dependency foundation in Infrastructure
  - Spring MVC/Security/Actuator/JWT-ready dependency foundation in Web Adapter
affects: [phase-02, cloud-server, persistence, sse, security, e2e]

# Tech tracking
tech-stack:
  added: [Spring Boot 3.5.9 BOM, Testcontainers 2.0.3 BOM, Spring MVC, Spring Security, Actuator, OAuth2 Resource Server, Spring JDBC, PostgreSQL JDBC, Flyway]
  patterns: [COLA outer-module dependencies only, Domain dependency quarantine, Docker-dependent checks deferred to Docker-enabled gates]

key-files:
  created: []
  modified:
    - pom.xml
    - pi-agent-client/pom.xml
    - pi-agent-infrastructure/pom.xml
    - pi-agent-adapter-web/pom.xml

key-decisions:
  - "Import Spring Boot and Testcontainers through root dependencyManagement while keeping module dependencies explicit."
  - "Add Spring/JDBC/Flyway/Security dependencies only to outer Client/Infrastructure/Adapter modules; Domain and App production code remain framework-isolated."

patterns-established:
  - "Phase 2 cloud dependencies are introduced at module boundaries before production Java code."
  - "Docker/Testcontainers capabilities are available in dependency management but non-container Java 21 verification remains the local gate for this plan."

requirements-completed: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-05, CLOUD-06, E2E-01, E2E-04, E2E-05]

# Metrics
duration: 3m 06s
completed: 2026-06-14
---

# Phase 02 Plan 01: Add Phase 2 Maven Dependency and Verification Foundation Summary

**Spring Boot/Testcontainers Maven foundation for Cloud Server, JDBC/Flyway/PostgreSQL persistence, JWT-ready Security, and explicit non-container Java 21 checks.**

## Performance

- **Duration:** 3m 06s
- **Started:** 2026-06-14T04:58:25Z
- **Completed:** 2026-06-14T05:01:31Z
- **Tasks:** 1
- **Files modified:** 4

## Accomplishments

- Added root dependency management for Spring Boot 3.5.9 and Testcontainers 2.0.3 so later Phase 2 modules share consistent versions.
- Added Infrastructure dependencies for App access, Spring JDBC, PostgreSQL JDBC, Flyway core, Flyway PostgreSQL database support, and Testcontainers PostgreSQL/JUnit gates.
- Added Web Adapter dependencies for Infrastructure composition, Spring MVC, Spring Security, Actuator, OAuth2 Resource Server/Jose, Spring Boot Test, Spring Security Test, and Testcontainers.
- Added Client test dependencies for JUnit Jupiter and AssertJ without introducing production framework dependencies.
- Verified Domain remains free of forbidden Spring/Jackson/Jakarta/JDBC/PostgreSQL/Flyway/Vaadin/PF4J/MCP dependency markers.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Phase 2 Maven dependency foundation** - `c10442f` (chore)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pom.xml` - Added `spring-boot.version`, `testcontainers.version`, Spring Boot BOM import, and Testcontainers BOM import.
- `pi-agent-client/pom.xml` - Added test-scoped JUnit Jupiter and AssertJ for upcoming client DTO contract tests.
- `pi-agent-infrastructure/pom.xml` - Added dependency on App plus JDBC, PostgreSQL, Flyway, and Testcontainers foundations.
- `pi-agent-adapter-web/pom.xml` - Added Infrastructure dependency and Spring MVC/Security/Actuator/JWT-ready/test dependencies.

## Decisions Made

- Imported Spring Boot and Testcontainers through root `dependencyManagement` so later Phase 2 plans can add module dependencies without pinning versions repeatedly.
- Kept Spring/JDBC/Flyway/Security dependencies out of Domain and App production dependencies, preserving Phase 1 COLA and framework-isolation constraints.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Local targeted Maven verification emitted SLF4J no-provider warnings only; tests still passed. No functional issue.
- Docker/Testcontainers execution was intentionally not run in this plan because no PostgreSQL integration tests exist yet and the plan's validation gate is non-container Maven verification.

## User Setup Required

None - no external service configuration required for this plan. Docker-enabled CI/local execution will be required by later Phase 2 PostgreSQL/Testcontainers plans.

## Known Stubs

None found in files created or modified by this plan.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-client,pi-agent-app -am` — passed.
- `pi-agent-domain/pom.xml` forbidden dependency marker scan for `spring-boot|jackson|jakarta|jdbc|postgresql|flyway|vaadin|pf4j|mcp` — no matches.

## Self-Check: PASSED

- Summary file exists: `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-01-SUMMARY.md`.
- Key modified file exists: `pom.xml`.
- Task commit exists: `c10442f`.

## Next Phase Readiness

- Plan 02-02 can now add client REST DTO contract tests using JUnit/AssertJ.
- Later Infrastructure and Web Adapter plans can add JDBC/Flyway/PostgreSQL/Testcontainers and Spring MVC/Security tests without additional dependency-management changes.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
