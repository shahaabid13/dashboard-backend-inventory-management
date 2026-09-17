package com.inventory.msp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

@Configuration
public class LdapConfig {

    @Value("${app.ldap.urls}")
    private String ldapUrl;

    @Value("${app.ldap.manager-dn}")
    private String managerDn;

    @Value("${AD_LDAP_PASSWORD:}")
    private String managerPassword;

    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        // No base DN appended here — all search bases below are now FULL/absolute DNs instead.
        DefaultSpringSecurityContextSource contextSource =
                new DefaultSpringSecurityContextSource(ldapUrl);
        contextSource.setUserDn(managerDn);
        contextSource.setPassword(managerPassword);
        contextSource.setPooled(true);
        contextSource.afterPropertiesSet();
        return contextSource;
    }
}