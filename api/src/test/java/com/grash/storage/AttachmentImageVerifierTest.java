package com.grash.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AttachmentImageVerifierTest {
    @Test
    void identicalBytesReuseTheDecodedVerdictWithoutDependingOnAnObjectPath() {
        var verifier = new AttachmentImageVerifier();
        byte[] bytes = "image bytes".getBytes(StandardCharsets.UTF_8);
        try (var policy = mockStatic(AttachmentPolicy.class)) {
            policy.when(() -> AttachmentPolicy.verifiedImageType(any())).thenReturn("image/png");
            assertThat(verifier.verifiedType(bytes)).isEqualTo("image/png");
            assertThat(verifier.verifiedType(bytes.clone())).isEqualTo("image/png");
            policy.verify(() -> AttachmentPolicy.verifiedImageType(any()), times(1));
        }
    }

    @Test
    void replacingBytesRevalidatesAndAlsoCachesRefusedContent() {
        var verifier = new AttachmentImageVerifier();
        byte[] original = "image bytes".getBytes(StandardCharsets.UTF_8);
        byte[] replacement = "<html>test</html>".getBytes(StandardCharsets.UTF_8);
        try (var policy = mockStatic(AttachmentPolicy.class)) {
            policy.when(() -> AttachmentPolicy.verifiedImageType(original)).thenReturn("image/png");
            assertThat(verifier.verifiedType(original)).isEqualTo("image/png");
            assertThat(verifier.verifiedType(replacement)).isNull();
            assertThat(verifier.verifiedType(replacement.clone())).isNull();
            policy.verify(() -> AttachmentPolicy.verifiedImageType(any()), times(2));
        }
    }

    @Test
    void cacheIsBoundedAndEvictedContentMustBeDecodedAgain() {
        var verifier = new AttachmentImageVerifier();
        byte[] first = "first".getBytes(StandardCharsets.UTF_8);
        try (var policy = mockStatic(AttachmentPolicy.class)) {
            verifier.verifiedType(first);
            for (int i = 0; i < 256; i++) verifier.verifiedType(Integer.toString(i).getBytes(StandardCharsets.UTF_8));
            verifier.verifiedType(first);
            policy.verify(() -> AttachmentPolicy.verifiedImageType(any()), times(258));
        }
    }

    @Test
    void oversizedAndEmptyFilesNeverReachTheDecoderOrCache() {
        var verifier = new AttachmentImageVerifier();
        try (var policy = mockStatic(AttachmentPolicy.class)) {
            assertThat(verifier.verifiedType(new byte[0])).isNull();
            assertThat(verifier.verifiedType(new byte[AttachmentPolicy.MAX_IMAGE_BYTES + 1])).isNull();
            policy.verifyNoInteractions();
        }
    }
}
