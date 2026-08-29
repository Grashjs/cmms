package com.grash.repository;

import com.grash.model.Asset;
import com.grash.service.triage.AssetSearchRow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * The one read the triage matcher needs, kept out of {@link AssetRepository} on purpose.
 *
 * <p>AssetRepository is upstream's file and is merged on every sync; a query of ours living in it
 * is a conflict every time. Spring Data is happy with two repository interfaces over the same
 * entity, so this costs nothing and keeps the sync cheap - see the Upstream section of CLAUDE.md.
 */
public interface AssetTriageRepository extends Repository<Asset, Long> {

    /**
     * Every asset of a company, as scoreable text. Archived assets are left out: proposing an
     * asset that was taken out of service is worse than proposing nothing, because it looks like
     * a correct answer.
     *
     * <p>One row per asset even when an asset has several files or teams - the joins here are all
     * to-one, so no duplicates and no {@code DISTINCT} needed.
     */
    @Query("SELECT new com.grash.service.triage.AssetSearchRow("
            + "a.id, a.name, a.description, a.area, a.model, a.serialNumber, a.customId, a.barCode, "
            + "l.name, pl.name, pa.name) "
            + "FROM Asset a "
            + "LEFT JOIN a.location l "
            + "LEFT JOIN l.parentLocation pl "
            + "LEFT JOIN a.parentAsset pa "
            + "WHERE a.company.id = :companyId AND a.archived = false")
    List<AssetSearchRow> findSearchRows(@Param("companyId") Long companyId);
}
