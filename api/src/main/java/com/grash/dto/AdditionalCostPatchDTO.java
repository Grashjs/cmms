package com.grash.dto;

import com.grash.model.OwnUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdditionalCostPatchDTO {
    private String description;
    @Schema(implementation = IdDTO.class)
    private OwnUser assignedTo;
    private double cost;
}
