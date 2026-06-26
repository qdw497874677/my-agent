# Phase 12: Console Mobile-First Flow - Research

**Researched:** 2026-06-23  
**Domain:** Vaadin Flow 24 mobile Console layout, sticky chat composer, SSE/run controls, Playwright mobile E2E  
**Confidence:** HIGH for project/code constraints and verification shape; MEDIUM-HIGH for Vaadin mobile CSS/component tactics

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
## Implementation Decisions

### Chat-First Mobile Layout
- **D-01:** On phones, the default Console view must be **Chat Feed first**. Users should land in the chat/event stream as the primary workflow, not in Catalog or Session history.
- **D-02:** Agents, Sessions, and Run Context should be reachable through a touch-safe in-page segmented switcher/tab pattern, rather than relying only on the global shell drawer or desktop-style side columns.
- **D-03:** When a secondary panel is opened, the implementation may use either overlay/sheet behavior or inline/down-push expansion, but it must preserve Chat state. Returning to Chat must keep the current feed/session/input context rather than resetting the flow.
- **D-04:** Wider tablet/desktop layouts should restore a responsive multi-column Console where appropriate, such as Sessions/Catalog, Chat/Event Feed, and Run Context columns. Phase 12 should not degrade desktop Console efficiency while making the phone flow single-primary.

### Composer and Run Controls
- **D-05:** The mobile Chat composer must be bottom-sticky or otherwise persistently reachable while users scroll the event feed. The current normal-flow composer behavior is not sufficient for Phase 12.
- **D-06:** The composer must support multi-line prompts with bounded/limited auto-growth. When input exceeds the maximum comfortable height, the TextArea should scroll internally instead of pushing the feed and controls off screen.
- **D-07:** Active run state should be shown inline near the composer, covering queued/running/cancelling/terminal feedback so users understand what Send/Cancel will do without scanning the whole page.
- **D-08:** Active run cancellation should use a **dual-position** pattern: a primary touch-safe Cancel affordance near the composer/run controls plus a backup visible affordance in the shell/page status/action area where practical. Users should not lose access to Cancel while scrolling live output.

### Agent Catalog and Session Flow
- **D-09:** Mobile Agent Catalog should render as full-width stacked Agent cards. Reuse the existing AgentCard/Catalog model where possible, but the mobile presentation must not depend on desktop-width columns.
- **D-10:** The General Agent card should expose a prominent Start/Continue primary CTA. Other entry actions may remain available but should be visually secondary on phones.
- **D-11:** Mobile Session history should show enough information to choose safely: short title/summary, recent run/session status, updated time, and a clear active-session highlight.
- **D-12:** Selecting a historical session should return the user to Chat, load/show the session history, and make the active session identity clear. Users should be able to continue from that state without an extra route or desktop-only detail page.

### SSE Feed and Event Scope
- **D-13:** The event stream should be a vertical mobile feed that can show live/incremental SSE output and prior events while preserving access to composer and cancellation controls.
- **D-14:** Phase 12 should verify and improve feed placement/scroll behavior, but it should avoid a full redesign of individual runtime/tool/approval/dense-detail cards. Detailed runtime cards, tool cards, approval cards, dialogs, and safe detail expansion remain Phase 13.
- **D-15:** Streamed event UI should prove at least meaningful status/model/terminal or cancellation progression in mobile flow. It does not need to assert every event category or tool-card interior in this phase.

### Verification and Regression Gates
- **D-16:** Phase 12 must add a full Console mobile product-path E2E: open Console, select/start General Agent, enter a multi-line prompt, submit, observe streamed event UI, open/use session/history surfaces, and either cancel an active run or reach terminal status.
- **D-17:** The MVER-03 browser gate should run the main Console path on a representative mobile/tablet matrix: at minimum Mobile Chrome, Mobile Safari/WebKit proxy, and Tablet. Mobile Firefox may run smoke/viewport/no-overflow coverage if full SSE/composer behavior is too flaky in CI.
- **D-18:** E2E should assert event feed visibility and incremental behavior, plus that scrolling prior events does not remove practical access to composer and Cancel. It should not require all Phase 13 card details.
- **D-19:** Preserve or update desktop Console regression coverage after the mobile-first refactor. Phase 12 changes Console core behavior, so waiting until Phase 15 for all desktop regression would be too risky.
- **D-20:** Keep tests deterministic/no-key using the existing fake runtime, public REST/SSE APIs, stable `data-*` selectors, and Playwright helper patterns from Phases 10-11.

### Folded Todos
- No pending todos matched Phase 12 scope.

### the agent's Discretion
- Exact segmented control styling, labels, iconography, breakpoint values, and whether secondary panels are implemented as sheets, drawers, details, tabs, or inline regions are planner/designer discretion as long as D-01 through D-04 hold.
- Exact sticky composer CSS mechanics, TextArea max-height, and safe-area handling are implementation discretion, provided the composer remains usable at representative phone/tablet viewports.
- Exact distribution of the dual Cancel controls between composer, page status slot, and shell action slot is implementation discretion, provided mobile users have visible touch-safe cancellation while a run is cancellable.
- Exact fake run fixture timing/event sequence is planner discretion, provided MVER-03 proves a real browser-visible Console loop and remains deterministic/no-key.

### Deferred Ideas (OUT OF SCOPE)
## Deferred Ideas

- Full runtime timeline event card redesign, tool card detail redesign, approval risk-first card redesign, dialogs/drawers/notification confirmation UX, and dense redacted detail expansion — Phase 13.
- Full Admin Governance mobile card/detail conversion across overview, registry, operations, MCP, plugin, extension, policy, and audit — Phase 14.
- Broad orientation/cross-browser/accessibility hardening, final desktop/mobile regression expansion, and real-device/UAT release documentation — Phase 15.
- Native app, PWA/offline behavior, push/background monitoring, deep-linkable mobile incident triage, event filtering/copy/share, and new mobile-only Agent capabilities — future/out of scope for v1.1 unless a later roadmap adds them.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| MCON-01 | Mobile user can browse Agent Catalog as stacked cards and select/start the General Agent without desktop-width layout. | Reuse `AgentCatalogPanel`/`AgentCard`; add mobile full-width card CSS and prominent `cloud-general-agent` Start/Continue CTA hooks. |
| MCON-02 | Mobile user can type a multi-line chat prompt, submit it, and see active run/composer state in a mobile-first Chat/Run flow. | Vaadin TextArea supports `setMinRows`/`setMaxRows`; implement sticky composer wrapper with inline run status and send/cancel state. |
| MCON-03 | Mobile user can observe live SSE run output/events in a vertical feed and scroll previous events without losing access to current run controls. | Add explicit feed/composer containers in `ChatEventStreamPanel`; make feed vertical scroll region and composer sticky with safe-area padding. |
| MCON-04 | Mobile user can open session history, select a past session, continue it, and clearly see the active session. | Extend `SessionListPanel` cards with summary/status/updated-time/active attributes; selecting a session should switch in-page state back to Chat and preserve input/feed. |
| MCON-05 | Mobile user can cancel an active run from a visible touch-safe control and see terminal/cancelling feedback. | Preserve `RunContextPanel` cancel path; duplicate visible cancel near composer plus backup page/shell action/status slot when cancellable. |
| MVER-03 | Console mobile E2E starts a fake/no-key run, observes streamed event UI, opens tool/approval/session areas, and cancels or reaches terminal status. | Add `e2e/phase-12-console-mobile-flow.spec.ts` using existing Playwright projects, fake-runtime dev headers, stable selectors, no-overflow, tap target, and scroll assertions. |
</phase_requirements>

## Summary

Phase 12 should be planned as an adapter-web-only Vaadin Flow mobile UX refactor of the existing Console, not as a backend/API or frontend-stack change. The current `/console` implementation already has the correct public REST/SSE seams (`ConsoleHttpClient`, `EventStreamClient`, `planChatSubmission`, `selectSession`, `planCancelRunningRun`) and component boundaries (`AgentCatalogPanel`, `SessionListPanel`, `ChatEventStreamPanel`, `RunContextPanel`), but its DOM and CSS still reflect a desktop workbench: sessions/catalog render before chat, the composer is in normal flow, run cancellation is isolated in the right column, and the chat stream has no dedicated mobile feed/composer/status structure.

The best plan is to add a mobile Console view-state layer inside `ConsoleView` while preserving the same route and components: Chat is the default phone panel; Agents, Sessions, and Run Context are in-page segmented secondary panels; tablet/desktop restore multi-column layout. CSS should live in `pi-mobile/styles.css` using safe-area tokens and stable `data-*` hooks. Java contract tests should lock these hooks before browser E2E asserts geometry and behavior.

**Primary recommendation:** Refactor `ConsoleView` into a Chat-first responsive Console shell with explicit panel switcher, sticky bounded composer, inline run status, dual cancel controls, stacked catalog/session cards, and a deterministic Playwright mobile product-path spec reusing the Phase 10-11 harness.

## Project Constraints (from project instructions / CLAUDE.md)

- Java/Vaadin-first: keep production UI in Vaadin Flow Java plus `pi-mobile` theme; do not add React/Next/Hilla React/native/PWA scope.
- COLA/project boundary: UI work belongs in `pi-agent-adapter-web`; Domain/App/public DTOs must remain free of Vaadin, Playwright, responsive theme, and viewport-specific concerns.
- Public API boundary: do not create mobile-only REST/SSE DTOs or `/mobile/*` API forks; consume existing public APIs/read models.
- Cloud safety: run/tool/cancel/approval surfaces must remain policy/audit-aware and deterministic; do not bypass existing cancellation/approval public API paths.
- Workspace/runtime boundary: Phase 12 is UI adaptation and verification only; no new model/tool/runtime capability scope.
- Verification: every phase must have automated gates; use fake runtime/no-key browser verification and stable `data-*` selectors.
- AGENTS.md: none found in `/root/workspace/pi-java`.
- Project skills: no `.claude/skills/` or `.agents/skills/` directories found.

## Standard Stack

### Core

| Library / Component | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Java | 21.0.11 available; project `maven.compiler.release=21` | Vaadin Flow UI/component implementation | Existing project baseline and enterprise Java-first constraint. |
| Vaadin Flow / Vaadin BOM | Project pinned `24.8.4`; npm latest observed `@vaadin/bundles 24.9.17` published 2026-06-22 | Console components, TextArea, Buttons, server-side UI state | Existing production UI framework; Context7 official docs confirm Flow styling and TextArea row constraints. Keep pinned version unless a separate dependency upgrade is planned. |
| Spring Boot / Vaadin Spring | Project pinned Spring Boot `3.5.9` | Web app hosting and Vaadin integration | Existing adapter-web stack; no Phase 12 reason to change. |
| `pi-mobile` Vaadin theme | Project-owned | Responsive CSS, tap targets, focus, safe area, overflow | Phase 10 established this as the mobile theme owner; Phase 12 should extend it rather than add inline styles or a new theme. |
| Playwright Test | Installed `1.57.0`; npm latest observed `1.61.0` published 2026-06-22 | Mobile/tablet/desktop browser E2E | Existing no-key browser harness; official docs support project/device emulation for Mobile Chrome and Mobile Safari. Keep current installed version unless planning dependency update. |
| JUnit Jupiter + AssertJ | JUnit `5.10.3`, AssertJ `3.26.3` | Java component/contract tests | Existing fast gate for Vaadin component data hooks and CSS token contracts. |

### Supporting

| Library / Component | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `ConsoleHttpClient` | Project-owned | Public REST path/DTO boundary for agents, sessions, runs, cancellation, approvals | Continue using from Console UI; do not duplicate URL building in tests/components. |
| `EventStreamClient` | Project-owned | SSE stream URL construction with replay (`afterSequence`) | Use for live feed connection planning and tests; keep route `/api/sessions/{sessionId}/runs/{runId}/stream`. |
| `e2e/fixtures/mobile-smoke.ts` | Project-owned | No-overflow, tap-target, focus, stable selector helpers | Extend/reuse in Phase 12 E2E; do not duplicate helper logic. |
| `e2e/fixtures/fake-runtime.ts` | Project-owned | Dev headers, fake run/session/cancel API helpers | Reuse for deterministic no-key run setup; may need UI-facing helper additions for slow/cancellable runs. |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| CSS/Java in existing Vaadin components | React/Next/Hilla React | Explicitly out of scope; adds frontend stack and breaks Java/Vaadin-first constraint. |
| In-page segmented panel state | Global shell drawer only | Violates D-02; shell drawer navigates routes, not Console sub-workflow panels. |
| Existing `RunContextPanel` only for Cancel | Single cancel in right/secondary panel | Violates D-08 on phones because user can lose cancel while scrolling feed. |
| Full redesign of tool/approval cards | Phase 13 runtime card migration | Out of scope for Phase 12; only preserve reachability/placement and minimal streamed event visibility. |
| Exhaustive real-device matrix | Representative Playwright matrix | Phase 15 owns real-device/UAT and orientation hardening; Phase 12 should use Mobile Chrome, Mobile Safari, Tablet plus optional Mobile Firefox smoke. |

**Installation:** No new production dependency is required. Existing E2E tooling is already present. If a fresh checkout is used:

```bash
npm install
npm run e2e:install -- --with-deps=false
```

**Version verification performed:**

```bash
npm view @playwright/test version          # 1.61.0 latest, installed 1.57.0
npm view @playwright/test time.modified    # 2026-06-22T14:50:35.168Z
npm view @vaadin/bundles version           # 24.9.17 latest, project pinned 24.8.4
npm view @vaadin/bundles time.modified     # 2026-06-22T07:55:12.878Z
npx playwright --version                   # Version 1.57.0
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn --version
```

**Recommendation on versions:** Do **not** upgrade Vaadin or Playwright inside Phase 12 unless explicitly planned. This is a UX refactor with verification pressure; dependency upgrade churn should be separate.

## Current Console Inventory

| Area | Current Behavior Found | Phase 12 Implication |
|------|------------------------|----------------------|
| `ConsoleView` layout | Adds `sessionListPanel`, `chatPanel`, `runContextPanel` in that order; root has `data-layout="three-column-workbench"`; `columnOrder()` returns `sessions`, `chat-event-stream`, `run-context`. | Phone DOM/CSS order currently risks Sessions/Catalog before Chat; planner must add Chat-first mobile ordering/state without breaking desktop column order. |
| Agent catalog | `AgentCatalogPanel` lives inside `SessionListPanel`; cards have `data-agent-id`, `data-action="choose-agent"`, entry action buttons with `data-entry-action`. | Reuse cards but add stacked mobile card styles and prominent General Agent Start/Continue hook. Consider separating catalog from sessions in mobile panel switcher while preserving same model. |
| Chat composer | `ChatEventStreamPanel` directly adds `H2`, stream `Div`, `TextArea`, `Button`; input has `data-role="chat-input"`; send has `data-action="send-chat"`. | Needs explicit feed/composer wrappers (`data-role="event-feed"`, `data-role="chat-composer"`, `data-role="composer-run-status"`) and sticky CSS. |
| TextArea | Placeholder only; no `setMinRows`/`setMaxRows`, no wrapper, no CSS max-height. | Use Vaadin `TextArea.setMinRows(...)` and `setMaxRows(...)` or CSS on `vaadin-text-area`; Context7 docs confirm row constraints. |
| Run status/cancel | `RunContextPanel` owns status text and visible `data-action="cancel-run"` button only in right panel. | Add composer-near cancel and backup shell/page action slot/status; keep `planCancelRunningRun` path. |
| Session list | Rows are `Div` with `role="button"`, `data-session-id`, text combines id/title/updatedAt; selected row text prefixed with `▶`. | Needs mobile card semantics, active attribute (`data-session-active="true"`), status/updated labels, and click/tap flow returning to Chat. |
| CSS | At max-width 640px `.pi-console-workbench` becomes one-column, but children remain in DOM order; no sticky composer/feed containment rules. | Add Console-specific breakpoints, grid areas/order, `position: sticky` composer, safe-area bottom padding, and no-overflow guards. |
| Shell | `PiResponsiveShell` has private status/action slots with `data-page-status="shell"` and `data-primary-action="shell-action-slot"`. | Use where practical for backup cancel/status. Current API may need a safe method/event mechanism to populate slots from route content. |

## Architecture Patterns

### Recommended Project Structure

```text
pi-agent-adapter-web/
├── src/main/java/io/github/pi_java/agent/adapter/web/ui/console/
│   ├── ConsoleView.java              # Console state, panel switcher, run/session orchestration
│   ├── ChatEventStreamPanel.java     # feed + sticky composer + inline run controls
│   ├── AgentCatalogPanel.java        # stacked mobile catalog surface
│   ├── AgentCard.java                # General Agent CTA prominence hooks
│   ├── SessionListPanel.java         # session cards + active session hook
│   └── RunContextPanel.java          # secondary run details + backup cancel compatibility
├── src/main/frontend/themes/pi-mobile/styles.css
│                                      # all mobile layout/sticky/tap/overflow rules
└── src/test/java/io/github/pi_java/agent/adapter/web/
    ├── WebConsoleUserFlowTest.java   # existing behavior regression
    └── WebConsoleMobileFlowContractTest.java  # new hooks/CSS contract

e2e/
├── fixtures/mobile-smoke.ts          # reuse/extend helpers
├── fixtures/fake-runtime.ts          # reuse/extend fake run helpers
└── phase-12-console-mobile-flow.spec.ts
```

### Pattern 1: Route-local Console panel state, not new routes

**What:** Keep `/console` as the only Console route and switch Agents/Sessions/Run Context as in-page panels with stable attributes such as `data-console-panel="chat|agents|sessions|run-context"`, `data-console-panel-active="true|false"`, and `data-action="show-console-panel"`.

**When to use:** All Phase 12 secondary surfaces. This preserves chat state and avoids route navigation resets.

**Example:**

```java
// Project pattern; use stable attributes for browser tests.
Button chatTab = new Button("Chat");
chatTab.getElement().setAttribute("data-action", "show-console-panel");
chatTab.getElement().setAttribute("data-console-target", "chat");

Div chatPanel = new Div(chatEventStreamPanel);
chatPanel.getElement().setAttribute("data-console-panel", "chat");
chatPanel.getElement().setAttribute("data-console-panel-active", "true");
```

### Pattern 2: Explicit feed/composer split in `ChatEventStreamPanel`

**What:** The stream, composer, active run status, send button, and cancel button should be explicit child containers instead of direct siblings.

**When to use:** To support sticky composer, feed scroll checks, bounded TextArea, and E2E visibility assertions.

**Example:**

```java
// Source: Vaadin docs via Context7 confirm TextArea row constraints.
TextArea input = new TextArea("Message");
input.setMinRows(2);
input.setMaxRows(6);
input.getElement().setAttribute("data-role", "chat-input");

Div feed = new Div();
feed.addClassName("pi-console-event-feed");
feed.getElement().setAttribute("data-role", "event-feed");

Div composer = new Div(input, sendButton, cancelButton, runStatus);
composer.addClassName("pi-console-composer");
composer.getElement().setAttribute("data-role", "chat-composer");
```

### Pattern 3: CSS owns mobile behavior; Java owns state and hooks

**What:** Java components should expose semantic classes and attributes; `pi-mobile/styles.css` should implement order, sticky, visibility, safe area, and responsive grid behavior.

**When to use:** All mobile layout changes. Avoid per-component inline style sprawl because Phase 10-11 established theme-level contracts.

**Example:**

```css
@media (max-width: 640px) {
  .pi-console-workbench {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: var(--pi-mobile-space-md);
  }

  .pi-console-chat {
    order: 1;
    min-height: calc(100svh - 8rem);
  }

  .pi-console-composer {
    position: sticky;
    bottom: calc(var(--pi-mobile-safe-area-bottom) + var(--pi-mobile-space-sm));
    z-index: 5;
    padding: var(--pi-mobile-space-sm);
    background: var(--pi-mobile-shell-surface);
    border: 1px solid var(--pi-mobile-shell-border);
    border-radius: 1rem;
  }
}
```

### Pattern 4: Deterministic browser E2E through stable selectors

**What:** E2E should interact through stable `data-*` hooks and public APIs/dev headers, not brittle text or generated Vaadin internals.

**When to use:** MVER-03 and desktop regression updates.

**Example:**

```ts
await page.goto('/console', { waitUntil: 'domcontentloaded' });
await expect(page.locator('[data-console-panel="chat"]')).toBeVisible();
await page.locator('[data-action="show-console-panel"][data-console-target="agents"]').click();
await page.locator('[data-agent-id="cloud-general-agent"] [data-entry-action="start-chat"]').click();
await page.locator('[data-role="chat-input"]').fill('line one\nline two\nline three');
await page.locator('[data-action="send-chat"]').click();
await expect(page.locator('[data-role="event-feed"]')).toContainText(/running|model|completed|cancel/i);
```

### Anti-Patterns to Avoid

- **Mobile-only backend/API fork:** Breaks ROADMAP constraints; keep existing public REST/SSE DTOs.
- **DOM-only CSS reorder without state:** CSS `order` alone can make Chat appear first but won't satisfy panel switching, active session identity, or testable state preservation.
- **One cancel button in hidden panel:** Fails D-08 and MCON-05 when user is scrolling feed.
- **Using Vaadin generated shadow DOM selectors in E2E:** Brittle across Vaadin patches; use project `data-*` hooks.
- **Full runtime card redesign in this phase:** Phase 13 owns card interiors; Phase 12 should only keep them reachable and visible in a vertical feed.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Multi-line bounded prompt input | Custom contenteditable textarea | Vaadin `TextArea` with `setMinRows`/`setMaxRows` plus theme CSS | Built-in component handles value binding, labels, accessibility, and auto-height constraints. |
| Mobile/browser matrix | Custom browser launcher scripts | Existing Playwright projects in `playwright.config.ts` | Already provides Mobile Chrome, Mobile Safari, Mobile Firefox proxy, Tablet, desktop. |
| Horizontal overflow detection | One-off JS in each spec | `expectNoPageHorizontalOverflow` helper | Shared helper checks document and body scroll widths consistently. |
| Tap/focus checks | Manual bounding box assertions in each spec | `expectTapTargetAtLeast`, `expectFocusVisible` helpers | Phase 11 established these contracts. |
| REST/SSE URL construction | String concatenation in UI components | `ConsoleHttpClient`, `EventStreamClient` | Keeps public API path semantics centralized and already covered by tests. |
| Runtime fake data | Real model/provider calls or API key tests | Existing fake runtime/dev headers and test fixtures | Deterministic/no-key is a locked Phase 12 decision. |

**Key insight:** The hard part is not inventing new runtime features; it is preserving existing Console state and public API behavior while changing visual hierarchy, sticky controls, and testable mobile ergonomics.

## Common Pitfalls

### Pitfall 1: Chat-first only visually, not behaviorally
**What goes wrong:** CSS order shows Chat first, but Agents/Sessions are still the first focus/DOM workflow and returning to Chat resets input/feed.  
**Why it happens:** Treating mobile as pure CSS instead of a Console sub-workflow.  
**How to avoid:** Add route-local panel state and active attributes; tests should switch Agents/Sessions/Run Context and verify input/feed/session identity persists.  
**Warning signs:** E2E can pass route load but cannot assert `[data-console-panel-active="true"]` transitions or active session label.

### Pitfall 2: Sticky composer obscures feed or ignores safe areas
**What goes wrong:** Last event is hidden behind composer; iOS/WebKit bottom viewport/safe-area creates unusable controls.  
**Why it happens:** `position: fixed`/`sticky` without safe-area tokens and feed bottom padding.  
**How to avoid:** Use existing `--pi-mobile-safe-area-bottom`; add feed `padding-bottom` matching composer height; prefer sticky inside Console content unless fixed is necessary.  
**Warning signs:** Mobile Safari project shows send/cancel clipped or no-overflow passes while controls are partly off-screen.

### Pitfall 3: TextArea grows until controls disappear
**What goes wrong:** Long prompt pushes Send/Cancel and run status off screen.  
**Why it happens:** Vaadin TextArea auto-height is useful but unbounded by default.  
**How to avoid:** Use `TextArea.setMinRows(2)` and `setMaxRows(5 or 6)`; add CSS max-height/internal scroll if needed.  
**Warning signs:** E2E multi-line prompt causes `[data-action="send-chat"]` or `[data-action="cancel-run"]` to be out of viewport.

### Pitfall 4: Cancel race/flakiness in fake runtime E2E
**What goes wrong:** Run completes before browser can click Cancel; test becomes flaky.  
**Why it happens:** Fake runtime helper `createRun` waits for terminal; current API flow may be too fast for UI cancellation.  
**How to avoid:** Use deterministic fake prompt/event sequence that remains cancellable long enough, or allow E2E to assert either visible cancel path followed by cancellation attempt or terminal status per D-16/D-18. Planner should inspect whether a slow fake run trigger already exists before creating one.  
**Warning signs:** Test intermittently sees no visible cancel even though run terminal status appears.

### Pitfall 5: Desktop regression loss
**What goes wrong:** Mobile single-primary refactor breaks existing desktop workbench efficiency or selectors.  
**Why it happens:** Replacing the three-column structure wholesale instead of adding responsive behavior.  
**How to avoid:** Keep desktop/tablet multi-column CSS and update existing `phase-05-web-console.spec.ts`/Java tests; assert desktop `data-layout` and column availability.  
**Warning signs:** Desktop Chrome route smoke no longer finds `data-layout="three-column-workbench"` or `data-column="run-context"`.

### Pitfall 6: Styling Vaadin internals too deeply
**What goes wrong:** CSS targets generated internals/shadow parts that change, causing browser-specific breakage.  
**Why it happens:** Trying to fine-tune component internals before adding stable wrappers.  
**How to avoid:** Style project-owned wrapper classes and use Flow API for component constraints. Only use Vaadin theme parts when documented and necessary.  
**Warning signs:** CSS selectors include generated IDs or deep internal DOM assumptions.

## Code Examples

Verified/current patterns from official/project sources:

### Vaadin TextArea bounded rows

```java
// Source: Vaadin docs via Context7, Text Area Row Constraints.
TextArea textArea = new TextArea();
textArea.setMinRows(4);
textArea.setMaxRows(8);
```

### Existing cancel path must be preserved

```java
// Source: current ConsoleView.java
public CancelPlan planCancelRunningRun(String reason) {
    if (selectedSessionId == null || activeRunId == null) {
        throw new IllegalStateException("No active run to cancel");
    }
    runContextPanel.showCancelling();
    return new CancelPlan(httpClient.cancelRunPath(selectedSessionId, activeRunId), new CancelRunRequest(reason));
}
```

### Existing Playwright device project pattern

```ts
// Source: official Playwright docs via Context7 and current playwright.config.ts.
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  projects: [
    { name: 'Mobile Chrome', use: { ...devices['Pixel 5'] } },
    { name: 'Mobile Safari', use: { ...devices['iPhone 12'] } },
    { name: 'Tablet', use: { ...devices['iPad Pro 11'] } },
  ],
});
```

### Existing no-overflow helper

```ts
// Source: e2e/fixtures/mobile-smoke.ts
await expectNoPageHorizontalOverflow(page);
await expectTapTargetAtLeast(page.locator('[data-action="send-chat"]'), 44, 'send chat action');
```

## State of the Art

| Old Approach | Current Approach | When Changed / Source | Impact |
|--------------|------------------|-----------------------|--------|
| Desktop-first Console workbench only | Responsive Console with phone Chat-first panel plus tablet/desktop multi-column | Phase 12 locked decisions | Planner must include component structure and CSS changes, not only browser tests. |
| Route smoke only | Product-path mobile E2E with fake run, streamed UI, sessions, cancellation/terminal status | Phase 12 MVER-03 | Add a new E2E spec; Phase 10/11 smoke is insufficient. |
| Normal-flow composer | Sticky/bottom-reachable composer with bounded TextArea | Phase 12 D-05/D-06 and Vaadin TextArea docs | Requires wrapper DOM + CSS + tests for multi-line prompt and scroll. |
| Single right-column cancel | Dual-position visible cancel | Phase 12 D-08 | Cancel must be in composer/run controls and backup status/action area if practical. |

**Deprecated/outdated for this phase:**
- `data-layout="three-column-workbench"` as the sole Console behavior contract on phone. Keep it for regression if useful, but add mobile-specific Console panel/feed/composer hooks.
- Plain session row text with only `role="button"`; Phase 12 needs active/session summary/status/updated-time hooks.
- API-only Playwright console validation; MVER-03 needs browser-visible Console loop.

## Open Questions

1. **How should the shell backup cancel action be wired?**
   - What we know: `PiResponsiveShell` has private status/action slots with stable attributes.
   - What's unclear: No public route-to-shell API exists in current code.
   - Recommendation: Planner should either add a minimal shell slot API/event or implement the backup cancel in a page header/status region inside Console if shell-slot wiring is too invasive. Must still satisfy dual-position cancellation.

2. **Can fake runtime reliably keep a run cancellable long enough for UI E2E?**
   - What we know: `fake-runtime.ts` has `cancelRun(request)` API helper, but it creates and cancels via API, not browser UI. Existing `createRun` waits for terminal.
   - What's unclear: Whether current app has a prompt trigger such as `cancel me slowly` that stays active long enough in browser UI.
   - Recommendation: Before writing E2E, inspect fake runtime/server fixture behavior. If no deterministic slow run exists, add a test-only deterministic event timing hook in existing fake runtime path, not production runtime capability.

3. **Exact segmented control implementation?**
   - What we know: Vaadin has Tabs, Details, Buttons; user allowed labels/styling discretion.
   - What's unclear: Whether Tabs adds unnecessary route/focus complexity.
   - Recommendation: Use simple Flow `Button` segmented controls with ARIA/data hooks unless Vaadin Tabs are already easy to test. Avoid building a custom shadow-DOM-heavy control.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java | Maven/Vaadin tests | ✓ | 21.0.11 | — |
| Maven | Java contract tests | ✓ | 3.8.7 | Project recommends Maven 3.9.x+, but current test command succeeds with 3.8.7. |
| Node.js | Playwright/npm scripts | ✓ | v22.22.2 | — |
| npm | Playwright install/run | ✓ | 10.9.7 | — |
| Playwright CLI | E2E list/run | ✓ | 1.57.0 installed | Keep installed version; update only in separate dependency task. |
| Playwright browser binaries | Mobile browser E2E | Not fully probed in research | — | Run `npm run e2e:install -- --with-deps=false` before full E2E. |
| Vaadin dev server harness | Full browser E2E | Configured | `scripts/e2e-web-server.sh` via Playwright config | If local startup flakes, use `PLAYWRIGHT_SKIP_WEBSERVER=1 ... --list` for spec validation and rely on CI for full run. |

**Missing dependencies with no fallback:** None identified for planning.

**Missing dependencies with fallback:** Playwright browser binaries were not fully executed/probed during research; install command is the fallback/prep step.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Java framework | JUnit Jupiter 5.10.3 + AssertJ 3.26.3 via Maven Surefire 3.2.5 |
| Browser framework | Playwright Test installed 1.57.0 (`@playwright/test` pinned `^1.57.0`) |
| Config file | `playwright.config.ts`; Maven configs in root `pom.xml` and `pi-agent-adapter-web/pom.xml` |
| Quick Java command | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test` |
| Existing Java baseline command verified | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest test` succeeded: 6 tests, 0 failures |
| E2E list command | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list` |
| E2E representative run | `npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome"` |
| E2E matrix gate | `npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --project="Mobile Safari" --project="Tablet"` |
| Desktop regression command | `npm run e2e -- e2e/phase-05-web-console.spec.ts --project="chromium"` plus any updated Console mobile flow desktop smoke |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| MCON-01 | Agent Catalog stacked cards and General Agent Start/Continue on phone | Java contract + Playwright mobile | Maven quick command + `phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome"` | ❌ Wave 0 for new mobile contract/spec |
| MCON-02 | Multi-line prompt, submit, active run/composer state | Java contract + Playwright mobile | Same as above | ❌ Wave 0 |
| MCON-03 | Live vertical event feed and scroll without losing controls | Playwright mobile/tablet | Matrix gate for Mobile Chrome/Mobile Safari/Tablet | ❌ Wave 0 |
| MCON-04 | Open session history, select past session, continue, active session visible | Java contract + Playwright mobile | Maven quick + Mobile Chrome E2E | ❌ Wave 0 |
| MCON-05 | Visible touch-safe cancel and cancelling/terminal feedback | Java contract + Playwright mobile | Maven quick + Mobile Chrome E2E | ❌ Wave 0 |
| MVER-03 | Full fake/no-key Console mobile loop across representative matrix | Playwright E2E | Matrix gate; optional Mobile Firefox smoke/no-overflow if flaky | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** Java quick command for touched Console components; for CSS/test tasks also run Playwright `--list` for the new spec.
- **Per wave merge:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web test` scoped as practical, plus Mobile Chrome E2E for Phase 12 spec.
- **Phase gate:** Phase 12 E2E matrix (Mobile Chrome, Mobile Safari, Tablet), optional Mobile Firefox smoke/no-overflow, existing Phase 10/11 route/shell smoke, and desktop Console regression green before `/gsd-verify-work`.

### Wave 0 Gaps

- [ ] `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleMobileFlowContractTest.java` — covers MCON-01..MCON-05 stable hooks, CSS contract strings, TextArea row constraints, panel state.
- [ ] `e2e/phase-12-console-mobile-flow.spec.ts` — covers MVER-03 full mobile product path.
- [ ] Possible additions to `e2e/fixtures/fake-runtime.ts` — browser-visible slow/cancellable run helper if current runtime completes too fast.
- [ ] Existing `phase-10-mobile-route-smoke.spec.ts` and `phase-11-shell-navigation.spec.ts` selector expectations may need updates if `data-layout` evolves from `three-column-workbench` to a richer responsive Console contract.

## Sources

### Primary (HIGH confidence)
- Project context: `.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md`, `.planning/REQUIREMENTS.md`, `.planning/ROADMAP.md`, `.planning/STATE.md`.
- Implemented foundations: `docs/phase-10-mobile-baseline.md`, `docs/phase-11-responsive-shell.md`.
- Current code inspected: `ConsoleView.java`, `ChatEventStreamPanel.java`, `AgentCatalogPanel.java`, `AgentCard.java`, `SessionListPanel.java`, `RunContextPanel.java`, `PiResponsiveShell.java`, `PiPageHeader.java`, `pi-mobile/styles.css`, Playwright fixtures/specs.
- Vaadin official docs via Context7 `/vaadin/docs`: TextArea `setMinRows`/`setMaxRows`, styling with class names/CSS, Details compact variant.
- Playwright official docs via Context7 `/microsoft/playwright`: device emulation projects and mobile project configuration.

### Secondary (MEDIUM confidence)
- npm registry version checks for `@playwright/test` and `@vaadin/bundles` on 2026-06-23. Used only to note latest versions, not to recommend upgrades.

### Tertiary (LOW confidence)
- None used for critical decisions.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — existing project versions and installed tool versions verified; official docs checked for key APIs.
- Architecture: HIGH — based on current source inspection and locked user decisions.
- Vaadin mobile CSS tactics: MEDIUM-HIGH — official docs confirm primitives; final sticky/safe-area behavior must be validated in Playwright WebKit/Chromium.
- E2E fake runtime timing: MEDIUM — fixture APIs inspected, but cancellable browser-visible timing needs implementation-time validation.
- Pitfalls: HIGH — derived from concrete current-code gaps and prior Phase 10/11 known CI/emulation limits.

**Research date:** 2026-06-23  
**Valid until:** 2026-07-23 for project-code findings; 2026-06-30 for npm latest-version observations.
