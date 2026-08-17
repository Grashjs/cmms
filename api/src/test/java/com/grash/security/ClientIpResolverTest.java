package com.grash.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientIpResolverTest {

    @Mock
    private HttpServletRequest request;

    // ── resolve(): singleIngress mode ──────────────────────────────────

    @Test
    void singleIngress_withXForwardedFor_returnsLastIp() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.41.3.18");

        String result = resolver.resolve(request);

        assertEquals("70.41.3.18", result);
    }

    @Test
    void singleIngress_singleIpInHeader_returnsThatIp() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7");

        String result = resolver.resolve(request);

        assertEquals("198.51.100.7", result);
    }

    @Test
    void singleIngress_noForwardedHeader_returnsRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        String result = resolver.resolve(request);

        assertEquals("10.0.0.1", result);
    }

    @Test
    void singleIngress_blankForwardedHeader_returnsRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");

        String result = resolver.resolve(request);

        assertEquals("10.0.0.1", result);
    }

    @Test
    void singleIngress_unknownForwardedHeader_returnsRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");

        String result = resolver.resolve(request);

        assertEquals("10.0.0.1", result);
    }

    @Test
    void singleIngress_invalidIpInHeader_returnsRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("not-an-ip");

        String result = resolver.resolve(request);

        assertEquals("10.0.0.1", result);
    }

    @Test
    void singleIngress_withSpacesInHeader_returnsTrimmedLastIp() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4,  5.6.7.8 ");

        String result = resolver.resolve(request);

        assertEquals("5.6.7.8", result);
    }

    @Test
    void singleIngress_ipv4MappedIpv6_returnsNormalizedIpv4() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("::ffff:192.168.1.1");

        String result = resolver.resolve(request);

        assertEquals("192.168.1.1", result);
    }

    // ── resolve(): no trusted proxy configured ─────────────────────────

    @Test
    void noTrustedProxy_noForwardedHeader_returnsRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver("", false);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        String result = resolver.resolve(request);

        assertEquals("192.168.1.100", result);
    }

    @Test
    void noTrustedProxy_nullRemoteAddr_returnsUnknown() {
        ClientIpResolver resolver = new ClientIpResolver("", false);
        when(request.getRemoteAddr()).thenReturn(null);

        String result = resolver.resolve(request);

        assertEquals("unknown", result);
    }

    // ── resolve(): trusted proxy CIDR matching ─────────────────────────

    @Test
    void trustedProxyCidr_matchingRemoteAddr_usesForwardedHeader() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8", false);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50");

        String result = resolver.resolve(request);

        assertEquals("203.0.113.50", result);
    }

    @Test
    void trustedProxyCidr_nonMatchingRemoteAddr_ignoresHeader() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8", false);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        String result = resolver.resolve(request);

        assertEquals("192.168.1.1", result);
    }

    @Test
    void trustedProxyExactIp_matchingRemoteAddr_usesForwardedHeader() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.5", false);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7");

        String result = resolver.resolve(request);

        assertEquals("198.51.100.7", result);
    }

    @Test
    void trustedProxyExactIp_nonMatchingRemoteAddr_ignoresHeader() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.5", false);
        when(request.getRemoteAddr()).thenReturn("10.0.0.6");

        String result = resolver.resolve(request);

        assertEquals("10.0.0.6", result);
    }

    @Test
    void multipleTrustedProxies_anyMatch_usesForwardedHeader() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8, 172.16.0.0/12", false);
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        String result = resolver.resolve(request);

        assertEquals("1.2.3.4", result);
    }

    @Test
    void trustedProxyCidr_boundaryIpInsideRange_matches() {
        ClientIpResolver resolver = new ClientIpResolver("192.168.0.0/16", false);
        when(request.getRemoteAddr()).thenReturn("192.168.255.255");
        when(request.getHeader("X-Forwarded-For")).thenReturn("5.5.5.5");

        String result = resolver.resolve(request);

        assertEquals("5.5.5.5", result);
    }

    @Test
    void trustedProxyCidr_boundaryIpOutsideRange_doesNotMatch() {
        ClientIpResolver resolver = new ClientIpResolver("192.168.0.0/16", false);
        when(request.getRemoteAddr()).thenReturn("192.169.0.1");

        String result = resolver.resolve(request);

        assertEquals("192.169.0.1", result);
    }

    @Test
    void trustedProxyIpv6Cidr_matchingAddress_usesForwardedHeader() {
        ClientIpResolver resolver = new ClientIpResolver("fd00::/8", false);
        when(request.getRemoteAddr()).thenReturn("fd00::1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50");

        String result = resolver.resolve(request);

        assertEquals("203.0.113.50", result);
    }

    @Test
    void trustedProxyIpv6_exactMatch_usesForwardedHeader() {
        ClientIpResolver resolver = new ClientIpResolver("::1", false);
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        String result = resolver.resolve(request);

        assertEquals("1.2.3.4", result);
    }

    // ── normalizeIp() ──────────────────────────────────────────────────

    @Test
    void normalizeIpv4MappedIpv6_returnsIpv4() {
        assertEquals("192.168.1.1", ClientIpResolver.normalizeIp("::ffff:192.168.1.1"));
    }

    @Test
    void normalizeIpv4MappedIpv6_loopback_returnsIpv4() {
        assertEquals("127.0.0.1", ClientIpResolver.normalizeIp("::ffff:127.0.0.1"));
    }

    @Test
    void normalizeNull_returnsNull() {
        assertNull(ClientIpResolver.normalizeIp(null));
    }

    @Test
    void normalizePlainIpv4_returnsSameValue() {
        assertEquals("10.0.0.1", ClientIpResolver.normalizeIp("10.0.0.1"));
    }

    @Test
    void normalizeIpv6_returnsSameValue() {
        assertEquals("::1", ClientIpResolver.normalizeIp("::1"));
    }

    @Test
    void normalizeIpv6FullForm_returnsSameValue() {
        assertEquals("2001:0db8:0000:0000:0000:0000:0000:0001",
                ClientIpResolver.normalizeIp("2001:0db8:0000:0000:0000:0000:0000:0001"));
    }

    @Test
    void normalizeEmptyAfterPrefix_returnsOriginal() {
        assertEquals("::ffff:", ClientIpResolver.normalizeIp("::ffff:"));
    }

    @Test
    void normalizeIpv6WithFfffPrefix_notIpv4_returnsOriginal() {
        assertEquals("::ffff:abcd", ClientIpResolver.normalizeIp("::ffff:abcd"));
    }

    // ── TrustedNetwork: partial CIDR prefix lengths ────────────────────

    @Test
    void trustedProxyCidr_slash24_matchingSubnet() {
        ClientIpResolver resolver = new ClientIpResolver("10.10.10.0/24", false);
        when(request.getRemoteAddr()).thenReturn("10.10.10.128");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1");

        String result = resolver.resolve(request);

        assertEquals("1.1.1.1", result);
    }

    @Test
    void trustedProxyCidr_slash24_differentSubnet() {
        ClientIpResolver resolver = new ClientIpResolver("10.10.10.0/24", false);
        when(request.getRemoteAddr()).thenReturn("10.10.11.1");

        String result = resolver.resolve(request);

        assertEquals("10.10.11.1", result);
    }

    // ── TrustedNetwork: IPv4 vs IPv6 mismatch ──────────────────────────

    @Test
    void trustedProxyIpv4Network_ipv6Candidate_doesNotMatch() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8", false);
        when(request.getRemoteAddr()).thenReturn("::1");

        String result = resolver.resolve(request);

        assertEquals("::1", result);
    }

    // ── Edge cases with malformed inputs ───────────────────────────────

    @Test
    void trustedProxy_invalidCidr_notParsed_doesNotMatch() {
        ClientIpResolver resolver = new ClientIpResolver("not-a-valid-ip/24", false);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String result = resolver.resolve(request);

        assertEquals("10.0.0.1", result);
    }

    @Test
    void singleIngress_lastEntryInvalid_returnsRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, invalid-ip");

        String result = resolver.resolve(request);

        assertEquals("10.0.0.1", result);
    }

    @Test
    void trustedProxyCidr_partialBits_matchesCorrectly() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12", false);
        when(request.getRemoteAddr()).thenReturn("172.31.255.255");
        when(request.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9");

        String result = resolver.resolve(request);

        assertEquals("9.9.9.9", result);
    }

    @Test
    void trustedProxyCidr_partialBits_outsideRange() {
        ClientIpResolver resolver = new ClientIpResolver("172.16.0.0/12", false);
        when(request.getRemoteAddr()).thenReturn("172.32.0.1");

        String result = resolver.resolve(request);

        assertEquals("172.32.0.1", result);
    }

    // ── Single IP not in trusted list, no ingress ──────────────────────

    @Test
    void noTrustedProxy_withForwardedHeader_ignoresHeader() {
        ClientIpResolver resolver = new ClientIpResolver("", false);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        String result = resolver.resolve(request);

        assertEquals("192.168.1.100", result);
    }

    // ── singleIngress with valid last IP after comma chain ──────────────

    @Test
    void singleIngress_threeHopChain_returnsLastValidIp() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.1.1.1, 2.2.2.2, 3.3.3.3");

        String result = resolver.resolve(request);

        assertEquals("3.3.3.3", result);
    }

    @Test
    void singleIngress_localhostLast_returnsRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver("", true);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 127.0.0.1");

        String result = resolver.resolve(request);

        assertEquals("127.0.0.1", result);
    }
}
