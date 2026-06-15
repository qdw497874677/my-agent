package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.pi_java.agent.app.usecase.RunCommandService;
import io.github.pi_java.agent.app.usecase.RunQueryService;
import io.github.pi_java.agent.app.usecase.SessionCommandService;
import io.github.pi_java.agent.app.usecase.SessionQueryService;
import io.github.pi_java.agent.app.usecase.ToolRegistryQueryService;
import io.github.pi_java.agent.domain.runtime.AgentRuntime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = PiCloudServerApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebConsoleFoundationTest {

    @Autowired
    private MockMvc mockMvc;

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
        mockMvc.perform(get("/api/sessions").header("X-Pi-Dev-Disable-Defaults", "true"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/console"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/governance"))
                .andExpect(status().isOk());
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
}
