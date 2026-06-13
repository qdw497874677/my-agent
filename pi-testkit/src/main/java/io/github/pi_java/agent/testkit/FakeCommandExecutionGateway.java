package io.github.pi_java.agent.testkit;

import io.github.pi_java.agent.domain.workspace.CommandExecutionGateway;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FakeCommandExecutionGateway implements CommandExecutionGateway {
    private final Map<List<String>, CommandResult> results = new HashMap<>();

    @Override
    public CommandResult execute(CommandRequest request) {
        return results.getOrDefault(request.command(), new CommandResult(0, "", "", false, false));
    }

    public FakeCommandExecutionGateway register(List<String> command, CommandResult result) {
        results.put(List.copyOf(command), result);
        return this;
    }
}
