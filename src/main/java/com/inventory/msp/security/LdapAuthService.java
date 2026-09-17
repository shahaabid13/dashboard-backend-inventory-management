package com.inventory.msp.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.stereotype.Service;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LdapAuthService {

    private final BaseLdapPathContextSource contextSource;

    @Value("${app.ldap.user-search-base}")
    private String userSearchBase;

    @Value("${app.ldap.user-search-filter}")
    private String userSearchFilter;

    @Value("${app.ldap.group-search-base}")
    private String groupSearchBase;

    @Value("${app.ldap.group-search-filter}")
    private String groupSearchFilter;

    /**
     * Attempts LDAP bind authentication.
     * Returns the mapped application role if successful, empty if it fails.
     */
    public Optional<AdLoginResult> authenticate(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isBlank() || password == null || password.isBlank()) {
            log.warn("LDAP authentication rejected empty username or password");
            return Optional.empty();
        }

        try {
            FilterBasedLdapUserSearch userSearch =
                    new FilterBasedLdapUserSearch(userSearchBase, userSearchFilter, contextSource);

            BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
            bindAuthenticator.setUserSearch(userSearch);

            DirContextOperations userData =
                    bindAuthenticator.authenticate(
                            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                    normalizedUsername, password)
                    );

            DefaultLdapAuthoritiesPopulator populator =
                    new DefaultLdapAuthoritiesPopulator(contextSource, groupSearchBase);
            populator.setGroupSearchFilter(groupSearchFilter);
            populator.setSearchSubtree(true);

            Set<String> groups = new HashSet<>();
            populator.getGroupMembershipRoles(userData.getDn().toString(), normalizedUsername)
                    .forEach(a -> groups.add(a.getAuthority()));

            String role = mapGroupToRole(groups);
            if (role == null) {
                log.warn("AD user {} authenticated but has no mapped application role. Groups: {}",
                        normalizedUsername, groups);
                return Optional.empty();
            }

            Attributes attrs = userData.getAttributes();

            Attribute displayNameAttr = attrs.get("displayName");
            String displayName = displayNameAttr != null ? displayNameAttr.get().toString() : normalizedUsername;

            Attribute mailAttr = attrs.get("mail");
            String email = mailAttr != null ? mailAttr.get().toString() : null;

            return Optional.of(new AdLoginResult(normalizedUsername, displayName, email, role));

        } catch (Exception e) {
            log.warn("LDAP authentication failed for user {}: {}",
                    normalizedUsername, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private String mapGroupToRole(Set<String> groups) {
        for (String g : groups) {
            String group = g.toUpperCase();
            if (group.contains("SG_APP_INVENTORY_ADMIN")) return "ADMIN";
            if (group.contains("SG_APP_INVENTORY_AGENCY")) return "AGENCY";
            if (group.contains("SG_APP_INVENTORY_VIEWER")) return "VIEWER";
            if (group.contains("SG_APP_INVENTORY_REVIEWER")) return "REVIEWER";
            if (group.contains("SG_APP_INVENTORY_FIELD_PERSON")) return "FIELD_PERSON";
            if (group.contains("SG_APP_INVENTORY_SUPPORT_ENGINEER")) return "SUPPORT_ENGINEER";
        }
        return null;
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }

        String normalized = username.trim();
        int slashIndex = normalized.lastIndexOf('\\');
        if (slashIndex >= 0 && slashIndex < normalized.length() - 1) {
            normalized = normalized.substring(slashIndex + 1);
        }
        int atIndex = normalized.indexOf('@');
        if (atIndex > 0) {
            normalized = normalized.substring(0, atIndex);
        }
        return normalized;
    }

    public record AdLoginResult(String username, String displayName, String email, String role) {}
}