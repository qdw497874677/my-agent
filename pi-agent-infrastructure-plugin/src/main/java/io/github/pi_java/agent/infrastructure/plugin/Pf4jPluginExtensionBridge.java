package io.github.pi_java.agent.infrastructure.plugin;

import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import io.github.pi_java.agent.infrastructure.extension.ServiceLoaderExtensionDiscovery;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Pf4jPluginExtensionBridge {

    private final Path controlledDirectory;
    private final String platformApiVersion;

    public Pf4jPluginExtensionBridge(Path controlledDirectory, String platformApiVersion) {
        this.controlledDirectory = Objects.requireNonNull(controlledDirectory, "controlledDirectory must not be null");
        this.platformApiVersion = requireNonBlank(platformApiVersion, "platformApiVersion");
    }

    public List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> bridge(PluginWrapper wrapper,
                                                                         List<ExtensionSource> sources) {
        Objects.requireNonNull(wrapper, "wrapper must not be null");
        Objects.requireNonNull(sources, "sources must not be null");
        PluginDescriptorSummary descriptor = descriptor(wrapper);
        PluginLifecycleSummary lifecycle = lifecycle(wrapper, descriptor.compatibility());
        if (wrapper.getPluginState() == PluginState.FAILED || !descriptor.compatibility().compatible()) {
            return List.of(Pf4jPluginSourceDiscovery.PluginDiscoveredSource.failed(descriptor, lifecycle,
                    lifecycle.redactedError().isBlank() ? descriptor.compatibility().status() : lifecycle.redactedError()));
        }
        return sources.stream()
                .map(source -> Pf4jPluginSourceDiscovery.PluginDiscoveredSource.discovered(
                        descriptor, lifecycle, enrichSource(source, descriptor)))
                .toList();
    }

    private PluginDescriptorSummary descriptor(PluginWrapper wrapper) {
        String requires = requireNonBlank(wrapper.getDescriptor().getRequires(), "plugin requires");
        boolean compatible = supports(requires);
        PluginCompatibilitySummary compatibility = compatible
                ? PluginCompatibilitySummary.compatible(requires, platformApiVersion)
                : PluginCompatibilitySummary.incompatible(requires, platformApiVersion);
        return PluginDescriptorSummary.fromControlledPath(wrapper.getPluginId(), wrapper.getDescriptor().getPluginDescription(),
                wrapper.getDescriptor().getVersion(), wrapper.getDescriptor().getProvider(), controlledDirectory,
                wrapper.getPluginPath(), compatibility, Map.of("pluginClass", wrapper.getDescriptor().getPluginClass(),
                        "license", wrapper.getDescriptor().getLicense()));
    }

    private PluginLifecycleSummary lifecycle(PluginWrapper wrapper, PluginCompatibilitySummary compatibility) {
        ExtensionLifecycleState state = switch (wrapper.getPluginState()) {
            case STARTED -> ExtensionLifecycleState.STARTED;
            case RESOLVED -> ExtensionLifecycleState.LOADED;
            case DISABLED -> ExtensionLifecycleState.DISABLED;
            case FAILED -> ExtensionLifecycleState.FAILED;
            default -> ExtensionLifecycleState.DISCOVERED;
        };
        Throwable failed = wrapper.getFailedException();
        if (failed != null) {
            return PluginLifecycleSummary.failed(wrapper.getPluginId(), state, compatibility, true,
                    ServiceLoaderExtensionDiscovery.sanitize(failed));
        }
        if (!compatibility.compatible()) {
            return PluginLifecycleSummary.failed(wrapper.getPluginId(), ExtensionLifecycleState.QUARANTINED,
                    compatibility, true, compatibility.status());
        }
        return PluginLifecycleSummary.of(wrapper.getPluginId(), state, compatibility, true);
    }

    private boolean supports(String requires) {
        try {
            ExtensionApiVersion platform = ExtensionApiVersion.parse(platformApiVersion);
            return ExtensionCompatibility.supports(requires, nextMajor(requires)).supports(platform)
                    || platform.compareTo(ExtensionApiVersion.parse(requires)) >= 0;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String nextMajor(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        return (major + 1) + ".0.0";
    }

    private static ExtensionSource enrichSource(ExtensionSource delegate, PluginDescriptorSummary descriptor) {
        Objects.requireNonNull(delegate, "delegate must not be null");
        return new ExtensionSource() {
            @Override
            public ExtensionMetadata metadata() {
                ExtensionMetadata metadata = delegate.metadata();
                LinkedHashMap<String, Object> redacted = new LinkedHashMap<>(metadata.redactedMetadata());
                addPluginMetadata(redacted, descriptor);
                return new ExtensionMetadata(metadata.extensionId(), metadata.name(), metadata.version(), metadata.vendor(),
                        metadata.apiVersion(), metadata.compatibility(), metadata.lifecycleState(), metadata.health(),
                        metadata.enabled(), redacted);
            }

            @Override
            public List<ExtensionCapability> capabilities() {
                return delegate.capabilities().stream()
                        .map(capability -> enrichCapability(capability, descriptor))
                        .toList();
            }
        };
    }

    private static ExtensionCapability enrichCapability(ExtensionCapability delegate, PluginDescriptorSummary descriptor) {
        return new ExtensionCapability() {
            @Override
            public String capabilityId() {
                return delegate.capabilityId();
            }

            @Override
            public Type type() {
                return delegate.type();
            }

            @Override
            public Map<String, Object> redactedMetadata() {
                LinkedHashMap<String, Object> redacted = new LinkedHashMap<>(delegate.redactedMetadata());
                addPluginMetadata(redacted, descriptor);
                return Map.copyOf(redacted);
            }
        };
    }

    private static void addPluginMetadata(Map<String, Object> metadata, PluginDescriptorSummary descriptor) {
        metadata.put("sourceKind", "PLUGIN");
        metadata.put("plugin.id", descriptor.pluginId());
        metadata.put("plugin.version", descriptor.version());
        metadata.put("plugin.provider", descriptor.provider());
        metadata.put("plugin.sourcePath", descriptor.sourcePathSummary());
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
