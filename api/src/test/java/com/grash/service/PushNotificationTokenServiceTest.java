package com.grash.service;

import com.grash.model.OwnUser;
import com.grash.model.PushNotificationToken;
import com.grash.repository.PushNotificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationTokenServiceTest {

    @Mock
    private PushNotificationTokenRepository pushNotificationTokenRepository;

    @InjectMocks
    private PushNotificationTokenService pushNotificationTokenService;

    private PushNotificationToken pushNotificationToken;
    private OwnUser user;

    @BeforeEach
    void setUp() {
        user = new OwnUser();
        user.setId(1L);

        pushNotificationToken = new PushNotificationToken();
        pushNotificationToken.setId(1L);
        pushNotificationToken.setUser(user);
    }

    @Test
    void create() {
        when(pushNotificationTokenRepository.save(any(PushNotificationToken.class))).thenReturn(pushNotificationToken);

        PushNotificationToken result = pushNotificationTokenService.create(pushNotificationToken);

        assertNotNull(result);
        assertEquals(pushNotificationToken.getId(), result.getId());
        verify(pushNotificationTokenRepository).save(pushNotificationToken);
    }

    @Test
    void findByUser() {
        when(pushNotificationTokenRepository.findByUser_Id(1L)).thenReturn(Optional.of(pushNotificationToken));

        assertTrue(pushNotificationTokenService.findByUser(1L).isPresent());
    }

    @Test
    void save() {
        when(pushNotificationTokenRepository.save(any(PushNotificationToken.class))).thenReturn(pushNotificationToken);

        PushNotificationToken result = pushNotificationTokenService.save(pushNotificationToken);

        assertNotNull(result);
        assertEquals(pushNotificationToken.getId(), result.getId());
        verify(pushNotificationTokenRepository).save(pushNotificationToken);
    }

    @Test
    void delete() {
        doNothing().when(pushNotificationTokenRepository).deleteById(1L);
        pushNotificationTokenService.delete(1L);
        verify(pushNotificationTokenRepository).deleteById(1L);
    }
}
