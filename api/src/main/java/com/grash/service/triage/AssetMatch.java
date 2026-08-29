package com.grash.service.triage;

import java.util.List;

/**
 * One asset a matcher considers plausible, with the reason.
 *
 * @param assetId      the proposed asset
 * @param score        0..1, higher is better. Only comparable against scores from the same
 *                     {@link AssetMatcher#engineName()} - a lexical 0.6 and an embedding 0.6 mean
 *                     different things, which is why the engine name is stored alongside every
 *                     result.
 * @param matchedTerms the words that produced the score, best first. Shown to the user as the
 *                     explanation; a suggestion nobody can check is a suggestion nobody uses.
 */
public record AssetMatch(Long assetId, double score, List<String> matchedTerms) {
}
