package com.grash.dto;

import com.grash.model.*;
import com.grash.model.enums.ApprovalStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.model.enums.workflow.*;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowConditionPostDTO {
    @NotNull
    private WorkOrderCondition workOrderCondition;
    private RequestCondition requestCondition;
    private PurchaseOrderCondition purchaseOrderCondition;
    private PartCondition partCondition;
    private TaskCondition taskCondition;
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
    private Integer createdTimeStart;
    private Integer createdTimeEnd;
    @Schema(implementation = IdDTO.class)
    private Vendor vendor;
    @Schema(implementation = IdDTO.class)
    private Part part;
    @Schema(implementation = IdDTO.class)
    private PurchaseOrderCategory purchaseOrderCategory;
    private Status workOrderStatus;
    private ApprovalStatus purchaseOrderStatus;
    private Date startDate;
    private Date endDate;
    private String label;
    private String value;
    private Integer numberValue;

}

