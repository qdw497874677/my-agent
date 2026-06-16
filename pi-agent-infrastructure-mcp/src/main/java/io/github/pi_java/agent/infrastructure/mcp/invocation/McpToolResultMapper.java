package io.github.pi_java.agent.infrastructure.mcp.invocation;

import io.github.pi_java.agent.domain.tool.ToolExecutionRequest;
import io.github.pi_java.agent.domain.tool.ToolExecutionResult;
import io.github.pi_java.agent.domain.tool.ToolExecutionStatus;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class McpToolResultMapper {
    private final McpInvocationErrorMapper errorMapper;

    public McpToolResultMapper() {
        this(new McpInvocationErrorMapper());
    }

    McpToolResultMapper(McpInvocationErrorMapper errorMapper) {
        this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper must not be null");
    }

    public ToolExecutionResult map(ToolExecutionRequest request, String serverId, String toolName,
                                   McpSchema.CallToolResult result, Duration latency) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(result, "result must not be null");
        if (Boolean.TRUE.equals(result.isError())) {
            return errorMapper.toolError(request, serverId, toolName, latency);
        }
        List<Map<String, Object>> content = result.content() == null ? List.of()
                : result.content().stream().map(this::summarizeContent).toList();
        Map<String, Object> outputSummary = new LinkedHashMap<>();
        outputSummary.put("mcp.serverId", safe(serverId));
        outputSummary.put("mcp.toolName", safe(toolName));
        outputSummary.put("mcp.contentCount", content.size());
        outputSummary.put("mcp.contentTypes", content.stream().map(item -> item.get("type")).distinct().toList());
        outputSummary.put("mcp.redactionHint", "summary-only");
        if (result.structuredContent() != null) {
            outputSummary.put("mcp.hasStructuredContent", true);
        }

        Map<String, Object> rawOutput = new LinkedHashMap<>();
        rawOutput.put("mcp.content", content);
        if (result.structuredContent() != null) {
            rawOutput.put("mcp.structuredContent", safeValue(result.structuredContent()));
        }
        return new ToolExecutionResult(request.toolCallId(), request.toolId(), ToolExecutionStatus.SUCCESS,
                "MCP tool " + safe(serverId) + "/" + safe(toolName) + " completed with " + content.size() + " content item(s).",
                java.util.Optional.empty(), Map.of("mcp.remote", true), outputSummary, Set.of(),
                java.util.Optional.empty(), latency == null ? Duration.ZERO : latency, java.util.Optional.of(rawOutput));
    }

    private Map<String, Object> summarizeContent(McpSchema.Content content) {
        Map<String, Object> summary = new LinkedHashMap<>();
        String type = content == null ? "unknown" : content.type();
        summary.put("type", type == null ? "unknown" : type);
        if (content instanceof McpSchema.TextContent text) {
            summary.put("text", truncate(text.text()));
        } else if (content instanceof McpSchema.ImageContent image) {
            summary.put("mimeType", safe(image.mimeType()));
            summary.put("dataRedacted", true);
        } else if (content instanceof McpSchema.EmbeddedResource resource) {
            summary.put("resourceRef", resource.resource() == null ? "" : resource.resource().toString());
            summary.put("resourceContentRedacted", true);
        } else if (content instanceof McpSchema.ResourceLink link) {
            summary.put("name", safe(link.name()));
            summary.put("uri", safe(link.uri()));
            summary.put("mimeType", safe(link.mimeType()));
        }
        return Map.copyOf(summary);
    }

    private Object safeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(String.valueOf(key), safeValue(item)));
            return Map.copyOf(copy);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            list.forEach(item -> copy.add(safeValue(item)));
            return List.copyOf(copy);
        }
        if (value instanceof String text) {
            return truncate(text);
        }
        return value;
    }

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 512 ? text : text.substring(0, 512) + "…";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
