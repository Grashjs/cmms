package com.grash.storage;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Reuse decoded verdicts for identical bytes, never for a mutable object path or declared MIME type. */
@Component
public class AttachmentImageVerifier {
    private static final int MAX_ENTRIES = 256;
    private final Map<String, Optional<String>> verifiedTypes = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Optional<String>> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    // Serializing lookup/decoding also prevents concurrent requests from decoding the same image twice.
    public synchronized String verifiedType(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > AttachmentPolicy.MAX_IMAGE_BYTES) return null;
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            return verifiedTypes.computeIfAbsent(digest,
                    ignored -> Optional.ofNullable(AttachmentPolicy.verifiedImageType(bytes))).orElse(null);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required", impossible);
        }
    }
}
