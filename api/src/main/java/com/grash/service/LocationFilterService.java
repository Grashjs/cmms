package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LocationFilterService {
    private static final String LOCATION_FIELD = "location";
    private static final String EQUAL_OPERATION = "eq";
    private static final String IN_OPERATION = "in";

    private final LocationRepository locationRepository;

    public void expandLocationFilters(SearchCriteria searchCriteria) {
        if (searchCriteria == null || searchCriteria.getFilterFields() == null) {
            return;
        }
        for (FilterField filterField : searchCriteria.getFilterFields()) {
            if (!LOCATION_FIELD.equals(filterField.getField())) {
                continue;
            }
            if (!EQUAL_OPERATION.equals(filterField.getOperation()) && !IN_OPERATION.equals(filterField.getOperation())) {
                continue;
            }
            Collection<Long> locationIds = extractLocationIds(filterField);
            if (locationIds.isEmpty()) {
                continue;
            }
            Collection<Long> subtreeIds = locationRepository.findSubtreeIds(locationIds);
            if (subtreeIds == null || subtreeIds.isEmpty()) {
                continue;
            }
            filterField.setOperation(IN_OPERATION);
            filterField.setValue("");
            filterField.setValues(new ArrayList<>(subtreeIds));
        }
    }

    private Collection<Long> extractLocationIds(FilterField filterField) {
        Set<Long> ids = new LinkedHashSet<>();
        if (IN_OPERATION.equals(filterField.getOperation()) && filterField.getValues() != null) {
            filterField.getValues().forEach(value -> addLocationId(ids, value));
        } else {
            addLocationId(ids, filterField.getValue());
        }
        return ids;
    }

    private void addLocationId(Set<Long> ids, Object value) {
        if (value instanceof Number number) {
            ids.add(number.longValue());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            addLocationId(ids, map.get("id"));
            return;
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                ids.add(Long.parseLong(stringValue));
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
