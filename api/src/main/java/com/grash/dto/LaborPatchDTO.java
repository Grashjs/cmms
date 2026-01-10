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
public class LaborPatchDTO {
    @Schema(implementation = IdDTO.class)
    private OwnUser assignedTo;

    private boolean includeToTotalTime;

    private long hourlyRate;

    private int duration;

    private Date startedAt;
    @Schema(implementation = IdDTO.class)
    private TimeCategory timeCategory;
}
