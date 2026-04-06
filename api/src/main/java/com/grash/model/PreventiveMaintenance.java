package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.PermissionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Preventive maintenance schedule that generates recurring work orders")
public class PreventiveMaintenance extends WorkOrderBase {
    @Schema(description = "Unique identifier of the preventive maintenance", accessMode = Schema.AccessMode.READ_ONLY)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Schema(description = "Custom identifier for the preventive maintenance")
    private String customId;

    @Schema(description = "Name of the preventive maintenance schedule")
    private String name;

    @Schema(description = "Indicates whether this is a demo preventive maintenance")
    private boolean isDemo;

    @Schema(description = "Schedule that defines when this preventive maintenance generates work orders")
    @OneToOne(cascade = CascadeType.ALL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Schedule schedule = new Schedule(this);

    public boolean canBeEditedBy(OwnUser user) {
        return user.getRole().getEditOtherPermissions().contains(PermissionEntity.PREVENTIVE_MAINTENANCES)
                || this.getCreatedBy().equals(user.getId());
    }

}

