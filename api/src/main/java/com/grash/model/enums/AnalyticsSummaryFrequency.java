package com.grash.model.enums;

/**
 * How often a user receives the maintenance summary of the company by email (see AnalyticsSummaryEmailService).
 * Stored as its ordinal: only add values at the end.
 */
public enum AnalyticsSummaryFrequency {
    NEVER,
    // on Mondays, for the previous week (Monday to Sunday)
    WEEKLY,
    // on the 1st, for the previous month
    MONTHLY
}
