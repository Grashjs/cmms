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
@Schema(description = "Availability of an asset over a period")
public class AssetAvailability {

    @Schema(description = "Asset id")
    private Long id;

    @Schema(description = "Asset name")
    private String name;

    @Schema(description = "Time without downtime / time in service during the period, in percent")
    private double availability;

    @Schema(description = "Downtime during the period, in seconds")
    private long downtime;

    @Schema(description = "Downtimes overlapping the period")
    private int downtimeEvents;
}
