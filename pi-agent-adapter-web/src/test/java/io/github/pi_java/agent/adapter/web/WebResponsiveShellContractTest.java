package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.ui.AdminGovernanceLandingView;
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
import org.junit.jupiter.api.Test;

class WebResponsiveShellContractTest {

    @Test
    void routeRegistryContainsConsoleAndAdminNavigation() {
        List<PiRouteNavItem> items = PiRouteNavRegistry.items();

        assertThat(items).hasSize(8);
        assertThat(items).extracting(PiRouteNavItem::route).containsExactly(
                "console",
                "admin/governance",
                "admin/governance/overview",
                "admin/governance/registry",
                "admin/governance/operations",
                "admin/governance/policy-decisions",
                "admin/governance/audits",
                "admin/governance/approvals");
        assertThat(PiRouteNavRegistry.topLevelItems()).extracting(PiRouteNavItem::navLabel)
                .containsExactly("Console", "Admin");
        assertThat(PiRouteNavRegistry.adminItems()).extracting(PiRouteNavItem::navLabel)
                .containsExactly("Overview", "Registry", "Operations", "Policy Decisions", "Audits", "Approvals");

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
        assertThat(countAttribute(shell, "data-nav-item")).isEqualTo(8);
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

    private static long countAttribute(Component component, String attribute) {
        long self = component.getElement().hasAttribute(attribute) ? 1 : 0;
        return self + component.getChildren().mapToLong(child -> countAttribute(child, attribute)).sum();
    }
}
