package com.grash.dto.savedView;

import com.fasterxml.jackson.databind.JsonNode;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.model.enums.SavedViewEntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO for creating a saved list view")
public class SavedViewPostDTO {

    @Schema(description = "Name shown in the view picker")
    @NotNull
    private String name;

    @Schema(description = "The list page this view belongs to")
    @NotNull
    private SavedViewEntityType entityType;

    @Schema(description = "Filters, sort field and direction, page size — as posted to /<entity>/search")
    private SearchCriteria criteria;

    /**
     * Deliberately untyped: this is the frontend's table state (column order, visibility,
     * width, pinning) and the backend has no use for its shape. Typing it here would mean
     * changing the API every time a table gains a display option.
     */
    @Schema(description = "Table layout as understood by the frontend", type = "object")
    private JsonNode columnLayout;

    @Schema(description = "Make the view visible to the whole company")
    private boolean shared = false;
}
