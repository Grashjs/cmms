package com.grash.dto.analytics.meters;

import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Limit of a meter, from one of its work order triggers")
public class MeterThreshold {

    @Schema(description = "Name of the trigger")
    private String name;

    @Schema(description = "LESS_THAN: readings below the value are out of range; MORE_THAN: readings above it")
    private WorkOrderMeterTriggerCondition condition;

    @Schema(description = "Value of the trigger, in the unit of the meter")
    private double value;
}
