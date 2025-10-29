package com.grash.service;

import com.grash.model.Role;
import com.grash.model.UserInvitation;
import com.grash.repository.UserInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInvitationServiceTest {

    @Mock
    private UserInvitationRepository userInvitationRepository;

    @InjectMocks
    private UserInvitationService userInvitationService;

    private UserInvitation userInvitation;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(1L);

        userInvitation = new UserInvitation();
        userInvitation.setId(1L);
        userInvitation.setEmail("test@test.com");
        userInvitation.setRole(role);
    }

    @Test
    void create() {
        when(userInvitationRepository.save(any(UserInvitation.class))).thenReturn(userInvitation);

        UserInvitation result = userInvitationService.create(userInvitation);

        assertNotNull(result);
        assertEquals(userInvitation.getId(), result.getId());
        verify(userInvitationRepository).save(userInvitation);
    }

    @Test
    void getAll() {
        when(userInvitationRepository.findAll()).thenReturn(Collections.singletonList(userInvitation));

        assertEquals(1, userInvitationService.getAll().size());
    }

    @Test
    void delete() {
        doNothing().when(userInvitationRepository).deleteById(1L);
        userInvitationService.delete(1L);
        verify(userInvitationRepository).deleteById(1L);
    }

    @Test
    void findById() {
        when(userInvitationRepository.findById(1L)).thenReturn(Optional.of(userInvitation));

        assertTrue(userInvitationService.findById(1L).isPresent());
    }

    @Test
    void findByRoleAndEmail() {
        when(userInvitationRepository.findByRole_IdAndEmailIgnoreCase(1L, "test@test.com"))
                .thenReturn(Collections.singletonList(userInvitation));

        assertEquals(1, userInvitationService.findByRoleAndEmail(1L, "test@test.com").size());
    }
}
