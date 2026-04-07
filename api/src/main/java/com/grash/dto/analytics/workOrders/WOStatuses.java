package com.grash.dto.analytics.workOrders;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Work order status counts")
public class WOStatuses {
    @Schema(description = "Number of open work orders")
    private int open;
    
    @Schema(description = "Number of on-hold work orders")
    private int onHold;
    
    @Schema(description = "Number of in-progress work orders")
    private int inProgress;
    
    @Schema(description = "Number of complete work orders")
    private int complete;
}
