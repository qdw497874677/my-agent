package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.usecase.AgentCatalogQueryService;
import io.github.pi_java.agent.app.usecase.DefaultAgentCatalogQueryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AgentCatalogBeanConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AgentCatalogQueryService agentCatalogQueryService() {
        return new DefaultAgentCatalogQueryService();
    }
}
