package io.github.pi_java.agent.domain.tool;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ProvisionPreview(
        String previewId,
        String summary,
        Set<String> impacts,
        boolean approvalRecommended,
        Map<String, Object> redactedDetails
) {
    public ProvisionPreview {
        previewId = ToolValidation.requireNonBlank(previewId, "previewId");
        summary = Objects.requireNonNull(summary, "summary must not be null");
        impacts = Set.copyOf(Objects.requireNonNull(impacts, "impacts must not be null"));
        redactedDetails = Map.copyOf(Objects.requireNonNull(redactedDetails, "redactedDetails must not be null"));
    }
}
