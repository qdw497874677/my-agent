# Phase 15: Cross-Browser, Orientation, Accessibility, and Release Hardening - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-25
**Phase:** 15-cross-browser-orientation-accessibility-and-release-hardening
**Areas discussed:** Viewport/orientation matrix, desktop and cross-browser regression gates, accessibility hardening, release/UAT documentation

---

## Area Selection

| Option | Description | Selected |
|--------|-------------|----------|
| 全部讨论（推荐） | 一次性锁定视口/横竖屏矩阵、桌面回归范围、可访问性深度、发布/UAT 文档，便于后续规划不再追问。 | ✓ |
| 视口矩阵 | 决定 portrait/landscape/tablet 覆盖范围，以及是否复用现有项目或新增专门横屏项目。 | |
| 回归门禁 | 决定桌面与跨浏览器回归要跑到什么程度：所有既有 specs、关键路径、路由子集或 list-only。 | |
| 可访问性深度 | 决定是否只用现有 tap/focus helper，还是加入键盘 Tab 遍历、axe 类检查、landmark/label、reduced motion、drawer/dialog 检查。 | |
| 发布 UAT 文档 | 决定 Android Chrome、iOS Safari、Edge mobile、Firefox mobile 的真机期望与 CI/仿真缺口记录方式。 | |

**User's choice:** 全部讨论（推荐）
**Notes:** User asked for Chinese replies before continuing.

---

## Viewport and Orientation Matrix

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 15 的横竖屏验证范围应如何设定？ | 关键流+全路由（推荐） | 全 8 个路由做 portrait/landscape/tablet no-overflow 与导航检查；Console/Admin 关键路径做更深交互。覆盖充分但不爆炸。 | ✓ |
| Phase 15 的横竖屏验证范围应如何设定？ | 全路由全交互 | 每个路由在 portrait/landscape/tablet 都做关键交互。更强但容易慢且维护成本高。 | |
| Phase 15 的横竖屏验证范围应如何设定？ | 仅关键路径 | 只覆盖 Console 主流程和 Admin 代表页。最快，但 MVER-05 的全站信心较弱。 | |
| 是否需要在 Playwright 配置中新增专门的 landscape 项目？ | 测试内 setViewport（推荐） | 复用现有 browser projects，在 Phase 15 spec 内切换 portrait/landscape/tablet viewport，减少配置膨胀。 | ✓ |
| 是否需要在 Playwright 配置中新增专门的 landscape 项目？ | 新增 landscape projects | 为 Mobile Chrome/Safari/Firefox 增加独立 landscape project。报告清晰但项目矩阵膨胀。 | |
| 是否需要在 Playwright 配置中新增专门的 landscape 项目？ | 两者结合 | 少量新增 smoke project，同时在测试内做细分 viewport。更复杂。 | |
| Landscape 场景需要验证到什么程度？ | 导航+无溢出+关键控件（推荐） | 检查 shell/drawer、页面主内容、primary action、no horizontal overflow、触控控件可用。符合发布加固定位。 | ✓ |
| Landscape 场景需要验证到什么程度？ | 完整 Console run | 横屏也跑完整 Console fake run。更强但可能增加 flakiness。 | |
| Landscape 场景需要验证到什么程度？ | 只做无溢出 | 仅检测页面不横向溢出。太薄，可能漏掉导航/控件问题。 | |
| Tablet 验证应偏向哪种目标？ | 桥接手机与桌面（推荐） | 验证 tablet 既不能退化成拥挤手机，也不能出现桌面溢出；重点看 nav、Console 多列、Admin 卡片密度。 | ✓ |
| Tablet 验证应偏向哪种目标？ | 按手机处理 | tablet 主要沿用手机单列/抽屉行为，简单但可能浪费平板空间。 | |
| Tablet 验证应偏向哪种目标？ | 按桌面处理 | tablet 尽量使用桌面布局，效率高但风险是窄平板溢出。 | |

**User's choice:** Recommended options for all viewport/orientation questions.
**Notes:** Locked all-route orientation smoke plus deeper critical-flow checks, using in-test viewport switching.

---

## Desktop and Cross-Browser Regression Gates

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 15 的桌面回归门禁应覆盖到什么范围？ | 既有桌面+新汇总（推荐） | 保留/运行 Phase 05 桌面 Console 回归，并新增 Phase 15 汇总 gate 确认 Console/Admin 关键桌面路由未退化。 | ✓ |
| Phase 15 的桌面回归门禁应覆盖到什么范围？ | 所有 specs 都跑 desktop | 让 Phase 10-15 所有 Playwright specs 在 Desktop Chrome 下跑。最强但慢且可能有移动专用断言冲突。 | |
| Phase 15 的桌面回归门禁应覆盖到什么范围？ | 只跑 Phase 05 | 复用已有桌面 Console spec，不新增 Admin 桌面验证。较快但 Admin 回归信心不足。 | |
| 跨浏览器发布门禁应该怎么分层？ | 核心矩阵必跑（推荐） | Mobile Chrome/WebKit Safari proxy/Firefox proxy/Tablet/ Desktop Chrome 都跑 Phase 15 smoke；深交互只放在最稳定项目上。 | ✓ |
| 跨浏览器发布门禁应该怎么分层？ | 所有浏览器全深度 | 所有 browser project 都跑 Console/Admin 深交互。信心最高但容易慢和 flaky。 | |
| 跨浏览器发布门禁应该怎么分层？ | Chrome 深度，其它浅烟测 | Chrome 跑完整，WebKit/Firefox 只做路由加载和溢出。速度快但 Safari/Firefox 行为风险较高。 | |
| 遇到浏览器特有失败时，Phase 15 应如何处理？ | 修复或记录已知缺口（推荐） | 能用低风险 CSS/test 调整修复就修；真机/引擎限制无法修的，写入 release/UAT gap，不用跳过掩盖。 | ✓ |
| 遇到浏览器特有失败时，Phase 15 应如何处理？ | 全部必须修复 | 所有代理浏览器失败都必须修到绿。严格但可能被 CI 仿真限制卡住。 | |
| 遇到浏览器特有失败时，Phase 15 应如何处理？ | 允许跳过 flaky | 对不稳定浏览器直接 skip。快但发布文档可信度下降。 | |
| Phase 15 是否应引入截图视觉回归？ | 不作为主门禁（推荐） | 继续使用结构、选择器、溢出、交互、可访问性断言；截图可作为调试附件但不是 primary gate。 | ✓ |
| Phase 15 是否应引入截图视觉回归？ | 关键页面截图 | 对少量 Console/Admin 页面做截图 baseline。能发现视觉漂移，但 Vaadin/字体/浏览器差异会增加维护。 | |
| Phase 15 是否应引入截图视觉回归？ | 全站截图矩阵 | 每个路由/浏览器/视口截图。覆盖强但成本和 flakiness 高。 | |

**User's choice:** Recommended options for all regression gate questions.
**Notes:** Locked layered cross-browser gate and non-screenshot primary validation.

---

## Accessibility and Keyboard Hardening

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 15 的可访问性自动化深度应到哪里？ | 键盘+语义+焦点（推荐） | 在现有 tap/focus 基础上加入 Tab 顺序抽样、drawer/dialog/details 键盘可用、aria/label/heading/landmark 基础检查。 | ✓ |
| Phase 15 的可访问性自动化深度应到哪里？ | 加入 axe-core | 引入 axe 自动审计作为门禁。覆盖广，但要新增依赖并处理 Vaadin 组件/影子 DOM 的误报策略。 | |
| Phase 15 的可访问性自动化深度应到哪里？ | 只保留现有 helper | 继续只测 44px tap target 和 focus-visible。成本低但不符合最终 release hardening 深度。 | |
| 键盘遍历应覆盖哪些路径？ | 每类表面抽样（推荐） | 覆盖 shell drawer/nav、Console composer/panels/cancel、runtime Details/approval actions、Admin card Details/primary controls。 | ✓ |
| 键盘遍历应覆盖哪些路径？ | 全路由完整 Tab 链 | 每个路由从顶部 Tab 到底部。最细但慢且断言脆弱。 | |
| 键盘遍历应覆盖哪些路径？ | 只测 shell/nav | 只保证导航键盘可用。太窄，无法覆盖 converted surfaces。 | |
| 是否要在 Phase 15 加 prefers-reduced-motion / hover-only 检查？ | 加轻量合同（推荐） | 验证关键交互不依赖 hover；CSS 中加入/确认 reduced-motion 安全规则。适合发布加固。 | ✓ |
| 是否要在 Phase 15 加 prefers-reduced-motion / hover-only 检查？ | 暂不处理 | 只记录为后续增强。实现最少，但 accessibility/release completeness 较弱。 | |
| 是否要在 Phase 15 加 prefers-reduced-motion / hover-only 检查？ | 完整动效审计 | 审计所有 transition/animation。更彻底但超出当前阶段价值。 | |
| 可访问性失败的修复边界是什么？ | 低风险 UI/theme 修复（推荐） | 修 data hooks、aria-label、focus order、CSS focus/reduced-motion/hover fallback；不重构业务流或引入新组件体系。 | ✓ |
| 可访问性失败的修复边界是什么？ | 可重构组件结构 | 允许为了 a11y 重构 Console/Admin 组件。更强但可能扩大 Phase 15 范围。 | |
| 可访问性失败的修复边界是什么？ | 只文档化问题 | 发现问题只写 UAT gap，不修。适合真机限制，不适合可修的 UI 合同问题。 | |

**User's choice:** Recommended options for all accessibility questions.
**Notes:** Locked keyboard/semantic/focus hardening without mandatory axe-core gate.

---

## Release and UAT Documentation

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 15 的 release/UAT 文档应是什么形态？ | 发布清单+UAT 矩阵（推荐） | 新增 Phase 15 release hardening doc，包含 CI 覆盖、真机矩阵、手工步骤、已知仿真缺口、发布准入。 | ✓ |
| Phase 15 的 release/UAT 文档应是什么形态？ | 只写 HUMAN-UAT | 沿用前几阶段 UAT 模板，记录手工验证项。轻量但 release readiness 信息分散。 | |
| Phase 15 的 release/UAT 文档应是什么形态？ | 只更新既有 docs | 更新 phase-10/14 文档即可。改动少但 Phase 15 输出不够集中。 | |
| 真机/浏览器 UAT 应覆盖哪些目标？ | 四类浏览器必列（推荐） | Android Chrome、iOS Safari、Edge mobile、Firefox mobile 都列为 UAT 目标；CI 代理与真实设备差异逐项说明。 | ✓ |
| 真机/浏览器 UAT 应覆盖哪些目标？ | 只 Chrome/Safari | 聚焦主流移动浏览器。更快但不满足 MVER-07 对 Edge/Firefox 的明确记录。 | |
| 真机/浏览器 UAT 应覆盖哪些目标？ | 按可获得设备填写 | 只记录团队实际能测的设备。务实但可能漏掉要求中的浏览器族。 | |
| UAT 步骤需要细到什么程度？ | 关键路径脚本化清单（推荐） | 每个设备/浏览器按 Console run、Admin inspection、orientation switch、keyboard/focus、no-overflow 形成可执行步骤。 | ✓ |
| UAT 步骤需要细到什么程度？ | 高层 checklist | 只列检查点，不写详细操作步骤。更短但执行一致性差。 | |
| UAT 步骤需要细到什么程度？ | 完整逐页面手册 | 每路由每控件都写步骤。最全但维护成本高。 | |
| 发布准入如何处理未完成的真机验证？ | 显式 gap 分级（推荐） | CI 全绿是自动准入；真机未测/失败按 blocker/known limitation/follow-up 分类，不能含糊写通过。 | ✓ |
| 发布准入如何处理未完成的真机验证？ | 真机必须全绿 | 所有真机目标必须完成并通过才能发布。严格但可能受设备可得性阻塞。 | |
| 发布准入如何处理未完成的真机验证？ | 只要求 CI 全绿 | 真机仅文档化，不影响发布。快但弱化 MVER-07。 | |

**User's choice:** Recommended options for all release/UAT questions.
**Notes:** Locked concentrated Phase 15 release hardening doc with four-browser UAT matrix and explicit gap classification.

---

## the agent's Discretion

- Exact viewport dimensions and helper extraction.
- Exact deep-interaction distribution across browser projects.
- Exact release/UAT document filename and whether to pair it with a `15-HUMAN-UAT.md` template.
- Whether to research or add axe-core, as long as keyboard/semantic/focus checks are implemented reliably.

## Deferred Ideas

- Native mobile app, React/Next/Hilla rewrite, PWA/offline behavior, push/background monitoring, and new mobile-only Agent capabilities remain out of scope.
- Deep-linkable expanded mobile details, incident-triage shortcuts, event filtering, and mobile evidence copy/share remain future enhancements.
- Exhaustive device/browser/orientation permutations and screenshot visual regression are not Phase 15 primary gates.
