package io.github.pi_java.agent.client.api;

public record ApiResponse<T>(
        T data,
        ErrorResponse error,
        String traceId,
        String correlationId) {
}
