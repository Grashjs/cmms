package com.grash.factory;

import com.grash.model.WorkOrderCategory;

public final class WorkOrderCategoryFactory {

    private WorkOrderCategoryFactory() {
    }

    public static WorkOrderCategory createWorkOrderCategory(String name) {
        WorkOrderCategory category = new WorkOrderCategory();
        category.setName(name);
        return category;
    }
}
