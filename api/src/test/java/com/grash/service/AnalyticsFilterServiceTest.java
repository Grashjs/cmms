package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.dto.DateRange;
import com.grash.dto.analytics.AnalyticsFilter;
import com.grash.exception.CustomException;
import com.grash.model.enums.Priority;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsFilterServiceTest {

    private static final Long COMPANY_ID = 1L;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private AnalyticsFilterService analyticsFilterService;

    @Nested
    class WithoutFilters {

        @Test
        void nullDateRange_returnsNone() {
            assertSame(AnalyticsFilter.NONE, analyticsFilterService.resolve(COMPANY_ID, null));
        }

        @Test
        void noFilterFields_returnsNone() {
            DateRange dateRange = DateRange.builder().start(new Date(0)).end(new Date()).build();

            assertSame(AnalyticsFilter.NONE, analyticsFilterService.resolve(COMPANY_ID, dateRange));
            verifyNoInteractions(locationService);
        }

        @Test
        void emptyValues_areIgnored() {
            AnalyticsFilter filter = resolve(in("team"), in("priority"));

            assertTrue(filter.isEmpty());
        }
    }

    @Nested
    class SupportedFields {

        @Test
        void location_includesSubLocations() {
            when(locationService.findDescendantIdsIncludingSelf(any(), eq(COMPANY_ID)))
                    .thenReturn(List.of(10L, 11L, 12L));

            AnalyticsFilter filter = resolve(in("location", 10));

            assertEquals(Set.of(10L, 11L, 12L), filter.locationIds());
            verify(locationService).findDescendantIdsIncludingSelf(List.of(10L), COMPANY_ID);
        }

        @Test
        void location_keepsSelectedIdsNotFoundInTheCompany() {
            when(locationService.findDescendantIdsIncludingSelf(any(), anyLong())).thenReturn(List.of());

            AnalyticsFilter filter = resolve(in("location", 99));

            assertEquals(Set.of(99L), filter.locationIds());
            assertFalse(filter.isEmpty());
        }

        @Test
        void idsAndPriorities_areParsed() {
            AnalyticsFilter filter = resolve(
                    in("asset.category", 5, "6"),
                    in("team", 7L),
                    in("category", 9),
                    in("priority", "HIGH", "low"));

            assertEquals(Set.of(5L, 6L), filter.assetCategoryIds());
            assertEquals(Set.of(7L), filter.teamIds());
            assertEquals(Set.of(9L), filter.categoryIds());
            assertEquals(Set.of(Priority.HIGH, Priority.LOW), filter.priorities());
            assertTrue(filter.locationIds().isEmpty());
            verifyNoInteractions(locationService);
        }

        @Test
        void eq_usesTheSingleValue() {
            FilterField team = FilterField.builder().field("team").operation("eq").value(7).build();

            assertEquals(Set.of(7L), resolve(team).teamIds());
        }
    }

    @Nested
    class InvalidFilters {

        @Test
        void unsupportedField_isRejected() {
            assertBadRequest(in("primaryUser", 1));
        }

        @Test
        void unsupportedOperation_isRejected() {
            assertBadRequest(FilterField.builder().field("team").operation("ne").value(7).build());
        }

        @Test
        void missingOperation_isRejected() {
            assertBadRequest(FilterField.builder().field("team").value(7).build());
        }

        @Test
        void invalidId_isRejected() {
            assertBadRequest(in("team", "abc"));
        }

        @Test
        void invalidPriority_isRejected() {
            assertBadRequest(in("priority", "URGENT"));
        }

        private void assertBadRequest(FilterField filterField) {
            CustomException exception = assertThrows(CustomException.class, () -> resolve(filterField));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
        }
    }

    private AnalyticsFilter resolve(FilterField... filterFields) {
        DateRange dateRange = DateRange.builder()
                .start(new Date(0))
                .end(new Date())
                .filterFields(List.of(filterFields))
                .build();
        return analyticsFilterService.resolve(COMPANY_ID, dateRange);
    }

    private static FilterField in(String field, Object... values) {
        return FilterField.builder()
                .field(field)
                .operation("in")
                .value("")
                .values(new ArrayList<>(List.of(values)))
                .build();
    }
}
