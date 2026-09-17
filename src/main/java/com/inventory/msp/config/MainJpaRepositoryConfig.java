package com.inventory.msp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Keeps the host repositories attached to the host persistence unit after
 * SDNET registers its own repository set and transaction manager.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.inventory.msp.repository",
                "com.inventory.msp.incident.repository",
                "com.inventory.msp.task.repository",
                "com.inventory.msp.vms.repository"
        },
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class MainJpaRepositoryConfig {
}
