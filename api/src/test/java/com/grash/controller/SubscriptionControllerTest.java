package com.grash.controller;

import com.grash.dto.BrandConfig;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.Company;
import com.grash.model.OwnUser;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionChangeRequest;
import com.grash.repository.SubscriptionChangeRequestRepository;
import com.grash.service.BrandingService;
import com.grash.service.EmailService2;
import com.grash.service.SubscriptionService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.mail.MessagingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @InjectMocks
    private SubscriptionController subscriptionController;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserService userService;

    @Mock
    private EmailService2 emailService2;

    @Mock
    private SubscriptionChangeRequestRepository subscriptionChangeRequestRepository;

    @Mock
    private BrandingService brandingService;

    private MockHttpServletRequest request;
    private OwnUser companyOwner;
    private Company company;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        subscription = new Subscription();
        subscription.setId(1L);
        subscription.setUsersCount(10);
        company.setSubscription(subscription);

        companyOwner = new OwnUser();
        companyOwner.setId(1L);
        companyOwner.setCompany(company);
        companyOwner.setOwnsCompany(true);
    }

    @Nested
    @DisplayName("getAll Tests")
    class GetAllTests {

        @Test
        @DisplayName("Should return all subscriptions for SUPER_ADMIN")
        void getAll_shouldReturnAllSubscriptions() {
            // Given
            Subscription sub1 = new Subscription();
            Subscription sub2 = new Subscription();
            List<Subscription> subscriptions = Arrays.asList(sub1, sub2);
            when(subscriptionService.getAll()).thenReturn(subscriptions);

            // When
            List<Subscription> result = (List<Subscription>) subscriptionController.getAll(request);

            // Then
            assertEquals(2, result.size());
            verify(subscriptionService, times(1)).getAll();
        }
    }

    @Nested
    @DisplayName("upgrade Tests")
    class UpgradeTests {

        @Test
        @DisplayName("Should upgrade successfully when conditions are met")
        void upgrade_success() {
            // Given
            OwnUser userToEnable = new OwnUser();
            userToEnable.setId(2L);
            userToEnable.setEnabledInSubscription(false);
            userToEnable.setCompany(company);

            when(userService.whoami(request)).thenReturn(companyOwner);
            when(userService.findByCompany(company.getId())).thenReturn(Collections.singletonList(companyOwner));
            when(userService.findByIdAndCompany(2L, company.getId())).thenReturn(Optional.of(userToEnable));

            // When
            SuccessResponse response = subscriptionController.upgrade(Collections.singletonList(2L), request);

            // Then
            assertTrue(response.isSuccess());
            assertEquals("Users enabled successfully", response.getMessage());
            assertTrue(userToEnable.isEnabledInSubscription());
            verify(userService, times(1)).saveAll(anyCollection());
            verify(subscriptionService, times(1)).save(subscription);
        }

        @Test
        @DisplayName("Should throw FORBIDDEN when user is not company owner")
        void upgrade_notOwner_shouldThrowForbidden() {
            // Given
            companyOwner.setOwnsCompany(false);
            when(userService.whoami(request)).thenReturn(companyOwner);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> subscriptionController.upgrade(Collections.singletonList(2L), request));
            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw NOT_ACCEPTABLE when exceeding user count")
        void upgrade_exceedsUserCount_shouldThrowNotAcceptable() {
            // Given
            subscription.setUsersCount(1);
            when(userService.whoami(request)).thenReturn(companyOwner);
            when(userService.findByCompany(company.getId())).thenReturn(Collections.singletonList(companyOwner));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> subscriptionController.upgrade(Arrays.asList(2L, 3L), request));
            assertEquals("The subscription users count doesn't permit this operation", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw NOT_ACCEPTABLE if any user is already enabled")
        void upgrade_userAlreadyEnabled_shouldThrowNotAcceptable() {
            // Given
            OwnUser alreadyEnabledUser = new OwnUser();
            alreadyEnabledUser.setId(2L);
            alreadyEnabledUser.setEnabledInSubscription(true);
            alreadyEnabledUser.setCompany(company);

            when(userService.whoami(request)).thenReturn(companyOwner);
            when(userService.findByCompany(company.getId())).thenReturn(Collections.singletonList(companyOwner));
            when(userService.findByIdAndCompany(2L, company.getId())).thenReturn(Optional.of(alreadyEnabledUser));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> subscriptionController.upgrade(Collections.singletonList(2L), request));
            assertEquals("There are some already enabled users", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("requestUpgrade Tests")
    class RequestUpgradeTests {

        private SubscriptionChangeRequest changeRequest;

        @BeforeEach
        void setUp() {
            changeRequest = new SubscriptionChangeRequest();
            changeRequest.setUsersCount(20);
            changeRequest.setCode("TEST_CODE");
            changeRequest.setMonthly(true);
            ReflectionTestUtils.setField(subscriptionController, "recipients", new String[]{"test@example.com"});
        }

        @Test
        @DisplayName("Should send upgrade request successfully")
        void requestUpgrade_success() throws MessagingException {
            // Given
            when(userService.whoami(request)).thenReturn(companyOwner);
            BrandConfig brandConfig = BrandConfig.builder().shortName("TestBrand").build();
            when(brandingService.getBrandConfig()).thenReturn(brandConfig);

            // When
            SuccessResponse response = subscriptionController.requestUpgrade(changeRequest, request);

            // Then
            assertTrue(response.isSuccess());
            assertEquals("Success", response.getMessage());
            verify(subscriptionChangeRequestRepository, times(1)).save(changeRequest);
            verify(emailService2, times(1)).sendHtmlMessage(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw INTERNAL_SERVER_ERROR if mail recipients not set")
        void requestUpgrade_noRecipients_shouldThrowError() {
            // Given
            ReflectionTestUtils.setField(subscriptionController, "recipients", null);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> subscriptionController.requestUpgrade(changeRequest, request));
            assertEquals("MAIL_RECIPIENTS env variable not set", exception.getMessage());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw FORBIDDEN when user is not company owner")
        void requestUpgrade_notOwner_shouldThrowForbidden() {
            // Given
            companyOwner.setOwnsCompany(false);
            when(userService.whoami(request)).thenReturn(companyOwner);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> subscriptionController.requestUpgrade(changeRequest, request));
            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw INTERNAL_SERVER_ERROR on messaging exception")
        void requestUpgrade_messagingException_shouldThrowError() throws MessagingException {
            // Given
            when(userService.whoami(request)).thenReturn(companyOwner);
            BrandConfig brandConfig = BrandConfig.builder().shortName("TestBrand").build();
            when(brandingService.getBrandConfig()).thenReturn(brandConfig);
            doThrow(new MessagingException("Mail send failed")).when(emailService2).sendHtmlMessage(any(), anyString(), anyString());

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> subscriptionController.requestUpgrade(changeRequest, request));
            assertEquals("Mail send failed", exception.getMessage());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("downgrade Tests")
    class DowngradeTests {

        private OwnUser userToDisable;

        @BeforeEach
        void setUp() {
            userToDisable = new OwnUser();
            userToDisable.setId(2L);
            userToDisable.setEnabledInSubscription(true);
            userToDisable.setOwnsCompany(false);
            userToDisable.setCompany(company);
        }

        @Test
        @DisplayName("Should downgrade successfully when conditions are met")
        void downgrade_success() {
            // Given
            companyOwner.setEnabledInSubscription(true);
            List<OwnUser> usersInCompany = Stream.of(companyOwner, userToDisable).collect(Collectors.toList());

            when(userService.whoami(request)).thenReturn(companyOwner);
            when(userService.findByCompany(company.getId())).thenReturn(usersInCompany);
            when(userService.findByIdAndCompany(2L, company.getId())).thenReturn(Optional.of(userToDisable));

            // When
            SuccessResponse response = subscriptionController.downgrade(Collections.singletonList(2L), request);

            // Then
            assertTrue(response.isSuccess());
            assertEquals("Users enabled successfully", response.getMessage());
            assertFalse(userToDisable.isEnabledInSubscription());
            verify(userService, times(1)).saveAll(anyCollection());
            verify(subscriptionService, times(1)).save(subscription);
        }

        @Test
        @DisplayName("Should throw FORBIDDEN when user is not company owner")
        void downgrade_notOwner_shouldThrowForbidden() {
            // Given
            companyOwner.setOwnsCompany(false);
            when(userService.whoami(request)).thenReturn(companyOwner);

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> subscriptionController.downgrade(Collections.singletonList(2L), request));
            assertEquals("Access Denied", exception.getMessage());
            assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should throw NOT_ACCEPTABLE if any user is already disabled")
        void downgrade_userAlreadyDisabled_shouldThrowNotAcceptable() {
            // Given
            userToDisable.setEnabledInSubscription(false);
            companyOwner.setEnabledInSubscription(true);
            List<OwnUser> usersInCompany = Stream.of(companyOwner, userToDisable).collect(Collectors.toList());

            when(userService.whoami(request)).thenReturn(companyOwner);
            when(userService.findByCompany(company.getId())).thenReturn(usersInCompany);
            when(userService.findByIdAndCompany(2L, company.getId())).thenReturn(Optional.of(userToDisable));

            // When & Then
            CustomException exception = assertThrows(CustomException.class,
                    () -> subscriptionController.downgrade(Collections.singletonList(2L), request));
            assertEquals("There are some already disabled users", exception.getMessage());
            assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus());
        }

        @Test
        @DisplayName("Should not downgrade the company owner")
        void downgrade_shouldNotDowngradeOwner() {
            // Given
            companyOwner.setEnabledInSubscription(true);
            List<OwnUser> usersInCompany = Collections.singletonList(companyOwner);

            when(userService.whoami(request)).thenReturn(companyOwner);
            when(userService.findByCompany(company.getId())).thenReturn(usersInCompany);
            when(userService.findByIdAndCompany(1L, company.getId())).thenReturn(Optional.of(companyOwner));

            // When
            subscriptionController.downgrade(Collections.singletonList(1L), request);

            // Then
            verify(userService, times(1)).saveAll(argThat(list -> list.isEmpty()));
            assertTrue(companyOwner.isEnabledInSubscription());
        }
    }
}
