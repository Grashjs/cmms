package com.grash.storage;

import com.grash.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class AttachmentLinksTest {
    private final String key = "attachment-test-secret-32-characters-long";
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"), ZoneOffset.UTC);
    private final AttachmentLinks links = new AttachmentLinks(key, "https://cmms.test/api/", "MINIO:bucket", clock);

    @Test
    void createsApiCapabilitiesAndRejectsTampering() {
        String url = links.sign("company_1/report.html", "../report.html", 10);
        assertThat(url).startsWith("https://cmms.test/api/files/content/").doesNotContain("X-Amz", "storage.googleapis.com");
        String token = token(url);
        assertThat(links.verify(token).path()).isEqualTo("company_1/report.html");
        String[] parts = token.split("\\.");
        String altered = new String(Base64.getUrlDecoder().decode(parts[0])).replace("company_1", "company_2");
        String forged = Base64.getUrlEncoder().withoutPadding().encodeToString(altered.getBytes()) + "." + parts[1];
        assertThatThrownBy(() -> links.verify(forged)).isInstanceOf(CustomException.class);
        for (String invalid : new String[]{"", "invalid", token + ".extra", "a.b", "x".repeat(6001)}) {
            assertThatThrownBy(() -> links.verify(invalid)).isInstanceOf(CustomException.class);
        }
    }

    @Test
    void expiryRotationAndBucketChangesInvalidateLinks() {
        String token = token(links.sign("object", "object", 10));
        assertThatThrownBy(() -> new AttachmentLinks(key, "https://cmms.test", "MINIO:bucket",
                Clock.offset(clock, java.time.Duration.ofMinutes(10))).verify(token)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> new AttachmentLinks(key + "rotated", "https://cmms.test", "MINIO:bucket", clock).verify(token)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> new AttachmentLinks(key, "https://cmms.test", "GCP:bucket", clock).verify(token)).isInstanceOf(CustomException.class);
    }

    private String token(String url) { return url.substring(url.lastIndexOf('/') + 1); }
}
