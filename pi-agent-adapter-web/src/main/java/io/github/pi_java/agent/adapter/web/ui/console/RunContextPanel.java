package io.github.pi_java.agent.adapter.web.ui.console;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

/** Right workbench column for active run context, status, and cancellation affordance. */
public class RunContextPanel extends Div {

    private final Span status = new Span();
    private final Button cancel = new Button();
    private Runnable cancelHandler;

    public RunContextPanel() {
        addClassName("pi-console-run-context");
        getElement().setAttribute("data-column", "run-context");
        status.setText(getTranslation("runContext.noActiveRun"));
        status.getElement().setAttribute("data-role", "run-status");
        cancel.setText(getTranslation("runContext.cancelRun"));
        cancel.addClassName("pi-console-cancel-backup");
        cancel.getElement().setAttribute("data-action", "cancel-run");
        cancel.addClickListener(event -> {
            if (cancelHandler != null) {
                cancelHandler.run();
            }
        });
        cancel.setVisible(false);
        add(new H2(getTranslation("runContext.title")), status, cancel);
    }

    public void showRunning(String sessionId, String runId) {
        status.setText(getTranslation("console.run.running", runId, sessionId));
        cancel.setVisible(true);
        cancel.getElement().setAttribute("data-prominent", "true");
    }

    public void showCancelling() {
        status.setText(getTranslation("console.run.cancelling"));
        cancel.setVisible(true);
        cancel.getElement().setAttribute("data-prominent", "true");
    }

    public void showStatus(String runStatus, boolean terminal) {
        status.setText(getTranslation("console.run.statusPrefix") + " " + runStatus);
        cancel.setVisible(!terminal && isCancellable(runStatus));
        cancel.getElement().setAttribute("data-prominent", Boolean.toString(cancel.isVisible()));
    }

    public String statusText() {
        return status.getText();
    }

    public boolean cancelProminent() {
        return cancel.isVisible() && Boolean.parseBoolean(cancel.getElement().getAttribute("data-prominent"));
    }

    public void setCancelHandler(Runnable cancelHandler) {
        this.cancelHandler = cancelHandler;
    }

    private static boolean isCancellable(String runStatus) {
        return runStatus != null
                && (runStatus.equalsIgnoreCase("running")
                || runStatus.equalsIgnoreCase("queued")
                || runStatus.equalsIgnoreCase("cancelling"));
    }
}
