package com.grash.configuration;

import com.grash.model.Company;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.repository.CompanyRepository;
import com.grash.repository.UserRepository;
import com.grash.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class LdapUserDetailsMapperImpl extends LdapUserDetailsMapper {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleService roleService;

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
        // First, try to find user by ldapId
        User user = userRepository.findByLdapId(username).orElse(null);
        if (user == null) {
            // Create new user
            String email = ctx.getStringAttribute("mail");
            if (email == null) {
                email = username + "@ldap.local"; // fallback
            }
            String firstName = ctx.getStringAttribute("givenName");
            if (firstName == null) {
                firstName = username;
            }
            String lastName = ctx.getStringAttribute("sn");
            if (lastName == null) {
                lastName = "";
            }

            // Find LDAP company
            Company ldapCompany = companyRepository.findAll().stream()
                    .filter(c -> c.getLdapDomain() != null && !c.getLdapDomain().isEmpty())
                    .findFirst().orElse(null);
            if (ldapCompany == null) {
                throw new RuntimeException("No LDAP company found");
            }

            // Find default role
            Role defaultRole = roleService.findDefaultRoles().stream()
                    .filter(r -> r.getName().equals("User"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Default role not found"));

            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUsername(username);
            user.setLdapId(username);
            user.setCompany(ldapCompany);
            user.setRole(defaultRole);
            user.setEnabled(true);
            userRepository.save(user);
        }

        // Return the UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), "", user.isEnabled(), true, true, true, authorities);
    }
}
