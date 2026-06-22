---
phase: 11-shared-responsive-shell-and-navigation
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiRouteNavItem.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiRouteNavRegistry.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLayout.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLandingView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/MainConsoleLayout.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java
  - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebMobileBaselineContractTest.java
autonomous: true
requirements: [MH5-02]
must_haves:
  truths:
    - "Mobile user can navigate Console and every Admin Governance section through one compact shared shell."
    - "Console and Admin remain distinct top-level product areas while Admin exposes grouped sub-navigation."
    - "The old pseudo-layout/zombie layout classes no longer compete with the shared shell."
  artifacts:
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java"
      provides: "Shared RouterLayout visual shell with compact header, drawer/nav, active route title, and content slot"
      min_lines: 80
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiRouteNavRegistry.java"
      provides: "Single route/navigation source of truth for Console plus Admin sections"
      contains: "admin/governance/approvals"
    - path: "pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLandingView.java"
      provides: "Admin landing route that is a page, not a layout abstraction"
      contains: "@Route(value = \"admin/governance\", layout = PiResponsiveShell.class)"
  key_links:
    - from: "PiRouteNavRegistry.java"
      to: "PiResponsiveShell.java"
      via: "shell renders nav items from registry"
      pattern: "PiRouteNavRegistry\\.(items|all)"
    - from: "ConsoleView.java and Admin *View.java"
      to: "PiResponsiveShell.java"
      via: "@Route layout assignment"
      pattern: "layout\\s*=\\s*PiResponsiveShell\\.class"
    - from: "PiResponsiveShell.java"
      to: "Vaadin route content"
      via: "RouterLayout content slot"
      pattern: "showRouterLayoutContent"
---

<objective>
Create the single shared Vaadin responsive shell and route navigation registry for Phase 11.

Purpose: Implements locked decisions D-01 through D-08 by replacing per-area/pseudo-layout composition with one shared RouterLayout, while preserving Console and Admin as distinct product areas.
Output: A shared shell, a typed route/nav registry, all eight current routes attached to the shell, and retired competing layout patterns.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/REQUIREMENTS.md
@.planning/phases/11-shared-responsive-shell-and-navigation/11-CONTEXT.md
@.planning/phases/10-responsive-baseline-and-mobile-test-harness/10-03-SUMMARY.md
@docs/phase-10-mobile-baseline.md
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiWebAppShell.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLayout.java
@pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/MainConsoleLayout.java

<interfaces>
Existing route hooks and route set to preserve from Phase 10:
```java
@Route("console")
@PageTitle("Pi Agent Console")
public class ConsoleView extends Div { ... data-route="console" ... }

@Route("admin/governance")
@PageTitle("Pi Admin Governance")
public class AdminGovernanceLayout extends Main { ... data-route="admin-governance" ... }

@Route("admin/governance/overview")
@Route("admin/governance/registry")
@Route("admin/governance/operations")
@Route("admin/governance/policy-decisions")
@Route("admin/governance/audits")
@Route("admin/governance/approvals")
```

Route/navigation registry contract to create before shell wiring:
```java
public record PiRouteNavItem(
    String productArea,      // "console" or "admin"
    String groupLabel,       // "Console" or "Admin Governance"
    String route,            // no leading slash, e.g. "admin/governance/registry"
    String title,            // shell/page title text
    String navLabel,         // drawer link text
    boolean topLevel,
    String routeName         // existing data-route value
) {}
```

Vaadin research notes:
- Vaadin Flow 24.8 supports Java RouterLayout composition; use a server-side component shell rather than a React/Hilla rewrite.
- Keep `PiWebAppShell` as AppShellConfigurator/theme/meta owner; `PiResponsiveShell` owns visual chrome.
- App/layout CSS should keep `html, body, #outlet` full width/height; theme primitives are added in Plan 02.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Define route navigation contracts and registry</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiRouteNavItem.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiRouteNavRegistry.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java</files>
  <behavior>
    - Registry returns exactly eight current Phase 10 routes: console, admin/governance, overview, registry, operations, policy-decisions, audits, approvals.
    - Registry exposes Console and Admin as top-level product areas per D-02.
    - Admin Governance sub-navigation is grouped under Admin and includes Overview, Registry, Operations, Policy Decisions, Audits, and Approvals per D-06.
    - Matching by route path returns the correct title, nav label, routeName, and active item for shell rendering per D-08.
  </behavior>
  <action>Create the focused `WebResponsiveShellContractTest` method first, then implement `PiRouteNavItem` as a plain Java record and `PiRouteNavRegistry` as an adapter-web-only utility/service with immutable route metadata until the test passes. Do not add Domain/App/client DTO changes, mobile backend routes, or `/mobile/*` APIs per D-04. Include helper methods for `items()`, `topLevelItems()`, `adminItems()`, and `findByRoute(String route)` or equivalent so shell and tests share one source of truth per D-07.</action>
  <verify>
    <automated>./mvnw -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest#routeRegistryContainsConsoleAndAdminNavigation test</automated>
  </verify>
  <done>Registry compiles, has all eight route entries, keeps Console/Admin distinct, and contains no references outside adapter-web/Vaadin UI concerns.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Build shared responsive RouterLayout shell</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/PiResponsiveShell.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java</files>
  <behavior>
    - Shell root exposes `data-shell="pi-responsive-shell"`.
    - Navigation root exposes `data-nav="primary"` and every item exposes `data-nav-item` with route path or routeName.
    - Active route item exposes `data-nav-active="true"` and shell title exposes `data-page-title`.
    - Phone navigation has a compact header and hamburger/drawer trigger per D-05.
    - Closing drawer returns focus to the trigger via basic Java/Vaadin focus handling per D-12.
  </behavior>
  <action>Add the shell hook/title contract test first, then implement `PiResponsiveShell` as the single visual Vaadin RouterLayout for all Console/Admin routes per D-01 until it passes. Prefer a custom Java `Div`/`Header`/`Nav`/`Main` RouterLayout if it keeps selector/focus behavior explicit; Vaadin AppLayout is acceptable only if the stable hook and focus-return contracts remain testable. Render: compact header, hamburger drawer trigger, close control, grouped Console/Admin nav from `PiRouteNavRegistry`, current route title, optional status/action slot container, and content slot. Use plain Vaadin Java components; no frontend framework rewrite. Add stable data hooks required by D-17.</action>
  <verify>
    <automated>./mvnw -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest#sharedShellExposesStableHooksAndTitle test</automated>
  </verify>
  <done>`PiResponsiveShell` renders shell/header/nav/title/content hooks and can host routed content through `showRouterLayoutContent` without changing public REST/SSE DTOs.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Attach all routes to shared shell and retire competing layouts</name>
  <files>pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLayout.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/AdminGovernanceLandingView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/MainConsoleLayout.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminOperationsView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPolicyDecisionsView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminAuditView.java, pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminApprovalQueueView.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebResponsiveShellContractTest.java, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebMobileBaselineContractTest.java</files>
  <behavior>
    - `/console` and all seven Admin routes declare `layout = PiResponsiveShell.class`.
    - `/admin/governance` remains a landing route but no longer uses/acts as a pseudo-layout per D-03.
    - Route content keeps existing Phase 10 `data-route` markers and light smoke selectors.
    - No broad business-content rewrite is performed; Console flow and Admin card/detail migrations remain deferred per D-16.
  </behavior>
  <action>Add or update the route annotation/baseline compatibility tests first, then update every current route annotation to use the shared shell. Rename or replace `AdminGovernanceLayout` with `AdminGovernanceLandingView` so the route remains `/admin/governance` but is a normal page. Delete or empty-retire `MainConsoleLayout` if it is unused, and ensure no route points to it. Preserve all existing public API helper usage and route content behavior. If deleting classes is risky for imports, leave a minimal deprecated non-route class only if tests prove it is not a competing layout; otherwise remove it.</action>
  <verify>
    <automated>./mvnw -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest#allCurrentRoutesUseSharedShellAndKeepRouteHooks test</automated>
  </verify>
  <done>All current Console/Admin routes are under `PiResponsiveShell`, `/admin/governance` is a landing page, and no old layout abstraction competes with the shared shell.</done>
</task>

</tasks>

<verification>
Run the focused adapter-web shell contract suite after implementation:
```bash
./mvnw -pl pi-agent-adapter-web -Dtest=WebResponsiveShellContractTest test
```
Also run the Phase 10 route smoke listing to ensure route names remain discoverable:
```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --project="Mobile Chrome" --list
```
</verification>

<success_criteria>
- One shared shell owns Console/Admin visual chrome and stable navigation hooks.
- Route registry is the single source of truth for phone drawer and wider responsive navigation.
- All eight existing routes stay reachable at the same URLs and keep Phase 10 route markers.
- No Domain/App/client DTOs, backend APIs, or mobile-only server behavior are changed.
</success_criteria>

<output>
After completion, create `.planning/phases/11-shared-responsive-shell-and-navigation/11-shared-responsive-shell-and-navigation-01-SUMMARY.md`.
</output>
