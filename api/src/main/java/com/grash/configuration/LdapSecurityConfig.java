package com.grash.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Configuration
@ConditionalOnProperty(name = "ldap.enabled", havingValue = "true")
public class LdapSecurityConfig {

    @Value("${ldap.url}")
    private String ldapUrl;

    @Value("${ldap.base-dn}")
    private String ldapBaseDn;

    @Value("${ldap.user-dn-pattern:}")
    private String ldapUserDnPattern;

    @Value("${ldap.user-search-base:}")
    private String ldapUserSearchBase;

    @Value("${ldap.user-search-filter:}")
    private String ldapUserSearchFilter;

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

    // ========================
    // Context Source
    // ========================
    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();
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

    // ========================
    // LDAP Template (optional)
    // ========================
    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource contextSource) {
        return new LdapTemplate(contextSource);
    }

    // ========================
    // Authenticator
    // ========================
    @Bean
    public LdapAuthenticator ldapAuthenticator(LdapContextSource contextSource) {

        if (ldapUserSearchBase != null && !ldapUserSearchBase.isBlank()
                && ldapUserSearchFilter != null && !ldapUserSearchFilter.isBlank()) {

            BindAuthenticator authenticator = new BindAuthenticator(contextSource);

            authenticator.setUserSearch(new FilterBasedLdapUserSearch(
                    ldapUserSearchBase,
                    ldapUserSearchFilter,
                    contextSource
            ));

            return authenticator;
        }

        BindAuthenticator authenticator = new BindAuthenticator(contextSource);

        if (ldapUserDnPattern != null && !ldapUserDnPattern.isBlank()) {
            authenticator.setUserDnPatterns(new String[]{ldapUserDnPattern});
        }

        return authenticator;
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

    // ========================
    // Attribute Mapper (ENV driven)
    // ========================
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

    // ========================
    // Authentication Provider
    // ========================
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