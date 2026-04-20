package com.grash.dto;

import com.grash.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdditionalCostPatchDTO {
    private String description;
    @Schema(implementation = IdDTO.class)
    private User assignedTo;
    private double cost;
}
