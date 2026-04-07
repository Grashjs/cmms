package com.grash.dto.workOrder;

import com.grash.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "Minimal work order data transfer object with essential fields")
public class WorkOrderMiniDTO {
    @Schema(description = "Unique identifier of the work order")
    private Long id;
    @Schema(description = "Title of the work order")
    private String title;
    @Schema(description = "Due date of the work order")
    private Date dueDate;
    @Schema(description = "Custom ID for the work order")
    private String customId;
    @Schema(description = "Current status of the work order")
    private Status status;
    @Schema(description = "Creation date of the work order")
    private Date createdAt;

}
