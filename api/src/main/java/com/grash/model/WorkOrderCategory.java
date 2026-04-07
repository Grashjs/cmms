package com.grash.model;

import com.grash.model.abstracts.CategoryAbstract;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Work order category for classifying work orders")
public class WorkOrderCategory extends CategoryAbstract {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    public WorkOrderCategory(String name, CompanySettings companySettings) {
        super(name, companySettings);
    }
}

