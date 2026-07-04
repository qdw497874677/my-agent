# Phase 20: Provider/Model and Local Profile Stability - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-04
**Phase:** 20-provider-model-and-local-profile-stability
**Areas discussed:** Console Provider/Model Feedback, Model Selection and Run Metadata, Local Fallback and No-Key Semantics, SQLite Local Profile Persistence

---

## Console Provider/Model Feedback

| Option | Description | Selected |
|--------|-------------|----------|
| 紧凑状态条 | 在现有 model bar 内显示 provider/model/fallback 状态、刷新结果和可操作错误；保持 Kimi-style 主聊天不被打扰。 | ✓ |
| 显眼告警卡 | 在聊天上方/模型区显示较强告警卡；错误更醒目，但会增加主界面噪音。 | |
| 详情面板为主 | 模型区只给简单状态，具体错误进 Run/Details；界面安静但不够 actionable。 | |
| 你来决定 | 下游按现有 Vaadin 风格选择具体呈现方式。 | |

**User's choice:** 紧凑状态条
**Notes:** Keep the Kimi-style Console clean while making provider/model readiness actionable.

| Option | Description | Selected |
|--------|-------------|----------|
| 三态明确反馈 | refresh 后分别显示成功数量、空列表说明、红acted 错误摘要；不 silent catch。 | ✓ |
| 只显示异常 | 成功时安静，只在 empty/error 时提示；噪音少但成功反馈弱。 | |
| toast通知 | 用临时通知显示刷新结果；实现轻但测试/可发现性弱。 | |
| 你来决定 | 只锁定“不静默失败”，具体 UI 由 planner 决定。 | |

**User's choice:** 三态明确反馈
**Notes:** Success, empty, and error states all need visible feedback.

---

## Model Selection and Run Metadata

| Option | Description | Selected |
|--------|-------------|----------|
| 仅后续运行 | 立即持久化选择，但 active run 不变；下一次 create run 使用新 modelRef，并给出“applies next run”提示。 | ✓ |
| 立即影响运行 | 切换后尝试影响当前 active run；语义复杂，可能破坏流式/调试一致性。 | |
| 需点击保存 | 选择后不立即保存，必须显式保存；更安全但本地对话体验更重。 | |
| 你来决定 | 只锁定 PROV-03，具体交互由 planner 决定。 | |

**User's choice:** 仅后续运行
**Notes:** Model selector persists immediately but must not mutate active runs.

| Option | Description | Selected |
|--------|-------------|----------|
| 实际解析结果 | 记录 requested modelRef、resolved providerId/modelId、fallbackMode、readiness/error摘要；用于历史和调试。 | ✓ |
| 只记modelRef | 只记录最终 modelRef；轻量但无法解释 fallback/配置问题。 | |
| 完整诊断 | 记录更完整 provider config 快照；调试强但有泄密/过度耦合风险。 | |
| 你来决定 | 下游决定元数据字段。 | |

**User's choice:** 实际解析结果
**Notes:** Persist enough safe facts to explain which provider/model/fallback path a run used.

---

## Local Fallback and No-Key Semantics

| Option | Description | Selected |
|--------|-------------|----------|
| 允许但强标识 | 允许本地 fallback 继续跑 demo/dev 闭环，但回答和模型区都明确标为 Local fallback / not real model。 | |
| 默认阻止发送 | 无真实 provider 时禁用发送，要求配置 key；更真实但破坏 no-key 本地体验和既有 fake runtime 验证。 | ✓ |
| 需要确认一次 | 首次 fallback 弹出确认；安全但增加流程复杂度。 | |
| 你来决定 | 只锁定不能伪装为真实模型。 | |

**User's choice:** 默认阻止发送
**Notes:** No provider/key should not silently produce fake/local answers in the product Console.

| Option | Description | Selected |
|--------|-------------|----------|
| 模型区+气泡/元数据 | 模型区显示 fallback readiness；assistant bubble 或 secondary metadata 标识该回复来自 local fallback。 | ✓ |
| 只在模型区 | 模型区一直显示 fallback，聊天气泡不重复；更简洁但历史回看时可能不明显。 | |
| 只在详情里 | 主界面最安静，但不满足“clearly labeled”的产品语义风险较高。 | |
| 你来决定 | 下游选择具体位置。 | |

**User's choice:** 模型区+气泡/元数据
**Notes:** If fallback is explicitly enabled/used, labels must be visible both in current UI and restored history metadata.

---

## SQLite Local Profile Persistence

| Option | Description | Selected |
|--------|-------------|----------|
| 加固现有本地Profile | 在现有 local profile/SQLite 实现上补齐 schema、restart、ownership、provider config；避免本阶段抽象过大。 | ✓ |
| 抽正式Profile端口 | 新增 App 层 LocalProfile port/service；边界更干净，但 Phase 20 规划和迁移成本更高。 | |
| 只修Provider配置 | 最小化，只保证 provider config 持久化；但不能满足 SESS-05/PROV-06。 | |
| 你来决定 | 下游基于代码决定抽象程度。 | |

**User's choice:** 加固现有本地Profile
**Notes:** Continue from existing SQLite/local dev implementation unless research finds a blocker.

| Option | Description | Selected |
|--------|-------------|----------|
| 端到端恢复闭环 | 同一 SQLite DB 重建 app/store 后恢复 sessions、transcript、run metadata、provider config、selected model。 | ✓ |
| 仓储级验证 | 只验证 SQLite repository/store 重建后数据存在；快但产品路径信心弱。 | |
| 浏览器级验证 | 要求 Playwright 真重启服务器验证；最接近产品但成本高，可能更适合 Phase 21 扩大矩阵。 | |
| 你来决定 | 下游决定验证深度。 | |

**User's choice:** 端到端恢复闭环
**Notes:** Restart proof should recreate local persistence/store/config objects against the same SQLite DB and validate recovery.

---

## the agent's Discretion

- Exact UI copy, styling, selectors beyond the required stable hooks, DTO/class names, and run metadata column names are left to downstream research/planning.

## Deferred Ideas

- Automatic paid-provider fallback policy and multi-provider routing.
- Provider-specific context-window budgeting.
- Full Phase 21 release/security/regression matrix beyond targeted Phase 20 proof.
- Broad App-level LocalProfile abstraction unless needed by research.
