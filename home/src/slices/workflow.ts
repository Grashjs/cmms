import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import { Workflow } from '../models/owns/workflow';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'workflows';
interface WorkflowState {
  workflows: Workflow[];
  loadingGet: boolean;
}

const initialState: WorkflowState = {
  workflows: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'workflows',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getWorkflows(
      state: WorkflowState,
      action: PayloadAction<{ workflows: Workflow[] }>
    ) {
      const { workflows } = action.payload;
      state.workflows = workflows;
    },
    addWorkflow(
      state: WorkflowState,
      action: PayloadAction<{ workflow: Workflow }>
    ) {
      const { workflow } = action.payload;
      state.workflows = [...state.workflows, workflow];
    },
    editWorkflow(
      state: WorkflowState,
      action: PayloadAction<{ workflow: Workflow }>
    ) {
      const { workflow } = action.payload;
      state.workflows = state.workflows.map((workflow1) => {
        if (workflow1.id === workflow.id) {
          return workflow;
        }
        return workflow1;
      });
    },
    deleteWorkflow(
      state: WorkflowState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      const workflowIndex = state.workflows.findIndex(
        (workflow) => workflow.id === id
      );
      state.workflows.splice(workflowIndex, 1);
    },
    setLoadingGet(
      state: WorkflowState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getWorkflows = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoadingGet({ loading: true }));
  const workflows = await api.get<Workflow[]>(basePath);
  dispatch(slice.actions.getWorkflows({ workflows }));
  dispatch(slice.actions.setLoadingGet({ loading: false }));
};

export const addWorkflow =
  (workflow): AppThunk =>
  async (dispatch) => {
    const workflowResponse = await api.post<Workflow>(basePath, workflow);
    dispatch(slice.actions.addWorkflow({ workflow: workflowResponse }));
  };
export const editWorkflow =
  (id: number, workflow): AppThunk =>
  async (dispatch) => {
    const workflowResponse = await api.patch<Workflow>(
      `${basePath}/${id}`,
      workflow
    );
    dispatch(slice.actions.editWorkflow({ workflow: workflowResponse }));
  };
export const deleteWorkflow =
  (id: number): AppThunk =>
  async (dispatch) => {
    const workflowResponse = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = workflowResponse;
    if (success) {
      dispatch(slice.actions.deleteWorkflow({ id }));
    }
  };

export default slice;
