package io.github.pi_java.agent.adapter.web.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@Configuration(proxyBeanMethods = false)
class NoopRunActivationTrigger {

    @Bean
    @ConditionalOnMissingBean(RunController.RunActivationTrigger.class)
    RunController.RunActivationTrigger runActivationTrigger() {
        return () -> { };
    }
}
