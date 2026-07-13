package com.grash.model.abstracts;

import com.grash.model.CostCategory;
import lombok.Data;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

@Data
@MappedSuperclass
public abstract class Cost extends Audit {

    @NotNull
    private double cost;

    @ManyToOne
    private CostCategory category;

}
