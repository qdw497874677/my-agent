# Phase 5: Agent Web Console and Runtime Cockpit - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-15
**Phase:** 05-Agent Web Console and Runtime Cockpit
**Areas discussed:** Console Information Architecture, Run Cockpit Experience, Tool Cards and Approval Flow, Admin Governance Views, Browser E2E Verification

---

## Gray Area Selection

| Option | Description | Selected |
|--------|-------------|----------|
| 全部讨论 | UI 重阶段，同时有产品体验、安全审批、API 缺口和 E2E 决策；全讨论更利于后续规划。 | ✓ |
| Console 信息架构 | Agent Catalog、导航、页面结构、用户第一眼看到什么。 | |
| Run Cockpit 体验 | Chat 输入、流式输出、timeline、Session history、取消运行。 | |
| 工具与审批卡片 | 工具调用卡片字段、风险提示、进度、错误，以及 approve/reject 流程。 | |
| Admin 治理视图 | provider、extension、MCP、plugin、tool registry、policy、audit 哪些做 v1 基础版，哪些做占位。 | |
| 浏览器 E2E | Vaadin 浏览器 E2E 的测试栈选择和覆盖深度。 | |

**User's choice:** 全部讨论

---

## Console Information Architecture

| Decision Question | Option | Description | Selected |
|-------------------|--------|-------------|----------|
| Agent Web Console 的主导航应该怎么组织？ | Catalog → Cockpit → Admin | 三个一级区：Agent Catalog 作为入口，Run Cockpit 承接执行，Admin Governance 单独成区。 | |
| Agent Web Console 的主导航应该怎么组织？ | Dashboard 优先 | 首页先展示最近会话、运行状态、告警，再进入 Catalog/Admin。 | |
| Agent Web Console 的主导航应该怎么组织？ | Chat 优先 | 打开即进入默认 Agent Chat，Catalog 和 Admin 放侧边；更像聊天产品。 | ✓ |
| Agent Catalog 卡片应该展示到什么粒度？ | 运行决策信息 | 名称、描述、支持输入模式、模型引用、允许工具/风险标签、入口按钮。 | ✓ |
| Agent Catalog 卡片应该展示到什么粒度？ | 极简列表 | 只展示名称、描述、进入按钮。 | |
| Agent Catalog 卡片应该展示到什么粒度？ | 治理详情全展示 | 把 provider、policy、tool scopes、limits 全部放 Catalog。 | |
| 普通用户和 Admin 的入口要怎么区分？ | 同一 Console 分区 | 同一个 Vaadin 应用内通过导航分区和权限控制区分 User/Admin。 | |
| 普通用户和 Admin 的入口要怎么区分？ | 两个独立 UI | User Console 与 Admin Console 分开路由/布局。 | ✓ |
| 普通用户和 Admin 的入口要怎么区分？ | 先不区分 | 所有页面都可见。 | |
| Agent Catalog 的 Agent 来源首期怎么处理？ | 后端只读 Catalog API | Phase 5 补 `/api/agents` 或等价 read-model，UI 只消费公开 API。 | ✓ |
| Agent Catalog 的 Agent 来源首期怎么处理？ | Vaadin 内部静态列表 | 最快做出界面，但违反“Web GUI 使用公开 API”原则。 | |
| Agent Catalog 的 Agent 来源首期怎么处理？ | 完整 Agent 管理 | 支持创建/编辑/发布 Agent；属于 Agent Studio 能力。 | |

**User's choice:** Chat 优先；运行决策信息；两个独立 UI；后端只读 Catalog API。
**Notes:** 用户选择进入下一块，不继续细化 IA。

---

## Run Cockpit Experience

| Decision Question | Option | Description | Selected |
|-------------------|--------|-------------|----------|
| Run Cockpit 页面布局首期应采用哪种形态？ | Chat 主区 + 右侧 Timeline | 主区聊天与输入，右侧展示 Run 状态、事件、工具卡。 | |
| Run Cockpit 页面布局首期应采用哪种形态？ | 单列聊天流 | 所有消息、事件、工具卡都在一个流里。 | |
| Run Cockpit 页面布局首期应采用哪种形态？ | 三栏工作台 | 左 Session 列表、中 Chat、右 Timeline/详情。 | ✓ |
| 流式输出和运行事件应该怎么呈现？ | 回答流 + 事件分层 | model text delta 进入回答区域，run/tool/policy 事件进入 timeline/card。 | |
| 流式输出和运行事件应该怎么呈现？ | 全部事件进聊天流 | 所有消息、运行事件、工具事件都进入聊天流。 | ✓ |
| 流式输出和运行事件应该怎么呈现？ | 只显示最终回答 | 不展示 streaming/timeline 细节。 | |
| Session history / continue 首期要做到什么程度？ | 左侧最近 Sessions + 续聊 | 可选历史 Session，加载消息/工具/结果，继续发新 Run。 | ✓ |
| Session history / continue 首期要做到什么程度？ | 只在当前页历史 | 仅显示当前 Session 的历史。 | |
| Session history / continue 首期要做到什么程度？ | 完整搜索归档 | 支持搜索、过滤、归档、分支。 | |
| 取消运行的 UX 应该怎么做？ | 明显 Cancel + 状态反馈 | running 时显示 Cancel，点击后 timeline/stream 展示 cancelling/cancelled。 | ✓ |
| 取消运行的 UX 应该怎么做？ | 无确认直接取消 | 更快，但误触风险高。 | |
| 取消运行的 UX 应该怎么做？ | 只在 Admin 可取消 | 治理更强，但 GUI-05 不完整。 | |

**User's choice:** 三栏工作台；全部事件进聊天流；左侧最近 Sessions + 续聊；明显 Cancel + 状态反馈。
**Notes:** 用户选择进入下一块，不继续细化 Cockpit。

---

## Tool Cards and Approval Flow

| Decision Question | Option | Description | Selected |
|-------------------|--------|-------------|----------|
| 工具调用卡片在聊天流里默认展示多少信息？ | 摘要卡 + 可展开详情 | 默认展示工具名、状态、风险/副作用、目的、进度、脱敏摘要；展开看事件序列/错误。 | ✓ |
| 工具调用卡片在聊天流里默认展示多少信息？ | 完整详情常显 | 所有参数、事件、摘要都展开。 | |
| 工具调用卡片在聊天流里默认展示多少信息？ | 极简状态行 | 只显示工具名和状态。 | |
| 审批卡片应该包含哪些用户决策信息？ | 风险 + 预览 + 后果 | 展示工具、原因、risk/side-effect、preview/impact、脱敏参数摘要、Approve/Reject。 | ✓ |
| 审批卡片应该包含哪些用户决策信息？ | 只问是否批准 | 快，但审批质量差。 | |
| 审批卡片应该包含哪些用户决策信息？ | 要求用户编辑参数 | 引入 Tool Studio/表单编辑复杂度。 | |
| Approve/Reject 后 Run 应该如何继续？ | 原 Run 恢复/终止 | Approve 恢复等待中的 tool/run，Reject 产生 rejected/policy terminal 或可恢复结果。 | ✓ |
| Approve/Reject 后 Run 应该如何继续？ | 新建 Run 继续 | 语义割裂，Session/Run timeline 不连续。 | |
| Approve/Reject 后 Run 应该如何继续？ | 只记录审批不恢复 | 只能演示 UI，无法满足 GUI-06。 | |
| 审批权限首期怎么控制？ | User 或 Admin 可审批 | 符合 GUI-06；后端基于 principal/role 预留控制，dev auth 下可模拟。 | ✓ |
| 审批权限首期怎么控制？ | 只有 Admin | 普通用户场景不完整。 | |
| 审批权限首期怎么控制？ | 所有登录用户 | 开发简单，但治理边界弱。 | |

**User's choice:** 摘要卡 + 可展开详情；风险 + 预览 + 后果；原 Run 恢复/终止；User 或 Admin 可审批。
**Notes:** 用户选择进入下一块，不继续细化工具/审批卡。

---

## Admin Governance Views

| Decision Question | Option | Description | Selected |
|-------------------|--------|-------------|----------|
| Admin Governance 首期首页应该优先呈现什么？ | 运行健康 + 注册表概览 | provider/tool/extension/MCP/plugin/policy/audit 的状态摘要和入口。 | ✓ |
| Admin Governance 首期首页应该优先呈现什么？ | 审计日志优先 | 安全治理导向，但弱化 provider/tool/catalog 管理入口。 | |
| Admin Governance 首期首页应该优先呈现什么？ | 工具注册表优先 | 复用 `/api/tools` 最容易落地，但 GUI-07 覆盖面不够。 | |
| Phase 5 对 extension/MCP/plugin 这些后续阶段对象应该怎么展示？ | 只读占位状态 | 展示“未配置/后续阶段启用”的占位卡和空列表，不实现配置/加载能力。 | ✓ |
| Phase 5 对 extension/MCP/plugin 这些后续阶段对象应该怎么展示？ | 不展示 | 更简洁，但 GUI-07 覆盖弱。 | |
| Phase 5 对 extension/MCP/plugin 这些后续阶段对象应该怎么展示？ | 提前实现配置入口 | 侵入 Phase 6/7/8 范围。 | |
| Policy decisions 和 Audit records 首期怎么呈现？ | 最近记录只读列表 | 按 run/tool/session 展示最近 policy decision/audit 摘要，支持进入详情，全部脱敏。 | ✓ |
| Policy decisions 和 Audit records 首期怎么呈现？ | 只在 Run 详情里看 | Admin 治理面不完整。 | |
| Policy decisions 和 Audit records 首期怎么呈现？ | 完整审计搜索过滤 | 搜索/过滤/导出是后续治理增强。 | |
| Admin 是否允许修改配置？ | Phase 5 只读 | 先做 inspect/governance visibility，不做配置变更。 | ✓ |
| Admin 是否允许修改配置？ | 允许启停/编辑部分配置 | 扩大到配置管理和权限审计。 | |
| Admin 是否允许修改配置？ | 允许全部治理操作 | 超出 Phase 5。 | |

**User's choice:** 运行健康 + 注册表概览；只读占位状态；最近记录只读列表；Phase 5 只读。
**Notes:** 用户选择进入下一块，不继续细化 Admin。

---

## Browser E2E Verification

| Decision Question | Option | Description | Selected |
|-------------------|--------|-------------|----------|
| Web Console 浏览器 E2E 测试栈首期选哪个？ | Playwright | 跨浏览器、现代、适合验证真实 UI/SSE；Node 测试工具只用于测试。 | ✓ |
| Web Console 浏览器 E2E 测试栈首期选哪个？ | Selenium | 纯 Java 生态更一致，但编写和稳定性通常不如 Playwright 轻快。 | |
| Web Console 浏览器 E2E 测试栈首期选哪个？ | Vaadin TestBench | Vaadin 官方路线，但商业/许可和生态约束需要确认。 | |
| 浏览器 E2E 覆盖深度首期到哪里？ | 一条完整 happy path + 关键分支 | Catalog→Chat→stream→tool card→approval→history→cancel→Admin read-only；用 fake runtime。 | ✓ |
| 浏览器 E2E 覆盖深度首期到哪里？ | 只做烟测 | 加载页面和基础导航。 | |
| 浏览器 E2E 覆盖深度首期到哪里？ | 全面矩阵测试 | 每个页面/状态都测。 | |
| E2E 数据和运行环境怎么组织？ | No-key fake runtime | 复用 TestCloudRuntimeConfiguration/FakeModel/FakeTool，覆盖流式和审批，不依赖真实模型/外部服务。 | ✓ |
| E2E 数据和运行环境怎么组织？ | 真实 provider smoke | 不适合默认 CI，会引入 key 管理。 | |
| E2E 数据和运行环境怎么组织？ | 纯 mock 前端数据 | 不能验证 REST/SSE/read-model 集成。 | |
| UI 测试对截图/视觉回归有要求吗？ | 行为优先，少量截图 | 以功能/E2E 为门槛，保留关键页面截图作为调试产物。 | ✓ |
| UI 测试对截图/视觉回归有要求吗？ | 必须视觉回归 | 引入基线维护成本。 | |
| UI 测试对截图/视觉回归有要求吗？ | 不需要截图 | 失败排查和 UI 安全门弱一些。 | |

**User's choice:** Playwright；一条完整 happy path + 关键分支；No-key fake runtime；行为优先，少量截图。

---

## the agent's Discretion

- Exact Vaadin component structure, route names, styling, responsive breakpoints, icons, copy, and detailed UI states were left to downstream planning/design discretion.
- Exact API endpoint names for Agent Catalog, approval action/resume, admin read models, policy decisions, and audit summaries were left to planner discretion, constrained by the public REST/SSE/read-model boundary.

## Deferred Ideas

- Agent create/edit/publish and full Agent Studio.
- Extension/MCP/plugin configuration or lifecycle actions before their roadmap phases.
- Full audit search/filter/export.
- Full visual regression baseline suite.
