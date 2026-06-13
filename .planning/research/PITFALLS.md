# Domain Pitfalls

**Domain:** Java cloud Agent platform with SPI/Spring/dynamic plugins/MCP/Admin GUI/sessions/observability/policy-security  
**Project:** Pi Java Agent Platform  
**Researched:** 2026-06-13  
**Overall confidence:** HIGH for Java SPI/Spring Boot/MCP auth pitfalls from official docs; MEDIUM for agent security operations patterns from OWASP/current industry sources; MEDIUM for JVM plugin isolation practices from recent community/production writeups.

## Executive Guidance

This project’s highest-risk area is not the basic Agent Loop; it is the combination of **cloud execution + extensibility + tool authority**. Java SPI, Spring extension points, dynamic plugin JARs, and remote MCP are all reasonable independently, but putting all four into v1 can create undefined lifecycle semantics, classloader leaks, ambiguous security boundaries, and an Admin GUI that observes too late to prevent unsafe actions.

The roadmap should treat extension integration as a sequence of contracts, not as implementation plumbing. First define stable contracts for `Tool`, `ModelProvider`, `Plugin`, `Session`, `Run`, `Step`, `PolicyDecision`, and `EventEnvelope`; then support built-in/SPI/Spring registration; then add dynamic plugins; then add remote MCP. Skipping this order creates a platform where each integration mechanism invents its own lifecycle, policy, metrics, and failure behavior.

The core prevention pattern is: **all tool execution must pass through one deterministic gateway** with policy, timeout, cancellation, tenant/user/session context, audit, and observability. Neither Spring beans, SPI providers, dynamic plugins, nor MCP tools should be allowed to bypass that gateway.

---

## Critical Pitfalls

Mistakes that cause rewrites, security incidents, or unusable extension ecosystems.

### Pitfall 1: Treating SPI, Spring Beans, dynamic plugins, and MCP as equivalent registration mechanisms

**What goes wrong:**  
The platform allows tools/providers to be registered through multiple paths, but each path has different lifecycle behavior. SPI providers are lazily instantiated and outside Spring lifecycle by default; Spring beans are dependency-injected and lifecycle-managed; dynamic plugins need load/enable/disable/unload semantics; MCP tools are remote capabilities with transport/auth failure modes. If these are merged directly into `ToolRegistry`, runtime behavior becomes inconsistent and hard to govern.

**Why it happens:**  
Teams focus on discovery (`ServiceLoader`, `ApplicationContext`, scanning JARs, MCP `tools/list`) rather than defining the extension contract and activation model.

**Consequences:**
- Tool appears in Admin GUI but cannot execute because dependencies/lifecycle are missing.
- Policy/audit works for local tools but not for MCP tools.
- Plugin unload leaves stale tool definitions in registries.
- Spring-only assumptions leak into core APIs, making the Runtime no longer embeddable.
- Roadmap gets stuck because every new extension type needs special cases.

**Warning signs / early detection:**
- `ToolRegistry` has methods like `registerSpringBean`, `registerSpi`, `registerMcpTool` instead of accepting normalized descriptors and executors.
- Extension interfaces require Spring annotations or application context access.
- MCP tools are invoked from a separate code path rather than through the same `ToolExecutor`.
- No explicit states such as `DISCOVERED`, `VALIDATED`, `ENABLED`, `DISABLED`, `FAILED`.
- Admin GUI cannot explain why a tool is visible, enabled, disabled, or rejected.

**Prevention strategy:**
- Define a small framework-neutral **extension kernel API** before implementation mechanisms:
  - `ExtensionDescriptor` / `PluginDescriptor`
  - `CapabilityDescriptor`
  - `ToolDescriptor`
  - `ToolExecutor`
  - `ExtensionLifecycle` (`load`, `validate`, `enable`, `disable`, `close`)
  - `HealthContributor` / `PolicyContributor` / `EventSink`
- Build adapters:
  - SPI adapter discovers capability factories.
  - Spring adapter wraps existing beans into capability descriptors.
  - Dynamic plugin adapter loads JARs and exposes capabilities.
  - MCP adapter maps remote tool schemas into local `ToolDescriptor`s.
- Require all adapters to produce the same normalized registry entries and flow through the same execution gateway.
- Add a conformance test suite that every extension mechanism must pass: discovery, validation, enable/disable, execution, policy deny, timeout, cancellation, audit, metrics, unload.

**Phase should address:** Phase 1 / Runtime Contract Foundation. Do not start dynamic plugins or MCP integration before this contract exists.

**Confidence:** HIGH. Java `ServiceLoader` official docs describe lazy loading/caching/errors and classloader-specific discovery; Spring Boot docs describe classloader restrictions; MCP docs define transport/auth-specific requirements.

---

### Pitfall 2: Building dynamic plugins on top of ad-hoc classpath scanning

**What goes wrong:**  
The platform scans arbitrary plugin JARs for annotations/classes and loads them into the application classloader. This appears to work in development, then fails in packaged Spring Boot JARs, duplicates dependencies, breaks when two plugins use different library versions, and prevents clean unload.

**Why it happens:**  
Java developers are used to Spring component scanning. Plugin systems require classloader boundaries; Spring application scanning is not a plugin isolation model.

**Consequences:**
- Dependency hell: plugin A requires a library version incompatible with plugin B or the host.
- `ClassCastException` because API interfaces are loaded by multiple classloaders.
- Memory leaks because plugin objects, threads, or static caches keep classloaders alive.
- Plugin reload does not actually unload old bytecode.
- Spring Boot executable JAR packaging breaks discovery because nested JAR loading is not the same as a flat classpath.

**Warning signs / early detection:**
- Use of `ClassLoader.getSystemClassLoader()` to discover plugins.
- Runtime scanning for all classes in plugin JARs without an explicit descriptor or service file.
- Plugin API classes are bundled inside plugin JARs instead of provided by a shared parent/API classloader.
- No test where two plugins depend on conflicting versions of the same library.
- No unload test that checks classloader garbage collection after disable/remove.
- No explicit `close()` path for plugin classloaders/resources.

**Prevention strategy:**
- Use explicit plugin metadata and `ServiceLoader`/descriptor-based discovery rather than scanning arbitrary classes.
- Put plugin API contracts in a dedicated `pi-agent-api` module loaded by the host/shared parent.
- Use one isolated classloader per plugin for dynamic plugins; delegate only Java/platform and explicit API packages to parent.
- Validate plugin JARs at load time:
  - required manifest/descriptor present
  - compatible platform API version
  - service provider files present
  - no duplicate API classes inside plugin artifact
  - declared permissions/capabilities present
- Treat JVM plugin isolation as **accidental-failure isolation**, not malicious-code sandboxing. Untrusted plugins should run out-of-process or in containers.
- Close classloaders and release references on unload; stop plugin-created threads/executors; clear registries and caches.
- Create packaging tests against the actual Spring Boot executable artifact, not only IDE/classpath tests.

**Phase should address:** Phase 3 / Dynamic Plugin Runtime. Phase 1 should define the API boundary; Phase 3 should implement isolation and unload tests.

**Confidence:** HIGH for classloader/SPI mechanics from Oracle and Spring Boot docs; MEDIUM for implementation pattern specifics from production/community plugin architecture writeups.

---

### Pitfall 3: Assuming Java classloader isolation is a security sandbox

**What goes wrong:**  
The platform loads third-party plugin code in the same JVM and assumes a child classloader prevents malicious behavior. It does not. Plugin code can still consume CPU/memory, start threads, access network/files depending on process permissions, read environment variables, and interfere with process-wide state.

**Why it happens:**  
Classloader isolation solves dependency visibility and unloadability problems, not full process security. Modern Java no longer gives teams a simple, reliable in-process security-manager model for arbitrary hostile code.

**Consequences:**
- Plugin can exfiltrate credentials available to the host process.
- Plugin can overload CPU/memory and take down the cloud server.
- Plugin can mutate global state, system properties, logging, thread context classloaders, or static singletons.
- Enterprise users will not trust the platform’s plugin story.

**Warning signs / early detection:**
- Product copy says “sandboxed plugin” but implementation is only `URLClassLoader`.
- Plugins receive host credentials, datasource objects, or raw Spring `ApplicationContext`.
- No plugin permission declaration model.
- No resource limits for plugin-executed tools.
- No mode distinction between trusted in-process plugins and untrusted external plugins.

**Prevention strategy:**
- Define trust tiers:
  - **Built-in / trusted:** host classpath or Spring beans.
  - **Trusted dynamic plugin:** isolated classloader, declared permissions, still same process.
  - **Untrusted extension:** out-of-process worker/container or remote MCP-like boundary.
- Do not expose raw host internals to plugin code. Provide narrow capability APIs.
- Require plugin permission manifests and enforce permissions at the Tool Execution Gateway, not inside plugin code.
- For untrusted or customer-supplied code, execute out-of-process with OS/container controls: CPU/memory/time limits, filesystem mounts, network egress allowlists, secret isolation.
- Document the security model honestly in Admin GUI and docs.

**Phase should address:** Phase 3 / Dynamic Plugin Runtime and Phase 5 / Policy & Sandbox. The trust-tier decision must be made before plugin marketplace/distribution work.

**Confidence:** MEDIUM-HIGH. JVM isolation limits are well-known; recent plugin writeups explicitly warn plugins must be trusted and can access the JVM. Security hardening sources align on container/process isolation for untrusted tools.

---

### Pitfall 4: Letting tools bypass deterministic policy, timeout, cancellation, and audit

**What goes wrong:**  
Tools are called directly by the Agent Loop, plugin code, Spring beans, or MCP adapter. Some paths log; some enforce policy; some support cancellation; some do not. The model can trigger powerful operations without a uniform enforcement point.

**Why it happens:**  
Early prototypes optimize for “tool call works” and add policy/audit later. In agent platforms, policy/audit added later often cannot reconstruct enough context.

**Consequences:**
- Cloud safety requirement fails: no reliable `who/what/why/with-which-authority` trail.
- Admin GUI shows runs but cannot prove what happened.
- A prompt injection can reach privileged tools because policy is advisory or model-mediated.
- Cancellation stops the run but not a long-running tool.
- Tool failures produce inconsistent events and broken session state.

**Warning signs / early detection:**
- Tool interfaces expose only `execute(args)` without `ToolExecutionContext`.
- Policy is checked in prompts or model instructions instead of deterministic code.
- Tool logs are emitted by the agent layer rather than the executor/gateway layer.
- Timeout/cancellation is implemented only around model calls.
- Audit records omit tenant, user, session, run, step, tool version, permission decision, or resource target.

**Prevention strategy:**
- Build a single `ToolExecutionGateway` used by all tools: built-in, SPI, Spring, dynamic plugin, MCP.
- Require `ToolExecutionContext` containing tenant, user/principal, agent, session, run, step, workspace, authorization context, policy context, deadline, cancellation token, trace/span IDs.
- Enforce before execution:
  - schema validation
  - policy decision
  - allow/deny/approval requirement
  - rate limits and budgets
  - timeout/deadline
  - workspace/resource constraints
- Audit from the gateway, not from tools or the model.
- Treat authorization failures as hard stops; do not retry with fallback credentials or service accounts.
- Add tests proving no registered tool can execute except through the gateway.

**Phase should address:** Phase 1 / Runtime Core for gateway shape; Phase 2 / Cloud Server for tenant/user/session context; Phase 5 / Policy & Security for full enforcement.

**Confidence:** HIGH. This follows project constraints and is supported by OWASP/agent auth guidance emphasizing excessive agency, deterministic authorization, and full-context audit.

---

### Pitfall 5: Designing MCP as “just another HTTP tool source” and ignoring MCP authorization requirements

**What goes wrong:**  
The platform connects to remote MCP servers, lists tools, and executes them using static tokens or passthrough user tokens. It does not validate token audience/resource binding, does not implement proper 401 challenge handling, and does not distinguish public vs protected tools.

**Why it happens:**  
MCP looks like JSON-RPC-over-transport plus tool schemas, so teams defer OAuth details. The MCP authorization spec is transport-level and easy to miss in a prototype.

**Consequences:**
- Confused deputy vulnerabilities.
- Tokens issued for one resource are accepted or forwarded to another.
- Bearer tokens leak in logs, URLs, prompts, or tool outputs.
- Enterprise MCP servers reject the platform because it cannot parse `WWW-Authenticate`/protected-resource metadata flows.
- Unauthorized protected tools return model-visible errors instead of HTTP-layer auth challenges.

**Warning signs / early detection:**
- MCP credentials stored as plain provider config and injected into prompts/context.
- Platform forwards inbound user access tokens directly to upstream MCP/resource APIs.
- MCP client does not use `resource` parameter/resource indicators.
- 401/403 responses are treated as ordinary tool errors for the model to work around.
- No per-server/per-tool authorization model.
- No SSRF controls for metadata discovery or OAuth-related URL fetching.

**Prevention strategy:**
- Implement MCP remote tools behind the same `ToolExecutionGateway`, but with a dedicated MCP client/auth subsystem.
- Follow MCP HTTP authorization rules when authorization is supported:
  - bearer tokens in `Authorization` header, not query strings
  - validate/expect proper `WWW-Authenticate` metadata on 401
  - use OAuth protected-resource metadata and authorization-server metadata discovery
  - use PKCE for authorization code flow
  - include the `resource` parameter / resource indicators
  - validate audience/resource; reject token passthrough
  - HTTPS only outside localhost development
- Store tokens encrypted and never expose tokens to model context or Admin GUI logs.
- Support per-server and eventually per-tool authorization; protected tool calls should fail closed before reaching the remote tool.
- Add SSRF protections for all discovered URLs: scheme allowlist, DNS/IP private range denylist unless explicitly configured, redirect limits, timeout, response size limits.

**Phase should address:** Phase 4 / Remote MCP Integration. Minimal MCP can start with manually configured trusted servers, but production MCP auth must be a roadmap gate before multi-tenant use.

**Confidence:** HIGH. MCP official authorization docs explicitly require OAuth 2.1-style measures, resource indicators, token validation, `WWW-Authenticate`, HTTPS, PKCE, and no token passthrough.

---

### Pitfall 6: Giving the agent the user’s full authority instead of intersected, purpose-limited authority

**What goes wrong:**  
The platform lets an agent act with a full user token, an admin service account, or broad API key. If a prompt injection or ambiguous instruction manipulates the agent, the agent has the blast radius of the user/admin credential.

**Why it happens:**  
It is easier to reuse web-app auth sessions or service credentials than to model agent-specific delegated authority.

**Consequences:**
- “Excessive agency” incidents: delete/export/send/modify operations happen under valid credentials but unintended context.
- Audit says the user did it, not which agent/tool/policy allowed it.
- Multi-tenant isolation becomes fragile.
- Security review blocks release.

**Warning signs / early detection:**
- Tool context contains raw user session token.
- All tools use one platform API key.
- Policy checks only user permissions, not agent/tool/session/workspace permissions.
- No way to answer: “Why was this tool call allowed?”
- Admin GUI cannot show effective authority per run/tool call.

**Prevention strategy:**
- Use an **authority intersection rule**: effective tool authority = tenant policy ∩ user permissions ∩ agent permissions ∩ tool permissions ∩ session/workspace constraints ∩ approval state.
- Create scoped, short-lived delegated credentials or internal capability tokens for tool execution; do not pass raw user tokens to tools unless explicitly required and audited.
- Require high-risk tools to declare operation/resource scopes.
- Implement approval gates for destructive, external side-effect, credential, production-data, or spend-money tools.
- Make policy deterministic; never ask the model whether access is allowed.

**Phase should address:** Phase 2 / Cloud Server Auth Context and Phase 5 / Policy & Security. Do not expose destructive tools before this exists.

**Confidence:** MEDIUM-HIGH. OWASP identifies excessive agency as a core LLM application risk; current agent auth guidance consistently recommends scoped/intersected authority and deterministic enforcement.

---

### Pitfall 7: Treating prompt injection as a prompt-engineering problem instead of an information-flow/policy problem

**What goes wrong:**  
The system prompt says “ignore malicious instructions,” but the agent ingests untrusted content from web pages, files, tickets, emails, MCP tool output, or memory and then invokes privileged tools. Tool responses may contain instructions that influence later actions.

**Why it happens:**  
Teams rely on model behavior rather than separating trusted instructions, user intent, untrusted data, and privileged sinks.

**Consequences:**
- Indirect prompt injection causes data exfiltration or unwanted actions.
- MCP tool descriptions or tool outputs poison behavior.
- Admin GUI sees the incident after the fact but cannot prove which untrusted content influenced the call.
- Guardrails become inconsistent across model providers.

**Warning signs / early detection:**
- Untrusted tool output is appended directly into the conversation with no label.
- Tool descriptions from plugins/MCP are accepted verbatim and exposed to models.
- Privileged tools can be called after reading arbitrary external content without approval.
- No distinction between trusted system/developer instructions and retrieved content.
- Prompt-injection tests are absent from CI.

**Prevention strategy:**
- Track trust labels in runtime events and context: `trusted_system`, `user_input`, `untrusted_tool_output`, `private_data`, etc.
- Treat all tool outputs, retrieved documents, MCP descriptions, and plugin-provided descriptions as untrusted unless signed/curated.
- Enforce policies before sensitive tool calls based on what the model has read in the run, not only on requested tool name.
- For v1, implement pragmatic gates:
  - deny/approval-required if untrusted external content is in context and tool has external side effects
  - block secrets/credentials from model context
  - allowlist tools per agent/session
  - scan tool outputs for obvious instruction-injection markers and log them
- Build a red-team test corpus with indirect prompt injection in files/web/MCP outputs.

**Phase should address:** Phase 5 / Policy & Security; minimal labels should start in Phase 1 event/context model so retrofitting is possible.

**Confidence:** MEDIUM-HIGH. OWASP LLM risks and recent agent hardening guidance consistently rank prompt injection/excessive agency/tool poisoning as central risks; exact technical defenses are evolving.

---

### Pitfall 8: Session/Run/Step state is modeled as UI history instead of the source of truth for execution recovery

**What goes wrong:**  
The platform stores chat messages and final results, but not enough structured execution state to resume, cancel, debug, audit, or replay a run. Tool calls, policy decisions, streamed model deltas, and plugin/MCP failures are scattered across logs.

**Why it happens:**  
Admin GUI needs “show run history,” so teams model persistence around display rather than runtime semantics.

**Consequences:**
- Cannot reliably cancel or determine run state after server restart.
- SSE reconnect cannot replay missing events.
- Auditors cannot reconstruct tool arguments/results/policy decisions.
- Model/provider retries duplicate side-effecting tools.
- Memory/compaction becomes unsafe because provenance is lost.

**Warning signs / early detection:**
- `Session` table contains only messages.
- Run status has only `RUNNING/SUCCESS/FAILED` with no step-level state.
- Tool call IDs are not stable/idempotent.
- SSE event IDs are not persisted.
- Policy decisions are logs, not structured records tied to steps.

**Prevention strategy:**
- Define an append-only event model early:
  - `RunCreated`, `StepStarted`, `ModelRequestStarted`, `ModelDelta`, `ToolCallProposed`, `PolicyDecisionRecorded`, `ToolCallStarted`, `ToolCallCompleted`, `ToolCallFailed`, `RunCancelled`, `RunFailed`, `RunCompleted`.
- Persist stable IDs: tenant, user, session, run, step, tool call, tool version, plugin version, MCP server ID.
- Separate:
  - event log for replay/audit
  - materialized views for Admin GUI
  - message/context state for model calls
- Make side-effecting tool calls idempotency-aware with call IDs and replay rules.
- Support cancellation state that tools can observe through context/deadline tokens.

**Phase should address:** Phase 1 / Runtime Core and Phase 2 / Cloud Server Persistence/API/SSE. Delaying this creates rewrite risk.

**Confidence:** MEDIUM-HIGH. This is domain architecture reasoning grounded in the project requirements for sessions/runs/steps/audit/SSE.

---

### Pitfall 9: Streaming/SSE is treated as a transport detail, not part of the runtime contract

**What goes wrong:**  
The Cloud Server streams whatever the model emits directly to clients. Tool events, policy decisions, errors, cancellation, heartbeats, and reconnects are bolted on later. Admin GUI and future CLI/TUI cannot rely on stable event semantics.

**Why it happens:**  
Early demos stream tokens only. Real agent platforms stream structured execution events.

**Consequences:**
- Admin GUI cannot display accurate step/tool timelines.
- Clients break when provider streaming formats differ.
- Reconnect loses events or duplicates them.
- Cancellation races with in-flight model/tool calls.
- Future CLI/TUI protocol becomes incompatible with v1 APIs.

**Warning signs / early detection:**
- SSE event type is only `message` or raw provider delta.
- Event schema contains provider-specific fields at the top level.
- No event sequence number or persisted event ID.
- No heartbeat/keepalive strategy.
- No contract for terminal events and error semantics.

**Prevention strategy:**
- Define provider-neutral `AgentEvent` envelopes in core:
  - `eventId`, `sequence`, `timestamp`, `tenantId`, `sessionId`, `runId`, `stepId`, `type`, `payload`, `traceId`.
- Treat token deltas as one event type, not the whole stream.
- Persist or buffer events sufficiently for Admin GUI replay and SSE reconnect.
- Document terminal states and error payloads.
- Add compatibility tests with mock providers/tools producing slow streams, partial failures, cancellation, and reconnect.

**Phase should address:** Phase 1 event contract; Phase 2 Cloud Server API/SSE.

**Confidence:** MEDIUM. Based on agent platform architecture needs and project requirements; not tied to a single official source.

---

### Pitfall 10: Provider abstraction hides important model capability differences

**What goes wrong:**  
The platform defines a lowest-common-denominator `ChatModel.call(messages)` and assumes OpenAI-compatible APIs cover all needed behavior. Tool calling, streaming deltas, structured output, reasoning tokens, context limits, multimodal inputs, embeddings, usage accounting, and error/rate-limit semantics differ across providers.

**Why it happens:**  
OpenAI-compatible endpoints are convenient for v1, but “compatible” often means request shape compatibility, not identical semantics.

**Consequences:**
- Tool calling breaks or behaves inconsistently when adding Anthropic/Gemini/other providers.
- Cost/usage accounting is wrong.
- Provider errors are surfaced as generic failures; retries amplify rate-limit issues.
- Admin GUI cannot explain model capability or why a run cannot use a tool.
- Context compaction/memory logic is coupled to one provider’s token semantics.

**Warning signs / early detection:**
- Model provider interface has no capability descriptor.
- Tool support is a boolean rather than a structured capability model.
- Provider-specific response fields leak into runtime state.
- No normalized error taxonomy: auth, rate-limit, context-length, safety-block, transient, provider-bug.
- No provider contract tests.

**Prevention strategy:**
- Define provider capabilities explicitly:
  - streaming support
  - tool-calling mode and schema limits
  - structured output support
  - context window and max output
  - multimodal support
  - token/usage reporting quality
  - retryable error classes
- Keep OpenAI-compatible as first adapter, not the core abstraction.
- Normalize provider events into `AgentEvent`s and provider errors into platform errors.
- Add provider contract tests with fake adapters before adding more vendors.
- Surface model capability and selected provider in Admin GUI.

**Phase should address:** Phase 1 / Model Provider Abstraction and Phase 2 Admin GUI observability.

**Confidence:** MEDIUM. Strong practical pattern; specific provider behavior should be revalidated during stack/provider research.

---

### Pitfall 11: Admin GUI is read-only observability, not operational control

**What goes wrong:**  
The Admin GUI shows runs and logs but cannot disable a plugin, quarantine a tool, revoke a provider config, cancel runs, approve/reject gated actions, or inspect policy decisions. Operators can see unsafe behavior but cannot intervene.

**Why it happens:**  
“Minimal Admin GUI” is interpreted as a dashboard only.

**Consequences:**
- Production incidents require database edits or restarts.
- Dynamic plugin/MCP work becomes risky because operators cannot disable bad extensions quickly.
- Human-in-the-loop approvals need a separate future UI rewrite.
- Enterprise users see missing governance controls.

**Warning signs / early detection:**
- GUI screens only list runs/events.
- No backend APIs for plugin enable/disable, tool enable/disable, run cancel, approval decision.
- Policy decisions are not first-class objects.
- No RBAC for admin actions.

**Prevention strategy:**
- Define Admin GUI v1 around operational workflows, not charts:
  - view run timeline
  - cancel run
  - inspect tool call arguments/results/policy decision
  - approve/reject pending gated action
  - view plugin status and disable plugin/tool
  - view provider config health without revealing secrets
  - view audit records
- Implement admin actions through audited server APIs with RBAC.
- Make “disable tool/plugin” immediate for new runs and safe for in-flight runs.

**Phase should address:** Phase 2 / Cloud Server + Admin GUI MVP; Phase 5 for approval workflow hardening.

**Confidence:** MEDIUM. This is project-specific operational reasoning aligned with security/audit requirements.

---

### Pitfall 12: Observability starts at logs instead of traces, metrics, structured events, and audit records

**What goes wrong:**  
The platform writes logs for model calls and tool calls but cannot answer production questions: Which tenant is slow? Which plugin leaks memory? Which model/provider caused retries? Which tool was denied by policy? What did this run cost?

**Why it happens:**  
Observability is deferred until after runtime behavior is implemented.

**Consequences:**
- Debugging agent failures requires reading raw logs.
- Cost and latency regressions go unnoticed.
- Plugin/MCP reliability cannot be compared.
- Audit and observability diverge.

**Warning signs / early detection:**
- Logs contain free-form strings without run/tool/plugin IDs.
- No OpenTelemetry trace/span IDs in event envelopes.
- No metrics dimensions for tenant, provider, model, tool, plugin, MCP server, outcome.
- Tool audit and runtime event log are separate with no shared IDs.

**Prevention strategy:**
- Make IDs and trace context part of the core runtime context.
- Emit structured events for every model/tool/policy/plugin lifecycle action.
- Track core metrics from day one:
  - run duration and outcome
  - model latency/tokens/cost/error class
  - tool latency/error/deny/timeout/cancel
  - plugin load/enable/failure counts
  - MCP server health and auth failures
  - queue/concurrency saturation
- Align audit records with event IDs and trace IDs.
- Ensure Admin GUI consumes the same event/audit source as observability.

**Phase should address:** Phase 1 event model; Phase 2 Cloud observability; Phase 3/4 plugin and MCP metrics.

**Confidence:** MEDIUM-HIGH. Standard cloud platform practice plus project-specific audit/observability requirements.

---

## Moderate Pitfalls

### Pitfall 13: Versioning extension contracts too late

**What goes wrong:**  
Early plugins compile against unstable APIs; later changes break all extensions or force compatibility hacks.

**Warning signs:**
- Plugin API lives in the same module/package as runtime internals.
- No `apiVersion` in plugin descriptors.
- No semantic compatibility policy.

**Prevention:**
- Create separate `api` and `spi` modules.
- Require plugin descriptors to declare platform API compatibility range.
- Version serialized schemas for tools/events/policy decisions.
- Maintain compatibility tests using sample plugins from previous versions.

**Phase should address:** Phase 1 before any public/dynamic plugin work.

**Confidence:** HIGH.

---

### Pitfall 14: Letting plugins access Spring `ApplicationContext` directly

**What goes wrong:**  
Plugins become tightly coupled to host internals, bypass policy, and break when the server refactors.

**Warning signs:**
- Plugin interfaces accept `ApplicationContext` or arbitrary bean lookup.
- Plugins can fetch repositories, data sources, secret managers, or executor services.

**Prevention:**
- Expose narrow platform services: `ToolRegistration`, `SecretRefResolver`, `EventPublisher`, `PolicyClient`, `WorkspaceClient`.
- For Spring plugin integration, adapt Spring beans at the boundary rather than making core plugin API Spring-dependent.
- Enforce ArchUnit/module tests: core API must not depend on Spring.

**Phase should address:** Phase 1 / API boundary and Phase 2 / Spring integration.

**Confidence:** MEDIUM-HIGH.

---

### Pitfall 15: Treating remote MCP tool schemas/descriptions as trusted prompts

**What goes wrong:**  
MCP tool descriptions or parameters include malicious or overly broad instructions that influence the model.

**Warning signs:**
- Remote tool descriptions are injected unchanged into every model request.
- No approval/curation step for new MCP servers/tools.
- No per-agent MCP tool allowlist.

**Prevention:**
- Require MCP server registration/approval before use.
- Store discovered tools as untrusted metadata until reviewed or policy-approved.
- Use per-agent/session tool allowlists.
- Enforce allowlists both at prompt/tool-schema exposure time and execution time.

**Phase should address:** Phase 4 / MCP integration and Phase 5 / Policy.

**Confidence:** MEDIUM-HIGH from current agent hardening guidance on tool poisoning.

---

### Pitfall 16: No explicit backpressure/concurrency model for cloud runs

**What goes wrong:**  
Each run can recursively call tools and models, creating unbounded concurrent work. One tenant or bad plugin starves the server.

**Warning signs:**
- Agent loop uses common/global executors without tenant/run quotas.
- No max steps, max tool calls, max duration, or token budget per run.
- No queue visibility in Admin GUI.

**Prevention:**
- Define run budgets: max steps, wall-clock deadline, token budget, tool-call count, cost budget.
- Add tenant and provider concurrency limits.
- Isolate plugin/MCP execution pools from core server request threads.
- Surface queued/running/cancelled/timeout states.

**Phase should address:** Phase 2 / Cloud Server runtime controls; Phase 5 for policy-driven budgets.

**Confidence:** MEDIUM.

---

### Pitfall 17: Secret management leaks into prompts, logs, and plugin configuration

**What goes wrong:**  
Provider API keys, MCP tokens, database credentials, or business API tokens are stored in plain config, passed into tool arguments, logged, or visible in Admin GUI.

**Warning signs:**
- Tool arguments contain bearer tokens/API keys.
- Admin GUI displays raw provider/plugin config.
- Secrets are represented as strings in runtime context instead of references.
- No output scanning/redaction.

**Prevention:**
- Represent secrets as `SecretRef`, resolved only at execution boundary.
- Redact secret-like values in logs/events/Admin GUI.
- Encrypt token/secret storage.
- Never include secrets in prompts or model-visible tool output.
- Add CI tests for known secret patterns in event/audit serialization.

**Phase should address:** Phase 2 / Cloud Server config; Phase 4 MCP tokens; Phase 5 security hardening.

**Confidence:** HIGH.

---

### Pitfall 18: Workspace/filesystem assumptions inherited from local coding agents

**What goes wrong:**  
The Java cloud platform copies local CLI/coding-agent assumptions: unrestricted filesystem access, shell execution, repo-local state, and single-user workspace semantics.

**Warning signs:**
- Tool APIs accept raw filesystem paths without workspace resolution.
- File/shell tools are enabled by default.
- Session state assumes local working directory.
- No tenant/workspace boundary in tool context.

**Prevention:**
- Model `Workspace` as a cloud resource with tenant/session ownership.
- All file/shell/network tools must require workspace-scoped policy.
- Disable shell/file mutation tools by default in v1 unless sandboxed.
- Treat pi TypeScript CLI as conceptual reference only, not architecture source.

**Phase should address:** Phase 1 domain model and Phase 5 tool sandbox.

**Confidence:** MEDIUM-HIGH from project constraints.

---

## Minor Pitfalls

### Pitfall 19: Over-abstracting memory before run/event correctness

**What goes wrong:**  
Memory is added before the platform can track provenance, sensitivity, tenancy, and deletion. The agent later retrieves stale or unauthorized memories.

**Prevention:**
- Defer advanced memory.
- In v1, store run/session history with provenance and tenant/user ownership.
- Require memory entries to carry source, sensitivity, retention, and deletion metadata before retrieval is generalized.

**Phase should address:** Defer beyond core; only minimal session history in Phase 1/2.

**Confidence:** MEDIUM.

---

### Pitfall 20: Plugin/MCP health is binary instead of diagnosable

**What goes wrong:**  
Admin GUI shows “enabled” even when auth expired, schema validation failed, or provider endpoint is down.

**Prevention:**
- Track health states: unknown, validating, healthy, degraded, auth_failed, schema_invalid, disabled, failed.
- Store last check time, failure reason, and remediation hint.
- Run startup validation and periodic health checks.

**Phase should address:** Phase 2 Admin GUI; Phase 3/4 plugin/MCP integration.

**Confidence:** MEDIUM.

---

### Pitfall 21: No compatibility boundary between Admin GUI and future CLI/TUI

**What goes wrong:**  
Admin GUI calls internal endpoints directly; future CLI/TUI needs different event semantics and breaks server API assumptions.

**Prevention:**
- Define public Cloud Server REST/SSE API independently of Admin GUI.
- Admin GUI should be a client of the same stable API.
- Version API/event schemas.

**Phase should address:** Phase 2 / API contract.

**Confidence:** MEDIUM.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|----------------|------------|
| Phase 1: Runtime contracts | Extension mechanisms drive core abstractions | Define normalized descriptors, lifecycle, event envelope, tool gateway before adapters |
| Phase 1: Agent Loop | Tool execution wired directly into loop | Agent Loop proposes tool calls; ToolExecutionGateway executes/enforces/audits |
| Phase 1: Model provider | OpenAI-compatible assumptions become core | Capability descriptors, normalized provider events/errors, contract tests |
| Phase 1: Session/Run/Step | State is chat-history only | Append-only structured execution events with stable IDs |
| Phase 2: Cloud Server REST/SSE | Token streaming only | Provider-neutral event stream with sequence IDs, terminal states, reconnect semantics |
| Phase 2: Admin GUI | Dashboard without controls | Include cancel, inspect policy, disable tool/plugin, approve/reject gated actions |
| Phase 2: Auth/context | Agent inherits user/admin authority | Effective authority = tenant ∩ user ∩ agent ∩ tool ∩ session/workspace ∩ approval |
| Phase 3: Java SPI/Spring | SPI/Spring lifecycle mismatch | Adapter pattern; conformance tests; no Spring dependency in core API |
| Phase 3: Dynamic plugins | Classpath scanning and no unload | One classloader per plugin, explicit descriptor, API parent loader, close/unload tests |
| Phase 3: Plugin security | Classloader called sandbox | Trust tiers; untrusted code out-of-process/containerized |
| Phase 4: MCP | Token passthrough/confused deputy | Follow MCP auth spec: resource indicators, audience validation, no passthrough, HTTPS/PKCE |
| Phase 4: MCP tool metadata | Tool poisoning | Registration approval, metadata treated untrusted, per-agent allowlists |
| Phase 5: Policy/security | Policy encoded in prompts | Deterministic policy engine before tool execution; model never decides authorization |
| Phase 5: Observability/audit | Logs cannot reconstruct incident | Structured audit from gateway with shared IDs, trace IDs, policy context |
| Phase 6+: Memory/Workspace | Cross-tenant/provenance leaks | Defer advanced memory; require provenance, sensitivity, retention, and workspace boundaries |

---

## Recommended Roadmap Gates

Use these as “do not proceed until” checks.

### Gate A — Before any extension mechanism ships
- Core extension API is Spring-free.
- Every capability normalizes to descriptor + executor + lifecycle state.
- ToolExecutionGateway exists and is the only execution path.
- Conformance tests exist for policy deny, timeout, cancellation, audit, and metrics.

### Gate B — Before dynamic plugin JAR support ships
- Plugin API module separated from runtime internals.
- Plugin descriptor has platform compatibility version and permissions.
- One-plugin/two-conflicting-dependencies integration test passes.
- Plugin unload test releases classloader references and stops plugin resources.
- Security docs state same-JVM plugins are trusted unless out-of-process sandbox is used.

### Gate C — Before MCP support is enabled for multi-tenant/protected tools
- MCP credentials are encrypted and never model-visible.
- Token passthrough is rejected by design.
- `WWW-Authenticate`/metadata discovery behavior is implemented for protected resources.
- Resource/audience validation and `resource` parameter behavior are tested.
- SSRF controls exist for metadata/OAuth URL fetching.

### Gate D — Before destructive or external side-effect tools are enabled
- Effective authority intersection is implemented.
- Approval workflow exists or tools are denied by default.
- Audit records include user, tenant, agent, run, step, tool, arguments summary, resource, policy decision, result, and trace ID.
- Prompt-injection tests cover untrusted tool output before privileged calls.

---

## Source Notes and Confidence

### Official / High-confidence sources
- Oracle Java SE 25 `ServiceLoader` API docs: providers are lazy/cached, discovery is classloader-specific, `reload()` clears provider cache, instances are not thread-safe, provider configuration errors throw `ServiceConfigurationError`, and `ServiceLoader.load(service)` uses the thread context classloader.  
  URL: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ServiceLoader.html
- Spring Boot official docs via Context7: Java does not natively support nested JAR loading; Spring Boot executable JARs use a special nested structure; launched applications should use `Thread.getContextClassLoader()` because `ClassLoader.getSystemClassLoader()` fails for nested JAR classes.  
  URL: https://github.com/spring-projects/spring-boot/tree/main/documentation/spring-boot-docs
- MCP Authorization specification 2025-06-18: HTTP authorization should conform to the spec; MCP servers must implement protected resource metadata when auth is supported; access tokens must be in `Authorization` header, not query strings; tokens must be validated for intended audience/resource; clients must use resource indicators; PKCE/HTTPS/security considerations apply; token passthrough is forbidden.  
  URL: https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization
- OWASP Top 10 for LLMs and Gen AI Apps 2025 landing page (current risk taxonomy reference).  
  URL: https://genai.owasp.org/llm-top-10/

### Recent ecosystem / Medium-confidence sources
- Adevinta, “Java plugins with isolating class loaders” (2024): classloader isolation helps dependency conflicts; plugins still must be trusted and can consume JVM resources; memory use increases per classloader.  
  URL: https://adevinta.com/techblog/java-plugins-with-isolating-class-loaders/
- Nicolas Fränkel, “Rediscovering Java ServiceLoader” (2026): `ServiceLoader` is useful at library/module boundaries; it lacks dependency injection/lifecycle/ordering and should be bridged carefully to Spring.  
  URL: https://blog.frankel.ch/rediscovering-java-serviceloader/
- WorkOS, “The 2026 AI agent auth checklist” (2026): agent authority should be scoped/intersected; authorization failures should stop; audit should capture full execution context.  
  URL: https://workos.com/blog/ai-agent-auth-checklist
- Microsoft Agent Framework FIDES announcement (2026): emerging pattern for information-flow labels and deterministic policy before sensitive tool execution.  
  URL: https://devblogs.microsoft.com/agent-framework/fides/

### Gaps / Need phase-specific validation
- Exact Java plugin framework choice, if any (PF4J, custom classloader, JPMS layers, OSGi) was not decided in this pitfalls dimension. A dedicated stack/comparison phase should validate current maintenance, Spring Boot integration, and unload behavior.
- Exact policy engine choice (OPA/Cedar/CASL-like custom/Java rules) needs separate stack research.
- Exact observability stack and event store persistence model need architecture/stack research.
- MCP Java SDK maturity and Spring integration need current stack research before implementation.
