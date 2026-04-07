package com.grash.dto.analytics.parts;

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
@Schema(description = "Part consumption grouped by month")
public class PartConsumptionsByMonth {
    @Schema(description = "Total cost")
    private double cost;
    
    @Schema(description = "Date")
    private Date date;
}
