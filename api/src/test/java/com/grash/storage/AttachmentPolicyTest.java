package com.grash.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AttachmentPolicyTest {
    @ParameterizedTest
    @ValueSource(strings = {"<!doctype html><script>document.title='test'</script>",
            "<svg xmlns='http://www.w3.org/2000/svg' onload='document.title=1'/>",
            "%PDF-1.7\n<script>document.title='test'</script>", "GIF89a<script>document.title=1</script>",
            "\u0089PNG\r\n\u001a\n<html>test</html>", "document.title='test'", "unrecognized content", ""})
    void activeUnknownAndTruncatedContentNeverDisplaysInline(String content) {
        assertThat(AttachmentPolicy.verifiedImageType(content.getBytes(StandardCharsets.UTF_8))).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpeg", "gif"})
    void decodesRasterImagesWithoutNeedingNameOrMime(String format) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), format, output);
        assertThat(AttachmentPolicy.verifiedImageType(output.toByteArray())).isEqualTo("image/" + format);
        // A trailer cannot turn an image into HTML: delivery still uses the decoded type plus nosniff/sandbox.
        output.write("<script>document.title='test'</script>".getBytes(StandardCharsets.UTF_8));
        assertThat(AttachmentPolicy.verifiedImageType(output.toByteArray())).isEqualTo("image/" + format);
    }

    @Test
    void rejectsOversizedImagesBeforeDecoding() {
        assertThat(AttachmentPolicy.verifiedImageType(new byte[AttachmentPolicy.MAX_IMAGE_BYTES + 1])).isNull();
    }

    @Test
    void decodesWebp() throws Exception {
        try (var input = getClass().getResourceAsStream("/attachments/raster.webp")) {
            assertThat(AttachmentPolicy.verifiedImageType(input.readAllBytes())).isEqualTo("image/webp");
        }
    }

    @Test
    void sanitizesHeaderAndPathCharacters() {
        String name = AttachmentPolicy.safeFilename("../../dir\\report\";\r\nX-Test: yes\u202e.pdf");
        assertThat(name).isEqualTo("report____X-Test_ yes_.pdf");
        assertThat(AttachmentPolicy.disposition(name, false)).isEqualTo("attachment; filename=\"" + name + "\"");
        assertThat(AttachmentPolicy.safeFilename(".. ")).isEqualTo("attachment");
        assertThat(AttachmentPolicy.safeFilename(null)).isEqualTo("attachment");
        assertThat(AttachmentPolicy.safeFilename("CON.txt")).isEqualTo("_CON.txt");
        assertThat(AttachmentPolicy.safeStorageMetadata(AttachmentPolicy.DOWNLOAD_TYPE,
                "attachment; filename=\"..\"", AttachmentPolicy.CACHE_CONTROL, null)).isFalse();
        assertThat(AttachmentPolicy.safeFilename("a".repeat(400))).hasSize(150);
    }
}
