package com.inventory.msp.security;

import com.inventory.msp.exception.DirectoryProvisioningException;
import com.inventory.msp.exception.ErrorResponse;
import com.inventory.msp.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.NameAlreadyBoundException;
import org.springframework.ldap.NoPermissionException;
import org.springframework.ldap.UncategorizedLdapException;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.Name;
import javax.naming.directory.ModificationItem;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LdapUserProvisioningServiceTest {

    private LdapTemplate ldapTemplate;
    private LdapUserProvisioningService service;

    @BeforeEach
    void setUp() {
        ldapTemplate = mock(LdapTemplate.class);
        service = new LdapUserProvisioningService(ldapTemplate);
        ReflectionTestUtils.setField(service, "userCreateBase", "OU=ICCC Users,DC=ad,DC=iccc,DC=local");
        ReflectionTestUtils.setField(service, "groupSearchBase", "OU=ICCC Groups,DC=ad,DC=iccc,DC=local");
        ReflectionTestUtils.setField(service, "domainSuffix", "ad.iccc.local");
    }

    @Test
    void removesNewDirectoryUserWhenAddingToGroupFails() {
        doReturn(false).when(ldapTemplate).lookup(
                any(Name.class), any(String[].class), any(AttributesMapper.class));
        doAnswer(invocation -> {
            Name dn = invocation.getArgument(0);
            if (dn.toString().startsWith("CN=SG_APP_INVENTORY_ADMIN,")) {
                throw new NoPermissionException(new javax.naming.NoPermissionException("insufficient access"));
            }
            return null;
        }).when(ldapTemplate).modifyAttributes(any(Name.class), any(ModificationItem[].class));

        DirectoryProvisioningException error = assertThrows(DirectoryProvisioningException.class,
                () -> service.createAdUser("admin123", "NeverLogThisPassword", "Admin User",
                        "admin@example.com", "123", "ADMIN"));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
        assertEquals("The directory service account is not allowed to add users to group " +
                "SG_APP_INVENTORY_ADMIN. Contact the AD administrator.", error.getMessage());
        verify(ldapTemplate).unbind(any(Name.class));
    }

    @Test
    void reusesExistingDirectoryUserWithoutChangingPasswordAndAddsMissingGroupMembership() {
        doThrow(new NameAlreadyBoundException(
                new javax.naming.NameAlreadyBoundException("already bound")))
                .when(ldapTemplate).bind(any(DirContextOperations.class));
        doReturn(false).when(ldapTemplate).lookup(
                any(Name.class), any(String[].class), any(AttributesMapper.class));

        boolean created = service.createAdUser("admin123", "DoNotOverwriteExistingPassword",
                "Admin User", null, null, "ADMIN");

        assertFalse(created);
        verify(ldapTemplate).modifyAttributes(any(Name.class), any(ModificationItem[].class));
        verify(ldapTemplate, never()).unbind(any(Name.class));
    }

    @Test
    void doesNotDeleteExistingDirectoryUserWhenGroupUpdateFails() {
        doThrow(new NameAlreadyBoundException(
                new javax.naming.NameAlreadyBoundException("already bound")))
                .when(ldapTemplate).bind(any(DirContextOperations.class));
        doReturn(false).when(ldapTemplate).lookup(
                any(Name.class), any(String[].class), any(AttributesMapper.class));
        doThrow(new NoPermissionException(new javax.naming.NoPermissionException("insufficient access")))
                .when(ldapTemplate).modifyAttributes(any(Name.class), any(ModificationItem[].class));

        DirectoryProvisioningException error = assertThrows(DirectoryProvisioningException.class,
                () -> service.createAdUser("admin123", "DoNotOverwriteExistingPassword",
                        "Admin User", null, null, "ADMIN"));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
        verify(ldapTemplate, never()).unbind(any(Name.class));
    }

    @Test
    void passwordPolicyFailureMapsToBadRequestWithDirectoryReason() {
        doReturn(false).when(ldapTemplate).lookup(
                any(Name.class), any(String[].class), any(AttributesMapper.class));
        AtomicBoolean passwordWrite = new AtomicBoolean();
        doAnswer(invocation -> {
            ModificationItem[] changes = invocation.getArgument(1);
            if ("unicodePwd".equals(changes[0].getAttribute().getID())) {
                passwordWrite.set(true);
                throw new UncategorizedLdapException(
                        "LDAP error code 19: Password must contain a special character");
            }
            return null;
        }).when(ldapTemplate).modifyAttributes(any(Name.class), any(ModificationItem[].class));

        DirectoryProvisioningException error = assertThrows(DirectoryProvisioningException.class,
                () -> service.createAdUser("admin123", "NotLogged", "Admin User",
                        null, null, "ADMIN"));

        assertTrue(passwordWrite.get());
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertTrue(error.getMessage().contains("Password must contain a special character"));
        verify(ldapTemplate).unbind(any(Name.class));
    }

    @Test
    void apiMapsDirectoryConflictAndPermissionFailuresToSpecifiedResponses() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/users");

        ErrorResponse conflict = handler.handleDirectoryUserAlreadyExists(
                new NameAlreadyBoundException(new javax.naming.NameAlreadyBoundException()), request).getBody();
        assertEquals(409, conflict.getStatus());
        assertEquals("User already exists in the directory", conflict.getMessage());

        ErrorResponse permission = handler.handleDirectoryProvisioning(
                new DirectoryProvisioningException(HttpStatus.BAD_GATEWAY,
                        "The directory service account is not allowed to add users to group " +
                                "SG_APP_INVENTORY_ADMIN. Contact the AD administrator.", null),
                request).getBody();
        assertEquals(502, permission.getStatus());
        assertEquals("The directory service account is not allowed to add users to group " +
                "SG_APP_INVENTORY_ADMIN. Contact the AD administrator.", permission.getMessage());

        ErrorResponse password = handler.handleDirectoryProvisioning(
                new DirectoryProvisioningException(HttpStatus.BAD_REQUEST,
                        "Password must contain a special character", null), request).getBody();
        assertEquals(400, password.getStatus());
        assertEquals("Password must contain a special character", password.getMessage());
    }
}
