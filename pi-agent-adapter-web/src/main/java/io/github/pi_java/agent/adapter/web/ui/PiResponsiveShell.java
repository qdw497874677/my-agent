package io.github.pi_java.agent.adapter.web.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
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
import java.util.ArrayList;
import java.util.List;

/** Shared Vaadin RouterLayout for Console and Admin Governance responsive navigation. */
@Tag("div")
public class PiResponsiveShell extends Div implements RouterLayout, AfterNavigationObserver {

    private final Button drawerTrigger = new Button("Menu");
    private final Button drawerClose = new Button("Close");
    private final H2 pageTitle = new H2("Pi Agent Console");
    private final Div statusSlot = new Div();
    private final Div actionSlot = new Div();
    private final Main content = new Main();
    private final List<Anchor> navLinks = new ArrayList<>();

    public PiResponsiveShell() {
        addClassName("pi-shell");
        getElement().setAttribute("data-shell", "pi-responsive-shell");
        getElement().setAttribute("data-shell-drawer-open", "false");
        add(buildHeader(), buildDrawer(), content);
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
        pageTitle.setText(active.title());
        pageTitle.getElement().setAttribute("data-page-title", active.routeName());
        for (Anchor link : navLinks) {
            boolean activeLink = active.route().equals(link.getElement().getAttribute("data-nav-item"));
            link.getElement().setAttribute("data-nav-active", Boolean.toString(activeLink));
            link.getElement().setAttribute("aria-current", activeLink ? "page" : "false");
        }
    }

    private Header buildHeader() {
        Header header = new Header();
        header.addClassName("pi-shell-header");
        header.getElement().setAttribute("data-shell-header", "compact");

        drawerTrigger.addClassNames("pi-shell-drawer-trigger", "pi-tap-target");
        drawerTrigger.getElement().setAttribute("aria-label", "Open navigation");
        drawerTrigger.getElement().setAttribute("data-shell-drawer-trigger", "true");
        drawerTrigger.addClickListener(event -> setDrawerOpen(true));

        pageTitle.addClassName("pi-page-title");
        pageTitle.getElement().setAttribute("data-page-title", "console");

        statusSlot.addClassName("pi-page-status");
        statusSlot.getElement().setAttribute("data-page-status", "shell");
        actionSlot.addClassName("pi-page-actions");
        actionSlot.getElement().setAttribute("data-primary-action", "shell-action-slot");

        header.add(drawerTrigger, pageTitle, statusSlot, actionSlot);
        return header;
    }

    private Section buildDrawer() {
        Section drawer = new Section();
        drawer.addClassName("pi-shell-drawer");
        drawer.getElement().setAttribute("data-shell-drawer", "primary");

        drawerClose.addClassNames("pi-shell-drawer-close", "pi-tap-target");
        drawerClose.getElement().setAttribute("aria-label", "Close navigation");
        drawerClose.getElement().setAttribute("data-shell-drawer-close", "true");
        drawerClose.addClickListener(event -> {
            setDrawerOpen(false);
            drawerTrigger.focus();
        });

        Nav nav = new Nav();
        nav.addClassName("pi-shell-nav");
        nav.getElement().setAttribute("data-nav", "primary");
        addGroup(nav, "Console", PiRouteNavRegistry.topLevelItems().stream()
                .filter(item -> "console".equals(item.productArea()))
                .toList());
        addGroup(nav, "Admin Governance", PiRouteNavRegistry.items().stream()
                .filter(item -> "admin".equals(item.productArea()))
                .toList());
        drawer.add(drawerClose, nav);
        return drawer;
    }

    private void addGroup(Nav nav, String label, List<PiRouteNavItem> items) {
        Div group = new Div();
        group.addClassName("pi-shell-nav-group");
        group.getElement().setAttribute("data-nav-group", label);
        Div groupLabel = new Div(label);
        groupLabel.addClassName("pi-shell-nav-group-label");
        group.add(groupLabel);
        for (PiRouteNavItem item : items) {
            Anchor link = new Anchor(item.href(), item.navLabel());
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
