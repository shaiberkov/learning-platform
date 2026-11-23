package org.example.learningprojectserver.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dev only: ddl-auto=update adds new tables and columns but never changes an existing column's type.
 * Older dev databases hold user_entity.role as a native ENUM without PARENT, so inserting a parent
 * fails with "Data truncated for column 'role'". This widens it to VARCHAR(32) once.
 * Production (ddl-auto=none) needs the same change as a migration: see docs/MIGRATIONS.md.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevSchemaPatcher implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> types = jdbcTemplate.queryForList(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS"
                        + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_entity' AND COLUMN_NAME = 'role'",
                String.class);
        if (!types.isEmpty() && "enum".equalsIgnoreCase(types.get(0))) {
            jdbcTemplate.execute("ALTER TABLE user_entity MODIFY role VARCHAR(32) NOT NULL");
            log.info("Widened user_entity.role from ENUM to VARCHAR(32) so new roles (PARENT) can be stored");
        }
    }
}
