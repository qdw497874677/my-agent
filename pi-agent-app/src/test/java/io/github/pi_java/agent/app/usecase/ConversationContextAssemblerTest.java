package io.github.pi_java.agent.app.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ConversationContextAssemblerTest {

    @Test
    void policyDefaultsAreConservativeAndExposeTranscriptLimit() {
        ConversationContextPolicy policy = ConversationContextPolicy.defaults();

        assertThat(policy.maxRecentMessages()).isEqualTo(12);
        assertThat(policy.maxTotalCharacters()).isEqualTo(12_000);
        assertThat(policy.transcriptLimit()).isGreaterThanOrEqualTo(policy.maxRecentMessages());
    }

    @Test
    void policyRejectsNonPositiveBudgets() {
        assertThatThrownBy(() -> new ConversationContextPolicy(0, 12_000, 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRecentMessages");
        assertThatThrownBy(() -> new ConversationContextPolicy(12, 0, 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxTotalCharacters");
        assertThatThrownBy(() -> new ConversationContextPolicy(12, 12_000, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transcriptLimit");
    }

    @Test
    void metadataRecordsContextCountsAndTruncationState() {
        ConversationContextMetadata metadata = new ConversationContextMetadata(
                3,
                2,
                4,
                12_000,
                450,
                true);

        assertThat(metadata.includedCount()).isEqualTo(3);
        assertThat(metadata.droppedCount()).isEqualTo(2);
        assertThat(metadata.excludedCount()).isEqualTo(4);
        assertThat(metadata.maxTotalCharacters()).isEqualTo(12_000);
        assertThat(metadata.resultingCharacters()).isEqualTo(450);
        assertThat(metadata.truncated()).isTrue();
    }
}
