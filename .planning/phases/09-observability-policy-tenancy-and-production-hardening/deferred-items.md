# Deferred Items

## 2026-06-19 — 09-04 verification blocked by unrelated MCP compile error

- **Found during:** Plan 09-04 Task 2 verification command for `pi-agent-infrastructure-observability,pi-agent-adapter-web`.
- **Issue:** Reactor compilation stops in `pi-agent-infrastructure-mcp/src/main/java/io/github/pi_java/agent/infrastructure/mcp/registry/McpToolRegistry.java` because it calls `McpServerProperties.transportKind()`, which is unavailable in the current source shape.
- **Scope decision:** Out of scope for Plan 09-04 because this plan only owns governed tool/policy telemetry decorators and Adapter Web wiring.
- **Local verification still completed:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am -Dtest=TelemetryToolExecutionGatewayTest,TelemetryToolPolicyEvaluatorTest test` passed.
