package com.inventory.msp.security;

import com.inventory.msp.exception.DirectoryProvisioningException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.NameAlreadyBoundException;
import org.springframework.ldap.NoPermissionException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapName;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LdapUserProvisioningService {

    private final LdapTemplate ldapTemplate;

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
            "COORDINATOR", "SG_APP_INVENTORY_COORDINATOR",
            "REVIEWER", "SG_APP_INVENTORY_REVIEWER",
            "FIELD_PERSON", "SG_APP_INVENTORY_FIELD_PERSON",
            "SUPPORT_ENGINEER", "SG_APP_INVENTORY_SUPPORT_ENGINEER"
    );

    public boolean createAdUser(String username, String password, String fullName,
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
        String groupDn = buildGroupDn(groupCn).toString();
        boolean createdByThisCall = false;

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

            try {
                ldapTemplate.bind(context);
                createdByThisCall = true;
                log.info("AD account created (disabled) for user: {}", normalizedUsername);
            } catch (Exception bindFailure) {
                if (!isAlreadyBoundFailure(bindFailure)) {
                    throw bindFailure;
                }
                log.info("Reusing existing AD account for user: {}", normalizedUsername);
                ensureGroupMembership(userDn, groupCn);
                return false;
            }

            setPassword(userDn, password);
            enableAccount(userDn);
            ensureGroupMembership(userDn, groupCn);
            return true;
        } catch (Exception e) {
            log.error("Failed provisioning AD user '{}' for group DN {}", normalizedUsername, groupDn, e);
            if (createdByThisCall) {
                try {
                    deleteAdUser(normalizedUsername);
                } catch (Exception cleanupEx) {
                    log.error("Failed to remove newly-created AD user '{}' after provisioning failure",
                            normalizedUsername, cleanupEx);
                }
            }
            throw translateLdapFailure(e, groupCn);
        }
    }

    private void setPassword(Name userDn, String password) {
        String quotedPassword = "\"" + password + "\"";
        byte[] encodedPassword = quotedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);

        ModificationItem[] mods = new ModificationItem[] {
                new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                        new BasicAttribute("unicodePwd", encodedPassword))
        };
        ldapTemplate.modifyAttributes(userDn, mods);
    }

    private void enableAccount(Name userDn) {
        ModificationItem[] mods = new ModificationItem[] {
                new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                        new BasicAttribute("userAccountControl", String.valueOf(512)))
        };
        ldapTemplate.modifyAttributes(userDn, mods);
    }

    private void ensureGroupMembership(Name userDn, String groupCn) {
        Name groupDn = buildGroupDn(groupCn);
        if (isGroupMember(groupDn, userDn)) {
            return;
        }

        try {
            ldapTemplate.modifyAttributes(groupDn, new ModificationItem[] {
                    new ModificationItem(DirContext.ADD_ATTRIBUTE,
                            new BasicAttribute("member", userDn.toString()))
            });
        } catch (NoPermissionException e) {
            log.error("Insufficient rights adding user to AD group DN {}", groupDn, e);
            throw new DirectoryProvisioningException(HttpStatus.BAD_GATEWAY,
                    "The directory service account is not allowed to add users to group " +
                            groupCn + ". Contact the AD administrator.", e);
        } catch (Exception e) {
            log.error("Failed adding user to AD group DN {}", groupDn, e);
            throw e;
        }
    }

    private boolean isGroupMember(Name groupDn, Name userDn) {
        return Boolean.TRUE.equals(ldapTemplate.lookup(groupDn, new String[] { "member" },
                (AttributesMapper<Boolean>) attrs -> {
                    Attribute members = attrs.get("member");
                    if (members == null) {
                        return false;
                    }
                    for (int i = 0; i < members.size(); i++) {
                        Object member = members.get(i);
                        if (member != null && sameDn(member.toString(), userDn.toString())) {
                            return true;
                        }
                    }
                    return false;
                }));
    }

    private boolean sameDn(String first, String second) {
        try {
            return new LdapName(first).equals(new LdapName(second));
        } catch (javax.naming.InvalidNameException e) {
            return first.equalsIgnoreCase(second);
        }
    }

    public void deleteAdUser(String username) {
        Name userDn = buildUserDn(normalizeUsername(username));
        ldapTemplate.unbind(userDn);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warnOnMissingGroupWritePermissions() {
        List<String> groupsWithoutMemberWrite = new ArrayList<>();
        for (String groupCn : ROLE_TO_GROUP_CN.values()) {
            Name groupDn = buildGroupDn(groupCn);
            try {
                boolean canWriteMember = Boolean.TRUE.equals(
                        ldapTemplate.lookup(groupDn, new String[] { "allowedAttributesEffective" },
                                (AttributesMapper<Boolean>) attrs -> {
                                    Attribute allowed = attrs.get("allowedAttributesEffective");
                                    if (allowed == null) {
                                        return false;
                                    }
                                    for (int i = 0; i < allowed.size(); i++) {
                                        if ("member".equalsIgnoreCase(String.valueOf(allowed.get(i)))) {
                                            return true;
                                        }
                                    }
                                    return false;
                                }));
                if (!canWriteMember) {
                    groupsWithoutMemberWrite.add(groupDn.toString());
                }
            } catch (Exception e) {
                groupsWithoutMemberWrite.add(groupDn + " (permission check failed)");
                log.warn("Unable to verify member write permission for AD group DN {}", groupDn, e);
            }
        }
        if (!groupsWithoutMemberWrite.isEmpty()) {
            log.warn("LDAP bind account may not write the member attribute on AD groups: {}",
                    groupsWithoutMemberWrite);
        }
    }

    private DirectoryProvisioningException translateLdapFailure(Exception failure, String groupCn) {
        if (failure instanceof DirectoryProvisioningException directoryFailure) {
            return directoryFailure;
        }
        if (isAlreadyBoundFailure(failure)) {
            return new DirectoryProvisioningException(HttpStatus.CONFLICT,
                    "User already exists in the directory", failure);
        }
        if (isInsufficientAccessFailure(failure)) {
            return new DirectoryProvisioningException(HttpStatus.BAD_GATEWAY,
                    "The directory service account is not allowed to add users to group " +
                            groupCn + ". Contact the AD administrator.", failure);
        }

        NamingException namingFailure = findCause(failure, NamingException.class);
        if (isPasswordPolicyFailure(failure, namingFailure)) {
            String reason = namingFailure == null ? failure.getMessage() : namingFailure.getExplanation();
            if (reason == null || reason.isBlank()) {
                reason = "The password does not meet the directory password policy.";
            }
            return new DirectoryProvisioningException(HttpStatus.BAD_REQUEST, reason, failure);
        }
        return new DirectoryProvisioningException(HttpStatus.BAD_GATEWAY,
                "Failed to provision user in the directory service.", failure);
    }

    private boolean isPasswordPolicyFailure(Exception failure, NamingException namingFailure) {
        String detail = (failure.getMessage() + " " +
                (namingFailure == null ? "" : namingFailure.getExplanation() + " " + namingFailure.getMessage()))
                .toLowerCase(Locale.ROOT);
        return detail.matches("(?s).*\\b(error code|result code)\\s*[:=]?\\s*19\\b.*")
                || detail.contains("password")
                || detail.contains("0000052d")
                || (namingFailure != null && detail.contains("constraint violation"));
    }

    private boolean hasCause(Throwable failure, Class<? extends Throwable> expected) {
        return findCause(failure, expected) != null;
    }

    private boolean isAlreadyBoundFailure(Throwable failure) {
        if (hasCause(failure, NameAlreadyBoundException.class)
                || hasCause(failure, javax.naming.NameAlreadyBoundException.class)) {
            return true;
        }
        return causeMessages(failure).contains("error code 68")
                || causeMessages(failure).contains("result code 68");
    }

    private boolean isInsufficientAccessFailure(Throwable failure) {
        if (hasCause(failure, NoPermissionException.class)
                || hasCause(failure, javax.naming.NoPermissionException.class)) {
            return true;
        }
        return causeMessages(failure).contains("error code 50")
                || causeMessages(failure).contains("result code 50");
    }

    private String causeMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        Throwable current = failure;
        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return messages.toString().toLowerCase(Locale.ROOT);
    }

    private <T extends Throwable> T findCause(Throwable failure, Class<T> expected) {
        Throwable current = failure;
        while (current != null) {
            if (expected.isInstance(current)) {
                return expected.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private Name buildUserDn(String username) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        return LdapNameBuilder.newInstance(userCreateBase)
                .add("CN", normalizedUsername)
                .build();
    }

    private Name buildGroupDn(String groupCn) {
        String normalizedGroupCn = groupCn == null ? "" : groupCn.trim();
        if (normalizedGroupCn.isBlank()) {
            throw new IllegalArgumentException("Group CN cannot be blank");
        }
        try {
            return new LdapName("CN=" + normalizedGroupCn + "," + groupSearchBase);
        } catch (javax.naming.InvalidNameException e) {
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

        String invalidSamAccountNameCharacters = "/\\[]:;|=,+*?<>\"";
        if (normalized.length() > 20
                || normalized.chars().anyMatch(Character::isISOControl)
                || normalized.chars().anyMatch(c -> invalidSamAccountNameCharacters.indexOf(c) >= 0)) {
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