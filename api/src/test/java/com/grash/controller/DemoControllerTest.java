package com.grash.controller;

import com.grash.dto.SignupSuccessResponse;
import com.grash.dto.SuccessResponse;
import com.grash.model.Asset;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Role;
import com.grash.model.enums.RoleType;
import com.grash.service.AssetService;
import com.grash.service.ImportService;
import com.grash.service.RateLimiterService;
import com.grash.service.UserService;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoControllerTest {

    @InjectMocks
    private DemoController demoController;

    @Mock
    private UserService userService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private ImportService importService;

    @Mock
    private AssetService assetService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Bucket bucket;

    private OwnUser demoUser;
    private Company demoCompany;

    @BeforeEach
    void setUp() {
        demoCompany = new Company();
        demoCompany.setId(1L);
        demoCompany.setName("Demo Company");

        Role demoRole = new Role();
        demoRole.setRoleType(RoleType.ROLE_CLIENT);

        demoUser = new OwnUser();
        demoUser.setId(1L);
        demoUser.setCompany(demoCompany);
        demoUser.setRole(demoRole);
        demoUser.setFirstName("Demo");
        demoUser.setLastName("User");

        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("generateAccount Tests")
    class GenerateAccountTests {

        @Test
        @DisplayName("Should generate account and import data when rate limit is not exceeded and signup succeeds")
        void generateAccount_success() {
            // Given
            Asset mockAsset = new Asset();
            mockAsset.setName("Mock Asset");

            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(rateLimiterService.resolveBucket(anyString())).thenReturn(bucket);
            when(bucket.tryConsume(1)).thenReturn(true);

            SignupSuccessResponse<OwnUser> signupResponse = new SignupSuccessResponse<>(true, "Success", demoUser);
            when(userService.signup(any())).thenReturn(signupResponse);
            when(assetService.findByCompany(demoCompany.getId())).thenReturn(Collections.singletonList(mockAsset));

            // When
            SuccessResponse response = demoController.generateAccount(request);

            // Then
            assertTrue(response.isSuccess());
            assertEquals("Success", response.getMessage());
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());

            // Verify that import methods are called.
            // We can't verify the private methods directly, but we can verify their effects (calls to services).
            // The test setup doesn't load real CSV files, so the lists passed to import services will be empty.
            verify(importService, times(1)).importLocations(anyList(), any(Company.class));
            verify(importService, times(1)).importAssets(anyList(), any(Company.class));
            verify(importService, times(1)).importMeters(anyList(), any(Company.class));
            verify(importService, times(1)).importParts(anyList(), any(Company.class));
            verify(importService, times(1)).importWorkOrders(anyList(), any(Company.class));
        }

        @Test
        @DisplayName("Should return failure when rate limit is exceeded")
        void generateAccount_rateLimitExceeded() {
            // Given
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(rateLimiterService.resolveBucket(anyString())).thenReturn(bucket);
            when(bucket.tryConsume(1)).thenReturn(false);

            // When
            SuccessResponse response = demoController.generateAccount(request);

            // Then
            assertFalse(response.isSuccess());
            assertEquals("Rate limit exceeded. Try again later.", response.getMessage());
            verify(userService, never()).signup(any());
            verify(importService, never()).importLocations(any(), any());
        }

        @Test
        @DisplayName("Should not import data when signup fails")
        void generateAccount_signupFails() {
            // Given
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(rateLimiterService.resolveBucket(anyString())).thenReturn(bucket);
            when(bucket.tryConsume(1)).thenReturn(true);

            SignupSuccessResponse<OwnUser> signupResponse = new SignupSuccessResponse<>(false, "Signup Failed", null);
            when(userService.signup(any())).thenReturn(signupResponse);

            // When
            SuccessResponse response = demoController.generateAccount(request);

            // Then
            assertFalse(response.isSuccess());
            assertEquals("Signup Failed", response.getMessage());
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(importService, never()).importLocations(any(), any());
        }

        @Test
        @DisplayName("Should stop import on failure")
        void generateAccount_shouldStopImportOnFailure() {
            // Given
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(rateLimiterService.resolveBucket(anyString())).thenReturn(bucket);
            when(bucket.tryConsume(1)).thenReturn(true);

            SignupSuccessResponse<OwnUser> signupResponse = new SignupSuccessResponse<>(true, "Success", demoUser);
            when(userService.signup(any())).thenReturn(signupResponse);

            // This test is tricky because the exception is caught inside a private method.
            // We can't directly test the catch block without refactoring.
            // However, we can simulate a failure in one of the import steps and verify that subsequent steps are not executed.
            // Note: The private `parseCsv` method throws IOException, which is caught.
            // We will throw a RuntimeException from the service layer to test the sequence.
            doThrow(new RuntimeException("DB error")).when(importService).importLocations(anyList(), any(Company.class));

            // When
            // The exception will be caught by JUnit's default handler for uncaught exceptions,
            // but the important part is to verify the interactions.
            assertThrows(RuntimeException.class, () -> {
                demoController.generateAccount(request);
            });


            // Then
            // The overall response is still success because the exception is caught and printed.
            // We can verify that the other import methods were not called after the exception.
            verify(importService, times(1)).importLocations(anyList(), any(Company.class));
            verify(importService, never()).importAssets(anyList(), any(Company.class));
        }
    }
}
