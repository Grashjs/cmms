import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import api from '../utils/api';
import { FilterField, Page, SearchCriteria, SearchOperator } from '../models/page';
import { revertAll } from '../utils/redux';

export type SearchResultType = 'workOrder' | 'asset' | 'location' | 'part';

/**
 * Results are normalised here rather than in the screen so that adding an
 * entity to the search means adding one entry to `SOURCES` below.
 */
export interface SearchResult {
  id: number;
  type: SearchResultType;
  title: string;
  subtitle?: string;
}

interface SearchSource {
  type: SearchResultType;
  path: string;
  fields: string[];
  toResult: (item: any) => SearchResult;
}

const SOURCES: SearchSource[] = [
  {
    type: 'workOrder',
    path: 'work-orders/search',
    fields: ['title', 'description', 'customId'],
    toResult: (item) => ({
      id: item.id,
      type: 'workOrder',
      title: item.title,
      subtitle: `#${item.customId}`
    })
  },
  {
    type: 'asset',
    path: 'assets/search',
    fields: ['name', 'description'],
    toResult: (item) => ({
      id: item.id,
      type: 'asset',
      title: item.name,
      subtitle: item.location?.name
    })
  },
  {
    type: 'location',
    path: 'locations/search',
    fields: ['name', 'address'],
    toResult: (item) => ({
      id: item.id,
      type: 'location',
      title: item.name,
      subtitle: item.address
    })
  },
  {
    type: 'part',
    path: 'parts/search',
    fields: ['name', 'description'],
    toResult: (item) => ({
      id: item.id,
      type: 'part',
      title: item.name,
      subtitle: item.description
    })
  }
];

interface GlobalSearchState {
  query: string;
  results: Record<SearchResultType, SearchResult[]>;
  loading: boolean;
  error: boolean;
}

const emptyResults = (): Record<SearchResultType, SearchResult[]> => ({
  workOrder: [],
  asset: [],
  location: [],
  part: []
});

const initialState: GlobalSearchState = {
  query: '',
  results: emptyResults(),
  loading: false,
  error: false
};

const slice = createSlice({
  name: 'globalSearch',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    setQuery(state: GlobalSearchState, action: PayloadAction<string>) {
      state.query = action.payload;
    },
    setResults(
      state: GlobalSearchState,
      action: PayloadAction<Record<SearchResultType, SearchResult[]>>
    ) {
      state.results = action.payload;
    },
    clear(state: GlobalSearchState) {
      state.results = emptyResults();
      state.error = false;
    },
    setLoading(state: GlobalSearchState, action: PayloadAction<boolean>) {
      state.loading = action.payload;
    },
    setError(state: GlobalSearchState, action: PayloadAction<boolean>) {
      state.error = action.payload;
    }
  }
});

export const reducer = slice.reducer;
export const { setQuery, clear } = slice.actions;

/** Matches the OR-across-fields shape the per-entity list screens already use. */
const containsAnyOf = (fields: string[], query: string): FilterField[] => {
  const [first, ...rest] = fields;
  return [
    {
      field: first,
      value: query,
      operation: 'cn' as SearchOperator,
      alternatives: rest.map((field) => ({
        field,
        operation: 'cn' as SearchOperator,
        value: query
      }))
    }
  ];
};

export const search =
  (query: string, pageSize = 5): AppThunk =>
  async (dispatch) => {
    const trimmed = query.trim();
    if (!trimmed) {
      dispatch(slice.actions.clear());
      return;
    }
    dispatch(slice.actions.setLoading(true));
    try {
      const responses = await Promise.allSettled(
        SOURCES.map((source) => {
          const criteria: SearchCriteria = {
            filterFields: containsAnyOf(source.fields, trimmed),
            pageSize,
            pageNum: 0,
            direction: 'DESC'
          };
          return api.post<Page<any>>(source.path, criteria);
        })
      );

      const results = emptyResults();
      responses.forEach((response, index) => {
        if (response.status === 'fulfilled') {
          const source = SOURCES[index];
          results[source.type] = response.value.content.map(source.toResult);
        }
      });
      dispatch(slice.actions.setResults(results));
      // A single entity the user lacks permission for should not blank the
      // whole screen, so only a complete failure counts as an error.
      dispatch(
        slice.actions.setError(responses.every((r) => r.status === 'rejected'))
      );
    } finally {
      dispatch(slice.actions.setLoading(false));
    }
  };

export default slice;
