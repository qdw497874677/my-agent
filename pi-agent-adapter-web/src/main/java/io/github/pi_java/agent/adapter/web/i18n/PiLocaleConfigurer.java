package io.github.pi_java.agent.adapter.web.i18n;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PiLocaleConfigurer implements VaadinServiceInitListener {

    @Value("${pi.i18n.default-locale:en}")
    private String defaultLocale;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            UI ui = uiInitEvent.getUI();
            Locale locale = "zh".equalsIgnoreCase(defaultLocale) ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
            ui.setLocale(locale);
            if (ui.getSession() != null) {
                ui.getSession().setLocale(locale);
            }
        });
    }
}
