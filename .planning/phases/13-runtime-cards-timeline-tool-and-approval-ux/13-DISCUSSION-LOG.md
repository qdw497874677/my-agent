# Phase 13: Runtime Cards, Timeline, Tool, and Approval UX - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-24
**Phase:** 13-Runtime Cards, Timeline, Tool, and Approval UX
**Areas discussed:** Timeline 形态, Tool 卡片结构, Approval 风险UX, Dense Details 安全, Dialog/反馈边界, 测试验收范围

---

## Timeline 形态

| Option | Description | Selected |
|--------|-------------|----------|
| 增强现有Feed | 在 Chat/Event Feed 内把 runtime events 渲染成 compact timeline cards/accordions，延续 Phase 12 chat-first。 | ✓ |
| 独立Timeline面板 | 新增 RunTimelinePanel 或独立区域，和 Chat narrative 分开；更清晰但会冲击 Phase 12 信息架构。 | |
| Feed内可切换视图 | Chat/Event Feed 内提供 Chat/Timestamp/Tool 等视图切换；更强但接近新增功能复杂度。 | |

**User's choice:** 增强现有Feed
**Notes:** Timeline 应保持在既有 Chat/Event Feed 中，不引入新路线或竞争面板。

### Timeline 卡片密度

| Option | Description | Selected |
|--------|-------------|----------|
| 状态+时间+摘要 | 默认显示 status、timestamp/type、short summary；Details 展开更多。 | ✓ |
| 极简摘要优先 | 默认只显示类型/状态/摘要，时间和技术细节放展开区；视觉更轻但排障信息少。 | |
| 排障信息优先 | 默认显示更多 IDs、schema、duration 等；利于调试但手机上更密。 | |

**User's choice:** 状态+时间+摘要
**Notes:** 默认卡片需要足够排障上下文，但密集字段放入展开区。

---

## Tool 卡片结构

### Tool 展示粒度

| Option | Description | Selected |
|--------|-------------|----------|
| 按事件一张卡 | 沿用现有 RunEventRenderer/appendEvent/dedupe 模型；started/completed/error 各自是事件卡。 | ✓ |
| 按toolCall聚合 | 同一个 tool call 聚合为一张进度卡；体验更强但要新增聚合状态/更新模型。 | |
| 混合策略 | 重要工具聚合，普通事件一张卡；灵活但规则复杂，测试成本更高。 | |

**User's choice:** 按事件一张卡
**Notes:** 保持现有事件追加模型，不做 tool-call lifecycle 聚合。

### Tool 默认字段

| Option | Description | Selected |
|--------|-------------|----------|
| 关键摘要默认 | tool name/source/status/policy-approval/duration/error 默认，redacted input/output 放展开。 | ✓ |
| 全部摘要默认 | 把 redacted input/output summary 也默认显示；可见性强但卡片更长。 | |
| 最小默认 | 默认只显示 tool/status，其他全部展开；紧凑但不利于手机快速判断风险/失败原因。 | |

**User's choice:** 关键摘要默认
**Notes:** 输入/输出摘要可展开，默认展示关键风险和状态判断字段。

---

## Approval 风险UX

### Approval 交互形态

| Option | Description | Selected |
|--------|-------------|----------|
| Inline风险卡 | risk-first ApprovalCard + Details 展开 + 明确 Approve/Reject；不引入新 Dialog 风险。 | ✓ |
| 卡片+确认Dialog | 卡片展示风险，点击 approve/reject 后再弹 ConfirmDialog；更强防误触但 greenfield 焦点/溢出工作更大。 | |
| 审批专用面板 | 把审批从 feed 中抽到专用 pending approval 面板；清晰但可能偏离 chat-first timeline。 | |

**User's choice:** Inline风险卡
**Notes:** 审批保持 inline，风险优先呈现，不新增专用工作流。

### Intentional action

| Option | Description | Selected |
|--------|-------------|----------|
| 主次按钮+风险确认文案 | Reject/Approve 都是 44px，Approve 附近展示风险/后果/side-effect；不二次弹窗。 | ✓ |
| Approve需长按/二步 | Approve 需要二次点击或长按；更谨慎但移动可用性和测试复杂。 | |
| 仅高风险二次确认 | 低风险 inline，high/critical 风险再要求确认；平衡但需要风险等级规则。 | |

**User's choice:** 主次按钮+风险确认文案
**Notes:** intentional action 通过文案、风险上下文、按钮层级和触控尺寸实现，不通过二次确认弹窗实现。

---

## Dense Details 安全

### Detail 层级

| Option | Description | Selected |
|--------|-------------|----------|
| 只显示脱敏摘要 | 默认和展开区都只显示 redacted summaries/key-value；不展示 raw payload。 | |
| 允许raw但脱敏 | 展示格式化 raw-like JSON，但先脱敏；排障强但敏感泄漏/横向溢出风险更高。 | |
| 分层详情 | 默认脱敏摘要，另有“高级详情”仍脱敏；更细但范围更大。 | ✓ |

**User's choice:** 分层详情
**Notes:** 用户希望既有简洁 summary，又有更深的高级排障层。

### Raw payload 边界

| Option | Description | Selected |
|--------|-------------|----------|
| 不展示raw | 高级详情仍是结构化脱敏字段/摘要，不提供原始 JSON payload。 | |
| 脱敏raw可展示 | 允许展示脱敏后的 pretty JSON；更便于调试但要强化 wrapping/size/redaction。 | ✓ |
| 仅测试/开发可raw | 生产隐藏 raw，dev/test profile 可看脱敏 raw；边界更复杂。 | |

**User's choice:** 脱敏raw可展示
**Notes:** 可以展示 redacted pretty/raw-like JSON，但必须先脱敏并确保 wrapping/size/no-overflow。

---

## Dialog/反馈边界

| Option | Description | Selected |
|--------|-------------|----------|
| 暂不引入 | 继续 inline Details/status feedback；满足 MCARD-05 用 CSS/现有 viewport-fitting 容器约束表达。 | ✓ |
| 只引入ConfirmDialog | 仅用于审批确认；需要新增移动 fit、focus、close/action 控制和测试。 | |
| 引入轻量Notification | 用于 approve/reject/cancel 等结果反馈；需要定义 viewport fit 和稳定 selector。 | |

**User's choice:** 暂不引入
**Notes:** Phase 13 不新增 Dialog/ConfirmDialog/Notification，优先 inline 卡片、Details 和 status feedback。

---

## 测试验收范围

### E2E 深度

| Option | Description | Selected |
|--------|-------------|----------|
| 关键路径+卡片内断言 | fake tool/approval run，断言 timeline/tool/approval card interiors、Details 展开、approve/reject、no-overflow/tap/focus。 | |
| 仅卡片可见+无溢出 | 更快更稳，但可能放过字段缺失/风险文案缺失。 | |
| 全事件矩阵 | 覆盖所有 runtime event 类型和所有状态；质量高但 phase 过重。 | ✓ |

**User's choice:** 全事件矩阵
**Notes:** 用户希望测试覆盖更全面。

### 全事件矩阵边界

| Option | Description | Selected |
|--------|-------------|----------|
| Console代表矩阵 | 覆盖 Console runtime card 代表类别：status/model/tool/approval/policy/terminal/error/dense details。 | ✓ |
| 所有DTO类型逐项 | 每个已知 event type/payloadSchema 都要浏览器断言；最严格但可能过重/脆弱。 | |
| 浏览器代表+Java全量 | 浏览器覆盖代表矩阵，Java contract 覆盖更多 RenderEventRenderer/Tool/Approval 分支。 | |

**User's choice:** Console代表矩阵
**Notes:** 最终将“全事件矩阵”限定为 Console runtime card 代表类别矩阵；Java contracts 可覆盖额外分支。

---

## the agent's Discretion

- Exact visual styling, badge/chip names, timeline connector treatment, colors, typography, spacing, and breakpoint polish.
- Exact Java helper/class extraction for shared card primitives and redaction utilities.
- Exact representative event fixture content, as long as browser and Java verification cover the agreed categories and branches.

## Deferred Ideas

- Standalone Run Timeline route/panel separate from the Chat/Event Feed.
- Tool-call lifecycle aggregation into a single mutable/progress card.
- New modal confirmation/dialog/notification framework for approvals and runtime feedback.
- Admin Governance full-site mobile card/detail conversion beyond shared approval card behavior.
