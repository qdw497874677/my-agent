package io.github.pi_java.agent.app.port.tool;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface ToolArgumentValidator {

    ValidationResult validate(ToolDescriptor descriptor, ToolExecutionRequest request);

    record ValidationResult(boolean valid, Optional<String> errorCode, String message, Map<String, Object> redactedDetails) {
        public ValidationResult(boolean valid, String errorCode, String message, Map<String, Object> redactedDetails) {
            this(valid, Optional.ofNullable(errorCode), message, redactedDetails);
        }

        public ValidationResult {
            errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
            errorCode.ifPresent(value -> requireNonBlank(value, "errorCode"));
            message = Objects.requireNonNull(message, "message must not be null");
            redactedDetails = Map.copyOf(Objects.requireNonNull(redactedDetails, "redactedDetails must not be null"));
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, Optional.empty(), "valid", Map.of());
        }

        public static ValidationResult invalid(String errorCode, String message, Map<String, Object> redactedDetails) {
            return new ValidationResult(false, Optional.of(requireNonBlank(errorCode, "errorCode")), message, redactedDetails);
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
