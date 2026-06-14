package io.github.pi_java.agent.adapter.web;

import io.github.pi_java.agent.domain.model.ModelUsage;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiSpringAiModelFactory;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiStreamEvent;
import io.github.pi_java.agent.infrastructure.model.openai.OpenAiStreamSource;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class FakeOpenAiProviderE2EConfiguration {

    @Bean
    @Primary
    OpenAiSpringAiModelFactory fakeOpenAiSpringAiModelFactory() {
        return config -> new FakeOpenAiStreamSource();
    }

    private static final class FakeOpenAiStreamSource implements OpenAiStreamSource {
        @Override
        public Iterable<OpenAiStreamEvent> stream(String prompt, io.github.pi_java.agent.domain.runtime.CancellationToken cancellationToken) {
            return List.of(
                    OpenAiStreamEvent.text("fake-openai "),
                    OpenAiStreamEvent.text("delta"),
                    OpenAiStreamEvent.finish("stop", new ModelUsage(3, 2, 5)));
        }
    }
}
