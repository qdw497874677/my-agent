package io.github.pi_java.agent.domain.event;

import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.error.FailureSummary;
import io.github.pi_java.agent.domain.model.ModelFinishReason;
import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.domain.policy.PolicyDecision;
import io.github.pi_java.agent.domain.runtime.RunStatus;
import io.github.pi_java.agent.domain.runtime.StepStatus;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;
import io.github.pi_java.agent.domain.tool.ToolCall;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolResult;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public sealed interface RunEventPayload permits RunEventPayload.RunLifecyclePayload,
        RunEventPayload.StepLifecyclePayload, RunEventPayload.ModelDeltaPayload,
        RunEventPayload.ToolProposedPayload, RunEventPayload.ToolCompletedPayload,
        RunEventPayload.ToolLifecyclePayload, RunEventPayload.PolicyDecidedPayload, RunEventPayload.WorkspaceSnapshotPayload,
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

    record ModelDeltaPayload(String modelRef, String textDelta, String providerId, String modelId,
                             ModelFinishReason finishReason, ModelUsage usage, Duration latency) implements RunEventPayload {
        public ModelDeltaPayload(String modelRef, String textDelta) {
            this(modelRef, textDelta, null, null, null, null, null);
        }

        public ModelDeltaPayload {
            modelRef = requireNonBlank(modelRef, "modelRef");
            textDelta = Objects.requireNonNull(textDelta, "textDelta must not be null");
            requireNonBlankIfPresent(providerId, "providerId");
            requireNonBlankIfPresent(modelId, "modelId");
            if (latency != null && latency.isNegative()) {
                throw new IllegalArgumentException("latency must not be negative");
            }
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

    record ToolLifecyclePayload(
            String toolCallId,
            String toolId,
            String descriptorVersion,
            ToolProvenance provenance,
            Map<String, Object> redactedInputSummary,
            Map<String, Object> redactedOutputSummary,
            Optional<PolicyDecision> policyDecision,
            Optional<ToolExecutionStatus> executionStatus,
            Optional<ProvisionPreview> preview,
            Optional<String> errorCategory
    ) implements RunEventPayload {
        public ToolLifecyclePayload(
                String toolCallId,
                String toolId,
                String descriptorVersion,
                ToolProvenance provenance,
                Map<String, Object> redactedInputSummary,
                Map<String, Object> redactedOutputSummary,
                PolicyDecision policyDecision,
                ToolExecutionStatus executionStatus,
                ProvisionPreview preview,
                String errorCategory
        ) {
            this(toolCallId, toolId, descriptorVersion, provenance, redactedInputSummary, redactedOutputSummary,
                    Optional.ofNullable(policyDecision), Optional.ofNullable(executionStatus), Optional.ofNullable(preview),
                    Optional.ofNullable(errorCategory));
        }

        public ToolLifecyclePayload {
            toolCallId = requireNonBlank(toolCallId, "toolCallId");
            toolId = requireNonBlank(toolId, "toolId");
            descriptorVersion = requireNonBlank(descriptorVersion, "descriptorVersion");
            Objects.requireNonNull(provenance, "provenance must not be null");
            redactedInputSummary = Map.copyOf(Objects.requireNonNull(redactedInputSummary, "redactedInputSummary must not be null"));
            redactedOutputSummary = Map.copyOf(Objects.requireNonNull(redactedOutputSummary, "redactedOutputSummary must not be null"));
            policyDecision = Objects.requireNonNull(policyDecision, "policyDecision must not be null");
            executionStatus = Objects.requireNonNull(executionStatus, "executionStatus must not be null");
            preview = Objects.requireNonNull(preview, "preview must not be null");
            errorCategory = Objects.requireNonNull(errorCategory, "errorCategory must not be null");
            errorCategory.ifPresent(value -> requireNonBlank(value, "errorCategory"));
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

    private static void requireNonBlankIfPresent(String value, String fieldName) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank when present");
        }
    }
}
