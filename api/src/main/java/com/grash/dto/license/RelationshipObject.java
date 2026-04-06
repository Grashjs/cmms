package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Relationship object containing links and data to a related resource")
public class RelationshipObject {
    @Schema(description = "HATEOAS links to the related resource")
    private Map<String, String> links;
    @Schema(description = "Data identifying the related resource")
    private RelationshipData data;
}
