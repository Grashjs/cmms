package com.grash.service;

import com.grash.dto.AuthTokens;
import com.grash.exception.CustomException;
import com.grash.model.RefreshToken;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.enums.RoleType;
import com.grash.repository.RefreshTokenRepository;
import com.grash.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenValidityInMilliseconds", 604800000L);
        role = Role.builder().id(1L).roleType(RoleType.ROLE_CLIENT).build();
        user = new User();
        user.setId(1L);
        user.setEmail("john@test.com");
        user.setRole(role);
        user.setEnabled(true);
    }

    @Test
    void createRefreshToken_returnsRawTokenAndStoresOnlyHash() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String raw = refreshTokenService.createRefreshToken(user);

        assertNotNull(raw);
        assertNotEquals(raw, refreshTokenService.hash(raw));
        verify(refreshTokenRepository).save(argThat(rt ->
                rt.getTokenHash().equals(refreshTokenService.hash(raw))
                        && rt.getUser().equals(user)
                        && !rt.isRevoked()
                        && rt.getExpiresAt().after(new Date())));
    }

    @Test
    void createTokenPair_returnsAccessAndRefreshTokens() {
        when(jwtTokenProvider.createToken("john@test.com", Collections.singletonList(RoleType.ROLE_CLIENT)))
                .thenReturn("access-token");
        when(jwtTokenProvider.computeAccessTokenExpiration()).thenReturn(new Date(1784300000000L));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokens tokens = refreshTokenService.createTokenPair(user);

        assertEquals("access-token", tokens.getAccessToken());
        assertNotNull(tokens.getRefreshToken());
        assertEquals(new Date(1784300000000L), tokens.getAccessTokenExpiresAt());
    }

    @Test
    void hash_isDeterministicAndDoesNotContainRawToken() {
        String raw = "super-secret-refresh-token";
        String h1 = refreshTokenService.hash(raw);
        String h2 = refreshTokenService.hash(raw);

        assertEquals(h1, h2);
        assertFalse(h1.contains(raw));
        assertEquals(64, h1.length());
    }

    @Test
    void revokeAllForUser_marksAllTokensRevoked() {
        refreshTokenService.revokeAllForUser(user);

        verify(refreshTokenRepository).revokeAllByUser(user.getId());
    }

    @Nested
    class Rotate {

        private RefreshToken buildValidStored(String raw) {
            return RefreshToken.builder()
                    .id(1L)
                    .tokenHash(refreshTokenService.hash(raw))
                    .user(user)
                    .createdAt(new Date(System.currentTimeMillis() - 1000))
                    .expiresAt(new Date(System.currentTimeMillis() + 60000))
                    .revoked(false)
                    .build();
        }

        @Test
        void validToken_issuesNewPairAndRevokesOld() {
            String raw = "raw-token";
            RefreshToken stored = buildValidStored(raw);
            when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                    .thenReturn(Optional.of(stored));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtTokenProvider.createToken("john@test.com", Collections.singletonList(RoleType.ROLE_CLIENT)))
                    .thenReturn("new-access");
            when(jwtTokenProvider.computeAccessTokenExpiration()).thenReturn(new Date(1784300000000L));

            AuthTokens tokens = refreshTokenService.rotate(raw);

            assertEquals("new-access", tokens.getAccessToken());
            assertNotNull(tokens.getRefreshToken());
            assertEquals(new Date(1784300000000L), tokens.getAccessTokenExpiresAt());
            assertTrue(stored.isRevoked());
            assertNotNull(stored.getReplacedByTokenHash());
            assertEquals(refreshTokenService.hash(tokens.getRefreshToken()), stored.getReplacedByTokenHash());
        }

        @Test
        void unknownToken_throws401() {
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            CustomException ex = assertThrows(CustomException.class,
                    () -> refreshTokenService.rotate("unknown"));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        }

        @Test
        void revokedToken_detectsReuse_andRevokesAllSessions() {
            String raw = "raw-token";
            RefreshToken stored = buildValidStored(raw);
            stored.setRevoked(true);
            when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                    .thenReturn(Optional.of(stored));

            CustomException ex = assertThrows(CustomException.class,
                    () -> refreshTokenService.rotate(raw));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
            verify(refreshTokenRepository).revokeAllByUser(user.getId());
        }

        @Test
        void expiredToken_throws401AndRevokes() {
            String raw = "raw-token";
            RefreshToken stored = buildValidStored(raw);
            stored.setExpiresAt(new Date(System.currentTimeMillis() - 1000));
            when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                    .thenReturn(Optional.of(stored));

            CustomException ex = assertThrows(CustomException.class,
                    () -> refreshTokenService.rotate(raw));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
            assertTrue(stored.isRevoked());
            verify(refreshTokenRepository).save(stored);
        }

        @Test
        void tokenCreatedBeforeSessionInvalidation_throws401() {
            String raw = "raw-token";
            user.setSessionInvalidatedAt(new Date(System.currentTimeMillis()));
            RefreshToken stored = buildValidStored(raw);
            when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                    .thenReturn(Optional.of(stored));

            CustomException ex = assertThrows(CustomException.class,
                    () -> refreshTokenService.rotate(raw));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        }
    }
}
