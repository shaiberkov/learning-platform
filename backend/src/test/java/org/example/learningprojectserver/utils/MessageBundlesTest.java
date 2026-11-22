package org.example.learningprojectserver.utils;

import org.example.learningprojectserver.config.I18nConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MessageBundlesTest {

    private static Properties load(String name) throws Exception {
        Properties p = new Properties();
        try (var in = MessageBundlesTest.class.getResourceAsStream("/i18n/" + name)) {
            assertNotNull(in, name + " is missing");
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    void arabicAndEnglishHaveEveryHebrewKey() throws Exception {
        Properties he = load("messages.properties");
        Properties ar = load("messages_ar.properties");
        Properties en = load("messages_en.properties");
        assertEquals(he.keySet(), ar.keySet(), "Arabic keys differ from Hebrew");
        assertEquals(he.keySet(), en.keySet(), "English keys differ from Hebrew");
    }

    @Test
    void resolvesByRequestLocaleAndFallsBackToHebrew() {
        MessageSource source = new I18nConfig().messageSource();
        LocalizedMessages messages = new LocalizedMessages(source);
        try {
            LocaleContextHolder.setLocale(Locale.ENGLISH);
            assertEquals("Reply sent", messages.get("message.replySent"));
            LocaleContextHolder.setLocale(Locale.forLanguageTag("ar"));
            assertEquals("تم إرسال الرد", messages.get("message.replySent"));
            LocaleContextHolder.setLocale(Locale.FRENCH);
            assertEquals("התגובה נשלחה", messages.get("message.replySent"));
            LocaleContextHolder.resetLocaleContext();
            assertEquals("התגובה נשלחה", messages.get("message.replySent"));
            assertEquals("New message from Dana", messageIn(source, Locale.ENGLISH, "notification.newMessage.title", "Dana"));
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    private static String messageIn(MessageSource source, Locale locale, String key, Object... args) {
        return source.getMessage(key, args, locale);
    }
}
