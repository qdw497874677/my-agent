# Phase 3: Model Provider Registry and OpenAI-Compatible Adapter - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-14
**Phase:** 03-Model Provider Registry and OpenAI-Compatible Adapter
**Areas discussed:** Registry shape, Streaming contract, Adapter dependency, Credential boundary, Resilience strategy, Provider tests

---

## Gray Areas Selected

| Area | Selected |
|------|----------|
| Registry 形态 | ✓ |
| Streaming 契约 | ✓ |
| Adapter 依赖 | ✓ |
| Credential 边界 | ✓ |
| Resilience 策略 | ✓ |

---

## Registry Shape

| Option | Description | Selected |
|--------|-------------|----------|
| 分层注册表 | ProviderDescriptor + ModelDescriptor + Capability + CredentialRef 分开建模；modelRef 解析到 provider/model/capabilities。扩展性最好。 | ✓ |
| 简单映射表 | 只把 modelRef 映射到一个 ModelClient/config。最快，但后续 SPI/Admin/provider 能力治理会补债。 | |
| 配置即注册 | 以 application.yml 中 provider 列表为主，不先做显式 registry API。适合最小实现，但不利于 Phase 6 扩展。 | |

**User's choice:** 分层注册表
**Notes:** Registry must guide future provider extensibility and governance instead of being a temporary map.

### ModelRef Rule

| Option | Description | Selected |
|--------|-------------|----------|
| provider:model | 例如 `openai-compatible:gpt-4.1-mini`。清晰、短、适合第一版；高级 endpoint/config 从 provider registry 查。 | ✓ |
| URI 风格 | 例如 `model://openai-compatible/gpt-4.1-mini?profile=fast`。表达力强，但 Phase 3 可能过度设计。 | |
| 别名优先 | Agent 只写 `default-fast` 这类别名，由 registry 解析到 provider/model。对产品友好，但不如 provider:model 透明。 | |

**User's choice:** provider:model
**Notes:** Use explicit provider/model references for early backend transparency.

---

## Streaming Contract

| Option | Description | Selected |
|--------|-------------|----------|
| 扩展 ModelClient | 引入 Pi 自有 streaming callback/chunk 契约，例如 ModelClient.stream/nextStreaming；adapter 产出 Pi-owned chunks，runtime/event sink 负责发布。 | ✓ |
| 旁路发事件 | 保持 next() 只返回 terminal response，adapter 自己拿 EventSink 发布 MODEL_DELTA。实现快，但 adapter 会知道运行事件语义，边界更耦合。 | |
| 先非流式 | 只做 terminal response，streaming 留后续。与 Phase 3 success criteria 不符。 | |

**User's choice:** 扩展 ModelClient
**Notes:** Streaming should remain a Pi-owned model contract; adapters should not bypass runtime/event ownership.

### Tool-Call Fragment Aggregation

| Option | Description | Selected |
|--------|-------------|----------|
| Adapter 聚合完整意图 | OpenAI adapter 负责把 provider stream fragments 聚合成完整 Pi ToolCallIntent；Domain/App 只看 provider-neutral 完整意图。 | ✓ |
| Runtime 聚合 chunks | Runtime 接收 tool-call delta fragments 并聚合。更通用，但会把 provider streaming 细节推入核心契约。 | |
| 双层事件 | 同时暴露 raw-ish tool-call deltas 和 complete intent。调试强，但事件契约更复杂。 | |

**User's choice:** Adapter 聚合完整意图
**Notes:** Prevent OpenAI-specific fragmented tool-call semantics from entering core contracts.

---

## Adapter Dependency

External research was performed through a librarian subagent. Key findings: Spring AI 1.1.x `OpenAiChatModel` is preferred over direct HTTP and over `OpenAiSdkChatModel`; streaming requires Reactor/WebFlux at the adapter boundary; Pi should not rely on Spring AI ChatClient advisor tool loop.

| Option | Description | Selected |
|--------|-------------|----------|
| Spring AI 封装 | Infrastructure provider module 用 Spring AI OpenAiChatModel，隔离在 Pi 自有 ModelClient/chunk 契约后；直接 HTTP 作为后续逃生口。 | ✓ |
| 直接 HTTP | 项目自己用 WebClient/JDK HTTP 解析 OpenAI SSE。控制力最高，但要自维护 stream/tool-call fragment/兼容性。 | |
| 双实现并行 | Spring AI + direct HTTP 都做。灵活但 Phase 3 范围和测试量显著增加。 | |

**User's choice:** Spring AI 封装
**Notes:** Use Spring AI for streaming/tool-call compatibility maintenance while preserving Pi-owned abstractions.

### Module Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| 新 provider 模块 | 新增类似 `pi-agent-infrastructure-model-openai`，隔离 Spring AI/WebFlux/provider 依赖，主 infrastructure 只组合。 | ✓ |
| 现有 infrastructure | 放进 `pi-agent-infrastructure`。更简单，但主基础设施模块会直接带上 Spring AI/WebFlux 依赖。 | |
| adapter-web 内 | 放在 Spring Boot Web 层。能跑但边界不佳，模型 IO 不应属于 Web Adapter。 | |

**User's choice:** 新 provider 模块
**Notes:** Heavy provider dependencies should be isolated.

---

## Credential Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| 引用 + resolver | Domain/App 只持有 CredentialRef/SecretRef；Infrastructure 通过 SecretResolver 在 provider adapter 边界解析 raw secret，raw secret 不进入 registry records/events。 | ✓ |
| 配置属性直传 | application.yml/env 直接绑定 apiKey 给 adapter。快，但 registry/事件/调试中更容易误泄漏。 | |
| 完整凭据库 | 实现 credential store、加密、轮换、审计。长期需要，但 Phase 3 过重。 | |

**User's choice:** 引用 + resolver
**Notes:** Raw secrets must be resolved only at Infrastructure adapter boundaries.

### Secret Sources

| Option | Description | Selected |
|--------|-------------|----------|
| 环境/配置引用 | 先支持 env var / Spring config property 引用，例如 `env:OPENAI_API_KEY`、`config:pi.providers.x.api-key`；Vault/KMS 留接口。 | ✓ |
| 仅环境变量 | 最安全简单：只允许 `env:*`。但本地/测试和多 provider 配置稍不方便。 | |
| 内建加密表 | 数据库存加密 secret。治理能力强，但 Phase 3 范围扩大。 | |

**User's choice:** 环境/配置引用
**Notes:** Vault/KMS/DB credential stores are extension points, not Phase 3 scope.

---

## Resilience Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| 保守默认 | timeout/cancellation 必启；retry 只对连接失败/429/5xx 且未开始输出时启用；rate-limit/circuit-breaker 可配置启用。 | ✓ |
| 积极重试 | 对更多 provider 错误自动重试，包括部分 streaming 中断。体验可能更好，但重复 token/工具调用风险更高。 | |
| 只留钩子 | Phase 3 只定义接口，不启用 retry/rate-limit/circuit-breaker 默认实现。范围小，但 MODEL-05 兑现偏弱。 | |

**User's choice:** 保守默认
**Notes:** Avoid retries after streaming output begins to prevent duplicate deltas/tool-call risks.

---

## Provider Tests

| Option | Description | Selected |
|--------|-------------|----------|
| Fake SSE 全覆盖 | 用 WireMock/MockWebServer 模拟 OpenAI-compatible SSE，覆盖 text delta、tool-call fragments 聚合、usage/finish、429/5xx、超时、取消、secret redaction。 | |
| 真实 provider smoke | 加入可选真实 OpenAI-compatible smoke test，需要 API key 才跑。可作为手工/disabled profile，不作为默认门槛。 | |
| 两者都要 | 默认 fake SSE contract tests + 可选真实 provider smoke。覆盖最好，但真实 provider 测试不能阻塞无 key CI。 | ✓ |

**User's choice:** 两者都要
**Notes:** Default fake-provider tests are required; real-provider smoke tests are optional and profile/env gated.

---

## the agent's Discretion

- Exact Java type names and method signatures for streaming model contracts.
- Exact capability descriptor field names beyond required provider-neutral coverage.
- Exact provider config property naming.

## Deferred Ideas

- Native non-OpenAI provider adapters.
- Direct HTTP implementation as default provider path.
- Admin UI provider/credential governance screens.
- Vault/KMS/DB-backed credential store and secret rotation.
- Full ToolExecutionGateway behavior.
