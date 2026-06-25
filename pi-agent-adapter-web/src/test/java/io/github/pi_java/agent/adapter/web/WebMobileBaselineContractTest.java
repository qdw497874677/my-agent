package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.theme.Theme;
import io.github.pi_java.agent.adapter.web.ui.AdminGovernanceLandingView;
import io.github.pi_java.agent.adapter.web.ui.PiWebAppShell;
import io.github.pi_java.agent.adapter.web.ui.console.ConsoleView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WebMobileBaselineContractTest {

    private static final Path THEME_STYLES = Path.of(
            "src/main/frontend/themes/pi-mobile/styles.css");

    @Test
    void appShellOwnsProjectMobileTheme() {
        Theme theme = PiWebAppShell.class.getAnnotation(Theme.class);

        assertThat(theme)
                .as("Vaadin AppShell must wire the project-owned mobile baseline theme")
                .isNotNull();
        assertThat(theme.value()).isEqualTo("pi-mobile");
    }

    @Test
    void consoleRootExposesStableMobileCriticalHooks() {
        ConsoleView view = new ConsoleView();

        assertAttribute(view, "data-route", "console");
        assertAttribute(view, "data-layout", "three-column-workbench");
        assertAttribute(view, "data-mobile-critical", "true");
    }

    @Test
    void adminGovernanceRootExposesStableMobileCriticalHooks() {
        AdminGovernanceLandingView layout = new AdminGovernanceLandingView();

        assertAttribute(layout, "data-route", "admin-governance");
        assertAttribute(layout, "data-surface", "admin-governance");
        assertAttribute(layout, "data-mobile-critical", "true");
    }

    @Test
    void projectMobileThemeContainsResponsiveBaselineRules() throws IOException {
        String css = Files.readString(THEME_STYLES);

        assertThat(css).contains("box-sizing");
        assertThat(css).contains("overflow-x");
        assertThat(css).contains("max-width");
        assertThat(css).contains("overflow-wrap");
    }

    @Test
    void projectMobileThemeContainsAdminCardDetailContract() throws IOException {
        String css = Files.readString(THEME_STYLES);

        assertThat(css).contains(".pi-admin-card");
        assertThat(css).contains(".pi-admin-field");
        assertThat(css).contains(".pi-admin-details");
        assertThat(css).contains(".pi-admin-nested-card");
        assertThat(css).contains("[data-admin-details]");
        assertThat(css).contains("data-status-severity");
        assertThat(css).contains("overflow-wrap: anywhere");
    }

    private static void assertAttribute(Component component, String attribute, String expected) {
        assertThat(component.getElement().getAttribute(attribute))
                .as("%s on %s", attribute, component.getClass().getSimpleName())
                .isEqualTo(expected);
    }
}
