package com.grash.dto;

import com.grash.model.CostCategory;
import com.grash.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class AdditionalCostPatchDTO {
    private String description;
    @Schema(implementation = IdDTO.class)
    private User assignedTo;
    private double cost;

    @Schema(description = "Whether to include this cost in the total")
    private boolean includeToTotalCost;

    @Schema(description = "Date of the additional cost")
    private Date date;

    private CostCategory category;
}
