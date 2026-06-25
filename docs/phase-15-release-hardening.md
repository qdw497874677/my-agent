# Phase 15 Release Hardening

Phase 15 is the final v1.1 mobile H5 release-hardening layer for the existing Java/Vaadin Agent Web Console and Admin Governance surfaces. It concentrates the automated CI/browser evidence, viewport and orientation matrix, real-device UAT expectations, known CI/proxy gaps, and release go/no-go criteria for the mobile milestone.

This document is paired with the scripted manual checklist at [15-HUMAN-UAT.md](../.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md). Manual results in that file feed the go/no-go classification below; unrun or failed true-device checks must be recorded as `blocker`, `known limitation`, or `follow-up`, not treated as implicitly passed.

## Scope and Boundaries

In scope:

- Existing Vaadin Web Console and Admin Governance mobile H5 release readiness.
- Representative portrait, landscape, tablet, desktop, and layered cross-browser Playwright coverage.
- Console critical flow: agent selection, run/chat, event feed, runtime/tool/approval inspection, session selection, and cancel/terminal feedback.
- Admin critical inspection: Governance landing, Overview, Registry/MCP/Plugin/Extension, Operations, Policy Decisions, Audits, and Approvals card/detail surfaces.
- Keyboard/focus, reduced-motion, no-hover, tap-target, and no-horizontal-overflow contracts.
- Real-device UAT expectations for Android Chrome, iOS Safari, Edge mobile, and Firefox mobile.

Out of scope for v1.1 release hardening:

- Native iOS/Android app, PWA/offline behavior, push/background monitoring, React/Next.js/Hilla React rewrite, mobile-only backend/API fork, or new Agent runtime/model/tool capability.
- Exhaustive pixel-perfect validation for every device model.
- Screenshot visual regression as the primary release gate. Per D-09, structural selectors, content/action visibility, no-overflow checks, focus/keyboard checks, reduced-motion/no-hover contracts, and deterministic interactions are the pass/fail basis. Screenshots may be attached as UAT/debug evidence only.

## Requirement Traceability

| Requirement | Evidence | Release status |
| --- | --- | --- |
| **MVER-05**: Representative portrait, landscape, and tablet viewports pass mobile navigation and no-horizontal-overflow checks. | `e2e/phase-15-orientation-release-smoke.spec.ts` plus reusable `phase15ViewportCases` in `e2e/fixtures/mobile-smoke.ts`. | Automated gate defined; true-device orientation remains UAT. |
| **MVER-06**: Desktop Web Console/Admin browser regressions remain passing after mobile-first changes. | `e2e/phase-15-critical-flow-regression.spec.ts` desktop summary gate under `chromium`, preserving the detailed Phase 05 desktop Console baseline. | Automated gate defined. |
| **MVER-07**: Release documentation records real-device/UAT expectations for Android Chrome, iOS Safari, Edge mobile, and Firefox mobile, including CI/emulation gaps. | This document plus [15-HUMAN-UAT.md](../.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md). | Documentation complete; device results remain `status: partial` until humans fill outcomes. |

## CI and Browser Coverage Matrix

Phase 15 uses layered Playwright coverage. These commands are intentionally no-key/list gates for local execution in constrained environments; CI or a stable developer runner should run the same specs without `--list` when full browser execution is available.

| Gate | Command | Purpose | Expected coverage |
| --- | --- | --- | --- |
| Orientation release smoke | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-orientation-release-smoke.spec.ts --project="chromium" --project="Mobile Chrome" --project="Mobile Safari" --project="Mobile Firefox" --project="Tablet" --list` | Route × viewport discovery for Console and Admin across the layered browser matrix. | Desktop Chrome, Android Chrome proxy, iOS Safari/WebKit proxy, Firefox mobile proxy, and tablet proxy discover all portrait/landscape/tablet route checks. |
| Console/Admin critical mobile flow | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="Mobile Chrome" --list` | Deep deterministic Console run/chat/session/cancel/runtime-card flow and representative Admin card/detail inspection. | Mobile Chrome representative path for timing-sensitive product interactions. |
| Desktop release summary | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="chromium" --list` | Compact Console/Admin desktop confidence gate that supplements existing Phase 05 desktop baseline. | Desktop Chrome routes and primary surfaces remain discoverable after mobile changes. |
| Accessibility hardening | `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-accessibility-hardening.spec.ts --project="Mobile Chrome" --list` | Representative shell, Console, runtime/approval, Admin, reduced-motion, no-hover, focus, and keyboard checks. | Mobile Chrome representative accessibility contracts are discoverable. |

Skipped or flaky browser-specific failures must follow D-08: fix low-risk app/test issues where feasible; otherwise document the exact engine/proxy/environment reason, release impact, and classification (`blocker`, `known limitation`, or `follow-up`). Do not silently skip failures or imply a true-device pass from a Playwright proxy.

## Viewport and Orientation Matrix

The Phase 15 route/orientation gate uses named viewport cases from Plan 01:

| Viewport case | Dimensions | Intended pressure | Release expectation |
| --- | --- | --- | --- |
| Phone portrait | `390 × 844` | Primary one-handed mobile layout, stacked Console/Admin content, compact shell. | Console and all Admin routes load, show primary content/actions, keep active navigation, and avoid page-level horizontal overflow. |
| Phone landscape | `844 × 390` | Short-height orientation with drawer/nav and critical controls under vertical pressure. | Shell/drawer navigation remains usable, critical controls are visible/touchable, and no page-level horizontal overflow appears. |
| Tablet bridge | `834 × 1194` | Intermediate layout between phone and desktop. | Tablet does not remain overly cramped like a phone or prematurely assume full desktop width; Console/Admin density stays readable. |

The gate covers `/console`, `/admin/governance`, `/admin/governance/overview`, `/admin/governance/registry`, `/admin/governance/operations`, `/admin/governance/policy-decisions`, `/admin/governance/audits`, and `/admin/governance/approvals`.

## Critical-Flow and Desktop Regression Gates

`e2e/phase-15-critical-flow-regression.spec.ts` is the final release gate for high-value interactions:

- **Console mobile flow:** open `/console`, select the General Agent, submit deterministic fake-runtime prompts, observe event-feed growth, inspect runtime/tool/approval cards, open Sessions, select the active session card, and cancel or observe terminal feedback.
- **Admin mobile inspection:** open representative Admin routes, inspect mobile cards/details, verify redaction markers stay hidden, sample tap targets/focus, and enforce no page-level horizontal overflow.
- **Desktop regression summary:** run representative Console and Admin route checks under `chromium`, while preserving the earlier detailed `e2e/phase-05-web-console.spec.ts` desktop baseline unchanged.

## Accessibility, Reduced-Motion, and No-Hover Contracts

Phase 15 locks representative accessibility contracts through `e2e/phase-15-accessibility-hardening.spec.ts`, `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css`, and `WebPhase15AccessibilityContractTest`:

- Keyboard users can reach shared shell navigation, Console composer/panel controls, runtime details, approval actions, and Admin details/primary controls with visible focus.
- Important actions are not hover-only; no-hover/touch users can see and activate controls without desktop hover affordances.
- `prefers-reduced-motion` reduces unnecessary transitions/animations and avoids forced smooth scrolling.
- Current navigation and detail affordances retain visible focus/current-state signals.
- A `641px-899px` tablet bridge prevents layout breakage before the desktop breakpoint.
- Dense IDs, JSON-like blocks, errors, and redacted details wrap or scroll internally without causing page-level horizontal overflow.

## Real-Device UAT Matrix

True-device coverage is tracked in [15-HUMAN-UAT.md](../.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md), which starts as `status: partial` until humans record outcomes.

| Target | True-device expectation | Playwright relationship | Required classification if incomplete or failed |
| --- | --- | --- | --- |
| Android Chrome | Real Android phone or device-cloud Chrome. | `Mobile Chrome` is a Chromium phone viewport/touch proxy, useful but not a substitute for physical Android browser/keyboard/viewport chrome behavior. | `blocker`, `known limitation`, or `follow-up`. |
| iOS Safari | Real iPhone Safari. | `Mobile Safari` is a WebKit proxy using a Playwright iPhone descriptor; it is not proof of every true iOS Safari integration detail, OS keyboard behavior, browser chrome behavior, or device-specific rendering quirk. | `blocker`, `known limitation`, or `follow-up`. |
| Edge mobile | Real Android or iOS Edge browser. | No dedicated Phase 15 Playwright Edge mobile project; use Mobile Chrome/WebKit only as engine-family proxies. | `blocker`, `known limitation`, or `follow-up`. |
| Firefox mobile | Real Firefox for Android or iOS Firefox where available. | `Mobile Firefox` is a Firefox-engine mobile viewport/user-agent proxy, not proof of every true Firefox mobile browser behavior. | `blocker`, `known limitation`, or `follow-up`. |

## Known Playwright Proxy Limitations

- **Mobile Safari/WebKit proxy:** Playwright `Mobile Safari` covers WebKit layout pressure with an iPhone-like descriptor, but it is not a physical iPhone. It cannot prove OS-level keyboard interaction, iOS browser chrome viewport shifts, device memory/performance quirks, or all Safari version differences.
- **Mobile Firefox proxy:** Playwright `Mobile Firefox` uses the Firefox engine with mobile viewport/touch/user-agent settings. It is a Firefox-engine mobile proxy, not true Firefox for Android or iOS.
- **Edge mobile:** Phase 15 does not define a dedicated Playwright Edge mobile project. Edge mobile true-device validation must be recorded manually.
- **Full browser execution environment:** Local `--list` gates prove spec discovery without a web server. Full execution should run on CI/developer machines with stable Vaadin dev-mode/frontend startup and Playwright browser dependencies.

## Release Go/No-Go Criteria

### Go

- Required Phase 15 automated gates above pass in CI or an approved full-browser runner, or any environment-specific `--list` substitution is explicitly documented with impact.
- Android Chrome, iOS Safari, Edge mobile, and Firefox mobile true-device/UAT results are recorded in [15-HUMAN-UAT.md](../.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-HUMAN-UAT.md).
- No unresolved issue classified as `blocker` remains open.
- All `known limitation` entries have clear impact, owner/follow-up, and release notes.
- All `follow-up` entries are non-blocking for the v1.1 H5 release and are tracked outside the release gate.

### No-Go

- Any automated gate fails for a product issue and is not fixed or explicitly downgraded with documented rationale.
- Any true-device failure prevents Console run/chat/session/cancel or Admin critical inspection on a target browser and is classified as `blocker`.
- True-device validation is omitted but described as passed.
- Horizontal overflow, inaccessible primary controls, hover-only critical actions, or keyboard/focus traps are found on release-critical surfaces without a documented mitigation.

### Conditional Go

- A target browser/device remains unrun or has an engine-specific issue classified as `known limitation` or `follow-up`, with clear release impact and downstream tracking.
- CI can only run Playwright proxy coverage for a target and manual true-device coverage is scheduled but not yet complete; the release note must state that true-device coverage is `status: partial`.
