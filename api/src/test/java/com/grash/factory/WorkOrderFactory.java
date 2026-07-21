package com.grash.factory;

import com.grash.model.WorkOrder;
import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;

public final class WorkOrderFactory {

    private WorkOrderFactory() {
    }

    public static WorkOrder createWorkOrder() {
        WorkOrder wo = new WorkOrder();
        wo.setTitle("Test Work Order");
        wo.setDescription("Test description");
        wo.setStatus(Status.OPEN);
        wo.setPriority(Priority.NONE);
        wo.setEstimatedDuration(1.0);
        return wo;
    }

    public static WorkOrder createWorkOrderWithCreator(Long userId) {
        WorkOrder wo = createWorkOrder();
        wo.setCreatedBy(userId);
        return wo;
    }
}
