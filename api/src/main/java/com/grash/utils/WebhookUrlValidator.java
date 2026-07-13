package com.grash.utils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

public class WebhookUrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private WebhookUrlValidator() {
    }

    public static void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new WebhookUrlValidationException("Webhook URL must not be empty");
        }

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new WebhookUrlValidationException("Webhook URL is not a valid URI");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new WebhookUrlValidationException(
                    "Webhook URL must use http or https scheme");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new WebhookUrlValidationException("Webhook URL must have a valid host");
        }

        // Block IPv6 bracket notation that might bypass checks (e.g. [::1])
        if (host.startsWith("[") || host.endsWith("]")) {
            throw new WebhookUrlValidationException("Webhook URL host must not use bracket notation");
        }

        // Resolve DNS and validate the resolved IP addresses
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new WebhookUrlValidationException("Webhook URL host could not be resolved");
        }

        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new WebhookUrlValidationException(
                        "Webhook URL must not point to a private, loopback, link-local, or reserved address");
            }
        }

        // Block common cloud metadata hostnames (DNS-rebinding protection)
        String lowerHost = host.toLowerCase();
        if (lowerHost.equals("169.254.169.254") || lowerHost.equals("metadata.google.internal")
                || lowerHost.endsWith(".metadata.google.internal")
                || lowerHost.equals("instance-data")) {
            throw new WebhookUrlValidationException(
                    "Webhook URL must not point to a cloud metadata endpoint");
        }
    }

    private static boolean isBlockedAddress(InetAddress address) {
        // Loopback: 127.0.0.0/8, ::1
        if (address.isLoopbackAddress()) {
            return true;
        }

        // Any local address
        if (address.isAnyLocalAddress()) {
            return true;
        }

        // Link-local: 169.254.0.0/16, fe80::/10
        if (address.isLinkLocalAddress()) {
            return true;
        }

        // Site-local (deprecated but still blocked): 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        if (address.isSiteLocalAddress()) {
            return true;
        }

        // Multicast
        if (address.isMulticastAddress()) {
            return true;
        }

        // Unspecified (0.0.0.0, ::)
        byte[] addr = address.getAddress();

        // IPv4 checks
        if (addr.length == 4) {
            int b0 = addr[0] & 0xFF;
            int b1 = addr[1] & 0xFF;

            // 0.0.0.0/8 — unspecified
            if (b0 == 0) return true;

            // 100.64.0.0/10 — Carrier-grade NAT (RFC 6598)
            if (b0 == 100 && (b1 & 0xC0) == 64) return true;

            // 127.0.0.0/8 — loopback (already caught by isLoopbackAddress, but explicit)
            if (b0 == 127) return true;

            // 169.254.0.0/16 — link-local
            if (b0 == 169 && b1 == 254) return true;

            // 192.0.0.0/24 — IETF protocol assignments
            if (b0 == 192 && b1 == 0) return true;

            // 192.0.2.0/24 — documentation (TEST-NET-1)
            if (b0 == 192 && b1 == 0 && (addr[2] & 0xFF) == 2) return true;

            // 198.51.100.0/24 — documentation (TEST-NET-2)
            if (b0 == 198 && b1 == 51 && (addr[2] & 0xFF) == 100) return true;

            // 203.0.113.0/24 — documentation (TEST-NET-3)
            if (b0 == 203 && b1 == 0 && (addr[2] & 0xFF) == 113) return true;

            // 224.0.0.0/4 — multicast (already caught, but explicit)
            if (b0 >= 224 && b0 <= 239) return true;

            // 240.0.0.0/4 — reserved
            if (b0 >= 240) return true;
        }

        // IPv6 checks
        if (addr.length == 16) {
            int b0 = addr[0] & 0xFF;

            // :: (unspecified)
            boolean allZero = true;
            for (byte b : addr) {
                if (b != 0) { allZero = false; break; }
            }
            if (allZero) return true;

            // ::1 (loopback)
            if (addr[15] == 1 && allZeroExceptLast(addr)) return true;

            // fe80::/10 — link-local
            if (b0 == 0xFE && (addr[1] & 0xC0) == 0x80) return true;

            // fc00::/7 — unique local address
            if ((b0 & 0xFE) == 0xFC) return true;

            // fec0::/10 — site-local (deprecated)
            if (b0 == 0xFE && (addr[1] & 0xC0) == 0xC0) return true;
        }

        return false;
    }

    private static boolean allZeroExceptLast(byte[] addr) {
        for (int i = 0; i < addr.length - 1; i++) {
            if (addr[i] != 0) return false;
        }
        return true;
    }

    public static class WebhookUrlValidationException extends RuntimeException {
        public WebhookUrlValidationException(String message) {
            super(message);
        }
    }
}
