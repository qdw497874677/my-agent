package io.github.pi_java.agent.domain.model;

import io.github.pi_java.agent.domain.runtime.CancellationToken;

import java.util.Objects;

@FunctionalInterface
public interface StreamingModelClient {
    void stream(ModelRequest request, CancellationToken cancellationToken, ModelStreamSink sink);

    @FunctionalInterface
    interface ModelStreamSink {
        void accept(ModelStreamChunk chunk);

        default void error(ProviderErrorSummary errorSummary) {
            throw new ModelStreamException(errorSummary);
        }
    }

    final class ModelStreamException extends RuntimeException {
        private final ProviderErrorSummary errorSummary;

        public ModelStreamException(ProviderErrorSummary errorSummary) {
            super(Objects.requireNonNull(errorSummary, "errorSummary must not be null").safeMessage());
            this.errorSummary = errorSummary;
        }

        public ProviderErrorSummary errorSummary() {
            return errorSummary;
        }
    }
}
