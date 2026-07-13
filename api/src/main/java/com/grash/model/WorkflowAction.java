package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.workflow.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowAction extends CompanyAudit {
    private WorkOrderAction workOrderAction;
    private RequestAction requestAction;
    private PurchaseOrderAction purchaseOrderAction;
    private PartAction partAction;
    private TaskAction taskAction;
    private Priority priority;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Asset asset;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Location location;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private OwnUser user;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Team team;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WorkOrderCategory workOrderCategory;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Checklist checklist;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vendor vendor;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PurchaseOrderCategory purchaseOrderCategory;

    private String value;

    private AssetStatus assetStatus;

    private Integer numberValue;
}
