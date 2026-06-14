package io.github.pi_java.agent.adapter.web.mapper;

import io.github.pi_java.agent.client.event.RedactionDto;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.domain.common.PlatformIds.CausationId;
import io.github.pi_java.agent.domain.common.PlatformIds.CorrelationId;
import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.SessionId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.common.PlatformIds.TenantId;
import io.github.pi_java.agent.domain.common.PlatformIds.TraceId;
import io.github.pi_java.agent.domain.common.PlatformIds.UserId;
import io.github.pi_java.agent.domain.common.PlatformIds.WorkspaceId;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RunEventDtoMapper {

    private RunEventDtoMapper() {
    }

    public static RunEventDto toDto(RunEvent event) {
        String payloadSchema = payloadSchema(event.payload());
        int payloadVersion = payloadVersion(event.payload());
        return new RunEventDto(
                event.eventId(),
                value(event.tenantId()),
                value(event.userId()),
                value(event.sessionId()),
                value(event.runId()),
                value(event.stepId()),
                value(event.workspaceId()),
                event.sequence(),
                event.timestamp(),
                event.type().wireName(),
                value(event.traceId()),
                value(event.correlationId()),
                value(event.causationId()),
                event.visibility().name(),
                new RedactionDto(event.redaction().redacted(), event.redaction().redactedFields().stream().sorted().toList(), event.redaction().policyRef()),
                payloadSchema,
                payloadVersion,
                payload(event.payload()));
    }

    private static String payloadSchema(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            return extensionPayload.schema();
        }
        return payload.getClass().getSimpleName();
    }

    private static int payloadVersion(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            try {
                return Integer.parseInt(extensionPayload.version());
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private static Map<String, Object> payload(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            return extensionPayload.attributes();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (RecordComponent component : payload.getClass().getRecordComponents()) {
            try {
                values.put(component.getName(), component.getAccessor().invoke(payload));
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to map event payload component " + component.getName(), ex);
            }
        }
        return Map.copyOf(values);
    }

    private static String value(TenantId id) {
        return id.value();
    }

    private static String value(UserId id) {
        return id.value();
    }

    private static String value(SessionId id) {
        return id.value();
    }

    private static String value(RunId id) {
        return id.value();
    }

    private static String value(StepId id) {
        return id.value();
    }

    private static String value(WorkspaceId id) {
        return id.value();
    }

    private static String value(TraceId id) {
        return id.value();
    }

    private static String value(CorrelationId id) {
        return id.value();
    }

    private static String value(CausationId id) {
        return id.value();
    }
}
