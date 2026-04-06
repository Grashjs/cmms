package com.grash.dto.analytics.assets;

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
@Schema(description = "Downtime and costs grouped by date")
public class DowntimesByDate {
    @Schema(description = "Downtime duration in seconds")
    private long duration;
    
    @Schema(description = "Work order costs")
    private double workOrdersCosts;
    
    @Schema(description = "Date")
    private Date date;
}
