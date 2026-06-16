---
phase: 08-controlled-dynamic-plugin-jars
plan: 06
subsystem: admin-ui
tags: [plugins, vaadin, admin-governance, playwright, tdd]

requires:
  - phase: 08-controlled-dynamic-plugin-jars
    provides: Plugin App/client governance DTOs and mutation contracts from Plan 08-03.
  - phase: 08-controlled-dynamic-plugin-jars
    provides: Plugin Cloud Server status, refresh, disable, and quarantine REST endpoints from Plan 08-04.
provides:
  - ConsoleHttpClient path/type anchors for plugin governance, refresh, disable, and quarantine REST calls.
  - AdminRegistryStatusView rendering for plugin metadata, lifecycle, health, compatibility, capabilities, redacted errors, and action plans.
  - Non-sandbox warning copy for JVM classloader plugin isolation in Admin UI.
  - Playwright browser smoke coverage for plugin Admin REST and redaction behavior.
affects: [adapter-web, admin-governance, plugin-governance, phase-08-e2e]

tech-stack:
  added: []
  patterns:
    - Vaadin Admin views remain public-DTO-backed and expose action plans instead of direct private App/Domain access.
    - Browser E2E fixtures use deterministic no-key plugin governance data through the public App port.

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPluginGovernanceViewTest.java
    - e2e/phase-08-plugin-governance.spec.ts
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java

key-decisions:
  - "Render plugin disable/quarantine as confirmed POST action plans with optional reason metadata, not as upload/install/delete/upgrade workflows."
  - "Display JVM classloader isolation as lifecycle/dependency isolation only and explicitly warn that it is not a sandbox for untrusted code."
  - "Use e2e-profile PluginGovernanceCatalog fixture data so browser smoke remains no-key/no-Docker and public API/UI based."

patterns-established:
  - "Plugin Admin UI mirrors MCP governance style but permits only Phase 8's narrow refresh/disable/quarantine action plans."
  - "Component tests assert absence of deferred plugin marketplace/management controls alongside positive status/action rendering."

requirements-completed: [PLUG-04, PLUG-05, PLUG-06]

duration: 7m 51s
completed: 2026-06-16
---

# Phase 08 Plan 06: Plugin Governance Admin UI Summary

**Plugin Admin Governance now renders public plugin status, confirmed refresh/disable/quarantine action plans, redacted diagnostics, and an explicit non-sandbox warning.**

## Performance

- **Duration:** 7m 51s
- **Started:** 2026-06-16T17:49:08Z
- **Completed:** 2026-06-16T17:56:59Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Added `ConsoleHttpClient` plugin governance path/type anchors for status, refresh, disable, quarantine, mutation requests, and mutation responses.
- Extended `AdminRegistryStatusView` with `showPlugins(...)`, plugin metadata/capability rendering, redacted diagnostics, confirmed disable/quarantine action-plan text, and the required non-sandbox warning.
- Added component tests proving plugin status/action rendering and absence of upload/install/delete/upgrade/search/export plugin controls.
- Added deterministic e2e-profile plugin governance fixture data covering healthy, disabled, quarantined, and failed/incompatible plugins.
- Added a Playwright smoke test verifying public Admin plugin REST status/mutations, redacted errors, and absence of raw fake secrets/paths or deferred plugin controls.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add plugin governance UI path anchors and rendering** - `5000f01` (test)
2. **Task 1 GREEN: Add plugin governance UI path anchors and rendering** - `93fd3d2` (feat)
3. **Task 2 RED: Add browser smoke for plugin governance** - `f040a1e` (test)
4. **Task 2 GREEN: Add browser smoke for plugin governance** - `445b30c` (feat)

_Note: Both planned tasks were TDD tasks, so each produced a RED test commit and a GREEN implementation/fixture commit._

## Files Created/Modified

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPluginGovernanceViewTest.java` - Component tests for plugin REST anchors, status rendering, actions, warnings, redaction, and deferred-control absence.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` - Adds public DTO-backed plugin governance, refresh, disable, quarantine, and mutation type anchors.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java` - Renders plugin governance details, capabilities, redacted errors, non-sandbox copy, and confirmed action plans.
- `e2e/phase-08-plugin-governance.spec.ts` - Browser smoke for plugin Admin governance REST/status/mutation/redaction contracts.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration.java` - Adds e2e-profile fake plugin governance catalog data and mutation responses.

## Decisions Made

- Kept Vaadin plugin controls as explicit action-plan metadata and text so the UI communicates required confirmation/reason semantics without implementing a full workflow engine in this plan.
- Rendered action plans for each plugin row because disable/quarantine are scoped to a plugin ID and must be auditable per plugin.
- Verified plugin browser smoke primarily through public REST endpoints and raw JSON route rendering; this preserves the existing Phase 5/7 no-key Playwright pattern while avoiding fragile Vaadin client-side boot assumptions.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None. Empty maps/lists in tests are deterministic fixture data for plugins without capabilities or status counts, not production placeholders. Defensive `null`/empty string handling in Admin UI helpers prevents blank public DTO fields from breaking rendering and does not feed mock data into UI output.

## Issues Encountered

- TDD RED for Task 1 failed as expected on missing `ConsoleHttpClient` plugin methods and `AdminRegistryStatusView.showPlugins(...)`.
- The focused Maven test compile also surfaced an unrelated, pre-existing in-progress Plan 08-05 test (`PluginGovernedToolE2ETest`) that references `PluginE2EConfiguration`; this file was not part of Plan 08-06 and was left untouched.
- Pre-existing unrelated uncommitted planning artifacts under Phase 02/Phase 03 and `bun.lock` were present before execution and left untouched.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=AdminPluginGovernanceViewTest test` — passed
- `npm run e2e -- e2e/phase-08-plugin-governance.spec.ts` — passed

## Auth Gates

None.

## User Setup Required

None - tests use fake plugin governance data and dev/test auth headers with no external plugin JAR, model key, Docker, network service, or database requirement.

## Next Phase Readiness

- Plan 08-07 can add sample read-only plugin JAR packaging while relying on this UI/browser smoke for Admin visibility and narrow lifecycle controls.
- Plan 08-08 can include this non-sandbox warning and deferred-control absence in final architecture/docs/smoke traceability.

## Self-Check: PASSED

- Verified summary and key files exist: `08-06-SUMMARY.md`, `AdminPluginGovernanceViewTest.java`, `ConsoleHttpClient.java`, `AdminRegistryStatusView.java`, `phase-08-plugin-governance.spec.ts`, and `WebConsoleE2EFixtureConfiguration.java`.
- Verified task commits exist in git history: `5000f01`, `93fd3d2`, `f040a1e`, and `445b30c`.

---
*Phase: 08-controlled-dynamic-plugin-jars*
*Completed: 2026-06-16*
