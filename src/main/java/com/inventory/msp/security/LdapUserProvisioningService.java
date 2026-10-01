package com.inventory.msp.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LdapUserProvisioningService {

    private final BaseLdapPathContextSource contextSource;

    @Value("${app.ldap.user-create-base}")
    private String userCreateBase;

    @Value("${app.ldap.group-search-base}")
    private String groupSearchBase;

    @Value("${app.ldap.domain-suffix}")
    private String domainSuffix;

    private static final Map<String, String> ROLE_TO_GROUP_CN = Map.of(
            "ADMIN", "SG_APP_INVENTORY_ADMIN",
            "AGENCY", "SG_APP_INVENTORY_AGENCY",
            "VIEWER", "SG_APP_INVENTORY_VIEWER",
            "REVIEWER", "SG_APP_INVENTORY_REVIEWER",
            "FIELD_PERSON", "SG_APP_INVENTORY_FIELD_PERSON",
            "SUPPORT_ENGINEER", "SG_APP_INVENTORY_SUPPORT_ENGINEER"
    );

    private LdapTemplate ldapTemplate() {
        return new LdapTemplate(contextSource);
    }

    public void createAdUser(String username, String password, String fullName,
                             String email, String phone, String role) {

        String normalizedUsername = normalizeUsername(username);
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        String groupCn = ROLE_TO_GROUP_CN.get(normalizedRole);

        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("Username is required for AD user creation");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required for AD user creation");
        }
        if (groupCn == null) {
            throw new IllegalArgumentException(
                    "No AD group mapping configured for role: " + normalizedRole +
                            ". User will authenticate but LdapAuthService.mapGroupToRole() will reject them.");
        }

        String displayName = fullName == null || fullName.isBlank() ? normalizedUsername : fullName.trim();
        String[] nameParts = splitDisplayName(displayName);

        Name userDn = buildUserDn(normalizedUsername);

        try {
            DirContextAdapter context = new DirContextAdapter(userDn);
            context.setAttributeValues("objectClass", new String[] { "top", "person", "organizationalPerson", "user" });
            context.setAttributeValue("cn", normalizedUsername);
            context.setAttributeValue("sAMAccountName", normalizedUsername);
            context.setAttributeValue("userPrincipalName", normalizedUsername + "@" + domainSuffix);
            context.setAttributeValue("displayName", displayName);
            context.setAttributeValue("name", displayName);
            if (nameParts[0] != null) {
                context.setAttributeValue("givenName", nameParts[0]);
            }
            if (nameParts[1] != null) {
                context.setAttributeValue("sn", nameParts[1]);
            }
            context.setAttributeValue("userAccountControl", String.valueOf(514));

            if (email != null && !email.isBlank()) {
                context.setAttributeValue("mail", email.trim());
            }
            if (phone != null && !phone.isBlank()) {
                context.setAttributeValue("telephoneNumber", phone.trim());
            }

            ldapTemplate().bind(context);
            log.info("AD account created (disabled) for user: {}", normalizedUsername);

            setPassword(userDn, password);
            log.info("Password set for AD user: {}", normalizedUsername);

            enableAccount(userDn);
            log.info("AD account enabled for user: {}", normalizedUsername);

            addUserToGroup(userDn, groupCn);
            log.info("AD user {} added to group {}", normalizedUsername, groupCn);

        } catch (Exception e) {
            log.error("Failed to fully provision AD user '{}', attempting cleanup of partial state", normalizedUsername, e);
            try {
                deleteAdUser(normalizedUsername);
            } catch (Exception cleanupEx) {
                log.error("Cleanup of partially-created AD user '{}' also failed. Manual AD cleanup required.",
                        normalizedUsername, cleanupEx);
            }
            throw new RuntimeException("Failed to provision AD user: " + normalizedUsername, e);
        }
    }

    private void setPassword(Name userDn, String password) {
        String quotedPassword = "\"" + password + "\"";
        byte[] encodedPassword;
        try {
            encodedPassword = quotedPassword.getBytes("UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-16LE encoding not supported", e);
        }

        ModificationItem[] mods = new ModificationItem[] {
                new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                        new BasicAttribute("unicodePwd", encodedPassword))
        };
        ldapTemplate().modifyAttributes(userDn, mods);
    }

    private void enableAccount(Name userDn) {
        ModificationItem[] mods = new ModificationItem[] {
                new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                        new BasicAttribute("userAccountControl", String.valueOf(512)))
        };
        ldapTemplate().modifyAttributes(userDn, mods);
    }

    private void addUserToGroup(Name userDn, String groupCn) {
        Name groupDn = buildGroupDn(groupCn);

        ldapTemplate().modifyAttributes(groupDn, new ModificationItem[] {
                new ModificationItem(DirContext.ADD_ATTRIBUTE,
                        new BasicAttribute("member", userDn.toString()))
        });
    }

    public void deleteAdUser(String username) {
        Name userDn = buildUserDn(normalizeUsername(username));
        ldapTemplate().unbind(userDn);
    }

    private Name buildUserDn(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        try {
            return new LdapName("CN=" + normalizedUsername + "," + userCreateBase);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException("Invalid AD user DN for username: " + normalizedUsername, e);
        }
    }

    private Name buildGroupDn(String groupCn) {
        String normalizedGroupCn = groupCn == null ? "" : groupCn.trim();
        if (normalizedGroupCn.isBlank()) {
            throw new IllegalArgumentException("Group CN cannot be blank");
        }
        try {
            return new LdapName("CN=" + normalizedGroupCn + "," + groupSearchBase);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException("Invalid AD group DN for group: " + normalizedGroupCn, e);
        }
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }

        String normalized = username.trim();
        if (normalized.isBlank()) {
            return "";
        }

        if (normalized.contains(",") || normalized.contains("=") || normalized.contains("\\") || normalized.contains("/")) {
            throw new IllegalArgumentException("Invalid AD username: " + username);
        }

        return normalized;
    }

    private String[] splitDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return new String[] { null, null };
        }

        String trimmed = displayName.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace <= 0) {
            return new String[] { trimmed, trimmed };
        }

        String givenName = trimmed.substring(0, firstSpace).trim();
        String surname = trimmed.substring(firstSpace + 1).trim();
        return new String[] { givenName, surname };
    }
}