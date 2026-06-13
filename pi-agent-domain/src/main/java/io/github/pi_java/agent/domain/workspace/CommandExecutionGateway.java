package io.github.pi_java.agent.domain.workspace;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Domain port for command execution inside a workspace boundary.
 *
 * <p>This contract only describes the request/result shape. It intentionally has
 * no shell implementation and no dependency on host process APIs.</p>
 */
public interface CommandExecutionGateway {
    CommandResult execute(CommandRequest request);

    record CommandRequest(
            String workspaceSessionId,
            List<String> command,
            Map<String, String> environment,
            Duration timeout,
            String cancellationTokenId) {
        public CommandRequest {
            command = List.copyOf(command);
            environment = Map.copyOf(environment);
        }
    }

    record CommandResult(
            int exitCode,
            String outputSummary,
            String errorSummary,
            boolean timedOut,
            boolean cancelled) {}
}
