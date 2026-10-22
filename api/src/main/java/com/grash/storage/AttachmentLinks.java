package com.grash.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;

/** Short-lived capabilities for the protected API, never for a raw storage endpoint. */
@Service
public class AttachmentLinks {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final ObjectMapper mapper = new ObjectMapper();
    private final byte[] key;
    private final String apiUrl;
    private final String scope;
    private final Clock clock;

    @Autowired
    public AttachmentLinks(@Value("${storage.delivery.signing-key:${security.jwt.token.secret-key}}") String key,
                           @Value("${api.host}") String apiUrl,
                           @Value("${storage.type}") String provider,
                           @Value("${storage.minio.bucket:}") String minioBucket,
                           @Value("${storage.gcp.bucket-name:}") String gcpBucket) {
        this(key, apiUrl, provider + ":" + ("GCP".equalsIgnoreCase(provider) ? gcpBucket : minioBucket), Clock.systemUTC());
    }

    AttachmentLinks(String key, String apiUrl, String scope, Clock clock) {
        if (key == null || key.length() < 32) throw new IllegalArgumentException("Attachment signing key must contain at least 32 characters");
        this.key = key.getBytes(StandardCharsets.UTF_8);
        this.apiUrl = apiUrl.replaceAll("/+$", "");
        this.scope = scope;
        this.clock = clock;
    }

    public String sign(String path, String filename, long minutes) {
        if (path == null || path.isBlank() || path.length() > 2048 || minutes < 1 || minutes > 1440) {
            throw new IllegalArgumentException("Invalid attachment capability");
        }
        try {
            var claim = new Claim(path, AttachmentPolicy.safeFilename(filename), clock.instant().getEpochSecond() + minutes * 60, scope);
            String payload = ENCODER.encodeToString(mapper.writeValueAsBytes(claim));
            return apiUrl + "/files/content/" + payload + "." + ENCODER.encodeToString(mac(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign attachment", e);
        }
    }

    public Claim verify(String token) {
        try {
            if (token == null || token.length() > 6000) throw new IllegalArgumentException();
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2 || !MessageDigest.isEqual(mac(parts[0]), DECODER.decode(parts[1]))) {
                throw new IllegalArgumentException();
            }
            Claim claim = mapper.readValue(DECODER.decode(parts[0]), Claim.class);
            if (!scope.equals(claim.scope()) || claim.expires() <= clock.instant().getEpochSecond()
                    || claim.path() == null || claim.path().isBlank() || claim.path().length() > 2048) {
                throw new IllegalArgumentException();
            }
            return claim;
        } catch (Exception invalid) {
            // Do not log the bearer token, object path or signature.
            throw new CustomException("Invalid or expired attachment link", HttpStatus.FORBIDDEN);
        }
    }

    private byte[] mac(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(("atlas-attachment-v1:" + payload).getBytes(StandardCharsets.UTF_8));
    }

    public record Claim(String path, String filename, long expires, String scope) { }
}
