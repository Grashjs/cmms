package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Part consumption entity tracking part usage on work orders")
public class PartTransaction extends CompanyAudit {
    @NotNull
    @Schema(description = "Quantity consumed or restocked", requiredMode = Schema.RequiredMode.REQUIRED)
    private double quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Nullable
    private WorkOrder workOrder;

    private String description;

    public PartTransaction(Part part, @NotNull WorkOrder workOrder, double quantity) {
        this.part = part;
        this.workOrder = workOrder;
        this.quantity = quantity;
    }

    public PartTransaction(Part part, @Max(0) double quantity, String description) {// restock
        this.part = part;
        this.description = description;
        this.quantity = quantity;
    }

    public double getCost() {
        return part.getCost() * quantity;
    }

    @JsonIgnore
    public boolean isConsumption() {
        return quantity > 0;
    }
}


