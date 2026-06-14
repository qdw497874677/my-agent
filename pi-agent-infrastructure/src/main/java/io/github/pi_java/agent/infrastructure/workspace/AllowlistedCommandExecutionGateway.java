package io.github.pi_java.agent.infrastructure.workspace;

import io.github.pi_java.agent.domain.workspace.CommandExecutionGateway;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Command executor for bounded local-temp dev/test workspaces.
 *
 * <p>Commands are executed only after an explicit executable allowlist check, with the process
 * working directory set to a {@link LocalTempWorkspaceGateway} session root and a sanitized
 * environment. This is <strong>not a production sandbox</strong> and must not be used as an unrestricted
 * host shell.</p>
 */
public final class AllowlistedCommandExecutionGateway implements CommandExecutionGateway {
    private final LocalTempWorkspaceGateway workspaceGateway;
    private final Set<String> allowedExecutables;
    private final Duration defaultTimeout;
    private final int summaryLimitChars;

    public AllowlistedCommandExecutionGateway(
            LocalTempWorkspaceGateway workspaceGateway,
            Set<String> allowedExecutables,
            Duration defaultTimeout,
            int summaryLimitChars
    ) {
        this.workspaceGateway = Objects.requireNonNull(workspaceGateway, "workspaceGateway must not be null");
        this.allowedExecutables = Set.copyOf(Objects.requireNonNull(allowedExecutables, "allowedExecutables must not be null"));
        this.defaultTimeout = Objects.requireNonNull(defaultTimeout, "defaultTimeout must not be null");
        if (defaultTimeout.isNegative() || defaultTimeout.isZero()) {
            throw new IllegalArgumentException("defaultTimeout must be positive");
        }
        if (summaryLimitChars < 1) {
            throw new IllegalArgumentException("summaryLimitChars must be positive");
        }
        this.summaryLimitChars = summaryLimitChars;
    }

    @Override
    public CommandResult execute(CommandRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.command().isEmpty() || request.command().getFirst().isBlank()) {
            return new CommandResult(126, "", "command is empty", false, false);
        }
        String executable = request.command().getFirst();
        if (!allowedExecutables.contains(executable)) {
            return new CommandResult(126, "", "command not allowlisted: " + executable, false, false);
        }
        Duration timeout = request.timeout() == null || request.timeout().isNegative() || request.timeout().isZero()
                ? defaultTimeout
                : request.timeout();
        java.util.List<String> processCommand = new java.util.ArrayList<>(request.command());
        processCommand.set(0, resolvedExecutable(executable));
        ProcessBuilder processBuilder = new ProcessBuilder(processCommand);
        processBuilder.directory(workspaceGateway.rootFor(request.workspaceSessionId()).toFile());
        Map<String, String> environment = processBuilder.environment();
        environment.clear();
        environment.put("PATH", "/usr/bin:/bin");
        environment.putAll(sanitizedEnvironment(request.environment()));
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(124, "", "command timed out after " + timeout.toMillis() + "ms", true, false);
            }
            String out = truncate(read(process.getInputStream().readAllBytes()));
            String err = truncate(read(process.getErrorStream().readAllBytes()));
            return new CommandResult(process.exitValue(), out, err, false, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CommandResult(130, "", "command cancelled", false, true);
        } catch (IOException e) {
            return new CommandResult(1, "", truncate(e.getClass().getSimpleName() + ": " + e.getMessage()), false, false);
        }
    }

    private static Map<String, String> sanitizedEnvironment(Map<String, String> requested) {
        Map<String, String> sanitized = new HashMap<>();
        for (Map.Entry<String, String> entry : requested.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.matches("[A-Z_][A-Z0-9_]*") && !key.contains("SECRET") && !key.contains("TOKEN")) {
                sanitized.put(key, entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return sanitized;
    }

    private static String resolvedExecutable(String executable) {
        if (executable.contains("/")) {
            return executable;
        }
        java.nio.file.Path usrBin = java.nio.file.Path.of("/usr/bin", executable);
        if (java.nio.file.Files.isExecutable(usrBin)) {
            return usrBin.toString();
        }
        java.nio.file.Path bin = java.nio.file.Path.of("/bin", executable);
        if (java.nio.file.Files.isExecutable(bin)) {
            return bin.toString();
        }
        return executable;
    }

    private static String read(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String truncate(String value) {
        if (value.length() <= summaryLimitChars) {
            return value;
        }
        return value.substring(0, summaryLimitChars) + "... [truncated]";
    }
}
