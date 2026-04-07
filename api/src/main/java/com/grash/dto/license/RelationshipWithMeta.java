package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Relationship containing both links and metadata (e.g., count of related resources)")
public class RelationshipWithMeta {
    @Schema(description = "HATEOAS links to related resources")
    private Map<String, String> links;
    @Schema(description = "Metadata about the relationship, such as resource counts")
    private Map<String, Integer> meta;
}
