package com.grash.dto.workOrder;

import com.grash.model.WorkOrder;
import com.grash.model.enums.AssetStatus;
import lombok.Data;

@Data
public class WorkOrderPostDTO extends WorkOrder {
    private AssetStatus assetStatus;
}
