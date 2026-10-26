package com.sscl.sdnetmonitor;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Deliberately isolated persistence for this module: its own DataSource,
 * EntityManagerFactory and TransactionManager -- all explicitly named
 * "sdnetMonitor*" and configured from sdnet.datasource.* properties, never
 * spring.datasource.*, which the host application's own primary database
 * almost certainly already claims once this is embedded as a module
 * instead of run standalone.
 *
 * sdnetMonitorDataSource is built in two binding passes, not one -- this
 * matters and isn't just style:
 *   1. sdnetMonitorDataSourceProperties binds sdnet.datasource.* (url,
 *      username, password, driver-class-name) onto Spring's own
 *      DataSourceProperties class, which has real setUrl()/setUsername()/
 *      etc methods built for exactly this generic-property shape.
 *   2. .initializeDataSourceBuilder().type(HikariDataSource.class).build()
 *      then translates those generic properties into the specific
 *      HikariConfig setter calls Hikari actually needs (setJdbcUrl, not
 *      setUrl -- the mismatch that broke this the first time around: binding
 *      @ConfigurationProperties directly onto an already-built
 *      HikariDataSource silently drops "url" entirely, since HikariConfig
 *      has no setUrl() method, only setJdbcUrl(), and Spring's relaxed
 *      binder ignores properties with no matching setter rather than
 *      erroring -- which is exactly how it got this far without anyone
 *      noticing until Hikari's own validate() rejected the empty jdbcUrl
 *      at startup).
 *   3. The second @ConfigurationProperties (sdnet.datasource.hikari)
 *      applies directly to the resulting HikariDataSource bean for pool
 *      tuning (maximum-pool-size etc), where Hikari's real setter names
 *      DO match what's being bound, so this pass is fine as a direct bind.
 *
 * This is the same two-pass pattern Spring Boot's own primary-datasource
 * auto-configuration uses internally -- not a workaround, the normal way
 * to do this.
 *
 * Also deliberately does NOT rely on Spring Boot's auto-configured
 * EntityManagerFactoryBuilder (that comes from HibernateJpaAutoConfiguration,
 * which this module's own standalone bootstrap explicitly excludes -- see
 * SdnetMonitorApplication -- and which may or may not be active in a given
 * host app). Building the EntityManagerFactory by hand here means this
 * config works identically whether run standalone or embedded, regardless
 * of what the host app's own JPA auto-configuration looks like.
 *
 * Practical effect: this module keeps its own separate `sdnet_monitor`
 * database (same MySQL server as the host, or a different one entirely --
 * host's choice) rather than requiring its five tables to live inside the
 * host's own schema. See HOST_INTEGRATION.md for the properties a host app
 * needs to set for this to wire up.
 */
@Configuration
public class SdnetMonitorDataSourceConfig {

    @Bean(name = "sdnetMonitorDataSourceProperties")
    @ConfigurationProperties(prefix = "sdnet.datasource")
    public DataSourceProperties sdnetMonitorDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "sdnetMonitorDataSource")
    @ConfigurationProperties(prefix = "sdnet.datasource.hikari")
    public DataSource sdnetMonitorDataSource(
            @Qualifier("sdnetMonitorDataSourceProperties") DataSourceProperties properties,
            Environment environment) {
        if (!environment.containsProperty("SDNET_DB_PASSWORD")) {
            String primaryPassword = environment.getProperty("spring.datasource.password");
            if (primaryPassword != null) {
                properties.setPassword(primaryPassword);
            }
        }

        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "sdnetMonitorEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean sdnetMonitorEntityManagerFactory(
            @Qualifier("sdnetMonitorDataSource") DataSource dataSource) {

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("com.sscl.sdnetmonitor.entity");
        factory.setPersistenceUnitName("sdnetMonitor");

        Properties jpaProperties = new Properties();
        // schema.sql is authoritative -- validate only, same as before.
        jpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        jpaProperties.put("hibernate.jdbc.time_zone", "Asia/Kolkata");
        jpaProperties.put("hibernate.format_sql", "false");
        factory.setJpaProperties(jpaProperties);

        return factory;
    }

    @Bean(name = "sdnetMonitorTransactionManager")
    public PlatformTransactionManager sdnetMonitorTransactionManager(
            @Qualifier("sdnetMonitorEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}
