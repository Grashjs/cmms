package com.grash.service;

import com.grash.dto.LdapLoginRequest;
import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.directory.SearchControls;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LdapServiceTest {
    @InjectMocks LdapService service;
    @Mock UserRepository users;
    @Mock PasswordEncoder passwords;
    @Mock RoleService roles;
    @Mock CompanyService companies;
    @Mock CacheService cache;
    @Mock RefreshTokenService tokens;
    @Mock CompanyAuthorizationService authorization;
    @Mock LdapAuthenticationProvider provider;
    @Mock LdapTemplate ldap;
    @Mock UserService userService;

    Company company;
    User user;
    Role oldRole, newRole;

    @BeforeEach void fixture() {
        company = new Company(); company.setId(1L);
        oldRole = Role.builder().id(1L).code(RoleCode.ADMIN).roleType(RoleType.ROLE_CLIENT).build();
        newRole = Role.builder().id(2L).code(RoleCode.LIMITED_TECHNICIAN).roleType(RoleType.ROLE_CLIENT).build();
        user = new User(); user.setId(3L); user.setCompany(company); user.setRole(oldRole);
        user.setEmail("ldap@example.test"); user.setFirstName("LDAP"); user.setLastName("User");
        user.setSsoProvider("LDAP"); user.setSsoProviderId("ldap-user"); user.setEnabled(true);
        // Constructor injection does not populate Spring's optional field-injected LDAP collaborators.
        ReflectionTestUtils.setField(service, "ldapAuthenticationProvider", provider);
        ReflectionTestUtils.setField(service, "ldapTemplate", ldap);
        ReflectionTestUtils.setField(service, "ldapOrgAdmin", "owner@example.test");
        lenient().when(authorization.target(user.getId(), user)).thenReturn(user);
        lenient().when(users.save(user)).thenReturn(user);
    }

    private void signinFixture() {
        when(users.findBySsoProviderIdAndSsoProvider("ldap-user", "LDAP")).thenReturn(Optional.of(user));
        when(roles.findDefaultRoles()).thenReturn(List.of(newRole));
    }

    @Test void changedRoleRevokesBeforeIssuingNewSessionUnderCompanyLock() {
        signinFixture();
        service.signinLdap(new LdapLoginRequest("ldap-user", "test-password"));
        assertSame(newRole, user.getRole());
        var order = inOrder(authorization, tokens);
        order.verify(authorization).lockCompany(company.getId());
        order.verify(authorization).target(user.getId(), user);
        order.verify(authorization).authorizationChanged(user);
        order.verify(tokens).createTokenPair(user);
    }

    @Test void identicalRoleDoesNotRevokeExistingSessionsOrInvitations() {
        user.setRole(newRole);
        signinFixture();
        service.signinLdap(new LdapLoginRequest("ldap-user", "test-password"));
        verify(authorization, never()).authorizationChanged(any());
        verify(tokens).createTokenPair(user);
    }

    private void syncFixture(List<String> usernames) {
        ReflectionTestUtils.setField(service, "ldapSyncEnabled", true);
        ReflectionTestUtils.setField(service, "ldapSyncDisable", true);
        ReflectionTestUtils.setField(service, "ldapSyncUpdate", false);
        ReflectionTestUtils.setField(service, "objectClassAttr", "inetOrgPerson");
        when(companies.findByOwnerEmailAndOwnsCompany("owner@example.test")).thenReturn(Optional.of(company));
        when(users.findByCompany_Id(company.getId())).thenReturn(List.of(user));
        doReturn(usernames).when(ldap).search(anyString(), anyString(), any(SearchControls.class), any(AttributesMapper.class));
    }

    @Test void syncDisableAndReenableEachPermanentlyRevoke() {
        syncFixture(List.of());
        service.syncLdapUsers();
        assertFalse(user.isEnabled());
        doReturn(List.of("ldap-user")).when(ldap).search(anyString(), anyString(), any(SearchControls.class), any(AttributesMapper.class));
        service.syncLdapUsers();
        assertTrue(user.isEnabled());
        verify(authorization, times(2)).authorizationChanged(user);
        var order = inOrder(authorization, users);
        order.verify(authorization).lockCompany(company.getId());
        order.verify(users).findByCompany_Id(company.getId());
        order.verify(authorization).authorizationChanged(user);
    }

    @Test void unchangedSyncStateDoesNotRevoke() {
        syncFixture(List.of("ldap-user"));
        service.syncLdapUsers();
        user.setEnabled(false);
        doReturn(List.of()).when(ldap).search(anyString(), anyString(), any(SearchControls.class), any(AttributesMapper.class));
        service.syncLdapUsers();
        verify(authorization, never()).authorizationChanged(any());
    }
}
