package com.grash.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

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

    @Value("${ldap.group-search-base:}")
    private String ldapGroupSearchBase;

    @Value("${ldap.group-search-filter:}")
    private String ldapGroupSearchFilter;

    @Value("${ldap.manager-dn:}")
    private String ldapManagerDn;

    @Value("${ldap.manager-password:}")
    private String ldapManagerPassword;

    private final LdapUserDetailsMapper ldapUserDetailsMapper;

    public LdapSecurityConfig(LdapUserDetailsMapper ldapUserDetailsMapper) {
        this.ldapUserDetailsMapper = ldapUserDetailsMapper;
    }

    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBaseDn);

        if (!ldapManagerDn.isBlank()) {
            contextSource.setUserDn(ldapManagerDn);
            contextSource.setPassword(ldapManagerPassword);
        }

        contextSource.afterPropertiesSet();
        return contextSource;
    }

    @Bean
    public LdapAuthenticator ldapAuthenticator(LdapContextSource contextSource) {

        // CASE 1: User search (recommended for AD / enterprise LDAP)
        if (!ldapUserSearchBase.isBlank() && !ldapUserSearchFilter.isBlank()) {
            return new BindAuthenticator(contextSource) {{
                setUserSearch(new FilterBasedLdapUserSearch(
                        ldapUserSearchBase,
                        ldapUserSearchFilter,
                        contextSource
                ));
            }};
        }

        // CASE 2: DN pattern (simple LDAP)
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        if (!ldapUserDnPattern.isBlank()) {
            authenticator.setUserDnPatterns(new String[]{ldapUserDnPattern});
        }

        return authenticator;
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(
            LdapAuthenticator authenticator,
            LdapContextSource contextSource
    ) {

        DefaultLdapAuthoritiesPopulator authoritiesPopulator =
                new DefaultLdapAuthoritiesPopulator(
                        contextSource,
                        ldapGroupSearchBase
                );

        if (!ldapGroupSearchFilter.isBlank()) {
            authoritiesPopulator.setGroupSearchFilter(ldapGroupSearchFilter);
        }

        LdapAuthenticationProvider provider =
                new LdapAuthenticationProvider(authenticator, authoritiesPopulator);

        provider.setUserDetailsContextMapper(ldapUserDetailsMapper);

        return provider;
    }
}