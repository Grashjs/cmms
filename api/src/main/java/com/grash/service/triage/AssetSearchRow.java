package com.grash.service.triage;

/**
 * The searchable text of one asset, flattened into a single row.
 *
 * <p>A projection rather than the {@code Asset} entity, because the matcher scores every asset of
 * a company on every incoming request. Loading entities would pull each asset's location,
 * category, image and deprecation along one query at a time; this is one query for the whole
 * company and holds only what is scored.
 *
 * <p>The fields are grouped by how much a hit in them means, which is what
 * {@link LexicalAssetMatcher} weights them by: an exact serial number is near-proof, a name is
 * strong evidence, a location narrows things down, and a word from a long description is weak.
 */
public record AssetSearchRow(
        Long id,
        String name,
        String description,
        String area,
        String model,
        String serialNumber,
        String customId,
        String barCode,
        String locationName,
        String parentLocationName,
        String parentAssetName) {
}
