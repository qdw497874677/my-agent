---
phase: 07-mcp-client-bridge-and-governed-remote-tools
plan: 07
subsystem: ui
tags: [java, vaadin, mcp, governance, playwright]

requires:
  - phase: 07-mcp-client-bridge-and-governed-remote-tools
    provides: MCP governance DTOs, public REST status endpoint, refresh endpoint, and Adapter Web wiring from Plans 07-02 and 07-06
provides:
  - Read-only Vaadin MCP governance rendering for server, discovery, health, auth summary, tool, risk, schema, and redacted error status
  - ConsoleHttpClient public API helpers for GET MCP status and POST refresh only
  - Focused Java view tests and Playwright smoke discovery for MCP governance
affects: [phase-07, phase-08, admin-governance, web-console, e2e]

tech-stack:
  added: []
  patterns:
    - Vaadin Admin Governance views consume pi-agent-client DTOs through ConsoleHttpClient public API path helpers
    - MCP governance UI remains inspect-only with a refresh action plan and no server configuration CRUD controls

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java
    - e2e/phase-07-mcp-governance.spec.ts
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java

key-decisions:
  - "Apply Plan 07-07 to the existing ui/admin AdminRegistryStatusView class names rather than the older plan package names."
  - "Keep MCP refresh as a read-only action plan using POST /api/admin/governance/mcp/refresh; no add/edit/delete/disable helpers or controls were added."

patterns-established:
  - "MCP governance rendering uses server/tool DTO fields directly and records deterministic renderedText for no-browser unit assertions."
  - "Browser E2E for MCP governance asserts public UI/API behavior and absence of CRUD text rather than pixel snapshots."

requirements-completed: [MCP-04]

duration: 5m 08s
completed: 2026-06-16
---

# Phase 07 Plan 07: MCP Governance UI Summary

**Read-only MCP governance in the Java Admin Console with public REST helpers, status/refresh rendering, and Playwright smoke discovery.**

## Performance

- **Duration:** 5m 08s
- **Started:** 2026-06-16T09:36:20Z
- **Completed:** 2026-06-16T09:41:28Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Added `ConsoleHttpClient` helpers for MCP governance status and refresh endpoints using `pi-agent-client` DTO type anchors only.
- Replaced the Admin Registry MCP placeholder path with DTO-driven server/tool rendering, including transport, connection/discovery status, auth summary, tool count, last refresh, risk hints, schema summary, and redacted error text.
- Added focused Java view tests for healthy/unhealthy MCP fixtures and read-only refresh affordance.
- Added a Phase 7 Playwright smoke spec that verifies MCP governance status discovery and asserts CRUD controls are absent.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add ConsoleHttpClient MCP API helpers** - `c2a5dd1` (feat)
2. **Task 2: Render MCP server and tool status in Admin Governance** - `4317a98` (feat)
3. **Task 3: Add Phase 7 MCP browser governance smoke spec** - `c8160a1` (test)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/ConsoleHttpClient.java` - Added read-only MCP governance status and refresh public API helpers.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java` - Added MCP governance rendering and refresh action-plan metadata without configuration mutation controls.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java` - Added unit coverage for client helpers, MCP status rendering, redaction, and absent CRUD controls.
- `e2e/phase-07-mcp-governance.spec.ts` - Added browser smoke discovery for MCP governance endpoints/UI text and no-CRUD assertions.

## Decisions Made

- Applied the plan to the current `io.github.pi_java.agent.adapter.web.ui.admin.AdminRegistryStatusView` surface because the plan referenced older `admin/AdminRegistryView` names that no longer exist.
- Kept MCP governance inspect-only: refresh is represented as a POST action plan, while add/edit/delete/disable helpers and UI labels remain absent.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Used reactor-aware Maven verification**
- **Found during:** Task 1 (Add ConsoleHttpClient MCP API helpers)
- **Issue:** The plan's narrow command `mvn -pl pi-agent-adapter-web ...` could not resolve local snapshot reactor dependencies (`pi-agent-infrastructure-mcp`, `pi-agent-spring-boot-starter`) when they were not installed locally.
- **Fix:** Ran the verification with `-am` to build required local modules in the reactor.
- **Files modified:** None
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=McpAdminGovernanceViewTest test`
- **Committed in:** N/A (verification command adjustment only)

**2. [Rule 3 - Blocking] Applied UI work to current class/package names**
- **Found during:** Task 2 (Render MCP server and tool status in Admin Governance)
- **Issue:** The plan referenced `AdminGovernanceView` and `AdminRegistryView` under an older package; the repository's active implementation is `ui/admin/AdminRegistryStatusView`.
- **Fix:** Implemented MCP governance rendering in `AdminRegistryStatusView`, preserving the current Vaadin route and public DTO boundary.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java`, `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=McpAdminGovernanceViewTest test`
- **Committed in:** `4317a98`

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both adjustments were necessary to execute the intended plan against the current repository state. No feature scope was expanded beyond read-only MCP governance UI and refresh.

## Issues Encountered

- Pre-existing unrelated working-tree changes were present under older Phase 02 planning artifacts and `bun.lock`; they were left unstaged and untouched.

## Known Stubs

None. The new MCP rendering consumes real `McpGovernanceResponse`, `McpServerDto`, and `McpToolDto` data, and the E2E spec is discoverable for the existing Playwright harness.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=McpAdminGovernanceViewTest test`
- `npm run e2e -- --list`

## Next Phase Readiness

- Plan 07-08 can complete E2E fixture closeout with a discoverable MCP governance browser spec already in place.
- Phase 8 dynamic plugin governance can reuse the inspect-only Admin Registry rendering pattern and ConsoleHttpClient public API anchor.

## Self-Check: PASSED

- Verified key created/modified files exist.
- Verified task commits exist: `c2a5dd1`, `4317a98`, `c8160a1`.

---
*Phase: 07-mcp-client-bridge-and-governed-remote-tools*
*Completed: 2026-06-16*
