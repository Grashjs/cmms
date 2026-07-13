package com.grash.dto;

import com.grash.model.WorkOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RelationPatchDTO {

    private WorkOrder parent;

    private WorkOrder child;
}
