package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO for patching UI configuration settings")
public class UiConfigurationPatchDTO {

    @Schema(description = "Show requests module")
    private boolean requests;
    
    @Schema(description = "Show locations module")
    private boolean locations;
    
    @Schema(description = "Show meters module")
    private boolean meters;
    
    @Schema(description = "Show vendors and customers module")
    private boolean vendorsAndCustomers;
}
