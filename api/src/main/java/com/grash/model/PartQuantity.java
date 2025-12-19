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
public class PartQuantity extends CompanyAudit {

    @NotNull
    @Min(value = 0L, message = "The value must be positive")
    private double quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WorkOrder workOrder;

    private boolean isDemo;

    public double getCost() {
        return quantity * part.getCost();
    }

    public PartQuantity(Part part, WorkOrder workOrder, PurchaseOrder purchaseOrder, double quantity) {
        this.part = part;
        this.workOrder = workOrder;
        this.purchaseOrder = purchaseOrder;
        this.quantity = quantity;
    }
}
