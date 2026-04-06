package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Relationship data identifying a related resource by type and ID")
public class RelationshipData {
    @Schema(description = "Resource type of the related entity")
    private String type;
    @Schema(description = "Unique identifier of the related entity")
    private String id;
}
