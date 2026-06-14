package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.CorrelationContext;
import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.context.SecurityPrincipalContext;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.CreateSessionRequest;
import io.github.pi_java.agent.client.session.SessionHistoryResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppUseCaseContractTest {

    @Test
    void requestContextRequiresTenantUserTraceAndCorrelation() {
        assertThatThrownBy(() -> new SecurityPrincipalContext(null, "user-1", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecurityPrincipalContext(" ", "user-1", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SecurityPrincipalContext("tenant-1", "", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorrelationContext(null, "corr-1", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorrelationContext("trace-1", " ", null))
                .isInstanceOf(IllegalArgumentException.class);

        RequestContext context = new RequestContext(
                new SecurityPrincipalContext("tenant-1", "user-1", Set.of("run:create")),
                new CorrelationContext("trace-1", "corr-1", "cause-1")
        );

        assertThat(context.tenantId()).isEqualTo("tenant-1");
        assertThat(context.userId()).isEqualTo("user-1");
        assertThat(context.traceId()).isEqualTo("trace-1");
        assertThat(context.correlationId()).isEqualTo("corr-1");
        assertThat(context.causationId()).isEqualTo("cause-1");
        assertThat(context.principal().authorities()).containsExactly("run:create");
    }

    @Test
    void runCommandServiceKeepsSessionCentricSignature() throws NoSuchMethodException {
        Method createRun = RunCommandService.class.getMethod(
                "createRun", RequestContext.class, String.class, CreateRunRequest.class);
        Method cancelRun = RunCommandService.class.getMethod(
                "cancelRun", RequestContext.class, String.class, String.class, CancelRunRequest.class);

        assertThat(createRun.getReturnType()).isEqualTo(RunResponse.class);
        assertThat(cancelRun.getReturnType()).isEqualTo(RunStatusResponse.class);
    }

    @Test
    void runQueryServiceExposesCloud03Queries() throws NoSuchMethodException {
        assertThat(RunQueryService.class.getMethod("getRunDetail", RequestContext.class, String.class, String.class).getReturnType())
                .isEqualTo(RunDetailResponse.class);
        assertThat(RunQueryService.class.getMethod("getRunStatus", RequestContext.class, String.class, String.class).getReturnType())
                .isEqualTo(RunStatusResponse.class);
        assertThat(RunQueryService.class.getMethod("listEvents", RequestContext.class, String.class, String.class, long.class, int.class).getReturnType())
                .isEqualTo(EventHistoryResponse.class);
        assertThat(RunQueryService.class.getMethod("listSteps", RequestContext.class, String.class, String.class, int.class).getReturnType())
                .isEqualTo(PageResponse.class);
        assertThat(RunQueryService.class.getMethod("listMessages", RequestContext.class, String.class, String.class, int.class).getReturnType())
                .isEqualTo(PageResponse.class);
        assertThat(RunQueryService.class.getMethod("listToolCalls", RequestContext.class, String.class, String.class, int.class).getReturnType())
                .isEqualTo(PageResponse.class);
        assertThat(RunQueryService.class.getMethod("getRunResult", RequestContext.class, String.class, String.class).getReturnType())
                .isEqualTo(RunResultResponse.class);
    }

    @Test
    void sessionUseCasesRequireRequestContext() throws NoSuchMethodException {
        assertThat(SessionCommandService.class.getMethod("createSession", RequestContext.class, CreateSessionRequest.class).getReturnType())
                .isEqualTo(SessionResponse.class);
        assertThat(SessionQueryService.class.getMethod("getSession", RequestContext.class, String.class).getReturnType())
                .isEqualTo(SessionResponse.class);
        assertThat(SessionQueryService.class.getMethod("getSessionHistory", RequestContext.class, String.class).getReturnType())
                .isEqualTo(SessionHistoryResponse.class);
    }
}
