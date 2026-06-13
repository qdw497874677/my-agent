# Pi Java Agent Platform

Pi Java Agent Platform 是一个面向云端、企业集成和插件生态的 **Java 通用 Agent 基座**。它借鉴 [`earendil-works/pi`](https://github.com/earendil-works/pi) 的 Agent Loop、Provider Registry、Tool、Session、Extension、Skills 等思想，但不是 TypeScript pi 的 Java 复刻版，而是面向云上 Agent 服务重新设计的 Java 平台内核。

项目的第一阶段首要交付形态是 **Cloud Server + Agent Web Console**：一个可部署、可观测、可扩展、可治理的 Java 云上 Agent 服务，并提供各种 Agent 的统一入口、Chat/Run 等交互形态、Run/Session 管理、REST/SSE API、工具执行过程展示和基础 Admin Governance。

---

## 项目定位

Pi Java 的定位是：

> **Java Cloud Agent Runtime / Agent Platform Foundation**  
> 一个可嵌入业务系统、可部署为云服务、可扩展为多端产品序列的通用 Agent 基座。

它面向的不是单一聊天机器人，也不是本地 CLI 工具，而是一套可以支撑以下产品形态的底层平台：

- **Cloud Agent Server**：云上 Agent 运行服务，提供 REST/SSE API、Run 管理、Session 管理、工具治理和审计。
- **Agent Web Console**：面向用户的 Agent 入口，用于发现 Agent、进入 Chat/Run/Form 等交互形态、查看执行过程、处理审批和继续历史会话。
- **Admin Governance Console**：面向管理员和运维人员的治理入口，用于管理 Provider、Tool、Plugin、MCP、Policy、审计和运行状态。
- **Java SDK / 基础 Jar**：业务系统可以嵌入 Agent Runtime，注册自定义模型、工具、Memory、Workspace、Policy 和事件处理器。
- **Plugin / Extension Ecosystem**：通过 Java SPI、Spring Bean、动态插件 Jar、MCP 等机制扩展平台能力。
- **Future TUI / CLI Client**：未来可通过统一 REST/SSE 协议提供开发者终端体验，但不让 TUI/CLI 反向绑架核心架构。

---

## 核心价值

Pi Java 最重要的价值是：

> **让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。**

这意味着平台必须同时解决三类问题：

1. **能运行**：Agent 可以基于模型、上下文和工具完成可追踪的 Run。
2. **能扩展**：模型、工具、插件、MCP、Memory、Workspace、Policy 都能通过稳定接口接入。
3. **能治理**：云上工具调用必须具备权限、审计、超时、取消、审批、沙箱和可观测性边界。

---

## 为什么不是直接复刻 pi

`earendil-works/pi` 是一个优秀的 TypeScript/Node Agent Harness，主要面向本地 coding agent、CLI/TUI、工具调用和扩展体验。Pi Java 会重点参考它的设计思想：

- Agent Loop
- Provider Registry
- Tool Calling
- Session / Run / Event
- Extension API
- Skills / Prompt 组织方式
- Coding Agent 工具与交互经验

但 Pi Java 的目标不同：

| 维度 | TypeScript pi | Pi Java |
|---|---|---|
| 首要形态 | 本地 CLI / TUI coding agent | 云上 Agent Server |
| 核心环境 | 本地开发者工作站 | 云端、多租户、企业系统 |
| 权限假设 | 当前用户权限、本地文件系统 | Policy、Audit、Sandbox、Approval |
| 扩展方式 | TypeScript extension、npm/git packages | Java SPI、Spring Bean、动态插件 Jar、MCP |
| UI 重点 | TUI 交互体验 | Agent Web Console + Admin Governance |
| 架构目标 | Coding harness | Java Agent Platform Foundation |

因此本项目会把 pi 当作参考设计，而不是迁移目标。核心不会继承 Node/TUI/本地文件系统的隐含假设。

---

## 产品方向

Pi Java 的长期方向是形成一个通用 Agent Platform：

```text
Java Runtime Core
  + Cloud Server
  + Agent Web Console
  + Admin Governance Console
  + Extension SDK
  + MCP / Plugin / Tool Ecosystem
  + Future TUI / CLI / Web Clients
```

第一阶段聚焦 **Cloud Server + General Agent + Agent Web Console + Admin Governance + 扩展集成**。

后续再逐步增强：

- Coding Workspace / Repository Agent
- TUI / CLI 客户端
- 多 Agent 协作
- Workflow / Planner
- RAG / Knowledge Base
- Human-in-the-loop
- Evaluation / Replay
- Plugin Marketplace
- 多租户 RBAC / Quota / Billing

---

## v1 目标

v1 要验证的是：Pi Java 能否成为一个可信的云上 General Agent 基座。

v1 应具备：

### 1. Agent Runtime 内核

- AgentDefinition
- Agent Loop
- Session / Run / Step / Message / ToolCall / ToolResult / RunEvent
- 事件流
- 取消、超时、最大步数、终态管理
- Fake model / fake tool testkit
- Spring-free core contracts

### 2. Cloud Server

- Spring Boot 云服务
- REST API
- SSE RunEvent stream
- Run 创建、查询、取消
- Session / Run / Step / Event 持久化
- PostgreSQL + Flyway
- 基础安全上下文和结构化日志

### 3. Model Provider

- Provider Registry
- Model capabilities
- OpenAI-compatible streaming adapter
- Tool-call intent normalization
- CredentialRef / SecretRef
- Provider timeout、取消、错误归一化、使用量统计

### 4. Governed Tool Fabric

- ToolDescriptor
- ToolRegistry
- ToolExecutionGateway
- JSON Schema 参数校验
- ToolPolicy
- Approval / Sandbox hook
- Timeout / cancellation
- Audit trail
- Redaction
- Side-effect classification

### 5. Extension Fabric

- Java SPI
- Spring Bean / annotation registration
- Public extension API / SDK
- Extension metadata
- Version compatibility
- Health status
- Conformance tests

### 6. MCP Integration

- MCP client configuration
- Tool discovery
- Schema normalization
- MCP tools through ToolExecutionGateway
- MCP health, auth errors, invocation errors
- SSRF/network/security boundaries

### 7. Dynamic Plugins

- Controlled plugin directory
- Plugin descriptor
- Compatibility validation
- Lifecycle: discovered / loaded / started / disabled / failed / quarantined
- Admin disable/quarantine
- Explicitly not treating classloader isolation as security sandbox

### 8. Agent Web Console + Admin Governance

- Agent Catalog
- Agent Chat 入口
- Chat streaming
- Tool Call 卡片
- Run progress / timeline
- Session history
- Approval card
- Run list
- Runtime inspector
- Provider / Tool / MCP / Plugin status
- Audit view
- Cancel run / reject risky action
- API-first implementation

### 9. Observability and Security

- OpenTelemetry-compatible spans
- Metrics
- Structured logs
- Audit records
- Tenant/user/session/run/workspace/trace context
- Secret redaction
- Policy extension interface

---

## 架构原则

### 1. 通用基座优先

Pi Java 的核心不是“聊天系统”，也不是“Coding Agent”，而是通用 Agent Runtime。

核心模型必须支持多种 Agent 类型和交互形态：

- Chat Agent
- Task / Run Agent
- Workflow / Planner Agent
- Tool-only Agent
- Retrieval / Knowledge Agent
- Coding / Workspace Agent
- Business Process Agent

Chat 是 v1 Web Console 的首个入口形态，但不能成为 Runtime 的唯一抽象。Runtime 里应使用更通用的 `Run`、`Input`、`Message`、`Event`、`Artifact`、`Interaction`、`ToolCall` 等模型，避免把所有能力简化成 chat transcript。

### 2. Runtime Core 独立

核心 Runtime 不直接依赖 Spring Boot、Vaadin、PF4J、MCP、数据库或具体模型 SDK。

Core 只定义领域模型、状态机、端口和事件协议。

### 3. Adapter Containment

Spring Boot、Spring AI、MCP Java SDK、PF4J、Vaadin、PostgreSQL、OpenTelemetry 都是 adapter，不允许反向定义平台核心模型。

### 4. Event First

RunEvent 是平台的统一事实流。

REST/SSE、Agent Web Console、Admin Governance、未来 TUI/CLI、审计、Trace、Replay 都应该消费同一套事件语义。

### 5. Tool Gateway First

所有工具来源都必须通过同一个 `ToolExecutionGateway`：

- built-in Java tools
- Java SPI tools
- Spring Bean tools
- dynamic plugin tools
- MCP tools

任何工具调用都不能绕过 schema validation、policy、timeout、audit、redaction 和 observability。

### 6. Cloud Safety by Default

云上 Agent 的最大风险来自工具边界。

默认不提供无限制 shell/file/code execution。高风险工具必须显式经过 Workspace、Policy、Approval、Sandbox、Audit 约束。

### 7. API First

Agent Web Console、Admin Governance 和未来 TUI/CLI 使用同一套 REST/SSE/read-model API。

不允许 GUI 直接依赖私有 Runtime 或数据库细节。

### 8. Extension Without Core Pollution

### 9. UI 只是入口，不是内核

Agent Web Console、Admin Governance、未来 TUI/CLI 都是 Runtime 的客户端。

它们可以提供不同体验：Chat、任务表单、Agent Catalog、审批、Timeline、调试视图、治理视图，但不能让 UI 形态决定核心运行模型。所有客户端都应通过统一 API/Event 协议访问 Runtime。

模型、工具、Memory、Workspace、Policy、EventSink、Plugin 都可扩展，但扩展机制不能污染核心 Runtime。

先稳定 Java SPI / Spring Bean，再接 MCP，再接动态插件 Jar。

---

## 技术方向

推荐技术栈：

| 领域 | 方向 |
|---|---|
| Language | Java，优先面向企业 LTS 运行环境 |
| Build | Maven multi-module |
| Cloud Server | Spring Boot |
| Runtime Core | Framework-independent Java |
| Model Adapter | Spring AI 可作为 adapter，但不进入 core model |
| API | REST + SSE |
| Persistence | PostgreSQL + Flyway + JDBC/Spring Data JDBC |
| Web GUI | Vaadin Flow（v1 保持 All Java；优先 Agent Web Console + Admin Governance） |
| Extension | Java SPI + Spring Bean + PF4J + MCP |
| Observability | Micrometer + OpenTelemetry + Actuator |
| Resilience | Resilience4j |
| Security | Spring Security + platform policy engine |
| Testing | JUnit, AssertJ, Testcontainers, fake providers/tools, ArchUnit |

> 具体版本以实现阶段验证为准。当前规划推荐 Spring Boot 3.5.x、Spring AI 1.1.x stable、PostgreSQL 17/18、Vaadin 24.x、PF4J 作为动态插件候选。

---

## 路线图

当前 v1 规划为 9 个阶段：

| # | Phase | Goal |
|---|---|---|
| 1 | Runtime Spine and Domain Contracts | 建立 Spring-free Agent Runtime contracts、状态模型、事件 envelope、取消机制和 testkit |
| 2 | Cloud Server, Persistence, SSE, and Baseline Security | 通过 Spring Boot REST/SSE 和 PostgreSQL 暴露云上 Runtime |
| 3 | Model Provider Registry and OpenAI-Compatible Adapter | 引入真实模型流式调用、Provider Registry、usage/error 归一化 |
| 4 | Governed Tool Registry and Invocation Pipeline | 构建所有工具调用必须经过的安全网关 |
| 5 | Agent Web Console and Runtime Cockpit | 提供 all-Java Agent 入口和运行时 cockpit，用于 Agent Catalog、Chat/Run 入口、工具过程、审批、历史会话和基础治理 |
| 6 | Java Extension Surface: SPI and Spring | 稳定 Java SPI 与 Spring Bean/annotation 扩展 API |
| 7 | MCP Client Bridge and Governed Remote Tools | 将 MCP remote tools 接入统一 Tool Gateway |
| 8 | Controlled Dynamic Plugin JARs | 支持可信插件 Jar 的生命周期、健康、禁用和隔离 |
| 9 | Observability, Policy, Tenancy, and Production Hardening | 完成生产级可观测、安全、租户上下文和治理能力 |

完整规划见：

- [Project Context](.planning/PROJECT.md)
- [Requirements](.planning/REQUIREMENTS.md)
- [Roadmap](.planning/ROADMAP.md)
- [Research Summary](.planning/research/SUMMARY.md)

---

## 明确不做

v1 明确不做：

- 不做 TypeScript pi 的逐模块复刻。
- 不做完整 TUI/CLI 产品体验。
- 不做 Dify-style 可视化 workflow builder。
- 不做复杂 Agent Studio，但 v1 要提供基础 Agent Catalog + Chat 入口。
- 不做完整插件市场。
- 不做所有模型厂商适配。
- 不做无限制 shell/file/code execution。
- 不承诺 JVM 插件热卸载。
- 不做完整 RAG/Knowledge Base 产品。
- 不让 Agent 自主安装插件。

这些能力可以在 Runtime、Policy、Extension、Workspace、Event 模型稳定后逐步扩展。

---

## 当前状态

项目已完成 GSD 初始化：

- `.planning/PROJECT.md`
- `.planning/REQUIREMENTS.md`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`
- `.planning/research/`

下一步：

```text
/gsd-discuss-phase 1
```

或直接规划第一阶段：

```text
/gsd-plan-phase 1
```

---

## License

TBD.
