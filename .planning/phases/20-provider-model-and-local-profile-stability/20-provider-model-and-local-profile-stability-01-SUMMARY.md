---
phase: 20-provider-model-and-local-profile-stability
plan: 01
subsystem: ui
tags: [vaadin, provider-config, model-refresh, i18n, redaction, tdd]

# Dependency graph
requires:
  - phase: 19-multi-turn-runtime-context
    provides: selected-session runtime context and current Console streaming/session seams
provides:
  - Explicit provider model refresh response states: success, empty, error, not_configured
  - Compact Console provider/model readiness row with stable selectors
  - Localized refresh success, empty, error, and setup guidance copy
  - Component coverage for provider readiness, refresh states, and redacted errors
affects: [provider-model-stability, local-profile-stability, console-ux, phase-21-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Adapter-web provider response records keep backward-compatible accessors while adding explicit state metadata.
    - Console model-bar feedback uses stable data attributes and localized inline copy rather than dialogs or notifications.
    - Provider error messages are truncated/redacted before reaching REST/UI response state.

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigController.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/resources/messages.properties
    - pi-agent-adapter-web/src/main/resources/messages_zh.properties

key-decisions:
  - "Keep model refresh state in ProviderConfigController.ModelListResponse so REST and Console callers share one redacted status contract."
  - "Render provider/model readiness as compact inline spans with stable data attributes instead of adding an operations panel or notification-only feedback."

patterns-established:
  - "ModelListResponse state contract: success, empty, error, and not_configured with ready/modelCount/selectedModel/providerId metadata."
  - "Console model bar exposes data-role=provider-status, data-role=model-refresh-status, data-refresh-state, and data-provider-ready for tests and browser verification."
  - "Provider refresh failures are summarized with credential/bearer/API-key redaction before UI rendering."

requirements-completed: [PROV-01, PROV-02]

# Metrics
duration: 14m05s
completed: 2026-07-04
---

# Phase 20 Plan 01: Provider Model Bar Readiness Summary

**Compact Console provider/model readiness with explicit redacted model-refresh states and localized feedback**

## Performance

- **Duration:** 14m05s
- **Started:** 2026-07-04T10:03:59Z
- **Completed:** 2026-07-04T10:18:04Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Added explicit `ModelListResponse` state metadata for `success`, `empty`, `error`, and `not_configured` while preserving the existing `models()` and `error()` accessors.
- Replaced the Console model refresh silent-failure path with compact inline provider readiness and refresh status copy.
- Added English and Chinese resource-bundle messages for ready/not-configured guidance and refresh success/empty/error states.
- Added focused component/controller coverage proving stable selectors, model list updates, safe empty/not-configured responses, and provider error redaction.

## Task Commits

Each task was committed atomically:

1. **TDD RED: Provider/model bar tests** - `32cd129` (test)
2. **Task 1: Add explicit refresh/readiness response states** - `305b831` (feat)
3. **Task 2: Render compact model-bar feedback with stable selectors** - `74430a7` (feat)

_Note: This plan used TDD; the failing test commit was created before implementation commits._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/provider/ProviderConfigController.java` - Adds stateful model refresh response metadata and safe error summarization.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` - Adds compact provider/readiness and refresh feedback spans with stable data selectors.
- `pi-agent-adapter-web/src/main/resources/messages.properties` - Adds English Console model-bar feedback copy.
- `pi-agent-adapter-web/src/main/resources/messages_zh.properties` - Adds Chinese Console model-bar feedback copy.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java` - Adds focused controller/component tests for refresh states, selectors, and redaction.

## Decisions Made

- Keep explicit model refresh readiness in `ProviderConfigController.ModelListResponse`, not in a separate UI-only DTO, so Admin/REST/Console behavior can share a single safe contract.
- Keep feedback compact in the existing Console model area using spans/chips and data attributes; no large operations dashboard, dialog, or notification-only surface was introduced.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Used Java 21 explicitly for verification**
- **Found during:** Task 1 verification
- **Issue:** The shell environment had `JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto`, causing Maven `release version 21 not supported` despite Java 21 being installed.
- **Fix:** Ran verification with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
- **Files modified:** None
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleProviderModelBarTest test`
- **Committed in:** N/A (environment-only fix)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Verification required an environment correction only; implementation scope stayed aligned with the plan.

## Issues Encountered

- Focused verification with `mvn -pl pi-agent-adapter-web` initially failed because dependent modules contained newer constructor signatures not rebuilt in the local reactor. Re-ran the focused gate with `-am` to build required modules.
- Working tree contained unrelated parallel-executor changes (`.gitignore`, `.planning/STATE.md`, Phase 17 verification doc, and Phase 20 Plan 03 summary). These were not staged or committed by this plan.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. Stub-pattern scans found only defensive null/default handling, existing test server conditionals, and intentional empty immutable lists for response defaults; none block provider/model readiness.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleProviderModelBarTest test` — **PASSED** (7 tests)

## Next Phase Readiness

- Console now exposes stable provider readiness and model refresh selectors for Phase 20 browser/local-profile validation.
- Provider model refresh failures now surface actionable redacted copy instead of being silently swallowed.
- PROV-01 and PROV-02 are ready for requirements traceability.

## Self-Check: PASSED

- Found summary file: `.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-01-SUMMARY.md`
- Found task commit: `32cd129`
- Found task commit: `305b831`
- Found task commit: `74430a7`

---
*Phase: 20-provider-model-and-local-profile-stability*
*Completed: 2026-07-04*
