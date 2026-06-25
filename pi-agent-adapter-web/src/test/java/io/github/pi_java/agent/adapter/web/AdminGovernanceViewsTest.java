package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import io.github.pi_java.agent.adapter.web.ui.ConsoleHttpClient;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminGovernanceOverviewView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminAuditView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminPolicyDecisionsView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminRegistryStatusView;
import java.lang.reflect.Method;
import io.github.pi_java.agent.client.admin.AuditSummaryDto;
import io.github.pi_java.agent.client.admin.ExtensionCapabilityDto;
import io.github.pi_java.agent.client.admin.ExtensionGovernanceResponse;
import io.github.pi_java.agent.client.admin.ExtensionSourceDto;
import io.github.pi_java.agent.client.admin.GovernanceOverviewResponse;
import io.github.pi_java.agent.client.admin.GovernanceStatusDto;
import io.github.pi_java.agent.client.admin.PolicyDecisionSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminGovernanceViewsTest {

    @Test
    void adminMobileRedactorConservativelyRedactsSecretLikeValues() throws Exception {
        Class<?> redactor = Class.forName("io.github.pi_java.agent.adapter.web.ui.admin.AdminMobileRedactor");
        Method redact = redactor.getDeclaredMethod("redact", String.class);
        redact.setAccessible(true);

        assertThat((String) redact.invoke(null, "sk-test-secret")).isEqualTo("[REDACTED]");
        assertThat((String) redact.invoke(null, "rawSecret=abc123")).contains("[REDACTED]").doesNotContain("abc123");
        assertThat((String) redact.invoke(null, "apiKey=abc123")).contains("[REDACTED]").doesNotContain("abc123");
        assertThat((String) redact.invoke(null, "password=abc123")).contains("[REDACTED]").doesNotContain("abc123");
        assertThat((String) redact.invoke(null, "bearer abc123")).contains("[REDACTED]").doesNotContain("abc123");
        assertThat((String) redact.invoke(null, "token=abc123")).contains("[REDACTED]").doesNotContain("abc123");
    }

    @Test
    void adminMobileCardSupportCreatesStatusCardsWithStableFieldSelectors() throws Exception {
        Class<?> support = Class.forName("io.github.pi_java.agent.adapter.web.ui.admin.AdminMobileCardSupport");
        Method labelValue = support.getDeclaredMethod("labelValue", String.class, String.class);
        labelValue.setAccessible(true);
        Method statusCard = support.getDeclaredMethod(
                "statusCard", String.class, String.class, String.class, String.class, String.class, Component[].class);
        statusCard.setAccessible(true);

        Component statusField = (Component) labelValue.invoke(null, "owner", "admin");
        Component card = (Component) statusCard.invoke(
                null,
                "runtime",
                "Runtime",
                "HEALTHY",
                "1",
                "Runtime query API available",
                new Component[] {statusField});

        assertThat(card.getElement().getAttribute("data-admin-card")).isEqualTo("true");
        assertThat(card.getElement().getAttribute("data-admin-section")).isEqualTo("runtime");
        assertThat(card.getElement().getClassList()).contains("pi-admin-card");
        assertThat(countElementsWithAttribute(card, "data-admin-field")).isGreaterThanOrEqualTo(4);
    }

    @Test
    void adminMobileCardSupportCreatesCollapsedStructuredDetails() throws Exception {
        Class<?> support = Class.forName("io.github.pi_java.agent.adapter.web.ui.admin.AdminMobileCardSupport");
        Method details = support.getDeclaredMethod("details", String.class, String.class, Component[].class);
        details.setAccessible(true);

        Details component = (Details) details.invoke(null, "Metadata", "structured", new Component[] {new Div()});

        assertThat(component.isOpened()).isFalse();
        assertThat(component.getElement().getAttribute("data-expandable")).isEqualTo("true");
        assertThat(component.getElement().getAttribute("data-admin-details")).isEqualTo("true");
        assertThat(component.getElement().getAttribute("data-detail-layer")).isEqualTo("structured");
    }

    @Test
    void overviewShowsRuntimeRegistryGovernanceAndFuturePlaceholders() {
        AdminGovernanceOverviewView view = new AdminGovernanceOverviewView(new ConsoleHttpClient());
        view.showOverview(sampleOverview());

        assertThat(view.overviewPath()).isEqualTo("/api/admin/governance/overview");
        assertThat(view.registryStatusPath()).isEqualTo("/admin/governance/registry");
        assertThat(view.renderedText())
                .contains("Runtime")
                .contains("HEALTHY")
                .contains("Model Providers")
                .contains("Tool Registry")
                .contains("Extensions")
                .contains("FUTURE_ENABLED")
                .contains("MCP")
                .contains("UNCONFIGURED")
                .contains("Plugins")
                .contains("Policy decisions")
                .contains("Audit summaries");
        assertThat(view.getElement().getAttribute("data-admin-surface")).isEqualTo("inspect-only");
    }

    @Test
    void overviewRendersSixStackedStatusCardsWithoutPipeSeparatedSummaries() {
        AdminGovernanceOverviewView view = new AdminGovernanceOverviewView(new ConsoleHttpClient());
        view.showOverview(sampleOverviewWithSensitiveMetadata());

        assertThat(countElementsWithAttribute(view, "data-admin-overview-card")).isEqualTo(6);
        assertThat(countElementsWithAttributeValue(view, "data-admin-overview-card", "runtime")).isEqualTo(1);
        assertThat(countElementsWithAttributeValue(view, "data-admin-overview-card", "providers")).isEqualTo(1);
        assertThat(countElementsWithAttributeValue(view, "data-admin-overview-card", "toolRegistry")).isEqualTo(1);
        assertThat(countElementsWithAttributeValue(view, "data-admin-overview-card", "extensions")).isEqualTo(1);
        assertThat(countElementsWithAttributeValue(view, "data-admin-overview-card", "mcp")).isEqualTo(1);
        assertThat(countElementsWithAttributeValue(view, "data-admin-overview-card", "plugins")).isEqualTo(1);
        assertThat(countElementsWithAttribute(view, "data-admin-field")).isGreaterThanOrEqualTo(18);
        assertThat(visibleSpanTexts(view)).noneMatch(text -> text.contains(" | "));
    }

    @Test
    void overviewMetadataIsCollapsedRedactedDetailsAndKeepsRouteLinks() {
        AdminGovernanceOverviewView view = new AdminGovernanceOverviewView(new ConsoleHttpClient());
        view.showOverview(sampleOverviewWithSensitiveMetadata());

        assertThat(countElementsWithAttribute(view, "data-admin-details")).isGreaterThanOrEqualTo(6);
        assertThat(view.getChildren().filter(Details.class::isInstance).map(Details.class::cast)).allMatch(details -> !details.isOpened());
        assertThat(renderedComponentText(view)).contains("[REDACTED]").doesNotContain("sk-test-secret").doesNotContain("abc123");
        assertThat(anchorHrefs(view)).contains(
                "/admin/governance/registry",
                "/admin/governance/operations",
                "/admin/governance/policy-decisions",
                "/admin/governance/audits");
    }

    @Test
    void registryStatusViewIsReadOnlyAndShowsProviderToolExtensionMcpPluginStatus() {
        AdminRegistryStatusView view = new AdminRegistryStatusView(new ConsoleHttpClient());
        view.showOverview(sampleOverview());

        assertThat(view.overviewPath()).isEqualTo("/api/admin/governance/overview");
        assertThat(view.extensionGovernancePath()).isEqualTo("/api/admin/governance/extensions");
        assertThat(view.renderedText())
                .contains("Model Providers: HEALTHY")
                .contains("Tool Registry: HEALTHY")
                .contains("Extensions: FUTURE_ENABLED")
                .contains("MCP: UNCONFIGURED")
                .contains("Plugins: FUTURE_ENABLED")
                .contains("mutation=disabled")
                .contains("surface=placeholder");
        assertThat(view.mutationControlsPresent()).isFalse();
        assertThat(view.mutationActionText().toLowerCase())
                .doesNotContain("enable button")
                .doesNotContain("disable")
                .doesNotContain("delete")
                .doesNotContain("load plugin")
                .doesNotContain("add mcp");
    }

    @Test
    void registryStatusViewShowsReadOnlyExtensionSourcesAndCapabilities() {
        AdminRegistryStatusView view = new AdminRegistryStatusView(new ConsoleHttpClient());
        view.showExtensions(sampleExtensions());

        assertThat(view.renderedText())
                .contains("Extension Source: test-spring-extension")
                .contains("kind=SPRING_BEAN")
                .contains("status=USABLE")
                .contains("health=UP")
                .contains("compatibility=COMPATIBLE")
                .contains("enabled=true")
                .contains("Capability: listener.audit")
                .contains("type=EVENT_LISTENER")
                .contains("extension.sourceKind=SPRING_BEAN")
                .contains("[REDACTED]")
                .doesNotContain("enable button")
                .doesNotContain("disable button")
                .doesNotContain("delete");
        assertThat(view.mutationControlsPresent()).isFalse();
    }

    @Test
    void policyDecisionViewShowsRecentRedactedSummariesAndContextLinks() {
        AdminPolicyDecisionsView view = new AdminPolicyDecisionsView(new ConsoleHttpClient());
        view.showPolicyDecisions(sampleOverview().policyDecisions());

        assertThat(view.policyDecisionsPath()).isEqualTo("/api/admin/governance/policy-decisions");
        assertThat(view.renderedText())
                .contains("ALLOW")
                .contains("safe read-only tool")
                .contains("builtin.info")
                .contains("call-1")
                .contains("session-1")
                .contains("run-1")
                .contains("[REDACTED]")
                .doesNotContain("sk-test-secret")
                .doesNotContain("rawSecret");
        assertThat(view.contextLinks()).contains("/console/sessions/session-1", "/console/sessions/session-1/runs/run-1");
        assertThat(view.phaseFiveDeferredControlsPresent()).isFalse();
    }

    @Test
    void auditViewShowsRecentRedactedSummariesAndOmitsSearchFilterExport() {
        AdminAuditView view = new AdminAuditView(new ConsoleHttpClient());
        view.showAudits(sampleOverview().audits());

        assertThat(view.auditsPath()).isEqualTo("/api/admin/governance/audits");
        assertThat(view.renderedText())
                .contains("tool.policy")
                .contains("tool")
                .contains("builtin.info")
                .contains("session-1")
                .contains("run-1")
                .contains("[REDACTED]")
                .doesNotContain("sk-test-secret")
                .doesNotContain("apiKey")
                .doesNotContain("password");
        assertThat(view.contextLinks()).contains("/console/sessions/session-1", "/console/sessions/session-1/runs/run-1");
        assertThat(view.phaseFiveDeferredControlsPresent()).isFalse();
        assertThat(view.controlText().toLowerCase())
                .doesNotContain("search")
                .doesNotContain("filter")
                .doesNotContain("export");
    }

    static GovernanceOverviewResponse sampleOverview() {
        return new GovernanceOverviewResponse(
                new GovernanceStatusDto("runtime", "HEALTHY", "Runtime query API available", 1, Map.of("mode", "cloud")),
                new GovernanceStatusDto("providers", "HEALTHY", "Model provider registry available", 1, Map.of("defaultProvider", "openai-compatible")),
                new GovernanceStatusDto("toolRegistry", "HEALTHY", "Governed tool registry available", 2, Map.of("surface", "read-only")),
                new GovernanceStatusDto("extensions", "FUTURE_ENABLED", "SPI and Spring extension governance arrives in Phase 6", 0, Map.of("surface", "placeholder", "mutation", "disabled")),
                new GovernanceStatusDto("mcp", "UNCONFIGURED", "Remote MCP governance arrives in Phase 7", 0, Map.of("surface", "placeholder", "mutation", "disabled")),
                new GovernanceStatusDto("plugins", "FUTURE_ENABLED", "Dynamic plugin governance arrives in Phase 8", 0, Map.of("surface", "placeholder", "mutation", "disabled")),
                List.of(new PolicyDecisionSummaryDto("decision-1", "ALLOW", "safe read-only tool", "builtin.info", "call-1", "session-1", "run-1", Instant.parse("2026-06-15T00:00:00Z"), Map.of("summary", "[REDACTED]"))),
                List.of(new AuditSummaryDto("audit-1", "tool.policy", "tool", "builtin.info", "session-1", "run-1", Instant.parse("2026-06-15T00:00:01Z"), Map.of("decision", "ALLOW", "secret", "[REDACTED]"))),
                Instant.parse("2026-06-15T00:02:00Z"));
    }

    static GovernanceOverviewResponse sampleOverviewWithSensitiveMetadata() {
        return new GovernanceOverviewResponse(
                new GovernanceStatusDto("runtime", "HEALTHY", "Runtime query API available", 1, Map.of("mode", "cloud", "apiKey", "abc123")),
                new GovernanceStatusDto("providers", "HEALTHY", "Model provider registry available", 1, Map.of("defaultProvider", "openai-compatible", "token", "sk-test-secret")),
                new GovernanceStatusDto("toolRegistry", "HEALTHY", "Governed tool registry available", 2, Map.of("surface", "read-only")),
                new GovernanceStatusDto("extensions", "FUTURE_ENABLED", "SPI and Spring extension governance arrives in Phase 6", 0, Map.of("surface", "placeholder", "mutation", "disabled")),
                new GovernanceStatusDto("mcp", "UNCONFIGURED", "Remote MCP governance arrives in Phase 7", 0, Map.of("surface", "placeholder", "authorization", "bearer abc123")),
                new GovernanceStatusDto("plugins", "FUTURE_ENABLED", "Dynamic plugin governance arrives in Phase 8", 0, Map.of("surface", "placeholder", "rawSecret", "abc123")),
                sampleOverview().policyDecisions(),
                sampleOverview().audits(),
                Instant.parse("2026-06-15T00:02:00Z"));
    }

    static ExtensionGovernanceResponse sampleExtensions() {
        return new ExtensionGovernanceResponse(List.of(new ExtensionSourceDto(
                "test-spring-extension",
                "Test Spring Extension",
                "1.0.0",
                "SPRING_BEAN",
                "USABLE",
                true,
                "COMPATIBLE",
                "UP",
                "",
                List.of(new ExtensionCapabilityDto(
                        "listener.audit",
                        "EVENT_LISTENER",
                        "USABLE",
                        "1.0.0",
                        "test-spring-extension",
                        true,
                        "COMPATIBLE",
                        "UP",
                        Map.of("extension.sourceKind", "SPRING_BEAN", "error", "[REDACTED]"))))));
    }

    private static long countElementsWithAttribute(Component component, String attribute) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(component.getElement()),
                component.getElement().getChildren().flatMap(AdminGovernanceViewsTest::descendants))
                .filter(element -> element.hasAttribute(attribute))
                .count();
    }

    private static long countElementsWithAttributeValue(Component component, String attribute, String value) {
        return elementStream(component)
                .filter(element -> value.equals(element.getAttribute(attribute)))
                .count();
    }

    private static java.util.stream.Stream<String> visibleSpanTexts(Component component) {
        return componentTree(component)
                .filter(Span.class::isInstance)
                .map(Span.class::cast)
                .map(Span::getText);
    }

    private static String renderedComponentText(Component component) {
        return elementStream(component)
                .map(com.vaadin.flow.dom.Element::getText)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static java.util.List<String> anchorHrefs(Component component) {
        return component.getChildren()
                .flatMap(AdminGovernanceViewsTest::componentTree)
                .filter(Anchor.class::isInstance)
                .map(Anchor.class::cast)
                .map(anchor -> anchor.getHref())
                .toList();
    }

    private static java.util.stream.Stream<Component> componentTree(Component component) {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(component), component.getChildren().flatMap(AdminGovernanceViewsTest::componentTree));
    }

    private static java.util.stream.Stream<com.vaadin.flow.dom.Element> elementStream(Component component) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(component.getElement()),
                component.getElement().getChildren().flatMap(AdminGovernanceViewsTest::descendants));
    }

    private static java.util.stream.Stream<com.vaadin.flow.dom.Element> descendants(com.vaadin.flow.dom.Element element) {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(element),
                element.getChildren().flatMap(AdminGovernanceViewsTest::descendants));
    }
}
