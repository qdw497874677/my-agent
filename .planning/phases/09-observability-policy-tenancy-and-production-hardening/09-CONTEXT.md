# Phase 9: Observability, Policy, Tenancy, and Production Hardening - Context

**Gathered:** 2026-06-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 9 completes v1 production-readiness around OPS-01: structured logs, metrics, and OpenTelemetry-compatible traces/spans for run, model, tool, MCP, plugin, and policy lifecycles. It also surfaces operational metrics in Admin Governance, makes trace/run/session correlation consistent across API/SSE/logs/audit/traces, documents production hardening guidance, and adds regression coverage for critical safety and observability paths.

This phase does **not** implement full SaaS tenant/RBAC/billing, a full policy product, a production sandbox for untrusted code, a plugin marketplace, a complete KMS/vault subsystem, or a general APM/BI metrics explorer. Those remain future product capabilities; Phase 9 should provide production-ready hooks, defaults, documentation, and verification without breaking existing COLA/public API boundaries.

</domain>

<decisions>
## Implementation Decisions

### Telemetry Architecture
- **D-01:** Add a dedicated observability infrastructure module, e.g. `pi-agent-infrastructure-observability`, as the home for Micrometer, OpenTelemetry, structured logging helpers, telemetry redaction, and telemetry test utilities. Domain/App/client contracts must not leak OTel, Micrometer, Logback, or exporter types.
- **D-02:** Use an **event + decorator** instrumentation model. Runtime lifecycle metrics can be derived from `RunEvent`/fanout streams, while timing/error spans should wrap key chokepoints such as run dispatch, model calls, tool gateway execution, MCP invocation, plugin discovery/lifecycle actions, and policy evaluation.
- **D-03:** Treat Micrometer metrics, OpenTelemetry-compatible traces/spans, and structured JSON logs as the Phase 9 production baseline. Prometheus and OTLP export should be configuration-driven; default verification should not require real external backends.
- **D-04:** Prefer low-intrusion integration points already present in the codebase: `PersistingEventSink` / `RunEventFanout`, `DefaultRunDispatcher`, `DefaultToolExecutionGateway`, OpenAI-compatible model adapter boundaries, MCP registry/executor bindings, plugin discovery/governance services, and `DefaultToolPolicyEvaluator`.
- **D-05:** Add architecture gates proving observability implementation dependencies stay out of Domain/App/client/public extension APIs and remain isolated to infrastructure/adapter/test surfaces.

### Correlation and Trace Context
- **D-06:** Normalize Pi `traceId` to W3C-compatible 32-character lowercase hex going forward, so platform IDs and OpenTelemetry trace IDs can align cleanly.
- **D-07:** Perform an explicit historical trace-id migration rather than leaving old UUID-form trace IDs indefinitely. Downstream planning must treat this as a deliberate Flyway/data-compatibility task: convert existing UUID trace IDs deterministically where possible, preserve `correlationId` for human/debug continuity, and update tests/API expectations carefully.
- **D-08:** `correlationId` remains a separate operator/business correlation field even after `traceId` becomes W3C-shaped. API responses, SSE events, audit records, logs, and spans should continue to expose both where safe.
- **D-09:** Worker-thread, model-call, tool-call, MCP, and plugin contexts must restore MDC/span attributes **explicitly** from `RequestContext`, `RunEvent`, `ToolExecutionRequest`, or equivalent context records. Do not rely on request-thread MDC inheritance, virtual-thread inheritance, or opaque OTel auto-instrumentation as the primary propagation mechanism.
- **D-10:** Span attributes and metric tags must use the same redaction discipline as events/audit/Admin UI. Raw secrets, auth headers, unredacted tool args/results, provider payloads, MCP remote error bodies, and plugin-provided sensitive metadata must not appear in telemetry.

### Admin Operational Metrics
- **D-11:** Admin Governance should provide an **operations summary plus detail entry points**, not a full APM/BI explorer. The overview should show key health, count, latency, error, and recent-warning summaries; detail areas should cover runs, providers/models, tools/policy, MCP servers, and plugins.
- **D-12:** Phase 9 Admin metrics should cover the full critical path: run state/latency/error summaries, model call metrics, tool execution/policy decision metrics, MCP health/invocation metrics, plugin lifecycle/health metrics, error rates, and recent operational warning summaries.
- **D-13:** Admin metrics must be exposed through App/read-model/public DTO boundaries. Vaadin should consume public Admin DTOs/APIs only; it must not directly query Micrometer internals, private runtime services, or database tables.
- **D-14:** Actuator remains an operational endpoint, but the product Admin UI should translate raw metrics into Pi-specific operational read models that use existing source/capability/provenance/health language from extension, MCP, and plugin governance.

### Production Hardening Documentation
- **D-15:** Produce an operations-oriented Phase 9 hardening document, not a speculative future-system design. It should give actionable configuration examples, defaults, risk boundaries, extension points, verification commands, and explicit deferrals.
- **D-16:** The hardening documentation must cover all production topics named by the Phase 9 success criteria and roadmap research note: observability deployment, Actuator security, Prometheus/OTLP configuration, structured JSON logs, secrets/KMS/vault integration hooks, policy engine extension, tenant/RBAC hooks, sandbox strategy, retention/redaction, plugin/MCP/provider operational safety, and deployment guidance.
- **D-17:** Policy, tenancy/RBAC, secrets/KMS, and sandbox sections should define v1 hooks and safe defaults, but must not imply that v1 ships full SaaS RBAC/billing, full KMS management, or an untrusted-code sandbox.
- **D-18:** Documentation should explicitly distinguish trusted controlled plugin/MCP extension operation from untrusted execution. PF4J classloader isolation and configured MCP stdio remain lifecycle/dependency/trusted integration mechanisms, not security sandboxes.

### Regression and Verification Scope
- **D-19:** Phase 9 validation should be a full critical-path regression gate: telemetry unit/integration tests, no-key product-path E2E coverage for run/model/tool/MCP/plugin/policy telemetry, correlation propagation tests, telemetry redaction tests, Admin metrics API/UI smoke tests, and ArchUnit boundary tests.
- **D-20:** Default tests must use fake/in-memory telemetry backends such as test `MeterRegistry`, in-memory span exporter/collector, and log capture. Real OTel Collector, Prometheus, Docker/Testcontainers, or external observability stacks may be optional smoke/profile tests, but must not be required for default local/CI verification.
- **D-21:** Telemetry redaction becomes part of the security surface. Extend existing secret-absence patterns so span attributes, metric tags, structured logs, Admin metric DTOs, events, audit records, and persisted payloads are checked for fake sensitive values.
- **D-22:** Regression coverage must include critical existing paths named in Phase 9 success criteria: policy decisions, audit records, cancellation, timeout, extension/plugin/MCP behavior, event ordering, and trace/run/session correlation.

### Folded Todos
- No pending todos matched Phase 9 scope.

### the agent's Discretion
- Exact module/package/class names are planner discretion if they preserve the dedicated observability boundary and COLA dependency rules.
- Exact metric names, span names, tag keys, and Admin DTO record names are planner/researcher discretion, but they must be stable, documented, low-cardinality, redacted, and aligned with Pi's run/model/tool/MCP/plugin/policy vocabulary.
- Exact historical trace-id migration mechanics are planner/researcher discretion, but they must be explicit, tested, and compatible with existing API/SSE/audit/query paths.
- Exact UI layout for Admin metrics is designer/planner discretion, provided it remains an operational summary with details, not a full metrics explorer.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 9 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 9 — Phase goal, OPS-01 mapping, success criteria for telemetry, Admin metrics, correlation, production docs, and regression tests.
- `.planning/REQUIREMENTS.md` §Observability, Policy, and Security — OPS-01 through OPS-06 status, especially pending OPS-01 and already-validated audit/policy/tenant/redaction/testkit requirements.
- `.planning/REQUIREMENTS.md` §End-to-End Verification — E2E requirements already validated in earlier phases and regression expectations that Phase 9 must preserve.
- `.planning/PROJECT.md` — Project vision, Java-first/COLA/cloud-safety/extensibility/verification constraints, and Phase 8 current state leading into Phase 9.
- `.planning/STATE.md` — Current Phase 9 planning state and accumulated implementation decisions, including open hardening questions.

### Prior Phase Contracts and Decisions
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md` — RunEvent envelope, trace/correlation ID context, event taxonomy, framework-free Domain, and strict architecture gates.
- `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-CONTEXT.md` — Session-centric API, persist-then-emit SSE, dev-auth/JWT-ready security, tenant/user/trace/correlation placeholders, persistence/audit boundaries, and public DTO rules.
- `.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/03-CONTEXT.md` — Model/provider streaming, resilience, CredentialRef/SecretRef, provider error normalization, and no-key provider test patterns.
- `.planning/phases/04-governed-tool-registry-workspace-and-invocation-pipeline/04-CONTEXT.md` — Mandatory ToolExecutionGateway, policy/preview/approval/sandbox semantics, tool lifecycle events, audit, payload limits, and redaction requirements.
- `.planning/phases/05-agent-web-console-and-runtime-cockpit/05-CONTEXT.md` — Admin Governance public API/UI boundary, read-only governance surfaces, Playwright/no-key browser E2E patterns, and UI separation.
- `.planning/phases/06-java-extension-surface-spi-and-spring/06-CONTEXT.md` — Extension source/capability/provenance/health/governance language and event listener extension capability.
- `.planning/phases/07-mcp-client-bridge-and-governed-remote-tools/07-CONTEXT.md` — MCP health/invocation/error governance, static credential refs, remote tool redaction, and gateway-only execution.
- `.planning/phases/08-controlled-dynamic-plugin-jars/08-CONTEXT.md` — Plugin lifecycle/disable/quarantine/audit/redaction decisions and explicit deferral of plugin lifecycle telemetry to Phase 9.

### Existing Contract Documents
- `docs/phase-01-domain-contracts.md` — Runtime, event, session, workspace, and architecture boundary contract index.
- `docs/phase-02-cloud-server-api.md` — REST/SSE/session/run/event/audit API contract index and public DTO boundary.
- `docs/phase-03-model-provider-contracts.md` — Provider/model/credential/resilience contract index.
- `docs/phase-04-governed-tool-contracts.md` — Tool descriptor, gateway, policy, lifecycle, audit, redaction, and future source consumption guidance.
- `docs/phase-05-web-console.md` — Web Console/Admin public API boundary and UI/test patterns.
- `docs/phase-06-extension-surface.md` — Extension source/capability/provenance/health/governance language and event-listener extension notes.
- `docs/phase-07-mcp-client-bridge.md` — MCP governance, transport/auth, remote tool execution, health, and redaction contract.
- `docs/phase-08-controlled-dynamic-plugin-jars.md` — Plugin controlled-directory operation, lifecycle, disable/quarantine, non-sandbox warning, and Phase 9 telemetry deferral.

### Research and Stack Guidance
- `.planning/research/STACK.md` §Observability and Operations — Actuator, Micrometer, OpenTelemetry, collector, structured logging guidance.
- `.planning/research/STACK.md` §Resilience, Safety, and Governance — Resilience4j, policy engine, rate limiting, and governance guidance.
- `.planning/research/STACK.md` §Packaging and Deployment — JVM container, health/readiness, config/secrets, and production deployment guidance.
- `.planning/research/ARCHITECTURE.md` — Module guidance including the intended `pi-agent-infrastructure-observability` responsibility if present.

### External Documentation to Research During Planning
- Micrometer/Spring Boot 3.5 observability documentation — custom meters, observation conventions, actuator metrics exposure, Prometheus registry, and security.
- OpenTelemetry Java documentation — W3C trace ID requirements, span/exporter configuration, in-memory test exporters, and OTLP setup.
- Logback JSON encoder / Spring Boot structured logging guidance — MDC inclusion and JSON log configuration.
- Spring Security resource server and Actuator security docs — authenticated metrics/prometheus endpoints and Admin route authorization.
- Resilience4j documentation — rate limiter/bulkhead/time limiter hooks for provider/tool/MCP calls and future tenant quotas.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/correlation/CorrelationFilter.java` — Current request correlation/MDC setup point. It normalizes request IDs, sets response headers, attaches request attributes, and populates MDC for request-thread logs.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/context/RequestContext.java`, `CorrelationContext.java`, and `SecurityPrincipalContext.java` — App-layer context records carrying tenant/user/authority/trace/correlation/causation values.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/common/PlatformIds.java` — Typed tenant/user/session/run/workspace/trace/correlation/causation IDs. Phase 9 must update/validate trace ID shape here and downstream mappings.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEvent.java` — Canonical event envelope with tenant/user/session/run/step/workspace/trace/correlation/causation context and redaction metadata.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/EventSink.java` and `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/event/PersistingEventSink.java` — Persist-then-emit chokepoint for run/model/tool lifecycle events.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/event/RunEventFanout.java` — Existing fanout seam suitable for adding telemetry event consumers without changing Domain.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` — Run execution lifecycle chokepoint with cancellation/timeout/terminal handling and audit hooks.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java` — Tool execution chokepoint for validation, policy, preview, approval/sandbox, timeout/cancellation, execution, redaction, audit, and lifecycle events.
- `pi-agent-infrastructure-model-openai` — OpenAI-compatible model adapter/resilience boundary where model-call spans/metrics should be added without leaking provider SDK types.
- `pi-agent-infrastructure-mcp` — MCP registry/invocation/governance classes where MCP discovery/health/invocation telemetry can be attached.
- `pi-agent-infrastructure-plugin` — Plugin discovery/lifecycle/governance classes where plugin lifecycle telemetry should be added.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/EventListenerExtensionCapability.java` — Existing extension hook for event listeners; useful context for future telemetry/event-sink extensions.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java`, `DefaultGovernanceQueryService`, and Vaadin Admin views — Existing Admin Governance API/UI surfaces to extend with operational metrics read models.
- `pi-agent-adapter-web/pom.xml` and `application.yml` — Spring Boot Actuator is present and currently exposes health/info/metrics; Prometheus/OTLP and structured logging are not yet configured.

### Established Patterns
- COLA boundaries are strict: Domain stays framework-free; App owns plain Java ports/use cases; Infrastructure implements external systems; Adapter Web wires Spring/Vaadin/public API mapping.
- Public API/UI contracts live in `pi-agent-client` and adapter DTO mapping. Vaadin consumes public REST/SSE/read-model APIs rather than runtime/private database access.
- Persist-then-emit is the event consistency model. Telemetry must not publish a conflicting live-only lifecycle truth.
- Redaction is a first-class safety boundary for events, audit, Admin UI, provider errors, MCP errors, plugin diagnostics, and tool payloads. Telemetry becomes part of that redaction surface.
- No-key deterministic E2E and fake/in-memory infrastructure are the standard default verification style; external services are optional smoke paths.

### Integration Points
- Add `pi-agent-infrastructure-observability` to the Maven reactor and keep Micrometer/OTel/logging implementation dependencies there or in Adapter Web composition/test surfaces.
- Extend Adapter Web configuration with observability beans, Actuator/Prometheus/OTLP properties, JSON logging configuration, and authenticated endpoint policy.
- Decorate `AgentRuntime`/run dispatcher, `StreamingModelClient`, `ToolExecutionGateway`, MCP executor/registry, plugin discovery/governance, and policy evaluator with telemetry wrappers where appropriate.
- Add a telemetry-aware `RunEventFanout` or composite fanout so event-derived metrics align with persisted RunEvent lifecycle.
- Extend Admin Governance App/client DTOs with operational metrics summaries and detail views for runs, providers/models, tools/policy, MCP, and plugins.
- Add Flyway/data migration and tests for historical trace IDs if current persisted trace IDs are UUID-shaped.
- Extend existing security/redaction E2E patterns to telemetry attributes, metric tags, structured logs, Admin metric DTOs, events, and audits.

</code_context>

<specifics>
## Specific Ideas

- User chose to discuss all Phase 9 gray areas and selected the production-oriented recommended direction for telemetry architecture, Admin metrics, hardening documentation, and regression coverage.
- User explicitly chose to migrate Pi `traceId` to W3C-compatible format and to perform historical trace-id migration rather than only adding a parallel OTel ID.
- User wants Admin metrics to cover the full critical path while staying an operations summary/details experience, not a general metrics explorer.
- User wants production hardening documentation to cover every major production topic with actionable guidance and extension points.
- Default OPS/telemetry E2E must remain no-key/no-external-backend; real OTel Collector/Prometheus/Docker validation is optional.

</specifics>

<deferred>
## Deferred Ideas

- Full SaaS tenant/RBAC/billing/quota product — deferred beyond v1; Phase 9 documents hooks and safe defaults only.
- Full KMS/vault/secret-rotation management product — deferred; Phase 9 documents SecretResolver/KMS extension strategy and config examples.
- Production sandbox for untrusted code/plugins/MCP stdio/general shell — deferred; Phase 9 documents sandbox strategy and current non-sandbox boundaries.
- Full APM/BI metrics explorer with arbitrary querying, charting, dashboards, and long-range analytics — deferred; Admin gets operational summaries and detail entry points.
- Plugin marketplace, upload/install/upgrade/delete flows, and untrusted plugin execution — remain deferred from Phase 8.

</deferred>

---

*Phase: 09-observability-policy-tenancy-and-production-hardening*
*Context gathered: 2026-06-19*
