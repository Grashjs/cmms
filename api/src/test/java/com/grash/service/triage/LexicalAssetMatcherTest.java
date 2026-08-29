package com.grash.service.triage;

import com.grash.model.Company;
import com.grash.model.Request;
import com.grash.repository.AssetTriageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The behaviour of the matcher, expressed as the cases it was built for.
 *
 * <p>These are not coverage tests. Each one is a claim about German maintenance requests that the
 * scoring rules were chosen to satisfy, and if a future change to the weights breaks one of them,
 * the suggestions got worse in a way that is easy to miss by looking at a single example.
 */
@ExtendWith(MockitoExtension.class)
class LexicalAssetMatcherTest {

    @Mock
    private AssetTriageRepository assetTriageRepository;

    @InjectMocks
    private LexicalAssetMatcher matcher;

    private static final long COMPANY_ID = 7L;

    /**
     * The threshold is off for most tests, and that is deliberate rather than convenient.
     * Scores depend on inverse document frequency, which depends on how many assets the company
     * has - in a fixture with three of them every word looks common and every score comes out
     * low, in a real instance with two hundred the same match scores twice as high. Asserting
     * against the production threshold from a three-row fixture would therefore be measuring the
     * fixture. These tests assert on ranking and on what is found at all; the one test that is
     * about the threshold sets it itself.
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(matcher, "saturation", 3.0);
        ReflectionTestUtils.setField(matcher, "minScore", 0.0);
    }

    private Request request(String title, String description) {
        Request request = new Request();
        request.setId(1L);
        request.setTitle(title);
        request.setDescription(description);
        Company company = new Company();
        company.setId(COMPANY_ID);
        request.setCompany(company);
        return request;
    }

    private AssetSearchRow asset(long id, String name, String location) {
        return new AssetSearchRow(id, name, null, null, null, null, null, null, location, null, null);
    }

    private void withAssets(AssetSearchRow... rows) {
        when(assetTriageRepository.findSearchRows(COMPANY_ID)).thenReturn(List.of(rows));
    }

    @Nested
    @DisplayName("German compounds")
    class Compounds {

        /**
         * The single most important case. Reporters write where they are, not what the asset is
         * called: "Heizungsraum" for a thing named "Heizung". A trigram metric alone scores that
         * around 0.5 and drops it; the containment rule is what keeps it.
         */
        @Test
        void findsAnAssetWhoseNameIsPartOfACompoundInTheRequest() {
            withAssets(asset(10, "Heizung", "Keller"),
                    asset(11, "Aufzug", "Treppenhaus"));

            List<AssetMatch> matches = matcher.match(
                    request("Im Heizungsraum tropft es", null), 3);

            assertFalse(matches.isEmpty(), "the compound should still find the heating");
            assertEquals(10L, matches.get(0).assetId());
        }

        @Test
        void reportsTheWordThatProducedTheMatch() {
            withAssets(asset(10, "Heizung", "Keller"));

            List<AssetMatch> matches = matcher.match(request("Heizung defekt", null), 3);

            assertTrue(matches.get(0).matchedTerms().contains("heizung"),
                    "the explanation has to name the word, otherwise nobody can check the suggestion");
        }
    }

    @Nested
    @DisplayName("Ranking")
    class Ranking {

        /**
         * Two assets share the common word, one also matches the location. Without inverse
         * document frequency the shared word would dominate and the ranking would be arbitrary.
         */
        @Test
        void prefersTheAssetWhoseLocationAlsoMatches() {
            withAssets(asset(10, "Beleuchtung", "Tiefgarage"),
                    asset(11, "Beleuchtung", "Empfang"),
                    asset(12, "Beleuchtung", "Lager"));

            List<AssetMatch> matches = matcher.match(
                    request("Beleuchtung in der Tiefgarage flackert", null), 3);

            assertEquals(10L, matches.get(0).assetId());
            assertTrue(matches.get(0).score() > matches.get(1).score(),
                    "the location hit has to separate them, not just order them by chance");
        }

        /**
         * A serial number in the text is not a coincidence, and it has to beat a merely plausible
         * name. This is the case where a photographed type plate pays off.
         */
        @Test
        void anIdentifierBeatsANameMatch() {
            withAssets(new AssetSearchRow(10L, "Lueftungsanlage", null, null, null, "HZ-2201",
                            null, null, "Dach", null, null),
                    asset(11, "Lueftung Buero", "1. OG"));

            List<AssetMatch> matches = matcher.match(
                    request("Lueftung laut, Typenschild HZ-2201", null), 3);

            assertEquals(10L, matches.get(0).assetId());
        }

        /**
         * The identifier fields are matched on equality only. Two assets from the same series
         * differ by one character, and a fuzzy hit there would turn the strongest signal the
         * matcher has into its worst.
         */
        @Test
        void aNearMissOnAnIdentifierIsNotAMatch() {
            withAssets(new AssetSearchRow(10L, "Pumpe A", null, null, null, "AB-1200",
                    null, null, null, null, null));

            List<AssetMatch> matches = matcher.match(request("Geraet AB-1201 steht", null), 3);

            assertTrue(matches.isEmpty(), "AB-1201 is a different machine, not a weak match");
        }
    }

    @Nested
    @DisplayName("Saying nothing")
    class Silence {

        @Test
        void returnsNothingWhenNoWordIsShared() {
            withAssets(asset(10, "Heizung", "Keller"));

            assertTrue(matcher.match(request("Kaffeemaschine defekt", null), 3).isEmpty());
        }

        /**
         * A request made entirely of filler produces no tokens at all. The matcher must not fall
         * back to matching everything - and it must not read the asset list either, which is the
         * expensive half of the work.
         */
        @Test
        void doesNoWorkAtAllWhenTheRequestIsOnlyStopwords() {
            List<AssetMatch> matches = matcher.match(request("Das ist nicht", null), 3);

            assertTrue(matches.isEmpty());
            verifyNoInteractions(assetTriageRepository);
        }

        @Test
        void returnsNothingWhenTheCompanyHasNoAssets() {
            withAssets();

            assertTrue(matcher.match(request("Heizung defekt", null), 3).isEmpty());
        }

        /**
         * A single unremarkable word in common is not a suggestion. This is the rule that keeps
         * the card credible: an admin who is shown the nearest fire extinguisher for a broken door
         * stops reading the card, and then the good suggestions are lost with the bad ones.
         */
        @Test
        void doesNotOfferAMatchThatRestsOnOneCommonWord() {
            ReflectionTestUtils.setField(matcher, "minScore", 0.25);
            withAssets(asset(10, "Beleuchtung", "Lager"));

            assertTrue(matcher.match(request("Beleuchtung flackert", null), 3).isEmpty());
        }
    }

    @Nested
    @DisplayName("Scale")
    class Scale {

        /**
         * A long, careful description must not score lower than a terse one for the same asset.
         * Normalising by the number of words written - the obvious way to get a 0..1 scale -
         * would invert exactly that, and punish the reporters who help most.
         */
        @Test
        void aLongerDescriptionDoesNotWeakenTheMatch() {
            withAssets(asset(10, "Heizung", "Keller"));

            double terse = matcher.match(request("Heizung Keller", null), 3).get(0).score();
            double verbose = matcher.match(request("Heizung Keller",
                    "Seit gestern Abend ist es im ganzen Gebaeude spuerbar kuehler geworden "
                            + "und niemand konnte bisher sagen woran das liegt"), 3).get(0).score();

            assertTrue(verbose >= terse,
                    "writing more about the same problem must not make the suggestion weaker");
        }

        @Test
        void honoursTheCandidateLimit() {
            withAssets(asset(10, "Heizung Nord", "Keller"),
                    asset(11, "Heizung Sued", "Keller"),
                    asset(12, "Heizung Ost", "Keller"),
                    asset(13, "Heizung West", "Keller"));

            assertEquals(2, matcher.match(request("Heizung im Keller", null), 2).size());
        }
    }
}
