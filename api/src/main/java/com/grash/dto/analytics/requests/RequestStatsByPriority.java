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
@Schema(description = "Request statistics grouped by priority")
public class RequestStatsByPriority {
    @Schema(description = "None priority stats")
    private BasicStats none;
    
    @Schema(description = "Low priority stats")
    private BasicStats low;
    
    @Schema(description = "Medium priority stats")
    private BasicStats medium;
    
    @Schema(description = "High priority stats")
    private BasicStats high;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Basic statistics for a priority level")
    public static class BasicStats {
        @Schema(description = "Count")
        private int count;
    }
}
