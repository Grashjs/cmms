package com.grash.dto.analytics.meters;

/**
 * Length of the points of a meter trend, chosen from the length of the period (see MeterAnalyticsService).
 */
public enum MeterInterval {
    HOUR,
    DAY,
    // weeks start on Monday
    WEEK,
    MONTH
}
