# Phase 7: MCP Client Bridge and Governed Remote Tools - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-16T07:40:30+00:00
**Phase:** 07-MCP Client Bridge and Governed Remote Tools
**Areas discussed:** Configuration and Governance Boundary, Transport and Authentication Scope, Registration Model and Descriptor Normalization, Security Defaults and Policy Semantics, Health/Discovery/Failure Experience

---

## Areas Selected

| Area | Selected |
|------|----------|
| 配置与治理边界 | ✓ |
| 传输与认证范围 | ✓ |
| 注册模型复用 | ✓ |
| 安全默认与策略 | ✓ |
| 健康与失败体验 | ✓ |

---

## Configuration and Governance Boundary

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 7 是否要让 Admin UI/API 直接新增、编辑、删除 MCP Server 配置？ | 配置文件驱动 | MCP servers 由 Spring/YAML/env 配置；Admin 只读展示状态。 | |
| Phase 7 是否要让 Admin UI/API 直接新增、编辑、删除 MCP Server 配置？ | Admin 可配置 | Admin 提供新增/编辑/删除/禁用 MCP server 的 API 和 Vaadin UI。 | |
| Phase 7 是否要让 Admin UI/API 直接新增、编辑、删除 MCP Server 配置？ | 混合模式 | 配置文件定义 server；Admin 只允许启用/禁用或刷新发现。 | ✓ |
| MCP server 配置应该存在哪里作为 Phase 7 的权威来源？ | Spring properties | 类似 provider 配置，用 typed `@ConfigurationProperties` 绑定。 | |
| MCP server 配置应该存在哪里作为 Phase 7 的权威来源？ | 数据库持久化 | 适合 Admin 动态配置，但需要新增表、迁移、加密/凭证引用治理和 CRUD API。 | |
| MCP server 配置应该存在哪里作为 Phase 7 的权威来源？ | 双源合并 | 配置文件 + DB 合并。 | ✓ |
| Admin Governance 对 MCP 在 Phase 7 的操作能力应到什么程度？ | 只读+刷新 | 展示状态/错误/工具数/健康；最多提供 refresh discovery。 | ✓ |
| Admin Governance 对 MCP 在 Phase 7 的操作能力应到什么程度？ | 可禁用工具 | 允许 Admin 禁用 server/tool。 | |
| Admin Governance 对 MCP 在 Phase 7 的操作能力应到什么程度？ | 完整管理 | 新增/编辑/删除/禁用/刷新全部在 UI 中完成。 | |

**Clarification:** Initial selections created tension: mixed/double-source configuration but read-only+refresh governance. Follow-up asked how far double-source should go.

| Follow-up | Option | Description | Selected |
|-----------|--------|-------------|----------|
| 双源合并要做到什么程度？ | 配置文件为主+DB预留 | Phase 7 实现 Spring properties 权威配置；抽象 repository/DB schema 可预留或轻量实现，但不做 Admin CRUD。 | ✓ |
| 双源合并要做到什么程度？ | 真实双源合并 | Spring properties 和 DB 配置源真实合并，但 Admin 仍不提供 CRUD。 | |
| 双源合并要做到什么程度？ | 完整动态配置 | DB 为权威或并列权威，Phase 7 做后端 CRUD；UI 可暂时只读。 | |

**Captured decision:** Configuration-file-first; DB/repository seam allowed as future-proofing only; Admin is read-only plus refresh discovery.

---

## Transport and Authentication Scope

Before discussion, a librarian research pass checked Spring AI MCP / MCP Java SDK transport and auth maturity. Summary used in the conversation: Streamable HTTP is the recommended modern remote transport, stdio is supported, legacy SSE is compatibility/deprecated, static API key/Bearer/custom header auth is stable, full OAuth DCR/PRM should be deferred, Tools are the only Spring AI-bridged MCP capability needed for this phase.

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Phase 7 v1 的 MCP transport 支持范围怎么锁定？ | HTTP主力+受限stdio | Streamable HTTP 为主；stdio 受限/显式开启；legacy SSE 兼容或延后。 | |
| Phase 7 v1 的 MCP transport 支持范围怎么锁定？ | 仅HTTP | 只支持 Streamable HTTP/stateless HTTP；stdio 延后。 | |
| Phase 7 v1 的 MCP transport 支持范围怎么锁定？ | 全量transport | Streamable HTTP、stdio、legacy SSE 都作为能力。 | ✓ |
| Phase 7 v1 的 MCP auth 支持范围怎么锁定？ | 静态凭证优先 | CredentialRef 注入 API key/Bearer token/custom headers；OAuth DCR/PRM 延后。 | ✓ |
| Phase 7 v1 的 MCP auth 支持范围怎么锁定？ | 加client_credentials | 静态凭证 + 简单 OAuth client_credentials token 获取。 | |
| Phase 7 v1 的 MCP auth 支持范围怎么锁定？ | 完整OAuth | OAuth 2.1、PRM、DCR、scope step-up。 | |
| MCP Tools 以外的 MCP 能力是否纳入 Phase 7？ | 只做Tools | 只做 remote tools discovery/execution/governance；resources/prompts 等延后。 | ✓ |
| MCP Tools 以外的 MCP 能力是否纳入 Phase 7？ | Tools+Resources只读 | 额外把 resources 作为只读外部资源/工具暴露。 | |
| MCP Tools 以外的 MCP 能力是否纳入 Phase 7？ | 全协议覆盖 | 覆盖 tools/resources/prompts/sampling/elicitation。 | |

**Captured decision:** Support Streamable HTTP, stdio, and legacy SSE compatibility in v1; use static CredentialRef auth; Tools only.

---

## Registration Model and Descriptor Normalization

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| MCP server/tool 在内部治理模型上应如何注册？ | 独立MCP注册表 | 新增 McpServerRegistry/McpToolRegistry；治理读模型复用 Extension source/capability/health 语言，但不把远程 MCP server 伪装成 Java ExtensionSource。 | ✓ |
| MCP server/tool 在内部治理模型上应如何注册？ | 复用ExtensionSource | 每个 MCP server 表示为 ExtensionSource，tools 表示为 ToolExtensionCapability。 | |
| MCP server/tool 在内部治理模型上应如何注册？ | 双层适配 | 独立 registry，同时产出 Extension-like governance projection。 | |
| MCP tool 的全局 toolId 命名规则怎么定？ | serverId.toolName | 全局 ID 使用 `mcp.<serverId>.<toolName>` 或 `<serverId>.<toolName>`。 | ✓ |
| MCP tool 的全局 toolId 命名规则怎么定？ | SDK前缀风格 | 使用类似 Spring AI `clientName__toolName`。 | |
| MCP tool 的全局 toolId 命名规则怎么定？ | 保留原名冲突失败 | 保留 MCP 原 tool name；冲突时不注册。 | |
| MCP tool schema 如何映射到 Pi ToolDescriptor？ | JSON Schema透传 | MCP inputSchema 以 `dialect=json-schema` 透传到 ToolSchema.document。 | ✓ |
| MCP tool schema 如何映射到 Pi ToolDescriptor？ | 规范化子集 | 只接受平台支持的 JSON Schema 子集。 | |
| MCP tool schema 如何映射到 Pi ToolDescriptor？ | 自定义转换 | 转换为 Pi 自有 schema 模型。 | |

**Captured decision:** Dedicated MCP registry/adapter; governance language consistent with extensions; server-qualified tool IDs; JSON Schema passthrough.

---

## Security Defaults and Policy Semantics

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| MCP server allowlist/网络控制默认策略怎么定？ | 默认拒绝外网 | 未显式 allowlist 的 host/scheme/command 一律不可连接。 | |
| MCP server allowlist/网络控制默认策略怎么定？ | 配置即信任 | 只要 server 出现在配置里就信任连接。 | ✓ |
| MCP server allowlist/网络控制默认策略怎么定？ | 按环境分级 | dev/test 宽松，prod 严格。 | |
| MCP tool 的默认风险和审批策略怎么定？ | 保守审批 | 远程 MCP 默认 remote/external；readOnlyHint=true 可允许直接执行，其它默认 preview/approval 或按 policy scope。 | ✓ |
| MCP tool 的默认风险和审批策略怎么定？ | 全部需审批 | 所有 MCP tool 默认 REQUIRE_APPROVAL。 | |
| MCP tool 的默认风险和审批策略怎么定？ | 按MCP注解 | 主要信任 MCP annotations 决定风险。 | |
| MCP tool 是否必须显式加入 AgentDefinition.allowedToolScopes/allowlist 才可执行？ | 必须显式允许 | 发现/展示不等于可执行；Agent 必须允许 server/tool scope。 | ✓ |
| MCP tool 是否必须显式加入 AgentDefinition.allowedToolScopes/allowlist 才可执行？ | 可信server默认可用 | server 配置为 trusted 后所有 tools 默认可用。 | |
| MCP tool 是否必须显式加入 AgentDefinition.allowedToolScopes/allowlist 才可执行？ | 按server级scope | 只需允许 server scope，即可执行该 server 下所有 tools。 | |

**Clarification:** Initial selections meant connection-layer trust but execution-layer conservatism. Follow-up confirmed minimum safety checks.

| Follow-up | Option | Description | Selected |
|-----------|--------|-------------|----------|
| “配置即信任”是否保留最低限度连接安全校验？ | 保留最低校验 | 已配置 server 视为可信，但仍校验 scheme/command 来自配置、禁止危险 URL scheme、凭证不外泄、错误脱敏。 | ✓ |
| “配置即信任”是否保留最低限度连接安全校验？ | 完全信任配置 | 只要写入配置就尝试连接，不额外做 URL/命令限制。 | |
| “配置即信任”是否保留最低限度连接安全校验？ | 生产严格开关 | 默认配置即信任，但 production strict 开关启用更强 SSRF/allowlist 校验。 | |

**Captured decision:** Configured server trusted for connection, with minimum safety checks; explicit Agent allowlist remains mandatory; conservative policy for remote tools.

---

## Health, Discovery, and Failure Experience

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| MCP server 健康检查与工具发现应该何时发生？ | 启动+手动刷新 | 应用启动时连接/发现；Admin 可触发 refresh discovery；不做复杂定时轮询。 | ✓ |
| MCP server 健康检查与工具发现应该何时发生？ | 定时轮询 | 启动发现 + 后台周期健康检查/rediscovery。 | |
| MCP server 健康检查与工具发现应该何时发生？ | 懒加载 | 首次查询/首次执行时连接发现。 | |
| MCP server DOWN 或 discovery 失败时，其 tools 在 ToolRegistry 中如何表现？ | 保留但不可用 | 保留已知/发现失败状态；ToolRegistry 可列出但 resolve/execute 返回 unavailable/failed。 | ✓ |
| MCP server DOWN 或 discovery 失败时，其 tools 在 ToolRegistry 中如何表现？ | 隐藏工具 | server 不健康时不注册/不展示 tools。 | |
| MCP server DOWN 或 discovery 失败时，其 tools 在 ToolRegistry 中如何表现？ | 允许尝试执行 | 即使 DOWN 也允许调用时重试。 | |
| MCP 调用失败、认证失败、超时的用户/审计表现怎么定？ | 标准tool失败 | 统一映射为 tool.failed/tool.denied 或 policy/server unavailable 类错误；事件/审计只含 redacted summary 和 refs。 | ✓ |
| MCP 调用失败、认证失败、超时的用户/审计表现怎么定？ | 独立mcp事件 | 新增 mcp.connection/mcp.discovery/mcp.invocation 事件家族。 | |
| MCP 调用失败、认证失败、超时的用户/审计表现怎么定？ | 仅Admin状态 | 运行时只显示普通工具失败，详细 MCP 错误只在 Admin 展示。 | |

**Captured decision:** Startup discovery + manual refresh; unhealthy tools retained but unavailable; standard tool lifecycle failure semantics.

---

## the agent's Discretion

- Exact MCP module and class names.
- Whether Spring AI MCP or direct MCP Java SDK becomes the primary implementation detail after planning research confirms version compatibility.
- Exact DTO/endpoint names for MCP status and refresh.
- Exact timeout/retry/circuit breaker defaults.

## Deferred Ideas

- Admin CRUD for MCP server config.
- Full OAuth DCR/PRM and advanced token lifecycle.
- MCP resources/prompts/sampling/elicitation and Pi-as-MCP-server.
- Periodic health polling and advanced observability dashboards.
- Dynamic plugins and production sandboxing.
