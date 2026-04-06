package com.grash.dto;

import com.grash.model.enums.ApprovalStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.model.enums.workflow.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "Workflow condition configuration details")
public class WorkflowConditionShowDTO extends AuditShowDTO {
    @Schema(description = "Work order condition")
    private WorkOrderCondition workOrderCondition;
    @Schema(description = "Request condition")
    private RequestCondition requestCondition;
    @Schema(description = "Purchase order condition")
    private PurchaseOrderCondition purchaseOrderCondition;
    @Schema(description = "Part condition")
    private PartCondition partCondition;
    @Schema(description = "Task condition")
    private TaskCondition taskCondition;
    @Schema(description = "Priority requirement")
    private Priority priority;
    @Schema(description = "Associated asset")
    private AssetMiniDTO asset;
    @Schema(description = "Associated location")
    private LocationMiniDTO location;
    @Schema(description = "Associated user")
    private UserMiniDTO user;
    @Schema(description = "Associated team")
    private TeamMiniDTO team;
    @Schema(description = "Associated vendor")
    private VendorMiniDTO vendor;
    @Schema(description = "Associated part")
    private PartMiniDTO part;
    @Schema(description = "Work order category")
    private CategoryMiniDTO workOrderCategory;
    @Schema(description = "Purchase order category")
    private CategoryMiniDTO purchaseOrderCategory;
    @Schema(description = "Work order status requirement")
    private Status workOrderStatus;
    @Schema(description = "Purchase order status requirement")
    private ApprovalStatus purchaseOrderStatus;
    @Schema(description = "Created time range start")
    private Integer createdTimeStart;
    @Schema(description = "Created time range end")
    private Integer createdTimeEnd;
    @Schema(description = "Start date for condition")
    private Date startDate;
    @Schema(description = "End date for condition")
    private Date endDate;
    @Schema(description = "Label for the condition")
    private String label;
    @Schema(description = "String value for the condition")
    private String value;
    @Schema(description = "Numeric value for the condition")
    private Integer numberValue;
}
