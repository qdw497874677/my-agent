---
phase: 07-mcp-client-bridge-and-governed-remote-tools
verified: 2026-06-16T10:11:32Z
status: passed
score: 6/6 must-haves verified
---

# Phase 7: MCP Client Bridge and Governed Remote Tools Verification Report

**Phase Goal:** MCP Client Bridge and Governed Remote Tools — add a governed MCP client bridge that discovers configured remote MCP tools, normalizes them into the existing tool registry/governance model, executes them only through `ToolExecutionGateway`, exposes Admin status/refresh, and validates no-key E2E and redaction/architecture boundaries.

**Verified:** 2026-06-16T10:11:32Z  
**Status:** passed  
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Admin can configure trusted MCP servers with credential references, transport settings, server allowlists, and network controls. | ✓ VERIFIED | `pi-agent-infrastructure-mcp` exists in the reactor; `McpServerProperties` models `STREAMABLE_HTTP`, `SSE`, `STDIO`, timeout, auth refs, env refs, metadata, and redacted public summaries. `McpSafetyValidator` rejects unsafe schemes, missing hosts, URL credentials, invalid stdio config, and raw-looking secret/header refs. Adapter Web binds `pi.mcp.servers` in `McpGovernanceBeanConfiguration`. |
| 2 | Platform discovers MCP tools, normalizes schemas into `ToolDescriptor`, and registers them with provenance/server health. | ✓ VERIFIED | `McpServerRegistry` maintains refresh snapshots and failed-server visibility; `McpToolDescriptorMapper` creates `mcp.<server>.<tool>` IDs, `ToolProvenance.SourceKind.MCP`, `json-schema` `ToolSchema`, MCP metadata hints, and explicit scopes. `McpToolRegistry implements ToolRegistry` and Adapter Web composes it into the primary registry. |
| 3 | MCP tool calls execute only through `ToolExecutionGateway` with policy, timeout, cancellation, audit, redaction, and events. | ✓ VERIFIED | `McpToolRegistry.resolve()` returns a `ToolResolution` with `McpToolExecutorBinding`; `ToolGovernanceBeanConfiguration` passes the composed `ToolRegistry` into `DefaultToolExecutionGateway`. `McpGovernedToolE2ETest` creates REST runs using fake model tool-call intents and asserts `tool.proposed`, `tool.policy_decided`, `tool.started`, `tool.completed`, audit records, approval/deny no-invocation paths, and timeout/auth categories. |
| 4 | Admin can see MCP connection state, discovery errors, invocation errors, auth failures, and server health/status, and can refresh discovery. | ✓ VERIFIED | `McpGovernanceCatalogAdapter` maps snapshots to App MCP statuses; `DefaultGovernanceQueryService.mcp()`/`refreshMcp()` maps them to public DTOs; `AdminGovernanceController` exposes `GET /api/admin/governance/mcp` and `POST /api/admin/governance/mcp/refresh`. `AdminRegistryStatusView` renders server/tool status and refresh action using public DTOs. |
| 5 | Security boundaries for SSRF-sensitive controls, credential references, transport configuration, and dependency isolation are enforced. | ✓ VERIFIED | `McpSafetyValidator`, `McpSecretHeaderResolver`, `McpClientErrorSanitizer`, and `McpInvocationErrorMapper` keep raw secrets inside infrastructure and sanitize errors. `McpInfrastructureArchitectureTest` forbids MCP SDK/Spring AI MCP leakage into Domain/App/Client/Extension API/Spring starter and forbids MCP infrastructure from depending on Adapter/Vaadin/PF4J/plugin packages. |
| 6 | No-key E2E validates fake MCP discovery/execution through the governed pipeline and documents E2E-08 partial scope without claiming plugin completion. | ✓ VERIFIED | Focused Maven and Playwright gates passed. `McpGovernedToolE2ETest` and `McpSecurityRedactionE2ETest` run without Docker/model keys; `e2e/phase-07-mcp-governance.spec.ts` passed. `docs/phase-07-mcp-client-bridge.md` and `.planning/REQUIREMENTS.md` mark MCP portion complete and plugin portion pending Phase 8. |

**Score:** 6/6 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `pi-agent-infrastructure-mcp/pom.xml` | Dedicated MCP infrastructure module with isolated MCP dependencies | ✓ VERIFIED | Module exists, parent reactor includes it, and `spring-ai-starter-mcp-client` is declared only in this module; Adapter Web depends on this module for composition. |
| `McpServerProperties.java` / `McpSafetyValidator.java` | Typed trusted server configuration and safety validation | ✓ VERIFIED | Models transports/auth refs and validates HTTP/SSE/stdio safety before client creation. |
| `McpClientFactory.java` / `McpTransportFactory.java` / `McpSecretHeaderResolver.java` | Infrastructure-only MCP client creation and credential injection | ✓ VERIFIED | Creates initialized sync clients with minimal capabilities, resolves refs at final boundary, and uses fakeable seams for no-network tests. |
| `McpGovernanceCatalog.java` + client MCP DTOs | Public App/client Admin MCP read/refresh contracts | ✓ VERIFIED | App/client records use JDK-safe types; no MCP SDK/Spring/Vaadin types in public contracts. |
| `McpServerRegistry.java` / `McpToolRegistry.java` / `McpToolDescriptorMapper.java` | Discovery, descriptor normalization, governance status, and registry projection | ✓ VERIFIED | Implements discovery snapshots, `ToolRegistry`, server-qualified IDs, MCP provenance, JSON Schema passthrough, health/error projection. |
| `McpToolExecutorBinding.java` / result/error mappers | Governed remote MCP invocation binding | ✓ VERIFIED | Calls configured server/tool only, checks cancellation, maps success/errors to `ToolExecutionResult`, sanitizes failures. |
| `McpGovernanceBeanConfiguration.java` / `ToolGovernanceBeanConfiguration.java` | Cloud Server composition into the same registry/gateway path | ✓ VERIFIED | Binds `pi.mcp`, creates MCP registry/catalog beans, composes MCP registry after built-ins/extensions, and feeds the primary `ToolExecutionGateway`. |
| `AdminGovernanceController.java` | Admin MCP status and refresh endpoints | ✓ VERIFIED | Exposes `GET /api/admin/governance/mcp` and `POST /api/admin/governance/mcp/refresh`; no PUT/PATCH/DELETE config mutation endpoints. |
| `AdminRegistryStatusView.java` / `ConsoleHttpClient.java` | Admin UI/public API helpers for MCP status/refresh | ✓ VERIFIED | Uses public DTOs and paths; renders server/tool status, redacted errors, and refresh action; no CRUD controls. |
| `McpInfrastructureArchitectureTest.java` | MCP dependency boundary gate | ✓ VERIFIED | ArchUnit rules present and passed in focused gate. |
| `McpGovernedToolE2ETest.java` / `McpSecurityRedactionE2ETest.java` | No-key product-path E2E and redaction coverage | ✓ VERIFIED | Tests exercise REST-created runs, ToolExecutionGateway, policy, audit, events, Admin governance, and secret absence. |
| `e2e/phase-07-mcp-governance.spec.ts` | Browser/API smoke for MCP governance | ✓ VERIFIED | Passed with Playwright; checks status, discovered tools, redaction, refresh, and no CRUD controls. |
| `docs/phase-07-mcp-client-bridge.md` | Contract and verification index | ✓ VERIFIED | Documents configuration, transports, safety defaults, descriptor normalization, gateway-only invocation, Admin APIs/UI, verification commands, deferrals. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `pi-agent-infrastructure-mcp` | MCP SDK/Spring AI MCP | Module-scoped dependency | ✓ WIRED | `spring-ai-starter-mcp-client` appears in `pi-agent-infrastructure-mcp/pom.xml`; root only has BOM, and forbidden modules do not import MCP SDK types. |
| `McpClientFactory` | `McpServerProperties` | Validated config creates transport/client | ✓ WIRED | `McpClientFactory.create()` calls `McpSafetyValidator.validate(server)`, resolves secrets, creates transport, and initializes MCP client. |
| `DefaultGovernanceQueryService` | `McpGovernanceCatalog` | Constructor-injected App port | ✓ WIRED | Service stores `mcpGovernanceCatalog`, derives overview MCP status, maps `mcp()` and `refreshMcp()`. |
| `McpToolRegistry` | `ToolRegistry` | Implements App port and returns `ToolResolution` | ✓ WIRED | Class implements `ToolRegistry`; `resolve()` returns descriptor + `McpToolExecutorBinding`. |
| `McpToolRegistry.resolve` | `McpToolExecutorBinding` | `ToolRegistry.ToolResolution` executor | ✓ WIRED | `resolve()` creates binding from captured server/tool snapshot; request args cannot override endpoint/tool. |
| `ToolGovernanceBeanConfiguration` | `McpToolRegistry` | Composite registry includes MCP after built-ins/extensions | ✓ WIRED | Primary `toolRegistry(...)` accepts `Optional<McpToolRegistry>`, appends it to composite registry, and `ToolExecutionGateway` receives this registry. |
| `AdminRegistryStatusView` | `/api/admin/governance/mcp` | `ConsoleHttpClient` public REST helper | ✓ WIRED | UI helper exposes MCP status/refresh paths and DTO anchors; view renders public DTOs and refresh action. |
| `McpGovernedToolE2ETest` | `ToolExecutionGateway` | REST-created run with fake model `mcp.*` tool-call intent | ✓ WIRED | Test runtime injects `ToolExecutionGateway`, asserts `tool.lifecycle` payloads and audit events. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|---|---|---|---|---|
| `McpServerRegistry` | Discovery snapshots | Configured `McpServerProperties` + MCP client `listTools()` / fake discovery seams | Yes — successful snapshots include discovered tools; failures retain configured server status with redacted error | ✓ FLOWING |
| `McpToolRegistry` | `ToolDescriptor` list/resolution | `McpServerRegistry.snapshots()` mapped by `McpToolDescriptorMapper` | Yes — available tools are listed/resolved; failed/unavailable tools are not executable | ✓ FLOWING |
| `DefaultGovernanceQueryService` | MCP overview/DTOs | `McpGovernanceCatalog.servers()` and `refresh()` | Yes — catalog-derived server/tool counts/status replace Phase 5 placeholder | ✓ FLOWING |
| `AdminGovernanceController` | REST responses | `GovernanceQueryService.mcp()` / `refreshMcp()` | Yes — controller delegates to App service and returns public DTOs | ✓ FLOWING |
| `AdminRegistryStatusView` | Rendered MCP server/tool lines | `McpGovernanceResponse` DTO passed from public client/helper path | Yes — renders DTO fields; view tests assert healthy/unhealthy/tool/redacted content | ✓ FLOWING |
| `McpToolExecutorBinding` | Remote call result | Configured MCP client handle `callTool(new CallToolRequest(mcpToolName, args))` | Yes — maps SDK success/error/failure into `ToolExecutionResult` | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Focused Phase 7 Maven no-key gate | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-mcp,pi-agent-adapter-web -am -Dtest=McpInfrastructureArchitectureTest,McpServerPropertiesTest,McpClientFactoryTest,McpToolRegistryTest,McpToolExecutorBindingTest,McpToolRegistryWiringTest,McpGovernanceApiTest,McpAdminGovernanceViewTest,McpGovernedToolE2ETest,McpSecurityRedactionE2ETest test` | Completed successfully; no Docker required. Logs show Spring test contexts and MCP-focused tests ran. | ✓ PASS |
| Phase 7 Playwright MCP governance smoke | `npm run e2e -- e2e/phase-07-mcp-governance.spec.ts` | `1 passed (34.9s)`; server started under test/e2e profiles and spec verified status, discovered tools, redaction, refresh, no CRUD controls. | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| MCP-01 | 07-01, 07-02, 07-03, 07-06, 07-08 | Admin can configure trusted MCP servers as MCP client targets. | ✓ SATISFIED | `pi.mcp.servers` binding in `McpGovernanceBeanConfiguration`; typed `McpServerProperties`; safety and client factory tests; docs and REQUIREMENTS evidence. |
| MCP-02 | 07-04, 07-06, 07-08 | Discover MCP tools, normalize schemas into `ToolDescriptor`, register with provenance/server health metadata. | ✓ SATISFIED | `McpServerRegistry`, `McpToolDescriptorMapper`, `McpToolRegistry`, `ToolProvenance.SourceKind.MCP`, `json-schema`, `McpToolRegistryWiringTest`, E2E `/api/tools` checks. |
| MCP-03 | 07-05, 07-06, 07-08 | MCP tools execute only through `ToolExecutionGateway` with governance controls. | ✓ SATISFIED | `McpToolExecutorBinding` behind `ToolRegistry.ToolResolution`; composed registry injected into `DefaultToolExecutionGateway`; E2E asserts policy/audit/events/no-invocation on approval/deny. |
| MCP-04 | 07-02, 07-04, 07-05, 07-06, 07-07, 07-08 | Records/exposes connection, discovery, invocation, auth, health in Admin GUI and event/audit. | ✓ SATISFIED | `McpGovernanceCatalogAdapter`, `AdminGovernanceController`, `AdminRegistryStatusView`, `McpGovernanceApiTest`, `McpAdminGovernanceViewTest`, Playwright spec, E2E status/error assertions. |
| MCP-05 | 07-01, 07-03, 07-04, 07-05, 07-08 | Security boundaries for allowlists, credential refs, transport, SSRF-sensitive controls. | ✓ SATISFIED | `McpSafetyValidator`, `McpSecretHeaderResolver`, sanitized client/invocation error mappers, architecture gate, redaction E2E. |
| E2E-08 (partial MCP evidence) | 07-08 | Fake MCP server discovery/execution through gateway/policy/audit/event; plugin half remains Phase 8. | ✓ SATISFIED FOR PHASE 7 SCOPE | `.planning/REQUIREMENTS.md` intentionally keeps E2E-08 unchecked/pending overall while stating MCP portion is validated by `McpGovernedToolE2ETest`, `McpSecurityRedactionE2ETest`, and Playwright MCP governance spec. |

**Orphaned Phase 7 requirements:** None found. ROADMAP and REQUIREMENTS map Phase 7 to MCP-01..MCP-05 plus E2E-08; all are claimed by Plan frontmatter and accounted for above. E2E-08 remains pending overall because plugin JAR flows are Phase 8 scope, matching the user-provided requirement qualifier.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---:|---|---|---|
| `pi-agent-infrastructure-mcp/src/main/java/.../McpAuthProperties.java` | 100 | `return null` | ℹ️ Info | Optional-field normalization helper; not a user-visible stub and covered by tests. |
| `pi-agent-infrastructure-mcp/src/main/java/.../McpServerProperties.java` | 111 | `return null` | ℹ️ Info | Optional-field normalization helper; not a user-visible stub and covered by tests. |
| `pi-agent-adapter-web/src/main/java/.../ChatEventStreamPanel.java` | 15/27/43 | UI placeholder text | ℹ️ Info | Pre-existing chat input placeholder outside Phase 7 MCP files; not an MCP-goal blocker. |

No blocker stubs, orphaned MCP artifacts, or deferred `MCP_EXECUTION_DEFERRED` binding remain in production MCP code.

### Human Verification Required

None required for Phase 7 goal achievement. Visual polish can always be reviewed manually, but the required Admin MCP status/refresh behavior is covered by Java view tests and Playwright smoke.

### Gaps Summary

No gaps found. Phase 7 goal is achieved: MCP is implemented as a governed remote-tool adapter with configuration-file-first trusted servers, discovery/descriptor normalization into the existing tool registry, gateway-only invocation, Admin status/refresh, redaction/security boundaries, architecture isolation, and no-key E2E evidence. Full `E2E-08` remains pending only for the explicitly out-of-scope Phase 8 plugin portion.

---

_Verified: 2026-06-16T10:11:32Z_  
_Verifier: the agent (gsd-verifier)_
