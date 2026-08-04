package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.SavedViewEntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A named list configuration — filters, sorting and column layout — that a user saved and
 * can come back to or share.
 * <p>
 * The payload stays opaque to the backend on purpose. {@code criteria} is a serialised
 * {@link com.grash.advancedsearch.SearchCriteria} and {@code columnLayout} is the frontend
 * table state; both are stored and returned verbatim. The alternative — modelling filter
 * fields and columns as rows — buys nothing here, because every consumer of a view is the
 * same list page that produced it, and it would turn every new filter widget into a schema
 * migration.
 * <p>
 * Two consequences worth knowing:
 * <ul>
 *   <li>The stored criteria are <b>not</b> a security boundary. They are re-scoped through
 *       the owning service's {@code getSearchCriteria(user, ...)} on every use, exactly like
 *       criteria arriving from the client, so a hand-edited view cannot widen access.</li>
 *   <li>A view can go stale: renaming a filterable field leaves views referencing the old
 *       name. Applying such a view yields an error from the search endpoint rather than
 *       silently wrong data, which is the failure mode we want.</li>
 * </ul>
 */
@Entity
@Data
@NoArgsConstructor
@Schema(description = "A named, reusable list configuration (filters, sorting, columns)")
public class SavedView extends CompanyAudit {

    @Schema(description = "Name shown in the view picker", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    @Schema(description = "The list page this view belongs to", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SavedViewEntityType entityType;

    @Schema(description = "Serialised SearchCriteria: filter fields, sort field and direction, page size")
    @Column(columnDefinition = "TEXT")
    private String criteria;

    @Schema(description = "Serialised table layout: column order, visibility, width and pinning")
    @Column(columnDefinition = "TEXT")
    private String columnLayout;

    @Schema(description = "Whether every user of the company sees this view")
    private boolean shared = false;

    @Schema(description = "The user who created the view; only the owner and company admins may change it")
    @ManyToOne(fetch = FetchType.LAZY)
    private User owner;

    /**
     * Ownership check for edit and delete. Company admins are included so a shared view does
     * not become unmaintainable when its author leaves — the same reasoning as
     * {@code WorkOrder.canBeEditedBy}, which also lets a broader permission override
     * authorship.
     */
    public boolean canBeEditedBy(User user) {
        return (owner != null && owner.getId().equals(user.getId()))
                || user.isOwnsCompany();
    }

    /**
     * Read access: own views always, shared views for everyone in the company. The company
     * check itself is enforced by {@link CompanyAudit} on load, so this only decides between
     * private and shared.
     */
    public boolean isVisibleTo(User user) {
        return shared || (owner != null && owner.getId().equals(user.getId()));
    }
}
