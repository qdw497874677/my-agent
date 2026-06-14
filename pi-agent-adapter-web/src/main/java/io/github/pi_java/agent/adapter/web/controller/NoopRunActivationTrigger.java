package io.github.pi_java.agent.adapter.web.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class NoopRunActivationTrigger {

    @Bean
    RunController.RunActivationTrigger runActivationTrigger() {
        return () -> { };
    }
}
