package io.github.pi_java.agent.adapter.web.ui.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfig;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigController;
import io.github.pi_java.agent.adapter.web.provider.ProviderConfigStore;
import io.github.pi_java.agent.adapter.web.ui.PiResponsiveShell;

import java.util.List;

@Route(value = "admin/governance/providers", layout = PiResponsiveShell.class)
public class AdminProviderConfigView extends VerticalLayout {

    private final ProviderConfigStore store;
    private final ProviderConfigController controller;

    public AdminProviderConfigView(ProviderConfigStore store, ProviderConfigController controller) {
        this.store = store;
        this.controller = controller;
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

        ComboBox<String> modelId = new ComboBox<>(getTranslation("admin.providers.modelId"));
        modelId.setAllowCustomValue(true);
        modelId.setItems(List.of());
        modelId.setWidthFull();

        Span modelStatus = new Span();
        modelStatus.getElement().setAttribute("data-role", "model-list-status");

        Button refreshModels = new Button(getTranslation("admin.providers.refreshModels"));
        refreshModels.addClassName("pi-tap-target");
        refreshModels.getElement().setAttribute("data-action", "refresh-models");
        refreshModels.addClickListener(event -> {
            modelStatus.setText(getTranslation("admin.providers.statusFetching"));
            modelId.setItems(List.of());
            try {
                var response = controller.listModels();
                if (response.error() != null && !response.error().isBlank()) {
                    modelStatus.setText(getTranslation("admin.providers.statusFetchFailed") + ": " + response.error());
                } else {
                    modelId.setItems(response.models());
                    modelStatus.setText(getTranslation("admin.providers.statusModelsLoaded", response.models().size()));
                }
            } catch (Exception ex) {
                modelStatus.setText(getTranslation("admin.providers.statusFetchFailed") + ": " + ex.getMessage());
            }
        });

        HorizontalLayout modelRow = new HorizontalLayout(modelId, refreshModels);
        modelRow.setWidthFull();
        modelRow.setAlignItems(Alignment.END);
        modelRow.setFlexGrow(1, modelId);

        Span status = new Span();
        status.getElement().setAttribute("data-role", "provider-status");

        Button save = new Button(getTranslation("admin.providers.save"));
        save.addClassName("pi-tap-target");
        save.getElement().setAttribute("data-action", "save-provider-config");
        save.addClickListener(event -> {
            String selectedModel = modelId.getValue();
            if (selectedModel == null || selectedModel.isBlank()) {
                selectedModel = "gpt-4.1-mini";
            }
            ProviderConfig config = new ProviderConfig(
                    enabled.getValue(),
                    baseUrl.getValue(),
                    apiKey.getValue(),
                    selectedModel,
                    "openai-compatible",
                    "/chat/completions");
            store.update(config);
            String msg = config.isReady()
                    ? getTranslation("admin.providers.statusReady")
                    : getTranslation("admin.providers.statusSaved");
            status.setText(msg);
            Notification.show(msg, 3000, Notification.Position.TOP_CENTER);
        });

        Div form = new Div(enabled, baseUrl, apiKey, modelRow, modelStatus, save, status);
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
