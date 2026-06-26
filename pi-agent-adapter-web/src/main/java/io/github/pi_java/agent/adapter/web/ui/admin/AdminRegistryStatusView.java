package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.PiPageSection;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Read-only provider/tool/extension/MCP/plugin registry status view for Admin Governance. */
@Route(value = "admin/governance/registry", layout = PiResponsiveShell.class)
@PageTitle("Pi Admin Registry Status")
public class AdminRegistryStatusView extends Div {

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
        add(new H2(getTranslation("admin.registry.title")));
        add(new Span(getTranslation("admin.registry.description")));
        addStatus(getTranslation("admin.registry.modelProviders"), overview.providers());
        addStatus(getTranslation("admin.registry.toolRegistry"), overview.toolRegistry());
        addStatus(getTranslation("admin.overview.extensions"), overview.extensions());
        addStatus(getTranslation("admin.overview.mcp"), overview.mcp());
        addStatus(getTranslation("admin.overview.plugins"), overview.plugins());
    }

    public void showExtensions(ExtensionGovernanceResponse extensions) {
        Objects.requireNonNull(extensions, "extensions must not be null");
        renderedLines.clear();
        pluginMutationControlsPresent = false;
        removeAll();
        add(new H2(getTranslation("admin.registry.extensionTitle")));
        add(new Span(getTranslation("admin.registry.extensionDesc")));
        if (extensions.sources().isEmpty()) {
            String text = "Extensions: UNCONFIGURED | sources=0 | mutation=disabled";
            renderedLines.add(text);
            Div empty = new Div(new H3(getTranslation("admin.registry.noExtensions")), new Span(text));
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
        add(new H2(getTranslation("admin.registry.mcpTitle")));
        add(new Span(getTranslation("admin.registry.mcpDesc")));
        Button refresh = new Button(getTranslation("admin.registry.refreshMcp"));
        refresh.getElement().setAttribute("data-action-plan", "POST");
        refresh.getElement().setAttribute("data-action-path", mcpRefreshPath());
        refresh.getElement().setAttribute("data-read-only-refresh", "true");
        add(refresh);
        renderedLines.add(mcpRefreshActionText());
        if (governance.servers().isEmpty()) {
            String text = "MCP: UNCONFIGURED | servers=0 | mutation=disabled";
            renderedLines.add(text);
            Div empty = new Div(new H3(getTranslation("admin.registry.noMcp")), new Span(text));
            empty.getElement().setAttribute("data-governance-area", "mcp");
            empty.getElement().setAttribute("data-read-only", "true");
            add(empty);
            return;
        }
        for (McpServerDto server : governance.servers().stream()
                .sorted(Comparator.comparingInt((McpServerDto server) -> abnormalRank(server)).reversed())
                .toList()) {
            addMcpServer(server);
        }
    }

    public void showPlugins(PluginGovernanceResponse governance) {
        Objects.requireNonNull(governance, "governance must not be null");
        renderedLines.clear();
        pluginMutationControlsPresent = true;
        removeAll();
        add(new H2(getTranslation("admin.registry.pluginTitle")));
        String pluginWarning = getTranslation("admin.registry.pluginWarning");
        Span warning = new Span(pluginWarning);
        warning.getElement().setAttribute("data-plugin-warning", "not-a-sandbox");
        add(warning);
        renderedLines.add(pluginWarning);

        Button refresh = new Button(getTranslation("admin.registry.refreshPlugins"));
        refresh.getElement().setAttribute("data-action-plan", "POST");
        refresh.getElement().setAttribute("data-action-path", pluginRefreshPath());
        refresh.getElement().setAttribute("data-plugin-action", "refresh");
        add(refresh);
        renderedLines.add(pluginRefreshActionText());

        if (governance.plugins().isEmpty()) {
            String text = "Plugins: UNCONFIGURED | plugins=0 | mutation=refresh-only";
            renderedLines.add(text);
            Div empty = new Div(new H3(getTranslation("admin.registry.noPlugins")), new Span(text));
            empty.getElement().setAttribute("data-governance-area", "plugins");
            empty.getElement().setAttribute("data-read-only", "true");
            add(empty);
            return;
        }
        for (PluginSourceDto plugin : governance.plugins().stream()
                .sorted(Comparator.comparingInt((PluginSourceDto plugin) -> abnormalRank(plugin)).reversed())
                .toList()) {
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
        add(new H2(getTranslation("admin.registry.title")));
        Span empty = new Span(getTranslation("admin.registry.empty"));
        empty.getElement().setAttribute("data-state", "empty-registry-status");
        add(empty);
    }

    private void addStatus(String label, GovernanceStatusDto status) {
        String text = label + ": " + status.status()
                + " | count=" + status.count()
                + " | " + status.message()
                + " | " + metadata(status.metadata());
        renderedLines.add(text);
        PiPageSection card = AdminMobileCardSupport.statusCard(
                registrySection(status.area()),
                label,
                status.status(),
                String.valueOf(status.count()),
                status.message(),
                AdminMobileCardSupport.metadataDetails(status.metadata()));
        card.getElement().setAttribute("data-admin-registry-section", registrySection(status.area()));
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
        Div summary = new Div(
                AdminMobileCardSupport.labelValue("sourceId", source.sourceId()),
                AdminMobileCardSupport.labelValue("name", source.name()),
                AdminMobileCardSupport.labelValue("kind", source.kind()),
                AdminMobileCardSupport.labelValue("lifecycleStatus", source.lifecycleStatus()),
                AdminMobileCardSupport.labelValue("healthStatus", source.healthStatus()),
                AdminMobileCardSupport.labelValue("compatibilityStatus", source.compatibilityStatus()),
                AdminMobileCardSupport.labelValue("enabled", String.valueOf(source.enabled())),
                AdminMobileCardSupport.labelValue("capabilities", String.valueOf(source.capabilities().size())),
                AdminMobileCardSupport.statusChip(source.lifecycleStatus()),
                AdminMobileCardSupport.statusChip(source.healthStatus()),
                AdminMobileCardSupport.statusChip(source.compatibilityStatus()));
        summary.addClassName("pi-admin-card-summary");
        PiPageSection card = PiPageSection.card("extension-source", new H3(source.name()), summary,
                AdminMobileCardSupport.details(getTranslation("admin.registry.sourceDiagnostics"), "structured",
                        AdminMobileCardSupport.labelValue("redactedError", source.redactedError()),
                        AdminMobileCardSupport.labelValue("capabilityCount", String.valueOf(source.capabilities().size()))));
        card.addClassNames("pi-admin-card", "pi-admin-nested-card");
        card.getElement().setAttribute("data-admin-card", "true");
        card.getElement().setAttribute("data-admin-registry-section", "extensions");
        card.getElement().setAttribute("data-extension-source-card", source.sourceId());
        card.getElement().setAttribute("data-governance-area", "extensions");
        card.getElement().setAttribute("data-extension-source", source.sourceId());
        card.getElement().setAttribute("data-extension-kind", source.kind());
        card.getElement().setAttribute("data-status-severity", abnormalRank(source) > 0 ? "abnormal" : "normal");
        card.getElement().setAttribute("data-read-only", "true");
        for (ExtensionCapabilityDto capability : source.capabilities()) {
            card.add(addExtensionCapability(capability));
        }
        add(card);
    }

    private Component addExtensionCapability(ExtensionCapabilityDto capability) {
        String text = "Capability: " + safe(capability.capabilityId())
                + " | type=" + safe(capability.type())
                + " | status=" + safe(capability.status())
                + " | health=" + safe(capability.healthStatus())
                + " | compatibility=" + safe(capability.compatibilityStatus())
                + " | enabled=" + capability.enabled()
                + " | " + metadata(capability.metadata());
        renderedLines.add(text);
        Div summary = new Div(
                AdminMobileCardSupport.labelValue("capabilityId", capability.capabilityId()),
                AdminMobileCardSupport.labelValue("type", capability.type()),
                AdminMobileCardSupport.labelValue("status", capability.status()),
                AdminMobileCardSupport.labelValue("health", capability.healthStatus()),
                AdminMobileCardSupport.labelValue("compatibility", capability.compatibilityStatus()),
                AdminMobileCardSupport.labelValue("enabled", String.valueOf(capability.enabled())),
                AdminMobileCardSupport.statusChip(capability.status()),
                AdminMobileCardSupport.statusChip(capability.healthStatus()),
                AdminMobileCardSupport.statusChip(capability.compatibilityStatus()));
        summary.addClassName("pi-admin-card-summary");
        PiPageSection row = PiPageSection.card("extension-capability", new H3(capability.capabilityId()), summary,
                AdminMobileCardSupport.metadataDetails(capability.metadata()));
        row.addClassNames("pi-admin-card", "pi-admin-child-card");
        row.getElement().setAttribute("data-admin-card", "true");
        row.getElement().setAttribute("data-extension-capability-card", capability.capabilityId());
        row.getElement().setAttribute("data-extension-capability", capability.capabilityId());
        row.getElement().setAttribute("data-capability-type", capability.type());
        row.getElement().setAttribute("data-status-severity", abnormalRank(capability) > 0 ? "abnormal" : "normal");
        row.getElement().setAttribute("data-read-only", "true");
        return row;
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
        Div summary = new Div(
                AdminMobileCardSupport.labelValue("serverId", server.serverId()),
                AdminMobileCardSupport.labelValue("name", server.name()),
                AdminMobileCardSupport.labelValue("enabled", String.valueOf(server.enabled())),
                AdminMobileCardSupport.labelValue("transport", server.transport()),
                AdminMobileCardSupport.labelValue("auth", server.authSummary()),
                AdminMobileCardSupport.labelValue("tools", String.valueOf(server.toolCount())),
                AdminMobileCardSupport.labelValue("lastRefresh", server.lastRefreshedAt() == null ? "never" : server.lastRefreshedAt().toString()),
                AdminMobileCardSupport.labelValue("message", server.redactedError()),
                AdminMobileCardSupport.statusChip(server.connectionStatus()),
                AdminMobileCardSupport.statusChip(server.discoveryStatus()));
        summary.addClassName("pi-admin-card-summary");
        PiPageSection card = PiPageSection.card("mcp-server", new H3(server.name()), summary,
                AdminMobileCardSupport.metadataDetails(server.metadata()));
        card.addClassNames("pi-admin-card", "pi-admin-nested-card");
        card.getElement().setAttribute("data-admin-card", "true");
        card.getElement().setAttribute("data-admin-registry-section", "mcp");
        card.getElement().setAttribute("data-mcp-server-card", server.serverId());
        card.getElement().setAttribute("data-governance-area", "mcp");
        card.getElement().setAttribute("data-mcp-server", server.serverId());
        card.getElement().setAttribute("data-mcp-connection", server.connectionStatus());
        card.getElement().setAttribute("data-mcp-discovery", server.discoveryStatus());
        card.getElement().setAttribute("data-status-severity", abnormalRank(server) > 0 ? "abnormal" : "normal");
        card.getElement().setAttribute("data-read-only", "true");
        for (McpToolDto tool : server.tools()) {
            card.add(addMcpTool(tool));
        }
        add(card);
    }

    private Component addMcpTool(McpToolDto tool) {
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
        Div summary = new Div(
                AdminMobileCardSupport.labelValue("toolId", tool.serverQualifiedToolId()),
                AdminMobileCardSupport.labelValue("name", tool.mcpToolName()),
                AdminMobileCardSupport.labelValue("availability", tool.availabilityStatus()),
                AdminMobileCardSupport.labelValue("readOnly", String.valueOf(tool.readOnly())),
                AdminMobileCardSupport.labelValue("destructive", String.valueOf(tool.destructive())),
                AdminMobileCardSupport.labelValue("openWorld", String.valueOf(tool.openWorld())),
                AdminMobileCardSupport.labelValue("schema", tool.schemaSummary()),
                AdminMobileCardSupport.labelValue("redactedError", tool.redactedError()),
                AdminMobileCardSupport.statusChip(tool.availabilityStatus()));
        summary.addClassName("pi-admin-card-summary");
        PiPageSection row = PiPageSection.card("mcp-tool", new H3(tool.mcpToolName()), summary,
                AdminMobileCardSupport.metadataDetails(tool.metadata()));
        row.addClassNames("pi-admin-card", "pi-admin-child-card");
        row.getElement().setAttribute("data-admin-card", "true");
        row.getElement().setAttribute("data-mcp-tool-card", tool.serverQualifiedToolId());
        row.getElement().setAttribute("data-mcp-tool", tool.serverQualifiedToolId());
        row.getElement().setAttribute("data-mcp-tool-availability", tool.availabilityStatus());
        row.getElement().setAttribute("data-status-severity", abnormalRank(tool) > 0 ? "abnormal" : "normal");
        row.getElement().setAttribute("data-read-only", "true");
        return row;
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
        Div summary = new Div(
                AdminMobileCardSupport.labelValue("pluginId", plugin.pluginId()),
                AdminMobileCardSupport.labelValue("name", plugin.name()),
                AdminMobileCardSupport.labelValue("version", plugin.version()),
                AdminMobileCardSupport.labelValue("vendor", plugin.vendor()),
                AdminMobileCardSupport.labelValue("sourceKind", plugin.sourceKind()),
                AdminMobileCardSupport.labelValue("lifecycle", plugin.lifecycleStatus()),
                AdminMobileCardSupport.labelValue("enabled", String.valueOf(plugin.enabled())),
                AdminMobileCardSupport.labelValue("health", plugin.healthStatus()),
                AdminMobileCardSupport.labelValue("compatibility", plugin.compatibilityStatus()),
                AdminMobileCardSupport.labelValue("capabilities", String.valueOf(plugin.capabilityCount())),
                AdminMobileCardSupport.labelValue("reason", plugin.reason()),
                AdminMobileCardSupport.labelValue("error", plugin.redactedError()),
                AdminMobileCardSupport.labelValue("lastUpdated", plugin.lastUpdatedAt() == null ? "unknown" : plugin.lastUpdatedAt().toString()),
                AdminMobileCardSupport.statusChip(plugin.lifecycleStatus()),
                AdminMobileCardSupport.statusChip(plugin.healthStatus()),
                AdminMobileCardSupport.statusChip(plugin.compatibilityStatus()));
        summary.addClassName("pi-admin-card-summary");
        PiPageSection card = PiPageSection.card("plugin", new H3(plugin.name()), summary,
                AdminMobileCardSupport.details(getTranslation("admin.registry.pluginDiagnostics"), "structured",
                        AdminMobileCardSupport.labelValue("capabilityStatusCounts", metadata(plugin.capabilityStatusCounts())),
                        AdminMobileCardSupport.labelValue("path", plugin.relativePathSummary()),
                        AdminMobileCardSupport.metadataDetails(plugin.metadata())));
        card.addClassNames("pi-admin-card", "pi-admin-nested-card");
        card.getElement().setAttribute("data-admin-card", "true");
        card.getElement().setAttribute("data-admin-registry-section", "plugins");
        card.getElement().setAttribute("data-plugin-card", plugin.pluginId());
        card.getElement().setAttribute("data-governance-area", "plugins");
        card.getElement().setAttribute("data-plugin-id", plugin.pluginId());
        card.getElement().setAttribute("data-plugin-lifecycle", plugin.lifecycleStatus());
        card.getElement().setAttribute("data-plugin-health", plugin.healthStatus());
        card.getElement().setAttribute("data-plugin-compatibility", plugin.compatibilityStatus());
        card.getElement().setAttribute("data-status-severity", abnormalRank(plugin) > 0 ? "abnormal" : "normal");

        Button disable = new Button(getTranslation("admin.registry.disablePlugin"));
        disable.getElement().setAttribute("data-action-plan", "POST");
        disable.getElement().setAttribute("data-action-path", httpClient.adminPluginDisablePath(plugin.pluginId()));
        disable.getElement().setAttribute("data-plugin-action", "disable");
        disable.getElement().setAttribute("data-confirmation-required", "true");
        disable.getElement().setAttribute("data-reason", "optional");
        renderedLines.add(pluginDisableActionText(plugin.pluginId()));

        Button quarantine = new Button(getTranslation("admin.registry.quarantinePlugin"));
        quarantine.getElement().setAttribute("data-action-plan", "POST");
        quarantine.getElement().setAttribute("data-action-path", httpClient.adminPluginQuarantinePath(plugin.pluginId()));
        quarantine.getElement().setAttribute("data-plugin-action", "quarantine");
        quarantine.getElement().setAttribute("data-confirmation-required", "true");
        quarantine.getElement().setAttribute("data-reason", "optional");
        renderedLines.add(pluginQuarantineActionText(plugin.pluginId()));
        card.add(AdminMobileCardSupport.actionRow(disable, quarantine));

        for (PluginCapabilityDto capability : plugin.capabilities()) {
            card.add(addPluginCapability(capability));
        }
        add(card);
    }

    private Component addPluginCapability(PluginCapabilityDto capability) {
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
        Div summary = new Div(
                AdminMobileCardSupport.labelValue("capabilityId", capability.capabilityId()),
                AdminMobileCardSupport.labelValue("type", capability.type()),
                AdminMobileCardSupport.labelValue("status", capability.status()),
                AdminMobileCardSupport.labelValue("version", capability.version()),
                AdminMobileCardSupport.labelValue("plugin", capability.pluginId()),
                AdminMobileCardSupport.labelValue("enabled", String.valueOf(capability.enabled())),
                AdminMobileCardSupport.labelValue("compatibility", capability.compatibilityStatus()),
                AdminMobileCardSupport.labelValue("health", capability.healthStatus()),
                AdminMobileCardSupport.statusChip(capability.status()),
                AdminMobileCardSupport.statusChip(capability.healthStatus()),
                AdminMobileCardSupport.statusChip(capability.compatibilityStatus()));
        summary.addClassName("pi-admin-card-summary");
        PiPageSection row = PiPageSection.card("plugin-capability", new H3(capability.capabilityId()), summary,
                AdminMobileCardSupport.metadataDetails(capability.metadata()));
        row.addClassNames("pi-admin-card", "pi-admin-child-card");
        row.getElement().setAttribute("data-admin-card", "true");
        row.getElement().setAttribute("data-plugin-capability-card", capability.capabilityId());
        row.getElement().setAttribute("data-plugin-capability", capability.capabilityId());
        row.getElement().setAttribute("data-capability-type", capability.type());
        row.getElement().setAttribute("data-plugin-capability-status", capability.status());
        row.getElement().setAttribute("data-status-severity", abnormalRank(capability) > 0 ? "abnormal" : "normal");
        return row;
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

    private static String registrySection(String area) {
        String normalized = safe(area).toLowerCase();
        if (normalized.contains("extension")) {
            return "extensions";
        }
        if (normalized.contains("mcp")) {
            return "mcp";
        }
        if (normalized.contains("plugin")) {
            return "plugins";
        }
        return "registry";
    }

    private static int abnormalRank(ExtensionSourceDto source) {
        return abnormalRank(source.lifecycleStatus(), source.healthStatus(), source.compatibilityStatus(), source.redactedError(), null);
    }

    private static int abnormalRank(ExtensionCapabilityDto capability) {
        return abnormalRank(capability.status(), capability.healthStatus(), capability.compatibilityStatus(), null, null);
    }

    private static int abnormalRank(McpServerDto server) {
        return abnormalRank(server.connectionStatus(), server.discoveryStatus(), null, server.redactedError(), null);
    }

    private static int abnormalRank(McpToolDto tool) {
        return abnormalRank(tool.availabilityStatus(), null, null, tool.redactedError(), null);
    }

    private static int abnormalRank(PluginSourceDto plugin) {
        return abnormalRank(plugin.lifecycleStatus(), plugin.healthStatus(), plugin.compatibilityStatus(), plugin.redactedError(), plugin.reason());
    }

    private static int abnormalRank(PluginCapabilityDto capability) {
        return abnormalRank(capability.status(), capability.healthStatus(), capability.compatibilityStatus(), null, null);
    }

    private static int abnormalRank(String status, String health, String compatibility, String redactedError, String reason) {
        String joined = String.join(" ", safe(status), safe(health), safe(compatibility), safe(redactedError), safe(reason)).toUpperCase();
        if (!safe(redactedError).equals("unknown") || !safe(reason).equals("unknown")) {
            return 2;
        }
        return joined.contains("UNHEALTHY")
                || joined.contains("FAILED")
                || joined.contains("DOWN")
                || joined.contains("WARN")
                || joined.contains("DISCONNECTED")
                || joined.contains("DISABLED")
                || joined.contains("QUARANTINED")
                || joined.contains("INCOMPATIBLE") ? 1 : 0;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
