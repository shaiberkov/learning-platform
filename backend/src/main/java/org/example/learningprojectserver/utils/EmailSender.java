package org.example.learningprojectserver.utils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Injectable wrapper around {@link ApiEmailProcessor}; sending is asynchronous. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final Environment environment;

    @PostConstruct
    void configure() {
        String password = environment.getProperty("EMAIL_APP_PASSWORD");
        if (password == null || password.isBlank()) {
            log.warn("EMAIL_APP_PASSWORD is not set: e-mails (including login codes) will fail to send");
        }
        ApiEmailProcessor.setSenderPassword(password);
    }

    public void send(String recipient, String subject, String content) {
        ApiEmailProcessor.sendEmail(recipient, subject, content);
    }
}
