package com.grash.security;

import com.grash.configuration.LdapProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.LdapEntryIdentificationCallback;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.naming.directory.Attributes;
import java.util.Hashtable;

@Slf4j
@Service
@RequiredArgsConstructor
public class LdapAuthenticationService {

    private final LdapProperties ldapProperties;
    private LdapTemplate ldapTemplate;

    /**
     * Initialize LDAP template lazily when LDAP is enabled
     */
    private LdapTemplate getLdapTemplate() {
        if (ldapTemplate == null && ldapProperties.isEnabled()) {
            LdapContextSource contextSource = new LdapContextSource();
            contextSource.setUrl(ldapProperties.getUrl());
            contextSource.setBase(ldapProperties.getBase());
            
            // Use manager DN/password if provided, otherwise use username/password
            String bindDn = ldapProperties.getManagerDn() != null ? ldapProperties.getManagerDn() : ldapProperties.getUsername();
            String bindPassword = ldapProperties.getManagerPassword() != null ? ldapProperties.getManagerPassword() : ldapProperties.getPassword();
            
            if (bindDn != null && !bindDn.isEmpty()) {
                contextSource.setUserDn(bindDn);
                contextSource.setPassword(bindPassword);
            }
            
            // Set LDAP connection pool properties
            Hashtable<String, String> env = new Hashtable<>();
            env.put("com.sun.jndi.ldap.connect.timeout", "5000");
            env.put("com.sun.jndi.ldap.read.timeout", "5000");
            contextSource.setBaseEnvironmentProperties(env);
            
            contextSource.afterPropertiesSet();
            ldapTemplate = new LdapTemplate(contextSource);
            ldapTemplate.setDefaultCountLimit(0);
        }
        return ldapTemplate;
    }

    /**
     * Authenticate a user against LDAP
     * @param username the username to authenticate
     * @param password the password to verify
     * @return LdapUserDetails if authentication succeeds
     * @throws UsernameNotFoundException if user is not found
     * @throws IllegalArgumentException if LDAP is not enabled or authentication fails
     */
    public UserDetails authenticate(String username, String password) {
        if (!ldapProperties.isEnabled()) {
            throw new IllegalArgumentException("LDAP authentication is not enabled");
        }

        try {
            log.debug("Attempting LDAP authentication for user: {}", username);
            
            // Build the search filter
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectclass", "person"));
            filter.and(new EqualsFilter(
                ldapProperties.getUserFilter().replace("{0}", ""),
                username
            ));

            // Authenticate and retrieve user details
            return getLdapTemplate().authenticate(
                ldapProperties.getUserSearchBase(),
                filter.toString(),
                password,
                new AbstractContextMapper<UserDetails>() {
                    @Override
                    protected UserDetails doMapFromContext(DirContextAdapter context) {
                        log.debug("LDAP authentication successful for: {}", username);
                        return LdapUserDetails.builder()
                                .username(context.getStringAttribute("uid") != null ? 
                                        context.getStringAttribute("uid") : username)
                                .password("") // Don't store LDAP password
                                .email(context.getStringAttribute("mail") != null ? 
                                        context.getStringAttribute("mail") : username)
                                .firstName(context.getStringAttribute("givenName"))
                                .lastName(context.getStringAttribute("sn"))
                                .dn(context.getDn().toString())
                                .authorities(null) // LDAP authorities can be loaded separately if needed
                                .enabled(true)
                                .build();
                    }
                }
            );
        } catch (Exception e) {
            log.error("LDAP authentication failed for user: {}", username, e);
            throw new UsernameNotFoundException("LDAP authentication failed for user: " + username, e);
        }
    }

    /**
     * Check if LDAP authentication is enabled
     */
    public boolean isLdapEnabled() {
        return ldapProperties.isEnabled();
    }

    /**
     * Load user details from LDAP without authentication (for lookup purposes)
     */
    public UserDetails loadUserByUsername(String username) {
        if (!ldapProperties.isEnabled()) {
            throw new IllegalArgumentException("LDAP authentication is not enabled");
        }

        try {
            log.debug("Loading LDAP user details for: {}", username);
            
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectclass", "person"));
            filter.and(new EqualsFilter(
                ldapProperties.getUserFilter().replace("{0}", ""),
                username
            ));

            return getLdapTemplate().searchForObject(
                ldapProperties.getUserSearchBase(),
                filter.toString(),
                new AbstractContextMapper<UserDetails>() {
                    @Override
                    protected UserDetails doMapFromContext(DirContextAdapter context) {
                        return LdapUserDetails.builder()
                                .username(context.getStringAttribute("uid") != null ? 
                                        context.getStringAttribute("uid") : username)
                                .password("")
                                .email(context.getStringAttribute("mail") != null ? 
                                        context.getStringAttribute("mail") : username)
                                .firstName(context.getStringAttribute("givenName"))
                                .lastName(context.getStringAttribute("sn"))
                                .dn(context.getDn().toString())
                                .authorities(null)
                                .enabled(true)
                                .build();
                    }
                }
            );
        } catch (Exception e) {
            log.error("Failed to load LDAP user: {}", username, e);
            throw new UsernameNotFoundException("LDAP user not found: " + username, e);
        }
    }
}
