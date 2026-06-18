package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.port.plugin.PluginGovernanceCatalog;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.infrastructure.extension.ExtensionRegistrationProperties;
import io.github.pi_java.agent.infrastructure.plugin.DynamicPluginToolRegistry;
import io.github.pi_java.agent.infrastructure.plugin.InMemoryPluginStateStore;
import io.github.pi_java.agent.infrastructure.plugin.PluginGovernanceCatalogAdapter;
import io.github.pi_java.agent.infrastructure.plugin.PluginRegistryProperties;
import io.github.pi_java.agent.infrastructure.plugin.PluginStateStore;
import io.github.pi_java.agent.infrastructure.plugin.Pf4jControlledPluginDiscoveryService;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PluginGovernanceBeanConfiguration.PluginProperties.class)
public class PluginGovernanceBeanConfiguration {

    @Bean
    PluginRegistryProperties pluginRegistryProperties(PluginProperties properties) {
        return properties.toInfrastructure().requireValid();
    }

    @Bean
    PluginStateStore pluginStateStore(Clock clock) {
        return new InMemoryPluginStateStore(clock);
    }

    @Bean
    Pf4jControlledPluginDiscoveryService pluginDiscoveryService(PluginRegistryProperties properties) {
        return new Pf4jControlledPluginDiscoveryService(properties);
    }

    @Bean
    PluginGovernanceCatalogAdapter pluginGovernanceCatalogAdapter(PluginRegistryProperties properties,
                                                                    Pf4jControlledPluginDiscoveryService discoveryService,
                                                                    PluginStateStore stateStore) {
        var discovered = discoveryService.discover();
        ExtensionRegistrationProperties registrationProperties = new ExtensionRegistrationProperties(
                Set.of(), Set.of(), properties.allowDuplicateOverrides(), properties.platformApiVersion());
        return new PluginGovernanceCatalogAdapter(discovered, stateStore,
                registrationProperties, properties, discoveryService::discover);
    }

    @Bean
    PluginGovernanceCatalog pluginGovernanceCatalog(PluginGovernanceCatalogAdapter adapter) {
        return new PluginGovernanceCatalog() {
            @Override
            public List<io.github.pi_java.agent.app.port.plugin.PluginSourceStatus> plugins() {
                return adapter.plugins();
            }

            @Override
            public io.github.pi_java.agent.app.port.plugin.PluginMutationStatus refresh() {
                return adapter.refresh();
            }

            @Override
            public io.github.pi_java.agent.app.port.plugin.PluginMutationStatus disable(String pluginId, String actor,
                                                                                       String reason) {
                return adapter.disable(pluginId, actor, reason);
            }

            @Override
            public io.github.pi_java.agent.app.port.plugin.PluginMutationStatus quarantine(String pluginId, String actor,
                                                                                          String reason) {
                return adapter.quarantine(pluginId, actor, reason);
            }
        };
    }

    @Bean
    @Qualifier("pluginToolRegistry")
    ToolRegistry pluginToolRegistry(PluginRegistryProperties properties,
                                    PluginGovernanceCatalogAdapter adapter) {
        if (!properties.enabled() || properties.pluginDirectory().isEmpty()) {
            return new EmptyToolRegistry();
        }
        return new DynamicPluginToolRegistry(adapter::contributionRegistry);
    }

    private record EmptyToolRegistry() implements ToolRegistry {
        @Override
        public List<io.github.pi_java.agent.domain.tool.ToolDescriptor> listTools() {
            return List.of();
        }

        @Override
        public Optional<ToolResolution> resolve(String toolId) {
            return Optional.empty();
        }
    }

    @ConfigurationProperties(prefix = "pi.plugins")
    public record PluginProperties(
            Boolean enabled,
            Path directory,
            Boolean startupDiscovery,
            Boolean manualRefreshEnabled,
            List<String> allowlist,
            List<String> selected,
            String platformApiVersion,
            Boolean allowDuplicateOverrides,
            Boolean nonSandboxWarningAcknowledged
    ) {
        public PluginRegistryProperties toInfrastructure() {
            return new PluginRegistryProperties(
                    Boolean.TRUE.equals(enabled),
                    Optional.ofNullable(directory),
                    startupDiscovery == null || startupDiscovery,
                    manualRefreshEnabled == null || manualRefreshEnabled,
                    allowlist == null ? List.of() : allowlist,
                    selected == null ? List.of() : selected,
                    platformApiVersion == null || platformApiVersion.isBlank() ? "1.0.0" : platformApiVersion,
                    Boolean.TRUE.equals(allowDuplicateOverrides),
                    Boolean.TRUE.equals(nonSandboxWarningAcknowledged));
        }
    }
}
