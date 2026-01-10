package com.grash.dto.workOrder;

import com.grash.dto.IdDTO;
import com.grash.dto.WorkOrderBasePatchDTO;
import com.grash.model.OwnUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class WorkOrderPatchDTO extends WorkOrderBasePatchDTO {
    @Schema(implementation = IdDTO.class)
    private OwnUser completedBy;
    private Date completedOn;
    private boolean archived;
}
