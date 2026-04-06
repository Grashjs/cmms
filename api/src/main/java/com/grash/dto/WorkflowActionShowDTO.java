package com.grash.dto;

import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.workflow.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Workflow action configuration defining what changes to apply when a workflow is triggered")
public class WorkflowActionShowDTO extends AuditShowDTO {
    @Schema(description = "Work order action to perform (e.g., create, update, close)")
    private WorkOrderAction workOrderAction;
    @Schema(description = "Request action to perform")
    private RequestAction requestAction;
    @Schema(description = "Purchase order action to perform")
    private PurchaseOrderAction purchaseOrderAction;
    @Schema(description = "Part action to perform")
    private PartAction partAction;
    @Schema(description = "Task action to perform")
    private TaskAction taskAction;
    @Schema(description = "Priority level to assign")
    private Priority priority;
    @Schema(description = "Asset to associate with the action")
    private AssetMiniDTO asset;
    @Schema(description = "Location to associate with the action")
    private LocationMiniDTO location;
    @Schema(description = "User to assign or notify")
    private UserMiniDTO user;
    @Schema(description = "Team to assign the work to")
    private TeamMiniDTO team;
    @Schema(description = "Category for the work order")
    private CategoryMiniDTO workOrderCategory;
    @Schema(description = "Checklist template to attach")
    private ChecklistMiniDTO checklist;
    @Schema(description = "Vendor to associate with the action")
    private VendorMiniDTO vendor;
    @Schema(description = "Category for the purchase order")
    private CategoryMiniDTO purchaseOrderCategory;
    @Schema(description = "String value for custom field updates")
    private String value;
    @Schema(description = "Asset status to set")
    private AssetStatus assetStatus;
    @Schema(description = "Numeric value for custom field updates")
    private Integer numberValue;
}
