package com.grash.utils;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
