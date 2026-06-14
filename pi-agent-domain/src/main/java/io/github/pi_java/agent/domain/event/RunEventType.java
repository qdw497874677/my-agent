package io.github.pi_java.agent.domain.event;

public enum RunEventType {
    RUN_CREATED("run.created"),
    RUN_STARTED("run.started"),
    RUN_COMPLETED("run.completed"),
    RUN_FAILED("run.failed"),
    RUN_CANCELLED("run.cancelled"),
    RUN_POLICY_BLOCKED("run.policy_blocked"),
    STEP_STARTED("step.started"),
    STEP_COMPLETED("step.completed"),
    MODEL_REQUESTED("model.requested"),
    MODEL_DELTA("model.delta"),
    TOOL_PROPOSED("tool.proposed"),
    TOOL_POLICY_DECIDED("tool.policy_decided"),
    TOOL_PREVIEW_GENERATED("tool.preview_generated"),
    TOOL_APPROVAL_REQUIRED("tool.approval_required"),
    TOOL_STARTED("tool.started"),
    TOOL_UPDATED("tool.updated"),
    TOOL_COMPLETED("tool.completed"),
    TOOL_FAILED("tool.failed"),
    TOOL_DENIED("tool.denied"),
    TOOL_CANCELLED("tool.cancelled"),
    POLICY_DECIDED("policy.decided"),
    WORKSPACE_SNAPSHOT_CREATED("workspace.snapshot_created"),
    ARTIFACT_CREATED("artifact.created"),
    MESSAGE_APPENDED("message.appended");

    private final String wireName;

    RunEventType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}
