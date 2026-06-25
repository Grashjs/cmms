package com.grash.dto;

import com.grash.dto.workOrder.WorkOrderMiniDTO;
import com.grash.model.enums.RelationTypeInternal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying a work order relation")
public class RelationShowDTO extends AuditShowDTO {

    @Schema(description = "Type of relation")
    private RelationTypeInternal relationType = RelationTypeInternal.RELATED_TO;

    @Schema(description = "Parent work order")
    private WorkOrderMiniDTO parent;

    @Schema(description = "Child work order")
    private WorkOrderMiniDTO child;
}
