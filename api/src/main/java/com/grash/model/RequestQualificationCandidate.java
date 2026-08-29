package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One asset the matcher considers plausible for a request, with the reason it thinks so.
 *
 * <p>Not a CompanyAudit: a candidate is meaningless outside its qualification, is reached only
 * through it, and inherits its tenancy from it. Giving it its own company column would add a
 * second place for the two to disagree.
 *
 * <p>matchedTerms is the honest part of the design. A bare score tells an admin nothing and gets
 * ignored; "Heizung, Keller, HZ-2201" tells them in one glance whether the match is real or a
 * coincidence, and that is what makes a suggestion cheap enough to check.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "One asset proposed by triage, with the terms that produced the match")
public class RequestQualificationCandidate {

    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qualification_id", nullable = false)
    private RequestQualification qualification;

    @Schema(description = "The proposed asset")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Schema(description = "Match score between 0 and 1; only comparable within the same engine")
    @Column(nullable = false)
    private double score;

    @Schema(description = "Position in the ranking, 0 is the best candidate")
    @Column(nullable = false)
    private int ordinal;

    @Schema(description = "The words that produced the score, comma separated")
    @Column(length = 500)
    private String matchedTerms;

    public RequestQualificationCandidate(Asset asset, double score, int ordinal, String matchedTerms) {
        this.asset = asset;
        this.score = score;
        this.ordinal = ordinal;
        this.matchedTerms = matchedTerms;
    }
}
