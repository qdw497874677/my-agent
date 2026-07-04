package io.github.pi_java.agent.client.run;

/**
 * Safe provider/model/fallback facts pinned to the run that used them.
 *
 * <p>This client DTO deliberately carries only identifiers and summaries that
 * are safe for history/debugging surfaces. It must not contain API keys,
 * authorization headers, raw provider configuration snapshots, request bodies,
 * or provider SDK objects.
 */
public record RunProviderMetadata(
        String requestedModelRef,
        String selectedModelRef,
        String resolvedProviderId,
        String resolvedModelId,
        String fallbackMode,
        String readinessState,
        String safeErrorSummary) {

    public static final RunProviderMetadata EMPTY = new RunProviderMetadata(null, null, null, null, null, null, null);

    public static RunProviderMetadata selectedSnapshot(String providerId, String modelId, boolean ready) {
        String normalizedProvider = blankToNull(providerId);
        String normalizedModel = blankToNull(modelId);
        String modelRef = normalizedProvider == null || normalizedModel == null ? null : normalizedProvider + ":" + normalizedModel;
        return new RunProviderMetadata(
                modelRef,
                modelRef,
                normalizedProvider,
                normalizedModel,
                ready ? "NONE" : "local",
                ready ? "READY" : "NOT_CONFIGURED",
                null);
    }

    public boolean isEmpty() {
        return isBlank(requestedModelRef)
                && isBlank(selectedModelRef)
                && isBlank(resolvedProviderId)
                && isBlank(resolvedModelId)
                && isBlank(fallbackMode)
                && isBlank(readinessState)
                && isBlank(safeErrorSummary);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
