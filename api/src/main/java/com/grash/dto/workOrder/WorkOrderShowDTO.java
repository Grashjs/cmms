package com.grash.dto.workOrder;

import com.grash.dto.*;
import com.grash.dto.cutomField.CustomFieldValueShowDTO;
import com.grash.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Work order DTO for displaying work order details in API responses")
public class WorkOrderShowDTO extends WorkOrderBaseShowDTO {

    @Schema(description = "User who completed the work order")
    private UserMiniDTO completedBy;

    @Schema(description = "Date and time when the work order was completed")
    private Date completedOn;

    @Schema(description = "Whether the work order is archived")
    private boolean archived;

    @Schema(description = "Parent maintenance request that triggered this work order")
    private RequestMiniDTO parentRequest;

    @Schema(description = "Parent preventive maintenance that generated this work order")
    private PreventiveMaintenanceMiniDTO parentPreventiveMaintenance;

    @Schema(description = "Base64 encoded signature for the completed work order")
    private String signature;

    @Schema(description = "Current status of the work order")
    private Status status;

    @Schema(description = "Feedback or notes provided upon work order completion")
    private String feedback;

    @Schema(description = "Audio description file attached to the work order")
    private FileShowDTO audioDescription;

    @Schema(description = "Custom identifier for the work order")
    private String customId;

    @Schema(description = "Custom field values associated with the work order")
    private List<CustomFieldValueShowDTO> customFieldValues = new ArrayList<>();
}
