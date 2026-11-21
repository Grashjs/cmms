package com.grash.service;

import com.grash.advancedsearch.FilterField;
import com.grash.advancedsearch.SearchCriteria;
import com.grash.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationFilterServiceTest {

    @InjectMocks
    private LocationFilterService locationFilterService;

    @Mock
    private LocationRepository locationRepository;

    private SearchCriteria searchCriteria;

    @BeforeEach
    void setUp() {
        searchCriteria = new SearchCriteria();
        searchCriteria.setFilterFields(new ArrayList<>());
    }

    @Test
    void expandLocationFilters_eqFilter_includesDescendantLocations() {
        FilterField filterField = FilterField.builder()
                .field("location")
                .operation("eq")
                .value(1L)
                .build();
        searchCriteria.getFilterFields().add(filterField);
        when(locationRepository.findSubtreeIds(any())).thenReturn(Arrays.asList(1L, 2L, 3L));

        locationFilterService.expandLocationFilters(searchCriteria);

        assertEquals("in", filterField.getOperation());
        assertEquals(Arrays.asList(1L, 2L, 3L), filterField.getValues());
    }

    @Test
    void expandLocationFilters_inFilter_includesDescendantsOfEverySelectedLocation() {
        FilterField filterField = FilterField.builder()
                .field("location")
                .operation("in")
                .value("")
                .values(new ArrayList<>(Arrays.asList(1L, 4L)))
                .build();
        searchCriteria.getFilterFields().add(filterField);
        when(locationRepository.findSubtreeIds(any())).thenReturn(Arrays.asList(1L, 2L, 3L, 4L, 5L));

        locationFilterService.expandLocationFilters(searchCriteria);

        assertEquals("in", filterField.getOperation());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), filterField.getValues());
    }

    @Test
    void expandLocationFilters_nonLocationFilter_isUntouched() {
        FilterField filterField = FilterField.builder()
                .field("asset")
                .operation("in")
                .values(new ArrayList<>(Collections.singletonList(1L)))
                .build();
        searchCriteria.getFilterFields().add(filterField);

        locationFilterService.expandLocationFilters(searchCriteria);

        assertEquals("in", filterField.getOperation());
        assertEquals(Collections.singletonList(1L), filterField.getValues());
        verify(locationRepository, never()).findSubtreeIds(any());
    }

    @Test
    void expandLocationFilters_unsupportedOperation_isUntouched() {
        FilterField filterField = FilterField.builder()
                .field("location")
                .operation("ne")
                .value(1L)
                .build();
        searchCriteria.getFilterFields().add(filterField);

        locationFilterService.expandLocationFilters(searchCriteria);

        assertEquals("ne", filterField.getOperation());
        assertEquals(1L, filterField.getValue());
        verify(locationRepository, never()).findSubtreeIds(any());
    }

    @Test
    void expandLocationFilters_emptySubtree_keepsFilterUnchanged() {
        FilterField filterField = FilterField.builder()
                .field("location")
                .operation("eq")
                .value(1L)
                .build();
        searchCriteria.getFilterFields().add(filterField);
        when(locationRepository.findSubtreeIds(any())).thenReturn(Collections.emptyList());

        locationFilterService.expandLocationFilters(searchCriteria);

        assertEquals("eq", filterField.getOperation());
        assertEquals(1L, filterField.getValue());
    }

    @Test
    void expandLocationFilters_nullSearchCriteria_doesNothing() {
        locationFilterService.expandLocationFilters(null);

        verify(locationRepository, never()).findSubtreeIds(any());
    }

    @Test
    void expandLocationFilters_noFilters_doesNothing() {
        locationFilterService.expandLocationFilters(searchCriteria);

        verify(locationRepository, never()).findSubtreeIds(any());
    }
}
