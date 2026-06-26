package io.github.pi_java.agent.adapter.web.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/** Admin Governance landing page; visual shell/navigation is owned by {@link PiResponsiveShell}. */
@Route(value = "admin/governance", layout = PiResponsiveShell.class)
@PageTitle("Pi Admin Governance")
public class AdminGovernanceLandingView extends Main {

    public AdminGovernanceLandingView() {
        addClassName("pi-admin-governance-surface");
        getElement().setAttribute("data-route", "admin-governance");
        getElement().setAttribute("data-surface", "admin-governance");
        getElement().setAttribute("data-mobile-critical", "true");
        add(
                new H1(getTranslation("admin.landing.heading")),
                new Paragraph(getTranslation("admin.landing.description")));
    }
}
