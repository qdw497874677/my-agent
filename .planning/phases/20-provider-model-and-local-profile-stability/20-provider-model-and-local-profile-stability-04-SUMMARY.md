---
phase: 20-provider-model-and-local-profile-stability
plan: 04
subsystem: provider-runtime
tags: [java, vaadin, provider-model, run-queue, dispatcher, local-profile, fallback]
requires:
  - phase: 20-provider-model-and-local-profile-stability
    provides: typed RunProviderMetadata persistence and selector readiness feedback from plans 02/03
provides:
  - Console run creation pins selected provider/model/readiness facts into CreateRunRequest metadata
  - QueuedRun carries safe RunProviderMetadata snapshots from App usecase to workers
  - DefaultRunDispatcher derives each run AgentDefinition modelRef from queued snapshot metadata
  - DynamicAgentRuntime emits snapshot-aware provider/model labels for local fallback/runtime deltas
affects: [provider-model-selection, run-dispatch, local-runtime, fallback-labels, run-history]
tech-stack:
  added: []
  patterns:
    - Safe provider/model snapshot propagation via client DTO instead of provider config leakage
    - Dispatch-time AgentDefinition copy with per-run modelRef while preserving legacy defaults
key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelResolutionFlowTest.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherProviderModelTest.java
  modified:
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunProviderMetadata.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/QueuedRun.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandService.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/DynamicAgentRuntime.java
key-decisions:
  - "Use RunProviderMetadata as the App/queue snapshot boundary so provider configuration, API keys, and adapter-web types do not leak into dispatch contracts."
  - "Create a per-run AgentDefinition copy in DefaultRunDispatcher when queued metadata specifies a selected modelRef, preserving the constructor default only for legacy/no-snapshot runs."
  - "Keep local fallback labeling explicit through safe provider/model refs such as local-dev:not-configured, without adding automatic paid-provider fallback routing."
patterns-established:
  - "Console run creation snapshots providerId/modelId/readiness at submit time, so later selector changes affect subsequent runs only."
  - "QueuedRun remains backwards compatible through an overload that defaults providerMetadata to RunProviderMetadata.EMPTY."
requirements-completed: [PROV-03, PROV-04, PROV-05]
duration: 15m01s
completed: 2026-07-04
---

# Phase 20 Plan 04: Provider/Model Snapshot Dispatch Summary

**Per-run provider/model snapshots now flow from Console submit through queue dispatch into runtime model labels and fallback metadata**

## Performance

- **Duration:** 15m01s
- **Started:** 2026-07-04T10:28:14Z
- **Completed:** 2026-07-04T10:43:15Z
- **Tasks:** 2
- **Files modified:** 8 plan files

## Accomplishments

- Added safe `RunProviderMetadata.selectedSnapshot(...)` creation and wired Console submissions to include selected/requested model refs, provider id, model id, fallback mode, and readiness state without API keys or provider config snapshots.
- Extended `QueuedRun` to carry immutable safe provider metadata, and normalized request metadata in `DefaultRunCommandService` before enqueuing.
- Updated `DefaultRunDispatcher` to validate and dispatch with the queued run's selected/requested model ref instead of a stale constructor default, with default fallback for legacy no-snapshot tests/runs.
- Updated `DynamicAgentRuntime` model delta labels to use the run's effective model ref/provider/model, including explicit `local-dev:not-configured` style fallback labeling for not-configured local runs.
- Added focused adapter-web and infrastructure flow tests proving snapshot creation, snapshot immutability, dispatch modelRef selection, legacy fallback, and safe invalid metadata failure.

## Task Commits

Each task was committed atomically:

1. **Task 1: Carry selected model facts from Console run creation into the queued run** - `dc44ea4` (feat)
2. **Task 2: Use run model snapshot in dispatcher/runtime and record fallback facts** - `febabd0` (feat)

**Plan metadata:** pending final docs commit

_Note: Tests were added with their implementation task commits rather than separate RED/GREEN commits because the existing parallel working tree contained unrelated uncommitted files and task-level atomic commits were prioritized._

## Files Created/Modified

- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunProviderMetadata.java` - Adds safe selected snapshot construction for provider/model/readiness facts.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/QueuedRun.java` - Carries immutable safe provider metadata to workers with backwards-compatible constructor overload.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandService.java` - Normalizes safe metadata from create-run requests before enqueueing.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` - Snapshots selected provider/model metadata at chat submission time.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunProviderModelResolutionFlowTest.java` - Verifies Console/App snapshot propagation and selector immutability.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` - Selects dispatch-time modelRef from queued run metadata and copies AgentDefinition per run.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/DynamicAgentRuntime.java` - Emits snapshot-aware runtime/fallback model delta labels.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherProviderModelTest.java` - Verifies dispatcher snapshot selection, legacy default behavior, and invalid snapshot safety.

## Decisions Made

- Used the existing `RunProviderMetadata` DTO as the only cross-layer provider/model snapshot contract, avoiding arbitrary maps in `QueuedRun` and avoiding provider config/API-key leakage.
- Kept queue and dispatcher compatibility by adding a `QueuedRun` overload that supplies `RunProviderMetadata.EMPTY`, so existing test fakes and older queue rows continue to work.
- Derived a per-run `AgentDefinition` only inside Infrastructure dispatch, preserving COLA boundaries and avoiding mutation of the dispatcher constructor default.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Used Java 21 for Maven verification**
- **Found during:** Task 1 verification
- **Issue:** The project requires Java 21; Maven verification must run with the Java 21 runtime.
- **Fix:** Ran focused Maven verification with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
- **Files modified:** None.
- **Verification:** Both focused Maven commands completed successfully with Java 21.
- **Committed in:** N/A (environment command adjustment).

**2. [Rule 3 - Blocking] Adjusted new tests to existing constructor/test seams**
- **Found during:** Task 1 and Task 2 verification
- **Issue:** New tests initially assumed no-arg `ProviderConfigStore` and had one extra `RunProviderMetadata` constructor argument.
- **Fix:** Updated tests to use temp SQLite paths and the existing seven-field DTO constructor.
- **Files modified:** `RunProviderModelResolutionFlowTest.java`, `DefaultRunDispatcherProviderModelTest.java`.
- **Verification:** Focused Maven tests passed.
- **Committed in:** `dc44ea4`, `febabd0`.

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both fixes were required to execute the planned no-key Java verification; product scope remained unchanged.

## Issues Encountered

- The working tree already contained unrelated uncommitted changes (`.gitignore`, Phase 17 verification doc, and provider/fallback UI files) before this plan began. They were intentionally not staged or committed.
- Focused Maven runs compile upstream reactor modules and report existing deprecation warnings around Spring `@MockBean`; these warnings are pre-existing and out of this plan's scope.

## Verification

- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web,pi-agent-app -am -Dtest=RunProviderModelResolutionFlowTest test`
- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure,pi-agent-adapter-web -am -Dtest=DefaultRunDispatcherProviderModelTest,RunProviderModelResolutionFlowTest test`

## Known Stubs

None. Stub scan found only defensive null handling, existing UI placeholder text, and pre-existing unrelated fallback UI changes outside this plan's committed files.

## User Setup Required

None - no external provider credentials are required for these no-key focused tests.

## Next Phase Readiness

- New runs now carry selected provider/model facts that remain stable even if the Console selector changes later.
- Dispatch/runtime now use the per-run model snapshot, enabling restored history and fallback labels to reflect what each run actually requested/resolved.
- Phase 20 Plan 05 can build final local-profile verification/hardening on top of stable snapshot propagation.

## Self-Check: PASSED

- Created files exist: `RunProviderModelResolutionFlowTest.java`, `DefaultRunDispatcherProviderModelTest.java`.
- Modified key files exist and contain snapshot/dispatch wiring: `QueuedRun`, `DefaultRunCommandService`, `ConsoleView`, `DefaultRunDispatcher`, `DynamicAgentRuntime`, `RunProviderMetadata`.
- Task commits exist: `dc44ea4`, `febabd0`.
- Summary claims match focused verification outcomes.

---
*Phase: 20-provider-model-and-local-profile-stability*
*Completed: 2026-07-04*
