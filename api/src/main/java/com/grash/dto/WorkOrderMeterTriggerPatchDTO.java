package com.grash.dto;

import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkOrderMeterTriggerPatchDTO extends WorkOrderBasePatchDTO {
    private boolean recurrent;

    private String name;

    private WorkOrderMeterTriggerCondition triggerCondition;

    private int value;

    private int waitBefore;

}
