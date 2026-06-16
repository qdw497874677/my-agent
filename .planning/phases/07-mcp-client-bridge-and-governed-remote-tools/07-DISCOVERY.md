# Phase 07 Discovery: MCP Client Bridge and Governed Remote Tools

**Date:** 2026-06-16  
**Discovery level:** Level 2 — new external integration plus transport/auth/security choices.  
**Sources:** Phase 7 CONTEXT decisions, Context7 Spring AI MCP docs, Context7 MCP Java SDK docs, existing Phase 4/5/6 contract docs.

## Findings

### Spring AI MCP

- Spring AI 1.1.x documents MCP client configuration under `spring.ai.mcp.client` with `type: SYNC|ASYNC`, `request-timeout`, and named connections for `streamable-http`, `sse`, and `stdio`.
- Spring AI MCP can adapt MCP tools into Spring AI `ToolCallback` via `SyncMcpToolCallback`/`AsyncMcpToolCallback`, but Pi must not make Spring AI tool callbacks the authoritative internal model because Phase 4 already owns `ToolDescriptor` + `ToolExecutorBinding` + `ToolExecutionGateway`.
- For Pi Phase 7, Spring AI MCP is useful as reference and possibly dependency management, but the execution model should remain Pi-owned.

### MCP Java SDK

- MCP Java SDK exposes `McpSyncClient` via `McpClient.sync(transport).requestTimeout(Duration).capabilities(...).build()`.
- Tool operations are direct and sufficient for Phase 7 Tools-only scope:
  - initialize: `client.initialize()`
  - discover: `client.listTools()`
  - invoke: `client.callTool(new CallToolRequest(name, args))`
  - close: `client.closeGracefully()`
- Transport examples cover:
  - Streamable HTTP: `HttpClientStreamableHttpTransport.builder(baseUrl).endpoint("/mcp").build()`
  - legacy SSE: `new HttpClientSseClientTransport(baseUrl)`
  - stdio: `new StdioClientTransport(ServerParameters.builder(command).args(...).build())`
- Client capabilities include resources/prompts/sampling/elicitation/root support, but Phase 7 must keep capabilities minimal and Tools-only per D-09.

## Planning Decisions from Discovery

1. Add a dedicated `pi-agent-infrastructure-mcp` module. MCP SDK/Spring AI MCP dependencies stay isolated there and in adapter composition tests only; Domain/App/client/starter must not import MCP SDK types (D-28).
2. Use direct MCP Java SDK concepts inside infrastructure seams instead of routing through Spring AI `ToolCallback`, because Pi must normalize into `ToolDescriptor` and execute via `ToolExecutionGateway` (D-10, D-12).
3. Model Phase 7 configuration as typed Spring properties under `pi.mcp.servers` with transport enum `STREAMABLE_HTTP`, `STDIO`, `SSE`, static auth refs/headers, safety validation, and redacted public summaries (D-01, D-05, D-07, D-15).
4. Create App/client governance read models that contain only strings, booleans, counts, lists, and `Map<String,String>` metadata; no MCP SDK types leak upward (D-11, D-19, D-28).
5. Implement startup/manual refresh discovery through an infrastructure registry that retains failed/unhealthy configured server statuses instead of silently hiding them (D-20 through D-22).
6. Use server-qualified tool IDs as `mcp.<serverId>.<toolName>` and pass MCP `inputSchema` through as `ToolSchema("json-schema", document, sensitiveFields, payloadLimitBytes)` without custom conversion (D-13, D-14).
7. Default MCP tool descriptors to remote/external risk. `readOnlyHint=true` may map to read-only/low-risk when Agent allowlist permits it; unknown/non-read-only/destructive/open-world tools remain conservative (D-16, D-17).
8. Verification must be deterministic and no-key: fake/loopback MCP clients/servers, fake secrets, in-memory E2E, and focused architecture gates (D-25 through D-28).

## Out of Scope Guardrails

- No Admin CRUD for MCP server config.
- No OAuth 2.1/DCR/protected-resource implementation.
- No MCP resources/prompts/sampling/elicitation/Pi-as-MCP-server.
- No general shell execution through stdio; stdio is only a trusted configured MCP server command.
- No large `mcp.*` event family; tool failures stay in existing `tool.lifecycle`/audit semantics.
