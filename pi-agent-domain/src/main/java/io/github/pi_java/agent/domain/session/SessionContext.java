package io.github.pi_java.agent.domain.session;

import io.github.pi_java.agent.domain.artifact.Artifact;
import io.github.pi_java.agent.domain.artifact.Attachment;
import io.github.pi_java.agent.domain.artifact.ExternalReference;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import java.util.List;
import java.util.Optional;

/** Reconstructed active runtime context for the session's current leaf. */
public record SessionContext(
        List<SessionEntryPayload.MessageEntry> messages,
        List<Artifact> artifacts,
        List<Attachment> attachments,
        List<ExternalReference> externalReferences,
        List<SessionEntryPayload.MemoryReferenceEntry> memoryRefs,
        Optional<WorkspaceScope> workspaceScope,
        List<SessionEntry> activeEntryPath) {
    public SessionContext {
        messages = List.copyOf(messages);
        artifacts = List.copyOf(artifacts);
        attachments = List.copyOf(attachments);
        externalReferences = List.copyOf(externalReferences);
        memoryRefs = List.copyOf(memoryRefs);
        activeEntryPath = List.copyOf(activeEntryPath);
    }
}
