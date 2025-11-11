package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
public class PartConsumption extends CompanyAudit {
    @NotNull
    @Min(value = 0L, message = "The value must be positive")
    private double quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WorkOrder workOrder;

    public PartConsumption(Part part, WorkOrder workOrder, double quantity) {
        this.part = part;
        this.workOrder = workOrder;
        this.quantity = quantity;
    }

    public double getCost() {
        return part.getCost() * quantity;
    }
}
