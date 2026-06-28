# Pi Java Agent Platform

## What This Is

Pi Java Agent Platform 是一个基于 Java 的通用 Agent 基座，用于支撑云上 Agent 服务、Agent Web Console、Admin Governance、未来 TUI/CLI 客户端和插件生态。它借鉴 earendil-works/pi 的 Agent Loop、Provider Registry、Tool、Session、Extension、Skills 等思想，但面向云端、多租户、可集成、可扩展的产品平台重新设计。

第一阶段的首要交付形态是 Cloud Server + Agent Web Console：一个可部署的 Java 云上 Agent 服务，提供 Runtime、扩展集成、Agent Catalog、Chat/Run 等 Agent 入口、Run/Session 管理、API/SSE、工具执行过程展示和基础 Admin Governance。核心实现尽量采用 Java 全栈，避免把 TypeScript pi 的 CLI/TUI 结构直接搬到核心。

## Core Value

让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。

## Current Milestone: v1.2 Console 对话产品化

**Goal:** 将当前“能跑通”的 Console 对话入口升级为可日常使用的 Kimi 首页式 Agent 对话体验，支持历史会话、真实流式显示、多轮上下文和稳定本地配置。

**Target features:**
- Kimi 首页式 Console：默认聚焦单一输入框和对话流，隐藏未完成/运维向噪音，同时保留必要的模型选择和配置入口。
- 历史会话能力：可查看最近会话、选择历史会话、恢复消息记录，并在选中会话上继续多轮对话。
- 真正流式显示：模型 delta 实时追加到同一个助手气泡，发送后无需等待完整 run 结束才看到回复。
- 多轮上下文：新消息能够携带当前 session 的历史对话上下文，避免每轮都是孤立 prompt。
- 本地开发对话体验稳定化：provider 配置、模型选择、SQLite 持久化、空配置 fallback、错误反馈和自动验证形成闭环。

## Requirements

### Validated

- [x] Phase 1 validated a Java 21 Maven/COLA runtime foundation with framework-free Domain contracts, typed Agent/Run/Step/Message/Tool/Event models, first-class Workspace/Session/Artifact contracts, runtime/model/tool/policy/event ports, deterministic fake model/tool/workspace testkit, and executable architecture/contract gates. Validated in Phase 1: Runtime Spine, Workspace, and Domain Contracts.
- [x] Phase 2 validated the Cloud Server foundation: authenticated REST run/session APIs, provider-neutral event DTOs, replay-before-subscribe SSE, App-layer orchestration, JDBC/Flyway persistence and queue implementations, baseline security/correlation, Spring composition root, and API documentation. Validated in Phase 2: Cloud Server, Persistence, SSE, and Baseline Security. Docker/Testcontainers verification is tracked as human UAT because this execution environment has no Docker socket.
- [x] Phase 3 validated the Model Provider Registry and OpenAI-compatible adapter: provider/model Domain and App contracts, explicit `provider:model` refs, provider-neutral streaming chunks/events, deterministic fake streaming runtime, isolated OpenAI-compatible infrastructure module, Spring AI-backed adapter, secret redaction, resilience hooks, Cloud Server provider wiring, no-key provider contract/E2E tests, and downstream contract documentation. Validated in Phase 3: Model Provider Registry and OpenAI-Compatible Adapter.
- [x] Phase 4 validated the Governed Tool Registry and Invocation Pipeline: canonical tool descriptors, descriptor-first registry/catalog contracts, a single `ToolExecutionGateway`, JSON Schema validation boundary, conservative policy/preview/approval semantics, audit/redaction/payload limiting, bounded dev/test workspace and allowlisted built-ins, Cloud Server `/api/tools` and `tool.lifecycle` mapping, no-key governed tool E2E, and downstream tool contract documentation. Validated in Phase 4: Governed Tool Registry and Invocation Pipeline.
- [x] Phase 5 validated the Agent Web Console and Runtime Cockpit: Vaadin Console/Admin surfaces, public Agent Catalog API/cards, chat/run/session/SSE workbench, governed tool cards, user/admin approval APIs and cards, inspect-only Admin Governance APIs/views, no-key Playwright browser E2E, and Phase 5 contract documentation. Validated in Phase 5: Agent Web Console and Runtime Cockpit.
- [x] Phase 6 validated the Java Extension Surface for SPI and Spring: framework-free extension API contracts, ServiceLoader discovery and normalized contribution registry, Spring Boot starter and lightweight tool/listener annotations, Cloud Server starter consumption, read-only Admin extension governance APIs/UI, safety conformance tests for gateway/policy/audit/event/redaction/credential boundaries, and downstream extension contract documentation. Validated in Phase 6: Java Extension Surface: SPI and Spring.
- [x] Phase 7 validated the MCP Client Bridge and Governed Remote Tools: isolated MCP infrastructure module, trusted server configuration with static credential refs and safety validation, MCP client/transport seams, discovery snapshots, descriptor normalization into ToolRegistry, gateway-only remote MCP invocation, Admin MCP REST/UI status and refresh, architecture isolation, redaction coverage, no-key product-path E2E, and downstream MCP contract documentation. Validated in Phase 7: MCP Client Bridge and Governed Remote Tools.
- [x] Phase 8 validated Controlled Dynamic Plugin JARs: isolated PF4J plugin infrastructure, controlled-directory configuration, PF4J-to-Pi extension bridging, refreshable/state-aware plugin governance, dynamic plugin ToolRegistry resolution, Admin REST/UI plugin governance, sample plugin JAR product-path E2E, disable/quarantine/allowlist/selected semantics, redaction and architecture gates, and downstream plugin operator documentation. Validated in Phase 8: Controlled Dynamic Plugin JARs.
- [x] Phase 14 validated Admin Governance full-site mobile coverage: Governance Overview, Registry, MCP, Plugin, Extension, Operations, Policy Decisions, and Audit surfaces now render as stacked mobile cards/details with conservative redaction, stable selector contracts, focused Java gates, and an MVER-04 Playwright Mobile Chrome route matrix. Validated in Phase 14: Admin Governance Full-Site Mobile Coverage.
- [x] Phase 15 validated cross-browser, orientation, accessibility, and release hardening: all Console/Admin routes now have portrait/landscape/tablet Playwright release smoke coverage, critical Console/Admin/mobile/desktop regression list gates, representative keyboard/focus/reduced-motion/no-hover hardening, static CSS accessibility contracts, and explicit release/UAT documentation separating Playwright proxies from true-device sign-off. Validated in Phase 15: Cross-Browser, Orientation, Accessibility, and Release Hardening.

### Active

- [ ] 将 Console 从临时可用对话入口产品化为 Kimi 首页式 Agent 对话体验，默认聚焦输入、回复和历史入口，隐藏未完成运维噪音。
- [ ] 实现历史会话列表、选择、恢复消息记录和基于历史会话继续对话。
- [ ] 实现真实流式 UI：模型 delta 实时追加到同一助手气泡，并能正确处理完成、错误和取消状态。
- [ ] 实现 session 级多轮上下文输入，让后续消息能利用当前会话历史。
- [ ] 稳定 local profile 对话闭环：页面配置 provider、模型选择、SQLite 持久化、fallback/error 反馈和 Playwright 验证门槛。
- [ ] 将现有 Vaadin Web Console 与 Admin Governance 升级为移动优先 H5 体验，覆盖所有已存在用户和管理页面。
- [x] 为主流手机浏览器和典型移动/平板视口建立可自动运行的响应式、触控和关键路径验证门槛。Phase 15 added all-route portrait/landscape/tablet release smoke coverage, layered Mobile Chrome/Mobile Safari/Mobile Firefox/Tablet/desktop Playwright list gates, accessibility hardening checks, Java CSS contracts, and a pending true-device UAT checklist for Android Chrome, iOS Safari, Edge mobile, and Firefox mobile.
- [ ] 构建 Java Agent Runtime 内核，支持 Agent Loop、事件流、模型调用、工具调用和会话状态管理。
- [ ] 保持 Runtime 足够通用，不绑定 Chat、Coding、单模型、单工具协议或单 UI，支持多种 Agent 类型和交互模式。
- [x] 采用 COLA 分层组织代码，确保 Adapter/App/Domain/Infrastructure 边界清晰，Domain/Runtime Core 零外层依赖。Validated in Phase 1: Runtime Spine, Workspace, and Domain Contracts.
- [x] 交付 Cloud Server，暴露 REST/SSE API，可创建、运行、取消、查询 Agent Run。Validated in Phase 2: Cloud Server, Persistence, SSE, and Baseline Security.
- [x] 提供扩展集成体系，v1 覆盖 Java SPI、Spring Bean 注册、动态插件 Jar 和远程 MCP 工具接入。Phase 6 delivered Java SPI, Spring Bean, and lightweight annotation extension paths with read-only governance and conformance tests. Phase 7 delivered governed Remote MCP tool discovery/execution/status with no-key E2E. Phase 8 delivered controlled dynamic plugin JAR loading, governance, refresh, disable/quarantine, and sample plugin product-path validation.
- [x] 提供 Model Provider 抽象，优先支持 OpenAI-compatible Provider，并保留 Anthropic/Gemini/其他厂商扩展边界。Validated in Phase 3: Model Provider Registry and OpenAI-Compatible Adapter.
- [x] 提供 Tool Registry、Tool Executor、Tool Policy、Tool Audit，确保工具调用可扩展、可治理、可审计。Validated in Phase 4: Governed Tool Registry and Invocation Pipeline.
- [x] 提供 Workspace 抽象，统一文件、命令、资源、Artifact、Snapshot 和会话隔离边界，为 bash/file/code 等能力提供受控执行上下文。Validated in Phase 1: Runtime Spine, Workspace, and Domain Contracts.
- [x] 提供 Session/Run/Step 状态模型，支持执行历史、事件记录、工具调用记录和后续持久化。Validated in Phase 1: Runtime Spine, Workspace, and Domain Contracts.
- [x] 提供 Agent Web Console + Admin Governance，用于发现 Agent、进入 Chat、发起 Run、查看工具执行过程、处理审批、继续会话，并治理模型、工具、插件、MCP、审计和运行状态。Validated in Phase 5: Agent Web Console and Runtime Cockpit.
- [ ] 建立端到端验证体系，围绕 Run 生命周期验证 API、Runtime、Model、ToolGateway、Policy、Audit、Event、Persistence、SSE 和 Web Console 的闭环。Phase 2 added Docker/Testcontainers-backed Cloud Server E2E and persistence gates for API/Persistence/SSE, with human UAT tracking for Docker-enabled execution. Phase 3 added no-key OpenAI-compatible provider contract tests and Cloud Server fake provider E2E for model.delta persistence/replay. Phase 4 added no-key governed tool Cloud Server E2E and security-redaction E2E for ToolGateway, Policy, Audit, Event, and workspace-bound tool paths. Phase 5 added no-key Playwright browser E2E for Agent Catalog, Chat/Run/SSE, tool cards, approvals, session continuation, cancellation, and Admin Governance. Phase 6 added no-key extension conformance tests proving SPI/Spring extension tools and providers stay behind ToolExecutionGateway, Policy, Audit, Event, redaction, workspace, and credential-reference boundaries. Phase 7 added no-key governed MCP product-path E2E, MCP redaction E2E, architecture gates, and Playwright MCP governance smoke coverage. Phase 8 added no-key controlled dynamic plugin JAR product-path E2E for load, refresh, disable, quarantine, redaction, architecture isolation, and sample plugin packaging. Phase 14 added an MVER-04 no-key Admin Governance mobile route matrix covering landing, overview, registry, operations, policy decisions, audits, and approvals. Phase 15 added cross-browser/orientation/accessibility/release hardening gates for all Console/Admin mobile routes plus true-device UAT tracking.
- [ ] 为未来 TUI/CLI 客户端保留协议和事件接口，但不把 TUI 作为 v1 核心交付。

### Out of Scope

- 完整复刻 earendil-works/pi 的 TypeScript monorepo — 该项目以 Java 云端平台为目标，pi 只作为参考设计。
- v1 复刻完整 TUI/CLI 体验 — 首要交付是 Cloud Server、Agent Web Console 和 Admin Governance，TUI/CLI 后置。
- v1 构建复杂 Agent Studio / 可视化编排器 — 先做 Agent Catalog + Chat 入口 + 运行过程展示，复杂构建器后置。
- v1 构建完整插件市场 — 先定义插件协议和加载机制，市场、分发、评分、计费后置。
- v1 支持所有模型厂商 — 先做能力模型和 OpenAI-compatible，其他 Provider 按 adapter 扩展。
- v1 提供无限制 shell/file 工具 — 云上工具必须受 Workspace、Policy、Sandbox、Audit 约束。
- v1 把宿主机文件系统或宿主机 bash 当作默认执行模型 — 必须通过 WorkspaceGateway / CommandExecutionGateway 进入受控边界。

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
- **Architecture**: 采用 COLA 分层（Adapter → App → Domain ← Infrastructure）；Domain/Runtime Core 不直接依赖具体产品 UI、具体数据库、具体模型厂商或具体插件机制 — 保持 Runtime 可嵌入、可测试、可替换。
- **Generality**: Runtime 不绑定 Chat、Coding、单模型、单工具协议或单 UI — Chat 只是首个交互入口，核心必须支持多种 Agent 类型和运行模式。
- **Cloud safety**: 工具调用必须具备 Policy、Audit、Timeout、Approval/Sandbox 扩展点 — 云上 Agent 的主要风险来自 Tool 边界。
- **Workspace boundary**: 文件、命令、Artifact、资源访问必须通过 Workspace 抽象，不直接依赖宿主机文件系统或本地 shell。
- **Extensibility**: Model、Tool、Memory、Workspace、Policy、EventSink、Plugin 都必须可扩展 — v1 核心价值是扩展集成。
- **Verification**: 每个阶段必须有可自动运行的验证门槛；关键路径必须通过 fake model/tool/MCP/plugin 和 Testcontainers 完成端到端验证。
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
| Workspace 是一等领域概念 | 为 bash/file/code/resource/artifact/snapshot 提供统一且受控的执行边界 | — Pending |
| 架构分层采用 COLA | 用 Adapter/App/Domain/Infrastructure 分离入口、应用编排、领域内核和技术实现，保护 Runtime Core | — Pending |
| SPI + Spring + Dynamic Plugins + Remote MCP 都纳入 v1 目标 | 用户希望扩展范围更大；路线图需拆阶段控制复杂度 | — Pending |
| 不直接复刻 TypeScript pi | pi 面向本地 coding CLI，本项目面向 Java 云端通用基座 | — Pending |

## Evolution

## Current State

Milestone v1.2 started — focus shifts from mobile coverage to Console conversation productization. The immediate product gap is that the Console can now produce a visible model response locally, but historical sessions, real-time streaming UI, session context, and Kimi-style chat information architecture still need to become first-class milestone requirements.

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
*Last updated: 2026-06-28 after starting milestone v1.2 Console 对话产品化*
