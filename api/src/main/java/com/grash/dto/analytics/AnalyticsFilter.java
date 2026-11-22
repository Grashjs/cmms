package com.grash.dto.analytics;

import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.Priority;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Filters selected on the Analytics pages, resolved for one company by
 * {@link com.grash.service.AnalyticsFilterService}.
 * <p>
 * An empty set means that dimension is not filtered. {@code locationIds} already contains every sub-location of the
 * selected locations.
 */
public record AnalyticsFilter(Set<Long> locationIds,
                              Set<Long> assetCategoryIds,
                              Set<Long> teamIds,
                              Set<Long> categoryIds,
                              Set<Priority> priorities) {

    public static final AnalyticsFilter NONE = new AnalyticsFilter(Set.of(), Set.of(), Set.of(), Set.of(), Set.of());

    /**
     * Value passed to native queries for a dimension that is not filtered. Ids are always positive and priorities are
     * stored as their ordinal, so it never matches a real value. See {@code WorkOrderRepository.ANALYTICS_FILTER}.
     */
    public static final int NOT_FILTERED = -1;

    public AnalyticsFilter {
        locationIds = copy(locationIds);
        assetCategoryIds = copy(assetCategoryIds);
        teamIds = copy(teamIds);
        categoryIds = copy(categoryIds);
        priorities = copy(priorities);
    }

    public boolean isEmpty() {
        return locationIds.isEmpty() && assetCategoryIds.isEmpty() && teamIds.isEmpty() && categoryIds.isEmpty()
                && priorities.isEmpty();
    }

    /**
     * Whether a work order or a request passes every selected filter.
     */
    public boolean matches(WorkOrderBase item) {
        if (item == null) return false;
        Long locationId = item.getLocation() == null ? null : item.getLocation().getId();
        Long assetCategoryId = item.getAsset() == null || item.getAsset().getCategory() == null ? null :
                item.getAsset().getCategory().getId();
        Long teamId = item.getTeam() == null ? null : item.getTeam().getId();
        Long categoryId = item.getCategory() == null ? null : item.getCategory().getId();
        return allows(locationIds, locationId)
                && allows(assetCategoryIds, assetCategoryId)
                && allows(teamIds, teamId)
                && allows(categoryIds, categoryId)
                && allows(priorities, item.getPriority());
    }

    /**
     * @return the items that pass every selected filter, in their original order
     */
    public <T extends WorkOrderBase> List<T> apply(Collection<T> items) {
        if (isEmpty()) return List.copyOf(items);
        return items.stream().filter(this::matches).toList();
    }

    // Parameters of the native queries, see WorkOrderRepository.ANALYTICS_FILTER

    public List<Long> locationIdsParam() {
        return idsParam(locationIds);
    }

    public List<Long> assetCategoryIdsParam() {
        return idsParam(assetCategoryIds);
    }

    public List<Long> teamIdsParam() {
        return idsParam(teamIds);
    }

    public List<Long> categoryIdsParam() {
        return idsParam(categoryIds);
    }

    public List<Integer> prioritiesParam() {
        if (priorities.isEmpty()) return List.of(NOT_FILTERED);
        return priorities.stream().map(Priority::ordinal).sorted().toList();
    }

    private static <T> boolean allows(Set<T> selected, T value) {
        // immutable sets throw on contains(null)
        return selected.isEmpty() || value != null && selected.contains(value);
    }

    private static List<Long> idsParam(Set<Long> ids) {
        if (ids.isEmpty()) return List.of((long) NOT_FILTERED);
        return ids.stream().sorted().toList();
    }

    private static <T> Set<T> copy(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }
}
