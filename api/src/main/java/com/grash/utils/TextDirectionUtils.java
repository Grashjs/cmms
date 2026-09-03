package com.grash.utils;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;

/**
 * Converts logical (storage) order Unicode text into visual order suitable for
 * engines that do not perform their own BiDi (bidirectional) reordering or Arabic
 * glyph joining (e.g. open-source iText HTML-to-PDF).
 *
 * <p>The methods are stateless and therefore thread-safe.
 */
public final class TextDirectionUtils {

    private TextDirectionUtils() {
    }

    /**
     * Returns {@code true} if the text contains characters from a right-to-left
     * script (Arabic or Hebrew). Latin/CJK text is left untouched.
     */
    public static boolean isRtlText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            if (script == Character.UnicodeScript.ARABIC
                    || script == Character.UnicodeScript.HEBREW) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    /**
     * Shapes Arabic letters and reorders the text to visual order so that a
     * left-to-right rendering engine displays right-to-left scripts correctly.
     * For pure LTR text this is a no-op and returns the input unchanged.
     */
    public static String toVisualOrder(String logical) {
        if (!isRtlText(logical)) {
            return logical;
        }
        String shaped = shapeArabic(logical);
        String visual = reorderToVisual(shaped);
        return visual == null ? logical : visual;
    }

    /**
     * Walks an HTML document and rewrites every text run (content outside of
     * tags) to visual order while preserving tags, attributes, comments and
     * other markup. Content inside {@code <style>} and {@code <script>} is left
     * untouched. Safe to call on any document because pure LTR text is a no-op.
     */
    public static String transformHtmlText(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        StringBuilder out = new StringBuilder(html.length() + 16);
        StringBuilder text = new StringBuilder();
        int i = 0;
        int n = html.length();
        int skipDepth = 0;
        String pendingTag = null;

        while (i < n) {
            char c = html.charAt(i);
            if (c == '<') {
                Tag tag = readTag(html, i);
                if (tag == null) {
                    text.append(c);
                    i++;
                    continue;
                }
                flushText(text, out);
                String name = tag.name;
                if ("style".equalsIgnoreCase(name) || "script".equalsIgnoreCase(name)) {
                    skipDepth = ("/".equals(tag.prefix) || tag.selfClosing) ? 0 : 1;
                }
                out.append(html, tag.start, tag.end);
                if (skipDepth == 0 && (tag.comment)) {
                    int ce = html.indexOf("-->", tag.end);
                    if (ce < 0) {
                        out.append(html, tag.end, n);
                        break;
                    }
                    out.append(html, tag.end, ce + 3);
                    i = ce + 3;
                    continue;
                }
                if (skipDepth == 1) {
                    int close = findClosingTag(html, tag.end, name);
                    if (close < 0) {
                        out.append(html, tag.end, n);
                        break;
                    }
                    out.append(html, tag.end, close);
                    out.append(html, close, close + name.length() + 3);
                    skipDepth = 0;
                    i = close + name.length() + 3;
                    continue;
                }
                i = tag.end;
                continue;
            }
            text.append(c);
            i++;
        }
        flushText(text, out);
        return out.toString();
    }

    private static void flushText(StringBuilder text, StringBuilder out) {
        if (text.length() > 0) {
            out.append(toVisualOrder(text.toString()));
            text.setLength(0);
        }
    }

    private static int findClosingTag(String html, int from, String name) {
        String close = "</" + name;
        int idx = html.length();
        int lower = html.toLowerCase(java.util.Locale.ROOT).indexOf(close, from);
        if (lower >= 0) {
            idx = lower;
        }
        return idx == html.length() ? -1 : idx;
    }

    private static Tag readTag(String html, int start) {
        int n = html.length();
        int i = start + 1;
        if (i < n && html.charAt(i) == '!') {
            if (html.startsWith("<!--", start)) {
                return new Tag("", "", start, start + 4, true, true);
            }
            int end = html.indexOf('>', i);
            return end < 0 ? null : new Tag("", "", start, end + 1, false, false);
        }
        StringBuilder name = new StringBuilder();
        String prefix = "";
        if (i < n && html.charAt(i) == '/') {
            prefix = "/";
            i++;
        }
        while (i < n && (Character.isLetterOrDigit(html.charAt(i)) || html.charAt(i) == '-')) {
            name.append(html.charAt(i));
            i++;
        }
        if (name.length() == 0) {
            return null;
        }
        boolean selfClosing = false;
        boolean inQuote = false;
        char quote = 0;
        while (i < n) {
            char c = html.charAt(i);
            if (inQuote) {
                if (c == quote) {
                    inQuote = false;
                }
            } else if (c == '"' || c == '\'') {
                inQuote = true;
                quote = c;
            } else if (c == '>') {
                return new Tag(name.toString(), prefix, start, i + 1, selfClosing, false);
            } else if (c == '/' && i + 1 < n && html.charAt(i + 1) == '>') {
                selfClosing = true;
                i += 2;
                return new Tag(name.toString(), prefix, start, i, true, false);
            }
            i++;
        }
        return null;
    }

    private static final class Tag {
        final String name;
        final String prefix;
        final int start;
        final int end;
        final boolean selfClosing;
        final boolean comment;

        Tag(String name, String prefix, int start, int end, boolean selfClosing, boolean comment) {
            this.name = name;
            this.prefix = prefix;
            this.start = start;
            this.end = end;
            this.selfClosing = selfClosing;
            this.comment = comment;
        }
    }

    private static String shapeArabic(String logical) {
        try {
            ArabicShaping shaping = new ArabicShaping(
                    ArabicShaping.LETTERS_SHAPE
                            | ArabicShaping.TEXT_DIRECTION_LOGICAL
                            | ArabicShaping.LENGTH_GROW_SHRINK);
            return shaping.shape(logical);
        } catch (Exception e) {
            return logical;
        }
    }

    private static String reorderToVisual(String shaped) {
        try {
            Bidi bidi = new Bidi(shaped, Bidi.LTR);
            if (bidi.isLeftToRight()) {
                return shaped;
            }
            return bidi.writeReordered(Bidi.REORDER_DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }
}
