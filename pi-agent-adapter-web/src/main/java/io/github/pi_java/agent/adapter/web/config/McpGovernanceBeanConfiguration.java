package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.port.mcp.McpGovernanceCatalog;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.infrastructure.mcp.client.McpSecretHeaderResolver;
import io.github.pi_java.agent.infrastructure.mcp.config.McpAuthProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.github.pi_java.agent.infrastructure.mcp.config.McpTransportKind;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpGovernanceCatalogAdapter;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpServerRegistry;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpToolDescriptorMapper;
import io.github.pi_java.agent.infrastructure.mcp.registry.McpToolRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(McpGovernanceBeanConfiguration.McpServersProperties.class)
public class McpGovernanceBeanConfiguration {

    @Bean
    McpSecretHeaderResolver mcpSecretHeaderResolver(SecretResolver secretResolver) {
        return McpSecretHeaderResolver.from(secretResolver::resolve);
    }

    @Bean
    McpServerRegistry mcpServerRegistry(McpServersProperties properties,
                                        McpSecretHeaderResolver secretResolver,
                                        Clock clock) {
        return new McpServerRegistry(properties.toInfrastructureServers(), secretResolver, clock);
    }

    @Bean
    McpToolDescriptorMapper mcpToolDescriptorMapper() {
        return new McpToolDescriptorMapper();
    }

    @Bean
    McpToolRegistry mcpToolRegistry(McpServerRegistry serverRegistry,
                                    McpToolDescriptorMapper mapper,
                                    McpSecretHeaderResolver secretResolver,
                                    McpServersProperties properties) {
        McpToolRegistry registry = new McpToolRegistry(serverRegistry, mapper, secretResolver);
        if (properties.discovery().startup()) {
            serverRegistry.refresh();
        }
        return registry;
    }

    @Bean
    McpGovernanceCatalog mcpGovernanceCatalog(McpServerRegistry serverRegistry,
                                              McpToolDescriptorMapper mapper) {
        return new McpGovernanceCatalogAdapter(serverRegistry, mapper);
    }

    @ConfigurationProperties(prefix = "pi.mcp")
    public record McpServersProperties(List<Server> servers, Discovery discovery) {
        public McpServersProperties {
            servers = servers == null ? List.of() : List.copyOf(servers);
            discovery = discovery == null ? new Discovery(false) : discovery;
        }

        List<McpServerProperties> toInfrastructureServers() {
            return servers.stream().map(Server::toInfrastructure).toList();
        }
    }

    public record Discovery(boolean startup) {
    }

    public record Server(
            String id,
            Boolean enabled,
            String displayName,
            McpTransportKind transport,
            String baseUrl,
            String endpoint,
            String command,
            List<String> args,
            Map<String, String> envSecretRefs,
            Duration timeout,
            Auth auth,
            Map<String, String> metadata
    ) {
        McpServerProperties toInfrastructure() {
            return new McpServerProperties(
                    id,
                    enabled == null || enabled,
                    displayName,
                    transport,
                    baseUrl,
                    endpoint,
                    command,
                    Optional.ofNullable(args).orElseGet(List::of),
                    Optional.ofNullable(envSecretRefs).orElseGet(Map::of),
                    timeout,
                    auth == null ? McpAuthProperties.none() : auth.toInfrastructure(),
                    Optional.ofNullable(metadata).orElseGet(Map::of));
        }
    }

    public record Auth(
            String credentialRef,
            String bearerTokenRef,
            String apiKeyHeaderName,
            String apiKeySecretRef,
            Map<String, String> customHeaderSecretRefs
    ) {
        McpAuthProperties toInfrastructure() {
            return new McpAuthProperties(
                    credentialRef,
                    bearerTokenRef,
                    apiKeyHeaderName,
                    apiKeySecretRef,
                    Optional.ofNullable(customHeaderSecretRefs).orElseGet(Map::of));
        }
    }
}
