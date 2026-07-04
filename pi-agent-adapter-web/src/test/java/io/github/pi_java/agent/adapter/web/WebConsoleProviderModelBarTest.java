package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfig;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigController;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigStore;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.EventStreamClient;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleRunExecutionBridge;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import io.github.pi_java.agent.adapter.web.ui.console.RunEventRenderer;
import io.github.pi_java.agent.client.agent.AgentCatalogResponse;
import io.github.pi_java.agent.client.event.EventHistoryResponse;
import io.github.pi_java.agent.client.run.CancelRunRequest;
import io.github.pi_java.agent.client.run.CreateRunRequest;
import io.github.pi_java.agent.client.run.RunResponse;
import io.github.pi_java.agent.client.run.RunStatusResponse;
import io.github.pi_java.agent.client.session.SessionResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebConsoleProviderModelBarTest {

    @TempDir
    Path tempDir;

    @Test
    void notReadyProviderReturnsNotConfiguredStateAndSafeMessage() {
        ProviderConfigController controller = controllerWith(new ProviderConfig(false, "https://example.invalid/v1", "", "gpt-4.1-mini", "openai-compatible", "/chat/completions"));

        ProviderConfigController.ModelListResponse response = controller.listModels();

        assertThat(response.models()).isEmpty();
        assertThat(response.state()).isEqualTo("not_configured");
        assertThat(response.ready()).isFalse();
        assertThat(response.modelCount()).isZero();
        assertThat(response.selectedModel()).isEqualTo("gpt-4.1-mini");
        assertThat(response.providerId()).isEqualTo("openai-compatible");
        assertThat(response.message()).containsIgnoringCase("enable").containsIgnoringCase("api key");
        assertThat(response.error()).isNull();
    }

    @Test
    void readyProviderWithModelsReturnsSuccessStateCountAndNoError() throws Exception {
        try (ModelServer server = ModelServer.responding(200, "{\"data\":[{\"id\":\"z-model\"},{\"id\":\"a-model\"}]}", null)) {
            ProviderConfigController controller = controllerWith(readyConfig(server.baseUrl(), "sk-live-success-secret", "z-model"));

            ProviderConfigController.ModelListResponse response = controller.listModels();

            assertThat(response.state()).isEqualTo("success");
            assertThat(response.models()).containsExactly("a-model", "z-model");
            assertThat(response.modelCount()).isEqualTo(2);
            assertThat(response.ready()).isTrue();
            assertThat(response.selectedModel()).isEqualTo("z-model");
            assertThat(response.providerId()).isEqualTo("openai-compatible");
            assertThat(response.message()).contains("2");
            assertThat(response.error()).isNull();
        }
    }

    @Test
    void readyProviderWithNoModelIdsReturnsEmptyStateAndExplanation() throws Exception {
        try (ModelServer server = ModelServer.responding(200, "{\"data\":[{\"object\":\"model\"}]}", null)) {
            ProviderConfigController controller = controllerWith(readyConfig(server.baseUrl(), "sk-empty-secret", "gpt-empty"));

            ProviderConfigController.ModelListResponse response = controller.listModels();

            assertThat(response.state()).isEqualTo("empty");
            assertThat(response.models()).isEmpty();
            assertThat(response.modelCount()).isZero();
            assertThat(response.ready()).isTrue();
            assertThat(response.message()).containsIgnoringCase("no models");
            assertThat(response.error()).isNull();
        }
    }

    @Test
    void providerFailureReturnsRedactedErrorStateWithoutSecrets() throws Exception {
        String apiKey = "sk-test-super-secret-token";
        String rawBody = "Bearer " + apiKey + " should never be shown in UI because it is sensitive and long";
        try (ModelServer server = ModelServer.responding(503, rawBody, "Bearer " + apiKey)) {
            ProviderConfigController controller = controllerWith(readyConfig(server.baseUrl(), apiKey, "gpt-error"));

            ProviderConfigController.ModelListResponse response = controller.listModels();

            assertThat(response.state()).isEqualTo("error");
            assertThat(response.models()).isEmpty();
            assertThat(response.ready()).isTrue();
            assertThat(response.modelCount()).isZero();
            assertThat(response.message()).containsIgnoringCase("provider").doesNotContain(apiKey).doesNotContain("Bearer");
            assertThat(response.error()).doesNotContain(apiKey).doesNotContain("Bearer");
            assertThat(response.error().length()).isLessThanOrEqualTo(180);
        }
    }

    @Test
    void initialConsoleModelAreaExposesStableSelectorsAndReadiness() {
        ProviderConfigStore store = storeWith(readyConfig("https://example.invalid/v1", "sk-ready", "gpt-ready"));
        ConsoleView view = consoleView(store, new ProviderConfigController(store));

        assertThat(onlyDescendantWithAttribute(view, "data-role", "provider-status")).isNotNull();
        assertThat(onlyDescendantWithAttribute(view, "data-role", "model-selector")).isNotNull();
        assertThat(onlyDescendantWithAttribute(view, "data-action", "refresh-models")).isNotNull();
        Element status = onlyDescendantWithAttribute(view, "data-role", "provider-status").getElement();
        assertThat(status.getAttribute("data-provider-ready")).isEqualTo("true");
        assertThat(status.getTextRecursively()).contains("openai-compatible").contains("gpt-ready");
    }

    @Test
    void refreshSuccessEmptyAndErrorSetStateAndVisibleLocalizedCopy() throws Exception {
        try (ModelServer successServer = ModelServer.responding(200, "{\"data\":[{\"id\":\"alpha\"}]}", null)) {
            ProviderConfigStore store = storeWith(readyConfig(successServer.baseUrl(), "sk-success", "alpha"));
            ConsoleView view = consoleView(store, new ProviderConfigController(store));

            clickRefresh(view);

            Element refreshStatus = onlyDescendantWithAttribute(view, "data-role", "model-refresh-status").getElement();
            assertThat(refreshStatus.getAttribute("data-refresh-state")).isEqualTo("success");
            assertThat(refreshStatus.getTextRecursively()).contains("1").containsIgnoringCase("model");
            assertThat(((ComboBox<?>) onlyDescendantWithAttribute(view, "data-role", "model-selector")).getListDataView().getItems())
                    .containsExactly("alpha");
        }

        try (ModelServer emptyServer = ModelServer.responding(200, "{\"data\":[]}", null)) {
            ProviderConfigStore store = storeWith(readyConfig(emptyServer.baseUrl(), "sk-empty", "alpha"));
            ConsoleView view = consoleView(store, new ProviderConfigController(store));

            clickRefresh(view);

            Element refreshStatus = onlyDescendantWithAttribute(view, "data-role", "model-refresh-status").getElement();
            assertThat(refreshStatus.getAttribute("data-refresh-state")).isEqualTo("empty");
            assertThat(refreshStatus.getTextRecursively()).containsIgnoringCase("no models");
        }

        try (ModelServer errorServer = ModelServer.responding(500, "raw failure with Bearer sk-visible-secret", null)) {
            ProviderConfigStore store = storeWith(readyConfig(errorServer.baseUrl(), "sk-visible-secret", "alpha"));
            ConsoleView view = consoleView(store, new ProviderConfigController(store));

            clickRefresh(view);

            Element refreshStatus = onlyDescendantWithAttribute(view, "data-role", "model-refresh-status").getElement();
            assertThat(refreshStatus.getAttribute("data-refresh-state")).isEqualTo("error");
            assertThat(refreshStatus.getTextRecursively())
                    .containsIgnoringCase("provider")
                    .doesNotContain("sk-visible-secret")
                    .doesNotContain("Bearer");
        }
    }

    @Test
    void redactedProviderErrorCopyIsActionableWithoutRawExceptionDump() throws Exception {
        try (ModelServer errorServer = ModelServer.responding(401, "Authorization failed for Bearer sk-action-secret", null)) {
            ProviderConfigStore store = storeWith(readyConfig(errorServer.baseUrl(), "sk-action-secret", "alpha"));
            ConsoleView view = consoleView(store, new ProviderConfigController(store));

            clickRefresh(view);

            String visible = onlyDescendantWithAttribute(view, "data-role", "model-refresh-status").getElement().getTextRecursively();
            assertThat(visible).containsIgnoringCase("check").containsIgnoringCase("provider");
            assertThat(visible).doesNotContain("RestClientResponseException").doesNotContain("sk-action-secret").doesNotContain("Bearer");
        }
    }

    private ConsoleView consoleView(ProviderConfigStore store, ProviderConfigController controller) {
        return new ConsoleView(new ConsoleHttpClient(), new EventStreamClient(), context -> new AgentCatalogResponse(List.of()),
                new NoopBridge(), new RunEventRenderer(), store, controller);
    }

    private ProviderConfigController controllerWith(ProviderConfig config) {
        return new ProviderConfigController(storeWith(config));
    }

    private ProviderConfigStore storeWith(ProviderConfig config) {
        ProviderConfigStore store = new ProviderConfigStore(tempDir.resolve("provider-" + System.nanoTime() + ".db").toString());
        store.update(config);
        return store;
    }

    private static ProviderConfig readyConfig(String baseUrl, String apiKey, String modelId) {
        return new ProviderConfig(true, baseUrl, apiKey, modelId, "openai-compatible", "/chat/completions");
    }

    private static void clickRefresh(ConsoleView view) {
        ((Button) onlyDescendantWithAttribute(view, "data-action", "refresh-models")).click();
    }

    private static Component onlyDescendantWithAttribute(Component root, String attribute, String value) {
        List<Component> matches = descendants(root).stream()
                .filter(component -> value.equals(component.getElement().getAttribute(attribute)))
                .toList();
        assertThat(matches).as(attribute + "=" + value).hasSize(1);
        return matches.getFirst();
    }

    private static List<Component> descendants(Component root) {
        List<Component> found = new ArrayList<>();
        collect(root, found);
        return found;
    }

    private static void collect(Component component, List<Component> found) {
        found.add(component);
        component.getChildren().forEach(child -> collect(child, found));
    }

    private static final class ModelServer implements AutoCloseable {
        private final HttpServer server;

        private ModelServer(HttpServer server) {
            this.server = server;
        }

        static ModelServer responding(int status, String body, String authHeaderEcho) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/models", exchange -> {
                if (authHeaderEcho != null) {
                    exchange.getResponseHeaders().add("X-Auth-Echo", authHeaderEcho);
                }
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            });
            server.start();
            return new ModelServer(server);
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class NoopBridge implements ConsoleRunExecutionBridge {
        @Override
        public SessionResponse createSession() {
            return new SessionResponse("tenant", "user", "session", "workspace", null, "ACTIVE", Instant.now(), Instant.now(), Map.of());
        }

        @Override
        public RunResponse createRun(String sessionId, CreateRunRequest request) {
            return new RunResponse("tenant", "user", sessionId, "run", "workspace", "QUEUED", "trace", "correlation", Instant.now(), Instant.now());
        }

        @Override
        public EventHistoryResponse listEvents(String sessionId, String runId, long afterSequence) {
            return new EventHistoryResponse(sessionId, runId, List.of(), afterSequence, afterSequence, false);
        }

        @Override
        public RunStatusResponse cancelRun(String sessionId, String runId, CancelRunRequest request) {
            return new RunStatusResponse(sessionId, runId, "cancelled", true, Instant.now(), "trace", "correlation");
        }
    }
}
