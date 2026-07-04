package io.github.pi_java.agent.app.usecase;

import io.github.pi_java.agent.client.run.RunProviderMetadata;

import java.time.Instant;
import java.util.Map;

/**
 * App-internal run projection view consumed by the Phase 16 conversation
 * transcript assembler.
 *
 * <p>Carries the minimal run fields the assembler needs to fold a typed
 * transcript: run identity, creation time (for run-ordering per decision
 * D-09 / D-16), the run input map (source of the user message text), and the
 * run status (used to derive active-run state). This is an App-layer
 * orchestration read model, not a public client contract; public transcript
 * output is limited to Plan 01 client DTOs
 * ({@link io.github.pi_java.agent.client.conversation.ConversationMessageDto}
 * and friends).
 *
 * <p>The {@code input} field is defensively normalized: a {@code null} argument
 * becomes an empty immutable map, and a non-null argument is copied into an
 * immutable map so callers cannot mutate the record's state after construction.
 */
public record ConversationRunView(
        String runId,
        Instant createdAt,
        Map<String, Object> input,
        String status,
        RunProviderMetadata providerMetadata) {

    public ConversationRunView(String runId, Instant createdAt, Map<String, Object> input, String status) {
        this(runId, createdAt, input, status, RunProviderMetadata.EMPTY);
    }

    public ConversationRunView {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        input = input == null ? Map.of() : Map.copyOf(input);
        providerMetadata = providerMetadata == null ? RunProviderMetadata.EMPTY : providerMetadata;
    }
}
