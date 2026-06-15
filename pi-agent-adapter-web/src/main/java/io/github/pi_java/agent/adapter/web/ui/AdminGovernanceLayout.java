package io.github.pi_java.agent.adapter.web.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("admin/governance")
@PageTitle("Pi Admin Governance")
public class AdminGovernanceLayout extends Main {

    public AdminGovernanceLayout() {
        add(new H1("Pi Admin Governance"));
    }
}
