package com.grash.service;

import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.enums.RoleType;
import com.grash.repository.UserRepository;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Startup-only provisioning. No controller exposes this operation or issues a session for it. */
@Service
@RequiredArgsConstructor
public class InternalUserBootstrapService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.grash.repository.RoleRepository roleRepository;

    @Transactional
    public Role createPlatformRole(Role role) {
        if (role.getId() != null || role.getRoleType() != RoleType.ROLE_SUPER_ADMIN)
            throw new IllegalArgumentException("Internal platform role required");
        return roleRepository.save(role);
    }

    @Transactional
    public void createSuperAdministrator(Role role) {
        if (role.getRoleType() != RoleType.ROLE_SUPER_ADMIN || role.getCompanySettings() == null
                || role.getCompanySettings().getCompany() == null) {
            throw new IllegalArgumentException("Bootstrap requires the internal super administrator role");
        }
        if (!userRepository.findByCompany_Id(role.getCompanySettings().getCompany().getId()).isEmpty()) {
            return;
        }
        User user = new User();
        user.setEmail("superadmin@test.com");
        user.setPassword(passwordEncoder.encode("pls_change_me"));
        user.setFirstName("Super");
        user.setLastName("Admin");
        user.setPhone("");
        user.setUsername(Helper.generateStringId());
        user.setRole(role);
        user.setCompany(role.getCompanySettings().getCompany());
        user.setEnabled(true);
        userRepository.save(user);
    }
}
