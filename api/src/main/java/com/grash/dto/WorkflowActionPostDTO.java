package com.grash.dto;

import com.grash.model.*;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.workflow.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowActionPostDTO {
    private WorkOrderAction workOrderAction;
    private RequestAction requestAction;
    private PurchaseOrderAction purchaseOrderAction;
    private PartAction partAction;
    private TaskAction taskAction;
    private Priority priority;
    private Asset asset;
    private Location location;
    private OwnUser user;
    private Team team;
    private WorkOrderCategory workOrderCategory;
    private Checklist checklist;
    private Vendor vendor;
    private PurchaseOrderCategory purchaseOrderCategory;
    private String value;
    private AssetStatus assetStatus;
    private Integer numberValue;
}
