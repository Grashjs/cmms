package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.ApprovalStatus;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import com.grash.model.enums.workflow.*;
import com.grash.utils.Helper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;
import java.util.Objects;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowCondition extends CompanyAudit {
    private WorkOrderCondition workOrderCondition;
    private RequestCondition requestCondition;
    private PurchaseOrderCondition purchaseOrderCondition;
    private PartCondition partCondition;
    private TaskCondition taskCondition;
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
    private Vendor vendor;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Part part;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WorkOrderCategory workOrderCategory;
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PurchaseOrderCategory purchaseOrderCategory;
    private Status workOrderStatus;
    private ApprovalStatus purchaseOrderStatus;
    private Integer createdTimeStart;
    private Integer createdTimeEnd;
    private Date startDate;
    private Date endDate;
    private String label;
    private String value;
    private Integer numberValue;

    public boolean isMetForWorkOrder(WorkOrder workOrder) {
        switch (Objects.requireNonNull(workOrderCondition)) {
            case TEAM_IS:
                return workOrder.getTeam() != null && workOrder.getTeam().getId().equals(this.team.getId());
            case PRIORITY_IS:
                return workOrder.getPriority() != null && workOrder.getPriority().equals(this.getPriority());
            case ASSET_IS:
                return workOrder.getAsset() != null && workOrder.getAsset().getId().equals(this.asset.getId());
            case CATEGORY_IS:
                return workOrder.getCategory() != null && workOrder.getCategory().getId().equals(this.workOrderCategory.getId());
            case LOCATION_IS:
                return workOrder.getLocation() != null && workOrder.getLocation().getId().equals(this.location.getId());
            case USER_IS:
                return workOrder.getPrimaryUser() != null && workOrder.getPrimaryUser().getId().equals(this.user.getId());
            case CREATED_AT_BETWEEN:
                return workOrder.getCreatedAt().getHours() > this.getCreatedTimeStart() && workOrder.getCreatedAt().getHours() < (this.getCreatedTimeEnd());
            case DUE_DATE_BETWEEN:
                return workOrder.getDueDate() != null && workOrder.getDueDate().after(this.startDate) && workOrder.getDueDate().before(this.endDate);
            case STATUS_IS:
                return workOrder.getStatus().equals(this.workOrderStatus);
            case DUE_DATE_AFTER:
                return workOrder.getDueDate() != null && workOrder.getDueDate().after(this.endDate);
            default:
                return false;
        }
    }

    public boolean isMetForRequest(Request request) {
        switch (Objects.requireNonNull(requestCondition)) {
            case TEAM_IS:
                return request.getTeam() != null && request.getTeam().getId().equals(this.team.getId());
            case PRIORITY_IS:
                return request.getPriority() != null && request.getPriority().equals(this.getPriority());
            case ASSET_IS:
                return request.getAsset() != null && request.getAsset().getId().equals(this.asset.getId());
            case CATEGORY_IS:
                return request.getCategory() != null && request.getCategory().getId().equals(this.workOrderCategory.getId());
            case LOCATION_IS:
                return request.getLocation() != null && request.getLocation().getId().equals(this.location.getId());
            case USER_IS:
                return request.getPrimaryUser() != null && request.getPrimaryUser().getId().equals(this.user.getId());
            case CREATED_AT_BETWEEN:
                return request.getCreatedAt().getHours() > this.getCreatedTimeStart() && request.getCreatedAt().getHours() < (this.getCreatedTimeEnd());
            case DUE_DATE_BETWEEN:
                return request.getDueDate() != null && request.getDueDate().after(this.startDate) && request.getDueDate().before(this.endDate);
            case DUE_DATE_AFTER:
                return request.getDueDate() != null && request.getDueDate().after(this.endDate);
            default:
                return false;
        }
    }

    public boolean isMetForPurchaseOrder(PurchaseOrder purchaseOrder) {
        switch (Objects.requireNonNull(purchaseOrderCondition)) {
            case VENDOR_IS:
                return purchaseOrder.getVendor() != null && purchaseOrder.getVendor().getId().equals(this.vendor.getId());
            case STATUS_IS:
                return purchaseOrder.getStatus().equals(this.purchaseOrderStatus);
            case CATEGORY_IS:
                return purchaseOrder.getCategory() != null && purchaseOrder.getCategory().getId().equals(this.purchaseOrderCategory.getId());
            case DUE_DATE_AFTER:
                return purchaseOrder.getShippingDueDate() != null && purchaseOrder.getShippingDueDate().after(this.endDate);
            case DUE_DATE_BETWEEN:
                return purchaseOrder.getShippingDueDate() != null && purchaseOrder.getShippingDueDate().after(this.startDate) && purchaseOrder.getShippingDueDate().before(this.endDate);
            default:
                return false;
        }
    }

    public boolean isMetForPart(Part part) {
        switch (Objects.requireNonNull(partCondition)) {
            case PART_IS:
                return part.getId().equals(this.part.getId());
            case QUANTITY_INFERIOR:
                return part.getQuantity() < this.numberValue;
            default:
                return false;
        }
    }


    public boolean isMetForTask(Task task) {
        switch (Objects.requireNonNull(taskCondition)) {
            case NAME_IS:
                return task.getTaskBase().getLabel().equals(this.label);
            case VALUE_IS:
                return task.getValue() != null && task.getValue().equals(this.value);
            case NAME_CONTAINS:
                return task.getTaskBase().getLabel().contains(this.label);
            case VALUE_CONTAINS:
                return task.getValue() != null && task.getValue().contains(this.value);
            case NUMBER_VALUE_INFERIOR:
                return task.getValue() != null && Helper.isNumeric(task.getValue()) && Double.parseDouble(task.getValue()) < this.getNumberValue();
            case NUMBER_VALUE_SUPERIOR:
                return task.getValue() != null && Helper.isNumeric(task.getValue()) && Double.parseDouble(task.getValue()) > this.getNumberValue();
            default:
                return false;
        }
    }

}
