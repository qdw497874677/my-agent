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
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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
        if (payload instanceof RunEventPayload.ToolLifecyclePayload) {
            return "tool.lifecycle";
        }
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
        if (payload instanceof RunEventPayload.ToolLifecyclePayload toolPayload) {
            return toolLifecyclePayload(toolPayload);
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (RecordComponent component : payload.getClass().getRecordComponents()) {
            try {
                Object value = component.getAccessor().invoke(payload);
                if (value != null) {
                    values.put(component.getName(), safeValue(value));
                }
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to map event payload component " + component.getName(), ex);
            }
        }
        return Map.copyOf(values);
    }

    private static Map<String, Object> toolLifecyclePayload(RunEventPayload.ToolLifecyclePayload payload) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("toolCallId", payload.toolCallId());
        values.put("toolId", payload.toolId());
        values.put("descriptorVersion", payload.descriptorVersion());
        values.put("provenance", provenance(payload.provenance()));
        values.put("redactedInputSummary", safeMap(payload.redactedInputSummary()));
        values.put("redactedOutputSummary", safeMap(payload.redactedOutputSummary()));
        payload.policyDecision().map(Enum::name).ifPresent(value -> values.put("policyDecision", value));
        payload.executionStatus().map(Enum::name).ifPresent(value -> values.put("executionStatus", value));
        payload.preview().map(RunEventDtoMapper::preview).ifPresent(value -> values.put("preview", value));
        payload.errorCategory().ifPresent(value -> values.put("errorCategory", value));
        return Map.copyOf(values);
    }

    private static Map<String, Object> provenance(ToolProvenance provenance) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sourceKind", provenance.sourceKind().name());
        values.put("sourceId", provenance.sourceId());
        values.put("bindingRef", provenance.bindingRef());
        Map<String, String> metadata = new LinkedHashMap<>();
        provenance.metadata().forEach((key, value) -> {
            if (!isSensitiveKey(key)) {
                metadata.put(key, value);
            }
        });
        values.put("metadata", Map.copyOf(metadata));
        return Map.copyOf(values);
    }

    private static Map<String, Object> preview(ProvisionPreview preview) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("previewId", preview.previewId());
        values.put("summary", preview.summary());
        values.put("impacts", preview.impacts().stream().sorted().toList());
        values.put("approvalRecommended", preview.approvalRecommended());
        values.put("redactedDetails", safeMap(preview.redactedDetails()));
        return Map.copyOf(values);
    }

    private static Map<String, Object> safeMap(Map<String, Object> values) {
        Map<String, Object> safe = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (!isSensitiveKey(key)) {
                Object safeValue = safeValue(value);
                if (safeValue != null) {
                    safe.put(key, safeValue);
                }
            }
        });
        return Map.copyOf(safe);
    }

    private static Object safeValue(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.map(RunEventDtoMapper::safeValue).orElse(null);
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof ToolProvenance provenance) {
            return provenance(provenance);
        }
        if (value instanceof ProvisionPreview preview) {
            return preview(preview);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            map.forEach((nestedKey, nestedValue) -> {
                String key = String.valueOf(nestedKey);
                if (!isSensitiveKey(key)) {
                    Object safeNestedValue = safeValue(nestedValue);
                    if (safeNestedValue != null) {
                        nested.put(key, safeNestedValue);
                    }
                }
            });
            return Map.copyOf(nested);
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase();
        return normalized.contains("secret")
                || normalized.contains("token")
                || normalized.contains("password")
                || normalized.contains("apikey")
                || normalized.contains("api_key")
                || normalized.contains("executor")
                || normalized.contains("class");
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
