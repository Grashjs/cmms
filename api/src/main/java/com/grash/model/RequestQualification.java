package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.QualificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * What the triage matcher thinks about one request: a ranked list of assets it might be about.
 *
 * <p>The invariant that makes the whole feature safe is that this entity never writes to the
 * request. The matcher reads, scores and stores an opinion; the request only changes when a
 * human applies one of the candidates, and then through the normal asset field on the normal
 * edit path. A wrong suggestion therefore costs a glance, not a correction.
 *
 * <p>At most one qualification per request is live at a time - the newest. Re-running triage
 * does not overwrite the previous row, it marks that one SUPERSEDED and writes a new one, so
 * "what did it suggest before the photo arrived" stays answerable.
 *
 * <p>Note that this entity is created off the request thread, from RequestTriageListener, where
 * there is no security context at all - portal requests are submitted anonymously.
 * CompanyAudit.beforePersist therefore does nothing and the company has to be set explicitly by
 * the service. Its counterpart afterLoad is equally inert on that thread, which is why the
 * repository methods here take an explicit company id rather than trusting the tenancy aspect.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Triage result for a maintenance request: ranked asset suggestions")
public class RequestQualification extends CompanyAudit {

    @Schema(description = "The request this qualification is about", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Schema(description = "Whether the suggestion is still open, was applied, rejected or superseded")
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private QualificationStatus status = QualificationStatus.PENDING;

    @Schema(description = "Matcher and version that produced the candidates, e.g. lexical-v1")
    @NotNull
    @Column(length = 50, nullable = false)
    private String engine;

    @Schema(description = "User who applied or rejected the suggestion")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Schema(description = "When the suggestion was applied or rejected")
    private Date decidedAt;

    @Schema(description = "The candidate the user actually applied, which need not be the top one")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_asset_id")
    private Asset chosenAsset;

    @Schema(description = "Proposed assets, best first")
    @OneToMany(mappedBy = "qualification", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("ordinal ASC")
    private List<RequestQualificationCandidate> candidates = new ArrayList<>();

    public void addCandidate(RequestQualificationCandidate candidate) {
        candidate.setQualification(this);
        candidates.add(candidate);
    }

    /** A decision has been made and this row is history. */
    public boolean isDecided() {
        return status != QualificationStatus.PENDING;
    }
}
