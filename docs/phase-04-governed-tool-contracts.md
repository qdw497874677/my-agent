# Phase 04 Governed Tool Contracts

Phase 04 validates the governed tool registry, workspace execution boundary, and mandatory invocation pipeline for Pi Java Agent Platform. Every future tool source must normalize into these contracts before execution.

## Requirement Coverage

| Requirement | Phase 04 validation |
|-------------|---------------------|
| `TOOL-01` | `ToolDescriptor` captures id/name/description, input/output `ToolSchema`, `ToolProvenance`, version, scopes, risk, side-effect classification, timeout, and metadata. |
| `TOOL-02` | `GeneralAgentLoop` turns model `ToolCall` intents into `ToolExecutionRequest` and invokes only `ToolExecutionGateway`; Cloud Server E2E proves model-to-tool-to-model execution through the gateway. |
| `TOOL-03` | `DefaultToolExecutionGateway` resolves descriptors, validates arguments through the App `ToolArgumentValidator` port, executes the binding, and normalizes `ToolExecutionResult`. |
| `TOOL-04` | The gateway applies cancellation checks, payload limit checks, redaction, result summarization, normalized error categories, and safe summaries. |
| `TOOL-05` | `ToolPolicyEvaluator` returns allow, deny/block, require approval, or require sandbox decisions before executor invocation. |
| `TOOL-06` | Tool lifecycle `RunEvent` and audit actions cover `tool.proposed`, `tool.policy_decided`, `tool.preview_generated`, `tool.approval_required`, `tool.started`, `tool.completed`, `tool.failed`, `tool.denied`, and `tool.cancelled`. |
| `TOOL-07` | Built-ins include safe `builtin.info`, side-effectful `builtin.workspace.write`, and allowlisted `builtin.workspace.command` without broad shell/file access. |
| `WORK-03` | `WorkspaceCommandTool` uses `CommandExecutionGateway`; `AllowlistedCommandExecutionGateway` runs with a workspace directory, sanitized environment, timeout, and executable allowlist. |
| `WORK-07` | Local-temp workspace support is dev/test bounded and documented as not a production sandbox. |
| `WORK-08` | `ProvisionPreview` is generated before side-effectful/preview-required actions and approval-required paths do not execute bindings. |
| `OPS-02` | `AuditRepository` records run creation/worker activity plus tool proposal, policy, preview, start, completion, denial, and approval-required summaries. |
| `OPS-03` | `DefaultToolPolicyEvaluator` is the conservative default policy engine and the App policy port remains pluggable. |
| `OPS-05` | `DefaultToolRedactor`, payload limiting, event DTO mapping, and security E2E prevent fake sensitive values from appearing in default REST/events/audit payloads. |
| `E2E-02` | `CloudServerGovernedToolE2ETest` proves successful REST-created model→tool→model execution through `ToolExecutionGateway`. |
| `E2E-03` | `CloudServerGovernedToolE2ETest` proves deny and approval-required paths emit events/audit and prevent unauthorized executor side effects. |
| `E2E-06` | `GovernedToolSecurityRedactionE2ETest` proves fake secrets are absent from REST details, event history, persisted events, audit details, and safe exception paths. |

## ToolDescriptor

`ToolDescriptor` is the canonical source-agnostic registration contract. It includes:

- stable tool id, display name, and description;
- JSON-schema-compatible input schema and optional output schema;
- `ToolProvenance` with source kind, source id, binding reference, and filtered metadata;
- descriptor version;
- required scopes;
- risk level and side-effect classification;
- default timeout;
- metadata for catalog/API/UI hints such as preview and approval recommendations.

All future SPI, Spring Bean, MCP, and dynamic plugin tools must produce a `ToolDescriptor` plus executor binding before entering the registry.

## ToolRegistry

The App `ToolRegistry` port resolves a descriptor and `ToolExecutorBinding` by id. Infrastructure currently provides `InMemoryToolRegistry` and built-in registrations. The registry remains descriptor-first and intentionally avoids source-specific core methods such as `registerMcpTool` or `registerPluginTool`.

## ToolExecutionGateway

`ToolExecutionGateway` is the only supported invocation path for runtime/provider/extension tool calls. The default gateway flow is:

1. resolve the descriptor and executor binding;
2. redact and summarize arguments;
3. emit/audit `tool.proposed`;
4. enforce argument payload limits and schema validation;
5. evaluate `ToolPolicyEvaluator`;
6. emit/audit `tool.policy_decided`;
7. generate `ProvisionPreview` when policy or side-effect classification requires it;
8. stop for deny/block, `approval`, or `sandbox` gates without executor invocation;
9. emit/audit `tool.started`;
10. execute the binding with cancellation support;
11. summarize/redact/limit raw output;
12. emit/audit terminal lifecycle events and return normalized `ToolExecutionResult`.

## Validation Library Boundary

Domain owns schema value objects only. App owns the `ToolArgumentValidator` port. Infrastructure owns the concrete `NetworkntToolArgumentValidator`, keeping JSON Schema library dependencies out of Domain and App orchestration contracts.

## Policy Decisions

The policy decision model preserves `ALLOW`, `DENY`, `BLOCK`, `REQUIRE_APPROVAL`, and `REQUIRE_SANDBOX` semantics. `DENY`/`BLOCK` and gate decisions are observable through events and audit, and must not invoke executor bindings.

## ProvisionPreview, Approval, and Sandbox Semantics

`ProvisionPreview` is a static impact estimate and must not perform writes or process execution. Side-effectful workspace tools default to preview/approval behavior. Approval-required currently returns a non-executing gateway status and a policy-blocked fake run terminal status until Phase 5 introduces Web Console approval cards/resume flows. `REQUIRE_SANDBOX` is modeled as a capability gate; local-temp execution is not treated as a production sandbox.

## Lifecycle Events

Tool lifecycle events use stable wire names on `RunEventType` and public `RunEventDto.type`:

- `tool.proposed`
- `tool.policy_decided`
- `tool.preview_generated`
- `tool.approval_required`
- `tool.started`
- `tool.completed`
- `tool.failed`
- `tool.denied`
- `tool.cancelled`

Public DTO payloads use `payloadSchema=tool.lifecycle` and redacted summary maps for input, output, preview, policy, status, provenance, and error category.

## Audit, Redaction, and Payload Limits

Audit records are summary-level and decision-level. They include redacted input/output summaries, policy reasons/refs, preview identifiers/details, and error categories. `DefaultToolRedactor` uses schema sensitive fields plus conservative key/value patterns. `DefaultToolPayloadLimiter` records estimated bytes, limit bytes, truncation state, preview text, and value type instead of persisting unbounded raw payloads.

## Built-in Tools

Phase 4 built-ins are ordinary descriptor/binding registrations:

- `builtin.info` — safe read-only injected platform info.
- `builtin.workspace.write` — workspace-scoped resource write/append, side-effectful and preview/approval recommended.
- `builtin.workspace.command` — allowlisted command execution through `CommandExecutionGateway`, side-effectful and preview/approval recommended.

## Bounded Local-Temp Workspace Limitations

`LocalTempWorkspaceGateway` and `AllowlistedCommandExecutionGateway` are deterministic dev/test infrastructure. They constrain paths to workspace roots, sanitize command environments, allow only configured executable names, and enforce timeout/output summary limits. They are explicitly not a production sandbox, not a Coding Agent shell, and not a general host filesystem interface.

## REST Catalog API

Cloud Server exposes `GET /api/tools` as a read-only catalog endpoint. It returns client DTOs for descriptor metadata and provenance without executor classes, raw secrets, or implementation-only details. Create/update/delete and admin governance actions are deferred.

## E2E Verification

No-key focused commands:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=CloudServerGovernedToolE2ETest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=GovernedToolSecurityRedactionE2ETest test
```

The first test suite creates Cloud Server runs via REST, activates worker/runtime paths, asserts persisted event history and in-memory audit records, and proves success, deny, approval-required, preview, and workspace-command paths. The second suite uses fake sensitive values only in test code and asserts absence from API DTOs, event history, persisted events, audit records, and normalized exception paths.

## Deferrals

- Java SPI and Spring Bean tool discovery: Phase 6.
- MCP tool discovery/execution: Phase 7.
- Dynamic plugin tool registration/lifecycle: Phase 8.
- Agent Web Console tool cards and approval cards: Phase 5.
- Production sandbox/Coding Agent workspace execution: later hardening beyond local-temp dev/test support.
- Distributed quotas, persistent workspace providers, and advanced policy engine backends: later governance/hardening work.
