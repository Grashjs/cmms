package com.grash.configuration;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.service.LicenseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(name = "ldap.enabled", havingValue = "true")
public class LdapSecurityConfig {

    @Value("${ldap.url}")
    private String ldapUrl;

    @Value("${ldap.base-dn}")
    private String ldapBaseDn;

    @Value("${ldap.user-search-bases:}")
    private String ldapUserSearchBases;

    @Value("${ldap.user-search-filter:}")
    private String ldapUserSearchFilter;
    @Value("${ldap.search-subtree:true}")
    private boolean searchSubtree;

    @Value("${ldap.manager-dn:}")
    private String ldapManagerDn;

    @Value("${ldap.manager-password:}")
    private String ldapManagerPassword;

    @Value("${ldap.attributes.email:mail}")
    private String emailAttr;

    @Value("${ldap.attributes.first-name:givenName}")
    private String firstNameAttr;

    @Value("${ldap.attributes.last-name:sn}")
    private String lastNameAttr;


    @Bean
    public LdapContextSource contextSource(LicenseService licenseService) {
        LdapContextSource contextSource = new LdapContextSource();
        if (!licenseService.hasEntitlement(LicenseEntitlement.SSO))
            throw new IllegalStateException("SSO entitlement is required for LDAP authentication");
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBaseDn);

        // Optional service account (needed for search mode)
        if (ldapManagerDn != null && !ldapManagerDn.isBlank()) {
            contextSource.setUserDn(ldapManagerDn);
            contextSource.setPassword(ldapManagerPassword);
        }

        contextSource.afterPropertiesSet();
        return contextSource;
    }


    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource contextSource) {
        return new LdapTemplate(contextSource);
    }


    @Bean
    public LdapAuthenticator ldapAuthenticator(LdapContextSource contextSource) {

        if (ldapUserSearchFilter != null && !ldapUserSearchFilter.isBlank()
                && ldapUserSearchBases != null && !ldapUserSearchBases.isBlank()) {

            List<String> bases = Arrays.asList(ldapUserSearchBases.split("[|]"));

            List<LdapAuthenticator> authenticators = bases.stream()
                    .map(String::trim)
                    .filter(base -> !base.isBlank())
                    .map(base -> {
                        BindAuthenticator auth = new BindAuthenticator(contextSource);
                        FilterBasedLdapUserSearch search = new FilterBasedLdapUserSearch(
                                base, ldapUserSearchFilter, contextSource);
                        search.setSearchSubtree(searchSubtree);
                        auth.setUserSearch(search);
                        return (LdapAuthenticator) auth;
                    }).toList();

            return authentication -> {
                for (LdapAuthenticator auth : authenticators) {
                    try {
                        return auth.authenticate(authentication);
                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                    }
                }
                throw new BadCredentialsException("User not found in any allowed OU");
            };
        } else
            throw new IllegalArgumentException("LDAP user search base and filter must be provided when LDAP " +
                    "authentication is enabled");
    }

    @Bean
    public DefaultLdapAuthoritiesPopulator authoritiesPopulator(LdapContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator populator =
                new DefaultLdapAuthoritiesPopulator(contextSource, null) {
                    @Override
                    protected Set<GrantedAuthority> getAdditionalRoles(DirContextOperations user, String username) {
                        return new HashSet<>();
                    }
                };

        populator.setIgnorePartialResultException(true);

        return populator;
    }

    @Bean
    public LdapUserDetailsMapper ldapUserDetailsMapper() {
        return new LdapUserDetailsMapper() {

            @Override
            public UserDetails mapUserFromContext(
                    DirContextOperations ctx,
                    String username,
                    Collection<? extends GrantedAuthority> authorities
            ) {

                String email = getAttr(ctx, emailAttr);
                String firstName = getAttr(ctx, firstNameAttr);
                String lastName = getAttr(ctx, lastNameAttr);

                return new CustomLdapUserDetails(
                        username,
                        "",
                        authorities,
                        firstName,
                        lastName,
                        email
                );
            }

            private String getAttr(DirContextOperations ctx, String attr) {
                try {
                    return ctx.getStringAttribute(attr);
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }


    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(
            LdapAuthenticator authenticator,
            DefaultLdapAuthoritiesPopulator authoritiesPopulator,
            LdapUserDetailsMapper ldapUserDetailsMapper
    ) {

        LdapAuthenticationProvider provider =
                new LdapAuthenticationProvider(authenticator, authoritiesPopulator);

        provider.setUserDetailsContextMapper(ldapUserDetailsMapper);

        return provider;
    }

    public class CustomLdapUserDetails extends org.springframework.security.core.userdetails.User {

        private final String firstName;
        private final String lastName;
        private final String email;

        public CustomLdapUserDetails(
                String username,
                String password,
                Collection<? extends GrantedAuthority> authorities,
                String firstName,
                String lastName,
                String email
        ) {
            super(username, password, authorities);
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getEmail() {
            return email;
        }
    }
}