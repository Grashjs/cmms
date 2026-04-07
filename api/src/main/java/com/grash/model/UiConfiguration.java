package com.grash.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "UI configuration for toggling visibility of modules")
public class UiConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Whether requests module is visible")
    private boolean requests = true;
    @Schema(description = "Whether locations module is visible")
    private boolean locations = true;
    @Schema(description = "Whether meters module is visible")
    private boolean meters = true;
    @Schema(description = "Whether vendors and customers module is visible")
    private boolean vendorsAndCustomers = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private CompanySettings companySettings;

    public UiConfiguration(CompanySettings companySettings) {
        this.companySettings = companySettings;
    }
}

