package com.grash.dto.savedView;

import com.fasterxml.jackson.databind.JsonNode;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.dto.AuditShowDTO;
import com.grash.dto.UserMiniDTO;
import com.grash.model.enums.SavedViewEntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO for displaying a saved list view")
public class SavedViewShowDTO extends AuditShowDTO {

    @Schema(description = "Name shown in the view picker")
    private String name;

    @Schema(description = "The list page this view belongs to")
    private SavedViewEntityType entityType;

    @Schema(description = "Filters, sort field and direction, page size")
    private SearchCriteria criteria;

    @Schema(description = "Table layout as understood by the frontend", type = "object")
    private JsonNode columnLayout;

    @Schema(description = "Whether the whole company sees this view")
    private boolean shared;

    @Schema(description = "The user who created the view")
    private UserMiniDTO owner;

    /**
     * Resolved server-side rather than derived in the frontend from owner id and role: the
     * rule (owner or company owner) lives in {@link com.grash.model.SavedView#canBeEditedBy},
     * and duplicating it in TypeScript is how the two drift apart.
     */
    @Schema(description = "Whether the requesting user may rename, overwrite or delete this view")
    private boolean editable;
}
