package io.github.pi_java.agent.adapter.web.provider;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ProviderConfigStore {

    private final AtomicReference<ProviderConfig> config = new AtomicReference<>(ProviderConfig.defaults());
    private volatile long version = 0;

    public ProviderConfig current() {
        return config.get();
    }

    public long version() {
        return version;
    }

    public ProviderConfig update(ProviderConfig newConfig) {
        ProviderConfig resolved = new ProviderConfig(
                newConfig.enabled(),
                newConfig.baseUrl(),
                newConfig.apiKey(),
                newConfig.modelId(),
                newConfig.providerId(),
                newConfig.completionsPath());
        config.set(resolved);
        version++;
        return resolved;
    }

    public boolean isReady() {
        return config.get().isReady();
    }
}
