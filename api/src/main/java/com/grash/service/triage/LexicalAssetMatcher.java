package com.grash.service.triage;

import com.grash.model.Request;
import com.grash.repository.AssetTriageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the asset a request is about by comparing words - no model, no API call, no network.
 *
 * <p>This is stage 1 on purpose, and the reason is not cost. Asset matching is the bottleneck of
 * the whole triage idea, and how well it can possibly work is decided by the asset master data,
 * not by the matcher. A lexical matcher measures that directly: if it finds the right asset most
 * of the time, the data is good and a better matcher will do better still; if it does not, the
 * names and locations in the asset list are too thin, and an embedding model would only hide that
 * behind plausible-looking wrong answers. Run this first, look at the numbers, then decide.
 *
 * <h2>How a score is built</h2>
 *
 * Each word of the request is matched against the words of each asset, in several fields, and
 * keeps its best hit:
 *
 * <pre>
 *   score_raw = SUM over request words of  MAX over fields ( fieldWeight * similarity * idf )
 *               + IDENTIFIER_BONUS for each identifier found whole in the text
 *   score     = score_raw / (score_raw + SATURATION)
 * </pre>
 *
 * <p><b>fieldWeight</b> says what a hit is worth where it landed - a name is strong evidence, a
 * location narrows things down, a word in a long description is weak.
 *
 * <p><b>Identifiers</b> - serial number, model, custom id, barcode - are not part of that sum at
 * all. They are compared whole against the whole request text, with punctuation removed on both
 * sides, and they are the only thing here with no fuzziness: "AB-1200" and "AB-1201" are two
 * different pumps, and a rule that treats them as a near match turns the most reliable evidence
 * the matcher has into its most confident mistake.
 *
 * <p><b>idf</b> (inverse document frequency, over the assets of that one company) is what makes
 * the ranking useful rather than merely non-random. In a building with fifty light fittings the
 * word "Beleuchtung" identifies nothing and is worth almost zero; "HZ-2201" appears once and is
 * worth a great deal. Without it every request about lighting matches fifty assets equally and
 * the ranking is arbitrary.
 *
 * <p><b>SATURATION</b> squashes the open-ended sum into 0..1 without a denominator that depends on
 * how much the reporter wrote. Dividing by the word count - the obvious normalisation - punishes
 * a careful description and rewards a terse one, which is precisely backwards.
 *
 * <h2>What it deliberately does not do</h2>
 *
 * No stemming and no dictionary. German compounds are handled by the containment rule in
 * {@link LexicalScorer#similarity}, which covers the cases that actually occur ("Heizungsraum"
 * against "Heizung") at a fraction of the complexity; a stemmer would need a German dictionary in
 * the image for the rest.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LexicalAssetMatcher implements AssetMatcher {

    private final AssetTriageRepository assetTriageRepository;

    /**
     * Controls where the 0..1 scale bends. At 3.0 a single strong, rare name hit lands around 0.6
     * and two of them around 0.75, which is roughly how confident a human reading the same two
     * words would be. Raising it makes the matcher sound more cautious without changing the
     * ranking at all - it is presentation, not accuracy.
     */
    @Value("${triage.asset-match.saturation:3.0}")
    private double saturation;

    /**
     * Candidates below this are not offered. The number is a floor against noise, not a quality
     * bar: one weak word in common is how a matcher ends up proposing the nearest fire
     * extinguisher for a broken door, and an admin who sees that twice stops reading the card.
     */
    @Value("${triage.asset-match.min-score:0.25}")
    private double minScore;

    private static final String ENGINE = "lexical-v1";

    /** Weight per field, for the word-by-word part of the score. */
    private static final double W_NAME = 1.0;
    private static final double W_LOCATION = 0.7;
    private static final double W_AREA = 0.6;
    private static final double W_PARENT_ASSET = 0.5;
    private static final double W_DESCRIPTION = 0.4;

    /**
     * What a serial number, model, custom id or barcode found verbatim in the request text is
     * worth. A flat amount rather than a weight times an inverse document frequency, because an
     * identifier is unique by construction - there is no rarity left to measure - and because it
     * should mean the same thing in a company with twenty assets as in one with two thousand.
     *
     * <p>Set so that an identifier alone clears any sensible threshold on its own: with the
     * default saturation of 3.0 it lands near 0.6, which is the right answer for "the reporter
     * typed the number off the type plate".
     */
    private static final double IDENTIFIER_BONUS = 4.5;

    /**
     * Identifiers shorter than this are ignored. They are compared as substrings of the request
     * text, and a two-character custom id like "A1" occurs inside ordinary German words often
     * enough to make the strongest rule here fire at random.
     */
    private static final int MIN_IDENTIFIER_LENGTH = 4;

    @Override
    public String engineName() {
        return ENGINE;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetMatch> match(Request request, int limit) {
        String text = (request.getTitle() == null ? "" : request.getTitle()) + " "
                + (request.getDescription() == null ? "" : request.getDescription());
        List<String> queryTokens = LexicalScorer.tokenize(text);
        if (queryTokens.isEmpty()) return List.of();
        // Separators removed rather than cut on, so an identifier can be looked for whole
        // regardless of how the reporter spaced or hyphenated it. See LexicalScorer.compact.
        String compactText = LexicalScorer.compact(text);

        List<AssetSearchRow> assets = assetTriageRepository.findSearchRows(request.getCompany().getId());
        if (assets.isEmpty()) return List.of();

        List<ScoredAsset> scoredAssets = new ArrayList<>(assets.size());
        for (AssetSearchRow row : assets) scoredAssets.add(new ScoredAsset(row));

        Map<String, Double> idf = inverseDocumentFrequency(scoredAssets);

        List<AssetMatch> matches = new ArrayList<>();
        for (ScoredAsset asset : scoredAssets) {
            AssetMatch match = score(asset, queryTokens, compactText, idf);
            if (match != null) matches.add(match);
        }
        matches.sort(Comparator.comparingDouble(AssetMatch::score).reversed());
        log.debug("Lexical asset match for request {}: {} of {} assets above {}",
                request.getId(), matches.size(), assets.size(), minScore);
        return matches.size() > limit ? new ArrayList<>(matches.subList(0, limit)) : matches;
    }

    /**
     * How rare each word is among the assets of this company. Computed per call rather than
     * cached: a request arrives at human speed, the asset list of one company fits in memory
     * several times over, and a cache would need invalidating on every asset edit - the cheapest
     * correct thing here is to not have one. If an instance ever grows an asset list where this
     * hurts, the fix is a cache keyed by company with a short time to live, not a schema change.
     */
    private Map<String, Double> inverseDocumentFrequency(List<ScoredAsset> assets) {
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (ScoredAsset asset : assets) {
            for (String token : asset.allTokens) {
                documentFrequency.merge(token, 1, Integer::sum);
            }
        }
        int total = assets.size();
        Map<String, Double> idf = new HashMap<>(documentFrequency.size());
        documentFrequency.forEach((token, frequency) ->
                idf.put(token, Math.log(1.0 + (double) total / frequency)));
        return idf;
    }

    private AssetMatch score(ScoredAsset asset, List<String> queryTokens, String compactText,
                             Map<String, Double> idf) {
        double raw = 0.0;
        // Insertion-ordered and deduplicated: the same word hitting name and location should be
        // named once in the explanation, in the order the reporter wrote it.
        Set<String> matchedTerms = new LinkedHashSet<>();

        // Identifiers first, and whole. An identifier that merely resembles the one in the text is
        // not weak evidence, it is evidence of a different machine: "AB-1200" and "AB-1201" are two
        // pumps. So this is the one place with no fuzziness at all - all of it, or nothing.
        for (Identifier identifier : asset.identifiers) {
            if (compactText.contains(identifier.compact())) {
                raw += IDENTIFIER_BONUS;
                matchedTerms.add(identifier.original());
            }
        }

        for (String queryToken : queryTokens) {
            double best = 0.0;
            String bestTerm = null;

            for (FieldTokens field : asset.fields) {
                for (String assetToken : field.tokens()) {
                    double similarity = LexicalScorer.similarity(queryToken, assetToken);
                    if (similarity == 0.0) continue;
                    double weighted = field.weight() * similarity * idf.getOrDefault(assetToken, 0.0);
                    if (weighted > best) {
                        best = weighted;
                        bestTerm = assetToken;
                    }
                }
            }
            if (best > 0.0) {
                raw += best;
                matchedTerms.add(bestTerm);
            }
        }

        if (raw <= 0.0) return null;
        double score = raw / (raw + saturation);
        if (score < minScore) return null;
        return new AssetMatch(asset.row.id(), score, List.copyOf(matchedTerms));
    }

    /**
     * The text of one asset, prepared once per request rather than once per request word.
     *
     * <p>The identifiers are held apart from the word fields because they are compared in a
     * completely different way - whole, against the whole request text - and because they must not
     * take part in the inverse document frequency either. A serial number appears once by
     * definition, so counting how rare it is measures nothing.
     */
    private static final class ScoredAsset {
        private final AssetSearchRow row;
        private final List<FieldTokens> fields;
        private final List<Identifier> identifiers;
        private final Set<String> allTokens = new LinkedHashSet<>();

        private ScoredAsset(AssetSearchRow row) {
            this.row = row;
            this.fields = List.of(
                    new FieldTokens(W_NAME, LexicalScorer.tokenSet(row.name())),
                    new FieldTokens(W_LOCATION,
                            LexicalScorer.tokenSet(row.locationName(), row.parentLocationName())),
                    new FieldTokens(W_AREA, LexicalScorer.tokenSet(row.area())),
                    new FieldTokens(W_PARENT_ASSET, LexicalScorer.tokenSet(row.parentAssetName())),
                    new FieldTokens(W_DESCRIPTION, LexicalScorer.tokenSet(row.description())));
            for (FieldTokens field : fields) allTokens.addAll(field.tokens());
            this.identifiers = identifiersOf(row);
        }

        private static List<Identifier> identifiersOf(AssetSearchRow row) {
            List<Identifier> result = new ArrayList<>(4);
            for (String value : List.of(
                    nullToEmpty(row.serialNumber()), nullToEmpty(row.model()),
                    nullToEmpty(row.customId()), nullToEmpty(row.barCode()))) {
                String compact = LexicalScorer.compact(value);
                if (compact.length() >= MIN_IDENTIFIER_LENGTH) {
                    result.add(new Identifier(value.trim(), compact));
                }
            }
            return result;
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value;
        }
    }

    /** An identifier as written on the asset, and the form it is compared in. */
    private record Identifier(String original, String compact) {
    }

    private record FieldTokens(double weight, Set<String> tokens) {
    }
}
