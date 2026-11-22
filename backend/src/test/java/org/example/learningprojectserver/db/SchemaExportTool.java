package org.example.learningprojectserver.db;

import jakarta.persistence.Entity;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Prints the MySQL DDL Hibernate would create for the current entities, without a database.
 * Used to write Flyway migrations (V1 baseline was generated with it). Skipped unless asked for:
 * <pre>./mvnw test -Dtest=SchemaExportTool -Dschema.export=target/schema.sql</pre>
 * The naming strategies are Spring Boot's defaults, so table and column names match what the app maps.
 */
class SchemaExportTool {

    @Test
    @EnabledIfSystemProperty(named = "schema.export", matches = ".+")
    void exportSchema() throws Exception {
        Path output = Path.of(System.getProperty("schema.export")).toAbsolutePath();
        Files.deleteIfExists(output);
        Files.createDirectories(output.getParent());

        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        settings.put("hibernate.boot.allow_jdbc_metadata_access", false);
        settings.put("hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy.class.getName());
        settings.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName());
        settings.put("hibernate.format_sql", true);
        settings.put("hibernate.hbm2ddl.delimiter", ";");
        settings.put("jakarta.persistence.schema-generation.database.action", "none");
        settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
        settings.put("jakarta.persistence.schema-generation.scripts.create-target", output.toString());

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            scanner.findCandidateComponents("org.example.learningprojectserver.entities").stream()
                    .map(definition -> definition.getBeanClassName())
                    .sorted()
                    .forEach(sources::addAnnotatedClassName);
            Metadata metadata = sources.buildMetadata();

            SchemaManagementToolCoordinator.process(metadata, registry, settings, null);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
        System.out.println("Schema written to " + output);
    }
}
