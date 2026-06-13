package io.github.pi_java.agent.domain.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.domain.artifact.Artifact;
import io.github.pi_java.agent.domain.artifact.Attachment;
import io.github.pi_java.agent.domain.artifact.ExternalReference;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SessionTreeContextResolverTest {

    @Test
    void resolvesCurrentLeafPathFromRootToLeaf() {
        SessionEntry root = entry("root", null, new SessionEntryPayload.MessageEntry("system", "You are Pi."));
        SessionEntry message = entry("message-1", "root", new SessionEntryPayload.MessageEntry("user", "Create report"));
        Artifact artifact = new Artifact("artifact-1", "document", "Report", "Draft report", Map.of("format", "md"));
        SessionEntry artifactEntry = entry("artifact-1-entry", "message-1", new SessionEntryPayload.ArtifactEntry(artifact));
        SessionEntry branchSummary = entry(
                "branch-summary-1",
                "artifact-1-entry",
                new SessionEntryPayload.BranchSummaryEntry("branch-1", "Report branch summarized"));
        Session session = new Session(
                "session-1",
                "tenant-1",
                "user-1",
                branchSummary.entryId(),
                List.of(root, message, artifactEntry, branchSummary));

        SessionContext context = new SessionContextResolver().resolve(session);

        assertThat(context.activeEntryPath()).extracting(SessionEntry::entryId)
                .containsExactly("root", "message-1", "artifact-1-entry", "branch-summary-1");
        assertThat(context.messages()).extracting(SessionEntryPayload.MessageEntry::content)
                .containsExactly("You are Pi.", "Create report");
        assertThat(context.artifacts()).containsExactly(artifact);
    }

    @Test
    void separatesMessagesArtifactsAttachmentsExternalReferencesMemoryRefsAndWorkspaceScope() {
        Artifact artifact = new Artifact("artifact-1", "document", "Report", "Draft report", Map.of());
        Attachment attachment = new Attachment("attachment-1", "image", "diagram.png", "Architecture diagram", Map.of());
        ExternalReference reference = new ExternalReference(
                "reference-1", "url", "https://example.com/spec", "Spec", Map.of("source", "test"));
        WorkspaceScope workspaceScope = new WorkspaceScope(
                "tenant-1",
                "user-1",
                "session-1",
                "run-1",
                "workspace-1",
                Set.of("resource-1"),
                Set.of("mount-1"));
        List<SessionEntry> entries = List.of(
                entry("root", null, new SessionEntryPayload.MessageEntry("system", "System prompt")),
                entry("artifact", "root", new SessionEntryPayload.ArtifactEntry(artifact)),
                entry("attachment", "artifact", new SessionEntryPayload.AttachmentEntry(attachment)),
                entry("external", "attachment", new SessionEntryPayload.ExternalReferenceEntry(reference)),
                entry("memory", "external", new SessionEntryPayload.MemoryReferenceEntry("memory-1", "semantic", "Memory summary")),
                entry("workspace", "memory", new SessionEntryPayload.WorkspaceScopeEntry(workspaceScope)),
                entry("compaction", "workspace", new SessionEntryPayload.CompactionSummaryEntry("compaction-1", "Compacted root context")));
        Session session = new Session("session-1", "tenant-1", "user-1", "compaction", entries);

        SessionContext context = new SessionContextResolver().resolve(session);

        assertThat(context.messages()).extracting(SessionEntryPayload.MessageEntry::content).containsExactly("System prompt");
        assertThat(context.artifacts()).containsExactly(artifact);
        assertThat(context.attachments()).containsExactly(attachment);
        assertThat(context.externalReferences()).containsExactly(reference);
        assertThat(context.memoryRefs()).extracting(SessionEntryPayload.MemoryReferenceEntry::memoryRefId)
                .containsExactly("memory-1");
        assertThat(context.workspaceScope()).contains(workspaceScope);
        assertThat(context.activeEntryPath()).hasSize(7);
    }

    private static SessionEntry entry(String id, String parentId, SessionEntryPayload payload) {
        return new SessionEntry(id, parentId, Instant.parse("2026-06-13T00:00:00Z"), payload);
    }
}
