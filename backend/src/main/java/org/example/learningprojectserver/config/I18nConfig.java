package org.example.learningprojectserver.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Server-side translations: Hebrew is the default, Arabic and English are picked from
 * the request's Accept-Language header (the frontend sends the language the user chose).
 * Texts live in resources/i18n/messages*.properties and are read through LocalizedMessages.
 */
@Configuration
public class I18nConfig {

    public static final Locale HEBREW = Locale.forLanguageTag("he");
    public static final List<Locale> SUPPORTED_LOCALES =
            List.of(HEBREW, Locale.forLanguageTag("ar"), Locale.ENGLISH);

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SUPPORTED_LOCALES);
        resolver.setDefaultLocale(HEBREW);
        return resolver;
    }
}
