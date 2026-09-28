package com.inventory.msp.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.Name;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
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

    // Maps our application role -> exact CN of the AD security group.
    // MUST match LdapAuthService.mapGroupToRole() substrings exactly.
    // Confirm these CNs with your AD admin before relying on this in production.
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

    /**
     * Creates a new user in Active Directory: disabled -> set password -> enabled
     * -> add to the AD group matching the given application role.
     *
     * Throws on any failure. Caller is responsible for DB rollback semantics.
     */
    public void createAdUser(String username, String password, String fullName,
                             String email, String phone, String role) {

        String groupCn = ROLE_TO_GROUP_CN.get(role);
        if (groupCn == null) {
            throw new IllegalArgumentException(
                    "No AD group mapping configured for role: " + role +
                            ". User will authenticate but LdapAuthService.mapGroupToRole() will reject them.");
        }

        Name userDn = buildUserDn(username);

        try {
            // Step 1: create the account, disabled (userAccountControl=514)
            DirContextAdapter context = new DirContextAdapter(userDn);
            context.setAttributeValues("objectClass", new String[] { "top", "person", "organizationalPerson", "user" });
            context.setAttributeValue("cn", username);
            context.setAttributeValue("sAMAccountName", username);
            context.setAttributeValue("userPrincipalName", username + "@" + domainSuffix);
            context.setAttributeValue("displayName", fullName);
            context.setAttributeValue("userAccountControl", "514"); // normal account, disabled

            if (email != null && !email.isBlank()) {
                context.setAttributeValue("mail", email);
            }
            if (phone != null && !phone.isBlank()) {
                context.setAttributeValue("telephoneNumber", phone);
            }

            ldapTemplate().bind(context);
            log.info("AD account created (disabled) for user: {}", username);

            // Step 2: set password — REQUIRES ldaps:// connection, which app.ldap.urls already uses
            setPassword(userDn, password);
            log.info("Password set for AD user: {}", username);

            // Step 3: enable the account (userAccountControl=512)
            enableAccount(userDn);
            log.info("AD account enabled for user: {}", username);

            // Step 4: add to the role's security group
            addUserToGroup(userDn, groupCn);
            log.info("AD user {} added to group {}", username, groupCn);

        } catch (Exception e) {
            log.error("Failed to fully provision AD user '{}', attempting cleanup of partial state", username, e);
            // Best-effort cleanup: if the account object was created but a later
            // step failed, remove it rather than leaving a broken/disabled AD account behind.
            try {
                deleteAdUser(username);
            } catch (Exception cleanupEx) {
                log.error("Cleanup of partially-created AD user '{}' also failed. Manual AD cleanup required.",
                        username, cleanupEx);
            }
            throw new RuntimeException("Failed to provision AD user: " + username, e);
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
                        new BasicAttribute("userAccountControl", "512"))
        };
        ldapTemplate().modifyAttributes(userDn, mods);
    }

    private void addUserToGroup(Name userDn, String groupCn) {
        Name groupDn = LdapNameBuilder.newInstance(groupSearchBase)
                .add("CN", groupCn)
                .build();

        ModificationItem[] mods = new ModificationItem[] {
                new ModificationItem(DirContext.ADD_ATTRIBUTE,
                        new BasicAttribute("member", userDn.toString() + "," + contextSource.getBaseLdapPathAsString()))
        };
        // Note: 'member' attribute value must be the FULL absolute DN of the user.
        // Since userDn here is built relative to contextSource's base, LdapTemplate
        // resolves it correctly when passed directly — see caution below.
        ldapTemplate().modifyAttributes(groupDn, new ModificationItem[] {
                new ModificationItem(DirContext.ADD_ATTRIBUTE,
                        new BasicAttribute("member", buildFullUserDn(userDn.toString())))
        });
    }

    /**
     * Deletes an AD user. Used both for explicit admin-triggered deletion and
     * as a rollback compensating action if DB write fails after AD creation succeeds.
     */
    public void deleteAdUser(String username) {
        Name userDn = buildUserDn(username);
        ldapTemplate().unbind(userDn);
    }

    private Name buildUserDn(String username) {
        return LdapNameBuilder.newInstance(userCreateBase)
                .add("CN", username)
                .build();
    }

    private String buildFullUserDn(String relativeDn) {
        // contextSource has no base path configured (per LdapConfig comment: "No base DN
        // appended here"), so DNs used with LdapTemplate are already absolute. This helper
        // exists in case that assumption changes — verify against your actual LdapConfig.
        return relativeDn;
    }
}