package com.grash.dto.analytics.requests;

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
@Schema(description = "Request metrics grouped by month")
public class RequestsByMonth {
    @Schema(description = "Cycle time in days")
    private long cycleTime;
    
    @Schema(description = "Date")
    private Date date;
}
