package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
public class AssetMiniDTO {
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Custom identifier")
    private String customId;

    @Schema(description = "Parent asset ID")
    private Long parentId;

    @Schema(description = "Location ID")
    private Long locationId;

}
