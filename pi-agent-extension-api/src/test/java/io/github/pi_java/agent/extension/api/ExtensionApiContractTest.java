package io.github.pi_java.agent.extension.api;

import io.github.pi_java.agent.app.port.tool.ToolExecutorBinding;
import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionApiContractTest {

    @Test
    void extension_source_exposes_metadata_and_capabilities() {
        ToolExtensionCapability toolCapability = new ToolExtensionCapability("cap.tool.echo", toolDescriptor(),
                (request, cancellationToken) -> null, Map.of("runtime", "testkit"));
        ExtensionMetadata metadata = metadata(ExtensionLifecycleState.STARTED, ExtensionHealth.up("ready"));
        ExtensionSource source = new ExtensionSource() {
            @Override
            public ExtensionMetadata metadata() {
                return metadata;
            }

            @Override
            public List<ExtensionCapability> capabilities() {
                return List.of(toolCapability);
            }
        };

        assertThat(source.metadata()).isEqualTo(metadata);
        assertThat(source.capabilities()).containsExactly(toolCapability);
        assertThat(source.capabilities().getFirst().type()).isEqualTo(ExtensionCapability.Type.TOOL);
        assertThat(source.capabilities().getFirst().capabilityId()).isEqualTo("cap.tool.echo");
    }

    @Test
    void metadata_rejects_blank_identity_and_keeps_redacted_metadata_immutable() {
        assertThatThrownBy(() -> new ExtensionMetadata(" ", "Echo", "1.0.0", "Pi", ExtensionApiVersion.parse("1.0.0"),
                ExtensionCompatibility.supports("1.0.0", "1.1.0"), ExtensionLifecycleState.DISCOVERED,
                ExtensionHealth.up("ok"), true, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("extensionId");

        ExtensionMetadata metadata = metadata(ExtensionLifecycleState.STARTED, ExtensionHealth.up("ok"));

        assertThat(metadata.redactedMetadata()).containsEntry("safe", "visible");
        assertThatThrownBy(() -> metadata.redactedMetadata().put("secret", "nope"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void tool_capability_requires_descriptor_and_binding() {
        ToolDescriptor descriptor = toolDescriptor();
        ToolExecutorBinding binding = (request, cancellationToken) -> null;

        ToolExtensionCapability capability = new ToolExtensionCapability("cap.tool.echo", descriptor, binding, Map.of());

        assertThat(capability.descriptor()).isSameAs(descriptor);
        assertThat(capability.binding()).isSameAs(binding);
        assertThat(capability.type()).isEqualTo(ExtensionCapability.Type.TOOL);
        assertThatThrownBy(() -> new ToolExtensionCapability("cap.tool.echo", null, binding, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("descriptor");
        assertThatThrownBy(() -> new ToolExtensionCapability("cap.tool.echo", descriptor, null, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("binding");
    }

    @Test
    void capability_family_covers_phase_six_extension_surface() {
        List<ExtensionCapability> capabilities = List.of(
                new ToolExtensionCapability("tool.echo", toolDescriptor(), (request, cancellationToken) -> null, Map.of()),
                new ModelProviderExtensionCapability("model.openai-compatible", "openai-compatible", Set.of("chat", "streaming"), Map.of()),
                new PolicyExtensionCapability("policy.default", "tool-risk-policy", Map.of()),
                new EventListenerExtensionCapability("event.audit", Set.of("tool.lifecycle", "model.delta"), Map.of()),
                new WorkspaceProviderExtensionCapability("workspace.local-temp", "local-temp", Set.of("file", "command"), Map.of()),
                new MemoryProviderExtensionCapability("memory.placeholder", "metadata-only", Set.of(), Map.of("wiring", "deferred"))
        );

        assertThat(capabilities).extracting(ExtensionCapability::type).containsExactly(
                ExtensionCapability.Type.TOOL,
                ExtensionCapability.Type.MODEL_PROVIDER,
                ExtensionCapability.Type.POLICY,
                ExtensionCapability.Type.EVENT_LISTENER,
                ExtensionCapability.Type.WORKSPACE_PROVIDER,
                ExtensionCapability.Type.MEMORY_PROVIDER
        );
        assertThat(capabilities).extracting(ExtensionCapability::capabilityId)
                .containsExactly("tool.echo", "model.openai-compatible", "policy.default", "event.audit",
                        "workspace.local-temp", "memory.placeholder");
    }

    @Test
    void lifecycle_compatibility_and_health_are_machine_checkable() {
        ExtensionCompatibility compatibility = ExtensionCompatibility.supports("1.2.0", "2.0.0");

        assertThat(compatibility.supports(ExtensionApiVersion.parse("1.2.0"))).isTrue();
        assertThat(compatibility.supports(ExtensionApiVersion.parse("1.9.9"))).isTrue();
        assertThat(compatibility.supports(ExtensionApiVersion.parse("2.0.0"))).isFalse();
        assertThat(ExtensionLifecycleState.FAILED.isAvailable()).isFalse();
        assertThat(ExtensionLifecycleState.STARTED.isAvailable()).isTrue();
        assertThat(ExtensionHealth.down("bad config").status()).isEqualTo(ExtensionHealth.Status.DOWN);
    }

    private static ExtensionMetadata metadata(ExtensionLifecycleState state, ExtensionHealth health) {
        return new ExtensionMetadata("ext.echo", "Echo Extension", "1.0.0", "Pi Java",
                ExtensionApiVersion.parse("1.0.0"), ExtensionCompatibility.supports("1.0.0", "2.0.0"),
                state, health, true, Map.of("safe", "visible"));
    }

    private static ToolDescriptor toolDescriptor() {
        ToolSchema schema = new ToolSchema("json-schema:draft-2020-12", Map.of("type", "object"), Set.of(), 4096);
        return new ToolDescriptor("tool.echo", "Echo", "Echoes input", schema, Optional.empty(),
                new ToolProvenance(ToolProvenance.SourceKind.SPI, "ext.echo", "echo", Map.of()), "1.0.0",
                Set.of("demo:read"), ToolRiskLevel.LOW, ToolSideEffect.READ_ONLY, Duration.ofSeconds(5), Map.of());
    }
}
