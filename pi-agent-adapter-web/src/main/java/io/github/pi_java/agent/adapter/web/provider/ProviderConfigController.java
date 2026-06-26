package io.github.pi_java.agent.adapter.web.provider;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            return new ModelListResponse(List.of(), "Provider not configured (enabled + API key required)");
        }
        try {
            String modelsUrl = config.baseUrl().replaceAll("/+$", "") + "/models";
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
            return new ModelListResponse(modelIds, null);
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.length() > 200) {
                msg = msg.substring(0, 200);
            }
            return new ModelListResponse(List.of(), msg);
        }
    }

    public record ProviderConfigUpdateRequest(
            Boolean enabled,
            String baseUrl,
            String apiKey,
            String modelId) {
    }

    public record ModelListResponse(List<String> models, String error) {
    }
}
