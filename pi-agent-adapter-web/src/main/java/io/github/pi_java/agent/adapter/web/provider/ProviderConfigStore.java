package io.github.pi_java.agent.adapter.web.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class ProviderConfigStore {

    private final AtomicReference<ProviderConfig> config = new AtomicReference<>(ProviderConfig.defaults());
    private volatile long version = 0;
    private final SqliteLocalPersistence persistence;

    @Autowired
    public ProviderConfigStore(@Value("${pi.local.db-path:data/pi-local.db}") String dbPath) {
        this.persistence = new SqliteLocalPersistence(dbPath);
        ProviderConfig loaded = persistence.loadProviderConfig();
        if (loaded != null) {
            this.config.set(loaded);
        }
    }

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
        persistence.saveProviderConfig(resolved);
        return resolved;
    }

    public boolean isReady() {
        return config.get().isReady();
    }

    public SqliteLocalPersistence persistence() {
        return persistence;
    }
}
