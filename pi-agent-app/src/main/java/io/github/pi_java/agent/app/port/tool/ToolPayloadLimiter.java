package io.github.pi_java.agent.app.port.tool;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface ToolPayloadLimiter {

    LimitCheck checkArguments(ToolDescriptor descriptor, Map<String, Object> arguments);

    LimitCheck checkResult(ToolDescriptor descriptor, Map<String, Object> result);

    Map<String, Object> summarize(ToolDescriptor descriptor, Map<String, Object> payload);

    record LimitCheck(boolean allowed, Optional<String> reason, Map<String, Object> redactedDetails) {
        public LimitCheck(boolean allowed, String reason, Map<String, Object> redactedDetails) {
            this(allowed, Optional.ofNullable(reason), redactedDetails);
        }

        public LimitCheck {
            reason = Objects.requireNonNull(reason, "reason must not be null");
            redactedDetails = Map.copyOf(Objects.requireNonNull(redactedDetails, "redactedDetails must not be null"));
        }

        public static LimitCheck ok() {
            return new LimitCheck(true, Optional.empty(), Map.of());
        }

        public static LimitCheck rejected(String reason, Map<String, Object> redactedDetails) {
            return new LimitCheck(false, Optional.of(Objects.requireNonNull(reason, "reason must not be null")), redactedDetails);
        }
    }
}
