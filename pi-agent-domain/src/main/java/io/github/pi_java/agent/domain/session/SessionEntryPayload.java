package io.github.pi_java.agent.domain.session;

import io.github.pi_java.agent.domain.artifact.Artifact;
import io.github.pi_java.agent.domain.artifact.Attachment;
import io.github.pi_java.agent.domain.artifact.ExternalReference;
import io.github.pi_java.agent.domain.workspace.WorkspaceScope;

/** Typed payloads for append-only session entries. */
public sealed interface SessionEntryPayload permits
        SessionEntryPayload.MessageEntry,
        SessionEntryPayload.ArtifactEntry,
        SessionEntryPayload.AttachmentEntry,
        SessionEntryPayload.ExternalReferenceEntry,
        SessionEntryPayload.MemoryReferenceEntry,
        SessionEntryPayload.WorkspaceScopeEntry,
        SessionEntryPayload.CompactionSummaryEntry,
        SessionEntryPayload.BranchSummaryEntry {

    record MessageEntry(String role, String content) implements SessionEntryPayload {}

    record ArtifactEntry(Artifact artifact) implements SessionEntryPayload {}

    record AttachmentEntry(Attachment attachment) implements SessionEntryPayload {}

    record ExternalReferenceEntry(ExternalReference externalReference) implements SessionEntryPayload {}

    record MemoryReferenceEntry(String memoryRefId, String kind, String summary) implements SessionEntryPayload {}

    record WorkspaceScopeEntry(WorkspaceScope workspaceScope) implements SessionEntryPayload {}

    record CompactionSummaryEntry(String compactionId, String summary) implements SessionEntryPayload {}

    record BranchSummaryEntry(String branchId, String summary) implements SessionEntryPayload {}
}
