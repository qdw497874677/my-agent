---
phase: 19-multi-turn-runtime-context
plan: 04
subsystem: testing
tags: [java, junit, archunit, testkit, multi-turn-context, safety]

# Dependency graph
requires:
  - phase: 19-multi-turn-runtime-context-01
    provides: App-layer conversation context policy and assembler contracts
  - phase: 19-multi-turn-runtime-context-02
    provides: Runtime dispatcher context assembly wiring and observability metadata
  - phase: 19-multi-turn-runtime-context-03
    provides: Provider-boundary chat-message mapping that appends current prompt exactly once
provides:
  - Fake streaming and non-streaming model clients expose captured ModelRequest history
  - No-key fake-model semantic proof for ordered prior turns and exactly-once current prompt
  - Strengthened App ArchUnit boundary guard for Phase 19 context classes
  - Safety coverage for tool/error/provider/audit/status/visibility/redaction/credential-like exclusions
affects: [phase-19-verification, phase-20-provider-local-profile, phase-21-regression-hardening]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Testkit fake clients retain immutable snapshots of captured ModelRequest calls through Optional/List accessors
    - App-layer context safety drops unsafe history instead of redacting placeholders into model prompts
    - ArchUnit explicitly pins context assembly classes away from outer layers and provider SDKs

key-files:
  created:
    - pi-testkit/src/test/java/io/github/pi_java/agent/testkit/FakeModelContextCaptureTest.java
  modified:
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeModelClient.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationContextAssemblerTest.java

key-decisions:
  - "Keep fake model request capture as testkit-only accessors without changing production ModelClient or StreamingModelClient contracts."
  - "Treat credential-like transcript metadata as model-context sensitive even when visible text is otherwise safe."
  - "Leave Domain architecture gate unchanged because the existing broad rule already rejects Spring AI, OpenAI, Vaadin, App, Infra, Adapter, and persistence dependencies."

patterns-established:
  - "Fake ModelRequest capture: fake clients capture before scripted actions execute and expose lastRequest()/requests() for semantic assertions."
  - "Context safety exclusions: unsafe text or metadata causes full message exclusion before model prompt construction."
  - "Phase 19 App ArchUnit guard: ConversationContextAssembler/Policy/Metadata are explicitly covered by no-outer-layer dependency checks."

requirements-completed: [CTX-03, CTX-04, CTX-05]

# Metrics
duration: 7m21s
completed: 2026-07-01
---

# Phase 19 Plan 04: Final Fake-Model Capture and Context Safety Gates Summary

**No-key fake model request capture with semantic multi-turn proof, credential-safe context filtering, and ArchUnit boundaries for Phase 19 context assembly**

## Performance

- **Duration:** 7m21s
- **Started:** 2026-07-01T14:33:12Z
- **Completed:** 2026-07-01T14:40:33Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added `lastRequest()` and `requests()` capture APIs to both fake model clients so tests can inspect actual `ModelRequest.context().sessionContext().messages()` without real provider keys.
- Added `FakeModelContextCaptureTest` proving prior user/assistant messages are ordered before the current `RunInput.ChatInput` and that the current prompt appears exactly once across model-visible semantics.
- Strengthened App ArchUnit coverage so `ConversationContextAssembler`, `ConversationContextPolicy`, and `ConversationContextMetadata` are explicitly barred from Vaadin, Spring, JDBC/SQLite, infrastructure, adapter, Spring AI, OpenAI SDK, MCP SDK, LangChain4j, AWS SDK, and provider-package dependencies.
- Hardened context safety filtering to exclude credential-like metadata and added tests covering unsafe roles, failed/cancelled statuses, invisible/redacted messages, tool/error/provider/audit-like items, sensitive text, sensitive metadata, current-run exclusion, and metadata counts.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add fake model request capture and CTX-04 semantic proof** - `6986f45` (feat)
2. **Task 2: Strengthen safety and architecture gates for context boundaries** - `c18f170` (fix)

**Plan metadata:** pending final docs commit

_Note: The plan marked both tasks as TDD, but implementation used focused test-first iteration within the task and committed each task atomically after verification._

## Files Created/Modified

- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java` - Captures every streaming `ModelRequest` before scripted stream actions execute and exposes `lastRequest()`/`requests()`.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeModelClient.java` - Captures every non-streaming `ModelRequest` before scripted responses execute and exposes `lastRequest()`/`requests()`.
- `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/FakeModelContextCaptureTest.java` - No-key semantic proof for prior-turn ordering and exactly-once current prompt in both streaming and non-streaming fake-runtime paths.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java` - Excludes credential-like metadata before producing model-visible `SessionEntryPayload.MessageEntry` history.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java` - Explicitly covers Phase 19 context classes in no-outer-layer/provider dependency rules.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationContextAssemblerTest.java` - Adds comprehensive context safety assertions and metadata count checks.

## Decisions Made

- Kept fake capture APIs in `pi-testkit` concrete fake classes rather than production model interfaces, preserving provider-neutral Domain contracts.
- Treated credential-like metadata as sensitive enough to exclude the full historical item, matching the Phase 19 rule to drop unsafe history instead of adding redaction placeholders to model context.
- Did not modify `DomainDependencyArchTest`; its existing broad rule already rejects the outer layers and provider SDKs relevant to Phase 19 without weakening Domain boundaries.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Excluded credential-like transcript metadata before model context**
- **Found during:** Task 2 (Strengthen safety and architecture gates for context boundaries)
- **Issue:** The assembler excluded sensitive visible text and diagnostic metadata, but safe-looking user/assistant text with sensitive metadata such as `authorization: Bearer ...` could still be included in model context.
- **Fix:** Added sensitive metadata marker filtering and a safety test proving such items are excluded before model context construction.
- **Files modified:** `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java`, `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/ConversationContextAssemblerTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app,pi-agent-domain -Dtest=ConversationContextAssemblerTest,AppDependencyArchTest,DomainDependencyArchTest test`
- **Committed in:** `c18f170`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** The auto-fix is security/correctness hardening directly required by CTX-03/CTX-05 and does not add product scope.

## Issues Encountered

- Initial focused test compile used the wrong `AgentId` import; corrected to `io.github.pi_java.agent.domain.common.PlatformIds.AgentId`.
- Initial focused test used a non-W3C trace id; corrected test fixture to a 32-character lowercase hex trace id.
- Unrelated pre-existing working tree items were observed and left untouched: `.gitignore` modification and `.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md` untracked.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-testkit -Dtest=FakeModelContextCaptureTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app,pi-agent-domain -Dtest=ConversationContextAssemblerTest,AppDependencyArchTest,DomainDependencyArchTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app,pi-agent-domain,pi-testkit -Dtest=ConversationContextAssemblerTest,AppDependencyArchTest,DomainDependencyArchTest,FakeModelContextCaptureTest test` — passed.

## Known Stubs

None. Stub scan only found intentional null/empty-list handling in testkit/runtime and assembler guard code, not UI-flowing placeholder or mock data.

## Next Phase Readiness

- Phase 19 now has no-key proof that assembled prior context reaches fake model requests and that current prompts are not duplicated.
- Context safety and architecture gates are ready for Phase 20 provider/local-profile stability and Phase 21 broader regression/security hardening.
- No blockers remain for Phase 19 completion.

## Self-Check: PASSED

- Found summary file: `.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-04-SUMMARY.md`
- Found created semantic proof test: `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/FakeModelContextCaptureTest.java`
- Found task commit: `6986f45`
- Found task commit: `c18f170`

---
*Phase: 19-multi-turn-runtime-context*
*Completed: 2026-07-01*
