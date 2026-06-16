# Phase 07 MCP Client Bridge Contracts

Phase 07 validates `MCP-01` through `MCP-05` and the MCP portion of `E2E-08`. MCP is implemented as a governed remote-tool adapter: trusted MCP servers are configured at the Adapter/Infrastructure boundary, discovered tools are normalized into Pi `ToolDescriptor` records, and runtime invocation enters the existing `ToolExecutionGateway` pipeline.

## Requirement Coverage

| Requirement | Phase 07 validation |
|-------------|---------------------|
| `MCP-01` | `pi.mcp.servers` binds trusted server definitions with enablement, transport, endpoint/command, timeout, metadata, and static credential references. `McpServerPropertiesTest`, `McpClientFactoryTest`, and this document validate configuration-file-first setup. |
| `MCP-02` | `McpServerRegistry`, `McpToolDescriptorMapper`, `McpToolRegistryTest`, and `McpToolRegistryWiringTest` discover MCP tools, normalize JSON Schema into `ToolDescriptor`, expose `mcp.<server>.<tool>` IDs, and keep failed servers visible in governance while unavailable tools stay out of runtime resolution. |
| `MCP-03` | `McpToolExecutorBinding`, `McpToolExecutorBindingTest`, `McpGovernedToolE2ETest`, and `McpSecurityRedactionE2ETest` prove MCP tool calls execute through `ToolExecutionGateway`, policy, timeout/cancellation, audit, redaction, and `tool.lifecycle` events. |
| `MCP-04` | `McpGovernanceCatalogAdapter`, `McpGovernanceApiTest`, `McpAdminGovernanceViewTest`, and `e2e/phase-07-mcp-governance.spec.ts` expose connection state, discovery errors, invocation/auth/timeout status, server health, and redacted error text through public Admin REST/UI. |
| `MCP-05` | `McpSafetyValidator`, `McpSecretHeaderResolver`, `McpClientFactoryTest`, architecture gates, and redaction E2E validate credential refs, SSRF-sensitive HTTP controls, trusted stdio command controls, safe error normalization, and package boundary isolation. |
| `E2E-08` | MCP portion complete in Phase 7 by no-key fake MCP discovery/execution/status tests. Sample plugin JAR loading/disable/quarantine remains pending Phase 8. |

## Configuration-File-First Server Setup

Cloud Server binds MCP configuration under `pi.mcp` in Adapter Web composition. Server definitions are static configuration for v1; Admin Governance is inspect-only and does not add/edit/delete/disable MCP servers.

Example shape:

```yaml
pi:
  mcp:
    discovery:
      startup: false
    servers:
      - id: github
        enabled: true
        display-name: GitHub MCP
        transport: STREAMABLE_HTTP
        base-url: https://mcp.example.test
        endpoint: /mcp
        timeout: 10s
        auth:
          bearer-token-ref: env:GITHUB_MCP_TOKEN
        metadata:
          owner: platform
```

Static auth material is always represented as references (`env:`, `config:`, or future credential-reference schemes). Raw secret values are resolved only inside `McpSecretHeaderResolver` immediately before transport construction and are never exposed through public DTOs, events, audits, or UI text.

## Supported Transports and Safety Defaults

Phase 07 supports three configured transport kinds at the infrastructure boundary:

- `STREAMABLE_HTTP` — default remote HTTP MCP transport. Requires a configured base URL/endpoint and passes SSRF-sensitive validation before client creation.
- `SSE` — supported as an explicit remote transport selection for compatible MCP servers.
- `STDIO` — supported only for trusted configured commands. Tests assert transport selection through fake seams; arbitrary shell launch is not required for no-key validation.

Safety defaults:

1. disabled servers remain visible in governance but are not discovered or registered,
2. failed discovery preserves redacted server state while stale tools become unavailable,
3. HTTP configuration is validated before transport creation,
4. stdio requires an explicit trusted command,
5. raw credential/header values are summarized by kind/count only,
6. remote errors are normalized to categories such as `MCP_AUTH_FAILED`, `MCP_TIMEOUT`, or `MCP_TOOL_ERROR` with safe operator hints.

OAuth/protected-resource negotiation, dynamic credential CRUD, resources/prompts, and public Admin server mutation controls are deferred.

## Discovery, Refresh, and Descriptor Normalization

`McpServerRegistry` owns replace-all per-server snapshots. Manual refresh is exposed as:

```text
POST /api/admin/governance/mcp/refresh
```

The refresh endpoint triggers rediscovery only; it is not server configuration mutation. Tool descriptors use server-qualified names:

```text
mcp.<server-id>.<tool-name>
```

MCP input schemas pass through as JSON Schema-compatible `ToolSchema` documents. Provenance is `ToolProvenance.SourceKind.MCP`, `sourceId=<server-id>`, and `bindingRef=mcp:<server-id>:<tool-name>`. Scopes include `tool:mcp`, `mcp:server:<server-id>`, and `mcp:tool:<server-id>:<tool-name>`. MCP annotations map to Pi risk/side-effect hints without becoming policy decisions themselves.

## Gateway-Only Invocation

Remote MCP execution is intentionally not a runtime shortcut:

```text
Fake/real model tool-call intent
  -> GeneralAgentLoop
  -> ToolRegistry.resolve("mcp.<server>.<tool>")
  -> ToolExecutionGateway
  -> schema validation
  -> ToolPolicyEvaluator
  -> audit repository
  -> tool.lifecycle events
  -> McpToolExecutorBinding
  -> MCP client handle
```

`McpToolExecutorBinding` captures the configured server id and MCP tool name from discovery/registration, so request arguments cannot redirect execution to arbitrary MCP endpoints or tools. The binding maps successful text/structured content into summarized output and redacts binary/error bodies by default.

## Governance API and UI

Public Admin surfaces are read-only except for rediscovery refresh:

- `GET /api/admin/governance/mcp` — server/tool status, auth summary, transport, discovery state, health/error text, schema/risk hints.
- `POST /api/admin/governance/mcp/refresh` — manual rediscovery.
- Admin Registry Vaadin view — renders the same public DTO fields and refresh action plan.

No Phase 07 UI/API provides add/edit/delete/disable server controls. That keeps trusted server configuration auditable via deployment configuration and avoids accidental credential entry in the browser.

## Verification Commands

Focused architecture gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp -am -Dtest=McpInfrastructureArchitectureTest test
```

No-key product-path MCP E2E and redaction gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=McpGovernedToolE2ETest,McpSecurityRedactionE2ETest test
```

Focused Phase 07 smoke gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp,pi-agent-adapter-web -am -Dtest=McpInfrastructureArchitectureTest,McpToolRegistryTest,McpToolExecutorBindingTest,McpGovernanceApiTest,McpGovernedToolE2ETest,McpSecurityRedactionE2ETest test
npm run e2e -- e2e/phase-07-mcp-governance.spec.ts
```

All gates are no-key and use fake clients, fake discovery, fake model tool-call intents, in-memory state, and deterministic redaction fixtures.

## Architecture Boundary

`McpInfrastructureArchitectureTest` enforces D-28 boundaries:

- `io.modelcontextprotocol..` and Spring AI MCP packages are allowed only in MCP infrastructure and Adapter Web composition/test surfaces,
- Domain, App, Client, Extension API, and Spring starter do not depend on MCP SDK/Spring AI MCP types,
- MCP infrastructure does not depend on Adapter Web, Vaadin, PF4J, or plugin packages.

## Explicit Deferrals

- OAuth/protected-resource flows and dynamic credential lifecycle.
- MCP resource and prompt surfaces; Phase 07 covers tools only.
- Admin CRUD for MCP server configuration.
- Exposing Pi as an MCP server/gateway.
- Dynamic plugin JAR loading/disable/quarantine and the remaining plugin portion of `E2E-08` — Phase 8.
- Production telemetry spans/metrics for MCP calls — Phase 9 `OPS-01`.
