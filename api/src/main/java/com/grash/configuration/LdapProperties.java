package com.grash.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ldap")
public class LdapProperties {

    /**
     * Enable or disable LDAP authentication
     */
    private boolean enabled = false;

    /**
     * LDAP server URL (e.g., ldap://localhost:389 or ldaps://localhost:636)
     */
    private String url = "ldap://localhost:389";

    /**
     * LDAP base DN (e.g., dc=example,dc=com)
     */
    private String base = "dc=example,dc=com";

    /**
     * LDAP bind username (optional, for anonymous bind leave empty)
     */
    private String username;

    /**
     * LDAP bind password
     */
    private String password;

    /**
     * LDAP user filter pattern (e.g., (uid={0}) or (sAMAccountName={0}))
     */
    private String userFilter = "(uid={0})";

    /**
     * LDAP user search base (e.g., ou=people)
     */
    private String userSearchBase = "ou=people";

    /**
     * LDAP group search base (e.g., ou=groups)
     */
    private String groupSearchBase = "ou=groups";

    /**
     * LDAP manager DN (alternative to username)
     */
    private String managerDn;

    /**
     * LDAP manager password (alternative to password)
     */
    private String managerPassword;
}
