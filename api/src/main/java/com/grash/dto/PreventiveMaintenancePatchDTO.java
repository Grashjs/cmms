package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching preventive maintenance information")
public class PreventiveMaintenancePatchDTO extends WorkOrderBasePatchDTO {
    @Schema(description = "Name")
    private String name;
}
