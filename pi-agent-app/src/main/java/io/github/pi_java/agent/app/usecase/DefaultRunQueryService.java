package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.app.context.RequestContext;
import io.github.pi_java.agent.app.port.persistence.RunEventStore;
import io.github.pi_java.agent.app.port.persistence.RunProjectionRepository;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.event.RedactionDto;
import io.github.pi_java.agent.client.event.RunEventDto;
import io.github.pi_java.agent.client.run.RunDetailResponse;
import io.github.pi_java.agent.client.run.RunResultResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.domain.event.RedactionMetadata;
import io.github.pi_java.agent.domain.event.RunEvent;
import io.github.pi_java.agent.domain.event.RunEventPayload;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.tool.ToolProvenance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DefaultRunQueryService implements RunQueryService {

    private final RunProjectionRepository runProjectionRepository;
    private final RunEventStore runEventStore;

    public DefaultRunQueryService(RunProjectionRepository runProjectionRepository, RunEventStore runEventStore) {
        this.runProjectionRepository = Objects.requireNonNull(runProjectionRepository, "runProjectionRepository must not be null");
        this.runEventStore = Objects.requireNonNull(runEventStore, "runEventStore must not be null");
    }

    @Override
    public RunDetailResponse getRunDetail(RequestContext context, String sessionId, String runId) {
        return runProjectionRepository.getRunDetail(context, sessionId, runId);
    }

    @Override
    public RunStatusResponse getRunStatus(RequestContext context, String sessionId, String runId) {
        return runProjectionRepository.getStatus(context, sessionId, runId);
    }

    @Override
    public EventHistoryResponse listEvents(RequestContext context, String sessionId, String runId, long afterSequence, int limit) {
        List<RunEventDto> events = runEventStore.listByRun(runId, afterSequence, limit).stream()
                .map(DefaultRunQueryService::toDto)
                .toList();
        long nextAfterSequence = events.stream()
                .mapToLong(RunEventDto::sequence)
                .max()
                .orElse(afterSequence);
        return new EventHistoryResponse(sessionId, runId, events, afterSequence, nextAfterSequence,
                runEventStore.hasTerminalEvent(runId));
    }

    @Override
    public PageResponse<Map<String, Object>> listSteps(RequestContext context, String sessionId, String runId, int limit) {
        return runProjectionRepository.listSteps(context, sessionId, runId, limit);
    }

    @Override
    public PageResponse<Map<String, Object>> listMessages(RequestContext context, String sessionId, String runId, int limit) {
        return runProjectionRepository.listMessages(context, sessionId, runId, limit);
    }

    @Override
    public PageResponse<Map<String, Object>> listToolCalls(RequestContext context, String sessionId, String runId, int limit) {
        return runProjectionRepository.listToolCalls(context, sessionId, runId, limit);
    }

    @Override
    public RunResultResponse getRunResult(RequestContext context, String sessionId, String runId) {
        return runProjectionRepository.getRunResult(context, sessionId, runId);
    }

    private static RunEventDto toDto(RunEvent event) {
        return new RunEventDto(
                event.eventId(), event.tenantId().value(), event.userId().value(), event.sessionId().value(),
                event.runId().value(), event.stepId().value(), event.workspaceId().value(), event.sequence(),
                event.timestamp(), event.type().wireName(), event.traceId().value(), event.correlationId().value(),
                event.causationId().value(), event.visibility().name(), toDto(event.redaction()), payloadSchema(event.payload()),
                1, payloadAttributes(event.payload()));
    }

    private static RedactionDto toDto(RedactionMetadata redaction) {
        return new RedactionDto(redaction.redacted(), List.copyOf(redaction.redactedFields()), redaction.policyRef());
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

    private static Map<String, Object> payloadAttributes(RunEventPayload payload) {
        if (payload instanceof RunEventPayload.RunLifecyclePayload runLifecyclePayload) {
            return runLifecyclePayload.failureSummary() == null
                    ? Map.of("status", runLifecyclePayload.status().name())
                    : Map.of("status", runLifecyclePayload.status().name(), "failureSummary", runLifecyclePayload.failureSummary().message());
        }
        if (payload instanceof RunEventPayload.ExtensionPayload extensionPayload) {
            return extensionPayload.attributes();
        }
        if (payload instanceof RunEventPayload.ToolLifecyclePayload toolLifecyclePayload) {
            return toolLifecyclePayload(toolLifecyclePayload);
        }
        return Map.of("kind", payload.getClass().getSimpleName());
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
        payload.preview().map(DefaultRunQueryService::preview).ifPresent(value -> values.put("preview", value));
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
                safe.put(key, safeValue(value));
            }
        });
        return Map.copyOf(safe);
    }

    private static Object safeValue(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.map(DefaultRunQueryService::safeValue).orElse(null);
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
                    nested.put(key, safeValue(nestedValue));
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
}
