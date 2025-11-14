package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.RelationTypeInternal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Relation extends CompanyAudit {
    @NotNull
    private RelationTypeInternal relationType = RelationTypeInternal.RELATED_TO;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private WorkOrder parent;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private WorkOrder child;


}
