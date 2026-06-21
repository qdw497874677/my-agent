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
