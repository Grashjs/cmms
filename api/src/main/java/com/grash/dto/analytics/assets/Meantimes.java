package com.grash.dto.analytics.assets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Mean time metrics")
public class Meantimes {
    @Schema(description = "Mean time between downtimes")
    private long betweenDowntimes;
    
    @Schema(description = "Mean time between maintenances")
    private long betweenMaintenances;
}
