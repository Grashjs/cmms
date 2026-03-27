package com.grash.controller;

import com.grash.dto.AuthResponse;
import com.grash.dto.UserLoginRequest;
import com.grash.exception.CustomException;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.RoleCode;
import com.grash.security.JwtTokenProvider;
import com.grash.security.LdapAuthenticationService;
import com.grash.security.LdapUserDetails;
import com.grash.service.RoleService;
import com.grash.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;

/**
 * Controller for LDAP authentication
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "auth")
@RequiredArgsConstructor
public class LdapAuthController {

    private final LdapAuthenticationService ldapAuthenticationService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RoleService roleService;

    /**
     * LDAP Login endpoint
     * Authenticates user against LDAP and creates a JWT token
     * If user doesn't exist in local database, creates a new user account
     */
    @PostMapping("/ldap/login")
    public ResponseEntity<AuthResponse> ldapLogin(@Valid @RequestBody UserLoginRequest loginRequest) {
        try {
            log.info("LDAP login attempt for user: {}", loginRequest.getEmail());
            
            // Authenticate against LDAP
            UserDetails ldapUserDetails = ldapAuthenticationService.authenticate(
                loginRequest.getEmail(),
                loginRequest.getPassword()
            );
            
            LdapUserDetails ldapUser = (LdapUserDetails) ldapUserDetails;
            
            // Check if user exists in local database
            OwnUser localUser = userService.findByEmail(ldapUser.getEmail())
                    .orElse(null);
            
            if (localUser == null) {
                // Create new user account based on LDAP details
                log.info("Creating new local account for LDAP user: {}", ldapUser.getEmail());
                localUser = createLocalUserFromLdap(ldapUser);
            } else {
                // Update user details from LDAP if needed
                log.info("Updating existing user details from LDAP for: {}", ldapUser.getEmail());
                updateUserFromLdap(localUser, ldapUser);
            }
            
            // Generate JWT token
            String token = jwtTokenProvider.createToken(
                localUser.getEmail(),
                Collections.singletonList(localUser.getRole().getRoleType())
            );
            
            log.info("LDAP login successful for user: {}", ldapUser.getEmail());
            return new ResponseEntity<>(new AuthResponse(token), HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("LDAP login failed for user: {}", loginRequest.getEmail(), e);
            throw new CustomException("LDAP authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }
    
    /**
     * Create a local user account from LDAP details
     */
    private OwnUser createLocalUserFromLdap(LdapUserDetails ldapUser) {
        OwnUser user = new OwnUser();
        user.setEmail(ldapUser.getEmail());
        user.setUsername(ldapUser.getUsername());
        user.setPassword(""); // No password stored locally for LDAP users
        user.setFirstName(ldapUser.getFirstName() != null ? ldapUser.getFirstName() : "");
        user.setLastName(ldapUser.getLastName() != null ? ldapUser.getLastName() : "");
        user.setEnabled(true);
        user.setAccountExpired(false);
        user.setAccountLocked(false);
        user.setCredentialsExpired(false);
        
        // Set default role (User role by default)
        List<Role> defaultRoles = roleService.findDefaultRoles();
        Role defaultRole = defaultRoles.stream()
                .filter(role -> role.getCode() == RoleCode.USER)
                .findFirst()
                .orElse(defaultRoles.get(0));
        user.setRole(defaultRole);
        
        return userService.save(user);
    }
    
    /**
     * Update local user details from LDAP
     */
    private void updateUserFromLdap(OwnUser localUser, LdapUserDetails ldapUser) {
        boolean updated = false;
        
        if (ldapUser.getFirstName() != null && !ldapUser.getFirstName().isEmpty()) {
            localUser.setFirstName(ldapUser.getFirstName());
            updated = true;
        }
        
        if (ldapUser.getLastName() != null && !ldapUser.getLastName().isEmpty()) {
            localUser.setLastName(ldapUser.getLastName());
            updated = true;
        }
        
        if (updated) {
            userService.save(localUser);
        }
    }
}
