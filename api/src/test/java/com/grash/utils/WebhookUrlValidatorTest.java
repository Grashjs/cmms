package com.grash.utils;

import com.grash.utils.WebhookUrlValidator.WebhookUrlValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class WebhookUrlValidatorTest {

    @Test
    void nullUrl_throws() {
        WebhookUrlValidationException ex = assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate(null));
        assertTrue(ex.getMessage().contains("must not be empty"));
    }

    @Test
    void blankUrl_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("   "));
    }

    @Test
    void invalidUri_throws() {
        WebhookUrlValidationException ex = assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://exa mple.com"));
        assertTrue(ex.getMessage().contains("not a valid URI"));
    }

    @Test
    void missingScheme_throws() {
        WebhookUrlValidationException ex = assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("example.com"));
        assertTrue(ex.getMessage().contains("http or https"));
    }

    @Test
    void unsupportedScheme_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("ftp://example.com"));
    }

    @Test
    void missingHost_throws() {
        WebhookUrlValidationException ex = assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http:///path"));
        assertTrue(ex.getMessage().contains("must have a valid host"));
    }

    @Test
    void bracketNotationHost_throws() {
        WebhookUrlValidationException ex = assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://[::1]/"));
        assertTrue(ex.getMessage().contains("bracket notation"));
    }

    @Test
    void unresolvableHost_unknownHost_throws() throws UnknownHostException {
        UnknownHostException cause = new UnknownHostException("no such host");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("nonexistent.invalid")).thenThrow(cause);
            WebhookUrlValidationException ex = assertThrows(WebhookUrlValidationException.class,
                    () -> WebhookUrlValidator.validate("http://nonexistent.invalid/webhook"));
            assertTrue(ex.getMessage().contains("could not be resolved"));
        }
    }

    @Test
    void validHttpUrl_doesNotThrow() throws UnknownHostException {
        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("webhook.example.com"))
                    .thenReturn(new InetAddress[]{publicIp});
            assertDoesNotThrow(() -> WebhookUrlValidator.validate("http://webhook.example.com/hook"));
        }
    }

    @Test
    void validHttpsUrl_doesNotThrow() throws UnknownHostException {
        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("webhook.example.com"))
                    .thenReturn(new InetAddress[]{publicIp});
            assertDoesNotThrow(() -> WebhookUrlValidator.validate("https://webhook.example.com/hook"));
        }
    }

    @Test
    void uppercaseScheme_isAllowed() {
        assertDoesNotThrow(() -> WebhookUrlValidator.validate("HTTPS://8.8.8.8/hook"));
    }

    @Test
    void publicLiteralIp_doesNotThrow() {
        assertDoesNotThrow(() -> WebhookUrlValidator.validate("http://8.8.8.8:8080/hook"));
    }

    @Test
    void loopbackLiteral_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://127.0.0.1/webhook"));
    }

    @Test
    void privateLiteral_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://10.0.0.1/webhook"));
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://172.16.0.1/webhook"));
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://192.168.1.1/webhook"));
    }

    @Test
    void linkLocalLiteral_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://169.254.169.254/webhook"));
    }

    @Test
    void cgnatLiteral_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://100.64.0.1/webhook"));
    }

    @Test
    void unspecifiedLiteral_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://0.0.0.0/webhook"));
    }

    @Test
    void multicastLiteral_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://224.0.0.1/webhook"));
    }

    @Test
    void reservedLiteral_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://240.0.0.1/webhook"));
    }

    @Test
    void ietfAssignmentLiteral_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://192.0.0.10/webhook"));
    }

    @Test
    void documentationLiteral_throws() {
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://198.51.100.5/webhook"));
        assertThrows(WebhookUrlValidationException.class,
                () -> WebhookUrlValidator.validate("http://203.0.113.5/webhook"));
    }

    @Test
    void privateHost_throws() throws UnknownHostException {
        InetAddress privateIp = InetAddress.getByName("10.1.2.3");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("internal.example.com"))
                    .thenReturn(new InetAddress[]{privateIp});
            WebhookUrlValidationException ex = assertThrows(WebhookUrlValidationException.class,
                    () -> WebhookUrlValidator.validate("http://internal.example.com/hook"));
            assertTrue(ex.getMessage().contains("private, loopback"));
        }
    }

    @Test
    void ipv6LoopbackHost_throws() throws UnknownHostException {
        InetAddress loopbackV6 = InetAddress.getByName("::1");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("ipv6host.example.com"))
                    .thenReturn(new InetAddress[]{loopbackV6});
            assertThrows(WebhookUrlValidationException.class,
                    () -> WebhookUrlValidator.validate("http://ipv6host.example.com/hook"));
        }
    }

    @Test
    void ipv6LinkLocalHost_throws() throws UnknownHostException {
        InetAddress linkLocalV6 = InetAddress.getByName("fe80::1");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("ipv6host.example.com"))
                    .thenReturn(new InetAddress[]{linkLocalV6});
            assertThrows(WebhookUrlValidationException.class,
                    () -> WebhookUrlValidator.validate("http://ipv6host.example.com/hook"));
        }
    }

    @Test
    void ipv6UniqueLocalHost_throws() throws UnknownHostException {
        InetAddress uniqueLocalV6 = InetAddress.getByName("fd00::1");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("ipv6host.example.com"))
                    .thenReturn(new InetAddress[]{uniqueLocalV6});
            assertThrows(WebhookUrlValidationException.class,
                    () -> WebhookUrlValidator.validate("http://ipv6host.example.com/hook"));
        }
    }

    @Test
    void ipv6PublicHost_doesNotThrow() throws UnknownHostException {
        InetAddress publicV6 = InetAddress.getByName("2606:4700:4700::1111");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("ipv6host.example.com"))
                    .thenReturn(new InetAddress[]{publicV6});
            assertDoesNotThrow(() -> WebhookUrlValidator.validate("http://ipv6host.example.com/hook"));
        }
    }

    @Test
    void metadataGoogleHost_throws() throws UnknownHostException {
        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("metadata.google.internal"))
                    .thenReturn(new InetAddress[]{publicIp});
            WebhookUrlValidationException ex = assertThrows(WebhookUrlValidationException.class,
                    () -> WebhookUrlValidator.validate("http://metadata.google.internal/"));
            assertTrue(ex.getMessage().contains("cloud metadata"));
        }
    }

    @Test
    void metadataGoogleSubdomain_throws() throws UnknownHostException {
        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("api.metadata.google.internal"))
                    .thenReturn(new InetAddress[]{publicIp});
            assertThrows(WebhookUrlValidationException.class,
                    () -> WebhookUrlValidator.validate("http://api.metadata.google.internal/"));
        }
    }

    @Test
    void instanceDataHost_throws() throws UnknownHostException {
        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        try (MockedStatic<InetAddress> dns = mockStatic(InetAddress.class)) {
            dns.when(() -> InetAddress.getAllByName("instance-data"))
                    .thenReturn(new InetAddress[]{publicIp});
            assertThrows(WebhookUrlValidationException.class,
                    () -> WebhookUrlValidator.validate("http://instance-data/"));
        }
    }
}
