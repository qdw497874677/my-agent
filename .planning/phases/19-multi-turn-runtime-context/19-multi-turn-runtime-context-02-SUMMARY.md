---
phase: 19-multi-turn-runtime-context
plan: 02
subsystem: infrastructure-runtime-dispatch
tags: [java, infrastructure, app-layer-context, run-dispatcher, audit-metadata, cola, tdd]

requires:
  - phase: 19-multi-turn-runtime-context
    plan: 01
    provides: App-layer ConversationContextAssembler, ConversationContextPolicy, and ConversationContextMetadata
provides:
  - Dispatch-time ConversationContextAssembler invocation before AgentRuntime.start
  - RunContext.sessionContext.messages populated with bounded prior transcript messages
  - Safe worker-start audit metadata for context counts and truncation state
  - Cloud/local Spring composition wiring for assembler-enabled dispatch
affects: [phase-19-provider-message-boundary, phase-19-fake-model-proof, phase-21-regression-hardening]

tech-stack:
  added: []
  patterns: [app-assembler-invoked-from-infrastructure, safe-count-only-observability, spring-composition-root-wiring]

key-files:
  created:
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherContextTest.java
  modified:
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java

key-decisions:
  - "Invoke the App-layer ConversationContextAssembler from DefaultRunDispatcher after ownership RequestContext creation and before AgentRuntime.start."
  - "Keep current prompt sourcing in RunInput while SessionContext.messages carries assembler-produced prior messages only."
  - "Expose context observability as primitive count/truncation metadata on worker-start audit details, never as message text."
  - "Wire context assembly in Spring composition roots, including local/dev, instead of assembling context in Vaadin UI state."

patterns-established:
  - "DefaultRunDispatcher preserves backward-compatible constructors with an empty assembler while production composition supplies the real App assembler."
  - "Worker audit details use contextIncludedCount/contextDroppedCount/contextExcludedCount/contextMaxChars/contextResultChars/contextTruncated safe fields."

requirements-completed: [CTX-01, CTX-02, CTX-05]

duration: 20m18s
completed: 2026-07-01
---

# Phase 19 Plan 02: Runtime Dispatcher Context Injection Summary

**Queued run dispatch now injects bounded App-assembled prior conversation history into `RunContext.sessionContext().messages()` while recording safe context counts in worker audit metadata.**

## Performance

- **Duration:** 20m18s
- **Started:** 2026-07-01T14:07:57Z
- **Completed:** 2026-07-01T14:28:15Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added assembler-aware `DefaultRunDispatcher` constructors that accept `ConversationContextAssembler` and `ConversationContextPolicy` while preserving existing constructor compatibility through an empty assembler fallback.
- Changed dispatch flow so `DefaultRunDispatcher` creates `RequestContext`, validates the provider/model ref, assembles selected-session context, then builds `RunContext` with assembler-produced `SessionEntryPayload.MessageEntry` history before calling `AgentRuntime.start`.
- Proved with `DefaultRunDispatcherContextTest` that prior `user`/`assistant` history reaches runtime and the current prompt remains only in `RunInput.ChatInput`, not duplicated into `SessionContext.messages`.
- Added safe worker-start audit metadata fields: `contextIncludedCount`, `contextDroppedCount`, `contextExcludedCount`, `contextMaxChars`, `contextResultChars`, and `contextTruncated`.
- Wired `ConversationContextPolicy` and `ConversationContextAssembler` into cloud and local/dev Spring composition roots using `ConversationQueryService`/`DefaultConversationQueryService`; Vaadin component state does not assemble runtime context.

## Task Commits

Each task was committed atomically:

1. **Task 1: Inject assembler into DefaultRunDispatcher context creation** - `ff37013` (feat)
2. **Task 2: Record safe context metadata and wire local composition** - `d4da21b` (feat)

**Plan metadata:** pending final docs commit

_Note: Both tasks were executed with TDD-style RED/GREEN loops. Task 1 first failed because the dispatcher did not yet support the new assembler seam. Task 2 first failed because worker-start audit metadata still only exposed `workerId`._

## Files Created/Modified

- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` - Dispatch-time assembler injection, context-backed `SessionContext`, and safe worker-start metadata.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherContextTest.java` - Focused dispatcher tests for prior-history injection, current-prompt-once behavior, empty-history behavior, and safe metadata.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java` - Local profile composition for transcript assembler, conversation query service, context policy, and context assembler.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` - Cloud composition for context policy/assembler and assembler-enabled `DefaultRunDispatcher` construction.

## Decisions Made

- Kept the context business rules in the App-layer `ConversationContextAssembler`; Infrastructure only invokes it at dispatch time and carries the resulting Domain messages into `RunContext`.
- Preserved current input semantics by continuing to build `RunInput` from the queued run input and never adding the current run's transcript entry to `SessionContext.messages`.
- Used primitive, count-only audit details for context observability so worker projections/audit records are useful for debugging without exposing prior prompt text, assistant text, provider/tool payloads, or secrets.
- Added local/dev composition for the same App assembler path used in cloud composition, avoiding any Vaadin-owned context assembly shortcut.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Installed updated App artifact before compiling Infrastructure focused tests**
- **Found during:** Task 1
- **Issue:** Running only `mvn -pl pi-agent-infrastructure ...` resolved `pi-agent-app` from the local Maven repository, which did not yet include Plan 01's newly committed `ConversationContextAssembler` classes.
- **Fix:** Ran `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app -DskipTests install` once so the focused Infrastructure module test could resolve the latest App artifact.
- **Files modified:** None.
- **Verification:** Subsequent focused Infrastructure test compiled and passed.
- **Committed in:** Not applicable; local build artifact only.

**2. [Rule 2 - Missing Critical] Wired cloud composition in addition to local/dev composition**
- **Found during:** Task 2
- **Issue:** The plan explicitly called out local/dev wiring, but leaving the cloud composition root on the backwards-compatible empty assembler would make production dispatch silently omit multi-turn context.
- **Fix:** Added `ConversationContextPolicy`/`ConversationContextAssembler` beans to `CloudRuntimeBeanConfiguration` and supplied them to `DefaultRunDispatcher`, matching the local/dev composition path.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -DskipTests test`
- **Committed in:** `d4da21b`

---

**Total deviations:** 2 auto-fixed (1 blocking local artifact issue, 1 missing production composition requirement)
**Impact on plan:** Both fixes stayed within the planned dispatcher/composition scope and improved correctness for CTX-01/CTX-05.

## Issues Encountered

- Existing unrelated working-tree changes were present before and after execution: `.gitignore` modified and `.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md` untracked. They were not staged or committed by this plan.
- Adapter-web compile emits existing deprecation warnings for Spring `@MockBean` in tests; these are unrelated to the plan changes and were not modified.

## Known Stubs

None. Stub scan found only existing null/empty defensive handling and local in-memory repository fallback behavior, not plan-blocking UI/rendering stubs or placeholder context wiring.

## User Setup Required

None - no external credentials or services required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure -Dtest=DefaultRunDispatcherContextTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure,pi-agent-adapter-web -Dtest=DefaultRunDispatcherContextTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -DskipTests test` — passed.

## Next Phase Readiness

- Plan 03 can convert populated `SessionContext.messages()` plus the current `RunInput` into ordered provider/model chat messages.
- Plan 04 can add fake-model end-to-end proof that the prior messages injected here become provider-visible in order and that current prompt appears exactly once.
- Phase 21 can include the safe metadata keys in regression/security assertions without needing message-text inspection.

## Self-Check: PASSED

- Created/modified files verified present: `DefaultRunDispatcher.java`, `DefaultRunDispatcherContextTest.java`, `LocalDevRuntimeBeanConfiguration.java`, `CloudRuntimeBeanConfiguration.java`, and this summary.
- Task commits verified present in git history: `ff37013`, `d4da21b`.

---
*Phase: 19-multi-turn-runtime-context*
*Completed: 2026-07-01*
