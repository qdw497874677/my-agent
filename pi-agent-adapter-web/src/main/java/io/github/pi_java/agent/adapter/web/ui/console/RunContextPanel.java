package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

/** Right workbench column for active run context, status, and cancellation affordance. */
public class RunContextPanel extends Div {

    private final Span status = new Span("No active run");
    private final Button cancel = new Button("Cancel run");

    public RunContextPanel() {
        addClassName("pi-console-run-context");
        getElement().setAttribute("data-column", "run-context");
        cancel.getElement().setAttribute("data-action", "cancel-run");
        cancel.setVisible(false);
        add(new H2("Run context"), status, cancel);
    }

    public void showRunning(String sessionId, String runId) {
        status.setText("Running run " + runId + " in session " + sessionId);
        cancel.setVisible(true);
        cancel.getElement().setAttribute("data-prominent", "true");
    }

    public void showCancelling() {
        status.setText("Cancelling run…");
        cancel.setVisible(true);
        cancel.getElement().setAttribute("data-prominent", "true");
    }

    public void showStatus(String runStatus, boolean terminal) {
        status.setText("Run status: " + runStatus);
        cancel.setVisible(!terminal && isCancellable(runStatus));
        cancel.getElement().setAttribute("data-prominent", Boolean.toString(cancel.isVisible()));
    }

    public String statusText() {
        return status.getText();
    }

    public boolean cancelProminent() {
        return cancel.isVisible() && Boolean.parseBoolean(cancel.getElement().getAttribute("data-prominent"));
    }

    private static boolean isCancellable(String runStatus) {
        return runStatus != null
                && (runStatus.equalsIgnoreCase("running")
                || runStatus.equalsIgnoreCase("queued")
                || runStatus.equalsIgnoreCase("cancelling"));
    }
}
