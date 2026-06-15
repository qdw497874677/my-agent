package io.github.pi_java.agent.spring.autoconfigure;

import io.github.pi_java.agent.app.port.extension.ExtensionGovernanceCatalog;
import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.infrastructure.extension.DefaultExtensionContributionRegistry;
import io.github.pi_java.agent.infrastructure.extension.ExtensionGovernanceCatalogAdapter;
import io.github.pi_java.agent.infrastructure.extension.ExtensionModelProviderRegistry;
import io.github.pi_java.agent.infrastructure.extension.ExtensionRegistrationProperties;
import io.github.pi_java.agent.infrastructure.extension.ExtensionToolRegistry;
import io.github.pi_java.agent.infrastructure.extension.ServiceLoaderExtensionDiscovery;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PiAgentExtensionProperties.class)
@ConditionalOnProperty(prefix = "pi.extensions", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PiAgentExtensionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SpringExtensionSourceFactory springExtensionSourceFactory() {
        return new SpringExtensionSourceFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    ServiceLoaderExtensionDiscovery serviceLoaderExtensionDiscovery() {
        return new ServiceLoaderExtensionDiscovery();
    }

    @Bean
    @ConditionalOnMissingBean
    DefaultExtensionContributionRegistry extensionContributionRegistry(
            ServiceLoaderExtensionDiscovery discovery,
            SpringExtensionSourceFactory springSourceFactory,
            ObjectProvider<ExtensionSource> extensionSources,
            PiAgentExtensionProperties properties) {
        return DefaultExtensionContributionRegistry.build(
                discovery.discover(springSourceFactory.orderedSources(extensionSources)),
                new ExtensionRegistrationProperties(properties.getDisabledSources(), properties.getDisabledCapabilities(),
                        properties.isAllowDuplicateCapabilityOverrides(), properties.getPlatformApiVersion()));
    }

    @Bean
    @ConditionalOnMissingBean(ToolRegistry.class)
    ExtensionToolRegistry extensionToolRegistry(DefaultExtensionContributionRegistry contributions) {
        return new ExtensionToolRegistry(contributions);
    }

    @Bean
    @ConditionalOnMissingBean(ModelProviderRegistry.class)
    ExtensionModelProviderRegistry extensionModelProviderRegistry(DefaultExtensionContributionRegistry contributions) {
        return new ExtensionModelProviderRegistry(contributions);
    }

    @Bean
    @ConditionalOnMissingBean(ExtensionGovernanceCatalog.class)
    ExtensionGovernanceCatalogAdapter extensionGovernanceCatalog(DefaultExtensionContributionRegistry contributions) {
        return new ExtensionGovernanceCatalogAdapter(contributions);
    }
}
