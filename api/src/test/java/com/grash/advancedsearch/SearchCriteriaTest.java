package com.grash.advancedsearch;

import com.grash.model.Company;
import com.grash.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchCriteriaTest {

    @Mock
    private User user;

    @Mock
    private Company company;

    private SearchCriteria createCriteriaWithUser() {
        lenient().when(user.getCompany()).thenReturn(company);
        lenient().when(company.getId()).thenReturn(1L);
        lenient().when(user.getId()).thenReturn(42L);
        return new SearchCriteria();
    }

    @Test
    void constructor_shouldSetDefaults() {
        SearchCriteria criteria = new SearchCriteria();
        assertNotNull(criteria.getFilterFields());
        assertTrue(criteria.getFilterFields().isEmpty());
        assertEquals(Sort.Direction.ASC, criteria.getDirection());
        assertEquals(0, criteria.getPageNum());
        assertEquals(10, criteria.getPageSize());
        assertEquals("id", criteria.getSortField());
    }

    @Test
    void builder_shouldCreateCustomCriteria() {
        SearchCriteria criteria = SearchCriteria.builder()
                .filterFields(new ArrayList<>())
                .direction(Sort.Direction.DESC)
                .pageNum(2)
                .pageSize(20)
                .sortField("name")
                .build();

        assertEquals(Sort.Direction.DESC, criteria.getDirection());
        assertEquals(2, criteria.getPageNum());
        assertEquals(20, criteria.getPageSize());
        assertEquals("name", criteria.getSortField());
        assertNotNull(criteria.getFilterFields());
    }

    @Test
    void filterCompany_shouldAddCompanyFilter() {
        SearchCriteria criteria = createCriteriaWithUser();
        criteria.filterCompany(user);

        assertEquals(1, criteria.getFilterFields().size());
        FilterField filter = criteria.getFilterFields().get(0);
        assertEquals("company", filter.getField());
        assertEquals(1L, filter.getValue());
        assertEquals("eq", filter.getOperation());
    }

    @Test
    void filterCreatedBy_shouldAddCreatedByFilter() {
        SearchCriteria criteria = createCriteriaWithUser();
        criteria.filterCreatedBy(user);

        assertEquals(1, criteria.getFilterFields().size());
        FilterField filter = criteria.getFilterFields().get(0);
        assertEquals("createdBy", filter.getField());
        assertEquals(42L, filter.getValue());
        assertEquals("eq", filter.getOperation());
    }

    @Test
    void filterCompanyAndCreatedBy_shouldAddBothFilters() {
        SearchCriteria criteria = createCriteriaWithUser();
        criteria.filterCompany(user);
        criteria.filterCreatedBy(user);

        assertEquals(2, criteria.getFilterFields().size());
        assertEquals("company", criteria.getFilterFields().get(0).getField());
        assertEquals("createdBy", criteria.getFilterFields().get(1).getField());
    }

    @Test
    void clone_shouldCreateDeepCopy() {
        SearchCriteria criteria = createCriteriaWithUser();
        criteria.filterCompany(user);
        criteria.setSortField("name");
        criteria.setDirection(Sort.Direction.DESC);
        criteria.setPageNum(3);
        criteria.setPageSize(50);

        SearchCriteria cloned = criteria.clone();
        assertEquals(criteria.getSortField(), cloned.getSortField());
        assertEquals(criteria.getDirection(), cloned.getDirection());
        assertEquals(criteria.getPageNum(), cloned.getPageNum());
        assertEquals(criteria.getPageSize(), cloned.getPageSize());
        assertEquals(criteria.getFilterFields().size(), cloned.getFilterFields().size());
        assertNotSame(criteria.getFilterFields(), cloned.getFilterFields());
    }

    @Test
    void clone_shouldBeIndependentFromOriginal() {
        SearchCriteria criteria = createCriteriaWithUser();
        criteria.filterCompany(user);

        SearchCriteria cloned = criteria.clone();
        cloned.filterCreatedBy(user);

        assertEquals(1, criteria.getFilterFields().size());
        assertEquals(2, cloned.getFilterFields().size());
    }

    @Test
    void setters_shouldUpdateFields() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setPageNum(5);
        criteria.setPageSize(100);
        criteria.setSortField("createdAt");
        criteria.setDirection(Sort.Direction.DESC);

        assertEquals(5, criteria.getPageNum());
        assertEquals(100, criteria.getPageSize());
        assertEquals("createdAt", criteria.getSortField());
        assertEquals(Sort.Direction.DESC, criteria.getDirection());
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        FilterField filter = FilterField.builder().field("name").operation("eq").build();
        SearchCriteria criteria = new SearchCriteria(
                new ArrayList<>(List.of(filter)),
                Sort.Direction.DESC, 2, 25, "priority");

        assertEquals(1, criteria.getFilterFields().size());
        assertSame(filter, criteria.getFilterFields().get(0));
        assertEquals(Sort.Direction.DESC, criteria.getDirection());
        assertEquals(2, criteria.getPageNum());
        assertEquals(25, criteria.getPageSize());
        assertEquals("priority", criteria.getSortField());
    }
}
