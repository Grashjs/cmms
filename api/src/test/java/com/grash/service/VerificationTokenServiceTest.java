package com.grash.service;

import com.grash.dto.AuthResponse;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.VerificationToken;
import com.grash.repository.UserRepository;
import com.grash.repository.VerificationTokenRepository;
import com.grash.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VerificationTokenService verificationTokenService;

    private VerificationToken verificationToken;
    private OwnUser user;

    @BeforeEach
    void setUp() {
        user = new OwnUser();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setRole(new Role());

        verificationToken = new VerificationToken();
        verificationToken.setId(1L);
        verificationToken.setToken("test-token");
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(new Date(System.currentTimeMillis() + 100000));
    }

    @Test
    void getVerificationTokenEntity() {
        when(verificationTokenRepository.findVerificationTokenEntityByToken("test-token")).thenReturn(verificationToken);

        VerificationToken result = verificationTokenService.getVerificationTokenEntity("test-token");

        assertNotNull(result);
        assertEquals(verificationToken, result);
    }

    @Test
    void deleteVerificationTokenEntity() {
        ArrayList<VerificationToken> tokens = new ArrayList<>();
        tokens.add(verificationToken);
        when(verificationTokenRepository.findAllVerificationTokenEntityByUser(user)).thenReturn(tokens);

        verificationTokenService.deleteVerificationTokenEntity(user);

        verify(verificationTokenRepository).deleteAll(tokens);
    }

    @Test
    void confirmMail() throws Exception {
        when(verificationTokenRepository.findVerificationTokenEntityByToken("test-token")).thenReturn(verificationToken);
        when(jwtTokenProvider.createToken(any(), any())).thenReturn("jwt-token");

        AuthResponse response = verificationTokenService.confirmMail("test-token");

        assertNotNull(response);
        assertEquals("jwt-token", response.getAccessToken());
        verify(userService).enableUser("test@test.com");
    }

    @Test
    void confirmResetPassword() throws Exception {
        verificationToken.setPayload("new-password");
        when(verificationTokenRepository.findVerificationTokenEntityByToken("test-token")).thenReturn(verificationToken);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
        when(userRepository.save(any(OwnUser.class))).thenReturn(user);

        OwnUser result = verificationTokenService.confirmResetPassword("test-token");

        assertNotNull(result);
        assertEquals("encoded-password", result.getPassword());
        verify(userRepository).save(user);
    }
}
