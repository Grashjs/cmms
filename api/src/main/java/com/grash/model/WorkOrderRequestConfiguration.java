package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.grash.model.FieldConfiguration.createFieldConfigurations;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "companySettings")
public class WorkOrderRequestConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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
