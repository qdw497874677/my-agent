# Pi Java Agent Platform

## What This Is

Pi Java Agent Platform 是一个基于 Java 的通用 Agent 基座，用于支撑云上 Agent 服务、Agent Web Console、Admin Governance、未来 TUI/CLI 客户端和插件生态。它借鉴 earendil-works/pi 的 Agent Loop、Provider Registry、Tool、Session、Extension、Skills 等思想，但面向云端、多租户、可集成、可扩展的产品平台重新设计。

第一阶段的首要交付形态是 Cloud Server + Agent Web Console：一个可部署的 Java 云上 Agent 服务，提供 Runtime、扩展集成、Agent Catalog、Chat/Run 等 Agent 入口、Run/Session 管理、API/SSE、工具执行过程展示和基础 Admin Governance。核心实现尽量采用 Java 全栈，避免把 TypeScript pi 的 CLI/TUI 结构直接搬到核心。

## Core Value

让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] 构建 Java Agent Runtime 内核，支持 Agent Loop、事件流、模型调用、工具调用和会话状态管理。
- [ ] 保持 Runtime 足够通用，不绑定 Chat、Coding、单模型、单工具协议或单 UI，支持多种 Agent 类型和交互模式。
- [ ] 交付 Cloud Server，暴露 REST/SSE API，可创建、运行、取消、查询 Agent Run。
- [ ] 提供扩展集成体系，v1 覆盖 Java SPI、Spring Bean 注册、动态插件 Jar 和远程 MCP 工具接入。
- [ ] 提供 Model Provider 抽象，优先支持 OpenAI-compatible Provider，并保留 Anthropic/Gemini/其他厂商扩展边界。
- [ ] 提供 Tool Registry、Tool Executor、Tool Policy、Tool Audit，确保工具调用可扩展、可治理、可审计。
- [ ] 提供 Session/Run/Step 状态模型，支持执行历史、事件记录、工具调用记录和后续持久化。
- [ ] 提供 Agent Web Console + Admin Governance，用于发现 Agent、进入 Chat、发起 Run、查看工具执行过程、处理审批、继续会话，并治理模型、工具、插件、MCP、审计和运行状态。
- [ ] 为未来 TUI/CLI 客户端保留协议和事件接口，但不把 TUI 作为 v1 核心交付。

### Out of Scope

- 完整复刻 earendil-works/pi 的 TypeScript monorepo — 该项目以 Java 云端平台为目标，pi 只作为参考设计。
- v1 复刻完整 TUI/CLI 体验 — 首要交付是 Cloud Server、Agent Web Console 和 Admin Governance，TUI/CLI 后置。
- v1 构建复杂 Agent Studio / 可视化编排器 — 先做 Agent Catalog + Chat 入口 + 运行过程展示，复杂构建器后置。
- v1 构建完整插件市场 — 先定义插件协议和加载机制，市场、分发、评分、计费后置。
- v1 支持所有模型厂商 — 先做能力模型和 OpenAI-compatible，其他 Provider 按 adapter 扩展。
- v1 提供无限制 shell/file 工具 — 云上工具必须受 Workspace、Policy、Sandbox、Audit 约束。

## Context

- 参考项目 `earendil-works/pi` 已下载到 `/root/workspace/pi-agent`，本地提交为 `032c01c Add [Unreleased] section for next cycle`。
- pi 是 TypeScript/Node monorepo，核心模块包括：
  - `packages/ai`：多 Provider LLM API、模型元数据、流式事件。
  - `packages/agent`：Agent Runtime、Agent Loop、Tool Calling、State、Session Harness、Skills、Compaction。
  - `packages/coding-agent`：CLI、SDK、工具、扩展系统、设置管理、Session 管理。
  - `packages/tui`：终端 UI 框架。
- 本项目不是直接翻译 pi，而是借鉴其领域抽象，重新设计 Java 云原生 Agent Platform。
- 用户确认的产品方向：
  - 首要交付：Cloud Server。
  - 首批 Agent 类型：General Agent。
  - 技术边界：尽量 All Java。
  - v1 核心价值：扩展集成。
  - v1 UI：Agent Web Console + Admin Governance，Chat/Run 入口优先，Admin 治理辅助。
  - v1 扩展：SPI + Spring、Dynamic Plugins、Remote MCP 都纳入目标。
- 动态插件 + MCP + SPI 同入 v1 是高复杂度约束，需要在路线图中拆成可验证阶段，避免拖垮 Agent Runtime 内核。

## Constraints

- **Tech stack**: Java 优先，核心 Runtime、Cloud Server、Agent Web Console、Admin Governance 后端尽量采用 Java 生态 — 便于作为企业级云上 Agent 基座和基础 Jar/SDK。
- **Architecture**: Core 不直接依赖具体产品 UI、具体数据库、具体模型厂商或具体插件机制 — 保持 Runtime 可嵌入、可测试、可替换。
- **Generality**: Runtime 不绑定 Chat、Coding、单模型、单工具协议或单 UI — Chat 只是首个交互入口，核心必须支持多种 Agent 类型和运行模式。
- **Cloud safety**: 工具调用必须具备 Policy、Audit、Timeout、Approval/Sandbox 扩展点 — 云上 Agent 的主要风险来自 Tool 边界。
- **Extensibility**: Model、Tool、Memory、Workspace、Policy、EventSink、Plugin 都必须可扩展 — v1 核心价值是扩展集成。
- **Product order**: Cloud Server、Agent Web Console 和 Admin Governance 优先，TUI/CLI 作为后续客户端 — 避免本地 CLI 体验绑架云端内核设计。
- **Reference boundary**: pi 是参考设计而非迁移目标 — 避免继承 Node/TUI/本地文件系统的隐含假设。

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 以 Cloud Server 作为首要交付形态 | 用户明确优先云上 Agent 基座，而不是先做 Jar-only 或 TUI | — Pending |
| 以 General Agent 作为首批服务对象 | 通用集成能力优先，Coding Agent/Workspace 可作为后续模块 | — Pending |
| 采用 All Java 的核心技术边界 | 用户偏好 Java 全栈；利于企业集成、Spring、审计和部署 | — Pending |
| v1 核心价值聚焦扩展集成 | 工具、Provider、插件、MCP、Memory、业务系统接入是平台成败关键 | — Pending |
| Agent Web Console + Admin Governance 优先，TUI/CLI 后置 | v1 既需要用户 Chat 入口，也需要平台治理；TUI/CLI 不进入核心交付 | — Pending |
| Runtime 必须足够通用 | 避免把平台锁死为 Chat App、Coding Agent 或单一工作流系统 | — Pending |
| SPI + Spring + Dynamic Plugins + Remote MCP 都纳入 v1 目标 | 用户希望扩展范围更大；路线图需拆阶段控制复杂度 | — Pending |
| 不直接复刻 TypeScript pi | pi 面向本地 coding CLI，本项目面向 Java 云端通用基座 | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-06-13 after initialization*
