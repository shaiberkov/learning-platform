package org.example.learningprojectserver.integration;

import org.example.learningprojectserver.utils.SelfPingTask;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for tests that need the whole application on a real MySQL 8 (Testcontainers).
 * Without a Docker daemon the tests are skipped, not failed, so {@code ./mvnw test} still works on a laptop
 * without Docker; CI (GitHub Actions, ubuntu-latest) has Docker and runs them.
 * <p>
 * Do not call endpoints that send SMS or e-mail from these tests (register, send-otp, forgot-password):
 * those reach real providers. Seed users through the repositories instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMySqlIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("learning")
            .withUrlParam("serverTimezone", "UTC");

    /** Keeps the test context from pinging the production server every five minutes. */
    @MockitoBean
    SelfPingTask selfPingTask;
}
