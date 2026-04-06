package com.grash.dto;

import com.grash.dto.requestPortal.RequestPortalMiniDTO;
import com.grash.dto.workOrder.WorkOrderMiniDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Request DTO for displaying maintenance request details in API responses")
public class RequestShowDTO extends WorkOrderBaseShowDTO {
    @Schema(description = "Whether the request has been cancelled")
    private boolean cancelled;

    @Schema(description = "Reason provided for cancelling the request")
    private String cancellationReason;

    @Schema(description = "Work order created from this request (if approved)")
    private WorkOrderMiniDTO workOrder;

    @Schema(description = "Audio description file attached to the request")
    private FileMiniDTO audioDescription;

    @Schema(description = "Custom identifier for the request")
    private String customId;

    @Schema(description = "Portal through which this request was submitted")
    private RequestPortalMiniDTO requestPortal;

    @Schema(description = "Contact information of the person who submitted the request")
    private String contact;
}
