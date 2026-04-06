package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.grash.model.FieldConfiguration.createFieldConfigurations;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "companySettings")
@Schema(description = "Work order configuration for customizing work order fields")
public class WorkOrderConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "workOrderConfiguration", fetch = FetchType.LAZY)
    private Set<FieldConfiguration> workOrderFieldConfigurations = new HashSet<>(createFieldConfigurations(Arrays.asList("description", "asset",
            "priority", "images", "primaryUser", "assignedTo", "team", "location", "dueDate", "category", "purchaseOrder", "files", "signature", "completeFiles", "completeTasks", "completeTime", "completeParts", "completeCost"), null, this));

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private CompanySettings companySettings;

    public WorkOrderConfiguration(CompanySettings companySettings) {
        this.companySettings = companySettings;
    }

}

