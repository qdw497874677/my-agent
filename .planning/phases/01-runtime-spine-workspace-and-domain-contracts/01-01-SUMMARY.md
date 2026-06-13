---
phase: 01-runtime-spine-workspace-and-domain-contracts
plan: 01
subsystem: runtime-foundation
tags: [java21, maven, cola, archunit, junit, assertj]

requires: []
provides:
  - Java 21 Maven parent project with COLA module skeleton
  - Spring-free Domain module dependency baseline
  - Executable ArchUnit boundary gates for Domain and App layers
  - Empty source trees for all planned Phase 1 modules
affects: [phase-01, runtime-core, workspace-contracts, app-layer, testkit]

tech-stack:
  added: [Maven, Java 21, JUnit Jupiter 5.10.3, AssertJ 3.26.3, ArchUnit 1.3.0]
  patterns: [COLA module split, dependencyManagement, ArchUnit boundary tests, generated Maven output ignore]

key-files:
  created:
    - pom.xml
    - pi-agent-client/pom.xml
    - pi-agent-domain/pom.xml
    - pi-agent-app/pom.xml
    - pi-agent-infrastructure/pom.xml
    - pi-agent-adapter-web/pom.xml
    - pi-testkit/pom.xml
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java
    - .gitignore
  modified: []

key-decisions:
  - "Use a Maven parent with explicit dependency/plugin management and no framework BOM in Phase 1 foundation."
  - "Keep pi-agent-domain production dependencies empty while allowing only test-scoped JUnit, AssertJ, and ArchUnit."
  - "Codify COLA dependency direction immediately with ArchUnit before domain implementation starts."

patterns-established:
  - "COLA modules are split into client, domain, app, infrastructure, adapter-web, and testkit."
  - "Domain dependency pollution is guarded by an ArchUnit deny-list for Spring, Jakarta, Jackson annotations, DB, UI, plugin, MCP, model SDK, and provider SDK packages."
  - "App layer may depend only on Java/test libraries plus app, domain, and client packages."

requirements-completed: [CORE-06, CORE-09]

duration: 11m 46s
completed: 2026-06-13
---

# Phase 01 Plan 01: Create Java 21 Maven/COLA Skeleton and Architecture Gates Summary

**Java 21 Maven COLA skeleton with Spring-free Domain dependencies and executable ArchUnit boundary gates.**

## Performance

- **Duration:** 11m 46s
- **Started:** 2026-06-13T18:29:35Z
- **Completed:** 2026-06-13T18:41:21Z
- **Tasks:** 2
- **Files modified:** 16 implementation files, plus this summary and planning metadata

## Accomplishments

- Created the Java 21 Maven multi-module parent with modules exactly matching the Phase 1 COLA foundation: client, domain, app, infrastructure, adapter-web, and testkit.
- Added module POM dependencies that keep Domain production-code framework-free and connect App/Infrastructure/Adapter/Testkit to their planned inner-layer dependencies.
- Added executable ArchUnit tests guarding Domain from outer frameworks/SDKs and App from dependency direction drift.
- Preserved empty main Java source trees for all modules so later plans have stable source roots.

## Task Commits

Each task was committed atomically, with small follow-up commits for generated-output hygiene and empty source-tree preservation:

1. **Task 1: Create Java 21 Maven multi-module skeleton** - `ec31747` (feat)
2. **Task 2: Add executable COLA architecture gates** - `86dea57` (test)
3. **Generated-output hygiene** - `0fa2257` (chore)
4. **Empty source tree preservation** - `3fb22d2` (chore)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pom.xml` - Maven parent POM with Java 21 release, UTF-8 encodings, module list, dependency management, compiler plugin, and surefire plugin.
- `pi-agent-client/pom.xml` - Client contracts module POM.
- `pi-agent-domain/pom.xml` - Domain module POM with only test-scoped JUnit, AssertJ, and ArchUnit dependencies.
- `pi-agent-app/pom.xml` - App layer POM depending on Domain and Client plus test libraries.
- `pi-agent-infrastructure/pom.xml` - Infrastructure module POM depending on Domain.
- `pi-agent-adapter-web/pom.xml` - Web Adapter module POM depending on App and Client.
- `pi-testkit/pom.xml` - Testkit module POM depending on Domain plus test libraries.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java` - ArchUnit guard blocking outer framework, DB, provider, UI, plugin, MCP, and annotation dependencies in Domain.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java` - ArchUnit guard limiting App dependencies to Java/test libraries, App, Domain, and Client packages.
- `.gitignore` - Ignores Maven `target/` outputs created by verification.
- `*/src/main/java/.gitkeep` - Preserves empty main Java source trees for all six modules.

## Decisions Made

- Used a framework-neutral Maven parent rather than Spring Boot dependency management because Phase 1 must keep the runtime/domain foundation independent of concrete adapters.
- Kept `pi-agent-domain` free of production dependencies; test dependencies are scoped to architecture verification only.
- Added `.gitignore` for Maven output after test execution so generated `target/` directories remain untracked.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Installed Java 21 and repaired Maven verification environment**
- **Found during:** Task 2 verification
- **Issue:** The container defaulted Maven to Java 17, so Java 21 compilation failed with `release version 21 not supported`.
- **Fix:** Installed OpenJDK 21 and ran verification with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` and Java 21 on `PATH`.
- **Files modified:** None in repo.
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH mvn -q test` exited 0.
- **Committed in:** N/A (environment-only fix)

**2. [Rule 3 - Blocking] Fixed transient DNS for Maven dependency resolution**
- **Found during:** Task 2 verification
- **Issue:** Maven could not resolve `repo.maven.apache.org` after Java installation changed resolver-related packages.
- **Fix:** Updated container DNS to resolve Maven Central and re-ran dependency resolution.
- **Files modified:** None in repo.
- **Verification:** Maven downloaded missing surefire provider dependency and `mvn test` completed successfully under Java 21.
- **Committed in:** N/A (environment-only fix)

**3. [Rule 3 - Blocking] Added generated Maven output ignore rule**
- **Found during:** Post-task untracked file check
- **Issue:** Maven verification generated module `target/` directories that were untracked.
- **Fix:** Added `.gitignore` entries for root and module `target/` directories.
- **Files modified:** `.gitignore`
- **Verification:** `git status --short` no longer reports `target/` directories.
- **Committed in:** `0fa2257`

---

**Total deviations:** 3 auto-fixed (3 blocking)
**Impact on plan:** All fixes were necessary to verify or keep the working tree clean. No product scope was added.

## Issues Encountered

- Maven initially passed before tests existed, then failed once ArchUnit tests required Java 21 compilation under a Java 17 default runtime. Verification now passes when Java 21 is selected explicitly.
- Maven Central DNS resolution temporarily failed after package installation; the container resolver was adjusted and dependency download succeeded.

## User Setup Required

None - no external service configuration required. Developers running the project locally need Java 21 selected for Maven.

## Known Stubs

None - no placeholder production behavior or UI data stubs were introduced. Empty source trees are intentional skeleton scaffolding for later Phase 1 plans.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH mvn -q test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH mvn test` — passed.

## Self-Check: PASSED

- Found created file: `pom.xml`.
- Found created file: `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java`.
- Found created file: `pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java`.
- Found task commits: `ec31747`, `86dea57`, `0fa2257`, `3fb22d2`.

## Next Phase Readiness

- Plan 01-02 can add runtime state, error, AgentDefinition, RunInput, and RunEvent contracts into the Spring-free Domain module.
- Future plans should keep using Java 21 for Maven commands until the execution image defaults to Java 21.

---
*Phase: 01-runtime-spine-workspace-and-domain-contracts*
*Completed: 2026-06-13*
