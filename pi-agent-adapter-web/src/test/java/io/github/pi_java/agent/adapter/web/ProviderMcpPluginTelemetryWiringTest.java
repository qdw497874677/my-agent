package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.domain.model.StreamingModelClient;
import io.github.pi_java.agent.infrastructure.mcp.client.McpSecretHeaderResolver;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiProviderProperties;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiSpringAiModelFactory;
import io.github.pi_java.agent.infrastructure.observability.PiTelemetry;
import io.github.pi_java.agent.infrastructure.observability.TelemetryMcpGovernanceCatalog;
import io.github.pi_java.agent.infrastructure.observability.TelemetryPluginGovernanceCatalog;
import io.github.pi_java.agent.infrastructure.observability.TelemetryStreamingModelClient;
import io.github.pi_java.agent.infrastructure.plugin.InMemoryPluginStateStore;
import io.github.pi_java.agent.infrastructure.plugin.PluginRegistryProperties;
import io.github.pi_java.agent.infrastructure.plugin.Pf4jControlledPluginDiscoveryService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderMcpPluginTelemetryWiringTest {

    @Test
    void model_mcp_and_plugin_configuration_wraps_with_telemetry_when_available() {
        PiTelemetry telemetry = new PiTelemetry(new SimpleMeterRegistry(), null);
        ModelProviderBeanConfiguration modelConfig = new ModelProviderBeanConfiguration();
        OpenAiProviderProperties modelProperties = modelConfig.openAiProviderProperties(
                "openai-compatible", "https://example.invalid/v1", "/chat/completions", "gpt-test", "config:key");
        StreamingModelClient modelClient = modelConfig.openAiCompatibleStreamingModelClient(modelProperties,
                ref -> Optional.empty(), OpenAiSpringAiModelFactory.springAi(), Optional.of(telemetry));

        McpGovernanceBeanConfiguration mcpConfig = new McpGovernanceBeanConfiguration();
        McpSecretHeaderResolver secretResolver = mcpConfig.mcpSecretHeaderResolver(ref -> Optional.empty());
        var mcpProperties = new McpGovernanceBeanConfiguration.McpServersProperties(List.of(),
                new McpGovernanceBeanConfiguration.Discovery(false));
        var serverRegistry = mcpConfig.mcpServerRegistry(mcpProperties, secretResolver, Clock.systemUTC());
        var mapper = mcpConfig.mcpToolDescriptorMapper();
        McpGovernanceCatalog mcpCatalog = mcpConfig.mcpGovernanceCatalog(serverRegistry, mapper, Optional.of(telemetry));

        PluginGovernanceBeanConfiguration pluginConfig = new PluginGovernanceBeanConfiguration();
        PluginRegistryProperties pluginProperties = new PluginRegistryProperties(false, Optional.empty(), true, true,
                List.of(), List.of(), "1.0.0", false, false);
        var adapter = pluginConfig.pluginGovernanceCatalogAdapter(pluginProperties,
                new Pf4jControlledPluginDiscoveryService(pluginProperties), new InMemoryPluginStateStore(Clock.systemUTC()));
        PluginGovernanceCatalog pluginCatalog = pluginConfig.pluginGovernanceCatalog(adapter, Optional.of(telemetry));

        assertThat(modelClient).isInstanceOf(TelemetryStreamingModelClient.class);
        assertThat(mcpCatalog).isInstanceOf(TelemetryMcpGovernanceCatalog.class);
        assertThat(pluginCatalog).isInstanceOf(TelemetryPluginGovernanceCatalog.class);
    }
}
