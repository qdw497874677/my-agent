package io.github.pi_java.agent.client.admin;

public record PluginMutationRequest(String operation, String reason) {
    public PluginMutationRequest {
        operation = requireSupportedOperation(operation);
        reason = reason == null ? "" : reason;
    }

    private static String requireSupportedOperation(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("operation must not be blank");
        }
        if (!"refresh".equals(value) && !"disable".equals(value) && !"quarantine".equals(value)) {
            throw new IllegalArgumentException("operation must be refresh, disable, or quarantine");
        }
        return value;
    }
}
