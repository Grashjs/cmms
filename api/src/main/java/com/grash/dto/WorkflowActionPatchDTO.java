package com.grash.dto;

import com.grash.model.*;
import com.grash.model.enums.AssetStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.workflow.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkflowActionPatchDTO {
    private WorkOrderAction workOrderAction;
    private RequestAction requestAction;
    private PurchaseOrderAction purchaseOrderAction;
    private PartAction partAction;
    private TaskAction taskAction;
    private Priority priority;
    @Schema(implementation = IdDTO.class)
    private Asset asset;
    @Schema(implementation = IdDTO.class)
    private Location location;
    @Schema(implementation = IdDTO.class)
    private OwnUser user;
    @Schema(implementation = IdDTO.class)
    private Team team;
    @Schema(implementation = IdDTO.class)
    private WorkOrderCategory workOrderCategory;
    @Schema(implementation = IdDTO.class)
    private Checklist checklist;
    @Schema(implementation = IdDTO.class)
    private Vendor vendor;
    @Schema(implementation = IdDTO.class)
    private PurchaseOrderCategory purchaseOrderCategory;

    private String value;

    private AssetStatus assetStatus;
    private Integer numberValue;
}
