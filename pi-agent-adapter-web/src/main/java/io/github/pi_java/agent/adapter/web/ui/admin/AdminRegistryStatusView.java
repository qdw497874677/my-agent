package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;
import io.github.pi_java.agent.client.admin.ExtensionCapabilityDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.ExtensionSourceDto;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpServerDto;
import io.github.pi_java.agent.client.admin.McpToolDto;
import io.github.pi_java.agent.client.admin.PluginCapabilityDto;
import io.github.pi_java.agent.client.admin.PluginGovernanceResponse;
import io.github.pi_java.agent.client.admin.PluginSourceDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Read-only provider/tool/extension/MCP/plugin registry status view for Admin Governance. */
@Route(value = "admin/governance/registry", layout = PiResponsiveShell.class)
@PageTitle("Pi Admin Registry Status")
public class AdminRegistryStatusView extends Div {

    private static final String PLUGIN_NON_SANDBOX_WARNING = "Plugin governance warning: JVM classloader isolation "
            + "is dependency/lifecycle isolation only; it is not a sandbox for untrusted code.";

    private final ConsoleHttpClient httpClient;
    private final List<String> renderedLines = new ArrayList<>();
    private boolean pluginMutationControlsPresent;

    public AdminRegistryStatusView() {
        this(new ConsoleHttpClient());
    }

    public AdminRegistryStatusView(ConsoleHttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        addClassName("pi-admin-registry-status");
        getElement().setAttribute("data-route", "admin-registry-status");
        getElement().setAttribute("data-admin-surface", "inspect-only");
        getElement().setAttribute("data-mutation-controls", "absent");
        renderEmpty();
    }

    public String overviewPath() {
        return httpClient.adminGovernanceOverviewPath();
    }

    public String extensionGovernancePath() {
        return httpClient.adminExtensionGovernancePath();
    }

    public String mcpGovernancePath() {
        return httpClient.adminMcpGovernancePath();
    }

    public String mcpRefreshPath() {
        return httpClient.adminMcpRefreshPath();
    }

    public String mcpRefreshActionText() {
        return "Refresh MCP discovery via " + mcpRefreshPath();
    }

    public String pluginGovernancePath() {
        return httpClient.adminPluginGovernancePath();
    }

    public String pluginRefreshPath() {
        return httpClient.adminPluginRefreshPath();
    }

    public String pluginRefreshActionText() {
        return "Refresh plugin discovery via " + pluginRefreshPath();
    }

    public String pluginDisableActionText(String pluginId) {
        return "Disable plugin " + safe(pluginId) + " via " + httpClient.adminPluginDisablePath(pluginId)
                + " | confirmation required | optional reason";
    }

    public String pluginQuarantineActionText(String pluginId) {
        return "Quarantine plugin " + safe(pluginId) + " via " + httpClient.adminPluginQuarantinePath(pluginId)
                + " | confirmation required | optional reason";
    }

    public void showOverview(GovernanceOverviewResponse overview) {
        Objects.requireNonNull(overview, "overview must not be null");
        renderedLines.clear();
        pluginMutationControlsPresent = false;
        removeAll();
        add(new H2("Registry and Integration Status"));
        add(new Span("Provider, tool, extension, MCP, and plugin status is inspect-only in Phase 5."));
        addStatus("Model Providers", overview.providers());
        addStatus("Tool Registry", overview.toolRegistry());
        addStatus("Extensions", overview.extensions());
        addStatus("MCP", overview.mcp());
        addStatus("Plugins", overview.plugins());
    }

    public void showExtensions(ExtensionGovernanceResponse extensions) {
        Objects.requireNonNull(extensions, "extensions must not be null");
        renderedLines.clear();
        pluginMutationControlsPresent = false;
        removeAll();
        add(new H2("Extension Governance"));
        add(new Span("Extension sources and capabilities are inspect-only; enable/disable remains configuration-driven."));
        if (extensions.sources().isEmpty()) {
            String text = "Extensions: UNCONFIGURED | sources=0 | mutation=disabled";
            renderedLines.add(text);
            Div empty = new Div(new H3("No extension sources"), new Span(text));
            empty.getElement().setAttribute("data-governance-area", "extensions");
            empty.getElement().setAttribute("data-read-only", "true");
            add(empty);
            return;
        }
        for (ExtensionSourceDto source : extensions.sources()) {
            addExtensionSource(source);
        }
    }

    public void showMcpGovernance(McpGovernanceResponse governance) {
        Objects.requireNonNull(governance, "governance must not be null");
        renderedLines.clear();
        pluginMutationControlsPresent = false;
        removeAll();
        add(new H2("MCP Governance"));
        add(new Span("Remote MCP server and tool status is inspect-only; server configuration CRUD remains disabled."));
        Button refresh = new Button("Refresh MCP discovery");
        refresh.getElement().setAttribute("data-action-plan", "POST");
        refresh.getElement().setAttribute("data-action-path", mcpRefreshPath());
        refresh.getElement().setAttribute("data-read-only-refresh", "true");
        add(refresh);
        renderedLines.add(mcpRefreshActionText());
        if (governance.servers().isEmpty()) {
            String text = "MCP: UNCONFIGURED | servers=0 | mutation=disabled";
            renderedLines.add(text);
            Div empty = new Div(new H3("No MCP servers"), new Span(text));
            empty.getElement().setAttribute("data-governance-area", "mcp");
            empty.getElement().setAttribute("data-read-only", "true");
            add(empty);
            return;
        }
        for (McpServerDto server : governance.servers()) {
            addMcpServer(server);
        }
    }

    public void showPlugins(PluginGovernanceResponse governance) {
        Objects.requireNonNull(governance, "governance must not be null");
        renderedLines.clear();
        pluginMutationControlsPresent = true;
        removeAll();
        add(new H2("Plugin Governance"));
        Span warning = new Span(PLUGIN_NON_SANDBOX_WARNING);
        warning.getElement().setAttribute("data-plugin-warning", "not-a-sandbox");
        add(warning);
        renderedLines.add(PLUGIN_NON_SANDBOX_WARNING);

        Button refresh = new Button("Refresh plugin discovery");
        refresh.getElement().setAttribute("data-action-plan", "POST");
        refresh.getElement().setAttribute("data-action-path", pluginRefreshPath());
        refresh.getElement().setAttribute("data-plugin-action", "refresh");
        add(refresh);
        renderedLines.add(pluginRefreshActionText());

        if (governance.plugins().isEmpty()) {
            String text = "Plugins: UNCONFIGURED | plugins=0 | mutation=refresh-only";
            renderedLines.add(text);
            Div empty = new Div(new H3("No plugins"), new Span(text));
            empty.getElement().setAttribute("data-governance-area", "plugins");
            empty.getElement().setAttribute("data-read-only", "true");
            add(empty);
            return;
        }
        for (PluginSourceDto plugin : governance.plugins()) {
            addPlugin(plugin);
        }
    }

    public boolean mutationControlsPresent() {
        return pluginMutationControlsPresent;
    }

    public String mutationActionText() {
        return "";
    }

    public String renderedText() {
        return renderedLines.stream().collect(Collectors.joining("\n"));
    }

    private void renderEmpty() {
        renderedLines.clear();
        pluginMutationControlsPresent = false;
        removeAll();
        add(new H2("Registry and Integration Status"));
        Span empty = new Span("Registry status has not been loaded.");
        empty.getElement().setAttribute("data-state", "empty-registry-status");
        add(empty);
    }

    private void addStatus(String label, GovernanceStatusDto status) {
        String text = label + ": " + status.status()
                + " | count=" + status.count()
                + " | " + status.message()
                + " | " + metadata(status.metadata());
        renderedLines.add(text);
        Div card = new Div(new H3(label), new Span(text));
        card.getElement().setAttribute("data-governance-area", status.area());
        card.getElement().setAttribute("data-governance-status", status.status());
        card.getElement().setAttribute("data-read-only", "true");
        add(card);
    }

    private void addExtensionSource(ExtensionSourceDto source) {
        String text = "Extension Source: " + safe(source.sourceId())
                + " | name=" + safe(source.name())
                + " | kind=" + safe(source.kind())
                + " | status=" + safe(source.lifecycleStatus())
                + " | health=" + safe(source.healthStatus())
                + " | compatibility=" + safe(source.compatibilityStatus())
                + " | enabled=" + source.enabled()
                + " | capabilities=" + source.capabilities().size()
                + " | error=" + safe(source.redactedError());
        renderedLines.add(text);
        Div card = new Div(new H3(source.name()), new Span(text));
        card.getElement().setAttribute("data-governance-area", "extensions");
        card.getElement().setAttribute("data-extension-source", source.sourceId());
        card.getElement().setAttribute("data-extension-kind", source.kind());
        card.getElement().setAttribute("data-read-only", "true");
        add(card);
        for (ExtensionCapabilityDto capability : source.capabilities()) {
            addExtensionCapability(capability);
        }
    }

    private void addExtensionCapability(ExtensionCapabilityDto capability) {
        String text = "Capability: " + safe(capability.capabilityId())
                + " | type=" + safe(capability.type())
                + " | status=" + safe(capability.status())
                + " | health=" + safe(capability.healthStatus())
                + " | compatibility=" + safe(capability.compatibilityStatus())
                + " | enabled=" + capability.enabled()
                + " | " + metadata(capability.metadata());
        renderedLines.add(text);
        Div row = new Div(new Span(text));
        row.getElement().setAttribute("data-extension-capability", capability.capabilityId());
        row.getElement().setAttribute("data-capability-type", capability.type());
        row.getElement().setAttribute("data-read-only", "true");
        add(row);
    }

    private void addMcpServer(McpServerDto server) {
        String text = "MCP Server: " + safe(server.serverId())
                + " | name=" + safe(server.name())
                + " | enabled=" + server.enabled()
                + " | transport=" + safe(server.transport())
                + " | connection=" + safe(server.connectionStatus())
                + " | discovery=" + safe(server.discoveryStatus())
                + " | auth=" + safe(server.authSummary())
                + " | tools=" + server.toolCount()
                + " | lastRefresh=" + (server.lastRefreshedAt() == null ? "never" : server.lastRefreshedAt())
                + " | error=" + safe(server.redactedError())
                + " | " + metadata(server.metadata());
        renderedLines.add(text);
        Div card = new Div(new H3(server.name()), new Span(text));
        card.getElement().setAttribute("data-governance-area", "mcp");
        card.getElement().setAttribute("data-mcp-server", server.serverId());
        card.getElement().setAttribute("data-mcp-connection", server.connectionStatus());
        card.getElement().setAttribute("data-mcp-discovery", server.discoveryStatus());
        card.getElement().setAttribute("data-read-only", "true");
        add(card);
        for (McpToolDto tool : server.tools()) {
            addMcpTool(tool);
        }
    }

    private void addMcpTool(McpToolDto tool) {
        String text = "MCP Tool: " + safe(tool.serverQualifiedToolId())
                + " | name=" + safe(tool.mcpToolName())
                + " | availability=" + safe(tool.availabilityStatus())
                + " | readOnly=" + tool.readOnly()
                + " | destructive=" + tool.destructive()
                + " | openWorld=" + tool.openWorld()
                + " | schema=" + safe(tool.schemaSummary())
                + " | error=" + safe(tool.redactedError())
                + " | " + metadata(tool.metadata());
        renderedLines.add(text);
        Div row = new Div(new Span(text));
        row.getElement().setAttribute("data-mcp-tool", tool.serverQualifiedToolId());
        row.getElement().setAttribute("data-mcp-tool-availability", tool.availabilityStatus());
        row.getElement().setAttribute("data-read-only", "true");
        add(row);
    }

    private void addPlugin(PluginSourceDto plugin) {
        String text = "Plugin: " + safe(plugin.pluginId())
                + " | name=" + safe(plugin.name())
                + " | version=" + safe(plugin.version())
                + " | vendor=" + safe(plugin.vendor())
                + " | sourceKind=" + safe(plugin.sourceKind())
                + " | lifecycle=" + safe(plugin.lifecycleStatus())
                + " | enabled=" + plugin.enabled()
                + " | health=" + safe(plugin.healthStatus())
                + " | compatibility=" + safe(plugin.compatibilityStatus())
                + " | capabilities=" + plugin.capabilityCount()
                + " | capabilityStatusCounts=" + metadata(plugin.capabilityStatusCounts())
                + " | path=" + safe(plugin.relativePathSummary())
                + " | reason=" + safe(plugin.reason())
                + " | error=" + safe(plugin.redactedError())
                + " | lastUpdated=" + plugin.lastUpdatedAt()
                + " | " + metadata(plugin.metadata());
        renderedLines.add(text);
        Div card = new Div(new H3(plugin.name()), new Span(text));
        card.getElement().setAttribute("data-governance-area", "plugins");
        card.getElement().setAttribute("data-plugin-id", plugin.pluginId());
        card.getElement().setAttribute("data-plugin-lifecycle", plugin.lifecycleStatus());
        card.getElement().setAttribute("data-plugin-health", plugin.healthStatus());
        card.getElement().setAttribute("data-plugin-compatibility", plugin.compatibilityStatus());
        add(card);

        Button disable = new Button("Disable plugin");
        disable.getElement().setAttribute("data-action-plan", "POST");
        disable.getElement().setAttribute("data-action-path", httpClient.adminPluginDisablePath(plugin.pluginId()));
        disable.getElement().setAttribute("data-plugin-action", "disable");
        disable.getElement().setAttribute("data-confirmation-required", "true");
        disable.getElement().setAttribute("data-reason", "optional");
        add(disable);
        renderedLines.add(pluginDisableActionText(plugin.pluginId()));

        Button quarantine = new Button("Quarantine plugin");
        quarantine.getElement().setAttribute("data-action-plan", "POST");
        quarantine.getElement().setAttribute("data-action-path", httpClient.adminPluginQuarantinePath(plugin.pluginId()));
        quarantine.getElement().setAttribute("data-plugin-action", "quarantine");
        quarantine.getElement().setAttribute("data-confirmation-required", "true");
        quarantine.getElement().setAttribute("data-reason", "optional");
        add(quarantine);
        renderedLines.add(pluginQuarantineActionText(plugin.pluginId()));

        for (PluginCapabilityDto capability : plugin.capabilities()) {
            addPluginCapability(capability);
        }
    }

    private void addPluginCapability(PluginCapabilityDto capability) {
        String text = "Plugin Capability: " + safe(capability.capabilityId())
                + " | type=" + safe(capability.type())
                + " | status=" + safe(capability.status())
                + " | version=" + safe(capability.version())
                + " | plugin=" + safe(capability.pluginId())
                + " | enabled=" + capability.enabled()
                + " | compatibility=" + safe(capability.compatibilityStatus())
                + " | health=" + safe(capability.healthStatus())
                + " | " + metadata(capability.metadata());
        renderedLines.add(text);
        Div row = new Div(new Span(text));
        row.getElement().setAttribute("data-plugin-capability", capability.capabilityId());
        row.getElement().setAttribute("data-capability-type", capability.type());
        row.getElement().setAttribute("data-plugin-capability-status", capability.status());
        add(row);
    }

    private static String metadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "metadata=none";
        }
        return metadata.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> safe(entry.getKey()) + "=" + safe(entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
