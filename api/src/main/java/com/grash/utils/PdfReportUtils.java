package com.grash.utils;

import com.grash.model.File;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ITagWorkerFactory;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.attach.impl.DefaultTagWorkerFactory;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.WebColors;
import com.itextpdf.layout.IPropertyContainer;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.styledxmlparser.node.IElementNode;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

@Slf4j
public final class PdfReportUtils {
    public static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "jpe", "png", "gif", "webp", "bmp",
            "tif", "tiff", "svg", "ico", "avif", "heic", "heif", "jfif");
    private static final int PDF_IMAGE_MAX_DIMENSION_PX = 1600;
    private static final long PDF_IMAGE_MAX_PIXELS = 40L * 1024 * 1024;
    private static final int PDF_IMAGE_MAX_UNOPTIMIZED_BYTES = 512 * 1024;
    private static final byte[] EMPTY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
    private static final List<String> REPORT_FONT_RESOURCES = List.of(
            "/fonts/Inter-Regular.ttf",
            "/fonts/Inter-Medium.ttf",
            "/fonts/Inter-SemiBold.ttf",
            "/fonts/Inter-Bold.ttf");
    private static final FontProvider REPORT_FONT_PROVIDER = createReportFontProvider();

    private PdfReportUtils() {
    }

    public static @Nullable String getImageReportStoragePath(File file) {
        if (file == null || file.getPath() == null) return null;

        String name = file.getName() != null ? file.getName().toLowerCase(Locale.ROOT) : "";
        String extension = name.substring(name.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);

        boolean isImage = IMAGE_EXTENSIONS.contains(extension);

        if (!isImage) {
            return null;
        }
        return file.getPath();
    }

    public static byte[] optimizeImageForPdf(byte[] rawBytes, String fileName) {
        String name = fileName != null ? fileName.toLowerCase(Locale.ROOT) : "";
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        if (!IMAGE_EXTENSIONS.contains(extension) || extension.equals("svg")) return rawBytes;

        if (extension.equals("png") && exceedsPngDimensions(rawBytes)) {
            log.warn("Skipping oversized PNG '{}' ({} bytes) in PDF report to avoid memory exhaustion",
                    fileName, rawBytes.length);
            return EMPTY_PNG;
        }

        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (source == null) return rawBytes;
            int width = source.getWidth();
            int height = source.getHeight();
            if (width <= 0 || height <= 0) return rawBytes;

            boolean hasAlpha = source.getColorModel().hasAlpha();
            if (Math.max(width, height) <= PDF_IMAGE_MAX_DIMENSION_PX
                    && rawBytes.length <= PDF_IMAGE_MAX_UNOPTIMIZED_BYTES) {
                return rawBytes;
            }

            BufferedImage scaled = Math.max(width, height) > PDF_IMAGE_MAX_DIMENSION_PX
                    ? Thumbnails.of(source).size(PDF_IMAGE_MAX_DIMENSION_PX, PDF_IMAGE_MAX_DIMENSION_PX)
                    .asBufferedImage()
                    : source;

            ByteArrayOutputStream optimized = new ByteArrayOutputStream();
            if (hasAlpha) {
                ImageIO.write(scaled, "png", optimized);
            } else {
                BufferedImage rgb = new BufferedImage(scaled.getWidth(), scaled.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = rgb.createGraphics();
                graphics.drawImage(scaled, 0, 0, null);
                graphics.dispose();
                ImageIO.write(rgb, "jpg", optimized);
            }
            byte[] result = optimized.toByteArray();
            if (result.length > 0 && result.length < rawBytes.length) {
                log.debug("Optimized image '{}' for PDF report: {} -> {} bytes", fileName, rawBytes.length,
                        result.length);
                return result;
            }
            return rawBytes;
        } catch (Exception | OutOfMemoryError e) {
            log.warn("Failed to optimize image '{}' ({} bytes) for PDF report", fileName, rawBytes.length, e);
            if (rawBytes.length <= PDF_IMAGE_MAX_UNOPTIMIZED_BYTES) {
                return rawBytes;
            }
            log.warn("Falling back to placeholder image for '{}' ({} bytes) to avoid memory exhaustion",
                    fileName, rawBytes.length);
            return EMPTY_PNG;
        }
    }

    private static boolean exceedsPngDimensions(byte[] png) {
        if (png.length < 24) return false;
        boolean isPngSignature = (png[0] & 0xFF) == 0x89 && png[1] == 'P' && png[2] == 'N' && png[3] == 'G';
        if (!isPngSignature) return false;
        long width =
                ((png[16] & 0xFFL) << 24) | ((png[17] & 0xFFL) << 16) | ((png[18] & 0xFFL) << 8) | (png[19] & 0xFFL);
        long height =
                ((png[20] & 0xFFL) << 24) | ((png[21] & 0xFFL) << 16) | ((png[22] & 0xFFL) << 8) | (png[23] & 0xFFL);
        return width * height > PDF_IMAGE_MAX_PIXELS;
    }

    public static String resolveReportColor(String candidateColor, String fallbackColor) {
        String normalized = resolveCssColor(candidateColor);
        if (normalized == null) normalized = resolveCssColor(fallbackColor);
        return normalized;
    }

    private static FontProvider createReportFontProvider() {
        DefaultFontProvider fontProvider = new DefaultFontProvider(true, false, false);
        for (String fontResource : REPORT_FONT_RESOURCES) {
            try (InputStream fontStream = PdfReportUtils.class.getResourceAsStream(fontResource)) {
                if (fontStream == null) {
                    log.warn("Report font resource {} not found on classpath", fontResource);
                    continue;
                }
                fontProvider.addFont(fontStream.readAllBytes());
            } catch (IOException e) {
                log.warn("Failed to register report font {}", fontResource, e);
            }
        }
        return fontProvider;
    }

    public static @Nullable String resolveCssColor(String candidateColor) {
        if (candidateColor == null || candidateColor.trim().isEmpty()) return null;
        String value = candidateColor.trim().toLowerCase(Locale.ROOT);
        try {
            float[] rgb;
            if (value.charAt(0) == '#') {
                rgb = parseHexColor(value);
            } else if (value.startsWith("rgb")) {
                rgb = parseFunctionalRgbColor(value);
            } else {
                rgb = scaleToByteRange(WebColors.getRGBAColor(value.replaceAll("[\\s_-]+", "")));
            }
            return rgb != null ? toHexColor(rgb) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static @Nullable float[] parseHexColor(String value) {
        String hex = value.substring(1);
        if (hex.length() == 3 || hex.length() == 4) {
            StringBuilder expanded = new StringBuilder();
            for (int i = 0; i < 3; i++) expanded.append(hex.charAt(i)).append(hex.charAt(i));
            hex = expanded.toString();
        } else if (hex.length() == 8) {
            hex = hex.substring(0, 6);
        }
        if (hex.length() != 6) return null;
        for (int i = 0; i < 6; i++) {
            char c = hex.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) return null;
        }
        return scaleToByteRange(WebColors.getRGBAColor('#' + hex));
    }

    private static @Nullable float[] parseFunctionalRgbColor(String value) {
        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        if (open < 0 || close < open) return null;
        List<String> components = new ArrayList<>();
        for (String part : value.substring(open + 1, close).trim().split("[,\\s]+")) {
            if (!part.isEmpty()) components.add(part);
        }
        if (components.size() != 3 && components.size() != 4) return null;
        float[] rgb = new float[3];
        for (int i = 0; i < 3; i++) {
            String component = components.get(i);
            boolean isPercentage = component.endsWith("%");
            if (isPercentage) component = component.substring(0, component.length() - 1);
            float parsed;
            try {
                parsed = Float.parseFloat(component);
            } catch (NumberFormatException e) {
                return null;
            }
            rgb[i] = isPercentage ? parsed * 2.55f : parsed;
        }
        return rgb;
    }

    private static float[] scaleToByteRange(float[] rgba) {
        return new float[]{rgba[0] * 255f, rgba[1] * 255f, rgba[2] * 255f};
    }

    private static String toHexColor(float[] rgb) {
        return String.format(Locale.ROOT, "#%02x%02x%02x", clampChannel(rgb[0]), clampChannel(rgb[1]),
                clampChannel(rgb[2]));
    }

    private static int clampChannel(float component) {
        return Math.max(0, Math.min(255, Math.round(component)));
    }

    public static ConverterProperties createReportConverterProperties(Function<String, byte[]> imageLoader) {
        return new ConverterProperties()
                .setFontProvider(REPORT_FONT_PROVIDER)
                .setTagWorkerFactory(new ITagWorkerFactory() {
                    private final DefaultTagWorkerFactory defaultFactory = new DefaultTagWorkerFactory();

                    @Override
                    public ITagWorker getTagWorker(IElementNode tag, ProcessorContext context) {
                        if ("img".equals(tag.name()) && tag.getAttribute("data-storage-path") != null) {
                            return new DirectImageTagWorker(tag, imageLoader);
                        }
                        try {
                            return defaultFactory.getTagWorker(tag, context);
                        } catch (Exception e) {
                            log.warn("Failed to create tag worker for <{}>: {}", tag.name(), e.getMessage());
                            return null;
                        }
                    }
                });
    }

    /**
     * Builds the layout {@link Image} directly from storage bytes, bypassing pdfHTML's URL
     * resolver entirely. This avoids the base64 round-trip of data URIs and ensures only one
     * copy of the already-optimized bytes exists in memory.
     */
    private static final class DirectImageTagWorker implements ITagWorker {
        private final Image image;

        public DirectImageTagWorker(IElementNode tag, Function<String, byte[]> imageLoader) {
            String path = tag.getAttribute("data-storage-path");
            Image img = null;
            if (path != null && !path.isBlank()) {
                try {
                    byte[] optimized = optimizeImageForPdf(imageLoader.apply(path), path);
                    ImageData imageData = ImageDataFactory.create(optimized);
                    img = new Image(imageData);
                } catch (Exception | OutOfMemoryError e) {
                    log.warn("Failed to embed image '{}' in PDF report", path, e);
                }
            }
            this.image = img;
        }

        @Override
        public void processEnd(IElementNode element, ProcessorContext context) {
        }

        @Override
        public boolean processContent(String content, ProcessorContext context) {
            return true;
        }

        @Override
        public boolean processTagChild(ITagWorker childTagWorker, ProcessorContext context) {
            return false;
        }

        @Override
        public IPropertyContainer getElementResult() {
            return image;
        }
    }
}
