# Phase 17: Console Session Restore UX - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-28
**Phase:** 17-Console Session Restore UX
**Areas discussed:** Recent History Entry Point, Active Session Identity, Transcript Hydration and Bubble Shape

---

## Gray Areas Selected

The user selected these areas for discussion:

| Area | Selected |
|------|----------|
| 历史入口形态 | ✓ |
| Active session 标识 | ✓ |
| 气泡恢复深度 | ✓ |
| Phase 17/18 边界 | |
| 细节折叠入口 | |
| 滚动与空状态 | |
| 预存测试失败处理 | |

---

## Recent History Entry Point

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| 历史会话入口主形态 | 左侧紧凑栏 | 桌面/平板显示左侧 recent sessions；手机通过 History 按钮/面板打开。贴近 Kimi/ChatGPT，复用 `SessionListPanel` 和现有三栏/面板状态。 | ✓ |
| 历史会话入口主形态 | 首页横向列表 | 在输入区/hero 附近显示最近会话卡片；更“首页感”，但长历史和移动端滚动更拥挤。 | |
| 历史会话入口主形态 | 折叠历史区 | 默认只显示 History 按钮，展开后显示列表；最干净，但 CIA-01 recent history 可见性较弱。 | |
| 手机端选择 session 后 | 选择后回 Chat | 沿用 Phase 12 D-12：点历史后立即回到 Chat，清空当前 feed 并 hydrate transcript，突出 active session。 | ✓ |
| 手机端选择 session 后 | 停留历史面板 | 用户可继续浏览历史；但需要额外操作回 Chat，容易弱化恢复结果。 | |
| 手机端选择 session 后 | 分屏/下推显示 | 历史和聊天同屏展示；适合大屏，手机上复杂度高。 | |
| 最近会话卡片信息 | 标题+预览+时间+状态 | 匹配 Phase 16 `SessionSummaryDto`：title、lastMessagePreview、lastActivityAt、status/activeRunStatus；足够安全选择。 | ✓ |
| 最近会话卡片信息 | 标题+时间即可 | 极简，但用户可能无法判断点哪个会话。 | |
| 最近会话卡片信息 | 加模型/agent 信息 | 更完整，但 provider/model 稳定性和 per-run pinning 主要在 Phase 20。 | |
| 历史列表数量/分页 | 有限最近列表 | 默认加载最近 N 条，可有轻量“更多”或 cursor hook；不做搜索/管理。符合 Phase 17 范围。 | ✓ |
| 历史列表数量/分页 | 完整分页体验 | 提供完整加载更多/分页状态；更完整但可能拉大 scope。 | |
| 历史列表数量/分页 | 只显示少量固定条 | 实现最小，但后续扩展和真实使用体验较弱。 | |

**User's choice:** `1A, 2A, 3A, 4A`.

**Notes:** Initial question tool returned mismatched option values for this batch, so the choices were re-confirmed as plain text. The confirmed selection was all recommended options.

---

## Active Session Identity

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| 标识位置 | 顶部紧凑 banner/chip | Chat feed 顶部显示“继续：会话标题 / 状态”，不抢输入焦点。 | ✓ |
| 标识位置 | 替换 hero 标题 | 选中历史后 hero 直接变成会话标题。 | |
| 标识位置 | composer 附近提示 | 在输入框上方/旁边提示“正在继续 xxx”。 | |
| 新 vs 继续区分 | 双态标题 | 未选中显示“新对话”；选中显示“继续：{title}”。 | ✓ |
| 新 vs 继续区分 | 仅选中态提示 | 只有选中历史时显示 banner；新对话不特别提示。 | |
| 新 vs 继续区分 | 强状态色区分 | 新/继续分别用不同颜色 chip。 | |
| 新建对话入口 | banner 内提供 | 选中历史后用户可一键回到新对话，避免误续写。 | ✓ |
| 新建对话入口 | 历史列表顶部 | 新建入口放在历史列表顶部。 | |
| 新建对话入口 | 暂不需要 | 只靠选择不同 session 或刷新状态。 | |
| 标题来源 | Phase 16 stable title | 使用 first-user-message-derived stable title；不做 rename/auto-retitle。 | ✓ |
| 标题来源 | latest preview 作为标题 | 用最近预览作为活跃标题。 | |
| 标题来源 | title + last preview | banner 中同时显示标题和最近预览。 | |

**User's choice:** “都用推荐的”.

---

## Transcript Hydration and Bubble Shape

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| 恢复哪些 transcript 角色 | user/assistant 主气泡，tool/error 紧凑 inline 卡片 | 主对话优先，同时不丢 tool/error 状态。 | ✓ |
| 恢复哪些 transcript 角色 | 只渲染 user/assistant | tool/error 留给详情面板。 | |
| 恢复哪些 transcript 角色 | 所有 role 同等气泡 | 所有 role 都渲染成同等聊天气泡。 | |
| 选择器/元数据 | 加入 role/session/run/status hooks | `data-message-role`、`data-session-id`、`data-run-id`、`data-message-status`/`data-stream-state`，视觉上只展示必要状态。 | ✓ |
| 选择器/元数据 | 只加最小 role 选择器 | 更少测试契约，但后续 Phase 18/21 验证力弱。 | |
| 选择器/元数据 | 完整展示所有元数据 | timestamp/runId/status 全量可见，信息噪音更大。 | |
| timestamp/status | 轻量时间/异常状态 | failed/cancelled/partial 明显；completed 弱化；详细 run/tool/provider 折叠。 | ✓ |
| timestamp/status | 只显示文本 | 状态隐藏。 | |
| timestamp/status | 完整时间/status badge | 每条消息完整展示。 | |
| 选择历史 session 后 feed 行为 | 清空→hydrate→banner→滚动到最新 | 后续用户阅读历史时避免自动抢滚动。 | ✓ |
| 选择历史 session 后 feed 行为 | 保留临时 feed 并追加历史 | 容易混淆不同 session。 | |
| 选择历史 session 后 feed 行为 | 恢复后停在顶部 | 便于从头读，但日常继续对话效率低。 | |
| Phase 17 pending/live bubble | 不做真正 pending/live assistant bubble | 发送后沿用现有 run/event/polling；Phase 18 实现 pending/delta/terminal lifecycle。 | ✓ |
| Phase 17 pending/live bubble | 最小 pending 占位 | 做占位但不处理 delta 聚合，可能产生半成品语义。 | |
| Phase 17 pending/live bubble | 完整 streaming lifecycle | 提前实现 Phase 18 范围。 | |

**User's choice:** “都是推荐”.

---

## the agent's Discretion

- Exact styling, icons, chip colors, spacing, labels, breakpoints, and i18n wording are left to downstream planning/implementation.
- Exact recent-list limit and cursor/more UI are discretionary within a bounded recent-list scope.
- Exact scroll anchoring mechanics are discretionary as long as initial restore reaches latest message and later updates do not fight a user reading older history.

## Deferred Ideas

- Phase 17/18 detailed streaming boundary was not separately discussed because the bubble-depth discussion selected the recommended boundary: no full pending/live lifecycle in Phase 17.
- Details folding, scroll/empty-state fine points, and pre-existing test failure scope were not deeply discussed; CONTEXT.md captures conservative defaults inferred from prior phase decisions and the selected recommendations.
