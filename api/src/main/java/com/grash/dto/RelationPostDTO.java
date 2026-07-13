package com.grash.dto;

import com.grash.model.Company;
import com.grash.model.WorkOrder;
import com.grash.model.enums.RelationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RelationPostDTO {

    @NotNull
    private WorkOrder parent;

    @NotNull
    private WorkOrder child;

    @NotNull
    private RelationType relationType = RelationType.RELATED_TO;

    private Company company;
}
