package com.grash.service;

import com.grash.dto.AuthTokens;
import com.grash.exception.CustomException;
import com.grash.model.RefreshToken;
import com.grash.model.User;
import com.grash.repository.RefreshTokenRepository;
import com.grash.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${security.jwt.refresh.expire-length:604800000}")
    private long refreshTokenValidityInMilliseconds;

    public AuthTokens createTokenPair(User user) {
        String accessToken = jwtTokenProvider.createToken(user.getEmail(),
                Collections.singletonList(user.getRole().getRoleType()));
        String refreshToken = createRefreshToken(user);
        return new AuthTokens(accessToken, refreshToken, jwtTokenProvider.computeAccessTokenExpiration());
    }

    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .createdAt(new Date())
                .expiresAt(new Date(System.currentTimeMillis() + refreshTokenValidityInMilliseconds))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public AuthTokens rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new CustomException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked()) {
            // Token reuse detection: a previously rotated/revoked token was presented again.
            revokeAllForUser(stored.getUser());
            throw new CustomException("Refresh token reuse detected. All sessions have been revoked. Please sign in " +
                    "again",
                    HttpStatus.UNAUTHORIZED);
        }

        if (stored.getExpiresAt().before(new Date())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new CustomException("Refresh token expired. Please sign in again", HttpStatus.UNAUTHORIZED);
        }

        User user = stored.getUser();
        Date sessionInvalidatedAt = user.getSessionInvalidatedAt();
        if (sessionInvalidatedAt != null && stored.getCreatedAt().before(sessionInvalidatedAt)) {
            throw new CustomException("Session has been revoked. Please sign in again", HttpStatus.UNAUTHORIZED);
        }

        String newRawToken = createRefreshToken(user);
        stored.setRevoked(true);
        stored.setReplacedByTokenHash(hash(newRawToken));
        refreshTokenRepository.save(stored);

        String accessToken = jwtTokenProvider.createToken(user.getEmail(),
                Collections.singletonList(user.getRole().getRoleType()));
        return new AuthTokens(accessToken, newRawToken, jwtTokenProvider.computeAccessTokenExpiration());
    }

    @Transactional
    public void revokeAllForUser(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser(user);
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
        }
        if (!tokens.isEmpty()) {
            refreshTokenRepository.saveAll(tokens);
        }
    }

    String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
