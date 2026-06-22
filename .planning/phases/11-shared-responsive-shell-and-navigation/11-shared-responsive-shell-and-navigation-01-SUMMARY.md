---
phase: 11-shared-responsive-shell-and-navigation
plan: 01
subsystem: ui
tags: [vaadin, routerlayout, responsive-shell, navigation, mobile]
requires:
  - phase: 10-responsive-baseline-and-mobile-test-harness
    provides: pi-mobile theme baseline and route smoke selector contract
provides:
  - Shared PiResponsiveShell RouterLayout for Console and Admin Governance routes
  - PiRouteNavRegistry single source of truth for eight Console/Admin routes
  - Admin Governance landing page replacing the old pseudo-layout route
affects: [phase-12-console-mobile-first-flow, phase-13-runtime-ux, phase-14-admin-mobile-coverage]
tech-stack:
  added: []
  patterns: [Vaadin RouterLayout shell, immutable route registry, stable data hook navigation]
key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiRouteNavItem.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiRouteNavRegistry.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLandingView.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java
key-decisions:
  - "Use PiResponsiveShell as the single RouterLayout for Console and Admin Governance visual chrome."
  - "Keep route/navigation metadata in PiRouteNavRegistry so Java shell and browser tests share route truth."
patterns-established:
  - "All Console/Admin Vaadin routes declare layout = PiResponsiveShell.class."
  - "Navigation exposes data-shell, data-nav, data-nav-item, data-nav-active, and data-page-title hooks."
requirements-completed: [MH5-02]
duration: 6min
completed: 2026-06-22
---

# Phase 11 Plan 01: Shared Responsive Shell and Route Registry Summary

**Vaadin RouterLayout shell with a typed Console/Admin navigation registry and all eight existing routes attached to shared chrome**

## Performance

- **Duration:** ~6 min
- **Started:** 2026-06-22T03:38:17Z
- **Completed:** 2026-06-22T03:44:14Z
- **Tasks:** 3
- **Files modified:** 14

## Accomplishments

- Added `PiRouteNavItem` and `PiRouteNavRegistry` containing exactly the eight Phase 10 Console/Admin routes.
- Added `PiResponsiveShell` with compact header, drawer controls, grouped navigation, content slot, active-state hooks, and route title hook.
- Replaced the Admin pseudo-layout with `AdminGovernanceLandingView`, deleted the unused `MainConsoleLayout`, and attached all Console/Admin routes to the shared shell.

## Task Commits

1. **Tasks 1-3: route registry, shell, and route attachment** - `2ca149f` (feat)

## Files Created/Modified

- `PiRouteNavItem.java` - Immutable route metadata record.
- `PiRouteNavRegistry.java` - Shared route/nav registry for Console and Admin Governance.
- `PiResponsiveShell.java` - Shared RouterLayout visual shell.
- `AdminGovernanceLandingView.java` - Normal landing route for `/admin/governance`.
- `ConsoleView.java`, `Admin*View.java` - Route annotations now use `layout = PiResponsiveShell.class`.
- `WebResponsiveShellContractTest.java` - Java contract coverage for registry, shell hooks, and route annotations.
- `WebMobileBaselineContractTest.java` - Updated landing route baseline class reference.

## Decisions Made

- Kept `PiWebAppShell` as theme/meta owner and introduced `PiResponsiveShell` only for visual chrome.
- Used plain Vaadin Java components for shell/nav rather than adding a frontend framework.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Used Maven directly because `./mvnw` is absent**
- **Found during:** Task 1 verification
- **Issue:** Plan commands referenced `./mvnw`, but the repository has no Maven wrapper.
- **Fix:** Ran equivalent `mvn` commands with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
- **Files modified:** None.
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest test` passed.
- **Committed in:** N/A, command/environment-only deviation.

**2. [Rule 3 - Blocking] Forced Java 21 for Maven validation**
- **Found during:** Task 1 verification
- **Issue:** System `mvn` defaulted to Java 17 and failed the Java 21 release compile.
- **Fix:** Set `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` for Maven validation commands.
- **Files modified:** None.
- **Verification:** Focused and full shell contract tests passed.
- **Committed in:** N/A, command/environment-only deviation.

**Total deviations:** 2 auto-fixed (blocking command/environment issues).  
**Impact on plan:** No product scope change.

## Issues Encountered

- Initial RouterLayout signature was adjusted to Vaadin Flow's `HasElement` content method and reverified.

## Known Stubs

None. Grep found pre-existing null/empty fallback patterns, but no Phase 11 stub blocks that prevent shell/navigation goals.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

The shared shell and route registry are ready for Phase 12 Console flow work and later Admin/runtime card migrations.

## Self-Check: PASSED

- Created files exist.
- Commit `2ca149f` exists.
- Validation claims were verified with Java 21 Maven and Playwright list gate.

---
*Phase: 11-shared-responsive-shell-and-navigation*
*Completed: 2026-06-22*
