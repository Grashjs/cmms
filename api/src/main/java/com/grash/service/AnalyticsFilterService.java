package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.dto.DateRange;
import com.grash.dto.analytics.AnalyticsFilter;
import com.grash.exception.CustomException;
import com.grash.model.enums.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Turns the {@code filterFields} sent with an analytics {@link DateRange} into an {@link AnalyticsFilter}.
 * <p>
 * Only the fields below are accepted, with the {@code in} (or {@code eq}) operation; anything else is rejected
 * instead of being silently ignored, so a report never looks filtered when it is not:
 * <ul>
 *     <li>{@code location}: location ids, sub-locations included</li>
 *     <li>{@code asset.category}: asset category ids</li>
 *     <li>{@code team}: team ids</li>
 *     <li>{@code category}: work order category ids</li>
 *     <li>{@code priority}: priority names ({@code NONE}, {@code LOW}, {@code MEDIUM}, {@code HIGH})</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AnalyticsFilterService {

    public static final String LOCATION = "location";
    public static final String ASSET_CATEGORY = "asset.category";
    public static final String TEAM = "team";
    public static final String CATEGORY = "category";
    public static final String PRIORITY = "priority";

    private static final Set<String> SUPPORTED_OPERATIONS = Set.of("in", "eq");

    private final LocationService locationService;

    public AnalyticsFilter resolve(Long companyId, DateRange dateRange) {
        if (dateRange == null || dateRange.getFilterFields() == null || dateRange.getFilterFields().isEmpty()) {
            return AnalyticsFilter.NONE;
        }
        Set<Long> locationIds = new HashSet<>();
        Set<Long> assetCategoryIds = new HashSet<>();
        Set<Long> teamIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        Set<Priority> priorities = EnumSet.noneOf(Priority.class);

        for (FilterField filterField : dateRange.getFilterFields()) {
            String field = filterField.getField();
            if (field == null || filterField.getOperation() == null
                    || !SUPPORTED_OPERATIONS.contains(filterField.getOperation())) {
                throw unsupported(field);
            }
            switch (field) {
                case LOCATION -> locationIds.addAll(ids(filterField));
                case ASSET_CATEGORY -> assetCategoryIds.addAll(ids(filterField));
                case TEAM -> teamIds.addAll(ids(filterField));
                case CATEGORY -> categoryIds.addAll(ids(filterField));
                case PRIORITY -> values(filterField).forEach(value -> priorities.add(priority(value)));
                default -> throw unsupported(field);
            }
        }
        if (!locationIds.isEmpty()) {
            // the selected ids are kept even if they are not found, so that the filter matches nothing instead of
            // being dropped
            locationIds.addAll(locationService.findDescendantIdsIncludingSelf(new ArrayList<>(locationIds),
                    companyId));
        }
        return new AnalyticsFilter(locationIds, assetCategoryIds, teamIds, categoryIds, priorities);
    }

    static List<Long> ids(FilterField filterField) {
        return values(filterField).stream().map(value -> {
            try {
                return Long.valueOf(value.toString());
            } catch (NumberFormatException e) {
                throw new CustomException("Invalid id for analytics filter " + filterField.getField() + ": " + value,
                        HttpStatus.BAD_REQUEST);
            }
        }).toList();
    }

    static Priority priority(Object value) {
        try {
            return Priority.valueOf(value.toString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid priority for analytics filter: " + value, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * @return the non-null values of the filter: {@code values} for {@code in}, or {@code value} for {@code eq}
     */
    static List<Object> values(FilterField filterField) {
        List<Object> values = new ArrayList<>();
        if (filterField.getValues() != null) values.addAll(filterField.getValues());
        if (values.isEmpty() && filterField.getValue() != null && !filterField.getValue().toString().isBlank()) {
            values.add(filterField.getValue());
        }
        values.removeIf(Objects::isNull);
        return values;
    }

    private static CustomException unsupported(String field) {
        return new CustomException("Unsupported analytics filter: " + field, HttpStatus.BAD_REQUEST);
    }
}
