---
phase: 05-agent-web-console-and-runtime-cockpit
plan: 06
subsystem: ui
tags: [vaadin, agent-catalog, tool-cards, run-event-renderer, redaction]

# Dependency graph
requires:
  - phase: 05-agent-web-console-and-runtime-cockpit
    provides: Public Agent Catalog DTOs and `/api/agents` API from Plan 05-02
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Redacted `tool.lifecycle` event payload contract for governed tool execution
provides:
  - Vaadin Agent Catalog panel and cards populated from public `AgentCatalogResponse` data
  - Chat-first Console integration that keeps Catalog available as a secondary Agent switcher
  - Expandable governed tool lifecycle cards with redacted summaries and diagnostics
  - Run event renderer wiring that turns `tool.lifecycle` events into `ToolCallCard` components
affects: [agent-web-console, runtime-cockpit, approval-cards, browser-e2e]

# Tech tracking
tech-stack:
  added: []
  patterns: [public-dto-driven-vaadin-components, expandable-tool-event-cards, redacted-diagnostic-rendering]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java
    - .gitignore

key-decisions:
  - "Render Agent Catalog cards from public AgentCatalogResponse fixtures and keep Catalog as a secondary switcher inside the Chat-first Console rather than a dominant landing page."
  - "Represent governed tool lifecycle events as expandable Vaadin components created from redacted public RunEventDto payload maps."
  - "Ignore Vaadin dev-mode generated frontend output because focused UI tests create runtime artifacts that should not enter source commits."

patterns-established:
  - "Vaadin Console components expose testable summary/accessor methods while still rendering normal Flow components for browser E2E readiness."
  - "Tool lifecycle rendering checks payloadSchema == tool.lifecycle so event type and schema contracts both route to ToolCallCard."

requirements-completed: [GUI-01, GUI-03, GUI-08]

# Metrics
duration: 7m 08s
completed: 2026-06-15
---

# Phase 05 Plan 06: Agent Catalog and Tool Cards Summary

**Public DTO-driven Agent Catalog cards and expandable redacted governed tool lifecycle cards inside the Chat-first Console.**

## Performance

- **Duration:** 7m 08s
- **Started:** 2026-06-15T05:48:31Z
- **Completed:** 2026-06-15T05:55:39Z
- **Tasks:** 2 planned TDD tasks completed
- **Files modified:** 7 plan-owned/source files plus `.gitignore`

## Accomplishments

- Added `AgentCatalogPanel` and `AgentCard` to render Agent choices from `AgentCatalogResponse` data, including name, description, input modes, capabilities, safe model ref, allowed tools/scopes, risk and side-effect labels, and entry actions.
- Integrated the Catalog into `ConsoleView` as a secondary switcher under the left workbench column while preserving the Chat-first center-panel experience.
- Added `ToolCallCard` for `tool.lifecycle` events, with default summary fields and expandable redacted details for event sequence, policy reason, preview references, diagnostics, status, progress, results, and errors.
- Updated `RunEventRenderer` so governed tool lifecycle events produce inline `ToolCallCard` components in the integrated stream.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Agent Catalog card behavior tests** - `c1431f4` (test)
2. **Task 1 GREEN: Agent Catalog panel and cards** - `3a54fde` (feat)
3. **Task 2 RED: Tool lifecycle card behavior tests** - `88862e9` (test)
4. **Task 2 GREEN: Governed tool lifecycle cards and renderer wiring** - `f0601fc` (feat)
5. **Generated artifact hygiene** - `4b02de1` (chore)

**Plan metadata:** pending final docs commit.

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java` - Secondary Catalog switcher panel populated from public Agent Catalog responses.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCard.java` - Agent card rendering run-decision metadata and entry actions.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java` - Expandable governed tool lifecycle card with redacted summary/detail text.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` - Wires Catalog panel into the Chat-first workbench without changing the primary Chat flow.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java` - Routes `tool.lifecycle` event schema/type to `ToolCallCard` components.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java` - TDD coverage for Catalog cards, API fixture population, tool card summary/details, and renderer wiring.
- `.gitignore` - Ignores Vaadin dev-mode generated frontend artifacts created during focused tests.

## Decisions Made

- Catalog rendering is entirely DTO-driven and test fixtures use `AgentCatalogResponse`; the UI does not hardcode Agent definitions beyond preserving the pre-existing default selected Agent ID for run creation.
- Tool lifecycle card rendering remains defensive over optional payload map keys because Phase 4 guarantees redacted public payloads but individual lifecycle events may omit fields.
- Vaadin-generated frontend output is treated as runtime/generated test output and ignored rather than committed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Ignored generated Vaadin frontend output**
- **Found during:** Task 2 verification
- **Issue:** Running Vaadin/Spring tests created untracked `pi-agent-adapter-web/src/main/frontend/` generated artifacts, which would leave the workspace dirty and risk committing generated runtime output.
- **Fix:** Added `pi-agent-adapter-web/src/main/frontend/` to `.gitignore` and committed the hygiene change separately.
- **Files modified:** `.gitignore`
- **Verification:** `git status --short pi-agent-adapter-web/src/main/frontend` no longer reports generated files.
- **Committed in:** `4b02de1`

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** No feature scope creep; this keeps generated Vaadin test artifacts out of source control.

## Issues Encountered

- None in plan-owned tests. Focused verification passed.

## Validation Results

- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=WebConsoleCatalogAndToolCardsTest test`
- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=WebConsoleCatalogAndToolCardsTest,GovernedToolSecurityRedactionE2ETest test`
- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=WebConsoleCatalogAndToolCardsTest,WebConsoleUserFlowTest,GovernedToolSecurityRedactionE2ETest test`

## Known Stubs

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/AgentCatalogPanel.java:35` uses `List.of()` only as a defensive empty response fallback for null/empty public API data; it does not replace the `/api/agents` data source.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java:12` and `ToolCallCard.java:30` use `Map.of()` only as defensive fallbacks for missing event payloads; they do not supply mock UI data.

## User Setup Required

None - no external services or model keys are required for focused verification.

## Next Phase Readiness

- Plan 05-07 can build approval cards using the same expandable card pattern and the existing `tool.approval_required` lifecycle summary fields.
- Browser E2E in Plan 05-09 can target stable data attributes on Agent cards, Catalog panel, and ToolCallCard components.

## Self-Check: PASSED

- Created/modified files exist.
- Task commits exist: `c1431f4`, `3a54fde`, `88862e9`, `f0601fc`, `4b02de1`.

---
*Phase: 05-agent-web-console-and-runtime-cockpit*
*Completed: 2026-06-15*
