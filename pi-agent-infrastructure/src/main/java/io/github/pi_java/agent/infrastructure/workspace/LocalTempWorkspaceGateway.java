package io.github.pi_java.agent.infrastructure.workspace;

import io.github.pi_java.agent.domain.workspace.Resource;
import io.github.pi_java.agent.domain.workspace.WorkspaceGateway;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import io.github.pi_java.agent.domain.workspace.WorkspaceSession;
import io.github.pi_java.agent.domain.workspace.WorkspaceSnapshot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded local temporary workspace for deterministic dev/test execution.
 *
 * <p>This implementation is root-constrained and useful for tests and local demos, but it is
 * <strong>not a production sandbox</strong>. Production command/file isolation must be supplied by a
 * stronger workspace/sandbox implementation behind the same Domain ports.</p>
 */
public final class LocalTempWorkspaceGateway implements WorkspaceGateway {
    private final Path baseRoot;
    private final Map<String, WorkspaceSession> sessions = new HashMap<>();
    private final Map<String, Path> sessionRoots = new HashMap<>();
    private final Map<String, WorkspaceSnapshot> snapshots = new HashMap<>();
    private int sessionCounter;
    private int snapshotCounter;

    public LocalTempWorkspaceGateway(Path baseRoot) {
        this.baseRoot = Objects.requireNonNull(baseRoot, "baseRoot must not be null").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to create workspace base root", e);
        }
    }

    @Override
    public synchronized WorkspaceSession openSession(WorkspaceScope scope) {
        Objects.requireNonNull(scope, "scope must not be null");
        String safeRunId = safeSegment(scope.runId());
        String workspaceSessionId = "local-temp-" + safeRunId + "-" + ++sessionCounter;
        Path root = baseRoot.resolve(workspaceSessionId).normalize();
        ensureInsideBase(root);
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to create workspace session root", e);
        }
        WorkspaceSession session = new WorkspaceSession(workspaceSessionId, scope.workspaceId(), scope.runId(), scope, Instant.now());
        sessions.put(workspaceSessionId, session);
        sessionRoots.put(workspaceSessionId, root);
        return session;
    }

    @Override
    public synchronized WorkspaceSnapshot createSnapshot(String workspaceSessionId, String reason) {
        WorkspaceSession session = requireSession(workspaceSessionId);
        WorkspaceSnapshot snapshot = new WorkspaceSnapshot(
                "local-temp-snapshot-" + ++snapshotCounter,
                session.workspaceId(),
                "local-temp-" + snapshotCounter,
                Map.of("reason", reason == null ? "" : reason),
                true,
                Instant.now()
        );
        snapshots.put(snapshot.snapshotId(), snapshot);
        return snapshot;
    }

    @Override
    public synchronized void restoreSnapshot(String workspaceSessionId, String snapshotId) {
        requireSession(workspaceSessionId);
        if (!snapshots.containsKey(snapshotId)) {
            throw new IllegalArgumentException("unknown snapshot " + snapshotId);
        }
    }

    @Override
    public synchronized Optional<Resource> findResource(String workspaceId, String resourceId) {
        return sessions.values().stream()
                .filter(session -> session.workspaceId().equals(workspaceId))
                .filter(session -> Files.exists(resolve(session.workspaceSessionId(), resourceId)))
                .findFirst()
                .map(session -> new Resource(resourceId, "workspace-file", "workspace://" + workspaceId + "/" + resourceId, Map.of()));
    }

    @Override
    public synchronized void closeSession(String workspaceSessionId) {
        Path root = sessionRoots.remove(workspaceSessionId);
        sessions.remove(workspaceSessionId);
        if (root != null) {
            deleteRecursively(root);
        }
    }

    public Path rootFor(String workspaceSessionId) {
        requireSession(workspaceSessionId);
        return sessionRoots.get(workspaceSessionId);
    }

    public void writeText(String workspaceSessionId, String logicalPath, String content, boolean append) {
        Path target = resolve(workspaceSessionId, logicalPath);
        try {
            Files.createDirectories(target.getParent());
            if (append) {
                Files.writeString(target, content, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } else {
                Files.writeString(target, content, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write workspace resource", e);
        }
    }

    public String readText(String workspaceSessionId, String logicalPath) {
        Path target = resolve(workspaceSessionId, logicalPath);
        try {
            return Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read workspace resource", e);
        }
    }

    private Path resolve(String workspaceSessionId, String logicalPath) {
        Path root = rootFor(workspaceSessionId);
        if (logicalPath == null || logicalPath.isBlank()) {
            throw new IllegalArgumentException("logicalPath must not be blank");
        }
        Path requested = Path.of(logicalPath);
        if (requested.isAbsolute()) {
            throw new IllegalArgumentException("path escapes workspace boundary: " + logicalPath);
        }
        Path resolved = root.resolve(requested).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("path escapes workspace boundary: " + logicalPath);
        }
        return resolved;
    }

    private WorkspaceSession requireSession(String workspaceSessionId) {
        WorkspaceSession session = sessions.get(workspaceSessionId);
        if (session == null) {
            throw new IllegalArgumentException("unknown workspace session " + workspaceSessionId);
        }
        return session;
    }

    private void ensureInsideBase(Path root) {
        if (!root.startsWith(baseRoot)) {
            throw new IllegalArgumentException("workspace root escapes base root");
        }
    }

    private static String safeSegment(String value) {
        String input = value == null || value.isBlank() ? "run" : value;
        return input.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private static void deleteRecursively(Path root) {
        try (var walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("failed to clean workspace session", e);
        }
    }
}
