package com.grash.dto;

import com.grash.model.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying preventive maintenance details in API responses")
public class PreventiveMaintenanceShowDTO extends WorkOrderBaseShowDTO {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Schedule information")
    private Schedule schedule;

    @Schema(description = "Custom identifier")
    private String customId;

    @Schema(description = "Next work order date")
    private Date nextWorkOrderDate;
}
