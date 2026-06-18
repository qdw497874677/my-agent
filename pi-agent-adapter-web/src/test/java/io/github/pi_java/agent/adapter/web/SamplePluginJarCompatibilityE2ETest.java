package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pi_java.agent.infrastructure.extension.ExtensionRegistrationProperties;
import io.github.pi_java.agent.infrastructure.extension.ExtensionToolRegistry;
import io.github.pi_java.agent.infrastructure.plugin.InMemoryPluginStateStore;
import io.github.pi_java.agent.infrastructure.plugin.PluginGovernanceCatalogAdapter;
import io.github.pi_java.agent.infrastructure.plugin.Pf4jPluginSourceDiscovery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {PiCloudServerApplication.class, SamplePluginJarE2ETest.SamplePluginRuntimeConfiguration.class,
        InMemoryCloudE2EConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "pi.plugins.enabled=true",
        "pi.plugins.non-sandbox-warning-acknowledged=true",
        "pi.plugins.platform-api-version=1.0.0"
})
class SamplePluginJarCompatibilityE2ETest {
    private static final Path EMPTY_CONTROLLED_PLUGIN_DIR = prepareEmptyControlledPluginDirectory();

    @DynamicPropertySource
    static void pluginDirectory(DynamicPropertyRegistry registry) {
        registry.add("pi.plugins.directory", () -> EMPTY_CONTROLLED_PLUGIN_DIR.toString());
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void samplePluginJarCompatibilityFailureRemainsVisibleButUnusable() throws IOException {
        Path pluginDirectory = SamplePluginJarE2ETest.prepareControlledPluginDirectory();
        List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discovered = discover(pluginDirectory, "0.5.0");

        PluginGovernanceCatalogAdapter adapter = new PluginGovernanceCatalogAdapter(discovered, new InMemoryPluginStateStore(),
                new ExtensionRegistrationProperties(Set.of(), Set.of(), false, "0.5.0"));

        assertThat(adapter.plugins()).singleElement().satisfies(plugin -> {
            assertThat(plugin.pluginId()).isEqualTo(SamplePluginJarE2ETest.SAMPLE_PLUGIN_ID);
            assertThat(plugin.compatibilityStatus()).isEqualTo("INCOMPATIBLE");
            assertThat(plugin.enabled()).isFalse();
            assertThat(plugin.relativePathSummary()).contains("pi-sample-plugin-readonly");
        });
        assertThat(new ExtensionToolRegistry(adapter.contributionRegistry()).resolve(SamplePluginJarE2ETest.SAMPLE_TOOL_ID)).isEmpty();
    }

    @Test
    void adminRefreshDiscoversSamplePluginCopiedIntoControlledDirectoryAfterStartup() throws IOException {
        assertThat(get("/api/admin/governance/plugins", String.class)).doesNotContain(SamplePluginJarE2ETest.SAMPLE_PLUGIN_ID);
        assertThat(get("/api/tools", String.class)).doesNotContain(SamplePluginJarE2ETest.SAMPLE_TOOL_ID);

        Path sampleJar = SamplePluginJarE2ETest.samplePluginJar();
        Files.copy(sampleJar, EMPTY_CONTROLLED_PLUGIN_DIR.resolve(sampleJar.getFileName()),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        io.github.pi_java.agent.client.admin.PluginMutationResponse refresh = post("/api/admin/governance/plugins/refresh", null,
                io.github.pi_java.agent.client.admin.PluginMutationResponse.class);

        assertThat(refresh.applied()).isTrue();
        assertThat(refresh.status()).isEqualTo("REFRESHED");
        assertThat(get("/api/admin/governance/plugins", String.class))
                .contains(SamplePluginJarE2ETest.SAMPLE_PLUGIN_ID, SamplePluginJarE2ETest.SAMPLE_TOOL_ID);
        assertThat(get("/api/tools", String.class)).contains(SamplePluginJarE2ETest.SAMPLE_TOOL_ID);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        ResponseEntity<T> response = restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers()), responseType);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private <T> T get(String path, Class<T> responseType) {
        ResponseEntity<T> response = restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers()), responseType);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        headers.add("X-Pi-Dev-Tenant", "tenant-sample-plugin-refresh");
        headers.add("X-Pi-Dev-User", "user-sample-plugin-refresh");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static Path prepareEmptyControlledPluginDirectory() {
        try {
            Path directory = Files.createTempDirectory("pi-sample-plugin-empty-controlled-");
            directory.toFile().deleteOnExit();
            return directory;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to prepare empty controlled sample plugin directory", ex);
        }
    }

    private static List<Pf4jPluginSourceDiscovery.PluginDiscoveredSource> discover(Path pluginDirectory,
                                                                                    String platformApiVersion) {
        PluginManager pluginManager = new DefaultPluginManager(List.of(pluginDirectory));
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        return new Pf4jPluginSourceDiscovery(pluginManager, pluginDirectory, platformApiVersion).discover();
    }
}
