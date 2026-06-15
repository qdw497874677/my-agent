package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.app.port.tool.ToolRegistry;
import io.github.pi_java.agent.app.usecase.DefaultGovernanceQueryService;
import io.github.pi_java.agent.app.usecase.GovernanceQueryService;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.time.Clock;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GovernanceBeanConfiguration {

    @Bean
    @ConditionalOnMissingBean
    GovernanceQueryService governanceQueryService(
            ModelProviderRegistry modelProviderRegistry,
            ToolRegistry toolRegistry,
            Optional<AgentRuntime> agentRuntime,
            Clock clock) {
        return new DefaultGovernanceQueryService(modelProviderRegistry, toolRegistry, agentRuntime, clock);
    }
}
