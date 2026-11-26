package com.grash.service;

import com.grash.dto.license.LicenseEntitlement;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import com.grash.repository.WebhookDeliveryLogRepository;
import com.grash.repository.WebhookEndpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryLogServiceTest {

    @Mock
    private WebhookDeliveryLogRepository logRepository;
    @Mock
    private WebhookEndpointRepository endpointRepository;
    @Mock
    private WebhookDispatchService dispatchService;
    @Mock
    private LicenseService licenseService;
    @InjectMocks
    private WebhookDeliveryLogService service;

    private User user;
    private SubscriptionPlan plan;

    @BeforeEach
    void setUp() {
        plan = new SubscriptionPlan();
        plan.setFeatures(new HashSet<>(Set.of(PlanFeatures.WEBHOOK)));
        Subscription subscription = new Subscription();
        subscription.setSubscriptionPlan(plan);
        Company company = new Company();
        company.setId(1L);
        company.setSubscription(subscription);
        user = new User();
        user.setCompany(company);
        user.setRole(Role.builder().viewPermissions(new HashSet<>(Set.of(PermissionEntity.SETTINGS))).build());
        lenient().when(licenseService.hasEntitlement(LicenseEntitlement.WEBHOOK)).thenReturn(true);
    }

    @Test
    void findByEndpoint_withoutLicense_forbidden() {
        when(licenseService.hasEntitlement(LicenseEntitlement.WEBHOOK)).thenReturn(false);
        CustomException e = assertThrows(CustomException.class,
                () -> service.findByEndpoint(5L, PageRequest.of(0, 20), user));
        assertEquals(HttpStatus.FORBIDDEN, e.getHttpStatus());
        verifyNoInteractions(logRepository);
    }

    @Test
    void findByEndpoint_planWithoutWebhook_forbidden() {
        plan.setFeatures(new HashSet<>());
        assertThrows(CustomException.class, () -> service.findByEndpoint(5L, PageRequest.of(0, 20), user));
    }

    @Test
    void findByEndpoint_withoutSettingsPermission_forbidden() {
        user.getRole().setViewPermissions(new HashSet<>());
        assertThrows(CustomException.class, () -> service.findByEndpoint(5L, PageRequest.of(0, 20), user));
    }

    @Test
    void findByEndpoint_endpointNotInCompany_notFound() {
        when(endpointRepository.findByIdAndCompanyId(5L, 1L)).thenReturn(Optional.empty());
        CustomException e = assertThrows(CustomException.class,
                () -> service.findByEndpoint(5L, PageRequest.of(0, 20), user));
        assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus());
    }

    @Test
    void retry_logNotInCompany_notFound() {
        when(logRepository.findByIdAndCompanyId(3L, 1L)).thenReturn(Optional.empty());
        CustomException e = assertThrows(CustomException.class, () -> service.retry(3L, user));
        assertEquals(HttpStatus.NOT_FOUND, e.getHttpStatus());
        verifyNoInteractions(dispatchService);
    }

    @Test
    void retry_resendsStoredPayloadAndReturnsNewLog() {
        WebhookEndpoint endpoint = new WebhookEndpoint();
        WebhookDeliveryLog original = new WebhookDeliveryLog();
        original.setWebhookEndpoint(endpoint);
        original.setEventType("NEW_ASSET");
        original.setPayload("{\"x\":1}");
        WebhookDeliveryLog fresh = new WebhookDeliveryLog();
        when(logRepository.findByIdAndCompanyId(3L, 1L)).thenReturn(Optional.of(original));
        when(dispatchService.deliver(endpoint, "NEW_ASSET", "{\"x\":1}")).thenReturn(fresh);

        assertSame(fresh, service.retry(3L, user));
    }

    @Test
    void retry_withoutLicense_forbidden() {
        when(licenseService.hasEntitlement(LicenseEntitlement.WEBHOOK)).thenReturn(false);
        assertThrows(CustomException.class, () -> service.retry(3L, user));
        verifyNoInteractions(logRepository, dispatchService);
    }
}
