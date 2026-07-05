---
phase: 21-verification-security-and-regression-hardening
plan: 03
subsystem: testing
tags: [archunit, cola, provider-neutral, client-contracts, app-boundary, domain-boundary]

requires:
  - phase: 19-multi-turn-runtime-context
    provides: Provider-neutral App context assembly and OpenAI infrastructure isolation decisions.
  - phase: 20-provider-model-and-local-profile-stability
    provides: Provider/model selector handoff documentation and safe provider metadata boundaries.
provides:
  - Client DTO/API ArchUnit gate forbidding Vaadin, Spring AI, SQLite/JDBC, adapter, and infrastructure leaks.
  - Broadened App and Domain ArchUnit gates explicitly blocking SQL/SQLite/Spring AI/OpenAI infrastructure dependencies.
  - VER-03 documentation with the complete architecture gate command and forbidden dependency families.
affects: [phase-21-verification-hardening, cola-boundaries, provider-neutral-contracts]

tech-stack:
  added: [ArchUnit dependency in pi-agent-client]
  patterns: [forbidden package ArchUnit gates, provider-specific types isolated to infrastructure]

key-files:
  created:
    - pi-agent-client/src/test/java/io/github/pi_java/agent/client/architecture/ClientDependencyArchTest.java
    - docs/phase-21-verification-hardening.md
  modified:
    - pi-agent-client/pom.xml
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java
    - docs/phase-20-provider-model-local-profile.md

key-decisions:
  - "VER-03 uses explicit no-dependency ArchUnit rules in Client, App, and Domain so broad allowed package rules cannot permit SQL/JDBC, UI, provider SDK, adapter, or infrastructure leakage."
  - "OpenAI/Spring AI provider-specific contracts remain infrastructure-only and are blocked from client DTO/API, App orchestration, and Domain/runtime contracts."

patterns-established:
  - "Client contracts get their own ArchUnit gate alongside App and Domain rather than relying on downstream module tests."
  - "Documentation for release gates records exact Maven commands and forbidden dependency families."

requirements-completed: [VER-03]

duration: 4m29s
completed: 2026-07-05
---

# Phase 21 Plan 03: Architecture Boundary Hardening Summary

**Client, App, and Domain ArchUnit gates now reject Vaadin, Spring AI, SQLite/JDBC, provider/OpenAI infrastructure, adapter, and infrastructure implementation leaks.**

## Performance

- **Duration:** 4m29s
- **Started:** 2026-07-05T06:43:26Z
- **Completed:** 2026-07-05T06:47:55Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments

- Added a Client-module ArchUnit boundary test and ArchUnit test dependency so public DTO/API contracts are independently checked.
- Broadened App and Domain architecture tests to explicitly reject SQL/SQLite, Vaadin, Spring AI, provider SDK, OpenAI infrastructure, adapter, and infrastructure implementation dependencies.
- Corrected the Phase 20 selector handoff documentation from `current|next-run` to `future-runs|next-run` and documented the Phase 21 VER-03 gate command.

## Verification

All planned gates passed:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-client -am -Dtest=ClientDependencyArchTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-domain,pi-agent-app -am -Dtest='*ArchTest' test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-domain,pi-agent-app,pi-agent-client -am -Dtest='*ArchTest,*ArchitectureTest' test
```

## Task Commits

Each task was committed atomically:

1. **Task 1: Add client boundary ArchUnit test** - `116d57e` (test)
2. **Task 2: Broaden App/Domain architecture forbidden-type coverage** - `8e49c80` (test)
3. **Task 3: Fix selector handoff documentation and record architecture gates** - `8d4fef2` (docs)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-client/pom.xml` - Added the managed `archunit-junit5` test dependency.
- `pi-agent-client/src/test/java/io/github/pi_java/agent/client/architecture/ClientDependencyArchTest.java` - New Client DTO/API boundary ArchUnit gate.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java` - Added an explicit App production no-dependency rule for UI/persistence/provider/infra leaks.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java` - Added explicit SQLite and OpenAI infrastructure forbidden packages.
- `docs/phase-20-provider-model-local-profile.md` - Corrected model selection scope selector documentation.
- `docs/phase-21-verification-hardening.md` - Created Phase 21 hardening documentation with the VER-03 gate command and forbidden families.

## Decisions Made

- VER-03 uses explicit no-dependency ArchUnit rules in Client, App, and Domain so broad allowed package rules cannot permit SQL/JDBC, UI, provider SDK, adapter, or infrastructure leakage.
- OpenAI/Spring AI provider-specific contracts remain infrastructure-only and are blocked from client DTO/API, App orchestration, and Domain/runtime contracts.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None found in files created or modified by this plan. Stub-pattern scan only found unrelated historical docs outside this plan's modified files.

## Issues Encountered

- `docs/phase-21-verification-hardening.md` did not exist at task start; creating it was planned by Task 3.
- The working tree contained pre-existing parallel-executor changes (`.gitignore`, `.planning/STATE.md`, `.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md`, and untracked Phase 21 planning artifacts). They were left untouched and were not included in task commits.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- VER-03 is satisfied by focused ArchUnit coverage for Domain, App, and Client boundaries.
- Future Phase 21 plans can reference `docs/phase-21-verification-hardening.md` for the exact no-key architecture gate.

## Self-Check: PASSED

- Verified created files exist: summary, `ClientDependencyArchTest.java`, and `docs/phase-21-verification-hardening.md`.
- Verified task commits exist: `116d57e`, `8e49c80`, and `8d4fef2`.

---
*Phase: 21-verification-security-and-regression-hardening*
*Completed: 2026-07-05*
