package io.github.pi_java.agent.adapter.web.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("admin/governance")
@PageTitle("Pi Admin Governance")
public class AdminGovernanceLayout extends Main {

    public AdminGovernanceLayout() {
        addClassName("pi-admin-governance-surface");
        getElement().setAttribute("data-route", "admin-governance");
        getElement().setAttribute("data-surface", "admin-governance");
        getElement().setAttribute("data-mobile-critical", "true");
        add(
                new H1("Pi Admin Governance"),
                new Paragraph("Separated inspect-only Admin Governance surface for runtime and registry visibility."));
    }
}
