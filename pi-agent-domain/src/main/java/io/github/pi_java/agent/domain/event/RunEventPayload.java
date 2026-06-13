package io.github.pi_java.agent.domain.event;

import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.error.FailureSummary;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.domain.runtime.StepStatus;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolResult;

import java.util.Map;
import java.util.Objects;

public sealed interface RunEventPayload permits RunEventPayload.RunLifecyclePayload,
        RunEventPayload.StepLifecyclePayload, RunEventPayload.ModelDeltaPayload,
        RunEventPayload.ToolProposedPayload, RunEventPayload.ToolCompletedPayload,
        RunEventPayload.PolicyDecidedPayload, RunEventPayload.WorkspaceSnapshotPayload,
        RunEventPayload.ArtifactCreatedPayload, RunEventPayload.MessageAppendedPayload,
        RunEventPayload.ExtensionPayload {

    record RunLifecyclePayload(RunStatus status, FailureSummary failureSummary) implements RunEventPayload {
        public RunLifecyclePayload {
            Objects.requireNonNull(status, "status must not be null");
        }
    }

    record StepLifecyclePayload(StepId stepId, StepStatus status, String kind) implements RunEventPayload {
        public StepLifecyclePayload {
            Objects.requireNonNull(stepId, "stepId must not be null");
            Objects.requireNonNull(status, "status must not be null");
            kind = requireNonBlank(kind, "kind");
        }
    }

    record ModelDeltaPayload(String modelRef, String textDelta) implements RunEventPayload {
        public ModelDeltaPayload {
            modelRef = requireNonBlank(modelRef, "modelRef");
            textDelta = Objects.requireNonNull(textDelta, "textDelta must not be null");
        }
    }

    record ToolProposedPayload(ToolCall toolCall) implements RunEventPayload {
        public ToolProposedPayload {
            Objects.requireNonNull(toolCall, "toolCall must not be null");
        }
    }

    record ToolCompletedPayload(ToolResult toolResult) implements RunEventPayload {
        public ToolCompletedPayload {
            Objects.requireNonNull(toolResult, "toolResult must not be null");
        }
    }

    record PolicyDecidedPayload(String policyRef, String decision, String reason) implements RunEventPayload {
        public PolicyDecidedPayload {
            policyRef = requireNonBlank(policyRef, "policyRef");
            decision = requireNonBlank(decision, "decision");
            reason = Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    record WorkspaceSnapshotPayload(String snapshotId, String fingerprint) implements RunEventPayload {
        public WorkspaceSnapshotPayload {
            snapshotId = requireNonBlank(snapshotId, "snapshotId");
            fingerprint = requireNonBlank(fingerprint, "fingerprint");
        }
    }

    record ArtifactCreatedPayload(String artifactId, String artifactType) implements RunEventPayload {
        public ArtifactCreatedPayload {
            artifactId = requireNonBlank(artifactId, "artifactId");
            artifactType = requireNonBlank(artifactType, "artifactType");
        }
    }

    record MessageAppendedPayload(String messageId, String role) implements RunEventPayload {
        public MessageAppendedPayload {
            messageId = requireNonBlank(messageId, "messageId");
            role = requireNonBlank(role, "role");
        }
    }

    record ExtensionPayload(String schema, String version, Map<String, Object> attributes) implements RunEventPayload {
        public ExtensionPayload {
            schema = requireNonBlank(schema, "schema");
            version = requireNonBlank(version, "version");
            attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
