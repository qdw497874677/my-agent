package io.github.pi_java.agent.adapter.web.provider;

public record ProviderConfig(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String modelId,
        String providerId,
        String completionsPath) {

    public static ProviderConfig defaults() {
        return new ProviderConfig(
                false,
                "https://api.openai.com/v1",
                "",
                "gpt-4.1-mini",
                "openai-compatible",
                "/chat/completions");
    }

    public ProviderConfig {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl.trim();
        modelId = modelId == null || modelId.isBlank() ? "gpt-4.1-mini" : modelId.trim();
        providerId = providerId == null || providerId.isBlank() ? "openai-compatible" : providerId.trim();
        completionsPath = completionsPath == null || completionsPath.isBlank() ? "/chat/completions" : completionsPath.trim();
        apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean isReady() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /** Returns a masked view for API responses — never exposes the full key. */
    public ProviderConfig masked() {
        String masked = apiKey == null || apiKey.isBlank() ? "" :
                apiKey.length() <= 8 ? "****" : apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
        return new ProviderConfig(enabled, baseUrl, masked, modelId, providerId, completionsPath);
    }
}
