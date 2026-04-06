package com.grash.dto.analytics.workOrders;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mobile work order statistics")
public class MobileWOStats {
    @Schema(description = "Number of open work orders")
    private int open;
    
    @Schema(description = "Number of on-hold work orders")
    private int onHold;
    
    @Schema(description = "Number of in-progress work orders")
    private int inProgress;
    
    @Schema(description = "Number of complete work orders")
    private int complete;
    
    @Schema(description = "Number of work orders due today")
    private int today;
    
    @Schema(description = "Number of high priority work orders")
    private int high;
}
