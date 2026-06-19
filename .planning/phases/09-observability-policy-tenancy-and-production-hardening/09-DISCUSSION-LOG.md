# Phase 9: Observability, Policy, Tenancy, and Production Hardening - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-19
**Phase:** 09-observability-policy-tenancy-and-production-hardening
**Areas discussed:** Telemetry Architecture, Correlation Strategy, Admin Operational Metrics, Production Hardening Documentation, Regression Coverage

---

## Gray Area Selection

The user selected all proposed Phase 9 gray areas:

- Telemetry Architecture
- Correlation Strategy
- Admin Operational Metrics
- Production Hardening Documentation
- Regression Coverage

---

## Telemetry Architecture

### Observability code boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Independent module | Add `pi-agent-infrastructure-observability`, centralize Micrometer/OTel/logging implementation, keep Domain/App isolated. | ✓ |
| Adapter centralized | Put most observability configuration/implementation in `pi-agent-adapter-web`; smaller change but weaker boundary. | |
| Distributed local instrumentation | Directly instrument Run/Tool/MCP/Plugin/Provider modules; direct but likely to fragment boundaries. | |

**User's choice:** Independent module.
**Notes:** This locks a dedicated observability infrastructure boundary.

### Instrumentation pattern

| Option | Description | Selected |
|--------|-------------|----------|
| Event + decorator | Use events for lifecycle metrics and decorators for timing/error spans around key calls. | ✓ |
| Events only | Derive most telemetry from RunEvent/Audit streams. | |
| Decorators only | Wrap key calls directly and maintain lifecycle metrics separately. | |

**User's choice:** Event + decorator.

### Telemetry outputs

| Option | Description | Selected |
|--------|-------------|----------|
| Micrometer + OTel + JSON logs | Use Micrometer metrics, OpenTelemetry traces, and structured JSON logs as v1 production baseline. | ✓ |
| Metrics + logs first | Defer full OTel traces. | |
| OTel first | Prioritize traces while simplifying metrics/logs. | |

**User's choice:** Micrometer + OTel + JSON logs.

---

## Correlation Strategy

### Pi traceId and OpenTelemetry traceId

| Option | Description | Selected |
|--------|-------------|----------|
| Dual-track correlation | Keep Pi traceId/correlationId as business IDs and OTel traceId separately. | |
| Migrate to W3C | Make Pi traceId W3C 32hex-compatible. | ✓ |
| Pi ID only | Keep using Pi traceId as-is for OTel. | |

**User's choice:** Migrate to W3C.
**Notes:** User wants trace ID alignment rather than parallel IDs.

### W3C traceId migration strategy

| Option | Description | Selected |
|--------|-------------|----------|
| New writes W3C, old compatible | New requests/events use 32hex; old UUID values are accepted/displayed. | |
| Full historical migration | Migrate/convert historical DB trace IDs too. | ✓ |
| Add otelTraceId only | Keep existing Pi traceId and add a new OTel field. | |

**User's choice:** Full historical migration.
**Notes:** Agent flagged risk that UUID → W3C is not semantically equivalent. The decision remains to do explicit historical migration with careful Flyway/data compatibility and tests.

### Async/worker context propagation

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit context restoration | Restore MDC/span attributes from RequestContext/RunEvent/ToolExecutionRequest. | ✓ |
| TaskDecorator inheritance | Use Spring task/executor decoration to inherit MDC. | |
| OTel auto only | Rely mainly on OTel Java agent or automatic instrumentation. | |

**User's choice:** Explicit context restoration.

---

## Admin Operational Metrics

### Admin metric surface depth

| Option | Description | Selected |
|--------|-------------|----------|
| Operations summary + detail entry points | Overview key health/count/latency/error summaries plus detail pages by area; not a full APM explorer. | ✓ |
| Lightweight overview only | Add only a few health/count metrics to Admin overview. | |
| Full metrics explorer | Build query/chart/time-range metrics explorer. | |

**User's choice:** Operations summary + detail entry points.

### Metric categories

| Option | Description | Selected |
|--------|-------------|----------|
| Full critical path | Run, Model, Tool/Policy, MCP, Plugin, errors and warnings. | ✓ |
| Runtime first | Run, Model, Tool/Policy; MCP/Plugin mostly existing health. | |
| Governance first | Policy/Audit/MCP/Plugin security metrics; simpler Run/Model. | |

**User's choice:** Full critical path.

### Admin metric source

| Option | Description | Selected |
|--------|-------------|----------|
| App read model aggregation | Add App/Admin public DTO aggregation of operational metrics and Micrometer snapshots. | ✓ |
| Direct Actuator reads | Vaadin calls Actuator metrics directly. | |
| Direct DB aggregation | Adapter/Admin queries DB directly. | |

**User's choice:** App read model aggregation.

---

## Production Hardening Documentation

### Documentation depth

| Option | Description | Selected |
|--------|-------------|----------|
| Operations manual + extension points | Actionable config examples, risk boundaries, defaults, extension points, explicit deferrals. | ✓ |
| Minimal notes | Only current capabilities/configuration. | |
| Blueprint-level future design | Detailed design for future KMS/RBAC/Sandbox/Retention subsystems. | |

**User's choice:** Operations manual + extension points.

### Documentation topics

| Option | Description | Selected |
|--------|-------------|----------|
| All production topics | Secrets/KMS, Policy extension, tenant/RBAC hooks, Sandbox, Retention/Redaction, Deployment/Actuator/OTLP/Prometheus. | ✓ |
| Observability + security | Telemetry, Secrets, Redaction, Deployment; short Policy/RBAC/Sandbox hooks. | |
| Governance + tenancy | Policy, RBAC/Tenant, Audit/Retention; shorter telemetry deployment. | |

**User's choice:** All production topics.

---

## Regression Coverage

### Verification scope

| Option | Description | Selected |
|--------|-------------|----------|
| Full critical regression | Telemetry tests + no-key E2E for run/tool/model/MCP/plugin/policy + redaction + correlation + Admin UI smoke + ArchUnit. | ✓ |
| Telemetry-specific | Verify metrics/traces/logs with limited redaction/correlation. | |
| Docs + architecture gate | Mostly architecture and docs, light runtime telemetry tests. | |

**User's choice:** Full critical regression.

### External telemetry backend dependency

| Option | Description | Selected |
|--------|-------------|----------|
| Default no external dependencies | Use fake/in-memory MeterRegistry/SpanExporter/log capture; Collector/Prometheus optional profile/smoke only. | ✓ |
| Docker optional must-run | Use OTel Collector/Prometheus Testcontainers as primary verification. | |
| Documentation-only external verification | No backend simulation in tests. | |

**User's choice:** Default no external dependencies.

---

## the agent's Discretion

- Exact observability class/package names.
- Exact metric/span/tag naming if stable, documented, low-cardinality, and redacted.
- Exact Admin metrics UI layout within the operations-summary/detail-entry direction.
- Exact historical trace ID migration implementation, provided it is explicit and tested.

## Deferred Ideas

- Full SaaS tenant/RBAC/billing/quota product.
- Full KMS/vault/secret-rotation management product.
- Production sandbox for untrusted code/plugins/MCP stdio/general shell.
- Full APM/BI metrics explorer.
- Plugin marketplace/upload/install/upgrade/delete workflows.
