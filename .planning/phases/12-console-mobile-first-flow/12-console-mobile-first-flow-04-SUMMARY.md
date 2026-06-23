---
phase: 12-console-mobile-first-flow
plan: 04
subsystem: ui
tags: [vaadin, mobile-console, agent-catalog, session-history, callbacks, java-contract-tests]

requires:
  - phase: 12-console-mobile-first-flow-01
    provides: route-local Console panel switcher and mobile panel state
  - phase: 12-console-mobile-first-flow-02
    provides: chat composer, run context status, and cancel surfaces
provides:
  - Listener-backed Agent Catalog CTA, Session card activation, Send, and Cancel control paths
  - Console Agent Catalog containment and read-model initialization for browser-visible General Agent cards
  - Java regression contracts proving mobile Console controls are no longer selector-only
affects: [phase-12-plan-05, phase-13-runtime-cards, console-mobile-e2e, vaadin-console]

tech-stack:
  added: []
  patterns: [Vaadin component callbacks, adapter-web read-model initialization, listener-backed contract tests]

key-files:
  created:
    - .planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-04-SUMMARY.md
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java

key-decisions:
  - "Use route-local Vaadin callbacks instead of new routes or mobile-only APIs for Agent, Session, Send, and Cancel actions."
  - "Initialize Console Agent Catalog from the existing AgentCatalogQueryService read model, keeping Vaadin concerns in adapter-web."
  - "Keep historical Session population limited to existing/current flow seams; preserve explicit empty state until a history/list read model supplies rows."

patterns-established:
  - "Console components expose small Consumer/BiConsumer/Runnable handlers while ConsoleView owns state transitions."
  - "Fast Java contracts invoke Vaadin Button.click() and component activation seams to catch selector-only regressions."
  - "AgentCatalogPanel remains inside the Agents panel wrapper and is not reparented into Sessions."

requirements-completed: [MCON-01, MCON-02, MCON-04, MCON-05, MVER-03]

duration: 12min
completed: 2026-06-23
---

# Phase 12 Plan 04: Console Mobile Control Wiring Summary

**Listener-backed Vaadin Console controls for General Agent selection, session activation, prompt sending, cancellation, and read-model catalog initialization.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-06-23T09:08:25Z
- **Completed:** 2026-06-23T09:20:11Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments

- Added regression contracts that fail when the Agents panel is hollow, General Agent CTA is selector-only, session cards cannot activate, or Send/Cancel controls do not reach ConsoleView state seams.
- Wired Agent Catalog cards, session cards, Send, primary Cancel, and backup Cancel through real Vaadin listeners and ConsoleView callbacks.
- Removed the `sessionListPanel.add(agentCatalogPanel)` reparenting blocker so AgentCatalogPanel stays inside `[data-console-panel="agents"]`.
- Initialized normal Console construction from the existing `AgentCatalogQueryService`, making the General Agent browser-visible without hardcoded fake UI data.
- Preserved the explicit Session empty state while keeping rendered session rows activatable by click/Enter/Space.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add failing contracts for real Console UI activation** - `bee91d6` (test)
2. **Task 2: Implement callback contracts for Agent, Session, Send, and Cancel controls** - `d065514` (feat)
3. **Task 3: Keep catalog and session surfaces browser-visible with safe initial data paths** - `e59aa2e` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` - Wires component callbacks, keeps catalog in the Agents panel, initializes catalog data through `AgentCatalogQueryService`, and keeps Console state transitions route-local.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java` - Adds an Agent action handler seam and passes it to rendered `AgentCard` instances.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java` - Adds click listeners to entry action buttons when a handler is supplied.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java` - Adds click and keyboard activation wiring for session cards plus a test activation seam.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` - Connects Send and composer Cancel buttons to submit/cancel handlers and ignores blank sends.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunContextPanel.java` - Connects backup Cancel to the same cancel handler path.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java` - Adds MCON-01/MCON-02/MCON-04/MCON-05 contract coverage for real UI activation and initial catalog/session surfaces.

## Decisions Made

- Used small Java functional callbacks (`Consumer`, `BiConsumer`, `Runnable`) in Vaadin components so UI components remain reusable while ConsoleView retains action/state orchestration.
- Used the existing App-layer `AgentCatalogQueryService` as the production route read-model seam rather than inventing `/mobile/*`, new DTOs, or fake hardcoded catalog data.
- Kept Session history population limited to current/existing read-model paths; this plan verifies empty state plus activatable rendered rows rather than creating a new global session-list endpoint.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Preserve active session identity after first user-triggered send**
- **Found during:** Task 2 (callback wiring)
- **Issue:** The cancel click path failed after Send because `planChatSubmission` returned a pending session id but did not store it as `selectedSessionId`, leaving `planCancelRunningRun` without an active session.
- **Fix:** Track whether a session is newly needed, store the pending session id as selected, and preserve the existing create-session plan semantics.
- **Files modified:** `ConsoleView.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test`
- **Committed in:** `d065514`

**2. [Rule 1 - Bug] Allow Space key activation seam without trimming it to blank**
- **Found during:** Task 2 (session keyboard activation contract)
- **Issue:** The test seam used the shared non-blank text validator, which rejected a literal space key despite Space being a required keyboard activation path.
- **Fix:** Accept non-empty activation keys and explicitly handle `" "`/`Space` alongside click and Enter.
- **Files modified:** `SessionListPanel.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test`
- **Committed in:** `d065514`

---

**Total deviations:** 2 auto-fixed (2 Rule 1 bugs)
**Impact on plan:** Both fixes were required for planned real user activation paths; no architectural scope changes or mobile-only APIs were introduced.

## Issues Encountered

- The Task 1 RED gate failed at test compilation because the planned keyboard activation seam did not exist yet; this was expected for TDD RED and was resolved in Task 2.
- Existing unrelated working tree changes were present before execution; only files from this plan were staged and committed for task commits.

## Known Stubs

None blocking. Existing placeholder/empty-state text remains intentional UI copy: `ChatEventStreamPanel.PLACEHOLDER`, Agent Catalog empty state, and Session empty/unknown-updated labels are explicit fallback states, not fake data sources.

## Verification

- **Task 1 RED gate:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleMobileFlowContractTest test` failed as expected before production wiring.
- **Task 2 gate:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test` passed (21 tests, 0 failures).
- **Task 3/final gate:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleCatalogAndToolCardsTest,WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test` passed (29 tests, 0 failures).
- **Anti-pattern check:** `sessionListPanel.add(agentCatalogPanel)` no longer appears; Send/Cancel/Agent/Session components include listener-backed handlers.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 05 can now wire/run browser-visible execution and SSE/event flow on top of listener-backed controls instead of selector-only UI elements.
- Phase 13 can continue with runtime/tool/approval card interior work while relying on Agent/Session/Send/Cancel control activation seams.

## Self-Check: PASSED

- Found summary file: `.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-04-SUMMARY.md`
- Found modified Console file: `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java`
- Found task commit `bee91d6`: failing Console activation contracts
- Found task commit `d065514`: callback wiring implementation
- Found task commit `e59aa2e`: catalog read-model initialization

---
*Phase: 12-console-mobile-first-flow*
*Completed: 2026-06-23*
