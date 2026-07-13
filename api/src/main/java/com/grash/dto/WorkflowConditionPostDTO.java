package com.grash.dto;

import com.grash.model.*;
import com.grash.model.enums.ApprovalStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.model.enums.workflow.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
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
    private Asset asset;
    private Location location;
    private OwnUser user;
    private Team team;
    private WorkOrderCategory workOrderCategory;
    private Checklist checklist;
    private Integer createdTimeStart;
    private Integer createdTimeEnd;
    private Vendor vendor;
    private Part part;
    private PurchaseOrderCategory purchaseOrderCategory;
    private Status workOrderStatus;
    private ApprovalStatus purchaseOrderStatus;
    private Date startDate;
    private Date endDate;
    private String label;
    private String value;
    private Integer numberValue;

}
