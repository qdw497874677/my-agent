package io.github.pi_java.agent.infrastructure.model.openai;

import io.github.pi_java.agent.domain.common.PlatformIds.RunId;
import io.github.pi_java.agent.domain.common.PlatformIds.StepId;
import io.github.pi_java.agent.domain.model.ProviderErrorSummary;
import io.github.pi_java.agent.domain.tool.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiToolCallAccumulatorTest {

    private final OpenAiToolCallAccumulator accumulator = new OpenAiToolCallAccumulator(
            new RunId("run-openai"), new StepId("step-openai"));

    @Test
    void accumulatesFragmentedToolCallOnlyWhenJsonArgumentsAreComplete() {
        assertThat(accumulator.add(new OpenAiToolCallAccumulator.Fragment(0, "call_1", "get_weather", "{"))).isEmpty();
        assertThat(accumulator.add(new OpenAiToolCallAccumulator.Fragment(0, null, null, "\"city\":\"Hangzhou\","))).isEmpty();

        Optional<ToolCall> completed = accumulator.add(new OpenAiToolCallAccumulator.Fragment(0, null, null, "\"unit\":\"celsius\"}"));

        assertThat(completed).isPresent();
        assertThat(completed.orElseThrow().toolCallId()).isEqualTo("call_1");
        assertThat(completed.orElseThrow().toolName()).isEqualTo("get_weather");
        assertThat(completed.orElseThrow().arguments())
                .containsEntry("city", "Hangzhou")
                .containsEntry("unit", "celsius");
    }

    @Test
    void incompleteArgumentsRemainPendingUntilFinishedValidation() {
        accumulator.add(new OpenAiToolCallAccumulator.Fragment(1, "call_2", "search", "{\"query\":"));

        ProviderErrorSummary error = accumulator.validateComplete().orElseThrow();

        assertThat(error.piError().category().name()).isEqualTo("MODEL");
        assertThat(error.providerCode()).isEqualTo("tool_call_arguments_incomplete");
        assertThat(error.retryable()).isFalse();
        assertThat(error.recoverable()).isTrue();
        assertThat(error.safeMessage()).doesNotContain("sk-").doesNotContain("Bearer");
    }

    @Test
    void invalidJsonArgumentsProduceProviderError() {
        accumulator.add(new OpenAiToolCallAccumulator.Fragment(2, "call_3", "broken", "not-json"));

        ProviderErrorSummary error = accumulator.validateComplete().orElseThrow();

        assertThat(error.providerCode()).isEqualTo("tool_call_arguments_invalid");
        assertThat(error.retryable()).isFalse();
        assertThat(error.recoverable()).isTrue();
    }
}
