package io.github.pi_java.agent.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class WebPhase15AccessibilityContractTest {

    private static final Path THEME_STYLES = Path.of("src/main/frontend/themes/pi-mobile/styles.css");

    @Test
    void d10ReducedMotionContractMinimizesShellDrawerAnimation() throws IOException {
        String css = css();

        assertThat(css)
                .as("D-10 reduced-motion users must not be forced through shell drawer transitions")
                .contains("prefers-reduced-motion: reduce")
                .contains(".pi-shell-drawer")
                .contains("transition: none")
                .contains("scroll-behavior: auto");
    }

    @Test
    void d11KeyboardFocusContractCoversRepresentativeControls() throws IOException {
        String css = css();

        assertThat(css)
                .as("D-11 visible focus must cover shell, Console, runtime, approval, and Admin controls")
                .contains("focus-visible")
                .contains("--pi-mobile-focus-ring")
                .contains("data-shell-drawer-trigger")
                .contains("data-nav-item")
                .contains("data-action")
                .contains("data-primary-action")
                .contains("data-risk-action")
                .contains("data-read-only-refresh");
    }

    @Test
    void d12NoHoverContractKeepsCriticalActionsVisibleOnTouchDevices() throws IOException {
        String css = css();

        assertThat(css)
                .as("D-12 critical actions must not depend on hover-only affordances")
                .contains("hover: none")
                .contains("touch-action: manipulation")
                .contains("data-action")
                .contains("data-primary-action")
                .contains("data-risk-action")
                .contains("data-admin-action-link");
    }

    @Test
    void d13TabletBridgeContractPreventsPrematureDesktopOverflow() throws IOException {
        String css = css();

        assertThat(css)
                .as("D-13 tablet bridge should sit between phone and desktop breakpoints")
                .contains("min-width: 641px")
                .contains("max-width: 899px")
                .contains(".pi-console-workbench")
                .contains(".pi-admin-card-grid")
                .contains("grid-template-columns")
                .contains("minmax(0, 1.4fr)");
        assertThat(Pattern.compile("@media\\s*\\(min-width:\\s*641px\\)\\s*and\\s*\\(max-width:\\s*899px\\)", Pattern.DOTALL)
                .matcher(css)
                .find())
                .as("D-13 tablet bridge media query should remain explicit and bounded")
                .isTrue();
    }

    private static String css() throws IOException {
        return Files.readString(THEME_STYLES);
    }
}
