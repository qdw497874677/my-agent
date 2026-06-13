---
phase: 01-runtime-spine-workspace-and-domain-contracts
plan: 03
subsystem: workspace-session-domain-contracts
tags: [java21, domain, workspace, artifact, session-tree, ports, tdd]

requires:
  - 01-01 Maven/COLA skeleton and architecture gates
provides:
  - First-class Workspace, WorkspaceSession, WorkspaceScope, WorkspaceSnapshot, Resource, and Mount contracts
  - WorkspaceGateway and CommandExecutionGateway domain ports without host filesystem or shell implementation
  - Artifact, Attachment, and ExternalReference domain value types independent from messages
  - Append-only Session tree entries with current leaf context reconstruction
affects: [pi-agent-domain, workspace-boundary, session-context, artifact-model]

tech-stack:
  added: []
  patterns: [JDK-only domain records, sealed session payload hierarchy, domain gateway ports, root-to-leaf context reconstruction]

key-files:
  created:
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/Workspace.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceSession.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceScope.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceSnapshot.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/Resource.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/Mount.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceGateway.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/CommandExecutionGateway.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/artifact/Artifact.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/artifact/Attachment.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/artifact/ExternalReference.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/Session.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionEntry.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionEntryPayload.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionContext.java
    - pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionContextResolver.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/workspace/WorkspaceContractsTest.java
    - pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/session/SessionTreeContextResolverTest.java
    - .planning/phases/01-runtime-spine-workspace-and-domain-contracts/deferred-items.md
  modified: []

key-decisions:
  - "Model Workspace as a logical runtime boundary using resource and mount IDs instead of host filesystem paths."
  - "Keep command execution in Domain as a request/result port only; no shell or process implementation belongs in Phase 1 Domain."
  - "Represent session history as an append-only tree with current leaf reconstruction and separated non-message context lists."

patterns-established:
  - "WorkspaceScope carries tenant/user/session/run/workspace IDs plus allowed resource and mount IDs."
  - "WorkspaceSnapshot exposes fingerprint, driftMetadata, replaySafe, and createdAt metadata for future restore/replay gates."
  - "SessionContextResolver walks parentEntryId from currentLeafEntryId to root, reverses the path, and accumulates typed context."

requirements-completed: [CORE-02, CORE-08, WORK-01, WORK-02, WORK-04, WORK-05]

duration: 6m 34s
completed: 2026-06-13
---

# Phase 01 Plan 03: Workspace, Artifact, and Append-only Session Contracts Summary

**Workspace/resource boundaries and append-only session context reconstruction implemented as Spring-free Domain contracts.**

## Performance

- **Duration:** 6m 34s
- **Started:** 2026-06-13T18:46:23Z
- **Completed:** 2026-06-13T18:52:57Z
- **Tasks:** 3
- **Files modified:** 19 implementation/test/deferred-tracking files, plus this summary and planning metadata

## Accomplishments

- Added logical Workspace contracts that model tenant/run-scoped boundaries without `java.io.File`, `java.nio.file.Path`, shell, cwd, or host filesystem assumptions.
- Added snapshot metadata contracts with stable IDs, fingerprint, drift metadata, replay-safety marker, and creation timestamp.
- Added Domain ports for workspace lifecycle/snapshot/resource lookup and command execution request/result shape; no real shell execution is implemented.
- Added Artifact, Attachment, and ExternalReference value types as independent work products, not message fields.
- Added append-only Session and SessionEntry contracts with `currentLeafEntryId`, `parentEntryId`, typed sealed payloads, compaction/branch summary payloads, and current leaf context reconstruction.
- Added contract tests for workspace scope/snapshot/gateway semantics and session tree context separation.

## Task Commits

1. **Task 1 RED: Workspace contract tests** — `ec8cdfd` (test)
2. **Task 1 GREEN: Workspace boundary contracts** — `9a065c8` (feat)
3. **Task 2 RED: Workspace gateway tests** — `9c5c35d` (test)
4. **Task 2 GREEN: Workspace gateway ports** — `3eaf611` (feat)
5. **Task 3 RED: Session context resolver tests** — `5dfed05` (test)
6. **Task 3 GREEN: Session tree and artifact contracts** — `3689ed0` (feat)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/Workspace.java` — logical workspace aggregate boundary.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceSession.java` — run-scoped workspace session contract.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceScope.java` — tenant/user/session/run/workspace and allowed resource/mount scope.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceSnapshot.java` — snapshot metadata with fingerprint/drift/replay safety.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/Resource.java` — logical resource descriptor.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/Mount.java` — resource-to-logical-path mount descriptor.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceGateway.java` — workspace lifecycle/snapshot/resource Domain port.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/CommandExecutionGateway.java` — command request/result Domain port with timeout and cancellation token.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/artifact/Artifact.java` — independent artifact work product.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/artifact/Attachment.java` — independent attachment work product.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/artifact/ExternalReference.java` — external reference value type.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/Session.java` — append-only entry collection with current leaf pointer.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionEntry.java` — stable session tree entry with `parentEntryId`.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionEntryPayload.java` — sealed typed payload hierarchy.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionContext.java` — separated reconstructed active context.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionContextResolver.java` — current leaf path resolver.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/workspace/WorkspaceContractsTest.java` — workspace/snapshot/gateway contract tests.
- `pi-agent-domain/src/test/java/io/github/pi_java/agent/domain/session/SessionTreeContextResolverTest.java` — session context reconstruction tests.
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/deferred-items.md` — parallel execution observation log.

## Decisions Made

- Modeled Workspace as a logical resource boundary with `Resource` and `Mount` instead of any host filesystem path abstraction.
- Kept `CommandExecutionGateway` as a pure Domain port containing command args, environment map, timeout, and cancellation token ID, with no `ProcessBuilder` or shell coupling.
- Used a sealed `SessionEntryPayload` hierarchy so messages, artifacts, attachments, external references, memory references, workspace scope, compaction summaries, and branch summaries are explicit typed entries.
- Implemented context reconstruction from `currentLeafEntryId` by leaf-to-root traversal over `parentEntryId`, then root-to-leaf accumulation.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Avoided parallel Plan 02 ownership conflict during verification**
- **Found during:** Task 1/Task 3 verification
- **Issue:** While this parallel executor was running, untracked/partially written Plan 02 files existed in the same module and initially blocked full test compilation.
- **Fix:** Did not edit or stage the out-of-scope Plan 02 files. Documented the transient condition in `deferred-items.md`, continued with Plan 01-03-owned files, and re-ran verification after the owning executor completed its work.
- **Files modified:** `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/deferred-items.md`
- **Commit:** `3689ed0`

---

**Total deviations:** 1 auto-fixed blocking/process issue.
**Impact on plan:** No architecture or product scope changed; final full verification passed.

## Issues Encountered

- Parallel execution interleaved commits from Plan 01-02 with Plan 01-03. This did not change Plan 01-03 implementation scope, but required careful staging of only Plan 01-03 files.

## User Setup Required

None.

## Known Stubs

None - all introduced production contracts are concrete immutable records/interfaces/domain services. No UI data placeholders or mock-only production paths were introduced.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH mvn -q -pl pi-agent-domain test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH mvn test` — passed.
- Workspace source scan for `java.io.File`, `java.nio.file.Path`, and `ProcessBuilder` — no matches.
- Production source scan for `TODO`, `FIXME`, `placeholder`, `coming soon`, and `not available` — no matches.

## Self-Check: PASSED

- Found required file: `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceGateway.java`.
- Found required file: `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/CommandExecutionGateway.java`.
- Found required file: `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/SessionContextResolver.java`.
- Found required file: `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-03-SUMMARY.md`.
- Found task commits in `git log --oneline --all`: `ec8cdfd`, `9a065c8`, `9c5c35d`, `3eaf611`, `5dfed05`, `3689ed0`.
- Note: initial self-check attempted to use `rg`, which is unavailable in the container; verification was rerun with `git log --oneline --all`.

## Next Phase Readiness

- Plan 01-04 can consume `WorkspaceGateway`, `CommandExecutionGateway`, `Session`, `SessionContextResolver`, and artifact/session contracts for fake runtime/testkit loop implementation.
- Phase 2 persistence/API work can map sessions and workspace snapshots to durable tables/read models without changing Domain contracts.

---
*Phase: 01-runtime-spine-workspace-and-domain-contracts*
*Completed: 2026-06-13*
