package io.github.pi_java.agent.adapter.web.provider;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/providers")
public class ProviderConfigController {

    private final ProviderConfigStore store;

    public ProviderConfigController(ProviderConfigStore store) {
        this.store = store;
    }

    @GetMapping
    public ProviderConfig current() {
        return store.current().masked();
    }

    @PutMapping
    public ProviderConfig update(@RequestBody ProviderConfigUpdateRequest request) {
        ProviderConfig current = store.current();
        String apiKey = request.apiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("****")) {
            apiKey = current.apiKey();
        }
        ProviderConfig updated = new ProviderConfig(
                request.enabled() != null ? request.enabled() : current.enabled(),
                request.baseUrl() != null ? request.baseUrl() : current.baseUrl(),
                apiKey,
                request.modelId() != null ? request.modelId() : current.modelId(),
                current.providerId(),
                current.completionsPath());
        return store.update(updated).masked();
    }

    @GetMapping("/models")
    public ModelListResponse listModels() {
        ProviderConfig config = store.current();
        if (!config.isReady()) {
            return ModelListResponse.notConfigured(config,
                    "Provider not configured. Enable the provider and add an API key in Provider Settings, then refresh models.");
        }
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(config.baseUrl())
                    .defaultHeader("Authorization", "Bearer " + config.apiKey())
                    .build();
            JsonNode response = client.get()
                    .uri("/models")
                    .retrieve()
                    .body(JsonNode.class);
            List<String> modelIds = new ArrayList<>();
            if (response != null && response.has("data")) {
                for (JsonNode model : response.get("data")) {
                    String id = model.has("id") ? model.get("id").asText() : null;
                    if (id != null && !id.isBlank()) {
                        modelIds.add(id);
                    }
                }
            }
            modelIds.sort(String::compareTo);
            if (modelIds.isEmpty()) {
                return ModelListResponse.empty(config,
                        "Provider is ready, but no models were returned. Check provider model access and base URL.");
            }
            return ModelListResponse.success(config, modelIds,
                    "Refreshed " + modelIds.size() + " models from " + safeProviderId(config) + ".");
        } catch (Exception ex) {
            String summary = safeErrorSummary(ex, config);
            return ModelListResponse.error(config, summary,
                    "Provider model refresh failed. Check provider settings and credentials. Details: " + summary);
        }
    }

    private static String safeProviderId(ProviderConfig config) {
        return config == null || config.providerId() == null || config.providerId().isBlank()
                ? "provider"
                : config.providerId().trim();
    }

    private static String safeErrorSummary(Exception ex, ProviderConfig config) {
        String summary = ex == null || ex.getMessage() == null || ex.getMessage().isBlank()
                ? "request failed"
                : ex.getMessage();
        summary = summary.replaceAll("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+", "[REDACTED credential]");
        summary = summary.replace(config.apiKey(), "[REDACTED]");
        summary = summary.replaceAll("(?i)bearer\\s+\\[REDACTED]", "[REDACTED credential]");
        summary = summary.replaceAll("(?i)(api[_-]?key|authorization|token|secret)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]");
        summary = summary.replaceAll("(?i)sk-[A-Za-z0-9._-]+", "[REDACTED]");
        summary = summary.replace('\n', ' ').replace('\r', ' ').trim();
        if (summary.isBlank()) {
            summary = "request failed";
        }
        if (summary.length() > 160) {
            summary = summary.substring(0, 157) + "...";
        }
        return summary;
    }

    public record ProviderConfigUpdateRequest(
            Boolean enabled,
            String baseUrl,
            String apiKey,
            String modelId) {
    }

    public record ModelListResponse(
            List<String> models,
            String error,
            String state,
            String message,
            int modelCount,
            boolean ready,
            String selectedModel,
            String providerId) {

        public ModelListResponse(List<String> models, String error) {
            this(models, error,
                    error == null ? (models == null || models.isEmpty() ? "empty" : "success") : "error",
                    error,
                    models == null ? 0 : models.size(),
                    true,
                    null,
                    null);
        }

        public ModelListResponse {
            models = models == null ? List.of() : List.copyOf(models);
            modelCount = models.size();
            state = state == null || state.isBlank() ? (error == null ? "success" : "error") : state.toLowerCase(Locale.ROOT);
            selectedModel = selectedModel == null ? "" : selectedModel;
            providerId = providerId == null ? "" : providerId;
        }

        static ModelListResponse notConfigured(ProviderConfig config, String message) {
            return from(config, List.of(), null, "not_configured", message, false);
        }

        static ModelListResponse success(ProviderConfig config, List<String> models, String message) {
            return from(config, models, null, "success", message, true);
        }

        static ModelListResponse empty(ProviderConfig config, String message) {
            return from(config, List.of(), null, "empty", message, true);
        }

        static ModelListResponse error(ProviderConfig config, String error, String message) {
            return from(config, List.of(), error, "error", message, config.isReady());
        }

        private static ModelListResponse from(ProviderConfig config, List<String> models, String error, String state, String message, boolean ready) {
            ProviderConfig masked = config.masked();
            return new ModelListResponse(models, error, state, message, models == null ? 0 : models.size(), ready,
                    masked.modelId(), masked.providerId());
        }
    }
}
