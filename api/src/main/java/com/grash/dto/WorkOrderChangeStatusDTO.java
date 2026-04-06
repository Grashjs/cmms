package com.grash.dto;

import com.grash.model.File;
import com.grash.model.OwnUser;
import com.grash.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "DTO for changing the status of a work order")
public class WorkOrderChangeStatusDTO {
    @Schema(description = "The new status to set for the work order")
    private Status status;
    @Schema(description = "Signature captured upon status change to completed")
    private String signature;
    @Schema(description = "Feedback or notes provided during the status change")
    private String feedback;
}