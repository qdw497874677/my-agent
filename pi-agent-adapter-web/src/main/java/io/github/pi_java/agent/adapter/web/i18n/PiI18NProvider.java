package io.github.pi_java.agent.adapter.web.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Component
public class PiI18NProvider implements I18NProvider {

    private static final String BUNDLE_BASENAME = "messages";
    private static final List<Locale> PROVIDED_LOCALES = List.of(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE);

    @Override
    public List<Locale> getProvidedLocales() {
        return PROVIDED_LOCALES;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null || key.isBlank()) {
            return "";
        }
        Locale effective = resolveEffective(locale);
        ResourceBundle bundle = getBundle(effective);
        if (bundle == null) {
            return key;
        }
        try {
            String pattern = bundle.getString(key);
            if (params == null || params.length == 0) {
                return pattern;
            }
            return MessageFormat.format(pattern, params);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    private static Locale resolveEffective(Locale locale) {
        if (locale == null) {
            return Locale.ENGLISH;
        }
        String language = locale.getLanguage();
        if ("zh".equals(language)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        return Locale.ENGLISH;
    }

    private static ResourceBundle getBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle(BUNDLE_BASENAME, locale);
        } catch (MissingResourceException ex) {
            return null;
        }
    }

    public static boolean isChinese(Locale locale) {
        return locale != null && "zh".equals(locale.getLanguage());
    }
}
