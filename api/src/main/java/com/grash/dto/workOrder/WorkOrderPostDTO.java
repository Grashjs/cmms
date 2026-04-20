package com.grash.dto.workOrder;

import com.grash.dto.CustomFieldValueDto;
import com.grash.model.WorkOrder;
import com.grash.model.enums.AssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class WorkOrderPostDTO extends WorkOrder {

    @Schema(description = "The status of the asset associated with this work order")
    private AssetStatus assetStatus;

    private List<CustomFieldValueDto> customFields;
}
