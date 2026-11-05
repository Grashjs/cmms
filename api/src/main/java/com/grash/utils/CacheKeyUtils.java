package com.grash.utils;

import com.grash.advancedsearch.FilterField;
import com.grash.dto.DateRange;

import java.util.Date;
import java.util.List;

public class CacheKeyUtils {
    public static long roundToNearest20Minutes(Date date) {
        long millis = date.getTime();
        long interval = 20 * 60 * 1000L; // 20 minutes in ms
        return (millis / interval) * interval;
    }

    public static String dateRangeKey(Long userId, Date start, Date end) {
        return userId + "_" + roundToNearest20Minutes(start) + "_" + roundToNearest20Minutes(end);
    }

    /**
     * Key of an analytics cache entry: {@code c<companyId>_g<generation>_u<userId>[_<start>_<end>][_f<filters>]}.
     * <p>
     * Dates are rounded to 20 minutes so that requests whose end is "now" can still hit the cache; freshness is
     * guaranteed by the generation, which changes whenever the company's data changes.
     * The filter fields are part of the key so that differently filtered reports never share an entry.
     */
    public static String analyticsKey(long companyId, String generation, Long userId, DateRange dateRange) {
        StringBuilder key = new StringBuilder()
                .append('c').append(companyId)
                .append("_g").append(generation)
                .append("_u").append(userId);
        if (dateRange != null) {
            // endpoints with a fixed period (e.g. the last weeks) only send filter fields
            if (dateRange.getStart() != null && dateRange.getEnd() != null) {
                key.append('_').append(roundToNearest20Minutes(dateRange.getStart()))
                        .append('_').append(roundToNearest20Minutes(dateRange.getEnd()));
            }
            List<FilterField> filterFields = dateRange.getFilterFields();
            if (filterFields != null && !filterFields.isEmpty()) {
                key.append("_f").append(filterFields);
            }
        }
        return key.toString();
    }
}
