package com.grash.advancedsearch;

import org.springframework.data.domain.Sort;

/**
 * Helpers around {@link SearchCriteria} that do not belong on the DTO itself.
 * <p>
 * A separate class rather than methods on {@code SearchCriteria}: that type is upstream and
 * shared with the mobile client, and every field on it is serialised over the wire.
 */
public final class SearchCriteriaUtils {

    private SearchCriteriaUtils() {
    }

    /**
     * The criteria's sort order with the id appended as a tiebreaker, for reading through
     * results page by page.
     * <p>
     * Without the tiebreaker, Postgres is free to return rows with equal sort keys in any
     * order, and it does change that order between queries — so paging over "sort by status"
     * can hand out one row twice and skip another. Interactive lists get away with it because
     * nobody notices a row moving between page 3 and page 4; an export that has to reconcile
     * does not.
     * <p>
     * A blank sort field falls back to the id alone. {@code PageRequest.of(..., direction,
     * properties)} throws on a null property, and criteria arriving from a saved view or an
     * API client may well omit it.
     */
    public static Sort stableSort(SearchCriteria searchCriteria) {
        Sort.Direction direction = searchCriteria.getDirection() == null
                ? Sort.Direction.ASC
                : searchCriteria.getDirection();
        String sortField = searchCriteria.getSortField();
        if (sortField == null || sortField.isBlank() || "id".equals(sortField)) {
            return Sort.by(direction, "id");
        }
        return Sort.by(direction, sortField).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
