package com.grash.service;

import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.enums.RoleType;
import com.grash.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalUserBootstrapServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private InternalUserBootstrapService service;

    private Role internalRole() {
        Company company = new Company();
        company.setId(1L);
        CompanySettings settings = new CompanySettings();
        settings.setCompany(company);
        return Role.builder().id(2L).roleType(RoleType.ROLE_SUPER_ADMIN).companySettings(settings).build();
    }

    @Test
    void bootstrapCreatesInternalAdministratorWithoutPublicSignupOrInvitation() {
        Role role = internalRole();
        when(passwordEncoder.encode("pls_change_me")).thenReturn("encoded-password");

        service.createSuperAdministrator(role);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("superadmin@test.com", saved.getValue().getEmail());
        assertEquals("encoded-password", saved.getValue().getPassword());
        assertTrue(saved.getValue().isEnabled());
        assertSame(role, saved.getValue().getRole());
        assertSame(role.getCompanySettings().getCompany(), saved.getValue().getCompany());
        assertNotNull(saved.getValue().getUsername());
    }

    @Test
    void bootstrapDoesNotCreateAnotherAccountWhenCompanyAlreadyHasUsers() {
        when(userRepository.findByCompany_Id(1L)).thenReturn(List.of(new User()));

        service.createSuperAdministrator(internalRole());

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void bootstrapRejectsPublicRole() {
        Role role = internalRole();
        role.setRoleType(RoleType.ROLE_CLIENT);

        assertThrows(IllegalArgumentException.class, () -> service.createSuperAdministrator(role));

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void bootstrapRejectsRoleWithoutCompany() {
        Role role = internalRole();
        role.setCompanySettings(null);

        assertThrows(IllegalArgumentException.class, () -> service.createSuperAdministrator(role));

        verifyNoInteractions(userRepository, passwordEncoder);
    }
}
