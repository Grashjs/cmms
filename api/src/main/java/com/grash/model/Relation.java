package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.RelationTypeInternal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Schema(description = "Relation entity linking work orders together")
public class Relation extends CompanyAudit {
    @NotNull
    @Schema(description = "Type of relation", requiredMode = Schema.RequiredMode.REQUIRED)
    private RelationTypeInternal relationType = RelationTypeInternal.RELATED_TO;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private WorkOrder parent;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private WorkOrder child;


}


