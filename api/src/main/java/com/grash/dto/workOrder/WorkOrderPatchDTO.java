package com.grash.dto.workOrder;

import com.grash.dto.IdDTO;
import com.grash.dto.WorkOrderBasePatchDTO;
import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing work order")
public class WorkOrderPatchDTO extends WorkOrderBasePatchDTO {
    @Schema(description = "User who completed the work order", implementation = IdDTO.class)
    private User completedBy;
    @Schema(description = "Date and time when the work order was completed")
    private Date completedOn;
    @Schema(description = "Whether the work order is archived")
    private boolean archived;
    private List<CustomFieldValuePostDTO> customFields = new ArrayList<>();
}
