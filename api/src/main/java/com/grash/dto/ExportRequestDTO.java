package com.grash.dto;

import com.grash.advancedsearch.SearchCriteria;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * What to export: which rows and which columns.
 * <p>
 * Both fields are optional and both default to "everything", so an empty body reproduces the
 * behaviour of the older {@code GET /export/<entity>} endpoints. That is what makes this a
 * superset rather than a replacement, and why those endpoints stay.
 */
@Data
@Schema(description = "Row filter and column selection for an export")
public class ExportRequestDTO {

    @Schema(description = "The same criteria the list page posts to /<entity>/search. Null exports every row the user may see.")
    private SearchCriteria criteria;

    /**
     * Column keys from the entity's registry, in the order they should appear. Unknown keys are
     * rejected rather than skipped — a report missing a column silently is worse than one that
     * fails to build.
     */
    @Schema(description = "Column keys in output order. Null or empty exports the default column set.")
    private List<String> columns;
}
