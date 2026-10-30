package com.grash.utils;

import com.grash.advancedsearch.FilterField;
import com.grash.dto.DateRange;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CacheKeyUtilsTest {

    private static final long INTERVAL_MS = 20 * 60 * 1000L;

    @Test
    void roundToNearest20Minutes_exactBoundary_returnsSameTime() {
        long boundary = 1000 * INTERVAL_MS;
        assertEquals(boundary, CacheKeyUtils.roundToNearest20Minutes(new Date(boundary)));
    }

    @Test
    void roundToNearest20Minutes_midInterval_roundsDown() {
        long boundary = 1000 * INTERVAL_MS;
        long mid = boundary + (INTERVAL_MS / 2);
        assertEquals(boundary, CacheKeyUtils.roundToNearest20Minutes(new Date(mid)));
    }

    @Test
    void roundToNearest20Minutes_justBeforeBoundary_roundsDownToPrevious() {
        long boundary = 1000 * INTERVAL_MS;
        assertEquals(boundary, CacheKeyUtils.roundToNearest20Minutes(new Date(boundary + INTERVAL_MS - 1)));
    }

    @Test
    void roundToNearest20Minutes_epoch_returnsZero() {
        assertEquals(0, CacheKeyUtils.roundToNearest20Minutes(new Date(0)));
    }

    @Test
    void dateRangeKey_combinesUserIdAndRoundedDates() {
        Date start = new Date(123456789L);
        Date end = new Date(987654321L);

        String key = CacheKeyUtils.dateRangeKey(42L, start, end);

        assertEquals(42L + "_" + CacheKeyUtils.roundToNearest20Minutes(start)
                + "_" + CacheKeyUtils.roundToNearest20Minutes(end), key);
    }

    @Test
    void dateRangeKey_datesInSameIntervals_produceSameKey() {
        long base = 1000 * INTERVAL_MS;
        String key1 = CacheKeyUtils.dateRangeKey(7L, new Date(base + 1000), new Date(base + 2 * INTERVAL_MS + 5000));
        String key2 = CacheKeyUtils.dateRangeKey(7L, new Date(base + INTERVAL_MS - 1000),
                new Date(base + 3 * INTERVAL_MS - 1));
        assertEquals(key1, key2);
    }

    @Test
    void analyticsKey_withoutDateRange_containsCompanyGenerationAndUser() {
        assertEquals("c5_g0.3_u7", CacheKeyUtils.analyticsKey(5L, "0.3", 7L, null));
    }

    @Test
    void analyticsKey_withDateRange_appendsRoundedDates() {
        Date start = new Date(123456789L);
        Date end = new Date(987654321L);
        DateRange range = DateRange.builder().start(start).end(end).build();

        assertEquals("c5_g0.0_u7_" + CacheKeyUtils.roundToNearest20Minutes(start) + "_"
                + CacheKeyUtils.roundToNearest20Minutes(end), CacheKeyUtils.analyticsKey(5L, "0.0", 7L, range));
    }

    @Test
    void analyticsKey_differentFilters_produceDifferentKeys() {
        DateRange byLocation1 = rangeWithFilters(List.of(FilterField.builder().field("location").operation("in")
                .values(List.of(1L)).build()));
        DateRange byLocation2 = rangeWithFilters(List.of(FilterField.builder().field("location").operation("in")
                .values(List.of(2L)).build()));
        DateRange unfiltered = rangeWithFilters(List.of());

        String key1 = CacheKeyUtils.analyticsKey(5L, "0.0", 7L, byLocation1);
        assertNotEquals(key1, CacheKeyUtils.analyticsKey(5L, "0.0", 7L, byLocation2));
        assertNotEquals(key1, CacheKeyUtils.analyticsKey(5L, "0.0", 7L, unfiltered));
        assertEquals(key1, CacheKeyUtils.analyticsKey(5L, "0.0", 7L, rangeWithFilters(List.of(
                FilterField.builder().field("location").operation("in").values(List.of(1L)).build()))));
    }

    @Test
    void analyticsKey_withFiltersButNoDates_onlyAppendsFilters() {
        FilterField team = FilterField.builder().field("team").operation("in").values(List.of(3L)).build();
        DateRange onlyFilters = DateRange.builder().filterFields(List.of(team)).build();

        assertEquals("c5_g0.0_u7_f" + List.of(team), CacheKeyUtils.analyticsKey(5L, "0.0", 7L, onlyFilters));
    }

    private static DateRange rangeWithFilters(List<FilterField> filterFields) {
        return DateRange.builder().start(new Date(0)).end(new Date(INTERVAL_MS)).filterFields(filterFields).build();
    }
}
