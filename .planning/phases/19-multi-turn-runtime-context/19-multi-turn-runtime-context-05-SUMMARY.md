---
phase: 19-multi-turn-runtime-context
plan: 05
subsystem: infrastructure-model-openai
tags: [java, openai-compatible, adapter-web, provider-boundary, multi-turn-context, gap-closure]

requires:
  - phase: 19-multi-turn-runtime-context-03
    provides: OpenAI-compatible ordered message-list provider boundary
  - phase: 19-multi-turn-runtime-context-04
    provides: fake-model context proof and Phase 19 safety/architecture gates
provides:
  - Public OpenAiChatMessage record usable by external OpenAiStreamSource implementations
  - Adapter-web fake OpenAI provider test configuration migrated to stream(List<OpenAiChatMessage>, CancellationToken)
  - Successful Phase 19 gap verification reactor gate for pi-agent-adapter-web with dependencies
affects: [phase-19-verification, phase-20-provider-stability, phase-21-regression-hardening]

tech-stack:
  added: []
  patterns:
    - Public infrastructure-local OpenAI chat message carrier for test/config stream source implementations
    - Message-list-only OpenAiStreamSource boundary with no restored string-prompt override path

key-files:
  created:
    - .planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-05-SUMMARY.md
  modified:
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java

key-decisions:
  - "Expose OpenAiChatMessage as a public infrastructure-local record because OpenAiStreamSource is public and external test/config modules implement it."
  - "Keep the adapter-web fake provider on the messages-based OpenAiStreamSource signature instead of reintroducing any string-prompt stream path."

patterns-established:
  - "External OpenAiStreamSource implementations import OpenAiChatMessage and implement stream(List<OpenAiChatMessage>, CancellationToken)."
  - "Single-module adapter-web compile may use stale installed dependencies; the authoritative gate for this migration is the reactor build with -am."

requirements-completed: [CTX-01, CTX-04, CTX-05]

duration: 3m37s
completed: 2026-07-03
---

# Phase 19 Plan 05: OpenAI Stream Boundary Gap Closure Summary

**OpenAI-compatible streaming now exposes a public message carrier and adapter-web fake providers compile against the ordered message-list API across the reactor.**

## Performance

- **Duration:** 3m37s
- **Started:** 2026-07-03T02:30:47Z
- **Completed:** 2026-07-03T02:34:24Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments

- Made `OpenAiChatMessage` a public immutable record with public constructor and public `user(...)` / `assistant(...)` factories so modules outside `pi-agent-infrastructure-model-openai` can implement the public `OpenAiStreamSource` interface.
- Updated `FakeOpenAiProviderE2EConfiguration.FakeOpenAiStreamSource` to override `stream(List<OpenAiChatMessage>, CancellationToken)`, eliminating the stale `stream(String, CancellationToken)` test implementation reported in `19-VERIFICATION.md`.
- Re-ran the exact Phase 19 gap verification gate with reactor dependencies; `pi-agent-adapter-web` now compiles against the current messages-based OpenAI stream API.

## Task Commits

Each task was committed atomically:

1. **Task 1: Make OpenAiStreamSource messages API externally implementable** - `9122793` (feat)
2. **Task 2: Update adapter-web fake OpenAI stream source to message-list signature** - `e276d47` (fix)
3. **Task 3: Run the exact Phase 19 gap verification gate and record summary** - pending final docs commit

**Plan metadata:** pending final docs commit

_Note: Tasks 1 and 2 were marked TDD. Task 1 preserved the existing focused OpenAI provider-boundary tests; Task 2 used the reactor compile failure as the RED condition because adapter-web could not compile until the fake stream source implemented the public message-list signature._

## Files Created/Modified

- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java` - Public provider-specific role/content record that remains isolated to the OpenAI infrastructure module but is now usable by external stream source implementors.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java` - Fake OpenAI stream source now imports `OpenAiChatMessage` and implements `stream(List<OpenAiChatMessage>, CancellationToken)` while preserving deterministic fake events.
- `.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-05-SUMMARY.md` - Gap-closure execution record and verification evidence.

## Decisions Made

- Exposed `OpenAiChatMessage` publicly without moving it into Domain/App/client contracts. This keeps provider-specific chat semantics inside infrastructure while making the public `OpenAiStreamSource` API implementable across modules.
- Treated `mvn -pl pi-agent-adapter-web -am -DskipTests test` as the authoritative compile gate because a non-reactor adapter-web compile can observe stale installed OpenAI classes and miss or misreport boundary drift.
- Kept the adapter-web fake deterministic and minimal; it accepts the role-preserving message list but does not assemble model context or restore a production string-prompt boundary in adapter-web.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -DskipTests test` initially failed after the adapter-web code update because it used stale installed OpenAI classes that did not yet expose public `OpenAiChatMessage`. The required reactor gate with `-am` rebuilt dependencies and passed. This matches the plan's warning that the exact Phase 19 gap must be proven with the reactor command.
- Maven emitted pre-existing deprecation warnings for `@MockBean` in adapter-web tests. These warnings are unrelated to the Phase 19 provider-boundary migration and were not changed.
- Pre-existing working-tree items were observed and left unstaged unless directly related to this plan: `.gitignore` and `.planning/STATE.md` were already modified, and some planning verification/plan files were untracked before this executor's code changes.

## Known Stubs

None. Stub scan found no TODO/FIXME/placeholder text or UI-flowing empty/mock data in the files modified by this plan. Null/blank checks reported in OpenAI infrastructure code are defensive guards, not stubs.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiCompatibleStreamingModelClientTest test` — passed; 7 tests run, 0 failures/errors.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -DskipTests test` — passed; all 14 reactor modules succeeded and adapter-web test compilation completed with tests skipped as requested.
- `stream(String` search over `pi-agent-infrastructure-model-openai` and `FakeOpenAiProviderE2EConfiguration.java` — no matches found.

## Next Phase Readiness

- The Phase 19 verification blocker is closed at codebase compile level: adapter-web fake provider composition now uses the current OpenAI messages API, and the message carrier in the public signature is externally implementable.
- Ready for Phase 19 re-verification and Phase 20 provider/model local profile stability planning.

## Self-Check: PASSED

- Found modified files: `OpenAiChatMessage.java` and `FakeOpenAiProviderE2EConfiguration.java`.
- Found summary file: `.planning/phases/19-multi-turn-runtime-context/19-multi-turn-runtime-context-05-SUMMARY.md`.
- Found task commits in git history: `9122793`, `e276d47`.

---
*Phase: 19-multi-turn-runtime-context*
*Completed: 2026-07-03*
