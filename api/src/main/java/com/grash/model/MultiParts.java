package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.PermissionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Multi-parts entity for grouping multiple parts together")
public class MultiParts extends CompanyAudit {

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_MultiParts_Part_Associations",
            joinColumns = @JoinColumn(name = "id_multi_parts"),
            inverseJoinColumns = @JoinColumn(name = "id_part"),
            indexes = {
                    @Index(name = "idx_multi_parts_part_multi_parts_id", columnList = "id_multi_parts"),
                    @Index(name = "idx_multi_parts_part_part_id", columnList = "id_part")
            })
    private List<Part> parts = new ArrayList<>();

    @NotNull
    @Schema(description = "Name of the multi-parts group", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    public boolean canBeEditedBy(User user) {
        return user.getRole().getEditOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)
                || (this.getCreatedBy() != null && this.getCreatedBy().equals(user.getId()));
    }

    public boolean canBeDeletedBy(User user) {
        return user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS)
                || (this.getCreatedBy() != null && this.getCreatedBy().equals(user.getId()));
    }

    public boolean canBeViewedBy(User user) {
        return (user.getRole().getViewPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS) &&
                (user.getRole().getViewOtherPermissions().contains(PermissionEntity.PARTS_AND_MULTIPARTS) || (getCreatedBy() != null && getCreatedBy().equals(user.getId()))));
    }
}


