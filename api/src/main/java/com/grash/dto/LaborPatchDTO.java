package com.grash.dto;

import com.grash.model.OwnUser;
import com.grash.model.TimeCategory;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching labor information")
public class LaborPatchDTO {
    @Schema(description = "Assigned user", implementation = IdDTO.class)
    private OwnUser assignedTo;

    @Schema(description = "Whether to include this in total time calculation")
    private boolean includeToTotalTime;

    @Schema(description = "Hourly rate")
    private long hourlyRate;

    @Schema(description = "Duration in minutes")
    private int duration;

    @Schema(description = "Start timestamp")
    private Date startedAt;
    
    @Schema(description = "Time category", implementation = IdDTO.class)
    private TimeCategory timeCategory;
}
