package com.grash.dto;

import com.grash.model.Company;
import com.grash.model.WorkOrder;
import com.grash.model.enums.RelationType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RelationPostDTO {

    @Schema(implementation = IdDTO.class)
    @NotNull
    private WorkOrder parent;

    @Schema(implementation = IdDTO.class)
    @NotNull
    private WorkOrder child;

    @NotNull
    private RelationType relationType = RelationType.RELATED_TO;
}

