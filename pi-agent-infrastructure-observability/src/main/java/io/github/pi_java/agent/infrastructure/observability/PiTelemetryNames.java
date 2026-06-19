package io.github.pi_java.agent.infrastructure.observability;

public final class PiTelemetryNames {

    public static final String RUN_EVENTS_TOTAL = "pi.run.events.total";
    public static final String RUN_DISPATCH_DURATION = "pi.run.dispatch.duration";
    public static final String MODEL_CALLS_TOTAL = "pi.model.calls.total";
    public static final String MODEL_CALL_DURATION = "pi.model.call.duration";
    public static final String TOOL_EXECUTIONS_TOTAL = "pi.tool.executions.total";
    public static final String TOOL_EXECUTION_DURATION = "pi.tool.execution.duration";
    public static final String POLICY_DECISIONS_TOTAL = "pi.policy.decisions.total";
    public static final String MCP_INVOCATIONS_TOTAL = "pi.mcp.invocations.total";
    public static final String MCP_DISCOVERY_DURATION = "pi.mcp.discovery.duration";
    public static final String PLUGIN_LIFECYCLE_TOTAL = "pi.plugin.lifecycle.total";
    public static final String PLUGIN_DISCOVERY_DURATION = "pi.plugin.discovery.duration";

    public static final String RUN_SPAN = "pi.run";
    public static final String RUN_DISPATCH_SPAN = "pi.run.dispatch";
    public static final String MODEL_CALL_SPAN = "pi.model.call";
    public static final String TOOL_EXECUTION_SPAN = "pi.tool.execution";
    public static final String POLICY_DECISION_SPAN = "pi.policy.decision";
    public static final String MCP_INVOCATION_SPAN = "pi.mcp.invocation";
    public static final String PLUGIN_LIFECYCLE_SPAN = "pi.plugin.lifecycle";

    public static final String ATTR_TENANT_ID = "pi.tenant_id";
    public static final String ATTR_USER_ID = "pi.user_id";
    public static final String ATTR_SESSION_ID = "pi.session_id";
    public static final String ATTR_RUN_ID = "pi.run_id";
    public static final String ATTR_TRACE_ID = "pi.trace_id";
    public static final String ATTR_CORRELATION_ID = "pi.correlation_id";
    public static final String ATTR_COMPONENT = "pi.component";
    public static final String ATTR_STATUS = "pi.status";
    public static final String ATTR_EVENT_TYPE = "pi.event_type";
    public static final String ATTR_TOOL_ID = "pi.tool_id";
    public static final String ATTR_PROVIDER_ID = "pi.provider_id";
    public static final String ATTR_MCP_SERVER_ID = "pi.mcp.server_id";
    public static final String ATTR_PLUGIN_ID = "pi.plugin_id";

    private PiTelemetryNames() {
    }
}
