# Phase 16: Conversation Read Model and Recent Sessions - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-28
**Phase:** 16-Conversation Read Model and Recent Sessions
**Areas discussed:** 读模型边界, Transcript 来源, Recent 排序预览, 安全与兼容契约

---

## Gray Areas Selected

| Area | Selected |
|------|----------|
| 读模型边界 | ✓ |
| Transcript 来源 | ✓ |
| Recent 排序预览 | ✓ |
| 安全与兼容契约 | ✓ |

---

## 读模型边界

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 16 的对话读模型 API/DTO 边界怎么命名和组织？ | 新 Conversation 边界 | 新增 client/conversation DTO + ConversationQueryService，语义清晰，后续 Phase 17/18/19 可复用。 | ✓ |
| Phase 16 的对话读模型 API/DTO 边界怎么命名和组织？ | 扩展 Session 边界 | 在现有 SessionQueryService/client/session 下增加 recent/transcript。 | |
| Phase 16 的对话读模型 API/DTO 边界怎么命名和组织？ | 混合命名 | DTO 用 conversation，REST 挂在 /api/sessions。 | |
| Conversation read model 的 REST 路径选择哪个？ | Session 子资源 | GET /api/sessions/recent 和 /api/sessions/{id}/transcript；延续现有 session-centric API。 | ✓ |
| Conversation read model 的 REST 路径选择哪个？ | 顶层 conversation | GET /api/conversations/recent 和 /api/conversations/{id}/transcript。 | |
| Conversation read model 的 REST 路径选择哪个？ | 双路径兼容 | 同时支持 session 子资源和 conversation 别名。 | |
| Console 在 Phase 16 需要接入到什么程度来证明 read model？ | 最小证明 hook | 只让 Console/bridge 能加载 recent summaries 和 typed transcript 用于测试，不做正式布局/polish。 | ✓ |
| Console 在 Phase 16 需要接入到什么程度来证明 read model？ | 完整接入 UI | 本阶段就让 Console 加载并显示历史气泡。 | |
| Console 在 Phase 16 需要接入到什么程度来证明 read model？ | 仅 REST/App 测试 | 完全不碰 Vaadin。 | |

**Notes:** The first REST-path response was ambiguous and was re-asked; final answer was Session 子资源.

---

## Transcript 来源

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Transcript 的权威数据来源优先采用哪种策略？ | 投影表优先 | typed messages/session_entries 作为 read model，run_events 作为回填/审计来源。 | ✓ |
| Transcript 的权威数据来源优先采用哪种策略？ | 事件投影优先 | 从 run_events 组装 transcript。 | |
| Transcript 的权威数据来源优先采用哪种策略？ | 混合渐进 | 先从 events 装配，后续再双写/投影表。 | |
| Transcript 消息角色范围怎么定？ | 四类消息 | user/assistant/tool/error 都进入 typed transcript；tool/error 可摘要化。 | ✓ |
| Transcript 消息角色范围怎么定？ | 仅 user assistant | 简单像普通聊天，但 tool/error 恢复和上下文信息不足。 | |
| Transcript 消息角色范围怎么定？ | 消息诊断分层 | user/assistant 为主，tool/error 作为 linked detail items。 | |
| 取消/失败/部分回复在 transcript 中如何表示？ | 状态字段表达 | ConversationMessageDto 带 status 和 metadata；finish/error 不创建空文本消息。 | ✓ |
| 取消/失败/部分回复在 transcript 中如何表示？ | 独立 error 行 | 失败/取消作为单独 error message。 | |
| 取消/失败/部分回复在 transcript 中如何表示？ | Phase 18 决定 | Phase 16 只存原始足够信息。 | |

**Notes:** Initial multi-question answer repeated “状态字段表达”; source and role questions were re-asked and clarified.

---

## Recent 排序预览

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Recent sessions 的 latest activity / 排序锚点怎么定义？ | 最新对话活动 | 每次 user/assistant/tool/error transcript 消息或 run terminal 更新 session lastActivityAt；按该字段倒序。 | ✓ |
| Recent sessions 的 latest activity / 排序锚点怎么定义？ | Session updated_at | 直接使用 sessions.updated_at。 | |
| Recent sessions 的 latest activity / 排序锚点怎么定义？ | 最近 run updated_at | 按最近 run 的 updated_at/last_event_sequence。 | |
| Session title 如何生成和更新？ | 首条 user 消息 | 默认从首条 user message 截断生成，后续不自动改名。 | ✓ |
| Session title 如何生成和更新？ | 最新 user 消息 | 每轮更新成最新问题。 | |
| Session title 如何生成和更新？ | 运行摘要标题 | 未来可用模型/摘要生成。 | |
| Last-message preview 显示什么内容？ | 最新可见消息 | 最新 user/assistant/error/tool 摘要，按 redaction/visibility 过滤，空则 fallback。 | ✓ |
| Last-message preview 显示什么内容？ | 最新 user 消息 | 列表更像问题清单。 | |
| Last-message preview 显示什么内容？ | 最新 assistant 消息 | 像结果摘要。 | |
| Session status 在 recent summary 里如何折叠？ | 按最近 run 状态 | 显示 idle/running/failed/cancelled/completed 等由最近 run 派生的 conversation status。 | ✓ |
| Session status 在 recent summary 里如何折叠？ | 只显示 session open | 简单但不够可见。 | |
| Session status 在 recent summary 里如何折叠？ | 按严重度优先 | 有 active/failed 时优先显示异常。 | |

---

## 安全与兼容契约

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 16 如何处理现有 raw Map run/session 查询端点？ | 新增 typed 端点并保留旧端点 | Conversation transcript 用 typed DTO；旧 run detail/events Map 端点继续作为 diagnostics。 | |
| Phase 16 如何处理现有 raw Map run/session 查询端点？ | 替换旧端点 | 更彻底清理 raw map，但可能破坏现有 UI/tests/API。 | ✓ |
| Phase 16 如何处理现有 raw Map run/session 查询端点？ | 隐藏旧端点 UI | 后端保留，Console 主聊天禁用 raw map。 | |
| Ownership 过滤要锁定到什么程度？ | 仓储接口显式带上下文 | Conversation/Run projection 查询显式接收 RequestContext+sessionId/runId，并在 SQL 级 tenant/user/session/run 过滤。 | ✓ |
| Ownership 过滤要锁定到什么程度？ | 服务层先校验 | App service 先 find session/run 再查明细。 | |
| Ownership 过滤要锁定到什么程度？ | 混合补强 | service 校验 + 关键 SQL join 过滤。 | |
| Golden tests 如何证明“raw run-event maps 不是 UI transcript contract”？ | DTO schema + assembler tests | transcript response 是 typed DTO，无 payload Map；assembler golden 覆盖 user/delta/finish/tool/error。 | ✓ |
| Golden tests 如何证明“raw run-event maps 不是 UI transcript contract”？ | Console hook tests | Console restore path 调 typed transcript API/bridge，不调用 raw event path。 | |
| Golden tests 如何证明“raw run-event maps 不是 UI transcript contract”？ | 两者都要 | App/client 契约 + 最小 Console hook 都验证。 | |
| Phase 16 是否要处理 SQLite/local profile 持久化？ | 同步实现最小 SQLite | 提前减少 Phase 20 风险，但扩大本阶段范围。 | ✓ |

**Boundary clarification:** User chose “两者都坚持” after concern was raised. Final CONTEXT records a bounded interpretation: replace raw session history/transcript contract, preserve diagnostic run/event endpoints, and allow only minimal SQLite conversation-read-model adapter/contract alignment without Phase 20 local profile/restart productization.

---

## the agent's Discretion

- Exact class names, cursor semantics, redaction helper reuse, and adapter implementation details are left to downstream research/planning.

## Deferred Ideas

- Kimi-style visual restore UX, streaming bubble lifecycle, multi-turn context, provider/model local profile stability, search/rename/archive/branching/export/RAG are deferred to later phases or future requirements.
