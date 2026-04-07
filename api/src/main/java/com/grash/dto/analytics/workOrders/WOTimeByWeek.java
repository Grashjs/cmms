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
@Schema(description = "Work order time metrics by week")
public class WOTimeByWeek {
    @Schema(description = "Total time")
    private long total;
    
    @Schema(description = "Reactive time")
    private long reactive;
    
    @Schema(description = "Date")
    private Date date;
}
