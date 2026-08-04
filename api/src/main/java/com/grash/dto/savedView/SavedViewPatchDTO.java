package com.grash.dto.savedView;

import com.fasterxml.jackson.databind.JsonNode;
import com.grash.advancedsearch.SearchCriteria;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Partial update. Every field is nullable and null means "leave as is" — hence
 * {@link Boolean} rather than {@code boolean} for {@code shared}, so "do not touch the
 * sharing flag" and "unshare" stay distinguishable.
 * <p>
 * {@code entityType} is intentionally absent: a view belongs to the list page that created
 * it, and moving one across pages would carry filters and columns that do not exist there.
 */
@Data
@Schema(description = "DTO for updating a saved list view; null fields are left unchanged")
public class SavedViewPatchDTO {

    @Schema(description = "New name")
    private String name;

    @Schema(description = "Replacement filters and sorting")
    private SearchCriteria criteria;

    @Schema(description = "Replacement table layout", type = "object")
    private JsonNode columnLayout;

    @Schema(description = "Share with the company, or stop sharing")
    private Boolean shared;
}
