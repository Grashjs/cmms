package com.grash.dto;

import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.workflow.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkflowActionShowDTO extends AuditShowDTO {
    private WorkOrderAction workOrderAction;
    private RequestAction requestAction;
    private PurchaseOrderAction purchaseOrderAction;
    private PartAction partAction;
    private TaskAction taskAction;
    private Priority priority;
    private AssetMiniDTO asset;
    private LocationMiniDTO location;
    private UserMiniDTO user;
    private TeamMiniDTO team;
    private CategoryMiniDTO workOrderCategory;
    private ChecklistMiniDTO checklist;
    private VendorMiniDTO vendor;
    private CategoryMiniDTO purchaseOrderCategory;
    private String value;
    private AssetStatus assetStatus;
    private Integer numberValue;
}
