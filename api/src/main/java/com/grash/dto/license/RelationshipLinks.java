package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "HATEOAS links for a relationship resource")
public class RelationshipLinks {
    @Schema(description = "Map of link names to URLs for related resources")
    private Map<String, String> links;
}
