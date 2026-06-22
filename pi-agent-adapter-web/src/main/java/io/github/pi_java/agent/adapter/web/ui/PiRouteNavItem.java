package io.github.pi_java.agent.adapter.web.ui;

/** Immutable route metadata used by the shared responsive shell and browser gates. */
public record PiRouteNavItem(
        String productArea,
        String groupLabel,
        String route,
        String title,
        String navLabel,
        boolean topLevel,
        String routeName) {

    public PiRouteNavItem {
        productArea = requireText(productArea, "productArea");
        groupLabel = requireText(groupLabel, "groupLabel");
        route = normalizeRoute(route);
        title = requireText(title, "title");
        navLabel = requireText(navLabel, "navLabel");
        routeName = requireText(routeName, "routeName");
    }

    public String href() {
        return "/" + route;
    }

    private static String normalizeRoute(String value) {
        String route = requireText(value, "route");
        return route.startsWith("/") ? route.substring(1) : route;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
