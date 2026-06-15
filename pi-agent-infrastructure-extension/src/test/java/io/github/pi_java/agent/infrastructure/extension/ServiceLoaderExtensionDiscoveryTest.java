package io.github.pi_java.agent.infrastructure.extension;

import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceLoaderExtensionDiscoveryTest {

    @Test
    void discoversExtensionSourcesDeterministicallyFromServiceLoader() {
        ServiceLoaderExtensionDiscovery discovery = new ServiceLoaderExtensionDiscovery(
                Thread.currentThread().getContextClassLoader());

        List<ServiceLoaderExtensionDiscovery.DiscoveredSource> sources = discovery.discover();

        assertThat(sources).extracting(ServiceLoaderExtensionDiscovery.DiscoveredSource::sourceId)
                .containsExactly("fixture-beta", "fixture-alpha");
        assertThat(sources).allSatisfy(source -> {
            assertThat(source.status()).isEqualTo(ServiceLoaderExtensionDiscovery.DiscoveryStatus.DISCOVERED);
            assertThat(source.source()).isPresent();
            assertThat(source.redactedError()).isEmpty();
        });
        assertThatThrownBy(() -> sources.add(sources.getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void capturesServiceLoaderFailuresAsFailedSourceStatus() throws Exception {
        Path services = Files.createTempDirectory("pi-extension-services");
        Path descriptor = services.resolve("META-INF/services/io.github.pi_java.agent.extension.api.ExtensionSource");
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor,
                "io.github.pi_java.agent.infrastructure.extension.MissingFixtureSource\n" +
                        FailingFixtureSource.class.getName() + "\n");

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{services.toUri().toURL()},
                Thread.currentThread().getContextClassLoader())) {
            List<ServiceLoaderExtensionDiscovery.DiscoveredSource> sources =
                    new ServiceLoaderExtensionDiscovery(classLoader).discover();

            assertThat(sources).filteredOn(source -> source.status() == ServiceLoaderExtensionDiscovery.DiscoveryStatus.FAILED)
                    .hasSize(2)
                    .allSatisfy(source -> {
                        assertThat(source.sourceId()).contains("ServiceConfigurationError");
                        assertThat(source.redactedError()).contains("ServiceConfigurationError");
                        assertThat(source.redactedError()).doesNotContain("\tat ");
                    });
        }
    }

    public static final class AlphaFixtureSource implements ExtensionSource {
        @Override
        public ExtensionMetadata metadata() {
            return extensionMetadata("fixture-alpha", 20);
        }

        @Override
        public List<ExtensionCapability> capabilities() {
            return List.of(new SimpleCapability("alpha-tool"));
        }
    }

    public static final class BetaFixtureSource implements ExtensionSource {
        @Override
        public ExtensionMetadata metadata() {
            return extensionMetadata("fixture-beta", 10);
        }

        @Override
        public List<ExtensionCapability> capabilities() {
            return List.of(new SimpleCapability("beta-tool"));
        }
    }

    public static final class FailingFixtureSource implements ExtensionSource {
        public FailingFixtureSource() {
            throw new IllegalStateException("fixture boot failed with token=secret");
        }

        @Override
        public ExtensionMetadata metadata() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ExtensionCapability> capabilities() {
            throw new UnsupportedOperationException();
        }
    }

    record SimpleCapability(String capabilityId) implements ExtensionCapability {
        @Override
        public Type type() {
            return Type.TOOL;
        }

        @Override
        public Map<String, Object> redactedMetadata() {
            return Map.of("order", 0);
        }
    }

    private static ExtensionMetadata extensionMetadata(String id, int order) {
        return new ExtensionMetadata(
                id,
                id,
                "1.0.0",
                "test",
                io.github.pi_java.agent.extension.api.ExtensionApiVersion.parse("1.0.0"),
                ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                ExtensionLifecycleState.STARTED,
                ExtensionHealth.up("ok"),
                true,
                Map.of("order", order));
    }
}
