package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentCatalogControllerTest {

    @Test
    void catalogItemExposesRunDecisionMetadata() {
        io.github.pi_java.agent.client.agent.AgentCatalogItemDto item = new io.github.pi_java.agent.client.agent.AgentCatalogItemDto(
                "cloud-general-agent",
                "Cloud General Agent",
                "General purpose governed cloud agent.",
                Set.of("CHAT", "TASK"),
                Set.of("chat", "tool-calling", "workspace"),
                new io.github.pi_java.agent.client.agent.AgentCatalogItemDto.ModelRefDto(
                        "openai-compatible", "gpt-4.1-mini", "openai-compatible:gpt-4.1-mini"),
                Set.of("builtin.read_info", "builtin.write_resource"),
                Set.of("tool:read", "tool:workspace:write"),
                Set.of("LOW", "MEDIUM"),
                Set.of("READ_ONLY", "WORKSPACE_WRITE"),
                List.of(new io.github.pi_java.agent.client.agent.AgentCatalogItemDto.EntryActionDto(
                        "start-chat", "Start chat", "CREATE_RUN", "CHAT", Map.of("inputType", "chat"))),
                Duration.ofSeconds(30),
                Map.of("defaultWorkspacePolicy", "default-workspace-policy"));

        assertThat(item.id()).isEqualTo("cloud-general-agent");
        assertThat(item.supportedInputModes()).containsExactlyInAnyOrder("CHAT", "TASK");
        assertThat(item.capabilities()).contains("tool-calling", "workspace");
        assertThat(item.modelRef().safeRef()).isEqualTo("openai-compatible:gpt-4.1-mini");
        assertThat(item.allowedToolIds()).contains("builtin.read_info", "builtin.write_resource");
        assertThat(item.allowedToolScopes()).contains("tool:read", "tool:workspace:write");
        assertThat(item.riskLabels()).contains("LOW", "MEDIUM");
        assertThat(item.sideEffectLabels()).contains("READ_ONLY", "WORKSPACE_WRITE");
        assertThat(item.entryActions()).extracting(io.github.pi_java.agent.client.agent.AgentCatalogItemDto.EntryActionDto::actionType)
                .contains("CREATE_RUN");
    }

    @Test
    void agentCatalogDtosArePlainClientRecordsWithoutOuterLayerImports() throws Exception {
        Class<?> itemClass = Class.forName("io.github.pi_java.agent.client.agent.AgentCatalogItemDto");
        Class<?> responseClass = Class.forName("io.github.pi_java.agent.client.agent.AgentCatalogResponse");

        assertThat(itemClass.isRecord()).isTrue();
        assertThat(responseClass.isRecord()).isTrue();
        for (RecordComponent component : itemClass.getRecordComponents()) {
            assertThat(component.getType().getName()).doesNotContain("io.github.pi_java.agent.domain");
        }

        Path clientAgentDir = Path.of(System.getProperty("user.dir")).getParent()
                .resolve("pi-agent-client/src/main/java/io/github/pi_java/agent/client/agent");
        for (Path file : Files.list(clientAgentDir).filter(path -> path.toString().endsWith(".java")).toList()) {
            String source = Files.readString(file);
            assertThat(source)
                    .as(file.toString())
                    .doesNotContain("import io.github.pi_java.agent.domain")
                    .doesNotContain("import org.springframework")
                    .doesNotContain("import jakarta");
        }
    }
}
