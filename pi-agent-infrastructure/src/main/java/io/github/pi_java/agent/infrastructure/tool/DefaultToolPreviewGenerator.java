package io.github.pi_java.agent.infrastructure.tool;

import io.github.pi_java.agent.app.port.tool.ToolPreviewGenerator;
import io.github.pi_java.agent.domain.tool.ProvisionPreview;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DefaultToolPreviewGenerator implements ToolPreviewGenerator {

    @Override
    public ProvisionPreview generate(PreviewRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("toolId", request.descriptor().id());
        details.put("riskLevel", request.descriptor().riskLevel().name());
        details.put("sideEffect", request.descriptor().sideEffect().name());
        details.put("scopes", request.descriptor().scopes());
        details.put("policyDecision", request.policyEvaluation().decision().name());
        details.put("policyRef", request.policyEvaluation().policyRef());
        details.put("approvalRef", request.policyEvaluation().approvalRef().orElse(""));
        details.put("sandboxRef", request.policyEvaluation().sandboxRef().orElse(""));
        details.put("redactedArguments", request.redactedArguments());
        details.put("executesSideEffects", false);
        String previewId = "preview-" + Integer.toUnsignedString(Objects.hash(request.toolRequest().toolCallId(),
                request.descriptor().id(), request.descriptor().version()));
        return new ProvisionPreview(previewId,
                "Impact estimate for " + request.descriptor().sideEffect() + " tool " + request.descriptor().id(),
                Set.of(request.descriptor().sideEffect().name(), request.descriptor().riskLevel().name()),
                request.policyEvaluation().approvalRef().isPresent(),
                details);
    }
}
