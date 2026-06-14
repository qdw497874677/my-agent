package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.app.port.tool.ToolPayloadLimiter;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public final class DefaultToolPayloadLimiter implements ToolPayloadLimiter {

    private final int argumentLimitBytes;
    private final int resultLimitBytes;
    private final int previewChars;

    public DefaultToolPayloadLimiter() {
        this(16 * 1024, 64 * 1024, 512);
    }

    public DefaultToolPayloadLimiter(int argumentLimitBytes, int resultLimitBytes, int previewChars) {
        if (argumentLimitBytes <= 0 || resultLimitBytes <= 0 || previewChars <= 0) {
            throw new IllegalArgumentException("limits must be positive");
        }
        this.argumentLimitBytes = argumentLimitBytes;
        this.resultLimitBytes = resultLimitBytes;
        this.previewChars = previewChars;
    }

    @Override
    public LimitCheck checkArguments(ToolDescriptor descriptor, Map<String, Object> arguments) {
        return check(descriptor, arguments, Math.min(argumentLimitBytes, descriptor.inputSchema().payloadLimitBytes()), "arguments");
    }

    @Override
    public LimitCheck checkResult(ToolDescriptor descriptor, Map<String, Object> result) {
        int schemaLimit = descriptor.outputSchema().map(schema -> schema.payloadLimitBytes()).orElse(resultLimitBytes);
        return check(descriptor, result, Math.min(resultLimitBytes, schemaLimit), "result");
    }

    @Override
    public Map<String, Object> summarize(ToolDescriptor descriptor, Map<String, Object> payload) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        int estimated = estimateBytes(payload);
        int limit = Math.min(resultLimitBytes, descriptor.outputSchema().map(schema -> schema.payloadLimitBytes()).orElse(resultLimitBytes));
        boolean truncated = estimated > limit || payload.toString().length() > previewChars;
        return Map.of(
                "truncated", truncated,
                "estimatedBytes", estimated,
                "limitBytes", limit,
                "payloadPreview", preview(payload.toString()),
                "valueType", "object"
        );
    }

    private LimitCheck check(ToolDescriptor descriptor, Map<String, Object> payload, int limit, String kind) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        int estimated = estimateBytes(payload);
        if (estimated <= limit) {
            return LimitCheck.ok();
        }
        return LimitCheck.rejected(kind + " payload exceeds configured limit", Map.of(
                "payloadKind", kind,
                "estimatedBytes", estimated,
                "limitBytes", limit,
                "truncated", true,
                "payloadPreview", preview(payload.toString())
        ));
    }

    private int estimateBytes(Map<String, Object> payload) {
        return payload.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    private String preview(String value) {
        if (value.length() <= previewChars) {
            return value;
        }
        return value.substring(0, previewChars) + "...";
    }
}
