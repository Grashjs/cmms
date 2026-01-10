package com.grash.dto;

import com.grash.model.WorkOrder;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RelationPatchDTO {

    @Schema(implementation = IdDTO.class)
    private WorkOrder parent;

    @Schema(implementation = IdDTO.class)
    private WorkOrder child;
}
