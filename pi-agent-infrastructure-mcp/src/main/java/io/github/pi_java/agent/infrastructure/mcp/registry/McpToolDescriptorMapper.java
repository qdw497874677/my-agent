package io.github.pi_java.agent.infrastructure.mcp.registry;

import io.github.pi_java.agent.domain.tool.ToolDescriptor;
import io.github.pi_java.agent.domain.tool.ToolProvenance;
import io.github.pi_java.agent.domain.tool.ToolRiskLevel;
import io.github.pi_java.agent.domain.tool.ToolSchema;
import io.github.pi_java.agent.domain.tool.ToolSideEffect;
import io.github.pi_java.agent.infrastructure.mcp.config.McpServerProperties;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class McpToolDescriptorMapper {
    static final String SCHEMA_DIALECT = "json-schema";
    static final int DEFAULT_PAYLOAD_LIMIT_BYTES = 64 * 1024;

    public ToolDescriptor toDescriptor(McpServerProperties server, McpServerRegistry.DiscoveredTool tool) {
        Objects.requireNonNull(server, "server must not be null");
        Objects.requireNonNull(tool, "tool must not be null");
        McpSchema.Tool sdkTool = tool.sdkTool();
        McpSchema.ToolAnnotations annotations = sdkTool.annotations();
        String toolName = requireNonBlank(sdkTool.name(), "tool.name");
        String id = "mcp." + server.id() + "." + toolName;
        Map<String, Object> metadata = metadata(server, sdkTool, annotations, tool.available(), tool.redactedError());
        return new ToolDescriptor(
                id,
                toolName,
                sdkTool.description() == null ? "" : sdkTool.description(),
                new ToolSchema(SCHEMA_DIALECT, schemaDocument(sdkTool.inputSchema()), Set.of(), DEFAULT_PAYLOAD_LIMIT_BYTES),
                Optional.empty(),
                new ToolProvenance(ToolProvenance.SourceKind.MCP, server.id(), "mcp:" + server.id() + ":" + toolName,
                        Map.of("transport", server.transport().name(), "displayName", server.displayName())),
                version(sdkTool),
                Set.of("tool:mcp", "mcp:server:" + server.id(), "mcp:tool:" + server.id() + ":" + toolName),
                riskLevel(annotations),
                sideEffect(annotations),
                timeout(server),
                metadata);
    }

    public String descriptorId(String serverId, String toolName) {
        return "mcp." + requireNonBlank(serverId, "serverId") + "." + requireNonBlank(toolName, "toolName");
    }

    private static Map<String, Object> schemaDocument(McpSchema.JsonSchema schema) {
        if (schema == null) {
            return Map.of("type", "object");
        }
        Map<String, Object> document = new LinkedHashMap<>();
        if (schema.type() != null && !schema.type().isBlank()) {
            document.put("type", schema.type());
        }
        if (schema.properties() != null && !schema.properties().isEmpty()) {
            document.put("properties", schema.properties());
        }
        if (schema.required() != null && !schema.required().isEmpty()) {
            document.put("required", List.copyOf(schema.required()));
        }
        if (schema.additionalProperties() != null) {
            document.put("additionalProperties", schema.additionalProperties());
        }
        if (schema.defs() != null && !schema.defs().isEmpty()) {
            document.put("$defs", schema.defs());
        }
        if (schema.definitions() != null && !schema.definitions().isEmpty()) {
            document.put("definitions", schema.definitions());
        }
        return document.isEmpty() ? Map.of("type", "object") : Map.copyOf(document);
    }

    private static Map<String, Object> metadata(McpServerProperties server, McpSchema.Tool sdkTool,
                                                McpSchema.ToolAnnotations annotations, boolean available,
                                                String redactedError) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("mcp.serverId", server.id());
        metadata.put("mcp.toolName", sdkTool.name());
        metadata.put("mcp.transport", server.transport().name());
        metadata.put("mcp.remote", true);
        metadata.put("mcp.external", true);
        metadata.put("mcp.available", available);
        metadata.put("mcp.readOnlyHint", bool(annotations == null ? null : annotations.readOnlyHint()));
        metadata.put("mcp.destructiveHint", bool(annotations == null ? null : annotations.destructiveHint()));
        metadata.put("mcp.idempotentHint", bool(annotations == null ? null : annotations.idempotentHint()));
        metadata.put("mcp.openWorldHint", bool(annotations == null ? null : annotations.openWorldHint()));
        if (annotations != null && annotations.title() != null && !annotations.title().isBlank()) {
            metadata.put("mcp.title", annotations.title());
        }
        if (redactedError != null && !redactedError.isBlank()) {
            metadata.put("mcp.redactedError", redactedError);
        }
        return Map.copyOf(metadata);
    }

    private static ToolRiskLevel riskLevel(McpSchema.ToolAnnotations annotations) {
        if (bool(annotations == null ? null : annotations.destructiveHint())) {
            return ToolRiskLevel.HIGH;
        }
        if (bool(annotations == null ? null : annotations.readOnlyHint())
                && !bool(annotations == null ? null : annotations.openWorldHint())) {
            return ToolRiskLevel.LOW;
        }
        return ToolRiskLevel.MEDIUM;
    }

    private static ToolSideEffect sideEffect(McpSchema.ToolAnnotations annotations) {
        if (bool(annotations == null ? null : annotations.destructiveHint())) {
            return ToolSideEffect.DESTRUCTIVE;
        }
        if (bool(annotations == null ? null : annotations.readOnlyHint())
                && !bool(annotations == null ? null : annotations.openWorldHint())) {
            return ToolSideEffect.READ_ONLY;
        }
        return ToolSideEffect.EXTERNAL_WRITE;
    }

    private static Duration timeout(McpServerProperties server) {
        return server.timeout();
    }

    private static String version(McpSchema.Tool sdkTool) {
        Object version = sdkTool.meta() == null ? null : sdkTool.meta().get("version");
        return version instanceof String string && !string.isBlank() ? string : "mcp";
    }

    private static boolean bool(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
