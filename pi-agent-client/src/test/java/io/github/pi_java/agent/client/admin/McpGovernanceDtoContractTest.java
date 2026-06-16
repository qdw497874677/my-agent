package io.github.pi_java.agent.client.admin;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class McpGovernanceDtoContractTest {

    @Test
    void mcpGovernanceDtosArePublicRecordsWithNoMutationFields() {
        McpToolDto tool = new McpToolDto(
                "server-1:files.read",
                "files.read",
                "AVAILABLE",
                true,
                false,
                false,
                "object{path:string}",
                "",
                Map.of("policy", "read-only", "secret", "<redacted>"));
        McpServerDto server = new McpServerDto(
                "server-1",
                "Filesystem MCP",
                true,
                "stdio",
                "token:<redacted>",
                "HEALTHY",
                "HEALTHY",
                1,
                Instant.parse("2026-06-15T12:00:00Z"),
                "",
                List.of(tool),
                Map.of("mutation", "disabled"));
        McpGovernanceResponse response = new McpGovernanceResponse(List.of(server));
        McpRefreshResponse refresh = new McpRefreshResponse(
                true,
                1,
                1,
                0,
                "HEALTHY",
                "",
                Map.of("surface", "read-only"));

        assertThat(McpGovernanceResponse.class.isRecord()).isTrue();
        assertThat(McpServerDto.class.isRecord()).isTrue();
        assertThat(McpToolDto.class.isRecord()).isTrue();
        assertThat(McpRefreshResponse.class.isRecord()).isTrue();
        assertThat(response.servers()).containsExactly(server);
        assertThat(server.tools()).containsExactly(tool);
        assertThat(tool.metadata()).containsEntry("secret", "<redacted>");
        assertThat(refresh.refreshed()).isTrue();
        assertThat(componentNames(McpServerDto.class))
                .doesNotContain("create", "edit", "delete", "configuration");
        assertThat(componentTypes(McpToolDto.class))
                .contains(Map.class)
                .doesNotContain(Object.class);
    }

    private static List<String> componentNames(Class<?> recordType) {
        return List.of(recordType.getRecordComponents()).stream()
                .map(RecordComponent::getName)
                .toList();
    }

    private static List<Class<?>> componentTypes(Class<?> recordType) {
        return List.of(recordType.getRecordComponents()).stream()
                .map(RecordComponent::getType)
                .toList();
    }
}
