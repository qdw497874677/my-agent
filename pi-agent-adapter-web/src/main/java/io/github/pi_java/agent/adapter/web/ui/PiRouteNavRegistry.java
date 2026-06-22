package io.github.pi_java.agent.adapter.web.ui;

import java.util.List;
import java.util.Optional;

/** Single adapter-web route/navigation source of truth for Console and Admin Governance. */
public final class PiRouteNavRegistry {

    private static final List<PiRouteNavItem> ITEMS = List.of(
            new PiRouteNavItem("console", "Console", "console", "Pi Agent Console", "Console", true, "console"),
            new PiRouteNavItem("admin", "Admin Governance", "admin/governance", "Pi Admin Governance", "Admin", true, "admin-governance"),
            new PiRouteNavItem("admin", "Admin Governance", "admin/governance/overview", "Pi Admin Governance Overview", "Overview", false, "admin-governance-overview"),
            new PiRouteNavItem("admin", "Admin Governance", "admin/governance/registry", "Pi Admin Registry Status", "Registry", false, "admin-registry-status"),
            new PiRouteNavItem("admin", "Admin Governance", "admin/governance/operations", "Pi Admin Operations", "Operations", false, "admin-operations"),
            new PiRouteNavItem("admin", "Admin Governance", "admin/governance/policy-decisions", "Pi Admin Policy Decisions", "Policy Decisions", false, "admin-policy-decisions"),
            new PiRouteNavItem("admin", "Admin Governance", "admin/governance/audits", "Pi Admin Audit Summaries", "Audits", false, "admin-audit-summaries"),
            new PiRouteNavItem("admin", "Admin Governance", "admin/governance/approvals", "Pi Admin Approval Queue", "Approvals", false, "admin-approval-queue"));

    private PiRouteNavRegistry() {
    }

    public static List<PiRouteNavItem> items() {
        return ITEMS;
    }

    public static List<PiRouteNavItem> all() {
        return items();
    }

    public static List<PiRouteNavItem> topLevelItems() {
        return ITEMS.stream().filter(PiRouteNavItem::topLevel).toList();
    }

    public static List<PiRouteNavItem> adminItems() {
        return ITEMS.stream()
                .filter(item -> "admin".equals(item.productArea()))
                .filter(item -> !item.topLevel())
                .toList();
    }

    public static Optional<PiRouteNavItem> findByRoute(String route) {
        String normalized = normalize(route);
        return ITEMS.stream().filter(item -> item.route().equals(normalized)).findFirst();
    }

    public static PiRouteNavItem activeForRoute(String route) {
        String normalized = normalize(route);
        return findByRoute(normalized)
                .or(() -> ITEMS.stream()
                        .filter(item -> normalized.startsWith(item.route() + "/"))
                        .findFirst())
                .orElse(ITEMS.get(0));
    }

    private static String normalize(String route) {
        if (route == null || route.isBlank()) {
            return "console";
        }
        String normalized = route.trim();
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }
}
