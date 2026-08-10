import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import WorkOrder from '../models/workOrder';
import api from '../utils/api';
import { FilterField, Page, SearchCriteria } from '../models/page';
import { revertAll } from '../utils/redux';

const basePath = 'work-orders';

/** How many rows each bucket shows before deferring to the full list. */
export const MY_DAY_BUCKET_SIZE = 5;

export type MyDayBucket = 'overdue' | 'today' | 'inProgress';

interface MyDayState {
  buckets: Record<MyDayBucket, WorkOrder[]>;
  /** Total matches per bucket, which is usually larger than the rows held. */
  counts: Record<MyDayBucket, number>;
  loading: boolean;
  /** Null once a fetch succeeds; set when every bucket fails. */
  error: string | null;
}

const emptyBuckets = (): Record<MyDayBucket, WorkOrder[]> => ({
  overdue: [],
  today: [],
  inProgress: []
});

const initialState: MyDayState = {
  buckets: emptyBuckets(),
  counts: { overdue: 0, today: 0, inProgress: 0 },
  loading: false,
  error: null
};

const slice = createSlice({
  name: 'myDay',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    setBucket(
      state: MyDayState,
      action: PayloadAction<{
        bucket: MyDayBucket;
        workOrders: WorkOrder[];
        count: number;
      }>
    ) {
      const { bucket, workOrders, count } = action.payload;
      state.buckets[bucket] = workOrders;
      state.counts[bucket] = count;
    },
    setLoading(state: MyDayState, action: PayloadAction<{ loading: boolean }>) {
      state.loading = action.payload.loading;
    },
    setError(state: MyDayState, action: PayloadAction<{ error: string | null }>) {
      state.error = action.payload.error;
    }
  }
});

export const reducer = slice.reducer;

/** Start and end of the current day in local time. */
const todayBounds = () => {
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  const end = new Date();
  end.setHours(24, 0, 0, 0);
  return { start, end };
};

const notCompleted: FilterField = {
  field: 'status',
  operation: 'in',
  value: '',
  values: ['OPEN', 'IN_PROGRESS', 'ON_HOLD'],
  enumName: 'STATUS'
};

const notArchived: FilterField = {
  field: 'archived',
  operation: 'eq',
  value: false
};

export const bucketFilters = (
  bucket: MyDayBucket,
  assignedToUserId?: number
): FilterField[] => {
  const { start, end } = todayBounds();
  const base: FilterField[] = [notArchived];

  if (assignedToUserId !== undefined) {
    base.push({
      field: 'assignedToUser',
      operation: 'eq',
      value: assignedToUserId
    });
  }

  switch (bucket) {
    case 'overdue': {
      // Anything still open whose due date has already passed. The API only
      // implements `ge` and `le` for JS_DATE — `lt` and `gt` return a 500 —
      // so the boundary is expressed as the last instant before today.
      const lastInstantBeforeToday = new Date(start.getTime() - 1);
      return [
        ...base,
        notCompleted,
        {
          field: 'dueDate',
          operation: 'le',
          value: lastInstantBeforeToday,
          enumName: 'JS_DATE'
        }
      ];
    }
    case 'today':
      return [
        ...base,
        notCompleted,
        { field: 'dueDate', operation: 'ge', value: start, enumName: 'JS_DATE' },
        { field: 'dueDate', operation: 'le', value: end, enumName: 'JS_DATE' }
      ];
    case 'inProgress':
      return [
        ...base,
        {
          field: 'status',
          operation: 'in',
          value: '',
          values: ['IN_PROGRESS'],
          enumName: 'STATUS'
        }
      ];
  }
};

/**
 * Fetches the three buckets in parallel. They are requested separately rather
 * than filtered from one page because a single page of results could be filled
 * entirely by one bucket, leaving the others looking empty.
 */
export const getMyDay =
  (assignedToUserId?: number): AppThunk =>
  async (dispatch) => {
    const buckets: MyDayBucket[] = ['overdue', 'today', 'inProgress'];
    dispatch(slice.actions.setLoading({ loading: true }));
    try {
      const results = await Promise.allSettled(
        buckets.map((bucket) => {
          const criteria: SearchCriteria = {
            filterFields: bucketFilters(bucket, assignedToUserId),
            pageSize: MY_DAY_BUCKET_SIZE,
            pageNum: 0,
            direction: 'ASC'
          };
          return api.post<Page<WorkOrder>>(`${basePath}/search`, criteria);
        })
      );

      results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          dispatch(
            slice.actions.setBucket({
              bucket: buckets[index],
              workOrders: result.value.content,
              count: result.value.totalElements
            })
          );
        }
      });

      // One failed bucket still leaves a useful screen, so only a total
      // failure is surfaced as an error.
      const allFailed = results.every((r) => r.status === 'rejected');
      dispatch(
        slice.actions.setError({ error: allFailed ? 'my_day_load_failed' : null })
      );
    } finally {
      dispatch(slice.actions.setLoading({ loading: false }));
    }
  };

export default slice;
