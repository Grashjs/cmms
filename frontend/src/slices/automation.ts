import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';
import {
  AutomationMeta,
  AutomationRule,
  AutomationRulePayload,
  AutomationRun
} from '../models/owns/automation';

const basePath = 'automation-rules';

interface AutomationState {
  rules: AutomationRule[];
  /** Null until fetched. The editor cannot render without it, so it gates on this. */
  meta: AutomationMeta | null;
  /** Keyed by rule id, plus 'all' for the unfiltered log. */
  runs: { [key: string]: AutomationRun[] };
  loadingRules: boolean;
  loadingMeta: boolean;
  loadingRuns: boolean;
}

const initialState: AutomationState = {
  rules: [],
  meta: null,
  runs: {},
  loadingRules: false,
  loadingMeta: false,
  loadingRuns: false
};

const slice = createSlice({
  name: 'automation',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getRules(
      state: AutomationState,
      action: PayloadAction<{ rules: AutomationRule[] }>
    ) {
      state.rules = action.payload.rules;
    },
    addRule(
      state: AutomationState,
      action: PayloadAction<{ rule: AutomationRule }>
    ) {
      state.rules = [...state.rules, action.payload.rule];
    },
    editRule(
      state: AutomationState,
      action: PayloadAction<{ rule: AutomationRule }>
    ) {
      const { rule } = action.payload;
      state.rules = state.rules.map((existing) =>
        existing.id === rule.id ? rule : existing
      );
    },
    deleteRule(state: AutomationState, action: PayloadAction<{ id: number }>) {
      state.rules = state.rules.filter((rule) => rule.id !== action.payload.id);
    },
    getMeta(
      state: AutomationState,
      action: PayloadAction<{ meta: AutomationMeta }>
    ) {
      state.meta = action.payload.meta;
    },
    getRuns(
      state: AutomationState,
      action: PayloadAction<{ key: string; runs: AutomationRun[] }>
    ) {
      state.runs[action.payload.key] = action.payload.runs;
    },
    setLoading(
      state: AutomationState,
      action: PayloadAction<{
        field: 'loadingRules' | 'loadingMeta' | 'loadingRuns';
        loading: boolean;
      }>
    ) {
      state[action.payload.field] = action.payload.loading;
    }
  }
});

export const reducer = slice.reducer;

export const getAutomationRules = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoading({ field: 'loadingRules', loading: true }));
  try {
    const rules = await api.get<AutomationRule[]>(basePath);
    dispatch(slice.actions.getRules({ rules }));
  } finally {
    dispatch(
      slice.actions.setLoading({ field: 'loadingRules', loading: false })
    );
  }
};

/**
 * The editor's vocabulary. Fetched on every visit rather than cached across sessions on purpose:
 * it contains this company's custom fields and whether the engine is switched on, both of which
 * change outside this page.
 */
export const getAutomationMeta = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoading({ field: 'loadingMeta', loading: true }));
  try {
    const meta = await api.get<AutomationMeta>(`${basePath}/meta`);
    dispatch(slice.actions.getMeta({ meta }));
  } finally {
    dispatch(slice.actions.setLoading({ field: 'loadingMeta', loading: false }));
  }
};

export const addAutomationRule =
  (rule: AutomationRulePayload): AppThunk =>
  async (dispatch) => {
    const response = await api.post<AutomationRule>(basePath, rule);
    dispatch(slice.actions.addRule({ rule: response }));
  };

export const editAutomationRule =
  (id: number, rule: AutomationRulePayload): AppThunk =>
  async (dispatch) => {
    const response = await api.patch<AutomationRule>(
      `${basePath}/${id}`,
      rule
    );
    dispatch(slice.actions.editRule({ rule: response }));
  };

export const setAutomationRuleEnabled =
  (id: number, enabled: boolean): AppThunk =>
  async (dispatch) => {
    const response = await api.patch<AutomationRule>(
      `${basePath}/${id}/enabled?enabled=${enabled}`,
      {}
    );
    dispatch(slice.actions.editRule({ rule: response }));
  };

export const deleteAutomationRule =
  (id: number): AppThunk =>
  async (dispatch) => {
    const { success } = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    if (success) {
      dispatch(slice.actions.deleteRule({ id }));
    }
  };

/**
 * The run log, newest first. `ruleId` null reads the whole company's log — which is the view that
 * answers "did anything happen at all?", the question a rule that never fires actually raises.
 */
export const getAutomationRuns =
  (ruleId: number | null, size = 20): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoading({ field: 'loadingRuns', loading: true }));
    try {
      const path =
        ruleId == null
          ? `${basePath}/runs?size=${size}`
          : `${basePath}/${ruleId}/runs?size=${size}`;
      // Both endpoints return a Spring Page; only its content is of interest here.
      const page = await api.get<{ content: AutomationRun[] }>(path);
      dispatch(
        slice.actions.getRuns({
          key: ruleId == null ? 'all' : String(ruleId),
          runs: page.content ?? []
        })
      );
    } finally {
      dispatch(
        slice.actions.setLoading({ field: 'loadingRuns', loading: false })
      );
    }
  };

export default slice;
