# Phase 11: Shared Responsive Shell and Navigation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-21
**Phase:** 11-shared-responsive-shell-and-navigation
**Areas discussed:** Shell 结构, 移动导航形态, 触控与焦点契约, 共享 UI primitives, Selector/测试契约

---

## Gray Areas Selected

The user selected all proposed discussion areas:

| Area | Covered |
|------|---------|
| Shell 结构 | Single shell vs dual shell vs per-view header; global Console/Admin relationship; old layout handling; implementation boundary |
| 移动导航形态 | Phone nav idiom; Admin sub-navigation; tablet/desktop behavior; active route/title presentation |
| 触控与焦点契约 | Touch target size; CSS enforcement strategy; focus-visible strategy; drawer focus behavior |
| 共享 UI primitives | Route title/content/action/card primitives; container strategy; page header; content rewrite boundary |
| Selector/测试契约 | Stable data hooks; all-route navigation tests; touch/focus assertions; Java/Playwright test layering |

---

## Shell 结构

### Phase 11 的整体 Shell 结构怎么定？

| Option | Description | Selected |
|--------|-------------|----------|
| 单一共享 Shell | 所有 8 个 Console/Admin 路由挂到同一个 Vaadin RouterLayout/shell，下层用区域/子导航区分 Console 与 Admin。最利于一致性和测试。 | ✓ |
| Console/Admin 双 Shell | Console 和 Admin 分别有自己的 shell，但共享底层 CSS tokens/primitives。更符合两个产品区，但导航与测试会有重复。 | |
| View 内组合 Header | 不引入 RouterLayout，每个 view 自己加 header/nav。改动局部，但长期容易不一致。 | |

**User's choice:** 单一共享 Shell
**Notes:** Locks the shell direction to a shared RouterLayout/shell for all current routes.

### `/console` 和 `/admin/governance*` 之间的关系，在 shell 里怎么呈现？

| Option | Description | Selected |
|--------|-------------|----------|
| 全局双入口 | 顶层只暴露 Console 与 Admin 两个主入口；Admin 内部再呈现 governance 子导航。符合 Phase 5 的两个产品区域决策。 | ✓ |
| 全部平铺 | 把 Console、Overview、Registry、Operations、Policy、Audits、Approvals 等都作为一层 nav。入口直达，但移动端会更拥挤。 | |
| Admin 为默认 | Shell 主要面向 Admin，Console 只是一个链接。适合治理优先，但弱化用户 Console。 | |

**User's choice:** 全局双入口
**Notes:** Console/Admin stay distinct product areas while sharing one shell.

### Phase 11 要如何处理现有 `AdminGovernanceLayout` 占位路由和无路由的 `MainConsoleLayout`？

| Option | Description | Selected |
|--------|-------------|----------|
| 替换/收敛 | 用新 shared shell 替换/收敛这些旧布局；`/admin/governance` 保留为 Admin landing，但不再作为伪 layout。 | ✓ |
| 保留兼容 | 尽量保留现状，只在外层套 shell；旧类能不动就不动。风险是 zombie layout 继续存在。 | |
| 重新命名复用 | 把其中一个改造成新的 shell/layout 基类。节省类数量，但可能继承旧语义。 | |

**User's choice:** 替换/收敛
**Notes:** Downstream should avoid leaving zombie/competing layout classes.

### Shell 的实现边界怎么锁定？

| Option | Description | Selected |
|--------|-------------|----------|
| Adapter Web Only | 所有 Vaadin shell/nav/theme/test hook 只在 `pi-agent-adapter-web`；不改 Domain/App/client DTO，不加后端 viewport/mobile API。 | ✓ |
| 允许少量 API 支持 | 如果导航需要动态状态，可加少量公开 read-model API。更灵活，但可能扩大 Phase 11。 | |
| 先做静态导航 | 只做静态 route registry/nav，不考虑任何状态。最小，但可能错过 status/action placement 的基础。 | |

**User's choice:** Adapter Web Only
**Notes:** No backend/mobile API changes in Phase 11.

---

## 移动导航形态

### 手机端主导航形态选哪种？

| Option | Description | Selected |
|--------|-------------|----------|
| Header + Drawer | 顶部 compact header + 汉堡抽屉；适合 8 个路由与 Admin 子导航，空间占用低。 | ✓ |
| 顶部 Tabs | 适合少量入口；Admin 子路由较多时会横向拥挤。 | |
| 底部 Tab Bar | 移动 App 感强，常驻可达；但 Admin 多子页不适合全部放底部，且桌面/平板一致性较差。 | |

**User's choice:** Header + Drawer

### Admin Governance 的子导航怎么呈现？

| Option | Description | Selected |
|--------|-------------|----------|
| Drawer 内分组 | 抽屉里分 Console/Admin 分组；Admin 下展示 Overview/Registry/Operations/Policy/Audits/Approvals。清晰且不占内容宽度。 | ✓ |
| 进入 Admin 后顶部 Tabs | 顶层抽屉只切 Console/Admin，Admin 页面内显示 tabs。上下文强，但移动端可能占垂直空间。 | |
| 只在 Overview 跳转 | Admin 子页仍通过 Overview 的链接进入。最小改动，但不满足“所有区域导航”体验。 | |

**User's choice:** Drawer 内分组

### 桌面/平板上的导航形态怎么处理？

| Option | Description | Selected |
|--------|-------------|----------|
| 响应式同源 | 同一 route/nav registry；手机折叠成 drawer，平板/桌面可显示固定侧栏或展开导航。测试和维护一致。 | ✓ |
| 手机独立形态 | 手机用 drawer，桌面基本保持现状。风险是两套 IA 分叉。 | |
| 始终 Drawer | 所有宽度都用 header + drawer。最一致，但桌面体验可能不如固定侧栏。 | |

**User's choice:** 响应式同源

### 当前活跃路由和页面标题如何呈现？

| Option | Description | Selected |
|--------|-------------|----------|
| Header标题+Nav高亮 | header 显示当前 route title；nav item 有 active 状态；Admin 子页显示二级标题。满足 SC4 和测试可断言。 | ✓ |
| 只高亮 Nav | 标题由各 view 自己负责。较少改动，但一致性弱。 | |
| 面包屑样式 | 显示 Console / Admin / Registry 之类层级面包屑。信息清晰，但 Phase 11 可能略重。 | |

**User's choice:** Header标题+Nav高亮

---

## 触控与焦点契约

### 触控目标尺寸下限用哪个标准？

| Option | Description | Selected |
|--------|-------------|----------|
| 44px 下限 | CSS token 设 `--pi-mobile-tap-target: 44px`，对 links/buttons/toggles/expanders/nav items 等统一约束；符合常见移动可访问性下限。 | ✓ |
| 48px 下限 | 更宽松、更接近 Material 指南，但对紧凑 Admin 数据页占空间更大。 | |
| 按组件决定 | 只对关键 controls 做尺寸约束。最灵活，但测试和一致性弱。 | |

**User's choice:** 44px 下限

### 触控尺寸应该如何落地？

| Option | Description | Selected |
|--------|-------------|----------|
| 全局CSS+例外 | 在 `pi-mobile` theme 中为可交互元素建立默认 min-height/min-width/spacing，特殊紧凑组件必须显式 opt-out/补测试。 | ✓ |
| 只用class约束 | 新增 `.pi-touch-target` 等 class，逐个组件 opt-in。改动可控，但容易漏。 | |
| 只测关键动作 | 不做全局样式约束，只在 Playwright 检查 primary action。风险是大量次级操作不达标。 | |

**User's choice:** 全局CSS+例外

### focus-visible 和键盘/平板焦点策略怎么定？

| Option | Description | Selected |
|--------|-------------|----------|
| 全局focus token | 在主题内统一 `:focus-visible` outline/ring token；nav、drawer trigger、links、buttons、details/expanders 都要可见。 | ✓ |
| 仅导航焦点 | Phase 11 只保证 shell/nav 焦点，复杂内容区留给 Phase 12-14。更窄，但 SC2/SC3 涉及大量控件。 | |
| 浏览器默认 | 依赖默认 outline。改动少，但视觉一致性和可测试性弱。 | |

**User's choice:** 全局focus token

### Drawer 焦点行为 Phase 11 要做到多严格？

| Option | Description | Selected |
|--------|-------------|----------|
| 基础可用 | drawer 打开后触发器、关闭按钮、nav items 顺序可用；关闭后焦点返回触发器；不强制完整复杂 focus-trap。 | ✓ |
| 完整focus trap | drawer 打开后焦点限制在 drawer 内、Esc 关闭、返回触发器。最完整，但实现和测试更重。 | |
| 只做可见焦点 | 不定义返回/顺序契约，只保证可见 outline。更轻，但可能不满足键盘用户。 | |

**User's choice:** 基础可用

---

## 共享 UI primitives

### Phase 11 的共享 UI primitives 要包含哪些？

| Option | Description | Selected |
|--------|-------------|----------|
| Shell基础四件套 | route title、content container、status/action slot、基础 card/detail surface。足够支撑 Phase 12-14，又不进入具体业务重排。 | ✓ |
| 只做Shell/Nav | 仅 header/drawer/nav，不做 page primitives。更窄，但 SC4 的一致标题/容器/动作区会不足。 | |
| 做完整组件库 | 把 Console/Admin 常用 cards、lists、details、empty/error/loading 都抽出。长期好，但 Phase 11 过大。 | |

**User's choice:** Shell基础四件套

### 页面内容容器的宽度/布局策略怎么定？

| Option | Description | Selected |
|--------|-------------|----------|
| 统一容器+变体 | 统一 `.pi-page`/`.pi-content` 容器，提供 narrow/default/wide 或 admin-dense 变体；手机全宽安全边距，桌面限制最大宽度。 | ✓ |
| 各页面自管 | Shell 只包 outlet，内容宽度由每个 view 自己处理。改动少但一致性弱。 | |
| 全部全宽 | 所有页面都全宽，只靠内部 card/grid。适合 dense Admin，但 Console 阅读体验可能差。 | |

**User's choice:** 统一容器+变体

### 页面标题、状态、主动作的位置怎么规范？

| Option | Description | Selected |
|--------|-------------|----------|
| 统一PageHeader | 每个 route 使用共享 page header 区：title/subtitle/status/primary action slot；移动端垂直堆叠，宽屏水平排列。 | ✓ |
| Shell只显示标题 | header 显示标题，状态/动作继续留在 view 内。更轻，但“status/action placement”一致性不足。 | |
| 只规范CSS类 | 不新增 Java component，只要求 view 用统一 class。灵活但执行容易漂移。 | |

**User's choice:** 统一PageHeader

### Phase 11 是否要开始改具体 Console/Admin 内容卡片？

| Option | Description | Selected |
|--------|-------------|----------|
| 只建基础样式 | 只提供 card/detail/list/action-row 基础 class/component，现有页面只做最低限度接入；完整 Console/Admin 内容重排留给 Phase 12/14。 | ✓ |
| 顺手改高风险页 | 顺便把 Console 和 Registry 等高风险页做更多卡片化。能改善体验，但会侵入后续 Phase。 | |
| 不碰内容区 | 只做外壳导航，不改任何 page body。边界清晰，但 SC4 可能不达标。 | |

**User's choice:** 只建基础样式

---

## Selector/测试契约

### Shell/Nav 的稳定 selector 契约要标准化到什么程度？

| Option | Description | Selected |
|--------|-------------|----------|
| 完整标准化 | 新增 `data-shell`、`data-nav`、`data-nav-item`、`data-nav-active`、`data-page-title`、`data-primary-action`，并延续 Phase 10 的 data-* 契约。 | ✓ |
| 只加最少hook | 只加 `data-shell`/`data-nav`/`data-nav-item`。少改动，但 PageHeader/action 测试弱。 | |
| 靠可访问名称 | 尽量使用 role/name，不新增太多 data hook。更用户语义化，但 dense Vaadin 页面和多语言文案下较脆。 | |

**User's choice:** 完整标准化

### Phase 11 Playwright 应该验证哪些导航行为？

| Option | Description | Selected |
|--------|-------------|----------|
| 全路由导航门禁 | 在 Phase 10 route smoke 基础上，手机/平板/桌面项目验证 shell 可见、drawer 可打开、每个 nav item 可到达目标 route、active 状态、无横向溢出。 | ✓ |
| 仅手机门禁 | 主要验证手机 nav；桌面沿用 Phase 10 route smoke。更快，但响应式同源风险没覆盖。 | |
| 少量代表路由 | 只抽 console、admin overview、registry 三个 route。快，但不能证明所有区域可导航。 | |

**User's choice:** 全路由导航门禁

### 触控尺寸和焦点如何测试？

| Option | Description | Selected |
|--------|-------------|----------|
| 关键控件断言 | Playwright 对 nav items、drawer trigger、primary actions、关键 expanders/buttons 做 boundingBox >=44px 与 focus-visible/焦点返回抽样断言。 | ✓ |
| 只做Java/CSS契约 | Java contract 检查 theme tokens/hooks，少量或不做 browser 尺寸断言。快但真实保障较弱。 | |
| 完整a11y扫描 | 引入更广泛可访问性扫描/键盘巡航。更全面，但 Phase 11 可能过重，适合 Phase 15 扩展。 | |

**User's choice:** 关键控件断言

### 测试实现分层怎么定？

| Option | Description | Selected |
|--------|-------------|----------|
| Java契约+Playwright | 新增快速 Java contract test 验证 shell/theme/data hooks；新增/扩展 Playwright 验证真实导航、触控和焦点；复用 Phase 10 helpers。 | ✓ |
| 只Playwright | 用浏览器测试覆盖全部。真实但慢，失败定位可能差。 | |
| 只Java契约 | 最快，但无法验证实际 drawer/nav 行为。 | |

**User's choice:** Java契约+Playwright

---

## the agent's Discretion

- Exact shell class/package names, CSS class names, breakpoint values, icons, typography, and whether to use Vaadin `AppLayout` vs custom free-component `RouterLayout`, subject to research and licensing/dependency checks.

## Deferred Ideas

- Full Console mobile-first flow — Phase 12.
- Runtime/tool/approval card/dialog UX — Phase 13.
- Full Admin Governance card/detail migration — Phase 14.
- Final cross-browser/orientation/accessibility/UAT hardening — Phase 15.
