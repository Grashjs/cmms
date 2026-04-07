package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying multiparts details in API responses")
public class MultiPartsShowDTO extends AuditShowDTO {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Collection of parts")
    private Collection<PartMiniDTO> parts;
}
