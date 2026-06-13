---
phase: 01-runtime-spine-workspace-and-domain-contracts
plan: 05
subsystem: testing
tags: [java21, maven, archunit, junit, domain-contracts, runtime-events, testkit]

# Dependency graph
requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Runtime, workspace, session, event, and fake General Agent loop contracts from plans 01-04
provides:
  - Strengthened architecture dependency gate for Domain isolation from frameworks, SDKs, and outer COLA packages
  - RunEvent contract coverage for visibility, redaction metadata, stable wire names, and extension payloads
  - Fake GeneralAgentLoop scenario invariants for monotonic sequences and exactly one terminal event last
  - Human-readable downstream contract index with explicit Phase 1 deferred non-goals
affects: [phase-02-cloud-server, phase-03-model-provider, phase-04-tool-gateway, phase-05-web-console, phase-06-extensions, phase-07-mcp, phase-08-plugins]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - ArchUnit guards for framework-free Domain and COLA package boundaries
    - Event contract assertions for stable wire names, redaction metadata, and extension payload path
    - Testkit helper assertions for fake loop sequence, terminal event, visibility, and redaction invariants

key-files:
  created:
    - docs/phase-01-domain-contracts.md
  modified:
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/event/RunEventContractTest.java
    - pi-testkit/src/test/java/io/github/pi_java/agent/testkit/runtime/FakeGeneralAgentLoopTest.java

key-decisions:
  - "Use Java 21 explicitly for Maven verification in this environment because the system Maven launcher defaults to Java 17."
  - "Keep Phase 1 contract documentation as a downstream boundary document that names deferred provider, persistence, UI, MCP, plugin, shell, filesystem, and durable execution scopes."

patterns-established:
  - "Contract docs name exact Domain/testkit classes and deferred scopes to prevent later phases from pulling implementation concerns into Phase 1."
  - "Fake loop tests assert shared event-envelope invariants in every scenario, not only the happy path."

requirements-completed: [CORE-04, CORE-05, CORE-06, CORE-09, OPS-06]

# Metrics
duration: 4m 06s
completed: 2026-06-13
---

# Phase 01 Plan 05: Harden Verification and Contract Documentation Summary

**Architecture and event invariant test gates plus a downstream Phase 1 contract index for runtime, session, workspace, event, and testkit consumers**

## Performance

- **Duration:** 4m 06s
- **Started:** 2026-06-13T19:06:53Z
- **Completed:** 2026-06-13T19:10:59Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Strengthened the Domain architecture test to reject dependencies on outer COLA packages in addition to Spring, Jakarta, Jackson annotations, DB, Vaadin, PF4J, MCP, provider SDK, and related framework packages.
- Expanded `RunEventContractTest` to cover visibility/redaction metadata, stable key `wireName()` values, and the explicit `ExtensionPayload` path.
- Added `FakeGeneralAgentLoopTest` helper assertions named `assertMonotonicSequences`, `assertExactlyOneTerminalEventLast`, and `assertEveryEventHasVisibilityAndRedaction`, and applied them across fake loop scenarios.
- Created `docs/phase-01-domain-contracts.md` with module, runtime, session tree, workspace, event envelope, testkit, verification, and deferred-scope sections.
- Ran the final Phase 1 Maven gates successfully with Java 21: `mvn -q test` and `mvn test`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Strengthen architecture and event invariants** - `9f26d1c` (test)
2. **Task 2: Write downstream domain contract index** - `c9db3d9` (docs)
3. **Task 3: Run final Phase 1 verification gate** - `d499255` (test, empty verification marker commit)

**Plan metadata:** pending final metadata commit

## Files Created/Modified

- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/architecture/DomainDependencyArchTest.java` - Extends Domain forbidden dependency checks to project outer packages.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/event/RunEventContractTest.java` - Verifies event envelope visibility/redaction, stable wire names, redaction fields, and extension payloads.
- `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/runtime/FakeGeneralAgentLoopTest.java` - Applies reusable fake loop invariant helpers to all scenarios.
- `docs/phase-01-domain-contracts.md` - Documents Phase 1 public contracts and explicit deferred non-goals for downstream phases.

## Decisions Made

- Use `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` when running Maven in this environment because `mvn -v` defaults to Java 17 even though Java 21 is installed.
- Keep the downstream contract index focused on public contracts and explicit non-goals rather than implementation details for future REST/SSE, persistence, provider, tool, MCP, plugin, and UI phases.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Maven launcher defaulted to Java 17**
- **Found during:** Task 1 (Strengthen architecture and event invariants)
- **Issue:** `mvn -q test` failed with `release version 21 not supported` because Maven was running under Java 17 while the project targets Java 21.
- **Fix:** Verified Java 21 was installed and ran plan verification commands with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
- **Files modified:** None.
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test` and `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` both passed.
- **Committed in:** Not applicable; environment-only execution fix.

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Verification semantics are unchanged; Java 21 was required by the project and already installed.

## Issues Encountered

- The `rg` command was unavailable during documentation verification, so file content checks were performed with the repository content search tool instead.
- Task 3 required no source change after the final gates passed; an empty `--no-verify` verification marker commit was created to preserve the per-task atomic commit requirement for this parallel executor run.

## Known Stubs

None found in files created or modified by this plan.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` — passed.
- `pi-agent-domain/pom.xml` contains none of `spring`, `jakarta`, `jackson`, `vaadin`, `pf4j`, `mcp`, `jdbc`, or `openai`.

## Next Phase Readiness

- Phase 1 is ready for `/gsd-verify-work` with a green Maven gate and documented public contracts.
- Phase 2 can consume `RunEvent`, `AgentRuntime`, `SessionContextResolver`, `WorkspaceGateway`, and testkit fakes without introducing provider, persistence, UI, MCP, PF4J, shell, or real filesystem scope into Domain.

## Self-Check: PASSED

- Found `docs/phase-01-domain-contracts.md`.
- Found `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-05-SUMMARY.md`.
- Found modified test files for architecture, event contracts, and fake loop invariants.
- Found task commits `9f26d1c`, `c9db3d9`, and `d499255` in git history.

---
*Phase: 01-runtime-spine-workspace-and-domain-contracts*
*Completed: 2026-06-13*
