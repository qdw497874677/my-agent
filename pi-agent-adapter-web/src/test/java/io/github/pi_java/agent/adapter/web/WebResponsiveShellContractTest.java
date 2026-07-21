package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.AdminGovernanceLandingView;
import io.github.pi_java.agent.adapter.web.ui.PiPageHeader;
import io.github.pi_java.agent.adapter.web.ui.PiPageSection;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;
import io.github.pi_java.agent.adapter.web.ui.PiRouteNavItem;
import io.github.pi_java.agent.adapter.web.ui.PiRouteNavRegistry;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminApprovalQueueView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminAuditView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminGovernanceOverviewView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminOperationsView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminPolicyDecisionsView;
import io.github.pi_java.agent.adapter.web.ui.admin.AdminRegistryStatusView;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WebResponsiveShellContractTest {

    private static final Path THEME_STYLES = Path.of("src/main/frontend/themes/pi-mobile/styles.css");

    @Test
    void routeRegistryContainsConsoleAndAdminNavigation() {
        List<PiRouteNavItem> items = PiRouteNavRegistry.items();

        assertThat(items).hasSize(9);
        assertThat(items).extracting(PiRouteNavItem::route).containsExactly(
                "console",
                "admin/governance",
                "admin/governance/overview",
                "admin/governance/registry",
                "admin/governance/operations",
                "admin/governance/policy-decisions",
                "admin/governance/audits",
                "admin/governance/approvals",
                "admin/governance/providers");
        assertThat(PiRouteNavRegistry.topLevelItems()).extracting(PiRouteNavItem::navLabel)
                .containsExactly("Console", "Admin");
        assertThat(PiRouteNavRegistry.adminItems()).extracting(PiRouteNavItem::navLabel)
                .containsExactly("Overview", "Registry", "Operations", "Policy Decisions", "Audits", "Approvals", "Providers");

        PiRouteNavItem approvals = PiRouteNavRegistry.findByRoute("/admin/governance/approvals").orElseThrow();
        assertThat(approvals.title()).isEqualTo("Pi Admin Approval Queue");
        assertThat(approvals.routeName()).isEqualTo("admin-approval-queue");
        assertThat(PiRouteNavRegistry.activeForRoute("admin/governance/registry").navLabel()).isEqualTo("Registry");
    }

    @Test
    void sharedShellExposesStableHooksAndTitle() {
        PiResponsiveShell shell = new PiResponsiveShell();

        assertThat(shell.getElement().getAttribute("data-shell")).isEqualTo("pi-responsive-shell");
        assertThat(shell.getElement().getAttribute("data-shell-drawer-open")).isEqualTo("false");
        assertThat(hasAttribute(shell, "data-shell-drawer-trigger")).isTrue();
        assertThat(hasAttribute(shell, "data-shell-drawer-close")).isTrue();
        assertThat(hasAttribute(shell, "data-nav")).isTrue();
        assertThat(PiRouteNavRegistry.items()).hasSize(9);
        assertThat(attributeValues(shell, "data-nav-item"))
                .hasSize(2)
                .containsExactly("console", "admin/governance/providers");
        assertThat(hasAttribute(shell, "data-page-title")).isTrue();

        Div routedContent = new Div();
        routedContent.getElement().setAttribute("data-route", "console");
        shell.showRouterLayoutContent(routedContent);

        assertThat(hasAttribute(shell, "data-shell-content")).isTrue();
        assertThat(hasAttribute(shell, "data-route")).isTrue();
    }

    @Test
    void allCurrentRoutesUseSharedShellAndKeepRouteHooks() {
        assertRoute(ConsoleView.class, "console", "console");
        assertRoute(AdminGovernanceLandingView.class, "admin/governance", "admin-governance");
        assertRoute(AdminGovernanceOverviewView.class, "admin/governance/overview", "admin-governance-overview");
        assertRoute(AdminRegistryStatusView.class, "admin/governance/registry", "admin-registry-status");
        assertRoute(AdminOperationsView.class, "admin/governance/operations", "admin-operations");
        assertRoute(AdminPolicyDecisionsView.class, "admin/governance/policy-decisions", "admin-policy-decisions");
        assertRoute(AdminAuditView.class, "admin/governance/audits", "admin-audit-summaries");
        assertRoute(AdminApprovalQueueView.class, "admin/governance/approvals", "admin-approval-queue");
    }

    @Test
    void pagePrimitivesExposeTitleContainerAndActionHooks() {
        PiPageHeader header = new PiPageHeader("Runtime Status").withSubtitle("Healthy").withPrimaryAction(new Div("Refresh"));
        PiPageSection card = PiPageSection.card("runtime", new Div("Runtime details"));
        PiPageSection detail = PiPageSection.detail("audit", new Div("Audit details"));
        PiResponsiveShell shell = new PiResponsiveShell();
        shell.showRouterLayoutContent(new Div(header, card, detail));

        assertThat(header.getClassNames()).contains("pi-page-header");
        assertThat(hasAttribute(header, "data-page-title")).isTrue();
        assertThat(hasAttribute(header, "data-primary-action")).isTrue();
        assertThat(card.getClassNames()).contains("pi-card");
        assertThat(card.getElement().getAttribute("data-section")).isEqualTo("runtime");
        assertThat(detail.getClassNames()).contains("pi-detail");
        assertThat(hasAttribute(shell, "data-shell-content")).isTrue();
    }

    @Test
    void themeDefinesTapTargetFocusAndPagePrimitiveRules() throws IOException {
        String css = Files.readString(THEME_STYLES);

        assertThat(css).contains("--pi-mobile-tap-target: 44px");
        assertThat(css).contains("--pi-mobile-focus-ring");
        assertThat(css).contains(":focus-visible");
        assertThat(css).contains(".pi-compact-control");
        assertThat(css).contains("data-shell-drawer-trigger");
        assertThat(css).contains("data-shell-drawer-close");
        assertThat(css).contains("data-nav-item");
        assertThat(css).contains("data-primary-action");
        assertThat(css).contains("data-action");
        assertThat(css).contains(".pi-page");
        assertThat(css).contains(".pi-page-header");
        assertThat(css).contains(".pi-card");
        assertThat(css).contains(".pi-detail");
        assertThat(css).contains(".pi-action-row");
        assertThat(css).contains("overflow-x");
        assertThat(css).contains("overflow-wrap");
    }

    @Test
    void existingActionHooksInheritMobileInteractionContract() throws IOException {
        String css = Files.readString(THEME_STYLES);
        PiResponsiveShell shell = new PiResponsiveShell();

        assertThat(css).contains("data-action");
        assertThat(css).contains("data-action-plan");
        assertThat(css).contains("data-read-only-refresh");
        assertThat(css).contains("data-plugin-action");
        assertThat(css).contains("data-primary-action");
        assertThat(css).contains("data-shell-drawer-trigger");
        assertThat(css).contains("data-shell-drawer-close");
        assertThat(css).contains("data-nav-item");
        assertThat(hasAttribute(shell, "data-primary-action")).isTrue();
        assertThat(PiRouteNavRegistry.items()).hasSize(9);
        assertThat(attributeValues(shell, "data-nav-item"))
                .hasSize(2)
                .containsExactly("console", "admin/governance/providers");
    }

    @Test
    void consoleControlAndStatusChipThemeVariablesAreScopedToConsoleHome() throws IOException {
        String css = Files.readString(THEME_STYLES);

        assertThat(css).contains(".pi-console-home vaadin-button,\n.pi-console-home button {");
        assertThat(css).contains(".pi-console-home vaadin-combo-box,\n.pi-console-home vaadin-text-area {");
        assertThat(css).contains(".pi-console-home [data-status-chip],\n.pi-console-home .pi-status-chip,\n.pi-console-home .pi-risk-chip {");
        assertThat(css).contains(".pi-console-home [data-status-chip],\n.pi-console-home [data-role=\"fallback-label\"],\n.pi-console-home [data-card-label] {");
        assertThat(css).doesNotContain("vaadin-button,\nbutton {\n  --vaadin-button-background");
        assertThat(css).doesNotContain("vaadin-combo-box,\nvaadin-text-area {\n  --vaadin-input-field-background");
    }

    @Test
    void consoleViewportContainmentKeepsOnlyTheEventFeedScrollable() throws IOException {
        String css = Files.readString(THEME_STYLES);

        assertThat(css)
                .as("Console must contain its route inside the shared shell without changing Admin page scrolling")
                .contains(".pi-shell:has(.pi-console-home)")
                .contains("grid-template-rows: auto minmax(0, 1fr)")
                .contains("height: 100dvh")
                .contains("overflow: hidden")
                .contains(".pi-console-workbench.pi-console-home")
                .contains("grid-template-rows: auto auto auto minmax(0, 1fr)")
                .contains(".pi-console-workbench.pi-console-home .pi-console-panel-chat")
                .contains("height: 100%")
                .contains(".pi-console-chat")
                .contains("grid-template-rows: minmax(0, 1fr) auto")
                .contains(".pi-console-event-feed")
                .contains("overflow-y: auto")
                .contains("line-height: 1.06");
        assertThat(css)
                .as("the prior fixed chat height must not compete with the Console viewport grid")
                .doesNotContain("height: clamp(26rem, 52dvh, 32rem)")
                .doesNotContain("min-height: 26rem");
    }

    @Test
    void ampShellVisualsAreScopedToTheConsoleRoute() throws IOException {
        String css = Files.readString(THEME_STYLES);

        assertThat(css)
                .contains(".pi-shell:has(.pi-console-home) .pi-shell-header")
                .contains(".pi-shell:has(.pi-console-home) .pi-shell-drawer")
                .contains(".pi-shell:has(.pi-console-home) .pi-shell-nav-item")
                .contains(".pi-shell:has(.pi-console-home) .pi-page-title")
                .contains("body:has(.pi-console-home)")
                .contains("background: var(--pi-mobile-shell-muted);")
                .contains("border-bottom: 1px solid var(--pi-mobile-shell-border);")
                .contains("background: var(--pi-mobile-shell-surface);")
                .contains("border-radius: 0.75rem;\n  color: #172033;")
                .contains("font-size: clamp(1.1rem, 2vw, 1.5rem);");
        assertThat(css)
                .doesNotContain("body {\n  overflow-x: hidden;\n  max-width: 100vw;\n  background:\n    radial-gradient")
                .doesNotContain(".pi-shell {\n  display: grid;\n  grid-template-rows: auto 1fr;\n  min-height: 100vh;\n  width: 100%;\n  max-width: 100vw;\n  background: transparent;")
                .doesNotContain(".pi-shell-header {\n  position: sticky;\n  top: 0;\n  z-index: 10;\n  display: grid;\n  grid-template-columns: auto minmax(0, 1fr) auto auto;\n  align-items: center;\n  gap: var(--pi-mobile-space-sm);\n  padding: calc(var(--pi-mobile-safe-area-top) + var(--pi-mobile-space-sm)) var(--pi-mobile-space-md) var(--pi-mobile-space-sm);\n  border-bottom: 1px solid var(--pi-amp-line-soft);");
    }

    private static void assertRoute(Class<? extends Component> routeClass, String route, String routeName) {
        Route annotation = routeClass.getAnnotation(Route.class);
        assertThat(annotation).as("@Route on %s", routeClass.getSimpleName()).isNotNull();
        assertThat(annotation.value()).isEqualTo(route);
        assertThat(annotation.layout()).isEqualTo(PiResponsiveShell.class);
        assertThat(PiRouteNavRegistry.findByRoute(route)).isPresent().get().extracting(PiRouteNavItem::routeName).isEqualTo(routeName);
    }

    private static boolean hasAttribute(Component component, String attribute) {
        return component.getElement().hasAttribute(attribute)
                || component.getChildren().anyMatch(child -> hasAttribute(child, attribute));
    }

    private static List<String> attributeValues(Component component, String attribute) {
        List<String> values = new java.util.ArrayList<>();
        collectAttributeValues(component, attribute, values);
        return values;
    }

    private static void collectAttributeValues(Component component, String attribute, List<String> values) {
        if (component.getElement().hasAttribute(attribute)) {
            values.add(component.getElement().getAttribute(attribute));
        }
        component.getChildren().forEach(child -> collectAttributeValues(child, attribute, values));
    }
}
