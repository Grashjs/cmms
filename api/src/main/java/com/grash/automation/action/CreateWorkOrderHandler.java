package com.grash.automation.action;

import com.grash.automation.eval.ExecutionContext;
import com.grash.automation.model.ActionType;
import com.grash.automation.model.AutomationActionStep;
import com.grash.exception.CustomException;
import com.grash.model.Asset;
import com.grash.model.WorkOrder;
import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.Priority;
import com.grash.service.AssetService;
import com.grash.service.WorkOrderCategoryService;
import com.grash.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Creates a work order, optionally for the asset that triggered the rule. This is the action the
 * leading use case turns on, and the one the old engine only has as a {@code //TODO}.
 *
 * <p>Parameters: {@code title} (required, may interpolate placeholders), {@code priority},
 * {@code category} (id), {@code asset} (id, usually {@code ${trigger.asset.id}}).
 */
@Component
@RequiredArgsConstructor
public class CreateWorkOrderHandler implements ActionHandler {

    private final WorkOrderService workOrderService;
    private final AssetService assetService;
    private final WorkOrderCategoryService workOrderCategoryService;

    @Override
    public ActionType getType() {
        return ActionType.CREATE_WORK_ORDER;
    }

    @Override
    public void execute(AutomationActionStep step, ExecutionContext context) {
        ActionParameters parameters = ActionParameters.of(step.getParameters(), context);

        WorkOrder workOrder = new WorkOrder();
        // Explicitly, and this is the whole point of the note in ActionHandler: there is no
        // security context on this thread, so CompanyAudit.beforePersist would leave the company
        // null and the insert would fail on the not-null constraint.
        workOrder.setCompany(context.getCompany());
        workOrder.setTitle(parameters.requireString("title"));

        String priority = parameters.getString("priority");
        if (priority != null) {
            workOrder.setPriority(readPriority(priority));
        }

        Long assetId = parameters.getLong("asset");
        if (assetId != null) {
            workOrder.setAsset(loadAsset(assetId, context));
        }

        Long categoryId = parameters.getLong("category");
        if (categoryId != null) {
            workOrder.setCategory(loadCategory(categoryId, context));
        }

        // Deliberately the service and not the repository: it assigns the work order number,
        // sends the notifications and dispatches the webhook. A rule-created work order should
        // be indistinguishable from a hand-created one.
        workOrderService.create(workOrder, context.getCompany());
    }

    private Priority readPriority(String value) {
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new CustomException("Unknown priority \"" + value + "\"", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Asset loadAsset(Long assetId, ExecutionContext context) {
        return assetService.findByIdAndCompany(assetId, context.getCompany().getId())
                .orElseThrow(() -> new CustomException("Asset " + assetId + " not found in this company",
                        HttpStatus.NOT_FOUND));
    }

    private WorkOrderCategory loadCategory(Long categoryId, ExecutionContext context) {
        WorkOrderCategory category = workOrderCategoryService.findById(categoryId)
                .orElseThrow(() -> new CustomException("Work order category " + categoryId + " not found",
                        HttpStatus.NOT_FOUND));
        if (!category.getCompanySettings().getCompany().getId().equals(context.getCompany().getId())) {
            throw new CustomException("Work order category " + categoryId + " belongs to another company",
                    HttpStatus.FORBIDDEN);
        }
        return category;
    }
}
