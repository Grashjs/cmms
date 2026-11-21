package com.grash.dto.analytics.meters;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Meter that can be selected on the meter analytics")
public class MeterOption {

    @Schema(description = "Meter id")
    private Long id;

    @Schema(description = "Meter name")
    private String name;

    @Schema(description = "Unit of the readings")
    private String unit;

    @Schema(description = "Whether the meter accumulates")
    private boolean cumulative;

    @Schema(description = "Name of the asset the meter is installed on")
    private String assetName;
}
