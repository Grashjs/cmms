package com.grash.dto.analytics.workOrders;

import com.grash.model.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response and resolution times of reactive work orders of one priority")
public class PriorityResponseTimes {

    @Schema(description = "Priority")
    private Priority priority;

    @Schema(description = "Reactive work orders created in the period with this priority")
    private int count;

    @Schema(description = "Of count, those completed")
    private int completed;

    @Schema(description = "Mean time to acknowledge: from creation to the first action, in hours; null when none " +
            "was acknowledged")
    private Double mtta;

    @Schema(description = "Mean time to resolve: from creation (of the request, if any) to completion, in hours; null" +
            " when none was completed")
    private Double mttr;
}
