package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.app.usecase.ToolRegistryQueryService;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = PiCloudServerApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebConsoleFoundationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private SessionCommandService sessionCommandService;

    @MockBean
    private SessionQueryService sessionQueryService;

    @MockBean
    private RunCommandService runCommandService;

    @MockBean
    private RunQueryService runQueryService;

    @MockBean
    private io.github.pi_java.agent.adapter.web.controller.RunController.RunActivationTrigger runActivationTrigger;

    @MockBean
    private ToolRegistryQueryService toolRegistryQueryService;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private TransactionTemplate transactionTemplate;

    @MockBean
    private AgentRuntime agentRuntime;

    @Test
    void vaadinDependenciesStayInAdapterWebOnly() throws Exception {
        Path projectRoot = Path.of(System.getProperty("user.dir")).getParent();
        String rootPom = Files.readString(projectRoot.resolve("pom.xml"));
        String adapterPom = Files.readString(projectRoot.resolve("pi-agent-adapter-web/pom.xml"));
        String domainPom = Files.readString(projectRoot.resolve("pi-agent-domain/pom.xml"));
        String appPom = Files.readString(projectRoot.resolve("pi-agent-app/pom.xml"));

        assertThat(rootPom).contains("vaadin-bom");
        assertThat(adapterPom).contains("vaadin-spring-boot-starter");
        assertThat(domainPom).doesNotContain("vaadin");
        assertThat(appPom).doesNotContain("vaadin");
    }

    @Test
    void consoleAndAdminRoutesAreReachableButApiStaysAuthenticated() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Pi-Dev-Disable-Defaults", "true");
        ResponseEntity<String> apiResponse = restTemplate.exchange(
                "/api/sessions", HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers), String.class);
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> consoleResponse = restTemplate.getForEntity("/console", String.class);
        ResponseEntity<String> adminResponse = restTemplate.getForEntity("/admin/governance", String.class);

        assertThat(consoleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void uiPackageDoesNotImportPrivateAppDomainOrPersistenceTypes() throws Exception {
        Path uiRoot = Path.of(System.getProperty("user.dir"), "src/main/java/io/github/pi_java/agent/adapter/web/ui");
        try (Stream<Path> files = Files.walk(uiRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertThat(source)
                        .as(file.toString())
                        .doesNotContain("import io.github.pi_java.agent.app.usecase")
                        .doesNotContain("import io.github.pi_java.agent.domain")
                        .doesNotContain("repository")
                        .doesNotContain("JdbcTemplate")
                        .doesNotContain("TransactionTemplate");
            }
        }
    }

    @Test
    void consoleClientHelpersBuildOnlyPublicApiAndStreamUrls() {
        ConsoleHttpClient client = new ConsoleHttpClient();
        EventStreamClient streamClient = new EventStreamClient();

        assertThat(client.createSessionPath()).isEqualTo("/api/sessions");
        assertThat(client.sessionPath("session-1")).isEqualTo("/api/sessions/session-1");
        assertThat(client.sessionHistoryPath("session-1")).isEqualTo("/api/sessions/session-1/history");
        assertThat(client.createRunPath("session-1")).isEqualTo("/api/sessions/session-1/runs");
        assertThat(client.runEventsPath("session-1", "run-1", 42)).isEqualTo("/api/sessions/session-1/runs/run-1/events?afterSequence=42&limit=500");
        assertThat(client.cancelRunPath("session-1", "run-1")).isEqualTo("/api/sessions/session-1/runs/run-1/cancel");

        EventStreamClient.ConnectionSpec spec = streamClient.runEventStream("session-1", "run-1", 42);
        assertThat(spec.url()).isEqualTo("/api/sessions/session-1/runs/run-1/stream?afterSequence=42");
        assertThat(spec.withCredentials()).isTrue();
        assertThat(spec.eventSourceExpression()).contains("EventSource").contains(spec.url());
    }
}
