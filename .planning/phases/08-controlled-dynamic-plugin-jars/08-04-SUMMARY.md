---
phase: 08-controlled-dynamic-plugin-jars
plan: 04
subsystem: adapter-web
tags: [plugins, spring, admin-governance, tool-registry, tdd]

requires:
  - phase: 08-controlled-dynamic-plugin-jars
    provides: PF4J plugin infrastructure bridge and disable/quarantine state from Plans 08-01 and 08-02.
  - phase: 08-controlled-dynamic-plugin-jars
    provides: App/client plugin governance contracts and use-case methods from Plan 08-03.
provides:
  - Adapter Web dependency on isolated plugin infrastructure.
  - `pi.plugins` Spring configuration binding and safe plugin governance composition root.
  - Primary governed ToolRegistry composition with plugins after built-ins, extensions, and MCP tools.
  - Authenticated Admin REST endpoints for plugin status, refresh, disable, and quarantine.
affects: [adapter-web, admin-rest, plugin-governance, tool-registry, phase-08-e2e]

tech-stack:
  added:
    - pi-agent-infrastructure-plugin dependency in pi-agent-adapter-web
  patterns:
    - Spring Adapter composition over plugin Infrastructure while preserving App/client DTO boundaries.
    - Primary composite ToolRegistry with source ordering and first-registry-wins duplicate handling.
    - Narrow Admin plugin mutation endpoints with no upload/install/delete/upgrade routes.

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/PluginToolRegistryWiringTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/PluginGovernanceApiTest.java
  modified:
    - pi-agent-adapter-web/pom.xml
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java

key-decisions:
  - "Bind controlled plugin runtime settings under `pi.plugins` in Adapter Web and translate them into infrastructure `PluginRegistryProperties` without leaking PF4J types."
  - "Compose plugin tool capabilities after built-ins, Phase 6 extensions, and Phase 7 MCP tools so plugin tools do not silently override earlier governed sources."
  - "Expose only status, refresh, disable, and quarantine Admin REST endpoints; plugin upload/install/delete/upgrade remain deferred and unsupported."

patterns-established:
  - "Plugin Cloud Server wiring mirrors MCP wiring: source-specific infrastructure is adapted into ToolRegistry plus governance catalog beans behind App/client contracts."
  - "Tests use fake plugin governance/source objects and dev/test auth headers, requiring no plugin JAR, network, model key, or external service."

requirements-completed: [PLUG-01, PLUG-02, PLUG-03, PLUG-04, PLUG-05]

duration: 11m 00s
completed: 2026-06-16
---

# Phase 08 Plan 04: Plugin Cloud Server Wiring and Admin REST Summary

**Cloud Server now binds controlled plugin configuration, composes plugin tools into the governed registry, and exposes narrow Admin plugin lifecycle REST operations.**

## Performance

- **Duration:** 11m 00s
- **Started:** 2026-06-16T17:34:57Z
- **Completed:** 2026-06-16T17:45:57Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added `pi-agent-infrastructure-plugin` as an Adapter Web dependency.
- Added `PluginGovernanceBeanConfiguration` with `pi.plugins` property binding for enablement, controlled directory, startup/manual refresh flags, allowlist/selection lists, platform API version, duplicate override control, and non-sandbox warning acknowledgement.
- Wired plugin state/governance beans and a safe empty plugin ToolRegistry fallback when plugin loading is disabled or no controlled directory is configured.
- Composed plugin tools into the primary `ToolRegistry` after built-ins, extension tools, and MCP tools while preserving first-registry-wins duplicate semantics.
- Added Admin REST endpoints:
  - `GET /api/admin/governance/plugins`
  - `POST /api/admin/governance/plugins/refresh`
  - `POST /api/admin/governance/plugins/{pluginId}/disable`
  - `POST /api/admin/governance/plugins/{pluginId}/quarantine`
- Verified plugin status and mutation JSON stays on public DTOs and does not leak fake secrets or raw host paths.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add Adapter Web plugin composition and ToolRegistry wiring** - `aa3b92c` (test)
2. **Task 1 GREEN: Add Adapter Web plugin composition and ToolRegistry wiring** - `d722744` (feat)
3. **Task 2 RED: Add Admin plugin governance REST endpoints** - `0a5839a` (test)
4. **Task 2 GREEN: Add Admin plugin governance REST endpoints** - `2487729` (feat)

## Files Created/Modified

- `pi-agent-adapter-web/pom.xml` - Adds Adapter Web dependency on `pi-agent-infrastructure-plugin`.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java` - Binds `pi.plugins`, creates plugin state/governance beans, and exposes a plugin ToolRegistry bean with disabled/no-directory fallback.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java` - Adds plugin ToolRegistry to the primary composite registry after built-ins, extensions, and MCP.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java` - Adds plugin status, refresh, disable, and quarantine REST endpoints.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/PluginToolRegistryWiringTest.java` - Verifies plugin bean wiring, plugin provenance, safe path summaries, and final registry composition order.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/PluginGovernanceApiTest.java` - Verifies authenticated plugin Admin REST behavior, redaction, and absence of unsupported plugin CRUD routes.

## Decisions Made

- Kept PF4J/plugin implementation classes isolated to `pi-agent-infrastructure-plugin`; Adapter Web only composes infrastructure beans and public App/client governance contracts.
- Used `pi.plugins.directory` as the Spring property for the controlled plugin directory and mapped it to `PluginRegistryProperties.pluginDirectory`.
- Chose an empty ToolRegistry fallback for disabled/no-directory plugin configurations so Cloud Server startup remains safe without a plugin directory.
- Kept plugin Admin mutations as delegated use-case calls through `GovernanceQueryService`, matching existing MCP endpoint patterns and `RequestContext` construction.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added explicit empty plugin ToolRegistry fallback**
- **Found during:** Task 1 GREEN
- **Issue:** The App `ToolRegistry` port has no built-in static empty registry factory, but Cloud Server must start safely when plugins are disabled or no directory exists.
- **Fix:** Added a private empty ToolRegistry implementation in `PluginGovernanceBeanConfiguration` for disabled/no-directory plugin configurations.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java`
- **Commit:** `d722744`

**2. [Rule 1 - Bug] Added explicit `@PathVariable("pluginId")` names**
- **Found during:** Task 2 GREEN verification
- **Issue:** The test runtime could not infer method parameter names for plugin mutation routes, causing disable/quarantine endpoint requests to fail before controller delegation.
- **Fix:** Added explicit path-variable names for both plugin mutation endpoints.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java`
- **Commit:** `2487729`

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug)
**Impact on plan:** Both fixes were required for planned startup safety and REST verification; neither expanded the public plugin scope beyond the plan.

## Known Stubs

None. Empty lists/maps/strings and default `1.0.0` platform API values in plugin configuration are defensive optional-configuration defaults, not UI-facing placeholder data. Fake plugin data exists only in focused tests.

## Issues Encountered

- The TDD RED for Task 1 failed as expected because Adapter Web did not yet depend on plugin infrastructure or expose plugin registry beans.
- The TDD RED for Task 2 failed as expected because plugin Admin routes did not yet exist and requests fell through to Vaadin forwarding.
- Pre-existing unrelated uncommitted planning artifacts under Phase 02/Phase 03 and `bun.lock` were present before execution and left untouched.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=PluginToolRegistryWiringTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=PluginGovernanceApiTest test` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=PluginToolRegistryWiringTest,PluginGovernanceApiTest test` — passed
- Inspected route patterns and confirmed no plugin upload/install/delete/upgrade Admin endpoints were added.

## Auth Gates

None.

## User Setup Required

None - focused tests use fake plugin governance data and dev/test auth headers with no external plugin JAR, model key, network, or database requirement.

## Next Phase Readiness

- Plan 08-05 can build governed plugin tool E2E on the same composed `ToolRegistry` and `ToolExecutionGateway` path.
- Plan 08-06 can render plugin Admin UI controls against the new public REST endpoints without changing App/client DTO contracts.
- Plan 08-07 sample plugin packaging can plug into this Adapter composition root by replacing fake discovered plugin data with PF4J controlled-directory discovery.

## Self-Check: PASSED

- Verified summary and key files exist: `08-04-SUMMARY.md`, `PluginGovernanceBeanConfiguration.java`, `PluginToolRegistryWiringTest.java`, `PluginGovernanceApiTest.java`, and `AdminGovernanceController.java`.
- Verified task commits exist in git history: `aa3b92c`, `d722744`, `0a5839a`, and `2487729`.

---
*Phase: 08-controlled-dynamic-plugin-jars*
*Completed: 2026-06-16*
