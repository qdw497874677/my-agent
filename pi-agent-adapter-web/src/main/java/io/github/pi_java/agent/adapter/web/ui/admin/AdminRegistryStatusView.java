package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.client.admin.ExtensionCapabilityDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.ExtensionSourceDto;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import io.github.pi_java.agent.client.admin.McpGovernanceResponse;
import io.github.pi_java.agent.client.admin.McpServerDto;
import io.github.pi_java.agent.client.admin.McpToolDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Read-only provider/tool/extension/MCP/plugin registry status view for Admin Governance. */
@Route("admin/governance/registry")
@PageTitle("Pi Admin Registry Status")
public class AdminRegistryStatusView extends Div {

    private final ConsoleHttpClient httpClient;
    private final List<String> renderedLines = new ArrayList<>();

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

    public void showOverview(GovernanceOverviewResponse overview) {
        Objects.requireNonNull(overview, "overview must not be null");
        renderedLines.clear();
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

    public boolean mutationControlsPresent() {
        return false;
    }

    public String mutationActionText() {
        return "";
    }

    public String renderedText() {
        return renderedLines.stream().collect(Collectors.joining("\n"));
    }

    private void renderEmpty() {
        renderedLines.clear();
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
