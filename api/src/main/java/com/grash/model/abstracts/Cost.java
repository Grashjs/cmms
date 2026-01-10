package com.grash.model.abstracts;

import com.grash.model.CostCategory;
import lombok.Data;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;

@Data
@MappedSuperclass
public abstract class Cost extends Audit {

    @NotNull
    private double cost;

    @ManyToOne
    private CostCategory category;

}


