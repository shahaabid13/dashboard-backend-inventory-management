package com.inventory.msp.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:*,http://127.0.0.1:3000,http://localhost:3000,http://localhost:3000,http://localhost,http://127.0.0.1,http://172.30.0.37,http://172.30.0.37:3000,http://192.168.1.100,http://192.168.0.*:3000,capacitor://localhost}")
    private String allowedOrigins;

    @Value("${app.ldap.user-search-base}")
    private String ldapUserSearchBase;

    @Value("${app.ldap.user-search-filter}")
    private String ldapUserSearchFilter;

    @Value("${app.ldap.group-search-base}")
    private String ldapGroupSearchBase;

    @Value("${app.ldap.group-search-filter}")
    private String ldapGroupSearchFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public GrantedAuthoritiesMapper ldapGrantedAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new LinkedHashSet<>();
            for (GrantedAuthority authority : authorities) {
                String raw = authority.getAuthority();
                if (raw == null) {
                    mapped.add(authority);
                    continue;
                }
                String normalized = raw.toUpperCase(Locale.ROOT);
                if (normalized.contains("SG_APP_INVENTORY_ADMIN")) {
                    mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                } else if (normalized.contains("SG_APP_INVENTORY_AGENCY")) {
                    mapped.add(new SimpleGrantedAuthority("ROLE_AGENCY"));
                } else if (normalized.contains("SG_APP_INVENTORY_VIEWER")) {
                    mapped.add(new SimpleGrantedAuthority("ROLE_VIEWER"));
                } else if (normalized.contains("SG_APP_INVENTORY_REVIEWER")) {
                    mapped.add(new SimpleGrantedAuthority("ROLE_REVIEWER"));
                } else if (normalized.contains("SG_APP_INVENTORY_FIELD_PERSON")) {
                    mapped.add(new SimpleGrantedAuthority("ROLE_FIELD_PERSON"));
                } else if (normalized.contains("SG_APP_INVENTORY_SUPPORT_ENGINEER")) {
                    mapped.add(new SimpleGrantedAuthority("ROLE_SUPPORT_ENGINEER"));
                } else {
                    mapped.add(authority);
                }
            }
            return mapped;
        };
    }

    @Bean
    public AuthenticationProvider ldapAuthenticationProvider(BaseLdapPathContextSource contextSource,
                                                             GrantedAuthoritiesMapper ldapGrantedAuthoritiesMapper) {
        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
        bindAuthenticator.setUserSearch(new FilterBasedLdapUserSearch(
                ldapUserSearchBase,
                ldapUserSearchFilter,
                contextSource
        ));

        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, ldapGroupSearchBase);
        authoritiesPopulator.setGroupSearchFilter(ldapGroupSearchFilter);
        authoritiesPopulator.setSearchSubtree(true);

        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(bindAuthenticator, authoritiesPopulator);
        provider.setAuthoritiesMapper(ldapGrantedAuthoritiesMapper);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/excel/upload").permitAll()
                        .requestMatchers("/api/devices/create").permitAll()
                        .requestMatchers("/api/weighbridge/**").permitAll()
                        .requestMatchers("/api/weighbridge/report").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/api/debug/**").permitAll()
                                                .requestMatchers("/api/sdnet-monitor/**").authenticated()
                        // VMS/TMS Module endpoints
                        .requestMatchers(HttpMethod.GET, "/api/servers", "/api/servers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/channels", "/api/channels/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events", "/api/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/vms/status", "/vms/**").permitAll()
                        .requestMatchers("/api/admin/channels/**").hasRole("ADMIN")
                        // Incident module endpoints
                        .requestMatchers(HttpMethod.POST, "/api/incidents/tickets").hasRole("SUPPORT_ENGINEER")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/tickets/my").hasRole("SUPPORT_ENGINEER")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/tickets/my-queue").hasRole("FIELD_PERSON")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/tickets/my-ticket-history").hasRole("FIELD_PERSON")
                        .requestMatchers(HttpMethod.PUT, "/api/incidents/tickets/*/acknowledge").hasRole("FIELD_PERSON")
                        .requestMatchers(HttpMethod.POST, "/api/incidents/tickets/*/revalidation-action").hasRole("SUPPORT_ENGINEER")
                        .requestMatchers(HttpMethod.PUT, "/api/incidents/tickets/*/assign-reviewer").hasRole("FIELD_PERSON")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/tickets/reviewers").hasAnyRole("FIELD_PERSON", "SUPPORT_ENGINEER")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/tickets/review-queue").hasRole("REVIEWER")
                        .requestMatchers(HttpMethod.PUT, "/api/incidents/tickets/*/resolve").hasRole("REVIEWER")
                        .requestMatchers(HttpMethod.PUT, "/api/incidents/tickets/*/pending").hasRole("REVIEWER")
                        .requestMatchers(HttpMethod.PUT, "/api/incidents/tickets/*/reopen").hasRole("REVIEWER")
                        .requestMatchers(HttpMethod.PUT, "/api/incidents/tickets/*/reject").hasRole("REVIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/tickets/*").hasAnyRole("ADMIN","SUPPORT_ENGINEER","FIELD_PERSON","REVIEWER")
                        .requestMatchers("/api/incidents/tickets/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/dashboard/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/incidents/incident-types").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/incidents/incident-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/incident-types").hasAnyRole("ADMIN", "SUPPORT_ENGINEER", "FIELD_PERSON", "REVIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/incidents/field-persons").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/incidents/field-persons/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/field-persons").hasAnyRole("ADMIN", "SUPPORT_ENGINEER", "FIELD_PERSON", "REVIEWER")
                        // Task module endpoints
                        .requestMatchers(HttpMethod.POST, "/api/tasks").hasAnyRole("ADMIN", "REVIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/tasks/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tasks/my-history").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/tasks/*/action").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tasks").hasAnyRole("ADMIN", "REVIEWER", "SUPPORT_ENGINEER")
                        .requestMatchers(HttpMethod.GET, "/api/tasks/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/admin/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/users/assignable").hasAnyRole("ADMIN", "REVIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/admin/users").hasRole("ADMIN")
                        // Existing endpoints - base modules with CIMS role access
                        .requestMatchers(HttpMethod.GET, "/api/incidents/tickets/my-queue").hasRole("FIELD_PERSON")
                        .requestMatchers(HttpMethod.GET, "/api/devices/**").hasAnyRole("ADMIN", "VIEWER", "SUPPORT_ENGINEER", "FIELD_PERSON", "REVIEWER", "AGENCY")
                        .requestMatchers(HttpMethod.PATCH, "/api/devices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/devices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/devices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/devices/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/maintenance/**").hasAnyRole("ADMIN", "VIEWER", "SUPPORT_ENGINEER", "FIELD_PERSON", "REVIEWER", "AGENCY")
                        .requestMatchers(HttpMethod.POST, "/api/maintenance/**").hasAnyRole("ADMIN", "AGENCY")
                        .requestMatchers(HttpMethod.PUT, "/api/maintenance/**").hasAnyRole("ADMIN", "AGENCY")
                        .requestMatchers(HttpMethod.DELETE, "/api/maintenance/**").hasAnyRole("ADMIN", "AGENCY")
                        .requestMatchers(HttpMethod.GET, "/api/locations/**").hasAnyRole("ADMIN", "VIEWER", "SUPPORT_ENGINEER", "FIELD_PERSON", "REVIEWER", "AGENCY")
                        .requestMatchers(HttpMethod.POST, "/api/locations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/locations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/locations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/chartered-bike/**").hasAnyRole("ADMIN", "VIEWER", "SUPPORT_ENGINEER", "FIELD_PERSON", "REVIEWER", "AGENCY")
                        .requestMatchers(HttpMethod.POST, "/api/chartered-bike/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/chartered-bike/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/chartered-bike/**").hasRole("ADMIN")
                        .requestMatchers("/api/agency/**").hasRole("AGENCY")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration, driven by app.cors.allowed-origins property.
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList()
        );
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
