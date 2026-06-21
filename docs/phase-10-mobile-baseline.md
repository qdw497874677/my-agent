# Phase 10 Mobile Baseline

Phase 10 establishes a representative Playwright mobile/tablet harness for the existing Vaadin Agent Web Console and Admin Governance surfaces. It intentionally creates a CI-friendly baseline instead of a full real-device release matrix.

## Playwright Project Matrix

| Project name | Playwright basis | Browser engine | Approximates | Phase 10 purpose |
| --- | --- | --- | --- | --- |
| `chromium` | `Desktop Chrome` | Chromium | Existing desktop regression coverage | Preserves the pre-mobile desktop smoke gate. |
| `Mobile Chrome` | `Pixel 5` | Chromium | Android Chrome phone viewport/touch behavior | Representative mobile Chromium route and overflow checks. |
| `Mobile Safari` | `iPhone 12` | WebKit | WebKit-as-Mobile-Safari | CI-supported proxy for iOS Safari layout behavior. |
| `Mobile Firefox` | `Desktop Firefox` plus 390x844 mobile viewport, touch, `isMobile`, and Firefox Android user agent | Firefox | Firefox mobile-sized/touch context | Firefox engine coverage with documented mobile emulation limits. |
| `Tablet` | `iPad Pro 11` | WebKit | iPad/tablet Safari-like viewport | Representative tablet layout and navigation pressure. |

The exact project names above are the stable identifiers used by Phase 10 and downstream smoke specs.

## Browser Installation

The repository-level install entry point is:

```bash
npm run e2e:install -- --with-deps=false
```

It delegates to the local Playwright CLI and installs `chromium firefox webkit`. The script keeps the existing `--with-deps=false` normalization so CI jobs that manage operating-system packages separately can reuse the same command.

## Shared Smoke Helpers

`e2e/fixtures/mobile-smoke.ts` provides reusable helpers for future route smoke specs:

- `expectNoPageHorizontalOverflow(page, tolerance)` checks both `document.documentElement` and `document.body` with `scrollWidth <= clientWidth + tolerance`.
- Stable selector helpers assert visibility through `data-route`, `data-layout`, `data-panel`/`data-surface`, `data-action`, `data-primary-action`, or `data-mobile-critical` selectors.
- Route metadata types let each route declare its path, route marker, primary content, and primary actions without hard-coding brittle body-text selectors.

## Known CI and Emulation Gaps

### WebKit-as-Mobile-Safari

`Mobile Safari` uses Playwright WebKit with an iPhone descriptor. This is the closest portable CI proxy for Safari, but it is not a physical iPhone and does not cover every iOS Safari integration detail, OS-level keyboard behavior, viewport chrome behavior, or device-specific rendering quirk.

### Firefox Mobile Proxy

Playwright does not provide a true Android Firefox browser in standard CI. `Mobile Firefox` therefore uses the Firefox engine with a phone-sized viewport, touch flags, mobile mode, and a Firefox Android user agent. This covers a Firefox rendering engine under mobile layout pressure, but it is not equivalent to real Firefox for Android.

### Real Devices and UAT

Phase 10 does not require real Android/iOS devices, Edge mobile, app-store browsers, or broad orientation sweeps. Those release-hardening and UAT expectations are intentionally handed off to Phase 15, where the final converted mobile UX can be validated across real devices, portrait/landscape combinations, and accessibility checks.

## Scope Guardrails

- Keep the existing Desktop Chrome project available.
- Use representative phone/tablet contexts instead of exhaustive device/orientation matrices.
- Document emulation gaps instead of blocking Phase 10 on real-device infrastructure.
- Keep route smoke specs focused on route load, no page-level horizontal overflow, stable primary content/actions, and light interactions; deeper Console/Admin product flows remain in later phases.

## Route-Level Smoke Coverage

`e2e/phase-10-mobile-route-smoke.spec.ts` is the Phase 10 route-level smoke gate for every existing Console/Admin Governance route from D-05:

| Route | Stable marker | Primary route checks | Light deterministic interaction |
| --- | --- | --- | --- |
| `/console` | `[data-route="console"]` | Three-column workbench marker, chat stream column, chat input, send action, no page-level horizontal overflow | Focus the chat input and verify the send action is visible without submitting a run. |
| `/admin/governance` | `[data-route="admin-governance"]` | Governance surface/mobile-critical markers and no page-level horizontal overflow | Read the landing title/body content. |
| `/admin/governance/overview` | `[data-route="admin-governance-overview"]` | Inspect-only surface, empty/loaded overview state, and no page-level horizontal overflow | Read the overview title/content. |
| `/admin/governance/registry` | `[data-route="admin-registry-status"]` | Inspect-only registry surface, empty/loaded registry state, mutation-controls marker, and no page-level horizontal overflow | Verify mutation controls remain absent in the baseline route smoke. |
| `/admin/governance/operations` | `[data-route="admin-operations"]` | Operations surface, runs/warnings sections, and no page-level horizontal overflow | Read deterministic empty/summary section state. |
| `/admin/governance/policy-decisions` | `[data-route="admin-policy-decisions"]` | Inspect-only policy surface, policy empty/list state, and no page-level horizontal overflow | Read deterministic empty/list state. |
| `/admin/governance/audits` | `[data-route="admin-audit-summaries"]` | Inspect-only audit surface, audit empty/list state, and no page-level horizontal overflow | Read deterministic empty/list state. |
| `/admin/governance/approvals` | `[data-route="admin-approval-queue"]` | Separated governance approval surface, approval empty/list state, and no page-level horizontal overflow | Read deterministic empty/list state. |

The smoke spec intentionally avoids deep run-flow E2E, final Console mobile workflow conversion, final runtime cards, approval UX redesign, and Admin table/card migration. Those remain Phase 11-15 scope.

## Phase 10 Verification Commands

Install the configured browser engines before executing the route smoke gate:

```bash
npm run e2e:install -- --with-deps=false
```

Representative route smoke commands:

```bash
npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --project="Mobile Chrome"
npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts
```

During this local execution, Playwright successfully loaded the route smoke spec and listed all eight required Mobile Chrome tests using:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-10-mobile-route-smoke.spec.ts --project="Mobile Chrome" --list
```

Full browser execution reached the Vaadin development-mode bootstrap but timed out in this container while the first route waited for client-side initialization. The E2E server was adjusted to run from the adapter-web module base directory with `project.basedir` set so the `pi-mobile` theme is discoverable. The remaining timeout is documented as a local execution-environment limitation for this run, not a reduction in the route coverage contract. CI should run the full commands above on a runner with stable Vaadin frontend/dev-server startup and Playwright browser host dependencies.

## Phase 15 Handoff

Phase 15 should expand this baseline into final release hardening after the mobile-first UX migrations are complete:

- real Android Chrome/Edge/Firefox and real iOS Safari device or device-cloud UAT;
- portrait and landscape orientation sweeps for final Console/Admin flows;
- accessibility/touch-target checks beyond route load and page-level overflow;
- verification of final runtime-card, approval-card, and Admin card/table replacement patterns.
