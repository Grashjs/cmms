package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Custom sequence generator for entity IDs per company")
public class CustomSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    // Company reference to isolate sequences by company
    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;

    // Sequence counters for each entity type
    @Schema(description = "Current work order sequence number")
    private Long workOrderSequence = 1L;
    @Schema(description = "Current asset sequence number")
    private Long assetSequence = 1L;
    @Schema(description = "Current preventive maintenance sequence number")
    private Long preventiveMaintenanceSequence = 1L;
    @Schema(description = "Current location sequence number")
    private Long locationSequence = 1L;
    @Schema(description = "Current request sequence number")
    private Long requestSequence = 1L;

    // Constructor with company
    public CustomSequence(Company company) {
        this.company = company;
    }

    // Methods to get and increment counters
    public Long getAndIncrementWorkOrderSequence() {
        return workOrderSequence++;
    }

    public Long getAndIncrementAssetSequence() {
        return assetSequence++;
    }

    public Long getAndIncrementPreventiveMaintenanceSequence() {
        return preventiveMaintenanceSequence++;
    }

    public Long getAndIncrementLocationSequence() {
        return locationSequence++;
    }

    public Long getAndIncrementRequestSequence() {
        return requestSequence++;
    }
}

