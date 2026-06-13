package io.github.pi_java.agent.domain.runtime;

import java.util.Map;
import java.util.Objects;

public sealed interface RunInput permits RunInput.ChatInput, RunInput.TaskInput,
        RunInput.StructuredFormInput, RunInput.ToolDrivenInput, RunInput.WorkflowPlannerInput {

    record ChatInput(String text) implements RunInput {
        public ChatInput {
            text = requireNonBlank(text, "text");
        }
    }

    record TaskInput(String objective) implements RunInput {
        public TaskInput {
            objective = requireNonBlank(objective, "objective");
        }
    }

    record StructuredFormInput(Map<String, Object> fields) implements RunInput {
        public StructuredFormInput {
            fields = Map.copyOf(Objects.requireNonNull(fields, "fields must not be null"));
        }
    }

    record ToolDrivenInput(String toolName, Map<String, Object> arguments) implements RunInput {
        public ToolDrivenInput {
            toolName = requireNonBlank(toolName, "toolName");
            arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments must not be null"));
        }
    }

    record WorkflowPlannerInput(String planRequest) implements RunInput {
        public WorkflowPlannerInput {
            planRequest = requireNonBlank(planRequest, "planRequest");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
