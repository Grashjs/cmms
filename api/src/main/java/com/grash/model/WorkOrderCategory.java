package com.grash.model;

import com.grash.model.abstracts.CategoryAbstract;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@Data
@NoArgsConstructor
public class WorkOrderCategory extends CategoryAbstract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public WorkOrderCategory(String name, CompanySettings companySettings) {
        super(name, companySettings);
    }
}

