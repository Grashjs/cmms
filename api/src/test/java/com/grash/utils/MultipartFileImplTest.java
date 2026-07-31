package com.grash.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultipartFileImplTest {

    @TempDir
    Path tempDir;

    private static final byte[] CONTENT = {1, 2, 3, 4, 5};

    @Test
    void getName_returnsName() {
        MultipartFileImpl file = new MultipartFileImpl(CONTENT, "report.csv");
        assertEquals("report.csv", file.getName());
    }

    @Test
    void getOriginalFilename_returnsName() {
        MultipartFileImpl file = new MultipartFileImpl(CONTENT, "report.csv");
        assertEquals("report.csv", file.getOriginalFilename());
    }

    @Test
    void getContentType_csv_returnsTextCsv() {
        assertEquals("text/csv", new MultipartFileImpl(CONTENT, "data.csv").getContentType());
    }

    @Test
    void getContentType_pdf_returnsApplicationPdf() {
        assertEquals("application/pdf", new MultipartFileImpl(CONTENT, "file.pdf").getContentType());
    }

    @Test
    void getContentType_otherExtension_returnsNull() {
        assertNull(new MultipartFileImpl(CONTENT, "image.png").getContentType());
    }

    @Test
    void getContentType_noExtension_returnsNull() {
        assertNull(new MultipartFileImpl(CONTENT, "data").getContentType());
    }

    @Test
    void getContentType_uppercaseExtension_handledCaseInsensitively() {
        assertEquals("text/csv", new MultipartFileImpl(CONTENT, "DATA.CSV").getContentType());
        assertEquals("application/pdf", new MultipartFileImpl(CONTENT, "REPORT.PDF").getContentType());
    }

    @Test
    void isEmpty_nonEmptyContent_returnsFalse() {
        assertFalse(new MultipartFileImpl(CONTENT, "f").isEmpty());
    }

    @Test
    void isEmpty_emptyContent_returnsTrue() {
        assertTrue(new MultipartFileImpl(new byte[0], "f").isEmpty());
    }

    @Test
    void isEmpty_nullContent_returnsTrue() {
        assertTrue(new MultipartFileImpl(null, "f").isEmpty());
    }

    @Test
    void getSize_returnsContentLength() {
        assertEquals(CONTENT.length, new MultipartFileImpl(CONTENT, "f").getSize());
    }

    @Test
    void getSize_nullContent_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new MultipartFileImpl(null, "f").getSize());
    }

    @Test
    void getBytes_returnsContent() throws IOException {
        assertArrayEquals(CONTENT, new MultipartFileImpl(CONTENT, "f").getBytes());
    }

    @Test
    void getInputStream_readsContent() throws IOException {
        try (InputStream in = new MultipartFileImpl(CONTENT, "f").getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }
    }

    @Test
    void transferTo_writesContentToDestination() throws IOException {
        Path dest = tempDir.resolve("uploaded.bin");
        MultipartFileImpl file = new MultipartFileImpl(CONTENT, "uploaded.bin");
        file.transferTo(dest.toFile());
        assertArrayEquals(CONTENT, Files.readAllBytes(dest));
    }
}
