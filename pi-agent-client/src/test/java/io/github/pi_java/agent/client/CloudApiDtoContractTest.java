package io.github.pi_java.agent.client;

import io.github.pi_java.agent.client.api.ApiResponse;
import io.github.pi_java.agent.client.api.ErrorResponse;
import io.github.pi_java.agent.client.api.PageResponse;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudApiDtoContractTest {

    @Test
    void createRunRequestIsNotChatOnly() {
        CreateRunRequest request = new CreateRunRequest(
                "general-agent",
                "structured",
                Map.of("form", Map.of("priority", "high")),
                "workspace-1",
                Map.of("source", "contract-test"));

        assertThat(request.inputType()).isEqualTo("structured");
        assertThat(request.input()).containsKey("form");
        assertThat(componentNames(CreateRunRequest.class))
                .contains("agentId", "inputType", "input", "workspaceId", "metadata")
                .doesNotContain("chatTranscript");
    }

    @Test
    void sessionCentricRunResponsesExposeRunId() {
        RunResponse run = new RunResponse(
                "tenant-1",
                "user-1",
                "session-1",
                "run-1",
                "workspace-1",
                "RUNNING",
                "trace-1",
                "corr-1",
                null,
                null);
        RunStatusResponse status = new RunStatusResponse(
                "session-1",
                "run-1",
                "RUNNING",
                false,
                null,
                "trace-1",
                "corr-1");

        assertThat(run.sessionId()).isEqualTo("session-1");
        assertThat(run.runId()).isEqualTo("run-1");
        assertThat(status.sessionId()).isEqualTo("session-1");
        assertThat(status.runId()).isEqualTo("run-1");
    }

    @Test
    void restDtosDoNotImportDomainTypes() throws Exception {
        Path dtoRoot = Path.of("pi-agent-client", "src", "main", "java");

        if (!Files.exists(dtoRoot)) {
            return;
        }

        List<Path> dtoFiles;
        try (var stream = Files.walk(dtoRoot)) {
            dtoFiles = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }

        assertThat(dtoFiles).isNotEmpty();
        for (Path dtoFile : dtoFiles) {
            assertThat(Files.readString(dtoFile))
                    .as(dtoFile.toString())
                    .doesNotContain("import io.github.pi_java.agent.domain");
        }

        assertThat(ApiResponse.class.isRecord()).isTrue();
        assertThat(ErrorResponse.class.isRecord()).isTrue();
        assertThat(PageResponse.class.isRecord()).isTrue();
    }

    private static List<String> componentNames(Class<?> recordType) {
        return List.of(recordType.getRecordComponents()).stream()
                .map(RecordComponent::getName)
                .toList();
    }
}
