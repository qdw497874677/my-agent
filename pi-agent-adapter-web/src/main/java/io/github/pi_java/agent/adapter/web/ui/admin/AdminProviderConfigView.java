package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfig;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigStore;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;

@Route(value = "admin/governance/providers", layout = PiResponsiveShell.class)
public class AdminProviderConfigView extends VerticalLayout {

    private final ProviderConfigStore store;

    public AdminProviderConfigView(ProviderConfigStore store) {
        this.store = store;
        addClassName("pi-page");
        addClassName("pi-content");
        getElement().setAttribute("data-route", "admin-providers");
        getElement().setAttribute("data-mobile-critical", "true");

        H2 title = new H2(getTranslation("admin.providers.title"));
        title.addClassName("pi-page-title");
        title.getElement().setAttribute("data-page-title", "admin-providers");

        Paragraph description = new Paragraph(getTranslation("admin.providers.description"));

        Checkbox enabled = new Checkbox(getTranslation("admin.providers.enabled"));
        TextField baseUrl = new TextField(getTranslation("admin.providers.baseUrl"));
        baseUrl.setPlaceholder("https://api.openai.com/v1");
        baseUrl.setWidthFull();
        PasswordField apiKey = new PasswordField(getTranslation("admin.providers.apiKey"));
        apiKey.setPlaceholder("sk-...");
        apiKey.setWidthFull();
        TextField modelId = new TextField(getTranslation("admin.providers.modelId"));
        modelId.setPlaceholder("gpt-4.1-mini");
        modelId.setWidthFull();

        Span status = new Span();
        status.getElement().setAttribute("data-role", "provider-status");

        Button save = new Button(getTranslation("admin.providers.save"));
        save.addClassName("pi-tap-target");
        save.getElement().setAttribute("data-action", "save-provider-config");
        save.addClickListener(event -> {
            ProviderConfig config = new ProviderConfig(
                    enabled.getValue(),
                    baseUrl.getValue(),
                    apiKey.getValue(),
                    modelId.getValue(),
                    "openai-compatible",
                    "/chat/completions");
            store.update(config);
            String msg = config.isReady()
                    ? getTranslation("admin.providers.statusReady")
                    : getTranslation("admin.providers.statusSaved");
            status.setText(msg);
            Notification.show(msg, 3000, Notification.Position.TOP_CENTER);
        });

        Div form = new Div(enabled, baseUrl, apiKey, modelId, save, status);
        form.addClassName("pi-provider-form");
        form.getStyle().set("display", "flex");
        form.getStyle().set("flex-direction", "column");
        form.getStyle().set("gap", "1rem");
        form.setWidthFull();

        add(title, description, form);
        setPadding(true);
        setSpacing(true);

        ProviderConfig current = store.current();
        enabled.setValue(current.enabled());
        baseUrl.setValue(current.baseUrl());
        apiKey.setValue(current.apiKey());
        modelId.setValue(current.modelId());
        status.setText(current.isReady()
                ? getTranslation("admin.providers.statusReady")
                : getTranslation("admin.providers.statusNotReady"));
    }
}
