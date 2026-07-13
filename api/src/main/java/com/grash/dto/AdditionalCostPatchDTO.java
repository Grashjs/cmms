package com.grash.dto;

import com.grash.model.OwnUser;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdditionalCostPatchDTO {
    private String description;
    private OwnUser assignedTo;
    private double cost;
}
