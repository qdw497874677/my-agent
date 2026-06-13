package io.github.pi_java.agent.domain.session;

import io.github.pi_java.agent.domain.artifact.Artifact;
import io.github.pi_java.agent.domain.artifact.Attachment;
import io.github.pi_java.agent.domain.artifact.ExternalReference;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Rebuilds active session context by walking current leaf parent links. */
public final class SessionContextResolver {
    public SessionContext resolve(Session session) {
        Map<String, SessionEntry> entriesById = new HashMap<>();
        for (SessionEntry entry : session.entries()) {
            entriesById.put(entry.entryId(), entry);
        }

        List<SessionEntry> leafToRoot = new ArrayList<>();
        String cursor = session.currentLeafEntryId();
        while (cursor != null) {
            SessionEntry entry = entriesById.get(cursor);
            if (entry == null) {
                throw new IllegalArgumentException("Session current leaf path references missing entry: " + cursor);
            }
            leafToRoot.add(entry);
            cursor = entry.parentEntryId();
        }

        List<SessionEntry> activePath = leafToRoot.reversed();
        List<SessionEntryPayload.MessageEntry> messages = new ArrayList<>();
        List<Artifact> artifacts = new ArrayList<>();
        List<Attachment> attachments = new ArrayList<>();
        List<ExternalReference> externalReferences = new ArrayList<>();
        List<SessionEntryPayload.MemoryReferenceEntry> memoryRefs = new ArrayList<>();
        WorkspaceScope workspaceScope = null;

        for (SessionEntry entry : activePath) {
            switch (entry.payload()) {
                case SessionEntryPayload.MessageEntry message -> messages.add(message);
                case SessionEntryPayload.ArtifactEntry artifact -> artifacts.add(artifact.artifact());
                case SessionEntryPayload.AttachmentEntry attachment -> attachments.add(attachment.attachment());
                case SessionEntryPayload.ExternalReferenceEntry externalReference ->
                        externalReferences.add(externalReference.externalReference());
                case SessionEntryPayload.MemoryReferenceEntry memoryReference -> memoryRefs.add(memoryReference);
                case SessionEntryPayload.WorkspaceScopeEntry scope -> workspaceScope = scope.workspaceScope();
                case SessionEntryPayload.CompactionSummaryEntry ignored -> {
                    // Compaction summaries are path entries but do not replace separated context lists yet.
                }
                case SessionEntryPayload.BranchSummaryEntry ignored -> {
                    // Branch summaries are path entries but do not replace separated context lists yet.
                }
            }
        }

        return new SessionContext(
                messages,
                artifacts,
                attachments,
                externalReferences,
                memoryRefs,
                Optional.ofNullable(workspaceScope),
                activePath);
    }
}
