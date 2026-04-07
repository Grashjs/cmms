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
@Schema(description = "Work order request configuration for customizing request fields")
public class WorkOrderRequestConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "workOrderRequestConfiguration", fetch = FetchType.LAZY)
    private Set<FieldConfiguration> fieldConfigurations = new HashSet<>(createFieldConfigurations(Arrays.asList("asset", "location", "primaryUser", "dueDate", "category", "team"), this, null));

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private CompanySettings companySettings;

    public WorkOrderRequestConfiguration(CompanySettings companySettings) {
        this.companySettings = companySettings;
    }

}

