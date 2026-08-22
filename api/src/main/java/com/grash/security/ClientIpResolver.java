package com.grash.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the real client IP address for rate-limiting / audit purposes.
 *
 * <p><b>Why only {@code X-Forwarded-For}, and why the LAST hop:</b><br>
 * This app is deployed behind Koyeb's Edge Network, which is the sole public
 * ingress to the Service (TLS is terminated at the edge; the request then
 * travels over Koyeb's internal, encrypted service mesh to this instance).
 * Koyeb's documented behavior is to always <em>append</em> the address it
 * connected from to the end of {@code X-Forwarded-For} — it never strips or
 * validates whatever the client already sent. That means:
 * <ul>
 *   <li>Everything except the LAST entry in {@code X-Forwarded-For} is
 *       attacker-controlled and MUST NOT be trusted.</li>
 *   <li>The LAST entry is the one Koyeb itself certifies as valid.</li>
 *   <li>Headers such as {@code X-Real-IP}, {@code CF-Connecting-IP} or
 *       {@code True-Client-IP} are NOT set by Koyeb. Trusting them would
 *       just hand an attacker extra unvalidated inputs, so they are
 *       intentionally not consulted here. If a CDN such as Cloudflare is
 *       ever placed in front of Koyeb, revisit this.</li>
 * </ul>
 *
 * <p><b>Trusted-proxy check:</b> Because Koyeb is the only way to reach this
 * Service (there is no direct network path to the container), the "trusted
 * proxy" concept effectively reduces to "trust Koyeb's edge". By default,
 * this class is conservative and only trusts {@code X-Forwarded-For} when
 * {@code security.trusted-proxies.single-ingress=true} is set, OR when
 * {@code remoteAddr} matches an explicitly configured CIDR in
 * {@code security.trusted-proxies.ips}. If neither is configured,
 * {@code remoteAddr} is always returned as-is (fail closed).
 *
 * <p>Before enabling {@code single-ingress}, confirm in your deployed Koyeb
 * environment (e.g. via a temporary log statement) what
 * {@code req.getRemoteAddr()} actually resolves to, so you understand what
 * you are implicitly trusting.
 */
@Component
public class ClientIpResolver {

    private static final String FORWARDED_HEADER = "X-Forwarded-For";

    private final List<TrustedNetwork> trustedNetworks;
    private final boolean singleIngress;

    public ClientIpResolver(
            @Value("${security.trusted-proxies.ips:}") String trustedProxyIps,
            @Value("${security.trusted-proxies.single-ingress:true}") boolean singleIngress) {
        this.trustedNetworks = parseTrustedNetworks(trustedProxyIps);
        this.singleIngress = singleIngress;
    }

    /**
     * Resolve the client IP address for rate-limiting / audit purposes.
     *
     * <p>When the request is considered to come through a trusted proxy
     * (either {@code single-ingress} mode, or {@code remoteAddr} matching a
     * configured trusted CIDR), the LAST address in {@code X-Forwarded-For}
     * is used, since that is the one appended by the trusted proxy itself.
     * Otherwise {@code remoteAddr} is returned directly, preventing header
     * spoofing.</p>
     */
    public String resolve(HttpServletRequest req) {
        String remoteAddr = normalizeIp(req.getRemoteAddr());

        if (singleIngress || isTrustedProxy(remoteAddr)) {
            String ip = extractLastIp(req.getHeader(FORWARDED_HEADER));
            if (ip != null) {
                return ip;
            }
        }

        return remoteAddr != null ? remoteAddr : "unknown";
    }

    // ── internal helpers ────────────────────────────────────────────────

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || trustedNetworks.isEmpty()) {
            return false;
        }
        for (TrustedNetwork network : trustedNetworks) {
            if (network.contains(remoteAddr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract the LAST IP in a (possibly attacker-prefixed) X-Forwarded-For
     * chain, since that is the entry the trusted proxy itself appended.
     * The value is validated as a parseable IP address before being
     * returned, to avoid feeding garbage into logs / rate-limit keys.
     */
    private static String extractLastIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()
                || "unknown".equalsIgnoreCase(headerValue.trim())) {
            return null;
        }
        String[] parts = headerValue.split(",");
        String last = parts[parts.length - 1].trim();
        if (last.isEmpty() || !isValidIp(last)) {
            return null;
        }
        return normalizeIp(last);
    }

    private static boolean isValidIp(String ip) {
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Normalize an IP string: if it is an IPv4-mapped IPv6 address
     * (e.g. {@code ::ffff:192.168.1.1}) return the embedded IPv4 form.
     */
    static String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }
        if (ip.startsWith("::ffff:") && ip.length() > 7) {
            String maybeIpv4 = ip.substring(7);
            if (looksLikeIpv4(maybeIpv4)) {
                return maybeIpv4;
            }
        }
        return ip;
    }

    private static boolean looksLikeIpv4(String s) {
        String[] parts = s.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
            int val = Integer.parseInt(part);
            if (val < 0 || val > 255) {
                return false;
            }
        }
        return true;
    }

    // ── trusted-network parsing & matching ──────────────────────────────

    private static List<TrustedNetwork> parseTrustedNetworks(String csv) {
        List<TrustedNetwork> result = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return result;
        }
        for (String entry : csv.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains("/")) {
                String[] parts = trimmed.split("/", 2);
                String addr = parts[0].trim();
                int prefixLen = Integer.parseInt(parts[1].trim());
                result.add(new TrustedNetwork(addr, prefixLen));
            } else {
                result.add(new TrustedNetwork(trimmed, -1));
            }
        }
        return result;
    }

    /**
     * Represents a single trusted network (IP address or CIDR range).
     */
    static class TrustedNetwork {
        private final byte[] addressBytes;
        private final int prefixLength;          // -1 = exact match
        private final String originalForm;

        TrustedNetwork(String addressOrCidr, int prefixLength) {
            this.originalForm = addressOrCidr;
            this.prefixLength = prefixLength;
            this.addressBytes = parseAddress(addressOrCidr);
        }

        boolean contains(String candidateIp) {
            byte[] candidateBytes = parseAddress(candidateIp);
            if (candidateBytes == null || addressBytes == null) {
                return false;
            }
            if (addressBytes.length != candidateBytes.length) {
                // Length mismatch (e.g. IPv4 vs IPv6) – not comparable.
                return false;
            }
            if (prefixLength < 0) {
                return java.util.Arrays.equals(addressBytes, candidateBytes);
            }
            return matchesCidr(addressBytes, candidateBytes, prefixLength);
        }

        private static byte[] parseAddress(String ip) {
            try {
                return InetAddress.getByName(ip).getAddress();
            } catch (UnknownHostException e) {
                return null;
            }
        }

        private static boolean matchesCidr(byte[] network, byte[] candidate, int prefixBits) {
            int fullBytes = prefixBits / 8;
            int remainingBits = prefixBits % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (network[i] != candidate[i]) {
                    return false;
                }
            }
            if (remainingBits > 0 && fullBytes < network.length) {
                int mask = 0xFF << (8 - remainingBits);
                return (network[fullBytes] & mask) == (candidate[fullBytes] & mask);
            }
            return true;
        }

        @Override
        public String toString() {
            return prefixLength >= 0
                    ? originalForm + "/" + prefixLength
                    : originalForm;
        }
    }
}
