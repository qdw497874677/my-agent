---
status: partial
phase: 15-cross-browser-orientation-accessibility-and-release-hardening
source: [docs/phase-15-release-hardening.md]
started: 2026-06-25T00:00:00Z
updated: 2026-06-25T00:00:00Z
---

# Phase 15 Human UAT

This checklist captures true-device release validation for the v1.1 mobile H5 milestone. It complements the automated Playwright gates documented in [docs/phase-15-release-hardening.md](../../../docs/phase-15-release-hardening.md); it does not replace them.

`status: partial` is intentional until humans record actual outcomes. Do not mark true-device coverage as passed from Playwright proxies alone. For every pending, unrun, or failed item, record one classification:

- `blocker` — prevents release or prevents a critical Console/Admin path on the target browser/device.
- `known limitation` — acceptable for release only if impact and release-note wording are explicit.
- `follow-up` — non-blocking improvement tracked after release.

Use the same proxy language as the release guide: Playwright `Mobile Safari` is a WebKit proxy, and Playwright `Mobile Firefox` is a Firefox-engine mobile viewport/user-agent proxy. These automated projects are useful release signals, but the true-device rows below remain pending until humans test real devices and classify any unrun or failed items as `blocker`, `known limitation`, or `follow-up`.

## How to Record Results

For each target browser/device:

- Result: `[pending]`, `[passed]`, `[failed]`, or `[not run]`
- Classification: `blocker` / `known limitation` / `follow-up` / `none`
- Evidence: device/browser/OS version, screenshots/video links if available, and concise notes.
- If an item is unrun, keep the result as `[pending]` or `[not run]` and classify the gap. Do not convert it to `[passed]` without actual true-device execution.

## Automated Baseline to Review Before UAT

The maintainer should compare manual observations against the Phase 15 automated contracts:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-orientation-release-smoke.spec.ts --project="chromium" --project="Mobile Chrome" --project="Mobile Safari" --project="Mobile Firefox" --project="Tablet" --list
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="Mobile Chrome" --list
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-critical-flow-regression.spec.ts --project="chromium" --list
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-15-accessibility-hardening.spec.ts --project="Mobile Chrome" --list
```

## Device Matrix Summary

| Target | True device/browser required | Playwright proxy relationship | Overall result | Classification | Notes |
| --- | --- | --- | --- | --- | --- |
| Android Chrome | Real Android phone running Chrome | `Mobile Chrome` is a Chromium mobile viewport/touch proxy | `[pending]` | blocker / known limitation / follow-up / none |  |
| iOS Safari | Real iPhone running Safari | `Mobile Safari` is a WebKit proxy, not full true-device Safari proof | `[pending]` | blocker / known limitation / follow-up / none |  |
| Edge mobile | Real Android or iOS Edge browser | No dedicated Phase 15 Playwright Edge mobile project | `[pending]` | blocker / known limitation / follow-up / none |  |
| Firefox mobile | Real Firefox mobile browser where available | `Mobile Firefox` is a Firefox-engine mobile viewport/user-agent proxy | `[pending]` | blocker / known limitation / follow-up / none |  |

## Android Chrome Checklist

Device/browser evidence:

- Device model / OS version: `[pending]`
- Browser version: `[pending]`
- Network/profile notes: `[pending]`

### Console run/chat/session/cancel

- [ ] Open `/console`; shared shell, page title, and primary Console content render without horizontal overflow.
- [ ] Open the Agents panel; select/start the General Agent or continue the visible General Agent flow.
- [ ] Enter a multi-line prompt in the chat composer and submit it.
- [ ] Observe the event feed growing with readable runtime/tool/approval cards or terminal status.
- [ ] Open Sessions; confirm an active session card is visible and can return the user to Chat.
- [ ] Use a visible cancel action when a run is cancellable, or record terminal-state fallback if the run already completed.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Admin card/detail inspection

- [ ] Open `/admin/governance` and at least Overview, Registry, Operations, Policy Decisions, Audits, and Approvals.
- [ ] Confirm Admin cards/details are stacked/readable and do not rely on page-level horizontal table scrolling.
- [ ] Expand representative card Details and confirm dense IDs/JSON/error text wraps or scrolls internally.
- [ ] Confirm sensitive marker/raw secret values are not visible in summaries or expanded details.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Orientation and no-horizontal-overflow

- [ ] Validate portrait phone layout.
- [ ] Rotate to landscape; confirm shell navigation/drawer, Console critical controls, and Admin details remain usable.
- [ ] Return to portrait; confirm layout recovers without stale overlay/focus issues.
- [ ] Inspect for page-level horizontal overflow on Console and representative Admin routes.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Keyboard/focus and touch accessibility

- [ ] Focus composer/input controls and confirm visible focus indicators where the platform exposes keyboard navigation.
- [ ] Confirm primary controls are touch-safe and available without hover.
- [ ] If using hardware keyboard or accessibility switch input, sample shell navigation, composer, Details, and approval/cancel controls.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

## iOS Safari Checklist

Device/browser evidence:

- Device model / iOS version: `[pending]`
- Safari version: `[pending]`
- Network/profile notes: `[pending]`

### Console run/chat/session/cancel

- [ ] Open `/console`; shared shell, page title, and primary Console content render without horizontal overflow.
- [ ] Open the Agents panel; select/start the General Agent or continue the visible General Agent flow.
- [ ] Enter a multi-line prompt in the chat composer; observe iOS keyboard/viewport behavior and submit it.
- [ ] Observe the event feed growing with readable runtime/tool/approval cards or terminal status.
- [ ] Open Sessions; confirm an active session card is visible and can return the user to Chat.
- [ ] Use a visible cancel action when a run is cancellable, or record terminal-state fallback if the run already completed.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Admin card/detail inspection

- [ ] Open `/admin/governance` and at least Overview, Registry, Operations, Policy Decisions, Audits, and Approvals.
- [ ] Confirm Admin cards/details are stacked/readable and do not rely on page-level horizontal table scrolling.
- [ ] Expand representative card Details and confirm dense IDs/JSON/error text wraps or scrolls internally.
- [ ] Confirm sensitive marker/raw secret values are not visible in summaries or expanded details.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Orientation and no-horizontal-overflow

- [ ] Validate portrait iPhone layout.
- [ ] Rotate to landscape; confirm Safari browser chrome/viewport changes do not hide shell navigation, composer, cancel, or Admin Details controls.
- [ ] Return to portrait; confirm layout recovers without stale overlay/focus issues.
- [ ] Inspect for page-level horizontal overflow on Console and representative Admin routes.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Keyboard/focus and touch accessibility

- [ ] Confirm soft-keyboard focus does not permanently obscure the composer or action row.
- [ ] Confirm primary controls are touch-safe and available without hover.
- [ ] If using hardware keyboard or accessibility switch input, sample shell navigation, composer, Details, and approval/cancel controls.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

## Edge Mobile Checklist

Device/browser evidence:

- Device model / OS version: `[pending]`
- Edge mobile version: `[pending]`
- Network/profile notes: `[pending]`

### Console run/chat/session/cancel

- [ ] Open `/console`; shared shell, page title, and primary Console content render without horizontal overflow.
- [ ] Open the Agents panel; select/start the General Agent or continue the visible General Agent flow.
- [ ] Enter a multi-line prompt in the chat composer and submit it.
- [ ] Observe the event feed growing with readable runtime/tool/approval cards or terminal status.
- [ ] Open Sessions; confirm an active session card is visible and can return the user to Chat.
- [ ] Use a visible cancel action when a run is cancellable, or record terminal-state fallback if the run already completed.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Admin card/detail inspection

- [ ] Open `/admin/governance` and at least Overview, Registry, Operations, Policy Decisions, Audits, and Approvals.
- [ ] Confirm Admin cards/details are stacked/readable and do not rely on page-level horizontal table scrolling.
- [ ] Expand representative card Details and confirm dense IDs/JSON/error text wraps or scrolls internally.
- [ ] Confirm sensitive marker/raw secret values are not visible in summaries or expanded details.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Orientation and no-horizontal-overflow

- [ ] Validate portrait phone layout.
- [ ] Rotate to landscape; confirm shell navigation/drawer, Console critical controls, and Admin details remain usable.
- [ ] Return to portrait; confirm layout recovers without stale overlay/focus issues.
- [ ] Inspect for page-level horizontal overflow on Console and representative Admin routes.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Keyboard/focus and touch accessibility

- [ ] Focus composer/input controls and confirm visible focus indicators where the platform exposes keyboard navigation.
- [ ] Confirm primary controls are touch-safe and available without hover.
- [ ] If using hardware keyboard or accessibility switch input, sample shell navigation, composer, Details, and approval/cancel controls.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

## Firefox Mobile Checklist

Device/browser evidence:

- Device model / OS version: `[pending]`
- Firefox mobile version: `[pending]`
- Network/profile notes: `[pending]`

### Console run/chat/session/cancel

- [ ] Open `/console`; shared shell, page title, and primary Console content render without horizontal overflow.
- [ ] Open the Agents panel; select/start the General Agent or continue the visible General Agent flow.
- [ ] Enter a multi-line prompt in the chat composer and submit it.
- [ ] Observe the event feed growing with readable runtime/tool/approval cards or terminal status.
- [ ] Open Sessions; confirm an active session card is visible and can return the user to Chat.
- [ ] Use a visible cancel action when a run is cancellable, or record terminal-state fallback if the run already completed.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Admin card/detail inspection

- [ ] Open `/admin/governance` and at least Overview, Registry, Operations, Policy Decisions, Audits, and Approvals.
- [ ] Confirm Admin cards/details are stacked/readable and do not rely on page-level horizontal table scrolling.
- [ ] Expand representative card Details and confirm dense IDs/JSON/error text wraps or scrolls internally.
- [ ] Confirm sensitive marker/raw secret values are not visible in summaries or expanded details.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Orientation and no-horizontal-overflow

- [ ] Validate portrait phone layout.
- [ ] Rotate to landscape; confirm shell navigation/drawer, Console critical controls, and Admin details remain usable.
- [ ] Return to portrait; confirm layout recovers without stale overlay/focus issues.
- [ ] Inspect for page-level horizontal overflow on Console and representative Admin routes.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

### Keyboard/focus and touch accessibility

- [ ] Focus composer/input controls and confirm visible focus indicators where the platform exposes keyboard navigation.
- [ ] Confirm primary controls are touch-safe and available without hover.
- [ ] If using hardware keyboard or accessibility switch input, sample shell navigation, composer, Details, and approval/cancel controls.

Result: `[pending]`  
Classification: blocker / known limitation / follow-up / none  
Evidence/notes: `[pending]`

## Summary

total: 16  
passed: 0  
issues: 0  
pending: 16  
skipped: 0  
blocked: 0

## Gaps

- True-device Android Chrome validation: `[pending]`; classification must be set before final release sign-off.
- True-device iOS Safari validation: `[pending]`; classification must be set before final release sign-off.
- True-device Edge mobile validation: `[pending]`; classification must be set before final release sign-off.
- True-device Firefox mobile validation: `[pending]`; classification must be set before final release sign-off.
