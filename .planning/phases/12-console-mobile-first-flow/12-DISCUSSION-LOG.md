# Phase 12: Console Mobile-First Flow - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-23
**Phase:** 12-Console Mobile-First Flow
**Areas discussed:** Chat-first layout, Composer and run controls, Catalog/session flow, Mobile E2E scope

---

## Area Selection

| Option | Description | Selected |
|--------|-------------|----------|
| All areas | Recommended: user-visible mobile core flow benefits from locking all four directions. | ✓ |
| Chat-first layout | Phone movement between Catalog, sessions, feed, and run context. | |
| Composer/run controls | Prompt input, send state, active-run status, and cancellation. | |
| Catalog/session flow | General Agent selection, history, continuation, active session. | |
| Mobile E2E scope | MVER-03 fake/no-key run, streamed events, sessions, cancel/terminal. | |

**User's choice:** 全部讨论.
**Notes:** User requested Chinese interaction and then selected all areas.

---

## Chat-First Layout

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phone default view | Chat Feed first | Default opens to conversation/event stream; Catalog, Sessions, Run Context are secondary. | ✓ |
| Phone default view | Catalog first | User selects agent before Chat. | |
| Phone default view | Session first | Continuation-first. | |
| Secondary areas access | Segmented switcher | In-page Agents/Sessions/Run segments/tabs. | ✓ |
| Secondary areas access | Bottom action bar | App-like bottom actions. | |
| Secondary areas access | Shell drawer | Reuse global drawer. | |
| Auxiliary panel behavior | Overlay or down-push allowed | Preserve Chat state and return context; exact mechanism flexible. | ✓ |
| Auxiliary panel behavior | Inline down-push | Panel expands in page. | |
| Auxiliary panel behavior | Overlay panel | Panel overlays Chat. | |
| Tablet/desktop layout | Responsive multi-column | Phone single-primary, wider screens restore multi-column Console. | ✓ |
| Tablet/desktop layout | Single-column all sizes | Consistent but weaker desktop. | |
| Tablet/desktop layout | Phone-only changes | Leaves desktop mostly as-is. | |

**User's choice:** Chat Feed优先; 分段切换栏; 面板覆盖/下推均可; 响应式多列.
**Notes:** This locks a Chat-first phone IA while preserving wider-screen Console efficiency.

---

## Composer and Run Controls

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Composer placement | Bottom sticky composer | Input and send remain reachable while scrolling event history. | ✓ |
| Composer placement | Normal page flow | Current behavior; may scroll out of view. | |
| Composer placement | Collapsible composer | Saves space but adds complexity. | |
| Run-state feedback | Composer inline status | Show queued/running/cancelling/terminal near input/send. | ✓ |
| Run-state feedback | Shell top status | Global but farther from composer. | |
| Run-state feedback | Feed status card | Natural but may scroll away. | |
| Cancel placement | Dual-position Cancel | Composer primary + shell/page backup. | ✓ |
| Cancel placement | Composer only | Close to input but may be obscured. | |
| Cancel placement | Shell only | Global but disconnected from composer. | |
| TextArea growth | Bounded auto-growth | Multi-line input with max-height and internal scroll. | ✓ |
| TextArea growth | Fixed height | Simple but weaker long prompt editing. | |
| TextArea growth | Unlimited growth | Comfortable editing but can crush feed/controls. | |

**User's choice:** 底部Sticky composer; Composer内联状态; 双位置Cancel; 有限自增长.
**Notes:** Composer/cancel visibility is a key mobile usability requirement for Phase 12.

---

## Catalog / Session Flow

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Agent Catalog presentation | Stacked Agent cards | Full-width mobile cards; General Agent prominent. | ✓ |
| Agent Catalog presentation | Compact list | Space efficient but weaker descriptions/actions. | |
| Agent Catalog presentation | General Agent only | Simplest but weakens Catalog. | |
| Session info | Summary + status + time | Short summary/title, recent status, updated time, active highlight. | ✓ |
| Session info | Title only | Minimal but unclear. | |
| Session info | Detailed history | Too dense for this phase. | |
| Selecting a session | Return to Chat and load history | Close/collapse Sessions, show active session, continue in Chat. | ✓ |
| Selecting a session | Stay in Sessions panel | Easier browsing but extra return step. | |
| Selecting a session | Separate detail page | Adds route/flow complexity. | |
| General Agent entry | Prominent primary CTA | Start/Continue main action; other entry actions secondary. | ✓ |
| General Agent entry | All actions equal | Preserves existing actions but increases choice cost. | |
| General Agent entry | Auto-default General | Reduces steps but weakens Catalog. | |

**User's choice:** 堆叠Agent卡片; 摘要+状态+时间; 回到Chat并加载历史; 主CTA突出.
**Notes:** Catalog remains a real product surface, but General Agent gets the primary mobile path.

---

## Mobile E2E Scope

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Main E2E depth | Full Console loop | Open Console, start General Agent, multi-line prompt, streamed events, session/history, cancel or terminal. | ✓ |
| Main E2E depth | Segmented smoke | Separate visible/click checks only. | |
| Main E2E depth | Java contract only | Fast but no real browser flow. | |
| Browser project minimum | Representative matrix main path | Mobile Chrome + Mobile Safari/WebKit + Tablet for main path; Firefox smoke if needed. | ✓ |
| Browser project minimum | All projects full loop | Strongest but high flake risk. | |
| Browser project minimum | Mobile Chrome only | Stable but too narrow. | |
| Streamed event assertions | Feed visible + incremental | Verify vertical feed, meaningful event progression, composer/cancel usable while scrolling; no tool detail assertions. | ✓ |
| Streamed event assertions | All event card details | Too much Phase 13 scope. | |
| Streamed event assertions | Any text only | Too weak. | |
| Desktop regression | Preserve desktop regression | Update/keep existing desktop Console spec after mobile refactor. | ✓ |
| Desktop regression | Mobile-only tests | Focused but risky. | |
| Desktop regression | Defer to Phase 15 | Too late for Console core changes. | |

**User's choice:** 完整Console闭环; 代表矩阵跑主路径; 事件Feed可见+增量; 保留桌面回归.
**Notes:** MVER-03 should be a real no-key mobile Console product path, not just smoke.

---

## the agent's Discretion

- Exact overlay vs inline/down-push implementation for secondary panels.
- Exact segmented control visuals, labels, and breakpoints.
- Exact sticky composer CSS and TextArea max-height.
- Exact placement mechanics for shell/page backup Cancel.
- Exact deterministic fake event sequence for Playwright.

## Deferred Ideas

- Full runtime/tool/approval card and dense detail redesign — Phase 13.
- Full Admin Governance mobile conversion — Phase 14.
- Broad orientation/cross-browser/accessibility/release hardening — Phase 15.
- Native/PWA/offline/mobile-only product enhancements — future/out of scope.
