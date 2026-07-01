# Phase 19: Multi-Turn Runtime Context - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-01
**Phase:** 19-multi-turn-runtime-context
**Areas discussed:** Context injection seam, budget and truncation policy, role filtering and redaction, model message formatting, verification and safety gates

---

## Area Selection

| Option | Description | Selected |
|--------|-------------|----------|
| 全部讨论（推荐） | 依次锁定上下文来源/注入 seam、预算截断、脱敏过滤、模型消息格式与验证边界。 | ✓ |
| 上下文注入 seam | 历史 turns 放进 `SessionContext.messages`、扩展 `ModelRequest`，还是在 runtime/model client 边界转换。 | |
| 预算与截断策略 | 按最近 N 轮、字符预算、token 近似，如何记录 truncated metadata。 | |
| 脱敏与角色过滤 | 只给模型 user/assistant，还是包含工具摘要；如何排除敏感数据。 | |
| 验证与测试证据 | fake model 如何捕获请求，如何证明当前 prompt 只出现一次。 | |

**User's choice:** 全部讨论（推荐）

---

## Context Injection Seam

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| 历史 turns 的主载体优先放在哪里？ | 填充 SessionContext（推荐） | 复用已有 `SessionContext.messages` / `MessageEntry`，调度时注入。 | ✓ |
| 历史 turns 的主载体优先放在哪里？ | 扩展 ModelRequest | 显式在模型请求上带 history/messages。 | |
| 历史 turns 的主载体优先放在哪里？ | Runtime 内部加载 | 让 runtime 自己查 transcript。 | |
| 上下文组装逻辑应属于哪一层？ | App 层组装器（推荐） | 类似 `ConversationTranscriptAssembler` 的纯应用服务/函数。 | ✓ |
| 上下文组装逻辑应属于哪一层？ | Domain 策略对象 | 预算/过滤纯规则可下沉 Domain，但加载仍需 App。 | |
| 上下文组装逻辑应属于哪一层？ | Infrastructure 组装 | 调度器直接查库并组装。 | |
| 生产链路的最小改造路径应如何定位？ | 调度时填 SessionContext（推荐） | `DefaultRunDispatcher` 构建 RunContext 前调用 App context assembler。 | ✓ |
| 生产链路的最小改造路径应如何定位？ | 模型 client 读取 transcript | OpenAI adapter 自己按 sessionId 查历史。 | |
| 生产链路的最小改造路径应如何定位？ | 先只做 testkit runtime | 只证明 fake runtime 可用。 | |
| 当前 prompt 与历史 prompt 的去重原则是什么？ | 当前 prompt 单独追加一次（推荐） | 历史上下文只包含当前 run 之前的 turns；当前用户输入只来自 RunInput。 | ✓ |
| 当前 prompt 与历史 prompt 的去重原则是什么？ | Transcript 全量含当前 run | 从 transcript 读全量再去重。 | |
| 当前 prompt 与历史 prompt 的去重原则是什么？ | 由模型 adapter 去重 | 把重复风险推到 provider 边界。 | |

**User's choice:** All recommended options.

---

## Budget and Truncation Policy

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 19 默认上下文预算优先按什么限制？ | 最近轮数 + 字符预算（推荐） | 默认取最近 N 个 user/assistant turns，再用字符预算兜底。 | ✓ |
| Phase 19 默认上下文预算优先按什么限制？ | 只按最近轮数 | 最简单，但长消息可能撑爆 provider context。 | |
| Phase 19 默认上下文预算优先按什么限制？ | token 估算优先 | 更接近模型窗口，但需要估算规则。 | |
| 上下文截断方向怎么处理？ | 保留最新历史（推荐） | 超预算时丢更早内容，符合对话连续性。 | ✓ |
| 上下文截断方向怎么处理？ | 保留最早历史 | 保留开头背景，但容易丢掉最近上下文。 | |
| 上下文截断方向怎么处理？ | 首尾混合 | 保留第一轮加最近几轮；更复杂。 | |
| truncation metadata 需要记录在哪里？ | Run/Context metadata（推荐） | 记录 included/dropped counts、预算和 truncated 状态。 | ✓ |
| truncation metadata 需要记录在哪里？ | 只写日志 | 下游 UI/测试/审计难以断言。 | |
| truncation metadata 需要记录在哪里？ | 直接显示在 UI | Phase 19 UI hint 是 no。 | |
| 预算配置粒度怎么定？ | 平台默认 + 可配置（推荐） | 保守默认值，可配置最近轮数/字符预算。 | ✓ |
| 预算配置粒度怎么定？ | 硬编码默认 | 不满足 configurable budget。 | |
| 预算配置粒度怎么定？ | 按 provider model 动态窗口 | 依赖 Phase 20 provider/model 稳定性。 | |

**User's choice:** All recommended options.

---

## Role Filtering and Redaction

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| 哪些 transcript 角色允许进入模型上下文？ | 仅 user/assistant（推荐） | Phase 19 聚焦对话轮次；tool/error/audit/provider 不进 prompt。 | ✓ |
| 哪些 transcript 角色允许进入模型上下文？ | user/assistant + 工具摘要 | 保留工具结果语义，但范围更大。 | |
| 哪些 transcript 角色允许进入模型上下文？ | 全部可见消息 | tool/error/provider 诊断进入模型风险高。 | |
| assistant 历史状态如何过滤？ | completed + partial safe（推荐） | 完成可进；取消/失败仅安全 partial 文本可进。 | ✓ |
| assistant 历史状态如何过滤？ | 只 completed | 最安全，但会丢失停止后的有用 partial 回答。 | |
| assistant 历史状态如何过滤？ | 所有 assistant 文本 | 可能污染上下文。 | |
| 脱敏策略应如何复用/收敛？ | App 级 ContextRedactor（推荐） | App 层模型上下文专用 redactor，不依赖 adapter-web。 | ✓ |
| 脱敏策略应如何复用/收敛？ | 复用 adapter-web redactor | 层级错误。 | |
| 脱敏策略应如何复用/收敛？ | 只信任 transcript redacted 标志 | 发送给模型前仍需最后一道过滤。 | |
| 发现敏感或不可见消息时怎么处理？ | 排除并计数（推荐） | 不进入上下文，metadata 记录 excluded/redacted counts。 | ✓ |
| 发现敏感或不可见消息时怎么处理？ | 替换为占位符 | 可能污染上下文。 | |
| 发现敏感或不可见消息时怎么处理？ | 抛错阻断 run | 普通历史诊断/工具项会导致对话不可用。 | |

**User's choice:** All recommended options.

---

## Model Message Formatting

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Provider adapter 最终应该如何发送多轮消息？ | 消息列表（推荐） | `SessionContext.messages` + 当前 RunInput 转成 ordered chat messages。 | ✓ |
| Provider adapter 最终应该如何发送多轮消息？ | 拼接成单字符串 | provider-neutral role 语义弱。 | |
| Provider adapter 最终应该如何发送多轮消息？ | 只改内部 fake，不改 provider | 不满足真实多轮模型上下文。 | |
| 历史消息与当前消息的顺序？ | 历史按时间升序 + 当前 user 最后（推荐） | 符合 chat completion 语义。 | ✓ |
| 历史消息与当前消息的顺序？ | 当前 user 先放 | 不符合常见模型消息语义。 | |
| 历史消息与当前消息的顺序？ | 历史倒序 | 不符合对话自然顺序。 | |
| OpenAI-compatible adapter 接口改造边界？ | 内部 stream 接口改为 messages（推荐） | `OpenAiStreamSource` 不再只接收 String prompt。 | ✓ |
| OpenAI-compatible adapter 接口改造边界？ | 保留 stream(String) 并在上层拼接 | role 语义消失。 | |
| OpenAI-compatible adapter 接口改造边界？ | 新增 overload 兼容迁移 | 可迁移但需避免双路径漂移。 | |
| System/developer 指令是否进入 Phase 19 范围？ | 不新增系统消息（推荐） | 只处理当前 session 的 prior turns。 | ✓ |
| System/developer 指令是否进入 Phase 19 范围？ | 加入简单系统消息 | 扩大 prompt policy 范围。 | |
| System/developer 指令是否进入 Phase 19 范围？ | 设计完整 prompt stack | 超出 Phase 19。 | |

**User's choice:** All recommended options.

---

## Verification and Safety Gates

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| CTX-04 的核心测试证据应是什么？ | 捕获 ModelRequest/messages（推荐） | 增强 fake streaming model/testkit 捕获请求，断言 prior turns 和当前 prompt。 | ✓ |
| CTX-04 的核心测试证据应是什么？ | 只看模型回复文本 | 间接且容易假阳性。 | |
| CTX-04 的核心测试证据应是什么？ | 只做 UI E2E | UI 继续 session 不代表 runtime 收到上下文。 | |
| 所有权/隔离测试怎么覆盖？ | App/repository 层上下文查询测试（推荐） | 证明只加载同 tenant/user/session 的历史。 | ✓ |
| 所有权/隔离测试怎么覆盖？ | 只依赖 Phase 16 测试 | Phase 19 还需证明 context assembly 使用同样边界。 | |
| 所有权/隔离测试怎么覆盖？ | 放到 Phase 21 | Phase 19 至少需要本功能安全门槛。 | |
| 敏感数据测试最小集合？ | 角色+敏感键+不可见过滤（推荐） | 断言 ineligible content 不进模型上下文，并记录排除计数。 | ✓ |
| 敏感数据测试最小集合？ | 只测 token/password | 太窄。 | |
| 敏感数据测试最小集合？ | 只人工 review | 不满足 CTX-03。 | |
| 架构边界测试要锁什么？ | App 组装器无外层依赖（推荐） | context assembler/redactor/policy 纳入 App ArchUnit 规则。 | ✓ |
| 架构边界测试要锁什么？ | 只跑现有 ArchUnit | 可能无法防止新组装器误放。 | |
| 架构边界测试要锁什么？ | 不加架构测试 | 不满足 CTX-05。 | |

**User's choice:** All recommended options.

---

## the agent's Discretion

- Exact class names and package names are planner discretion.
- Exact default recent-turn/character budget values are planner/research discretion.
- Exact truncation metadata storage surface is planner discretion, provided it is observable and testable.

## Deferred Ideas

- Long-term memory, RAG/vector search, summarization/compaction controls, and advanced context UI.
- Provider/model-specific context-window budgeting and provider metadata persistence.
- Full milestone-wide regression/security matrix.
- Conversation search, rename/archive/pin/delete, branching/editing/regeneration, import/export, prompt templates, and automatic provider fallback.
