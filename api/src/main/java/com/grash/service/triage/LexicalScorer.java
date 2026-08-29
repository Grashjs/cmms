package com.grash.service.triage;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Text handling for {@link LexicalAssetMatcher}: how a sentence becomes comparable words, and how
 * two words are compared.
 *
 * <p>Separate from the matcher and free of Spring so it can be tested as what it is - a pile of
 * string decisions, each of which is arguable and all of which decide how good the suggestions
 * are. Everything here is deliberately German-first, because that is what the requests are
 * written in.
 */
final class LexicalScorer {

    private LexicalScorer() {
    }

    /**
     * Words that carry no information about which asset is meant. Kept short on purpose: an
     * over-long list starts eating words that matter in facility management ("dach", "boden"),
     * and a word that appears in every asset is already suppressed by the inverse document
     * frequency in the matcher. This list only has to remove words that appear in requests but
     * never in asset data, where idf cannot help.
     */
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            // German articles, pronouns, prepositions, conjunctions
            "der", "die", "das", "den", "dem", "des", "ein", "eine", "einen", "einem", "eines",
            "und", "oder", "aber", "auch", "noch", "nur", "schon", "sehr", "wieder", "immer",
            "ist", "sind", "war", "waren", "wird", "werden", "wurde", "wurden", "hat", "haben",
            "kann", "koennen", "muss", "muessen", "soll", "sollen", "sich", "man", "wir", "ich",
            "sie", "ihr", "uns", "mir", "mich", "es", "nicht", "kein", "keine", "keinen",
            "im", "in", "am", "an", "auf", "bei", "mit", "von", "vom", "zu", "zum", "zur",
            "fuer", "ueber", "unter", "vor", "nach", "aus", "durch", "gegen", "ohne", "um",
            "als", "wie", "wenn", "dass", "weil", "damit", "denn", "dann", "hier", "dort", "da",
            "bitte", "danke", "hallo", "guten", "tag", "mfg",
            // English, for portals that are used in English
            "the", "a", "an", "is", "are", "was", "were", "and", "or", "of", "in", "on", "at",
            "for", "to", "with", "not", "no", "this", "that", "it", "there", "please"));

    /**
     * Lowercases, expands German umlauts, strips remaining diacritics and cuts on anything that
     * is not a letter or digit.
     *
     * <p>Umlauts are expanded rather than stripped ("ue", not "u"), because that is how people
     * type them when the keyboard or the label does not have them, and it is how they appear in
     * imported asset data. Stripping would map "Tür" to "tur" and never match a "Tuer" in the
     * asset list.
     */
    static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = text.toLowerCase()
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        List<String> tokens = new ArrayList<>();
        for (String raw : normalized.split("[^a-z0-9]+")) {
            if (isUseful(raw)) tokens.add(raw);
        }
        return tokens;
    }

    /** Same as {@link #tokenize} but without repetitions, for the asset side where counts are noise. */
    static Set<String> tokenSet(String... texts) {
        Set<String> set = new LinkedHashSet<>();
        for (String text : texts) set.addAll(tokenize(text));
        return set;
    }

    /**
     * The same normalisation as {@link #tokenize}, but with every separator removed instead of
     * cut on: "AB-1200" and "AB 1200" and "ab1200" all become {@code ab1200}.
     *
     * <p>This exists because identifiers must not be tokenized. Splitting "AB-1200" into "ab" and
     * "1200" makes the fragment "ab" a match against "AB-1201", "AB-1300" and every other machine
     * in the series - so the field that should be the most reliable evidence the matcher has
     * becomes the one that produces its most confident wrong answers. Comparing the whole
     * identifier against the whole request text instead is both stricter and more forgiving in the
     * right places: it insists on all of it, and it does not care how the reporter punctuated it.
     */
    static String compact(String text) {
        if (text == null) return "";
        String normalized = text.toLowerCase()
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    /**
     * Anything of two characters or more that is not a stopword. The usual default is three,
     * and it is wrong here: in German facility management the short words carry a lot - "og",
     * "ug", "kg", "wc", "b1" - and cutting at three throws away exactly the words that identify
     * a floor or a room. The cost of keeping them is borne by the inverse document frequency in
     * the matcher, which makes a short word that appears everywhere worth almost nothing.
     */
    private static boolean isUseful(String token) {
        if (token.length() < 2) return false;
        if (STOPWORDS.contains(token)) return false;
        return true;
    }

    /**
     * How alike two words are, on a 0..1 scale, with the cases that matter in German named
     * explicitly rather than left to a general string metric:
     *
     * <ul>
     *   <li><b>1.0 - identical.</b></li>
     *   <li><b>0.9 - one contains the other</b>, both at least four characters. This is the
     *       compound case, and it is the single most valuable rule here: "Heizungsraum" contains
     *       "Heizung", "Lueftungsanlage" contains "Lueftung". A trigram metric scores those
     *       around 0.5 and would lose them below any sensible threshold.</li>
     *   <li><b>trigram overlap</b> otherwise, and only if it is convincing. This catches inflected
     *       forms and small typos; below the threshold it returns zero rather than a small number,
     *       because a weak match summed over many words is how a matcher starts recommending
     *       nonsense with confidence.</li>
     * </ul>
     */
    static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        if (a.length() >= 4 && b.length() >= 4 && (a.contains(b) || b.contains(a))) return 0.9;
        double jaccard = trigramJaccard(a, b);
        return jaccard >= TRIGRAM_THRESHOLD ? jaccard : 0.0;
    }

    private static final double TRIGRAM_THRESHOLD = 0.45;

    private static double trigramJaccard(String a, String b) {
        Set<String> left = trigrams(a);
        Set<String> right = trigrams(b);
        if (left.isEmpty() || right.isEmpty()) return 0.0;
        int shared = 0;
        for (String trigram : left) if (right.contains(trigram)) shared++;
        return (double) shared / (left.size() + right.size() - shared);
    }

    private static Set<String> trigrams(String word) {
        String padded = "  " + word + " ";
        Set<String> result = new HashSet<>();
        for (int i = 0; i + 3 <= padded.length(); i++) result.add(padded.substring(i, i + 3));
        return result;
    }
}
