package com.grash.dto.analytics.meters;

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
@Schema(description = "Readings of a meter during one interval of the trend")
public class MeterAnalyticsPoint {

    @Schema(description = "Start of the interval (local midnight for days, weeks and months)")
    private Date start;

    @Schema(description = "Readings taken during the interval")
    private int count;

    @Schema(description = "Lowest reading; null without readings")
    private Double min;

    @Schema(description = "Average reading; null without readings")
    private Double average;

    @Schema(description = "Highest reading; null without readings")
    private Double max;

    @Schema(description = "Last reading of the interval; null without readings")
    private Double last;

    @Schema(description = "Cumulative meters only: increase of the value during the interval, counter resets " +
            "included; null for other meters")
    private Double consumption;

    @Schema(description = "Readings outside the thresholds of the meter")
    private int outOfRange;
}
