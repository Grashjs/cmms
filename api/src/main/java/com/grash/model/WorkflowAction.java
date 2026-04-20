package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.workflow.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Workflow action defining what happens when conditions are met")
public class WorkflowAction extends CompanyAudit {
    @Schema(description = "Work order action to perform")
    private WorkOrderAction workOrderAction;
    @Schema(description = "Request action to perform")
    private RequestAction requestAction;
    @Schema(description = "Purchase order action to perform")
    private PurchaseOrderAction purchaseOrderAction;
    @Schema(description = "Part action to perform")
    private PartAction partAction;
    @Schema(description = "Task action to perform")
    private TaskAction taskAction;
    private Priority priority;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Asset asset;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Location location;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WorkOrderCategory workOrderCategory;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Checklist checklist;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vendor vendor;
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PurchaseOrderCategory purchaseOrderCategory;

    @Schema(description = "String value for the action")
    private String value;

    @Schema(description = "Asset status to set")
    private AssetStatus assetStatus;

    @Schema(description = "Numeric value for the action")
    private Integer numberValue;
}

