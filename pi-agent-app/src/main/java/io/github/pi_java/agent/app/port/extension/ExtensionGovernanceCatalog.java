package io.github.pi_java.agent.app.port.extension;

import java.util.List;

public interface ExtensionGovernanceCatalog {

    List<ExtensionSourceStatus> sources();

    default String overallStatus() {
        List<ExtensionSourceStatus> statuses = sources();
        if (statuses.isEmpty()) {
            return "UNCONFIGURED";
        }
        boolean anyUnhealthy = statuses.stream()
                .anyMatch(source -> "UNHEALTHY".equals(source.healthStatus())
                        || "FAILED".equals(source.lifecycleStatus())
                        || "INCOMPATIBLE".equals(source.compatibilityStatus()));
        if (anyUnhealthy) {
            return "DEGRADED";
        }
        boolean anyDisabled = statuses.stream().anyMatch(source -> !source.enabled());
        return anyDisabled ? "PARTIAL" : "HEALTHY";
    }
}
