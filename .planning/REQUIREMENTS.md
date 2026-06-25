# Requirements: Pi Java Agent Platform

**Defined:** 2026-06-20  
**Milestone:** v1.1 适配移动端web  
**Core Value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一、通用的 Runtime 运行、观测和治理。

## Milestone v1.1 Requirements

Scope: convert the existing Vaadin Web Console and Admin Governance from desktop-first browser UI into a mobile-first, full-site H5 experience. This milestone adapts existing surfaces and verification gates; it does not add new Agent runtime/tool/model capabilities or a new frontend stack.

### Mobile Foundation

- [x] **MH5-01**: Mobile user can open every existing Console and Admin Governance route at representative phone viewports without blank screens, route errors, or desktop-only blocking messages.
- [x] **MH5-02**: Mobile user can navigate all Console and Admin sections through a touch-friendly responsive shell, compact header, drawer/tabs, or equivalent mobile navigation.
- [x] **MH5-03**: Mobile user does not encounter page-level horizontal overflow at representative phone, phone landscape, and tablet viewports.
- [x] **MH5-04**: Touch user can reliably tap primary links, buttons, toggles, approvals, cancel controls, refresh controls, and details expanders using mobile-safe target sizes and spacing.
- [x] **MH5-05**: Keyboard/tablet user retains visible focus indicators and usable drawer/dialog/navigation focus order after mobile layout changes.

### Console Mobile Experience

- [x] **MCON-01**: Mobile user can browse Agent Catalog as stacked cards and select/start the General Agent without desktop-width layout.
- [x] **MCON-02**: Mobile user can type a multi-line chat prompt, submit it, and see active run/composer state in a mobile-first Chat/Run flow.
- [x] **MCON-03**: Mobile user can observe live SSE run output/events in a vertical feed and scroll previous events without losing access to current run controls.
- [x] **MCON-04**: Mobile user can open session history, select a past session, continue it, and clearly see the active session.
- [x] **MCON-05**: Mobile user can cancel an active run from a visible touch-safe control and see terminal/cancelling feedback.

### Runtime Cards, Timeline, Tools, and Approvals

- [x] **MCARD-01**: Mobile user can inspect run timeline events as compact cards or accordions with status, timestamp/type, summary, and expandable details.
- [x] **MCARD-02**: Mobile user can inspect tool cards with tool name, source, status, policy/approval state, duration, error, and redacted input/output summaries.
- [x] **MCARD-03**: Mobile user can expand dense run/tool/policy/audit details without exposing raw sensitive payloads or causing page-level horizontal overflow.
- [x] **MCARD-04**: Mobile user can approve or reject a pending tool approval from a risk-first card that clearly shows side-effect context and requires intentional action.
- [x] **MCARD-05**: Mobile user sees dialogs, drawers, notifications, and confirmations fit the viewport with safe scrolling and explicit close/action controls.

### Admin Governance Mobile Coverage

- [x] **MADM-01**: Mobile admin can read Governance Overview as stacked status cards with runtime/provider/tool/extension/MCP/plugin health, counts, messages, and links.
- [x] **MADM-02**: Mobile admin can inspect Registry and Operations data as cards or responsive row details without relying on page-level horizontal table scrolling.
- [x] **MADM-03**: Mobile admin can inspect MCP server/tool status, refresh/status metadata where already supported, and unhealthy/disconnected states.
- [x] **MADM-04**: Mobile admin can inspect Plugin state, selected/disabled/quarantined/load errors, and available plugin metadata in stacked card/detail layouts.
- [x] **MADM-05**: Mobile admin can inspect Extension sources, contributions, providers, tools, listeners, status, and expandable metadata.
- [x] **MADM-06**: Mobile admin can inspect Policy decisions with decision, reason, tool/run/session IDs, timestamp, and expandable redacted context.
- [x] **MADM-07**: Mobile admin can inspect Audit summaries with actor/source/action/status/timestamp and expandable redacted details.

### Mobile Verification and Release Gates

- [x] **MVER-01**: Automated browser tests run representative Mobile Chrome, Mobile Safari/WebKit, Mobile Firefox or Firefox mobile viewport, and tablet contexts where supported by CI.
- [x] **MVER-02**: Mobile smoke tests verify route load, no page-level horizontal overflow, visible primary action, and at least one key interaction per route category.
- [x] **MVER-03**: Console mobile E2E starts a fake/no-key run, observes streamed event UI, opens tool/approval/session areas, and cancels or reaches terminal status.
- [x] **MVER-04**: Admin mobile E2E opens overview, registry, operations, MCP, plugin, extension, policy, and audit pages and verifies mobile card/detail content.
- [x] **MVER-05**: Representative portrait, landscape, and tablet viewports pass mobile navigation and no-horizontal-overflow checks.
- [x] **MVER-06**: Desktop Web Console/Admin browser regressions remain passing after mobile-first changes.
- [x] **MVER-07**: Release documentation records real-device/UAT expectations for Android Chrome, iOS Safari, Edge mobile, and Firefox mobile, including any CI/emulation gaps.

## Future Requirements

Deferred beyond v1.1 unless explicitly pulled into a later milestone.

### Mobile Product Enhancements

- **MOB-FUT-01**: Native iOS/Android app.
- **MOB-FUT-02**: Push notifications or background run monitoring.
- **MOB-FUT-03**: Offline/PWA admin mode that caches governance/audit data.
- **MOB-FUT-04**: Mobile-specific new Agent capabilities beyond adapting existing Web Console/Admin behavior.
- **MOB-FUT-05**: Deep-linkable expanded mobile details, incident-triage shortcuts, event filtering, and mobile evidence copy/share.

## Out of Scope

Explicitly excluded for v1.1 to keep the milestone focused on verified full-site H5 adaptation.

| Feature | Reason |
|---------|--------|
| React/Next.js/Hilla rewrite | Violates current Java/Vaadin-first product boundary and adds a second frontend stack. |
| Native mobile app | This milestone is H5/mobile browser support, not app-store delivery. |
| Mobile-only reduced product | User selected full-site coverage, including Admin Governance. |
| Horizontal-scroll table as default admin solution | Fails mobile-first usability; dense admin data should use cards/details. |
| Offline admin cache | Sensitive governance/audit data needs separate security design. |
| Runtime/model/tool capability expansion | This is UI adaptation and verification, not Agent runtime expansion. |
| Pixel-perfect support for every device model | Use representative mobile/tablet breakpoints and browser families instead of unmaintainable per-device layouts. |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| MH5-01 | Phase 10 | Complete |
| MH5-02 | Phase 11 | Complete |
| MH5-03 | Phase 10 | Complete |
| MH5-04 | Phase 11 | Complete |
| MH5-05 | Phase 11 | Complete |
| MCON-01 | Phase 12 | Complete |
| MCON-02 | Phase 12 | Complete |
| MCON-03 | Phase 12 | Complete |
| MCON-04 | Phase 12 | Complete |
| MCON-05 | Phase 12 | Complete |
| MCARD-01 | Phase 13 | Complete |
| MCARD-02 | Phase 13 | Complete |
| MCARD-03 | Phase 13 | Complete |
| MCARD-04 | Phase 13 | Complete |
| MCARD-05 | Phase 13 | Complete |
| MADM-01 | Phase 14 | Complete |
| MADM-02 | Phase 14 | Complete |
| MADM-03 | Phase 14 | Complete |
| MADM-04 | Phase 14 | Complete |
| MADM-05 | Phase 14 | Complete |
| MADM-06 | Phase 14 | Complete |
| MADM-07 | Phase 14 | Complete |
| MVER-01 | Phase 10 | Complete |
| MVER-02 | Phase 10 | Complete |
| MVER-03 | Phase 12 | Complete |
| MVER-04 | Phase 14 | Complete |
| MVER-05 | Phase 15 | Complete |
| MVER-06 | Phase 15 | Complete |
| MVER-07 | Phase 15 | Complete |

**Coverage:**
- v1.1 requirements: 29 total
- Mapped to phases: 29 ✓
- Unmapped: 0 ✓
- Duplicate mappings: 0 ✓

---
*Requirements defined: 2026-06-20*
*Last updated: 2026-06-20 after milestone v1.1 roadmap creation*
