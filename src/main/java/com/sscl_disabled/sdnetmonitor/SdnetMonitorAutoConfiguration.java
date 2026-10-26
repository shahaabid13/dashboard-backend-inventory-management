package com.sscl.sdnetmonitor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for embedding this module into a host Spring Boot
 * application: add sdnet-monitor as a plain Maven dependency (see
 * HOST_INTEGRATION.md), and this class is picked up automatically via
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * -- no manual @Import or component-scan changes needed on the host side.
 *
 * Excludes anything annotated @SpringBootApplication from the component
 * scan on purpose: this module's own SdnetMonitorApplication (used only
 * for standalone/dev-mode runs) lives in this same package tree, and
 * scanning it in here too would re-trigger a second, nested
 * @EnableAutoConfiguration pass inside the host's already-initialized
 * context -- a well-known source of duplicate-bean and DataSource
 * conflicts when a library accidentally pulls in someone else's
 * application-bootstrap class.
 *
 * entityManagerFactoryRef/transactionManagerRef point at this module's own
 * isolated beans (see SdnetMonitorDataSourceConfig) rather than whatever
 * the host app's primary JPA setup is -- the two coexist in the same
 * Spring context without conflict as long as neither is left ambiguous,
 * which explicit bean names/refs guarantee.
 */
@AutoConfiguration
@Import(SdnetMonitorDataSourceConfig.class)
@ComponentScan(
        basePackages = "com.sscl.sdnetmonitor",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootApplication.class)
)
@EnableJpaRepositories(
        basePackages = "com.sscl.sdnetmonitor.repository",
        entityManagerFactoryRef = "sdnetMonitorEntityManagerFactory",
        transactionManagerRef = "sdnetMonitorTransactionManager",
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
@EnableScheduling
public class SdnetMonitorAutoConfiguration {
}
