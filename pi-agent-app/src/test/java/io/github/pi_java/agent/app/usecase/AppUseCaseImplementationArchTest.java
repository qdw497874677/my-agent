package io.github.pi_java.agent.app.usecase;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppUseCaseImplementationArchTest {

    @Test
    void defaultServicesDoNotImportFrameworkPersistenceServletOrSseTypes() throws IOException {
        List<Path> defaultServices;
        try (var stream = Files.walk(Path.of("src/main/java/io/github/pi_java/agent/app/usecase"))) {
            defaultServices = stream
                    .filter(path -> path.getFileName().toString().startsWith("Default"))
                    .filter(path -> path.getFileName().toString().endsWith("Service.java"))
                    .toList();
        }

        assertThat(defaultServices).isNotEmpty();
        for (Path service : defaultServices) {
            String source = Files.readString(service);
            assertThat(source).doesNotContain("org.springframework", "jakarta.servlet", "javax.servlet",
                    "JdbcTemplate", "SseEmitter", "DataSource");
        }
    }
}
