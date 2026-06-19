# Phase 09 Production Hardening Guide

Phase 09 closes `OPS-01` for v1 by making Pi observable and operable without changing the platform's trust boundaries. This guide is operations-oriented: it documents the shipped telemetry hooks, safe defaults, production configuration, verification commands, and explicit deferrals for observability, secrets, policy, tenancy, sandboxing, retention, and deployment.

## Observability deployment

Pi emits structured logs, Micrometer metrics, and OpenTelemetry-compatible spans around the critical runtime path: run events/dispatch, model calls, governed tool execution, policy decisions, MCP discovery/invocation, plugin lifecycle, and Admin operations summaries. The implementation lives behind `pi-agent-infrastructure-observability`; Domain, App, client DTOs, extension API, MCP contracts, and plugin contracts do not expose Micrometer, OpenTelemetry, or Logback types.

Recommended deployment pattern:

1. Run the Cloud Server with Java 21 and authenticated Admin/Actuator access.
2. Scrape `/actuator/prometheus` from a private Prometheus or collector network.
3. Export OTLP metrics/logs/traces only by explicit endpoint configuration.
4. Use Admin Governance operations summaries for Pi-specific health, not as a raw APM explorer.
5. Alert on low-cardinality Pi metrics and terminal event statuses, not raw prompts, user text, provider bodies, or tool payloads.

## Actuator security

Only health/info should be public in normal deployments. Metrics and Prometheus are operational endpoints and must be protected by the same authenticated server boundary used for Admin Governance.

Example Spring configuration:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
```

Security expectations:

- `/actuator/health` and `/actuator/info` may be available to load balancers.
- `/actuator/metrics` and `/actuator/prometheus` require authenticated operational access.
- Do not expose actuator endpoints directly to the public Internet.
- Keep Prometheus scraping credentials and network policy outside application logs.

## Prometheus and OTLP configuration

Prometheus scraping is pull-based through `/actuator/prometheus`. OTLP export is push-based and disabled unless endpoints are configured.

Example environment and YAML:

```bash
export PI_OTLP_METRICS_URL="https://otel-collector.example.internal:4318/v1/metrics"
export PI_OTLP_LOGS_URL="https://otel-collector.example.internal:4318/v1/logs"
```

```yaml
management:
  prometheus:
    metrics:
      export:
        enabled: true
  otlp:
    metrics:
      export:
        enabled: true
        url: ${PI_OTLP_METRICS_URL:}
logging:
  structured:
    format:
      console: logstash
pi:
  observability:
    otlp:
      logs-url: ${PI_OTLP_LOGS_URL:}
```

Use the exact metric/section vocabulary validated by Phase 09 tests: runs, models, tools, policies, MCP, plugins, errors, and warnings. Never create labels from prompts, raw errors, tool arguments, tool output, request bodies, authorization headers, absolute plugin paths, or provider/MCP payloads.

## Structured JSON logs

Structured logs include safe MDC fields where present: `traceId`, `correlationId`, `tenantId`, `userId`, `sessionId`, and `runId`. Context is written explicitly from `RequestContext`/`RunEvent`/telemetry scopes instead of depending on thread inheritance.

Redaction rules treat values containing secret, password, authorization, bearer, api_key, apikey, or token markers as sensitive. Phase 09 regression tests inject `PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK` into telemetry surfaces and assert it is absent from metrics, context attributes, logs, operations DTOs, event fixtures, audit fixtures, and persisted-payload fixtures.

Production logging guidance:

- Prefer JSON logs for collector ingestion.
- Include trace/run/session/correlation fields for incident correlation.
- Keep raw prompts, tool arguments/results, provider payloads, MCP bodies, auth headers, and plugin metadata out of log messages.
- Treat debug logging in provider/MCP/plugin adapters as sensitive and disable it by default in production.

## Secrets, KMS, and vault hooks

v1 ships credential-reference boundaries and secret-resolution hooks; it does not ship full KMS/vault management. Provider, MCP, and extension/plugin configuration should carry refs such as `env:OPENAI_API_KEY`, `config:tenant-a/provider-key`, or future vault refs rather than raw values.

Recommended production pattern:

```yaml
pi:
  providers:
    openai-compatible:
      credential-ref: env:OPENAI_API_KEY
  mcp:
    servers:
      filesystem:
        auth:
          type: bearer-token-ref
          token-ref: env:MCP_FILESYSTEM_TOKEN
```

Secrets should be resolved only at the last responsible adapter boundary and immediately redacted from diagnostics. Future KMS/vault implementations should plug into the existing resolver ports without changing Domain/App models.

## Policy engine extension

All tool execution continues through `ToolExecutionGateway`, which invokes the App-layer `ToolPolicyEvaluator` before execution. The default policy supports allow, deny, require approval, require sandbox, and block semantics. Phase 09 telemetry records low-cardinality policy decisions and Admin operations summaries without exposing raw tool inputs or policy payloads.

Production operators should:

- Keep destructive and side-effectful tools approval-gated until tenant-specific policy is installed.
- Use plugin/MCP/provider allowlists for trusted integration sources.
- Record policy decisions in audit and review denied/approval-required trends through Admin operations.
- Extend `ToolPolicyEvaluator` for RBAC/ABAC/quota/compliance checks rather than bypassing the gateway.

## Tenant and RBAC hooks

Runtime contexts already carry tenant ID, user ID, session ID, run ID, workspace ID, trace ID, correlation ID, and causation ID. v1 uses these fields for isolation context, audit, events, logs, metrics, and Admin visibility, but it does not ship full SaaS RBAC, billing, tenant catalog administration, or quota enforcement.

Production deployments should:

- Put the Cloud Server behind OAuth2/JWT or equivalent enterprise auth.
- Map authenticated principals into `tenantId`, `userId`, and authorities.
- Add tenant-scoped policy and rate-limit evaluators through App/Infrastructure ports.
- Keep Admin Governance restricted to operator/admin roles.

## Sandbox strategy

The current local-temp workspace, allowlisted command gateway, PF4J plugin loading, and configured MCP stdio/HTTP integrations are governed/trusted integration mechanisms. They are **not a sandbox** for untrusted code.

Safe v1 posture:

- Do not enable unrestricted shell/file tools.
- Treat dynamic plugin JARs as trusted artifacts from an operator-controlled directory.
- Treat MCP servers as trusted configured servers with allowlists, credential refs, and network controls.
- Require approval or block side-effectful workspace actions by default.
- Introduce an out-of-process sandbox, container isolation, seccomp/AppArmor, egress controls, and per-tenant workspace storage before accepting untrusted code execution.

## Retention and redaction

Persisted events and audit records use redacted summaries by default. Telemetry surfaces are part of the same redaction boundary as Web Console and Admin Governance.

Retention recommendations:

- Define retention windows separately for run history, audit records, structured logs, metrics, traces, and plugin/MCP lifecycle records.
- Keep audit records longer than high-volume telemetry when compliance requires it.
- Prefer payload summaries and truncation metadata over raw input/output persistence.
- Validate secret absence with the Phase 09 regression gate before production releases.
- Do not use metrics tags or span attributes for high-cardinality or sensitive values.

## Provider, MCP, and plugin operational safety

Provider adapters, MCP tools, and plugin capabilities must remain behind Pi-owned registries and gateway seams:

- Providers: use `provider:model` refs, credential refs, resilience wrappers, timeout/cancellation, and sanitized error mapping.
- MCP: configure trusted servers only; use static credential refs; expose discovery/invocation health through Admin MCP governance; execute remote tools only through `ToolExecutionGateway`.
- Plugins: load trusted JARs from the controlled directory; use allowlist/selected settings; disable/quarantine unhealthy plugins; remember PF4J lifecycle isolation is not a sandbox.
- Admin operations: inspect health and warning summaries through `/api/admin/governance/operations`; do not rely on raw Micrometer meters as product API.

## Deployment guidance

Baseline production deployment:

```yaml
spring:
  threads:
    virtual:
      enabled: true
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
pi:
  plugins:
    enabled: true
    directory: /opt/pi/plugins
    startup-discovery: true
    manual-refresh-enabled: true
    non-sandbox-warning-acknowledged: true
  mcp:
    servers: {}
```

Operational checklist:

- Use Java 21 and a JVM container first; native image is deferred.
- Configure readiness/liveness probes on actuator health endpoints.
- Store provider/MCP credentials in environment or external secret stores, not source files.
- Run database migrations before traffic.
- Restrict plugin directories to trusted operators and immutable deployment artifacts.
- Keep Admin and actuator endpoints on private/authenticated networks.

## Verification commands

Focused Phase 09 redaction and critical-path regression gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability,pi-agent-adapter-web -am -Dtest=Phase09TelemetryRedactionRegressionTest,Phase09CriticalPathRegressionTest test
```

Final no-key Phase 09 smoke gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-domain,pi-agent-infrastructure,pi-agent-infrastructure-observability,pi-agent-infrastructure-model-openai,pi-agent-infrastructure-mcp,pi-agent-infrastructure-plugin,pi-agent-adapter-web -am -Dtest=PlatformIdsTest,TraceIdMigrationTest,PiTelemetryArchitectureTest,RunEventTelemetrySinkTest,TelemetryToolExecutionGatewayTest,TelemetryToolPolicyEvaluatorTest,TelemetryStreamingModelClientTest,TelemetryMcpTelemetryTest,TelemetryPluginGovernanceCatalogTest,ObservabilityConfigurationTest,ActuatorSecurityTest,StructuredLoggingRedactionTest,MicrometerOperationsMetricsReaderTest,AdminOperationsControllerTest,AdminOperationsViewTest,Phase09TelemetryRedactionRegressionTest,Phase09CriticalPathRegressionTest test
```

Documentation/traceability smoke:

```bash
grep -q "## Observability deployment" docs/phase-09-production-hardening.md \
  && grep -q "## Actuator security" docs/phase-09-production-hardening.md \
  && grep -q "PI_OTLP_METRICS_URL" docs/phase-09-production-hardening.md \
  && grep -q "not a sandbox" docs/phase-09-production-hardening.md \
  && grep -q -- "- \[x\] \*\*OPS-01\*\*" .planning/REQUIREMENTS.md \
  && grep -q "09-09-PLAN.md" .planning/ROADMAP.md
```

## Explicit deferrals

Phase 09 intentionally does not claim the following as shipped:

- Full SaaS tenant/RBAC/billing/quota product.
- Full KMS/vault/secret-rotation management product.
- Untrusted-code sandbox for plugins, MCP stdio servers, shell commands, or Coding Agent workspaces.
- Plugin marketplace, upload/install/delete/upgrade/review/signing workflows.
- Full APM/BI metrics explorer with arbitrary queries, long-range dashboards, or chart builders.
- Native-image production packaging.
- Broad built-in provider catalog beyond the established OpenAI-compatible adapter boundary.

These capabilities can build on the v1 hooks documented above, but must not bypass the existing Workspace, Policy, Audit, Telemetry, Registry, and Gateway boundaries.
