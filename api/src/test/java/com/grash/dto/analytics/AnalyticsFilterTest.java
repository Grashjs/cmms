package com.grash.dto.analytics;

import com.grash.model.*;
import com.grash.model.enums.Priority;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsFilterTest {

    @Nested
    class Matches {

        @Test
        void none_matchesEverything() {
            assertTrue(AnalyticsFilter.NONE.isEmpty());
            assertTrue(AnalyticsFilter.NONE.matches(new WorkOrder()));
        }

        @Test
        void null_neverMatches() {
            assertFalse(AnalyticsFilter.NONE.matches(null));
        }

        @Test
        void byLocation() {
            AnalyticsFilter filter = filter(Set.of(1L, 2L), Set.of(), Set.of(), Set.of(), Set.of());

            assertTrue(filter.matches(workOrder(location(2L), null, null, null, Priority.NONE)));
            assertFalse(filter.matches(workOrder(location(3L), null, null, null, Priority.NONE)));
            assertFalse(filter.matches(workOrder(null, null, null, null, Priority.NONE)));
        }

        @Test
        void byAssetCategory() {
            AnalyticsFilter filter = filter(Set.of(), Set.of(5L), Set.of(), Set.of(), Set.of());

            assertTrue(filter.matches(workOrder(null, asset(5L), null, null, Priority.NONE)));
            assertFalse(filter.matches(workOrder(null, asset(6L), null, null, Priority.NONE)));
            assertFalse(filter.matches(workOrder(null, asset(null), null, null, Priority.NONE)));
            assertFalse(filter.matches(workOrder(null, null, null, null, Priority.NONE)));
        }

        @Test
        void byTeam() {
            AnalyticsFilter filter = filter(Set.of(), Set.of(), Set.of(7L), Set.of(), Set.of());

            assertTrue(filter.matches(workOrder(null, null, team(7L), null, Priority.NONE)));
            assertFalse(filter.matches(workOrder(null, null, team(8L), null, Priority.NONE)));
            assertFalse(filter.matches(workOrder(null, null, null, null, Priority.NONE)));
        }

        @Test
        void byCategory() {
            AnalyticsFilter filter = filter(Set.of(), Set.of(), Set.of(), Set.of(9L), Set.of());

            assertTrue(filter.matches(workOrder(null, null, null, category(9L), Priority.NONE)));
            assertFalse(filter.matches(workOrder(null, null, null, category(10L), Priority.NONE)));
            assertFalse(filter.matches(workOrder(null, null, null, null, Priority.NONE)));
        }

        @Test
        void byPriority() {
            AnalyticsFilter filter = filter(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(Priority.HIGH));

            assertTrue(filter.matches(workOrder(null, null, null, null, Priority.HIGH)));
            assertFalse(filter.matches(workOrder(null, null, null, null, Priority.LOW)));
            assertFalse(filter.matches(workOrder(null, null, null, null, null)));
        }

        @Test
        void severalDimensions_mustAllMatch() {
            AnalyticsFilter filter = filter(Set.of(1L), Set.of(), Set.of(7L), Set.of(), Set.of());

            assertTrue(filter.matches(workOrder(location(1L), null, team(7L), null, Priority.NONE)));
            assertFalse(filter.matches(workOrder(location(1L), null, team(8L), null, Priority.NONE)));
            assertFalse(filter.matches(workOrder(location(2L), null, team(7L), null, Priority.NONE)));
        }

        @Test
        void requests_areFilteredLikeWorkOrders() {
            AnalyticsFilter filter = filter(Set.of(1L), Set.of(), Set.of(), Set.of(), Set.of());
            Request request = new Request();
            request.setLocation(location(1L));

            assertTrue(filter.matches(request));
        }
    }

    @Test
    void apply_keepsMatchingItemsInOrder() {
        AnalyticsFilter filter = filter(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(Priority.HIGH, Priority.LOW));
        WorkOrder high = workOrder(null, null, null, null, Priority.HIGH);
        WorkOrder none = workOrder(null, null, null, null, Priority.NONE);
        WorkOrder low = workOrder(null, null, null, null, Priority.LOW);

        // compared by priority: WorkOrder#equals ignores the fields of WorkOrderBase
        assertEquals(List.of(Priority.HIGH, Priority.LOW), priorities(filter.apply(List.of(high, none, low))));
        assertEquals(List.of(Priority.HIGH, Priority.NONE, Priority.LOW),
                priorities(AnalyticsFilter.NONE.apply(List.of(high, none, low))));
    }

    @Test
    void params_useThePlaceholderWhenNotFiltered() {
        AnalyticsFilter none = AnalyticsFilter.NONE;

        assertEquals(List.of(-1L), none.locationIdsParam());
        assertEquals(List.of(-1L), none.assetCategoryIdsParam());
        assertEquals(List.of(-1L), none.teamIdsParam());
        assertEquals(List.of(-1L), none.categoryIdsParam());
        assertEquals(List.of(-1), none.prioritiesParam());
    }

    @Test
    void params_holdTheSelectedValues() {
        AnalyticsFilter filter = filter(Set.of(3L, 1L), Set.of(5L), Set.of(7L), Set.of(9L),
                Set.of(Priority.HIGH, Priority.NONE));

        assertEquals(List.of(1L, 3L), filter.locationIdsParam());
        assertEquals(List.of(5L), filter.assetCategoryIdsParam());
        assertEquals(List.of(7L), filter.teamIdsParam());
        assertEquals(List.of(9L), filter.categoryIdsParam());
        assertEquals(List.of(0, 3), filter.prioritiesParam());
    }

    @Test
    void nullSets_areTreatedAsNotFiltered() {
        AnalyticsFilter filter = new AnalyticsFilter(null, null, null, null, null);

        assertTrue(filter.isEmpty());
        assertTrue(filter.matches(new WorkOrder()));
    }

    private static List<Priority> priorities(List<WorkOrder> workOrders) {
        return workOrders.stream().map(WorkOrder::getPriority).toList();
    }

    private static AnalyticsFilter filter(Set<Long> locationIds, Set<Long> assetCategoryIds, Set<Long> teamIds,
                                          Set<Long> categoryIds, Set<Priority> priorities) {
        return new AnalyticsFilter(locationIds, assetCategoryIds, teamIds, categoryIds, priorities);
    }

    private static WorkOrder workOrder(Location location, Asset asset, Team team, WorkOrderCategory category,
                                       Priority priority) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setLocation(location);
        workOrder.setAsset(asset);
        workOrder.setTeam(team);
        workOrder.setCategory(category);
        workOrder.setPriority(priority);
        return workOrder;
    }

    private static Location location(Long id) {
        Location location = new Location();
        location.setId(id);
        return location;
    }

    private static Asset asset(Long categoryId) {
        Asset asset = new Asset();
        if (categoryId != null) {
            AssetCategory category = new AssetCategory();
            category.setId(categoryId);
            asset.setCategory(category);
        }
        return asset;
    }

    private static Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        return team;
    }

    private static WorkOrderCategory category(Long id) {
        WorkOrderCategory category = new WorkOrderCategory();
        category.setId(id);
        return category;
    }
}
