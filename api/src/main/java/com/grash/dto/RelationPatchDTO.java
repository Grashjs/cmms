package com.grash.dto;

import com.grash.model.WorkOrder;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching a work order relation")
public class RelationPatchDTO {

    @Schema(description = "Parent work order", implementation = IdDTO.class)
    private WorkOrder parent;

    @Schema(description = "Child work order", implementation = IdDTO.class)
    private WorkOrder child;
}
