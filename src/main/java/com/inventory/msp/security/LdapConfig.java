package com.inventory.msp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

@Configuration
public class LdapConfig {

    @Value("${app.ldap.urls}")
    private String ldapUrl;

    @Value("${app.ldap.manager-dn}")
    private String managerDn;

    @Value("${app.ldap.manager-password:${AD_LDAP_PASSWORD:}}")
    private String managerPassword;

    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        if (managerDn == null || managerDn.isBlank()) {
            throw new IllegalStateException("LDAP manager DN is blank. Set app.ldap.manager-dn or the AD_LDAP_PASSWORD env.");
        }
        if (managerPassword == null || managerPassword.isBlank()) {
            throw new IllegalStateException("LDAP manager password is blank. Set AD_LDAP_PASSWORD or app.ldap.manager-password.");
        }

        DefaultSpringSecurityContextSource contextSource =
                new DefaultSpringSecurityContextSource(ldapUrl);
        contextSource.setUserDn(managerDn);
        contextSource.setPassword(managerPassword);
        contextSource.setPooled(true);
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate(DefaultSpringSecurityContextSource contextSource) {
        return new LdapTemplate(contextSource);
    }
}