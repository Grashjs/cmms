package com.grash.storage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** The browser representation is decided from bytes, never from object metadata or a filename. */
public final class AttachmentPolicy {
    public static final String DOWNLOAD_TYPE = "application/octet-stream";
    public static final String CSP = "sandbox; default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'";
    public static final String CACHE_CONTROL = "private, no-store";
    public static final int MAX_IMAGE_BYTES = 16 * 1024 * 1024;
    private static final long MAX_PIXELS = 32_000_000;
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "png", "image/png", "jpeg", "image/jpeg", "gif", "image/gif", "webp", "image/webp");

    private AttachmentPolicy() { }

    /** A strict ASCII filename also works in S3 metadata and old mobile download clients. */
    public static String safeFilename(String name) {
        if (name == null) return "attachment";
        String basename = name.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1);
        basename = basename.replaceAll("[^a-zA-Z0-9._ -]", "_")
                .replaceAll("^[. ]+|[. ]+$", "");
        if (basename.isBlank()) return "attachment";
        if (basename.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?")) basename = "_" + basename;
        return basename.substring(0, Math.min(basename.length(), 150));
    }

    public static String disposition(String name, boolean inline) {
        return (inline ? "inline" : "attachment") + "; filename=\"" + safeFilename(name) + "\"";
    }

    public static Map<String, String> storageHeaders(String name) {
        return Map.of("Content-Type", DOWNLOAD_TYPE, "Content-Disposition", disposition(name, false),
                "Cache-Control", CACHE_CONTROL);
    }

    public static boolean safeStorageMetadata(String type, String disposition, String cache, String encoding) {
        if (!DOWNLOAD_TYPE.equals(type) || !CACHE_CONTROL.equals(cache) || encoding != null
                || disposition == null || !disposition.matches("attachment; filename=\"[a-zA-Z0-9._ -]{1,150}\"")) return false;
        String filename = disposition.substring("attachment; filename=\"".length(), disposition.length() - 1);
        return filename.equals(safeFilename(filename));
    }

    /** Oversized, malformed, unsupported and active documents (including PDFs) are downloads. */
    public static String verifiedImageType(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) return null;
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                String type = IMAGE_TYPES.get(reader.getFormatName().toLowerCase(Locale.ROOT));
                if (type == null) return null;
                reader.setInput(input, false, true);
                AtomicBoolean warning = new AtomicBoolean();
                reader.addIIOReadWarningListener((source, message) -> warning.set(true));
                int frames = reader.getNumImages(true);
                if (frames < 1 || frames > 256) return null;
                long totalPixels = 0;
                for (int frame = 0; frame < frames; frame++) {
                    int width = reader.getWidth(frame), height = reader.getHeight(frame);
                    totalPixels += (long) width * height;
                    if (width <= 0 || height <= 0 || totalPixels > MAX_PIXELS) return null;
                    var image = reader.read(frame);
                    if (image == null || warning.get()) return null;
                    image.flush();
                }
                return type;
            } finally {
                reader.dispose();
            }
        } catch (Exception invalidImage) {
            return null;
        }
    }
}
