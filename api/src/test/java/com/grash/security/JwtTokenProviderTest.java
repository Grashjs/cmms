package com.grash.security;

import com.grash.exception.CustomException;
import com.grash.model.Role;
import com.grash.model.User;
import com.grash.model.enums.RoleType;
import com.grash.utils.Consts;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String base64Key = Base64.getEncoder().encodeToString(new byte[32]);
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", base64Key);
        ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", 3600000L);
        jwtTokenProvider.init();
    }

    @Test
    void createToken_and_getUsername_roundTrip() {
        String username = "john@test.com";
        List<RoleType> roles = List.of(RoleType.ROLE_CLIENT);

        String token = jwtTokenProvider.createToken(username, roles);

        assertNotNull(token);
        String extractedUsername = jwtTokenProvider.getUsername(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void computeAccessTokenExpiration_returnsExpirationAfterValidity() {
        ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", 1800000L);

        Date before = new Date(System.currentTimeMillis());
        Date expiration = jwtTokenProvider.computeAccessTokenExpiration();
        Date after = new Date(System.currentTimeMillis());

        assertTrue(expiration.getTime() >= before.getTime() + 1800000L);
        assertTrue(expiration.getTime() <= after.getTime() + 1800000L);
    }

    @Nested
    class GetAuthentication {

        private User createUserWithRole(String email, boolean enabled) {
            Role role = new Role();
            role.setRoleType(RoleType.ROLE_CLIENT);
            User user = new User();
            user.setEmail(email);
            user.setEnabled(enabled);
            user.setRole(role);
            return user;
        }

        @Test
        void enabledUser_returnsAuthentication() {
            String username = "john@test.com";
            String token = jwtTokenProvider.createToken(username, List.of(RoleType.ROLE_CLIENT));
            User user = createUserWithRole(username, true);
            CustomUserDetail userDetail = CustomUserDetail.builder().user(user).build();
            when(customUserDetailsService.loadUserByUsername(username)).thenReturn(userDetail);

            Authentication auth = jwtTokenProvider.getAuthentication(token);

            assertNotNull(auth);
            assertEquals(userDetail, auth.getPrincipal());
            assertTrue(auth.isAuthenticated());
        }

        @Test
        void disabledUser_throwsCustomException() {
            String username = "disabled@test.com";
            String token = jwtTokenProvider.createToken(username, List.of(RoleType.ROLE_CLIENT));
            User user = createUserWithRole(username, false);
            CustomUserDetail userDetail = CustomUserDetail.builder().user(user).build();
            when(customUserDetailsService.loadUserByUsername(username)).thenReturn(userDetail);

            CustomException ex = assertThrows(CustomException.class,
                    () -> jwtTokenProvider.getAuthentication(token));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
            assertEquals("User account is disabled", ex.getMessage());
        }

        @Test
        void tokenIssuedBeforeSessionInvalidation_isRejected() {
            String username = "revoked@test.com";
            String token = jwtTokenProvider.createToken(username, List.of(RoleType.ROLE_CLIENT));
            User user = createUserWithRole(username, true);
            user.setSessionInvalidatedAt(new Date(System.currentTimeMillis() + 1000));
            CustomUserDetail userDetail = CustomUserDetail.builder().user(user).build();
            when(customUserDetailsService.loadUserByUsername(username)).thenReturn(userDetail);

            CustomException ex = assertThrows(CustomException.class,
                    () -> jwtTokenProvider.getAuthentication(token));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
            assertEquals("Session has been revoked. Please sign in again", ex.getMessage());
        }

        @Test
        void tokenIssuedAfterSessionInvalidation_isAccepted() {
            String username = "revalid@test.com";
            User user = createUserWithRole(username, true);
            user.setSessionInvalidatedAt(new Date(System.currentTimeMillis() - 5000));
            String token = jwtTokenProvider.createToken(username, List.of(RoleType.ROLE_CLIENT));
            CustomUserDetail userDetail = CustomUserDetail.builder().user(user).build();
            when(customUserDetailsService.loadUserByUsername(username)).thenReturn(userDetail);

            Authentication auth = jwtTokenProvider.getAuthentication(token);

            assertNotNull(auth);
            assertTrue(auth.isAuthenticated());
        }
    }

    @Nested
    class ResolveToken {

        @Test
        void authorizationHeaderWithBearer_returnsToken() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer my-jwt-token");

            String token = jwtTokenProvider.resolveToken(request);

            assertEquals("my-jwt-token", token);
        }

        @Test
        void usesConstsTokenPrefix_notHardcodedLiteral() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn(Consts.TOKEN_PREFIX + "sometoken");

            String token = jwtTokenProvider.resolveToken(request);

            assertEquals("sometoken", token);
        }

        @Test
        void authorizationHeaderWithoutBearer_returnsNull() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Basic my-jwt-token");

            String token = jwtTokenProvider.resolveToken(request);

            assertNull(token);
        }

        @Test
        void noAuthorizationHeader_cookieMatch_returnsToken() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn(null);
            Cookie cookie = new Cookie("swagger_jwt", "cookie-jwt");
            when(request.getCookies()).thenReturn(new Cookie[]{cookie});

            String token = jwtTokenProvider.resolveToken(request);

            assertEquals("cookie-jwt", token);
        }

        @Test
        void noAuthorizationHeader_noCookieMatch_returnsNull() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn(null);
            Cookie cookie = new Cookie("other_cookie", "value");
            when(request.getCookies()).thenReturn(new Cookie[]{cookie});

            String token = jwtTokenProvider.resolveToken(request);

            assertNull(token);
        }

        @Test
        void noAuthorizationHeader_noCookies_returnsNull() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(null);

            String token = jwtTokenProvider.resolveToken(request);

            assertNull(token);
        }
    }

    @Nested
    class ValidateToken {

        @Test
        void validToken_returnsTrue() {
            String token = jwtTokenProvider.createToken("user@test.com", List.of(RoleType.ROLE_CLIENT));

            boolean result = jwtTokenProvider.validateToken(token);

            assertTrue(result);
        }

        @Test
        void invalidToken_throwsCustomException() {
            String badToken = "eyJhbGciOiJIUzI1NiJ9.invalid.token";

            CustomException ex = assertThrows(CustomException.class,
                    () -> jwtTokenProvider.validateToken(badToken));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
            assertEquals("Invalid JWT token", ex.getMessage());
        }

        @Test
        void expiredToken_throwsCustomException() {
            ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", -1000L);
            jwtTokenProvider.init();
            String expiredToken = jwtTokenProvider.createToken("expired@test.com", List.of(RoleType.ROLE_CLIENT));

            CustomException ex = assertThrows(CustomException.class,
                    () -> jwtTokenProvider.validateToken(expiredToken));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
            assertEquals("JWT token has expired", ex.getMessage());
        }
    }

    @Nested
    class SignatureValidation {

        @Test
        void tokenSignedWithDifferentKey_throwsCustomException() {
            SecretKey differentKey = Jwts.SIG.HS256.key().build();
            String tamperedToken = Jwts.builder()
                    .subject("hacker@test.com")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(differentKey)
                    .compact();

            CustomException ex = assertThrows(CustomException.class,
                    () -> jwtTokenProvider.validateToken(tamperedToken));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        }
    }

    @Nested
    class GetUsername {

        @Test
        void malformedToken_throwsJwtException() {
            assertThrows(MalformedJwtException.class,
                    () -> jwtTokenProvider.getUsername("not.a.jwt"));
        }

        @Test
        void expiredToken_throwsExpiredJwtException() {
            ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", -1000L);
            jwtTokenProvider.init();
            String expiredToken = jwtTokenProvider.createToken("expired@test.com", List.of(RoleType.ROLE_CLIENT));

            assertThrows(ExpiredJwtException.class,
                    () -> jwtTokenProvider.getUsername(expiredToken));
        }

        // NOTE: Inconsistency — validateToken wraps JwtException in CustomException,
        // but getUsername (and getAuthentication) let raw JJWT exceptions propagate.
    }

    @Nested
    class GetAuthenticationWithMultipleRoles {

        @Test
        void tokenWithMultipleRoles_claimContainsAllRoles_butAuthenticationUsesUserRole() {
            String username = "multi@test.com";
            List<RoleType> jwtRoles = List.of(RoleType.ROLE_CLIENT, RoleType.ROLE_SUPER_ADMIN);
            String token = jwtTokenProvider.createToken(username, jwtRoles);

            SecretKey providerKey = (SecretKey) ReflectionTestUtils.getField(jwtTokenProvider, "key");
            Claims claims = Jwts.parser()
                    .verifyWith(providerKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            List<?> authClaim = (List<?>) claims.get("auth");
            assertNotNull(authClaim);
            assertEquals(2, authClaim.size(),
                    "JWT auth claim should contain both roles");

            Role role = new Role();
            role.setRoleType(RoleType.ROLE_CLIENT);
            User user = new User();
            user.setEmail(username);
            user.setEnabled(true);
            user.setRole(role);
            CustomUserDetail userDetail = CustomUserDetail.builder().user(user).build();
            when(customUserDetailsService.loadUserByUsername(username)).thenReturn(userDetail);

            Authentication authentication = jwtTokenProvider.getAuthentication(token);

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertEquals(1, authorities.size(),
                    "getAuthentication derives authorities from userDetails, not from the JWT auth claim");
            assertEquals("ROLE_CLIENT", authorities.iterator().next().getAuthority());
        }
    }
}
