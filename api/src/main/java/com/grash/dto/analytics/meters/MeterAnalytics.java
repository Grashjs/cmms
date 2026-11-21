package com.grash.dto.analytics.meters;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Readings of a meter over a period: trend, thresholds and, for cumulative meters, consumption")
public class MeterAnalytics {

    @Schema(description = "Meter id")
    private Long meterId;

    @Schema(description = "Meter name")
    private String name;

    @Schema(description = "Unit of the readings")
    private String unit;

    @Schema(description = "Whether the meter accumulates, like an hour meter or an energy meter")
    private boolean cumulative;

    @Schema(description = "Asset the meter is installed on")
    private Long assetId;

    @Schema(description = "Name of the asset")
    private String assetName;

    @Schema(description = "Length of each point of the trend")
    private MeterInterval interval;

    @Builder.Default
    @Schema(description = "One point per interval, from the start of the period to its end (or now)")
    private List<MeterAnalyticsPoint> points = new ArrayList<>();

    @Builder.Default
    @Schema(description = "Limits from the work order triggers of the meter")
    private List<MeterThreshold> thresholds = new ArrayList<>();

    @Schema(description = "Readings in the period")
    private int readings;

    @Schema(description = "Lowest reading; null without readings")
    private Double min;

    @Schema(description = "Average reading; null without readings")
    private Double average;

    @Schema(description = "Highest reading; null without readings")
    private Double max;

    @Schema(description = "Last reading of the period; null without readings")
    private Double lastValue;

    @Schema(description = "Date of the last reading of the period")
    private Date lastReadingAt;

    @Schema(description = "Readings outside the thresholds")
    private int outOfRange;

    @Schema(description = "Cumulative meters only: increase of the value during the period; null for other meters")
    private Double consumption;

    @Schema(description = "Cumulative meters only: times the value went down, counted as the counter restarting " +
            "from 0")
    private int resets;

    @Schema(description = "Downtimes of the asset that started in the period")
    private int failures;

    @Schema(description = "Cumulative meters only: consumption / failures, e.g. running hours between failures " +
            "for an hour meter; null without failures")
    private Double consumptionBetweenFailures;
}
