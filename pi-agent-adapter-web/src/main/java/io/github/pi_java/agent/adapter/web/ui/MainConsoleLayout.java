package io.github.pi_java.agent.adapter.web.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("console")
@PageTitle("Pi Agent Console")
public class MainConsoleLayout extends Main {

    public MainConsoleLayout() {
        add(
                new H1("Pi Agent Console"),
                new Paragraph("User Console surface for chat-first sessions and run cockpit views. Data access must use ConsoleHttpClient and EventStreamClient."));
    }
}
