package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.model.Company;
import com.grash.model.WebhookDeliveryLog;
import com.grash.model.WebhookEndpoint;
import com.grash.repository.CompanyRepository;
import com.grash.repository.WebhookDeliveryLogRepository;
import com.grash.repository.WebhookEndpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDispatchServiceTest {

    @Mock
    private WebhookEndpointRepository webhookEndpointRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private WebhookDeliveryLogRepository webhookDeliveryLogRepository;
    @Mock
    private LicenseService licenseService;
    @Mock
    private RestTemplate restTemplate;

    private WebhookDispatchService service;
    private WebhookEndpoint endpoint;

    @BeforeEach
    void setUp() {
        service = new WebhookDispatchService(webhookEndpointRepository, companyRepository,
                webhookDeliveryLogRepository, new ObjectMapper(), licenseService);
        service.setRestTemplate(restTemplate);

        Company company = new Company();
        company.setId(1L);
        endpoint = new WebhookEndpoint();
        endpoint.setId(7L);
        endpoint.setCompany(company);
        // literal public IP: no DNS lookup needed by WebhookUrlValidator
        endpoint.setUrl("https://93.184.216.34/hook");
        endpoint.setSecret("whsec_test");

        lenient().when(webhookDeliveryLogRepository.save(any(WebhookDeliveryLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void deliver_success_logsStatusBodyAndSendsBothSignatureHeaders() {
        when(restTemplate.postForEntity(eq(endpoint.getUrl()), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        WebhookDeliveryLog result = service.deliver(endpoint, "NEW_WORK_ORDER", "{\"a\":1}");

        assertTrue(result.isSuccess());
        assertEquals(200, result.getResponseStatus());
        assertEquals("ok", result.getResponseBody());
        assertEquals("{\"a\":1}", result.getPayload());
        assertEquals("NEW_WORK_ORDER", result.getEventType());
        assertSame(endpoint, result.getWebhookEndpoint());
        assertSame(endpoint.getCompany(), result.getCompany());
        assertNotNull(result.getDurationMs());
        assertNull(result.getErrorMessage());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(endpoint.getUrl()), captor.capture(), eq(String.class));
        HttpHeaders headers = captor.getValue().getHeaders();
        assertNotNull(headers.getFirst("X-Webhook-Signature"));
        assertEquals(headers.getFirst("X-Webhook-Signature"), headers.getFirst("X-Atlas-Signature"));
        verify(webhookEndpointRepository).save(endpoint);
        assertNotNull(endpoint.getLastTriggeredAt());
    }

    @Test
    void deliver_truncatesResponseBodyTo4000() {
        when(restTemplate.postForEntity(eq(endpoint.getUrl()), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("x".repeat(10_000)));

        WebhookDeliveryLog result = service.deliver(endpoint, "NEW_WORK_ORDER", "{}");

        assertEquals(4000, result.getResponseBody().length());
    }

    @Test
    void deliver_httpErrorStatus_isLoggedAsFailureWithStatusAndBody() {
        when(restTemplate.postForEntity(eq(endpoint.getUrl()), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "Bad Gateway",
                        new HttpHeaders(), "upstream down".getBytes(), null));

        WebhookDeliveryLog result = service.deliver(endpoint, "NEW_WORK_ORDER", "{}");

        assertFalse(result.isSuccess());
        assertEquals(502, result.getResponseStatus());
        assertEquals("upstream down", result.getResponseBody());
        assertNotNull(result.getErrorMessage());
        verify(webhookEndpointRepository, never()).save(any());
    }

    @Test
    void deliver_connectionException_isLoggedWithNullStatus() {
        when(restTemplate.postForEntity(eq(endpoint.getUrl()), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        WebhookDeliveryLog result = service.deliver(endpoint, "NEW_WORK_ORDER", "{}");

        assertFalse(result.isSuccess());
        assertNull(result.getResponseStatus());
        assertTrue(result.getErrorMessage().contains("timeout"));
        verify(webhookDeliveryLogRepository).save(result);
    }

    @Test
    void deliver_invalidUrl_isLoggedAndNothingIsSent() {
        endpoint.setUrl("ftp://example.com/hook");

        WebhookDeliveryLog result = service.deliver(endpoint, "NEW_WORK_ORDER", "{}");

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        verifyNoInteractions(restTemplate);
    }
}
