package com.grash.dto.analytics.workOrders;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Work order count grouped by week")
public class WOCountByWeek {
    @Schema(description = "Total count")
    private int count;
    
    @Schema(description = "Compliant count")
    private int compliant;
    
    @Schema(description = "Reactive count")
    private int reactive;
    
    @Schema(description = "Date")
    private Date date;
}
