# Phase 18: Streaming Bubble Lifecycle - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-29
**Phase:** 18-streaming-bubble-lifecycle
**Areas discussed:** Realtime path, Assistant bubble aggregation, Cancellation and terminal states, Delta coalescing and performance, Verification and Phase 17 carry-over fixes

---

## Area Selection

| Option | Description | Selected |
|--------|-------------|----------|
| 全选（推荐） | Phase 18 风险集中在实时路径、聚合语义、取消深度、性能节流和测试地基；建议一次性锁定。 | ✓ |
| 实时更新路径 | Vaadin Push/SSE 作为产品路径，还是保留 polling fallback；会影响研究重点和计划拆分。 | ✓ |
| 气泡聚合语义 | pending 气泡创建时机、aggregation key、dedupe、tool/status 事件是否进入主气泡。 | ✓ |
| 取消与终态语义 | cancel 后是 UI-only partial，还是必须接到 runtime/provider cancellation；如何展示 failed/cancelled/partial。 | ✓ |
| Delta 节流性能 | 每个 delta 立即更新，还是按时间/字符 coalesce；影响 Vaadin/mobile 性能和测试。 | ✓ |
| 验证与遗留修复 | 是否把 Phase 17 i18n fallback/隐藏面板等阻塞测试地基的问题纳入 Phase 18 前置修复。 | ✓ |

**User's choice:** 全选（推荐） plus all individual areas.
**Notes:** User asked to continue after selecting all areas.

---

## Realtime Path

| Option | Description | Selected |
|--------|-------------|----------|
| Push/SSE 优先（推荐） | 实现 Vaadin Push 或显式 SSE 订阅作为产品路径；保留 polling fallback 并用 data-stream-mode 标明。最符合“真实流式”。 | ✓ |
| 先做 polling fallback | 先用现有 750ms polling + reducer 完成一气泡语义；明确不称为真实 streaming，Push/SSE 后续补。 | |
| 双路径同等交付 | 本阶段同时完整交付 Push/SSE 和 polling fallback；质量最好但范围最大。 | |

**User's choice:** Push/SSE 优先（推荐）.
**Notes:** Product streaming must not be a relabeled polling loop; polling remains fallback/test seam.

---

## Assistant Bubble Aggregation

### Aggregation Key

| Option | Description | Selected |
|--------|-------------|----------|
| session+run+step（推荐） | 以 sessionId + runId + stepId 作为 aggregation key；若后续有 messageId 再兼容。匹配现有 RunEventDto。 | ✓ |
| run-only | 只按 runId 聚合；实现简单，但 session 切换/重放边界较弱。 | |
| 必须 messageId | 要求先引入显式 assistant messageId；语义最强，但可能扩大 Phase 18 数据模型改动。 | |

**User's choice:** session+run+step（推荐）.

### Event Routing

| Option | Description | Selected |
|--------|-------------|----------|
| delta入气泡，其他卡片（推荐） | 只有非空 model.delta text 追加助手气泡；tool/status/approval 继续二级 inline card；terminal 只改气泡状态。 | ✓ |
| 所有事件都进气泡 | 把更多运行事件转成主聊天文本；信息显眼但违背 Kimi-style 降噪。 | |
| terminal也独立卡片 | 完成/失败/取消全部显示为单独卡片；实现清晰但主回答状态不够一体。 | |

**User's choice:** delta入气泡，其他卡片（推荐）.

### Pending Bubble Timing

| Option | Description | Selected |
|--------|-------------|----------|
| run创建成功即出现（推荐） | create/reuse session + create run 成功后立即 beginAssistantMessage；满足 STRM-01，避免未获 runId 时错绑。 | ✓ |
| 发送后立即本地出现 | 用户点发送就显示 pending；反馈最快，但 run 创建失败时要回滚/重标错误。 | |
| 首个delta到达再出现 | 实现简单，但不满足“promptly pending bubble”的产品预期。 | |

**User's choice:** run创建成功即出现（推荐）.

### Dedupe

| Option | Description | Selected |
|--------|-------------|----------|
| sequence/eventId双去重（推荐） | 每 run 维护 lastSequence 和 rendered eventId set；重复 replay/poll/push 不重复追加。 | ✓ |
| 只用sequence | 依赖 sequence 单调递增；简单，但事件 id 异常或乱序诊断能力弱。 | |
| 只用文本去重 | 按 delta 文本避免重复；容易误删重复 token/词。 | |

**User's choice:** sequence/eventId双去重（推荐）.
**Notes:** The aggregation decisions together define the reducer contract downstream agents should plan around.

---

## Cancellation and Terminal States

### Cancellation Depth

| Option | Description | Selected |
|--------|-------------|----------|
| UI+runtime取消（推荐） | 至少接线 run cancellation token / runtime cancel seam；UI 聚合立即停止，持久化 cancelled terminal；provider 真停止若当前 adapter 支持则接上，否则记录限制。 | ✓ |
| 仅UI停止追加 | 点击取消后前端气泡标记 cancelled/partial 并忽略后续 delta；实现最小，但运行/审计可能不够真实。 | |
| 必须provider真取消 | 本阶段必须让底层 provider stream 停止；最严格，但可能放大到 provider SDK 深改。 | |

**User's choice:** UI+runtime取消（推荐）.

### Stop State

| Option | Description | Selected |
|--------|-------------|----------|
| partial + stopped（推荐） | 保留已生成文本，标记 partial/cancelled/stopped；不清空，不再追加后续 delta。 | ✓ |
| cancelled error | 作为错误状态显示；更醒目，但会把用户主动停止误导成失败。 | |
| 静默完成 | 保留文本不显示状态；最干净，但用户不知道已停止且审计语义弱。 | |

**User's choice:** partial + stopped（推荐）.

### Failure State

| Option | Description | Selected |
|--------|-------------|----------|
| 安全错误卡+气泡failed（推荐） | 助手气泡标记 failed；显示安全摘要错误卡/状态，不把原始 provider/tool/audit 详情塞进主回答。 | ✓ |
| 错误文本进气泡 | 把错误消息作为助手回答文本；简单但可能混淆模型输出和系统错误。 | |
| 只在详情面板显示 | 主聊天保持干净，但错误反馈不够明显。 | |

**User's choice:** 安全错误卡+气泡failed（推荐）.
**Notes:** Provider-level abort is best effort if supported; UI/runtime semantics are required.

---

## Delta Coalescing and Performance

### Throttle Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| 时间+字符阈值（推荐） | 例如 50-150ms 或累计若干字符 flush；终态强制 flush。兼顾流式感和移动性能。 | ✓ |
| 每个delta立即更新 | 最真实，但 Vaadin/server/mobile 负载可能偏高。 | |
| 只按轮询批量更新 | 实现简单但流式感较弱，且和 Push/SSE 产品路径冲突。 | |

**User's choice:** 时间+字符阈值（推荐）.

### Performance Test Signal

| Option | Description | Selected |
|--------|-------------|----------|
| 语义+组件数（推荐） | 验证 terminal 前可见增量、最终文本精确、同一气泡、组件数量不随 token 爆炸。 | ✓ |
| 只验证最终文本 | 简单，但会漏掉流式和聚合回归。 | |
| 必须测具体毫秒 | 严格，但 CI/Vaadin timing 容易不稳定。 | |

**User's choice:** 语义+组件数（推荐）.

---

## Verification and Phase 17 Carry-Over Fixes

### Legacy Fix Scope

| Option | Description | Selected |
|--------|-------------|----------|
| 作为前置修复纳入（推荐） | 只修阻塞 streaming 测试地基的问题：status i18n fallback、必要面板/控件可见性；不做广泛清理。 | ✓ |
| 完全不纳入 | Phase 18 只做 streaming；但浏览器/组件测试可能继续被地基问题阻塞。 | |
| 扩大为Console cleanup | 顺手清理所有 Console UI/translation/layout 问题；容易 scope creep。 | |

**User's choice:** 作为前置修复纳入（推荐）.

### Core Verification Coverage

| Option | Description | Selected |
|--------|-------------|----------|
| 慢流+重放+取消+失败（推荐） | fake slow stream：pending、增量合并、replay dedupe、terminal 前可见、cancel 后不追加、failed 安全错误。 | ✓ |
| 只做Vaadin组件测试 | 快且稳定，但产品路径可能没覆盖 Push/SSE 或浏览器行为。 | |
| 只做Playwright产品测试 | 贴近用户，但定位 reducer/状态机问题较慢。 | |

**User's choice:** 慢流+重放+取消+失败（推荐）.

### Stream Mode Hook

| Option | Description | Selected |
|--------|-------------|----------|
| data-stream-mode 明确标记（推荐） | UI 暴露 push/sse/polling-fallback 等稳定 hook；测试可防止 polling 被误认为产品 streaming。 | ✓ |
| 不暴露实现模式 | UI 更干净，但测试难以区分实时路径。 | |
| 只靠日志验证 | 不影响 DOM，但浏览器测试不可直接断言。 | |

**User's choice:** data-stream-mode 明确标记（推荐）.

---

## Context Creation Confirmation

| Option | Description | Selected |
|--------|-------------|----------|
| 创建 context | 写入 18-CONTEXT.md 和 18-DISCUSSION-LOG.md，更新 STATE.md。 | ✓ |
| 继续讨论 | 继续补充其他灰区或修改已选决策。 | |

**User's choice:** 创建 context.

---

## the agent's Discretion

- Exact class names and package locations for reducer/aggregator/bubble APIs.
- Exact Push versus SSE implementation mechanism after research validates Vaadin/session-lock behavior.
- Exact delta flush thresholds, styling, icons, and copy for pending/partial/failed/cancelled states.

## Deferred Ideas

- Phase 19 multi-turn runtime context.
- Phase 20 provider/model/local profile stability.
- Broad Console cleanup beyond streaming test blockers.
- Full provider-level abort where current adapter seams cannot support it safely in Phase 18.
