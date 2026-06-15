package io.github.pi_java.agent.infrastructure.extension;

import io.github.pi_java.agent.extension.api.ExtensionApiVersion;
import io.github.pi_java.agent.extension.api.ExtensionCapability;
import io.github.pi_java.agent.extension.api.ExtensionCompatibility;
import io.github.pi_java.agent.extension.api.ExtensionHealth;
import io.github.pi_java.agent.extension.api.ExtensionLifecycleState;
import io.github.pi_java.agent.extension.api.ExtensionMetadata;
import io.github.pi_java.agent.extension.api.ExtensionSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionContributionRegistryTest {

    @Test
    void ordersSourcesAndCapabilitiesDeterministically() {
        DefaultExtensionContributionRegistry registry = DefaultExtensionContributionRegistry.build(
                List.of(discovered(source("z-source", 20, capability("z-cap", 30), capability("a-cap", 10))),
                        discovered(source("a-source", 10, capability("b-cap", 0)))),
                new ExtensionRegistrationProperties());

        assertThat(registry.sourceEntries()).extracting(DefaultExtensionContributionRegistry.SourceEntry::sourceId)
                .containsExactly("a-source", "z-source");
        assertThat(registry.usableCapabilities()).extracting(DefaultExtensionContributionRegistry.CapabilityEntry::capabilityId)
                .containsExactly("b-cap", "a-cap", "z-cap");
    }

    @Test
    void duplicateCapabilityIdsFailFastByDefault() {
        List<ServiceLoaderExtensionDiscovery.DiscoveredSource> discovered = List.of(
                discovered(source("first", 0, capability("dup", 0))),
                discovered(source("second", 1, capability("dup", 0))));

        assertThatThrownBy(() -> DefaultExtensionContributionRegistry.build(discovered,
                new ExtensionRegistrationProperties()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate extension capability id");
    }

    @Test
    void explicitOverrideKeepsLaterOrderedDuplicate() {
        ExtensionRegistrationProperties properties = new ExtensionRegistrationProperties(
                List.of(), List.of(), true, "1.0.0");

        DefaultExtensionContributionRegistry registry = DefaultExtensionContributionRegistry.build(List.of(
                discovered(source("first", 0, capability("dup", 0, Map.of("owner", "first")))),
                discovered(source("second", 1, capability("dup", 0, Map.of("owner", "second"))))), properties);

        assertThat(registry.usableCapabilities()).hasSize(1);
        assertThat(registry.usableCapabilities().getFirst().sourceId()).isEqualTo("second");
    }

    @Test
    void disabledAndIncompatibleContributionsRemainVisibleButNotUsable() {
        ExtensionRegistrationProperties properties = new ExtensionRegistrationProperties(
                List.of("disabled-source"), List.of("disabled-cap"), false, "1.5.0");

        DefaultExtensionContributionRegistry registry = DefaultExtensionContributionRegistry.build(List.of(
                discovered(source("disabled-source", 0, capability("source-cap", 0))),
                discovered(source("enabled-source", 1, capability("disabled-cap", 0), capability("usable-cap", 1))),
                discovered(source("new-source", 2, ExtensionCompatibility.supports("2.0.0", "3.0.0"), capability("future-cap", 0)))), properties);

        assertThat(registry.capabilityEntries()).extracting(DefaultExtensionContributionRegistry.CapabilityEntry::capabilityId)
                .contains("source-cap", "disabled-cap", "usable-cap", "future-cap");
        assertThat(registry.usableCapabilities()).extracting(DefaultExtensionContributionRegistry.CapabilityEntry::capabilityId)
                .containsExactly("usable-cap");
        assertThat(registry.sourceEntries()).filteredOn(entry -> entry.sourceId().equals("new-source"))
                .singleElement()
                .extracting(DefaultExtensionContributionRegistry.SourceEntry::compatibilityStatus)
                .isEqualTo("INCOMPATIBLE");
    }

    @Test
    void exactAndRangeCompatibilityParserSupportsPlatformChecks() {
        ExtensionCompatibilityChecker checker = new ExtensionCompatibilityChecker(ExtensionApiVersion.parse("1.5.0"));

        assertThat(checker.supports("1.5.0")).isTrue();
        assertThat(checker.supports("[1.0.0,2.0.0)")).isTrue();
        assertThat(checker.supports("[1.6.0,2.0.0)")).isFalse();
    }

    @Test
    void discoveryFailuresAreRetainedWithRedactedErrors() {
        ServiceLoaderExtensionDiscovery.DiscoveredSource failed = new ServiceLoaderExtensionDiscovery.DiscoveredSource(
                "failed-source",
                ServiceLoaderExtensionDiscovery.DiscoveryStatus.FAILED,
                Optional.empty(),
                "IllegalStateException: token=<redacted>");

        DefaultExtensionContributionRegistry registry = DefaultExtensionContributionRegistry.build(List.of(failed),
                new ExtensionRegistrationProperties());

        assertThat(registry.sourceEntries()).singleElement().satisfies(entry -> {
            assertThat(entry.sourceId()).isEqualTo("failed-source");
            assertThat(entry.status()).isEqualTo("FAILED");
            assertThat(entry.redactedError()).contains("<redacted>");
        });
        assertThat(registry.usableCapabilities()).isEmpty();
    }

    private static ServiceLoaderExtensionDiscovery.DiscoveredSource discovered(ExtensionSource source) {
        return ServiceLoaderExtensionDiscovery.DiscoveredSource.discovered(source);
    }

    private static ExtensionSource source(String id, int order, ExtensionCapability... capabilities) {
        return source(id, order, ExtensionCompatibility.supports("1.0.0", "2.0.0"), capabilities);
    }

    private static ExtensionSource source(String id, int order, ExtensionCompatibility compatibility,
                                          ExtensionCapability... capabilities) {
        return new ExtensionSource() {
            @Override
            public ExtensionMetadata metadata() {
                return new ExtensionMetadata(id, id, "1.0.0", "test", ExtensionApiVersion.parse("1.0.0"),
                        compatibility, ExtensionLifecycleState.STARTED, ExtensionHealth.up("ok"), true,
                        Map.of("order", order));
            }

            @Override
            public List<ExtensionCapability> capabilities() {
                return List.of(capabilities);
            }
        };
    }

    private static ExtensionCapability capability(String id, int order) {
        return capability(id, order, Map.of());
    }

    private static ExtensionCapability capability(String id, int order, Map<String, Object> metadata) {
        return new ExtensionCapability() {
            @Override
            public String capabilityId() {
                return id;
            }

            @Override
            public Type type() {
                return Type.TOOL;
            }

            @Override
            public Map<String, Object> redactedMetadata() {
                java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>(metadata);
                values.put("order", order);
                return Map.copyOf(values);
            }
        };
    }
}
