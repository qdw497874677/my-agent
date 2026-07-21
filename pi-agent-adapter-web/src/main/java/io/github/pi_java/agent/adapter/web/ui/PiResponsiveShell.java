package io.github.pi_java.agent.adapter.web.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared Vaadin RouterLayout for Console and Admin Governance responsive navigation. */
@Tag("div")
public class PiResponsiveShell extends Div implements RouterLayout, AfterNavigationObserver {

    private final Button drawerTrigger = new Button();
    private final Button drawerClose = new Button();
    private final Button languageSwitch = new Button();
    private final H2 pageTitle = new H2();
    private final Div statusSlot = new Div();
    private final Div actionSlot = new Div();
    private final Main content = new Main();
    private final List<Anchor> navLinks = new ArrayList<>();

    public PiResponsiveShell() {
        addClassName("pi-shell");
        getElement().setAttribute("data-shell", "pi-responsive-shell");
        getElement().setAttribute("data-shell-drawer-open", "false");
        add(buildHeader(), buildDrawer(), content);
        applyTranslations();
    }

    private void applyTranslations() {
        Locale locale = currentLocale();
        drawerTrigger.setText(getTranslation("shell.menu"));
        drawerClose.setText(getTranslation("shell.close"));
        pageTitle.setText(getTranslation("shell.defaultTitle"));
        drawerTrigger.getElement().setAttribute("aria-label", getTranslation("shell.openNav"));
        drawerClose.getElement().setAttribute("aria-label", getTranslation("shell.closeNav"));
        languageSwitch.setText(getTranslation("lang.switch"));
    }

    private static Locale currentLocale() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null ? session.getLocale() : Locale.ENGLISH;
    }

    @Override
    public void showRouterLayoutContent(HasElement routeContent) {
        content.removeAll();
        content.addClassNames("pi-page", "pi-content");
        content.getElement().setAttribute("data-shell-content", "primary");
        if (routeContent instanceof Component component) {
            content.add(component);
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String route = event.getLocation().getPath();
        PiRouteNavItem active = PiRouteNavRegistry.activeForRoute(route);
        pageTitle.setText(navTitle(active));
        pageTitle.getElement().setAttribute("data-page-title", active.routeName());
        for (Anchor link : navLinks) {
            boolean activeLink = active.route().equals(link.getElement().getAttribute("data-nav-item"));
            link.getElement().setAttribute("data-nav-active", Boolean.toString(activeLink));
            link.getElement().setAttribute("aria-current", activeLink ? "page" : "false");
        }
    }

    private String navTitle(PiRouteNavItem item) {
        return getTranslation("nav." + item.routeName() + ".title");
    }

    private String navLabel(PiRouteNavItem item) {
        return getTranslation("nav." + item.routeName() + ".label");
    }

    private String navGroup(PiRouteNavItem item) {
        return getTranslation("nav." + item.routeName() + ".group");
    }

    private Header buildHeader() {
        Header header = new Header();
        header.addClassName("pi-shell-header");
        header.getElement().setAttribute("data-shell-header", "compact");

        drawerTrigger.addClassNames("pi-shell-drawer-trigger", "pi-tap-target");
        drawerTrigger.getElement().setAttribute("data-shell-drawer-trigger", "true");
        drawerTrigger.addClickListener(event -> setDrawerOpen(true));

        pageTitle.addClassName("pi-page-title");
        pageTitle.getElement().setAttribute("data-page-title", "console");

        statusSlot.addClassName("pi-page-status");
        statusSlot.getElement().setAttribute("data-page-status", "shell");
        actionSlot.addClassName("pi-page-actions");
        actionSlot.getElement().setAttribute("data-primary-action", "shell-action-slot");

        languageSwitch.addClassNames("pi-shell-lang-switch", "pi-tap-target");
        languageSwitch.getElement().setAttribute("data-action", "switch-language");
        languageSwitch.addClickListener(event -> toggleLanguage());

        header.add(drawerTrigger, pageTitle, statusSlot, actionSlot, languageSwitch);
        return header;
    }

    private void toggleLanguage() {
        Locale current = currentLocale();
        Locale next = "zh".equals(current.getLanguage()) ? Locale.ENGLISH : Locale.SIMPLIFIED_CHINESE;
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setLocale(next);
        }
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.setLocale(next);
            ui.getPage().reload();
        }
    }

    private Section buildDrawer() {
        Section drawer = new Section();
        drawer.addClassName("pi-shell-drawer");
        drawer.getElement().setAttribute("data-shell-drawer", "primary");

        drawerClose.addClassNames("pi-shell-drawer-close", "pi-tap-target");
        drawerClose.getElement().setAttribute("data-shell-drawer-close", "true");
        drawerClose.addClickListener(event -> {
            setDrawerOpen(false);
            drawerTrigger.focus();
        });

        Nav nav = new Nav();
        nav.addClassName("pi-shell-nav");
        nav.getElement().setAttribute("data-nav", "primary");
        addGroup(nav, PiRouteNavRegistry.items().stream()
                .filter(item -> "console".equals(item.route()))
                .toList());
        addGroup(nav, PiRouteNavRegistry.items().stream()
                .filter(item -> "admin/governance/providers".equals(item.route()))
                .toList());
        drawer.add(drawerClose, nav);
        return drawer;
    }

    private void addGroup(Nav nav, List<PiRouteNavItem> items) {
        if (items.isEmpty()) {
            return;
        }
        String label = navGroup(items.get(0));
        Div group = new Div();
        group.addClassName("pi-shell-nav-group");
        group.getElement().setAttribute("data-nav-group", label);
        Div groupLabel = new Div(label);
        groupLabel.addClassName("pi-shell-nav-group-label");
        group.add(groupLabel);
        for (PiRouteNavItem item : items) {
            Anchor link = new Anchor(item.href(), navLabel(item));
            link.getElement().setAttribute("href", item.href());
            link.addClassNames("pi-shell-nav-item", "pi-tap-target");
            link.getElement().setAttribute("data-nav-item", item.route());
            link.getElement().setAttribute("data-nav-route-name", item.routeName());
            link.getElement().setAttribute("data-nav-active", "false");
            link.getElement().setAttribute("data-product-area", item.productArea());
            navLinks.add(link);
            group.add(link);
        }
        nav.add(group);
    }

    private void setDrawerOpen(boolean open) {
        getElement().setAttribute("data-shell-drawer-open", Boolean.toString(open));
    }
}
