package com.grash.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
public class CustomSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // Company reference to isolate sequences by company
    @ManyToOne
    private Company company;

    // Sequence counters for each entity type
    private Long workOrderSequence = 1L;
    private Long assetSequence = 1L;
    private Long preventiveMaintenanceSequence = 1L;
    private Long locationSequence = 1L;
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
