package org.example.learningprojectserver.utils;

import lombok.RequiredArgsConstructor;
import org.example.learningprojectserver.config.I18nConfig;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Reads a user-facing text in the current request's language (from Accept-Language).
 * Outside a request (async listeners, scheduled jobs) there is no request locale, and the
 * text is Hebrew, the platform default, rather than whatever the server JVM runs in.
 */
@Component
@RequiredArgsConstructor
public class LocalizedMessages {

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, currentLocale());
    }

    private static Locale currentLocale() {
        LocaleContext context = LocaleContextHolder.getLocaleContext();
        Locale locale = context == null ? null : context.getLocale();
        return locale == null ? I18nConfig.HEBREW : locale;
    }
}
