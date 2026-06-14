package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.domain.model.CredentialRef;
import io.github.pi_java.agent.domain.model.SecretRef;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentAndPropertySecretResolverTest {

    @Test
    void resolvesEnvSecretFromInjectedEnvironmentMap() {
        EnvironmentAndPropertySecretResolver resolver = new EnvironmentAndPropertySecretResolver(
                Map.of("OPENAI_API_KEY", "sk-env-secret"),
                Map.of()
        );

        ResolvedSecret resolved = resolver.resolve(CredentialRef.of("env:OPENAI_API_KEY"));

        assertThat(resolved.rawValue()).isEqualTo("sk-env-secret");
        assertThat(resolved.toString()).contains("env:***").doesNotContain("sk-env-secret").doesNotContain("OPENAI_API_KEY");
    }

    @Test
    void resolvesConfigSecretFromInjectedPropertyMap() {
        EnvironmentAndPropertySecretResolver resolver = new EnvironmentAndPropertySecretResolver(
                Map.of(),
                Map.of("pi.providers.openai.api-key", "sk-config-secret")
        );

        ResolvedSecret resolved = resolver.resolve(SecretRef.of("config:pi.providers.openai.api-key")).orElseThrow();

        assertThat(resolved.rawValue()).isEqualTo("sk-config-secret");
        assertThat(resolved.toString()).contains("config:***").doesNotContain("sk-config-secret").doesNotContain("pi.providers.openai.api-key");
    }

    @Test
    void missingSecretExceptionRedactsTargetAndKnownValues() {
        EnvironmentAndPropertySecretResolver resolver = new EnvironmentAndPropertySecretResolver(
                Map.of("OPENAI_API_KEY", "sk-env-secret"),
                Map.of("pi.providers.openai.api-key", "sk-config-secret")
        );

        assertThatThrownBy(() -> resolver.resolve(CredentialRef.of("env:MISSING_KEY")))
                .isInstanceOf(SecretResolver.SecretResolutionException.class)
                .hasMessageContaining("env:***")
                .hasMessageNotContaining("MISSING_KEY")
                .hasMessageNotContaining("sk-env-secret")
                .hasMessageNotContaining("sk-config-secret");
    }
}
