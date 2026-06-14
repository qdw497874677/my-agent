package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.app.port.model.ModelProviderRegistry;
import io.github.pi_java.agent.app.port.model.ResolvedSecret;
import io.github.pi_java.agent.app.port.model.SecretResolver;
import io.github.pi_java.agent.domain.model.ProviderModelRef;
import io.github.pi_java.agent.domain.model.StreamingModelClient;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {PiCloudServerApplication.class, TestCloudRuntimeConfiguration.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pi.providers.openai-compatible.enabled=false",
        "pi.providers.openai-compatible.api-key=sk-test-secret",
        "pi.providers.openai-compatible.default-model-id=gpt-test"
})
class ModelProviderWiringIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ModelProviderRegistry modelProviderRegistry;

    @Autowired
    private SecretResolver secretResolver;

    @Autowired
    private AgentRuntime agentRuntime;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @Test
    void registryAndSecretResolverAreWiredWithoutExposingRawSecrets() {
        assertThat(modelProviderRegistry.resolve(ProviderModelRef.parse("openai-compatible:gpt-test"))).isPresent();

        ResolvedSecret resolved = secretResolver.resolve(modelProviderRegistry.listProviders().getFirst().credentialRef());

        assertThat(resolved.rawValue()).isEqualTo("sk-test-secret");
        assertThat(resolved.toString()).doesNotContain("sk-test-secret");
        assertThat(modelProviderRegistry.listProviders().getFirst().credentialRef().toString()).doesNotContain("sk-test-secret");
    }

    @Test
    void disabledProviderDoesNotReplaceNoKeyFakeRuntime() {
        assertThat(agentRuntime.getClass().getName()).contains("TestCloudRuntimeConfiguration");
        assertThat(applicationContext.getBeansOfType(StreamingModelClient.class)).isEmpty();
    }
}
