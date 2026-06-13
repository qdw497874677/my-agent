# Feature Landscape

**Domain:** Java cloud Agent runtime/platform product  
**Project:** Pi Java Agent Platform  
**Researched:** 2026-06-13  
**Research focus:** Features dimension only — table stakes vs differentiators vs anti-features for a Cloud Server first, General Agent first, All-Java platform with Admin GUI v1 and broad extension via Java SPI, Spring, dynamic plugins, and MCP.

## Executive Takeaway

Modern cloud Agent platforms are no longer just “LLM wrapper + tools.” The common product shape is a production runtime that manages **agents, runs/sessions, tools, model providers, streaming events, state, observability, safety controls, and extension integration**. Evidence from LangGraph, Claude Managed Agents, Google Gemini Enterprise Agent Platform, Dify, OpenAI Agents SDK docs, Spring AI MCP, and MCP Runtime converges on the same baseline: if a platform cannot create a stateful run, stream events, call governed tools, inspect execution history, and integrate external tools/providers, it will feel incomplete.

For Pi Java, the v1 product should be opinionated: build a **runtime + API + admin observability + extension control plane**, not a visual workflow builder or marketplace. The table stakes are the execution substrate and governance surfaces. The differentiators should lean into the project’s chosen territory: **Java-native embeddability, Spring/SPI integration, dynamic plugin lifecycle, MCP as first-class remote tool fabric, and enterprise audit/policy around every tool call**.

## Table Stakes

Features users of a cloud Agent runtime/platform expect. Missing = the product feels incomplete or unsafe for production use.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Agent definition / configuration | Platforms define an agent as model + instructions + tools + runtime configuration; Claude Managed Agents explicitly uses Agent = model, system prompt, tools, MCP servers, skills. | Medium | v1 needs at least a `GeneralAgentDefinition` with prompt, model config, tool allowlist, policies, and runtime options. |
| Run / session lifecycle API | Production platforms expose concepts like sessions, threads, runs, workflow runs, or events; users need create, run, cancel, resume/query. | High | Core API should support create run, append input, stream events, cancel, get status, get history. This is the backbone for Admin GUI and future CLI/TUI. |
| Streaming event protocol | Streaming via SSE or equivalent is baseline for long-running agents; Claude Managed Agents streams SSE and OpenAI docs emphasize streaming/background modes. | Medium | Prefer stable server-side event taxonomy: model delta, step started, tool requested, tool result, policy decision, run completed, run failed. |
| Tool registry | Agents need discoverable tools with names, descriptions, schemas, side-effect metadata, and availability. Dify, Spring AI, OpenAI, and MCP all center tool interfaces. | Medium | Registry must unify local Java tools, Spring beans, plugin tools, and MCP-discovered tools behind one metadata model. |
| Tool execution engine | A platform must execute tool calls reliably with timeout, retries where safe, error capture, and structured outputs. | High | Treat tool execution as a separate subsystem from model calling. Never let model loop call arbitrary code directly. |
| Tool policy and approval hooks | Cloud tools are risky; OpenAI docs include guardrails/human review, MCP Runtime enforces per-tool grants/sessions, and project constraints require policy/audit. | High | v1 must have allow/deny, timeout, side-effect classification, approval extension point, and audit record even if UI approval is basic. |
| Tool audit trail | Enterprise users expect evidence of who/what called which tool, with inputs/outputs/status. MCP Runtime highlights per-call audit and compliance evidence. | Medium | Store run ID, step ID, tenant/user if available, agent ID, tool ID, decision, input summary/redacted payload, output summary, duration, error. |
| Model provider abstraction | Platforms support multiple model providers or at least provider-swappable model clients; project explicitly targets OpenAI-compatible first. | Medium | v1 should ship OpenAI-compatible provider and extension boundary for Anthropic/Gemini/Bedrock/etc. Do not hardwire model semantics into runtime. |
| Function/tool-calling loop | General agents require iterative model → tool call → result → model loop. Dify distinguishes native function calling vs ReAct; OpenAI Agents SDK discusses runtime loop/state. | High | Implement one reliable general loop first. ReAct-style fallback can be deferred unless required for non-tool-call models. |
| Session/run/step state model | LangGraph persistence stores checkpoints organized into threads; Dify exposes workflow/chatflow run IDs; cloud products expose run history. | High | Use explicit Run, Step, Event, ToolCall, Message/Artifact records. This prevents later rewrite when adding GUI, replay, evaluation, or memory. |
| Persistence abstraction | Stateful agents need durable history and recovery. LangGraph persistence enables resume, human-in-loop, time travel, and fault tolerance. | High | v1 can start with relational persistence and pluggable repository interfaces. Avoid in-memory-only except tests. |
| Admin GUI: run inspector | Cloud Server v1 needs an Admin GUI to view runs, events, tool calls, plugin status, and model config per project context. | Medium | Must show live status and historical traces. It does not need no-code flow editing in v1. |
| Plugin/extension listing and health | Because v1’s core value is extension integration, admins need to see which extensions/tools/providers/plugins are loaded and healthy. | Medium | Include source type: core, SPI, Spring bean, plugin JAR, MCP server. Show errors and disabled state. |
| Java SPI extension points | Java platforms expect ServiceLoader/SPI for library-style extension. Project explicitly requires SPI. | Medium | SPI should cover providers, tools, policies, memory, workspaces, event sinks. Keep binary compatibility discipline. |
| Spring Bean integration | Java cloud users expect Spring-native registration and dependency injection; Spring AI demonstrates Spring Boot first integration for AI/MCP. | Medium | Provide auto-configuration and annotations/adapters for registering tools/providers/policies from beans. |
| Remote MCP client integration | MCP has become a standard external tool/resource protocol; Spring AI 2.0 supports MCP client/server transports and Dify imports MCP tools. | High | v1 should be MCP client first: connect to remote MCP servers, discover tools, normalize schemas, execute with policy/audit. MCP server exposure can be v1.5/phase-later. |
| Basic dynamic plugin loading | Project explicitly requires dynamic plugin JARs. Platform users will expect install/load/unload or at least load/disable without core rebuild. | High | In v1, narrow scope to signed/approved plugin directory + isolated classloader + lifecycle + health. Hot unload is harder; defer if needed. |
| Secrets/configuration management boundary | Tools and providers require API keys/OAuth/secrets; Dify separates environment variables for secrets and Google/AWS platforms integrate identity/IAM. | Medium | v1 should avoid building a full vault; provide secret reference abstraction and env/config-backed implementation. |
| Authentication for Cloud Server APIs | Any cloud runtime must protect run creation, admin GUI, model configs, and tools. | Medium | Minimal v1: Spring Security with API key/OIDC-ready boundary. Multi-tenant RBAC can be phased. |
| Cancellation and timeout | Long-running agents need cancellation/interruption; Claude Managed Agents supports interrupt/steer and mcp-agent cloud lists cancel/suspend/resume. | Medium | Cancellation must propagate to model stream and running tools where possible. Timeouts should be per-run, per-step, per-tool. |
| Error handling and retry semantics | Agents fail due to model/tool/network/schema issues; users need status and recoverability. | Medium | Retries should be policy-bound and idempotency-aware. Do not blindly retry side-effectful tools. |
| Observability hooks | Production platforms emphasize traces/logs/metrics; Google uses Cloud Trace/Logging/Monitoring; Dify supports observability integrations; OpenAI docs emphasize traces. | Medium | v1 should emit structured logs/metrics/events and support OpenTelemetry spans around model calls and tool calls. |
| Basic cost/token accounting | Agent admins need visibility into model usage, latency, and token cost; mcp-agent cloud highlights token accounting. | Medium | Store prompt/completion tokens when provider returns them; expose per-run aggregate in Admin GUI. |
| Workspace/sandbox abstraction | Managed agents often provide sandboxed files/commands; project constraints require Workspace/Policy/Sandbox boundaries for tool safety. | High | v1 for General Agent should define abstraction and restrict built-in workspace operations. Full code execution sandbox can be deferred. |
| API-first backend | Dify publishes apps via API/MCP/web; OpenAI/Claude/Google products expose APIs. | Low | All GUI actions should use public-ish REST/SSE APIs so CLI/TUI can come later without new backend semantics. |

## Differentiators

Features that can create competitive advantage for Pi Java if prioritized after the runtime baseline is reliable.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Java-native Agent Runtime Kernel | Most prominent agent frameworks are Python/TypeScript-first. A clean Java kernel gives enterprise teams embeddable, inspectable runtime semantics in their existing Spring/JVM stack. | High | Make core independent of Spring and UI; Spring adapter is outer layer. This is the project’s strongest positioning. |
| Unified extension fabric: SPI + Spring + dynamic plugin + MCP | Users can extend through the mechanism that matches their deployment model: library, app bean, plugin JAR, or remote MCP server. | Very High | This is differentiating but risky. Roadmap must phase it: common registry first, then SPI/Spring, then MCP, then dynamic JAR lifecycle. |
| Governed tool fabric across local and remote tools | A single policy/audit layer for Java tools, plugin tools, and MCP tools is more valuable than merely “supports tools.” | High | Use MCP Runtime-style thinking: grants/sessions/allow-deny/side-effect metadata/audit on every call. |
| Admin GUI as runtime cockpit, not no-code builder | Focused operators can debug runs, inspect traces, manage tools/plugins/providers, and review policy decisions without a full workflow studio. | Medium | This fits Cloud Server v1 and avoids competing head-on with Dify-style visual builders. |
| Dynamic plugin lifecycle for enterprise extensions | Enterprises can add customer-specific tools/providers/policies without redeploying the core platform. | Very High | Treat as controlled runtime extension, not marketplace. Include health, compatibility, signature/trust hooks, disable/quarantine. |
| MCP gateway/client governance | Most MCP clients simply connect to servers; Pi can broker remote MCP tools through Java policy, audit, tenancy, and schema normalization. | High | Start as MCP client manager; later expose Pi agents/tools as MCP server or gateway. |
| Explicit side-effect classification for tools | Differentiates safety posture by making read/write/external-impact visible to policy and GUI. | Medium | Include metadata: read-only, writes internal data, external side effect, file-system, network, shell/code. |
| Pluggable policy engine | Allows enterprises to bring custom approval, RBAC, ABAC, quota, or compliance checks. | High | v1 can use Java interface + default implementation; later integrate OPA/Cedar-like engines if needed. |
| Durable replay/debug model | Persisted event log can enable step replay, time-travel-like debugging, regression evaluation, and incident analysis. | High | Start by storing enough immutable events. Actual replay can be later. |
| Agent-to-tool schema normalization | Normalize JSON Schema/OpenAPI/MCP/Spring annotations into one canonical tool schema. | High | High leverage for extension ecosystem. Needs careful versioning and validation. |
| Multi-provider capability model | Instead of only provider adapters, represent capabilities: streaming, tool calling, structured output, vision, token usage, caching. | Medium | Prevents hidden provider assumptions. OpenAI-compatible first, but capability flags prepare future providers. |
| Extension SDK with compatibility tests | Makes plugin/tool authors successful and reduces support burden. | Medium | Ship test harnesses and sample extensions. This is more valuable than a marketplace in v1. |
| Enterprise audit export/event sink | Security teams can forward tool/model/run events to SIEM, OpenTelemetry, Kafka, or webhooks. | Medium | Project already lists EventSink as extensible; implement one simple sink plus extension interface. |
| Java annotation-based tool registration | Spring AI MCP uses annotation-style tool/resource/prompt registration; Pi can offer `@AgentTool` for Spring apps. | Medium | Developer experience differentiator. Should compile to same registry metadata as SPI/plugin/MCP. |
| Tenant-aware execution context | Multi-tenant context, quotas, feature flags, and tenant-scoped tool visibility are important in SaaS agent platforms; AWS AgentCore multi-tenant guidance emphasizes tenant headers and scoped tools. | High | For v1, model context fields and interfaces; full tenant admin/RBAC can be later. |
| Provider/tool simulation mode | Developers can test agent loops with fake providers and tool stubs before connecting real systems. | Medium | Very useful for Java enterprise CI. Differentiates on reliability. |
| Built-in evaluation hooks | Google Agent Runtime includes quality/evaluation services; OpenAI docs include agent evals. Pi can expose run datasets and evaluator extension points. | High | Defer UI-heavy evaluation, but design run data so evals are possible. |
| Human approval workflow | Risky actions can pause for review, similar to LangGraph human-in-loop and OpenAI guardrails/human review. | High | v1 can provide approval hook + pending state; full approval UI/work queues can be later. |

## Anti-Features

Features to explicitly NOT build for v1 because they distract from the project’s Cloud Server / Java Runtime / extension-integration goal or create unsafe complexity.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Full Dify-style visual workflow builder | High UI/UX complexity, different product category, and not required for General Agent runtime v1. | Build Admin GUI run/plugin/tool cockpit. Keep workflow graph DSL out of v1. |
| Full plugin marketplace | Marketplace requires distribution, review, ratings, billing, trust, moderation, and ecosystem operations. Project already marks this out of scope. | Provide plugin protocol, SDK, local/private installation, metadata, and compatibility checks. |
| Unrestricted shell/file/code execution | Cloud agents with shell/files can exfiltrate data or damage systems. Project explicitly excludes unlimited shell/file tools. | Define Workspace/Sandbox abstraction and require policy, timeout, audit, and allowlisted tools. |
| Support every model provider in v1 | Broad provider support dilutes runtime work and multiplies edge cases. | Ship OpenAI-compatible provider and capability-based adapter SPI. Add Anthropic/Gemini/Bedrock later. |
| Recreate pi TypeScript CLI/TUI in Java core | The reference project is local CLI/TUI-oriented; project target is cloud server and Admin GUI. | Preserve protocol/event compatibility for future clients; keep core UI-neutral. |
| Multi-agent orchestration as v1 centerpiece | Multi-agent handoffs/workflows are valuable but require stable single-agent runtime, state, policy, and observability first. | Build General Agent loop first; design AgentDefinition/Run model to allow handoffs later. |
| Autonomous self-modifying plugin installation | Letting agents install/enable arbitrary plugins is a severe supply-chain and security risk. | Admin-only plugin lifecycle with trust/signature/compatibility hooks and audit. |
| Global mutable singleton registries | Incompatible with cloud multi-tenancy, tests, plugin reload, and per-agent policy. | Use scoped registries: platform, tenant/workspace, agent, run context. |
| Vendor-specific provider leakage in core model | Hardcoding OpenAI/Anthropic semantics into runtime makes later providers difficult. | Use provider capability flags and normalized tool-call/message abstractions. |
| Store full sensitive tool payloads by default | Audit is necessary, but raw secrets/PII in logs creates compliance risk. | Store redacted payloads and configurable secure retention. Keep full payload capture opt-in. |
| Hot unload as a guaranteed v1 dynamic plugin feature | JVM classloader unloading and resource cleanup are subtle and failure-prone. | Support load/disable/restart-required unload semantics first; add true hot unload after lifecycle hardening. |
| Build a proprietary MCP alternative | MCP is becoming the standard external tool/resource protocol. | Embrace MCP client support and normalize MCP tools into Pi’s registry/policy/audit model. |
| Heavy RAG/knowledge-base product in v1 | RAG platforms require ingestion, chunking, vector DBs, permissions, re-ranking, and quality evaluation. | Define Memory/Retrieval extension points; ship minimal memory/session state first. |
| End-user chat app as primary UI | Project targets Admin GUI v1, not consumer chat UX. | Provide REST/SSE API and simple run tester in Admin GUI if needed. |
| Production claims without governance | Agent platforms without policy/audit/observability create enterprise distrust. | Make governance visible in the MVP, even if policy rules are initially simple. |

## Feature Dependencies

```text
Agent definition → Run/session lifecycle → Streaming events → Admin run inspector

Model provider abstraction → Function/tool-calling loop → Tool execution engine

Tool registry → Tool policy → Tool execution engine → Tool audit trail → Admin tool-call inspector

Canonical tool schema → SPI tools / Spring Bean tools / Dynamic plugin tools / MCP tools

Persistence abstraction → Run history → Cancellation/resume → Human approval → Replay/evaluation later

Event model → SSE API → Admin live view → Future CLI/TUI clients

Extension metadata model → Plugin listing/health → Dynamic plugin lifecycle → Private plugin distribution later

MCP client integration → MCP tool discovery → MCP tool execution → MCP governance/audit → MCP gateway/server exposure later

Security/auth → Admin GUI → Plugin management → Policy management → Multi-tenant controls

Workspace abstraction → Safe file/shell/code tools → Sandboxed execution later

Token/cost accounting → Run observability → Quotas/budgets later
```

## MVP Recommendation

Prioritize these capabilities for the first credible Cloud Server / General Agent milestone:

1. **AgentDefinition + Run/Session/Step/Event model** — without this, all APIs and GUI views become ad hoc.
2. **OpenAI-compatible model provider + basic tool-calling loop** — proves the General Agent runtime works end-to-end.
3. **Unified Tool Registry + Tool Executor + Tool Policy + Tool Audit** — central to cloud safety and v1 extension value.
4. **REST/SSE Cloud Server API** — create, stream, cancel, query, and inspect runs.
5. **Admin GUI v1 runtime cockpit** — run list/detail, event stream, tool calls, provider config, extension/tool/plugin health.
6. **SPI + Spring Bean tool/provider registration** — lowest-risk Java-native extension path.
7. **Remote MCP client integration** — discover and execute remote MCP tools through the same policy/audit path.
8. **Controlled dynamic plugin loading** — plugin directory, metadata, isolated classloader, lifecycle, health, disable; defer true hot unload if needed.

Defer:

- **Visual workflow builder:** different product surface; not needed for Cloud Server runtime MVP.
- **Full RAG/knowledge-base product:** implement memory/retrieval extension boundaries first.
- **Multi-agent orchestration:** build after single General Agent run semantics are stable.
- **Full tenant/RBAC/marketplace:** design context/hooks now, productize after v1 runtime proves value.
- **Code execution sandbox:** define Workspace/Sandbox contracts now; build hardened execution as a dedicated later phase.

## Suggested Requirements Buckets

### Phase 1 — Runtime Spine

- AgentDefinition model
- Run, Session, Step, Event entities
- OpenAI-compatible provider adapter
- Agent loop with tool-call support
- REST create/run/status/cancel APIs
- SSE event stream
- Persistence abstraction and default repository

### Phase 2 — Governed Tools

- Canonical tool schema
- Tool registry and executor
- Timeout/error handling
- Policy checks and side-effect metadata
- Audit events and redaction
- Admin run/tool-call inspector

### Phase 3 — Java Extension Surface

- Java SPI for tools/providers/policies/event sinks
- Spring Boot starter / auto-configuration
- Annotation or bean-based tool registration
- Extension SDK samples and tests

### Phase 4 — MCP and Dynamic Plugins

- MCP client manager
- MCP discovery and schema normalization
- MCP execution through policy/audit
- Dynamic plugin JAR loader with lifecycle/health
- Plugin admin listing/disable/quarantine

### Phase 5 — Hardening and Differentiators

- Human approval pending state
- OpenTelemetry spans/metrics
- Token/cost dashboard
- Tenant-aware context and quotas
- Evaluation/replay hooks
- Workspace/sandbox implementation

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Runtime/session/tool/event table stakes | HIGH | Confirmed across LangGraph, Claude Managed Agents, Dify, OpenAI Agents SDK docs, Google Agent Runtime. |
| MCP as first-class external tool integration | HIGH | Confirmed by Spring AI MCP 2.0 docs, Dify MCP tools, Claude Managed Agents MCP support, and MCP Runtime governance docs. |
| Java-native extension differentiator | MEDIUM-HIGH | Strongly aligned with project constraints and Spring AI ecosystem; fewer direct Java cloud agent platform competitors verified. |
| Dynamic plugin lifecycle complexity | MEDIUM | JVM/plugin lifecycle risk is based on platform engineering experience; specific implementation approach needs architecture research. |
| Marketplace/no-code builder as anti-features | MEDIUM-HIGH | Supported by project scope and Dify comparison; depends on future product strategy but is correct for v1 focus. |
| Multi-tenant feature depth | MEDIUM | AWS/Google sources indicate importance; v1 requirements should include context hooks, not full SaaS tenancy unless explicitly needed. |

## Sources

- LangGraph documentation via Context7 — overview and persistence/human-in-loop: durable execution, persistence checkpoints by thread, streaming, memory, human-in-the-loop, visibility with LangSmith. Confidence: HIGH.
- Claude Managed Agents official docs — agent/environment/session/events concepts, SSE streaming, persistent event history, secure tools, MCP servers, long-running stateful sessions, sandbox options. https://platform.claude.com/docs/en/managed-agents/overview Confidence: HIGH.
- Google Cloud Gemini Enterprise Agent Platform Agent Runtime official docs — deploy/manage/scale agents, sessions, memory bank, code execution sandbox, observability, governance, agent identity, gateway, enterprise security. https://docs.cloud.google.com/gemini-enterprise-agent-platform/build/runtime Confidence: HIGH.
- OpenAI Agents SDK official docs — code-first agents, runtime loop/state, models/providers, tools/MCP, sandbox agents, orchestration/handoffs, guardrails/human review, observability/evals. https://platform.openai.com/docs/guides/agents Confidence: HIGH.
- Dify official docs — apps/workflow/chatflow, API/MCP publishing, run/conversation variables, plugin/custom/workflow/MCP tools, knowledge grounding. https://docs.dify.ai/en/use-dify/getting-started/key-concepts and https://docs.dify.ai/en/use-dify/workspace/tools Confidence: HIGH.
- Spring AI 2.0 MCP official docs — MCP Java SDK architecture, client/server/session/transport layers, tool discovery/execution, resources/prompts, stdio/SSE/Streamable HTTP transports, Spring Boot starters, annotations. https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html Confidence: HIGH.
- MCP Runtime docs — Kubernetes-native MCP deployment/governance, registry, gateway, per-tool policy, grants/sessions, multi-team isolation, audit and observability. https://docs.mcpruntime.org/ Confidence: MEDIUM (project is alpha but useful for governance patterns).
- AWS blog on multi-tenant agents with Bedrock AgentCore — session-isolated compute, tenant context headers, scoped tools, quotas/entitlements, gateway and ABAC patterns. https://aws.amazon.com/blogs/machine-learning/building-multi-tenant-agents-with-amazon-bedrock-agentcore/ Confidence: MEDIUM (blog guidance, current 2026).
