package io.github.pi_java.agent.adapter.web.config;

import io.github.pi_java.agent.app.port.persistence.AuditRepository;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.usecase.DefaultApprovalService;
import io.github.pi_java.agent.domain.event.EventSink;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApprovalBeanConfiguration {

    @Bean(name = {"defaultApprovalService", "approvalQueryService", "approvalCommandService"})
    DefaultApprovalService defaultApprovalService(RunEventStore runEventStore, AuditRepository auditRepository,
                                                  EventSink eventSink, Clock clock) {
        return new DefaultApprovalService(runEventStore, auditRepository, eventSink, clock);
    }
}
