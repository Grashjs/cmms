package com.grash.configuration;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.service.LicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdapSecurityConfigTest {

    @Mock
    private LicenseService licenseService;

    private LdapSecurityConfig ldapSecurityConfig;

    @BeforeEach
    void setUp() {
        ldapSecurityConfig = new LdapSecurityConfig();
        ReflectionTestUtils.setField(ldapSecurityConfig, "ldapUrl", "ldap://localhost:389");
        ReflectionTestUtils.setField(ldapSecurityConfig, "ldapBaseDn", "dc=example,dc=com");
        ReflectionTestUtils.setField(ldapSecurityConfig, "ldapUserSearchBases", "ou=users,dc=example,dc=com");
        ReflectionTestUtils.setField(ldapSecurityConfig, "ldapUserSearchFilter", "(uid={0})");
        ReflectionTestUtils.setField(ldapSecurityConfig, "searchSubtree", true);
        ReflectionTestUtils.setField(ldapSecurityConfig, "ldapManagerDn", "");
        ReflectionTestUtils.setField(ldapSecurityConfig, "ldapManagerPassword", "");
        ReflectionTestUtils.setField(ldapSecurityConfig, "emailAttr", "mail");
        ReflectionTestUtils.setField(ldapSecurityConfig, "firstNameAttr", "givenName");
        ReflectionTestUtils.setField(ldapSecurityConfig, "lastNameAttr", "sn");
    }

    @Test
    void contextSource_withoutSsoEntitlement_throws() {
        when(licenseService.hasEntitlement(LicenseEntitlement.SSO)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> ldapSecurityConfig.contextSource(licenseService));
    }

    @Test
    void contextSource_withSsoEntitlement_returnsConfiguredSource() {
        when(licenseService.hasEntitlement(LicenseEntitlement.SSO)).thenReturn(true);

        LdapContextSource contextSource = ldapSecurityConfig.contextSource(licenseService);

        assertNotNull(contextSource);
        assertEquals("ldap://localhost:389", contextSource.getUrls()[0]);
    }

    @Test
    void ldapTemplate_returnsTemplateForContextSource() {
        LdapTemplate template = ldapSecurityConfig.ldapTemplate(mock(LdapContextSource.class));

        assertNotNull(template);
    }

    @Test
    void ldapAuthenticator_withoutSearchConfig_throws() {
        ReflectionTestUtils.setField(ldapSecurityConfig, "ldapUserSearchBases", "");
        ReflectionTestUtils.setField(ldapSecurityConfig, "ldapUserSearchFilter", "");

        assertThrows(IllegalArgumentException.class,
                () -> ldapSecurityConfig.ldapAuthenticator(mock(LdapContextSource.class)));
    }

    @Test
    void ldapAuthenticator_withSearchConfig_returnsAuthenticator() {
        LdapAuthenticator authenticator =
                ldapSecurityConfig.ldapAuthenticator(mock(LdapContextSource.class));

        assertNotNull(authenticator);
    }

    @Test
    void authoritiesPopulator_returnsPopulator() {
        DefaultLdapAuthoritiesPopulator populator =
                ldapSecurityConfig.authoritiesPopulator(mock(LdapContextSource.class));

        assertNotNull(populator);
    }

    @Test
    void ldapUserDetailsMapper_mapsAttributes() {
        DirContextOperations ctx = mock(DirContextOperations.class);
        when(ctx.getStringAttribute("mail")).thenReturn("john@test.com");
        when(ctx.getStringAttribute("givenName")).thenReturn("John");
        when(ctx.getStringAttribute("sn")).thenReturn("Doe");
        LdapUserDetailsMapper mapper = ldapSecurityConfig.ldapUserDetailsMapper();

        UserDetails details = mapper.mapUserFromContext(ctx, "jdoe", Collections.emptyList());

        LdapSecurityConfig.CustomLdapUserDetails custom = assertInstanceOf(
                LdapSecurityConfig.CustomLdapUserDetails.class, details);
        assertEquals("jdoe", custom.getUsername());
        assertEquals("John", custom.getFirstName());
        assertEquals("Doe", custom.getLastName());
        assertEquals("john@test.com", custom.getEmail());
    }

    @Test
    void ldapUserDetailsMapper_missingAttributes_returnsNulls() {
        DirContextOperations ctx = mock(DirContextOperations.class);
        when(ctx.getStringAttribute(any())).thenThrow(new RuntimeException("attribute missing"));
        LdapUserDetailsMapper mapper = ldapSecurityConfig.ldapUserDetailsMapper();

        LdapSecurityConfig.CustomLdapUserDetails custom = (LdapSecurityConfig.CustomLdapUserDetails)
                mapper.mapUserFromContext(ctx, "jdoe", Collections.emptyList());

        assertNull(custom.getFirstName());
        assertNull(custom.getLastName());
        assertNull(custom.getEmail());
        assertEquals("jdoe", custom.getUsername());
    }

    @Test
    void ldapAuthenticationProvider_returnsProvider() {
        LdapAuthenticationProvider provider = ldapSecurityConfig.ldapAuthenticationProvider(
                mock(LdapAuthenticator.class),
                mock(DefaultLdapAuthoritiesPopulator.class),
                mock(LdapUserDetailsMapper.class));

        assertNotNull(provider);
    }
}
