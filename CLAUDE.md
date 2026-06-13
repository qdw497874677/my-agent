<!-- GSD:project-start source:PROJECT.md -->
## Project

**Pi Java Agent Platform**

Pi Java Agent Platform 是一个基于 Java 的通用 Agent 基座，用于支撑云上 Agent 服务、Agent Web Console、Admin Governance、未来 TUI/CLI 客户端和插件生态。它借鉴 earendil-works/pi 的 Agent Loop、Provider Registry、Tool、Session、Extension、Skills 等思想，但面向云端、多租户、可集成、可扩展的产品平台重新设计。

第一阶段的首要交付形态是 Cloud Server + Agent Web Console：一个可部署的 Java 云上 Agent 服务，提供 Runtime、扩展集成、Agent Catalog、Chat 入口、Run/Session 管理、API/SSE、工具执行过程展示和基础 Admin Governance。核心实现尽量采用 Java 全栈，避免把 TypeScript pi 的 CLI/TUI 结构直接搬到核心。

**Core Value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。

### Constraints

- **Tech stack**: Java 优先，核心 Runtime、Cloud Server、Agent Web Console、Admin Governance 后端尽量采用 Java 生态 — 便于作为企业级云上 Agent 基座和基础 Jar/SDK。
- **Architecture**: Core 不直接依赖具体产品 UI、具体数据库、具体模型厂商或具体插件机制 — 保持 Runtime 可嵌入、可测试、可替换。
- **Cloud safety**: 工具调用必须具备 Policy、Audit、Timeout、Approval/Sandbox 扩展点 — 云上 Agent 的主要风险来自 Tool 边界。
- **Extensibility**: Model、Tool、Memory、Workspace、Policy、EventSink、Plugin 都必须可扩展 — v1 核心价值是扩展集成。
- **Product order**: Cloud Server、Agent Web Console 和 Admin Governance 优先，TUI/CLI 作为后续客户端 — 避免本地 CLI 体验绑架云端内核设计。
- **Reference boundary**: pi 是参考设计而非迁移目标 — 避免继承 Node/TUI/本地文件系统的隐含假设。
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Executive Recommendation
## Recommended Stack
### Core Language, Build, and Runtime
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| Java | **21 LTS** | Runtime and SDK language baseline | Use Java 21 as the minimum runtime. It gives virtual threads, modern language features, and broad enterprise support. Do not require Java 25 yet; it is newer and will reduce adoption for enterprise users. | HIGH |
| Maven | **3.9.x+** | Multi-module build and publishing | Use Maven for a Java SDK/platform intended for enterprise adoption. It matches Spring ecosystem conventions, BOM management, and publishing to Maven repositories. Gradle is fine technically, but Maven is less surprising for enterprise Java consumers. | MEDIUM |
| Spring Boot | **3.5.x**, latest patch at implementation time; Spring AI 1.1.5 release notes referenced Boot **3.5.14** | Cloud server, REST/SSE APIs, configuration, Actuator, security integration | Use Spring Boot 3.5.x for v1 production. It is the stable Spring Boot line paired with current Spring AI 1.x releases. Avoid starting on Spring Boot 4 unless Spring AI 2.x is stable and the team explicitly accepts migration churn. | HIGH |
| Spring Framework | Managed by Spring Boot 3.5 BOM | Dependency injection, web, validation, lifecycle | Do not pin directly unless needed. Let Spring Boot BOM manage it. | HIGH |
| Virtual threads | Java 21 + `spring.threads.virtual.enabled=true` | High-concurrency blocking model calls/tool calls | Enable virtual threads for request and execution workers where blocking provider/tool calls dominate. Still enforce timeouts, bulkheads, cancellation, and backpressure; virtual threads are not a safety model. | HIGH |
### Agent Runtime and AI Integration
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| Custom `pi-agent-core` | Project-owned | Agent loop, state machine, tool policy, provider abstraction, event model | Build this yourself as plain Java. It should have no dependency on Spring Web, Vaadin, PF4J, or concrete provider SDKs. This prevents framework lock-in and supports future CLI/TUI embedding. | HIGH |
| Spring AI | **1.1.5 stable** or latest 1.1.x patch; consider **2.0.x only after GA** | Default model/provider/tool abstraction, ChatClient, structured output, tool callbacks, observability, memory integration, MCP integration | Use Spring AI as the primary adapter for model calls, structured output, tool/function calling, and MCP bridge. It is Spring-native and covers major providers. Keep the runtime's provider interface narrower than Spring AI to avoid leaking Spring AI types into core. | HIGH |
| Spring AI OpenAI module | Spring AI managed | OpenAI-compatible provider support | Make OpenAI-compatible the first provider adapter. Most commercial/self-hosted gateways emulate OpenAI APIs, so this maximizes provider coverage with one adapter. | HIGH |
| Official provider SDKs | Via Spring AI where possible | Provider-specific escape hatches | Only add direct SDKs when Spring AI cannot expose a required capability. Wrap them behind the same provider interface. | MEDIUM |
| LangChain4j | **1.12.x+ line** found in Context7; validate latest before use | Optional integration/testing alternative | Do **not** use as core framework for v1. It has strong Java LLM abstractions, tools, memory, streaming, MCP support, and Spring Boot integration, but using both LangChain4j and Spring AI as first-class cores duplicates concepts. Consider a later `pi-langchain4j-adapter` only if customers request it. | MEDIUM-HIGH |
| LangGraph4j | v1.6.x line found in Context7; validate latest | Stateful graph/multi-agent workflows | Defer. The v1 requirement is a General Agent runtime, not a graph DSL. Add only after the internal run/step/event model is stable. | MEDIUM |
### Cloud Server API Layer
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| Spring MVC | Boot managed | REST API, Admin API, server-sent events | Use Spring MVC first. It is simpler with virtual threads and blocking provider SDKs. It supports SSE well enough for run event streaming. | HIGH |
| Spring WebFlux / Reactor | Boot managed | Reactive MCP/streaming integration where required | Use only at boundaries that are inherently reactive, such as MCP async clients or streaming bridges. Do not make the whole platform reactive by default. | MEDIUM-HIGH |
| Jackson | Boot managed | JSON serialization | Standardize public API schemas with Jackson. Be explicit about polymorphic event payloads; avoid exposing internal class names. | HIGH |
| Jakarta Bean Validation | Boot managed | API/request validation | Use for all API DTOs and plugin/provider config validation. | HIGH |
| springdoc-openapi | Validate latest 2.x/3.x compatible with Boot 3.5 | OpenAPI docs for REST API | Recommended for API docs if a machine-readable client contract is needed. Pin only after checking Boot 3.5 compatibility. | MEDIUM |
### MCP and Tool Integration
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| MCP Java SDK | **1.0.x line**; Spring AI 2.0 docs require MCP Java SDK 1.0.0 RC1+ | MCP client/server protocol implementation | Use the official MCP Java SDK through Spring AI MCP integration where possible. It supports sync/async clients, servers, tool discovery/execution, resources, prompts, sampling, elicitation, progress notifications, and structured logging. | HIGH |
| Spring AI MCP | Spring AI managed | Spring Boot starters, annotation model, MCP-to-ToolCallback bridge | Use for v1 remote MCP tools. It maps MCP tools into Spring AI tool callbacks and reduces custom protocol plumbing. Keep a thin project-owned `RemoteToolProvider` abstraction above it. | HIGH |
| Project-owned Tool Registry | Project-owned | Local Java tools, Spring Bean tools, plugin tools, MCP tools | Required. Normalize all tool sources into one registry with schema, policy, timeout, approval, audit, and execution metadata. Do not let MCP or Spring AI tool callbacks become the authoritative internal model. | HIGH |
| JSON Schema | Draft/version to validate during implementation | Tool input/output schema | Use JSON Schema-compatible contracts for tools. The model-provider layer, MCP layer, Admin GUI, and audit logs all need a stable schema format. | MEDIUM |
### Plugin and Extension System
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| Java SPI (`ServiceLoader`) | JDK built-in | Low-friction extension discovery for SDK users | Use as the lowest-level plugin/extension contract for `ToolProvider`, `ModelProvider`, `MemoryProvider`, `PolicyProvider`, etc. It keeps core embeddable and Spring-free. | HIGH |
| Spring Bean discovery | Boot managed | Enterprise/server extension registration | Support Spring Beans as first-class extension providers in the Cloud Server. This is the most natural integration path for Spring users. | HIGH |
| PF4J | Latest stable; docs confirm runtime plugin JAR loading/unloading and lifecycle | Dynamic plugin JAR loading | Use PF4J for dynamic plugin JARs. It provides plugin lifecycle (`start`, `stop`, `delete`), extension points, and runtime loading. Isolate it in `pi-plugin-pf4j`; do not make core depend on PF4J. | HIGH |
| PF4J Spring | Validate current compatibility before use | Bridge PF4J plugins with Spring contexts | Use cautiously. Treat it as an integration module, not core. Plugin classloader + Spring context lifecycle is a common source of leaks and complexity. | MEDIUM |
### Persistence, State, and Search
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| PostgreSQL | **18 current** where available; **17** acceptable conservative baseline | Durable sessions, runs, steps, tool calls, audit logs, configuration | Use PostgreSQL as the primary system of record. It has JSONB for flexible event/config payloads, full-text search, row-level security, robust transactions, indexing, and strong operational maturity. | HIGH |
| JDBC + Spring Data JDBC | Boot managed | Runtime persistence | Prefer JDBC/Spring Data JDBC over JPA for run/event/tool-call tables. The data model is event/state heavy and benefits from explicit SQL, predictable writes, and JSONB columns. | MEDIUM-HIGH |
| JPA/Hibernate | Boot managed | Optional admin/config entities | Avoid for core run/event persistence. Use only for simple admin/config CRUD if it materially speeds development. | MEDIUM |
| Flyway | Latest compatible with Boot 3.5 | Database migrations | Use Flyway from day one. Run/session/audit schemas will evolve; unversioned schema changes will cause migration pain. | HIGH |
| Redis or Valkey | Validate deployment target; version not researched deeply | Ephemeral cache, distributed locks, pub/sub, short-lived session acceleration | Optional in v1. Start without it unless horizontal scaling requires cross-node cancellation/event fan-out. PostgreSQL is enough for durable state. | MEDIUM |
| pgvector | Validate latest extension version before use | Embeddings/vector memory | Defer unless RAG/memory search is in the phase scope. If needed, start with pgvector on PostgreSQL rather than a separate vector DB to reduce operational load. | MEDIUM |
### Observability and Operations
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| Spring Boot Actuator | Boot managed | Health, metrics endpoints, operational introspection | Include in Cloud Server from phase 1. Agent platforms need visibility into provider latency, tool errors, queue depth, and plugin state. | HIGH |
| Micrometer | Boot managed | Metrics and Observation API | Use Micrometer as the application instrumentation facade. It powers Spring Boot observability and integrates with metrics/tracing backends. | HIGH |
| OpenTelemetry Java | Context7 verified **1.49.0**; validate latest 1.x before pinning | Vendor-neutral traces, metrics, logs export | Export via OTLP to collector/backends. Use custom spans around model calls, tool execution, plugin execution, MCP calls, policy decisions, and run lifecycle. | HIGH |
| OpenTelemetry Collector | Current stable at deployment time | Telemetry pipeline | Recommended for production deployment so the app is vendor-neutral. | HIGH |
| Structured logging | Logback + JSON encoder, version to validate | Correlatable logs | Use JSON logs with `trace_id`, `run_id`, `session_id`, `tenant_id`, `tool_name`, `provider`, and `plugin_id`. | MEDIUM-HIGH |
### Resilience, Safety, and Governance
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| Resilience4j | **2.2.0** found in Context7; validate latest | Retry, rate limiter, circuit breaker, bulkhead, time limiter | Use around model providers, MCP servers, and remote tools. Configure retries conservatively; tool calls are often non-idempotent. | HIGH |
| Spring Security | Boot managed | AuthN/AuthZ for Cloud Server/Admin GUI | Use OAuth2 Resource Server/JWT for APIs. Avoid building custom auth. Admin GUI should sit behind the same security model. | HIGH |
| Project-owned Policy Engine | Project-owned initially | Tool permissions, approval, sandbox gates, tenant limits | Start with a simple typed policy interface and auditable decisions. Do not bring in OPA/Cedar in v1 unless authorization complexity is proven. | MEDIUM-HIGH |
| Bucket4j or Resilience4j RateLimiter | Validate latest | Per-tenant/provider rate limiting | Use Resilience4j for internal remote-call limits first. Add Bucket4j later if you need distributed tenant quotas. | MEDIUM |
### Admin GUI
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| Vaadin Flow | **24.8.x** stable line; Context7 saw 24.8.4 and 24.9.2 | Java-first Admin GUI | Use Vaadin Flow for v1 Admin GUI because the project wants all-Java and the UI is operational/admin, not consumer-grade. It avoids introducing a TypeScript frontend stack early. | MEDIUM-HIGH |
| Vaadin Grid / Forms | Vaadin managed | Runs, sessions, tools, providers, plugins dashboards | Good fit for CRUD/admin monitoring screens. Use production mode in builds. | MEDIUM-HIGH |
| React/Next.js | Not recommended for v1 | Rich standalone frontend | Do not use for v1 unless UI complexity becomes product-critical. It adds a second language/build ecosystem and distracts from runtime/plugin/MCP risk. | HIGH |
### Testing and Quality
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| JUnit Jupiter | Boot managed | Unit/integration tests | Standard Java testing baseline. | HIGH |
| AssertJ | Boot managed | Fluent assertions | Use for readable domain tests. | HIGH |
| Mockito | Boot managed | Unit test mocks | Use sparingly; prefer fake providers/tools for runtime tests. | HIGH |
| Testcontainers Java | **2.0.3** current in Context7; 1.21.2 also present | PostgreSQL/Redis/MCP-like integration testing | Use for PostgreSQL, optional Redis, and fake external services. Agent platforms need integration tests for state/audit semantics. | HIGH |
| WireMock / MockWebServer | Validate latest | Provider/MCP HTTP simulation | Use for deterministic model-provider and tool-server tests. | MEDIUM |
| ArchUnit | Validate latest | Architecture boundary tests | Recommended to enforce `core` not depending on Spring, PF4J, Vaadin, provider SDKs, or persistence implementations. | MEDIUM-HIGH |
### Packaging and Deployment
| Technology | Version / Line | Purpose | Recommendation | Confidence |
|------------|----------------|---------|----------------|------------|
| Docker / OCI image | Current | Deployable cloud server | Build standard OCI images. Avoid native image for v1; dynamic plugins, reflection, provider SDKs, and Vaadin complicate GraalVM. | HIGH |
| Kubernetes | Current | Production deployment target | Design for K8s but do not require it for local dev. Expose health/readiness, config via env/secrets, and external PostgreSQL. | MEDIUM-HIGH |
| Spring Boot layered jars / buildpacks | Boot managed | Container image optimization | Use if it fits CI; otherwise Dockerfile is acceptable. | MEDIUM |
## Recommended Module Layout
## What NOT To Use as the v1 Foundation
| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Spring Boot 4 + Spring AI 2.0 milestone as baseline | Spring AI 2.0.0-M5 has breaking changes and active module movement. Starting v1 on milestones increases migration churn. | Spring Boot 3.5.x + Spring AI 1.1.x; track 2.0 for later upgrade. |
| LangChain4j as the core runtime | Good library, but it would own abstractions that the platform itself needs to own: agents, memory, tools, providers. | Custom core + optional LangChain4j adapter later. |
| Full WebFlux/reactive architecture by default | Most provider/tool SDKs are blocking or HTTP-client based; reactive everywhere adds complexity without solving policy/audit/state risks. | Spring MVC + virtual threads; reactive only at streaming/MCP boundaries. |
| JPA/Hibernate for run/event/audit log core | Agent events and tool calls are append-heavy, JSON-heavy, and need explicit query/index control. ORM mappings become friction. | JDBC/Spring Data JDBC + explicit SQL + JSONB. |
| Dynamic PF4J plugins before SPI contracts stabilize | Plugin classloaders and lifecycle are hard to change later. | Build SPI + Spring Bean extension first; add PF4J bridge once contracts are tested. |
| Direct provider SDK types in public API | Locks users and core runtime to provider churn. | Project-owned provider model with Spring AI/direct SDK adapters. |
| Unbounded shell/file tools | Cloud Agent risk is dominated by tools. Unlimited local capabilities create security and audit exposure. | Policy-gated, sandboxed, timeout-bound tools with audit records. |
| Native image in v1 | Dynamic plugins, reflection, Vaadin, and provider SDKs make it high-friction. | JVM container first; revisit native only for narrow edge deployments. |
| React/Next.js Admin UI in v1 | Adds TS/Node toolchain contrary to all-Java priority. | Vaadin Flow Admin GUI. |
## Installation Skeleton
## Version Confidence Notes
| Area | Confidence | Notes |
|------|------------|-------|
| Spring Boot 3.5.x | HIGH | Context7 lists 3.5.x; Spring AI 1.1.5 release notes mention upgrade to Boot 3.5.14. |
| Spring AI 1.1.5 / 2.0.0-M5 | HIGH | Official Spring blog confirms both. Recommendation prefers stable 1.1.x over 2.0 milestone. |
| MCP Java SDK 1.0.x | MEDIUM-HIGH | Spring AI MCP docs state Spring AI 2.0 requires MCP Java SDK 1.0.0 RC1+; exact GA patch should be checked at implementation time. |
| LangChain4j 1.x | MEDIUM | Context7 shows 1.12.1 and docs; not recommended as core. Validate latest Maven version before any adapter. |
| Vaadin 24.8.x/24.9.x | MEDIUM-HIGH | Context7 lists 24.8.4 and 24.9.2. Use latest Vaadin 24 LTS-compatible release with Boot 3.5 after validation. |
| Testcontainers 2.0.3 | HIGH | Context7 lists 2.0.3. |
| OpenTelemetry Java 1.49.0+ | MEDIUM | Context7 verified 1.49.0; exact 2026 latest likely higher. Pin via BOM after checking Maven Central. |
| PF4J latest | MEDIUM | Official docs verified capabilities; exact version not verified from official release page. Check Maven Central before pinning. |
| PostgreSQL 18/17 | MEDIUM-HIGH | Current PostgreSQL docs reflect 18; use 18 where ops supports it, 17 as conservative baseline. |
## Sources
- Spring AI reference via Context7: model providers, ChatClient, structured output, tool/function calling, observability, MCP integration.
- Spring AI official release post, 2026-04-27: Spring AI `1.0.6`, `1.1.5`, `2.0.0-M5`, Boot `3.5.14` upgrade, OpenAI SDK changes, MCP server improvements. URL: https://spring.io/blog/2026/04/27/spring-ai-1-0-6-1-1-5-2-0-0-M5-available-now
- Spring Boot 3.5 Context7 docs: virtual threads via `spring.threads.virtual.enabled=true`, Java 21+ behavior, Actuator/observability ecosystem.
- MCP Java SDK Context7 docs: sync/async clients, server capabilities, tools/resources/prompts, sampling, elicitation, progress, structured logging.
- LangChain4j Context7 docs: Java 17, Spring Boot 3.5+ integration, tools, memory, streaming, MCP support.
- PF4J official docs via Context7: plugin lifecycle, extension points, runtime plugin loading/unloading.
- OpenTelemetry Java Context7 docs: API/SDK/exporters, traces/metrics/logs, Java agent and OTLP exporter patterns.
- Micrometer Context7 docs: Observation API and Spring Boot observability integration.
- Vaadin Flow Context7 docs: Java UI framework, production build, Spring Security integration concerns.
- PostgreSQL current docs via Context7: JSONB, full-text search, row-level security.
- Resilience4j docs via Context7: Spring Boot 3 starter, retry, rate limiter, circuit breaker, bulkhead, time limiter.
- Testcontainers Java Context7 docs: version 2.0.3, JUnit 5, PostgreSQL and Redis container testing.
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
