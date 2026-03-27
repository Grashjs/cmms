package com.grash.security;

import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.Person;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * LDAP User Details implementation that wraps Spring Security LDAP Person object
 */
@Builder
public class LdapUserDetails implements UserDetails {
    private static final long serialVersionUID = 1L;
    
    private final String username;
    private final String password;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String dn;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public static LdapUserDetails fromPerson(Person person) {
        return LdapUserDetails.builder()
                .username(person.getUsername())
                .password(person.getPassword())
                .email(person.getMail() != null ? person.getMail() : person.getUsername())
                .firstName(person.getGivenName())
                .lastName(person.getSurname())
                .dn(person.getDn().toString())
                .authorities(person.getAuthorities())
                .enabled(person.isEnabled())
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities != null ? authorities : Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDn() {
        return dn;
    }
}
