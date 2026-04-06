package com.grash.dto;

import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "Work order base mini DTO with essential work order details")
public class WorkOrderBaseMiniDTO {
    @Schema(description = "Work order unique identifier")
    private Long id;
    @Schema(description = "Work order title")
    private String title;
    @Schema(description = "Work order due date")
    private Date dueDate;
    @Schema(description = "Work order creation timestamp")
    private Instant createdAt;
    @Schema(description = "Work order priority level")
    private Priority priority;
    @Schema(description = "Work order current status")
    private Status status;
    @Schema(description = "Custom work order identifier")
    private String customId;
}
