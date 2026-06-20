# Stack Research: Mobile-First H5 Adaptation for Vaadin Flow

**Project:** Pi Java Agent Platform  
**Domain:** Mobile-first H5 adaptation of an existing Java/Spring/Vaadin Agent Web Console and Admin Governance app  
**Researched:** 2026-06-20  
**Confidence:** **HIGH** for Vaadin responsive APIs/patterns and Playwright emulation; **MEDIUM-HIGH** for exact patch versions because Vaadin/Playwright patch lines move quickly.

## Executive Recommendation

Do **not** add a separate frontend stack. The right stack for this milestone is the existing **Vaadin Flow + Spring Boot + Playwright Java** stack, upgraded/configured for mobile-first H5: Vaadin responsive layout components and Lumo utility/theme CSS for UI adaptation, custom app-level CSS media/container queries for reusable mobile building blocks, and Playwright Java browser contexts with mobile viewport/touch emulation for automated acceptance.

The key stack change is not a new framework; it is a **design-system and verification layer** inside the existing Vaadin app. Treat mobile as the default layout and progressively enhance for tablet/desktop. Use Vaadin components' built-in responsive behavior where it exists, supplement with CSS classes and reusable Java view components where it does not, and verify real product paths at mobile widths with `hasTouch`, `isMobile`, and no-horizontal-overflow assertions.

Recommended version posture: stay on **Vaadin 24.x** while the platform remains on **Spring Boot 3.5.x**. The current repo pins Vaadin **24.8.4**. Vaadin's roadmap/search results show **24.10.7** as the latest Vaadin 24 line and Vaadin 25 requiring Spring Boot 4.0.4+; therefore, upgrade within Vaadin 24 if dependency validation passes, but do **not** move to Vaadin 25 just for mobile H5.

## Recommended Stack Additions / Changes

### Core Technologies

| Technology | Version / Line | Purpose | Why Recommended |
|------------|----------------|---------|-----------------|
| Vaadin Flow | Existing: **24.8.4**; recommended: latest compatible **24.x** patch, currently observed **24.10.7** | Existing Java UI framework for Console/Admin | Keeps the all-Java UI boundary and avoids a React/Next.js rewrite. Vaadin docs explicitly cover mobile-first responsiveness, built-in responsive component behavior, CSS media/container queries, and Lumo utilities. |
| Vaadin Lumo Theme | Managed by Vaadin BOM | Design tokens, sizes, spacing, typography, utility classes | Lumo exposes CSS custom properties and mobile-first utility classes. It is the lowest-risk way to standardize touch spacing, responsive display/flex/grid utilities, and accessibility helpers in a Vaadin Flow codebase. |
| App-level CSS resources | Vaadin frontend resources; `@StyleSheet` / existing theme folder | Mobile layout primitives, breakpoints, safe scrolling, touch sizing | Mobile adaptation needs reusable CSS building blocks, not scattered inline Java styles. Use a small set of project CSS classes for page shells, cards, tool timelines, approval bars, responsive grids, and mobile drawers. |
| Playwright Java | Add/pin test dependency; Maven Central search verified **1.60.0** on 2026-05-19; check latest before implementation | Mobile viewport/browser smoke and product-path E2E | Existing project already has Playwright E2E fixtures but no visible Maven dependency in the root POM. Add an explicit test dependency and run mobile contexts for Chromium/WebKit/Firefox where CI supports them. |
| JUnit Jupiter + Spring Boot Test | Existing | Test runner and app lifecycle for browser E2E | Keep the existing Java test harness. Mobile E2E should be another profile/tag in the same test suite, not a separate Node test project. |

### Vaadin Responsive APIs and Patterns

| API / Pattern | Version | Purpose | When to Use |
|---------------|---------|---------|-------------|
| `AppLayout` + `DrawerToggle` + Side Navigation | Vaadin 24.x | Responsive global shell | Use for Console/Admin navigation. Vaadin docs state App Layout's navigation drawer can be overlay/bottom navbar and collapses to hamburger on small viewports. Preserve one app shell, but make drawer/primary nav mobile-aware. |
| `FormLayout#setResponsiveSteps(...)` | Vaadin 24.x | Mobile-first forms | Use for Admin governance forms, provider/MCP/plugin settings, approval dialogs. Define `0px = 1 column`, tablet/desktop = 2+ columns. Avoid multi-column forms on phones. |
| `FlexLayout`, `HorizontalLayout`, `VerticalLayout` with wrapping | Vaadin 24.x | Responsive stacks/toolbars/cards | Use for page headers, action bars, card decks, tool metadata chips. Prefer mobile default vertical stacks, then switch to row layouts at wider breakpoints via CSS/classes. |
| `Scroller` | Vaadin 24.x | Constrained scroll regions | Use for chat transcript, run timeline, audit/event lists, and side panels. Prevent the whole viewport from becoming an uncontrolled nested scroll trap. |
| `Grid` responsive column strategy | Vaadin 24.x | Dense admin data on narrow screens | Do not try to show desktop tables on phones. On mobile, hide low-priority columns, use details rows/card renderers for secondary fields, and provide search/filter overlays. Use full Grid for tablet/desktop. |
| `VirtualList` / card list pattern | Vaadin 24.x | Mobile-friendly lists | Use for Session history, Agent catalog, MCP servers, plugins, audit summaries when list items are better represented as cards than tables. |
| `Dialog` / `ConfirmDialog` | Vaadin 24.x | Mobile approvals and confirmations | Vaadin dialogs adapt button toolbar vertically when space is limited. Use for approvals and destructive admin actions, but avoid complex desktop-style forms inside modal dialogs on phones. |
| `Tabs` / `MenuBar` overflow behavior | Vaadin 24.x | Compact navigation/actions | Vaadin tabs can scroll horizontally and menu bars overflow. Use carefully; avoid large tab bars on phones when a segmented/list navigation is clearer. |
| Vaadin field types (`EmailField`, `NumberField`, `PasswordField`, etc.) | Vaadin 24.x | Mobile keyboards and input behavior | Use Vaadin typed fields instead of generic text fields where possible so mobile OS keyboards and browser behaviors are appropriate. |

### CSS and Theme Resources

| Resource / Technique | Recommendation | Why |
|----------------------|----------------|-----|
| Lumo stylesheet | Keep/use Lumo; current Vaadin docs show `@StyleSheet(Lumo.STYLESHEET)` | Lumo is already the Vaadin default design language and exposes component style properties suitable for consistent mobile sizing/spacing. |
| Lumo Utility Classes | Load with `@StyleSheet(Lumo.UTILITY_STYLESHEET)` when using current Vaadin stylesheet-loading style; use `LumoUtility` constants in Java | Official Vaadin docs describe Lumo utilities as mobile-first with breakpoints such as Small `640px` and Medium `768px`. They are useful for layout/native HTML/simple Vaadin layout components. |
| Project CSS files | Add/organize app CSS under the existing Vaadin resource convention already used by the project; if using current `@StyleSheet`, include `styles.css`; if using a theme folder, keep additions there | The milestone needs repeatable primitives: `.mobile-shell`, `.responsive-toolbar`, `.card-list`, `.timeline-card`, `.approval-sticky-bar`, `.admin-filter-drawer`, `.no-horizontal-overflow`. Avoid view-specific one-off CSS. |
| CSS media queries | Use mobile default styles, then `@media (min-width: 640px)`, `768px`, `1024px` for enhancements | Vaadin docs recommend mobile-first when mobile is important. Use min-width breakpoints so phone layout remains the base. |
| CSS container queries | Use for cards/panels whose available width differs from viewport width | Vaadin docs call out container queries for resizable content areas. Useful for split panes, drawer content, timeline cards, and grid/detail regions. |
| `@media (pointer: coarse)` | Increase Lumo sizes/touch spacing for touch devices | Vaadin size/space docs recommend controlling sizing through Lumo custom properties and show increasing `--lumo-size-*` under coarse pointers. |
| Viewport/full-height CSS | Ensure `html, body, #outlet` remain `height: 100%; width: 100%; margin: 0`; consider dynamic viewport units for mobile-specific panels | Vaadin Flow's bootstrap CSS includes full-height/full-width defaults and an iOS standalone `100lvh` fix. Mobile chat/timeline layouts depend on correct viewport height handling. |
| Shadow DOM styling | Prefer normal document-scope CSS and component style properties/parts; avoid legacy `@CssImport(themeFor=...)` patterns unless already required | Current Vaadin styling docs state older component shadow-DOM injection approaches are still supported but no longer recommended. Keep mobile styling maintainable and forward-compatible. |

### Development / Test Tools

| Tool | Purpose | Configuration Tips |
|------|---------|--------------------|
| Playwright Java browser contexts | Mobile smoke and E2E without Node test stack | Create contexts with `setViewportSize`, `setDeviceScaleFactor`, `setIsMobile(true)`, `setHasTouch(true)`, and realistic user agents/devices where useful. Official Playwright Java docs confirm these options emulate user agent, screen size, viewport, touch, locale/timezone, permissions, and color scheme. |
| Playwright Chromium | Android/Chrome-like mobile baseline | Run for every PR because it is usually the most stable headless mobile emulation target in Linux CI. |
| Playwright WebKit | iOS Safari risk proxy | Run in CI if the environment supports Playwright WebKit. Use it as the best automated proxy for iOS Safari behavior, but keep a manual/human UAT note for real iOS Safari before release if CI cannot run WebKit reliably. |
| Playwright Firefox | Firefox mobile viewport/touch smoke | Run a smaller smoke set if full mobile E2E is too slow. Vaadin supports Firefox desktop evergreen/ESR; mobile Firefox support should be validated as product target, not assumed from Vaadin's official supported browser matrix. |
| Screenshot-on-failure | Debug narrow viewport regressions | Save screenshots/video/traces for mobile failures. Responsive bugs are often visual overflow/hidden action problems, not Java exceptions. |
| CSS overflow assertions | Catch H5 regressions automatically | Add assertions such as `document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1` for key pages at phone widths. |
| Accessibility/touch assertions | Verify mobile usability | Check visible/focusable primary actions, keyboard navigation for controls, tap target sizes for custom controls, and no hover-only functionality. |

## Installation / Configuration Sketch

### Maven Properties

Current root POM pins `vaadin.version` to `24.8.4`. For this milestone:

```xml
<properties>
  <!-- Existing Boot 3.5.x line stays. -->
  <spring-boot.version>3.5.9</spring-boot.version>

  <!-- Keep Vaadin 24.x for Spring Boot 3.5 compatibility. Validate latest 24.x patch before changing. -->
  <vaadin.version>24.10.7</vaadin.version>

  <!-- Add explicit Playwright Java test version. Maven Central search verified 1.60.0 on 2026-05-19. -->
  <playwright.version>1.60.0</playwright.version>
</properties>
```

If upgrading Vaadin introduces churn, it is acceptable to deliver the mobile milestone on **24.8.4** because the needed responsive APIs already exist. Upgrade within 24.x is recommended for browser fixes and supported-technology updates, not because mobile-first requires a new major version.

### Test Dependency

Add Playwright explicitly, probably in `pi-agent-adapter-web` test scope because that module owns the Vaadin UI and existing Playwright readiness fixture:

```xml
<dependency>
  <groupId>com.microsoft.playwright</groupId>
  <artifactId>playwright</artifactId>
  <version>${playwright.version}</version>
  <scope>test</scope>
</dependency>
```

Do not add Playwright to Domain/App/Infrastructure modules. It is an adapter-web E2E concern only.

### Vaadin Stylesheet Loading

Use the style-loading mechanism already present in the app. For current Vaadin docs style:

```java
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.StyleSheet;
import com.vaadin.flow.theme.lumo.Lumo;

@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
public class PiWebAppShell implements AppShellConfigurator {
}
```

If the existing app uses a Vaadin theme folder instead, do not churn solely to switch annotations. Put the mobile CSS into the existing theme and document the convention. The important requirement is one canonical place for responsive primitives.

### Example Mobile-First CSS Tokens

```css
/* Mobile defaults */
.pi-page {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  padding: var(--lumo-space-s);
}

.pi-responsive-toolbar {
  display: flex;
  flex-direction: column;
  gap: var(--lumo-space-s);
}

.pi-card-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--lumo-space-s);
}

@media (min-width: 640px) {
  .pi-responsive-toolbar {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

@media (min-width: 768px) {
  .pi-card-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1024px) {
  .pi-page {
    padding: var(--lumo-space-l);
  }
}

@media (pointer: coarse) {
  html {
    --lumo-size-xl: 4rem;
    --lumo-size-l: 3rem;
    --lumo-size-m: 2.5rem;
    --lumo-size-s: 2rem;
    --lumo-size-xs: 1.75rem;
  }
}
```

The `@media (pointer: coarse)` sizing pattern is directly aligned with Vaadin size/space guidance for touchscreens.

### Example Playwright Java Mobile Context

```java
try (Playwright playwright = Playwright.create()) {
    Browser browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(true));
    BrowserContext context = browser.newContext(new Browser.NewContextOptions()
            .setViewportSize(390, 844)       // iPhone-class portrait viewport
            .setScreenSize(390, 844)
            .setDeviceScaleFactor(3)
            .setIsMobile(true)
            .setHasTouch(true)
            .setColorScheme(ColorScheme.LIGHT));
    Page page = context.newPage();
    page.navigate(baseUrl + "/console");
    // Assert key action is visible, no horizontal overflow, and product path works.
}
```

Run at least these viewport classes:

| Class | Suggested Size | Browser Context | Why |
|-------|----------------|-----------------|-----|
| Narrow phone portrait | `360x740` or `375x667` | Chromium mobile + touch | Catches worst-case width and old/small phones. |
| Modern phone portrait | `390x844` or `393x873` | Chromium mobile + WebKit where available | Common iPhone/Android-class H5 shape. |
| Phone landscape | `844x390` | Chromium mobile + touch | Catches short-height chat/timeline/action bar issues. |
| Small tablet | `768x1024` | Chromium/WebKit | Ensures mobile/tablet breakpoints do not leave tablet in awkward desktop state. |
| Desktop regression | Existing desktop viewport | Existing tests | Confirms mobile-first refactor did not break validated desktop flows. |

## Integration Points in Existing App

| Existing Area | Stack Integration | Recommendation |
|---------------|------------------|----------------|
| `pi-agent-adapter-web` | Vaadin views, Spring Boot app, E2E fixture | Keep all mobile UI code here. Add CSS resources and Playwright mobile tests here; do not leak UI concerns into public client DTOs or app/domain layers. |
| App shell/navigation | Vaadin `AppLayout`, `DrawerToggle`, `SideNav`/navigation components | Rework navigation mobile-first: hamburger/drawer or bottom navigation for high-frequency Console actions; Admin pages can remain drawer-first but must be reachable and usable. |
| Agent Catalog | Card/grid CSS + optional `VirtualList` | Mobile default should be single-column cards with primary CTA visible. Desktop can use multi-column grid. |
| Chat/Run | `Scroller`, sticky composer/action bar, SSE event cards | Optimize for vertical reading and one-thumb actions. Avoid side-by-side transcript/timeline on phones. |
| Run Timeline / Tool Cards / Approval Cards | Card list + expandable details | Replace dense desktop panels with progressive disclosure. Critical status/action must be visible without horizontal scroll. |
| Session history | Search + card/list pattern | Avoid desktop table on phones. Use compact cards with last activity and resume action. |
| Admin Governance pages | Responsive forms, Grid-to-card/detail strategy, filter drawer | Admin can be slightly denser than Console, but every critical governance action must be touch-completable. Hide secondary columns/actions behind details/overflow menus. |
| REST/SSE DTO boundaries | No change | Mobile H5 is a presentation-layer adaptation. Do not fork APIs or introduce mobile-specific DTOs unless a proven performance issue emerges. |
| Playwright E2E | Add mobile-tagged smoke/product tests | Reuse existing no-key fake model/tool/MCP/plugin fixtures. Add viewport/touch matrix and overflow assertions. |

## Accessibility and Touch Considerations

| Concern | Recommendation | Why |
|---------|----------------|-----|
| Tap targets | Ensure custom buttons, icon-only actions, timeline affordances, drawer items, and approval actions have comfortable touch area; use Lumo sizes and padding rather than tiny custom CSS | Mobile usability fails quickly when actions are visually present but hard to tap. Vaadin docs recommend larger elements for touchscreens. |
| No hover-only behavior | Every tooltip/menu/secondary action must have tap and keyboard access | Phones do not have hover; hiding tool actions behind hover breaks H5. |
| Focus and keyboard | Preserve tab order, focus indicators, Enter/Escape behavior in dialogs/drawers, and screen-reader labels for icon buttons | Vaadin components provide accessibility foundations, but custom wrappers/cards need explicit labels and semantics. |
| Screen reader helper text | Use Lumo accessibility utilities such as screen-reader-only where appropriate | Lumo utility docs include accessibility helpers; use them for hidden labels and status context. |
| Motion and live updates | Keep SSE/timeline updates readable; avoid auto-scrolling away from user context unless the user is at the bottom | Chat/run views with streaming events can become unusable on mobile if the viewport jumps unexpectedly. |
| Color/contrast/dark mode | Test light and dark if dark mode is supported; avoid color-only status | Lumo supports theme tokens; status should include text/icon labels. |
| Safe area and sticky bars | Test iOS Safari/WebKit-like contexts for sticky composer and bottom approval actions | Bottom browser chrome and dynamic viewport heights are common H5 failure points. |

## Alternatives Considered

| Recommended | Alternative | Why Not / When Alternative Makes Sense |
|-------------|-------------|----------------------------------------|
| Vaadin Flow responsive refactor | React/Next.js mobile frontend | Not appropriate for this milestone. It violates the no React/Next.js constraint, duplicates API/client state, and creates a second frontend platform for a UI that Vaadin can adapt. |
| Vaadin 24.x latest patch | Vaadin 25 | Vaadin 25 search evidence indicates Spring Boot 4.0.4+ and Java 21 baseline. The current stack is Boot 3.5.x; major upgrade adds unrelated framework migration risk. Revisit in a later platform upgrade milestone. |
| Lumo utilities + project CSS | Tailwind CSS in Vaadin | Vaadin docs list Tailwind support as experimental and mutually exclusive with Lumo utility classes. It adds utility-class churn and potentially a different design language; use Lumo for this app. |
| Mobile browser emulation with Playwright Java | Node Playwright Test project | Java Playwright integrates with existing Maven/JUnit/Spring fixtures and avoids adding Node test orchestration. Node Playwright is excellent generally but unnecessary here. |
| Responsive single Vaadin UI | Separate mobile-only routes/UI | Separate UI doubles view code and creates divergence. Use separate mobile-specific components only for isolated cases where responsive design is clearly insufficient. |
| Vaadin typed components | Native HTML inputs | Vaadin docs recommend Vaadin fields over native inputs when min/max/disabled options/logic matter; typed Vaadin fields still trigger appropriate mobile keyboards where supported. |

## What NOT to Add

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| React, Next.js, Vite React app, Hilla React frontend | Creates a second frontend architecture and violates milestone constraints. Mobile-first H5 can be done in Vaadin Flow. | Vaadin Flow views + Lumo/project CSS + Playwright mobile E2E. |
| Native iOS/Android app, Capacitor, Cordova, Flutter, React Native | This milestone is H5 adaptation, not app-store/mobile-app delivery. Native wrappers hide web responsiveness problems instead of fixing them. | Responsive web app tested in mobile browser contexts. |
| Tailwind CSS | Vaadin docs say Tailwind support is experimental and mutually exclusive with Lumo utilities. It is not needed for this Java-first app. | Lumo Utility Classes + small project CSS classes. |
| Vaadin 25 migration | Pulls Spring Boot 4/Jackson 3 ecosystem migration into a mobile UI milestone. | Stay on Vaadin 24.x; upgrade only to latest compatible 24 patch. |
| Desktop tables squeezed onto phone screens | Horizontal scroll makes admin and run views unusable on H5. | Card/detail layouts, hidden low-priority columns, filter drawers, progressive disclosure. |
| Hover-dependent toolbars | Breaks touch devices. | Always-visible primary actions and tap-accessible overflow menus. |
| CSS-only patching without IA/layout refactor | The project requirement is mobile-first information architecture, not minor responsive tweaks. | Reusable mobile page templates and component-level layout variants. |
| Mobile-specific REST/SSE DTOs by default | Adds API divergence and risks breaking public client boundaries. | Keep current DTOs; optimize UI rendering and pagination/filtering first. |
| Visual-only testing | Responsive regressions often pass server tests. | Playwright product-path tests with mobile viewport/touch and overflow assertions. |

## Stack Patterns by Variant

**Console user flows (Agent Catalog, Chat/Run, Session history):**
- Use single-column card/list layouts by default.
- Use sticky bottom composer or primary action zones where needed.
- Use `Scroller` for transcript/timeline regions.
- Because these are high-frequency mobile workflows and must feel natural on phones.

**Admin Governance flows:**
- Use mobile filter drawers, one-column forms, expandable cards/details for Grid rows, and explicit overflow action menus.
- Keep full Grids only for tablet/desktop widths.
- Because admin pages contain dense data, but critical actions still need H5 operability.

**Run timeline/tool execution views:**
- Use event cards with severity/status badges and expandable payload sections.
- Make approval cards sticky/visible when waiting for user input.
- Because agent runs are event streams; mobile users need status/action clarity more than raw density.

**Cross-browser verification:**
- Chromium mobile context on every PR.
- WebKit mobile context in CI if supported, otherwise scheduled/manual gate for release.
- Firefox mobile-width smoke for product target validation.
- Because Playwright emulates many device properties, but real iOS Safari remains the highest-risk H5 browser.

## Version Compatibility

| Package / Tool | Compatible With | Notes |
|----------------|-----------------|-------|
| Vaadin 24.x | Spring Boot 3.5.x, Java 17+; project uses Java 21 | Vaadin 24.10 release notes/search evidence list Spring Boot 3.5+ from the 3.x series and Java 17+. Current project Java 21 is fine. |
| Vaadin 25 | Spring Boot 4.0.4+, Java 21+ | Do not adopt in this milestone; it is a platform upgrade, not an H5 adaptation prerequisite. |
| Lumo Utility Classes | Lumo theme | Vaadin docs: Lumo utilities work with Lumo, not Aura; Tailwind and Lumo utilities are mutually exclusive. |
| Playwright Java 1.60.0 | Java test runtime, Maven/JUnit | Maven Central search verified 1.60.0; Playwright docs also show release notes beyond 1.60, so check Maven Central before pinning in implementation. |
| Playwright WebKit | CI OS/browser dependencies | Use as iOS Safari proxy, but keep real-device/manual UAT if CI cannot reliably run WebKit. |

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Vaadin responsive APIs/patterns | HIGH | Verified through Context7 and official Vaadin responsiveness docs: mobile-first guidance, AppLayout, FormLayout, dialogs, tabs, media/container queries, Lumo utilities. |
| CSS/theme loading details | MEDIUM-HIGH | Current Vaadin docs emphasize `@StyleSheet`; existing Vaadin 24 apps may still use theme folders. Recommendation is intentionally compatible with either convention. |
| Vaadin version target | MEDIUM-HIGH | Current repo has 24.8.4. Search evidence shows 24.10.7 latest Vaadin 24 and Vaadin 25 requiring Boot 4. Validate exact patch in Maven before implementation. |
| Playwright mobile emulation | HIGH | Official Playwright Java docs verify `viewport`, `screenSize`, `deviceScaleFactor`, `isMobile`, `hasTouch`, `userAgent`, locale/timezone/colorScheme support. |
| Real mobile browser parity | MEDIUM | Emulation catches most layout/touch issues but cannot fully replace real iOS Safari/Android browser UAT for viewport chrome and OS behavior. |

## Sources

- Context7 `/vaadin/flow` — Vaadin Flow CSS/resource loading, full-height bootstrap CSS, iOS standalone height fix.
- Context7 `/websites/vaadin` — Vaadin responsiveness, responsive component behaviors, FormLayout responsive steps, Lumo utility classes.
- Vaadin official docs, Responsiveness, updated 2025-03-24: https://vaadin.com/docs/latest/designing-apps/responsiveness — mobile-first guidance, media/container queries, AppLayout/FormLayout/Dialog/Tabs/Menu responsive behavior, Lumo mobile-first breakpoints.
- Vaadin official docs, Utility Classes, updated 2025-12-18: https://vaadin.com/docs/latest/styling/utility-classes — Lumo utility loading with `@StyleSheet(Lumo.UTILITY_STYLESHEET)`, Tailwind experimental, Tailwind and Lumo utilities mutually exclusive.
- Vaadin official docs/search result, Size and Space: https://vaadin.com/docs/latest/designing-apps/size-space — touch sizing via Lumo custom properties and `@media (pointer: coarse)`.
- Vaadin roadmap/search evidence: https://vaadin.com/roadmap — Vaadin 24 stable production line, latest 24.10.7 observed; Vaadin 25/Spring Boot 4 evidence from Vaadin platform releases search.
- Playwright Java official docs, Emulation: https://playwright.dev/java/docs/emulation — `setViewportSize`, `setScreenSize`, `setDeviceScaleFactor`, `setIsMobile`, `setHasTouch`, locale/timezone/permissions/colorScheme.
- Context7 `/microsoft/playwright-java` — Java browser launch/context examples and cross-browser screenshots.
- Maven Central search evidence for `com.microsoft.playwright:playwright` — version 1.60.0 observed 2026-05-19; verify latest before final pinning.

---
*Stack research for: mobile-first H5 support in existing Vaadin Flow app*  
*Researched: 2026-06-20*
