package com.grash.dto.analytics.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request statistics and metrics")
public class RequestStats {
    @Schema(description = "Number of approved requests")
    private int approved;
    
    @Schema(description = "Number of pending requests")
    private int pending;
    
    @Schema(description = "Number of cancelled requests")
    private int cancelled;
    
    @Schema(description = "Average cycle time")
    private long cycleTime;
}
