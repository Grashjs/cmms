package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PreventiveMaintenanceMiniDTO {
    @Schema(description = "Schedule name")
    private String name;

    @Schema(description = "Work Order Title")
    private String title;


    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Custom identifier")
    private String customId;
}
