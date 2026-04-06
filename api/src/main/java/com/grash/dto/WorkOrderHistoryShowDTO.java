package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Work order history display DTO with audit information")
public class WorkOrderHistoryShowDTO extends AuditShowDTO {

    @Schema(description = "Work order history entry name")
    private String name;

    @Schema(description = "User associated with this work order history entry")
    private UserMiniDTO user;

}
