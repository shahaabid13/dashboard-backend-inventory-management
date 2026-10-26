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
                "com.inventory.msp.vms.repository",
<<<<<<< HEAD
                "com.inventory.msp.notification.repository",
                "com.inventory.msp.comments.repository"
=======
                "com.inventory.msp.notification.repository"
>>>>>>> 9ef025098d86db3f85cd377679ee6b39756e7de6
        },
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class MainJpaRepositoryConfig {
}
