package io.github.pi_java.agent.adapter.web.provider;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public record ProviderConfigUpdateRequest(
            Boolean enabled,
            String baseUrl,
            String apiKey,
            String modelId) {
    }
}
