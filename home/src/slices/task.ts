import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../utils/api';
import { Task } from '../models/owns/tasks';
import { revertAll } from 'src/utils/redux';

const basePath = 'tasks';

interface TaskState {
  tasksByWorkOrder: { [id: number]: Task[] };
  tasksByPreventiveMaintenance: { [id: number]: Task[] };
  loadingTasks: { [id: number]: boolean };
}

const initialState: TaskState = {
  tasksByWorkOrder: {},
  tasksByPreventiveMaintenance: {},
  loadingTasks: {}
};

const slice = createSlice({
  name: 'tasks',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getTasksByWorkOrder(
      state: TaskState,
      action: PayloadAction<{ id: number; tasks: Task[] }>
    ) {
      const { tasks, id } = action.payload;
      state.tasksByWorkOrder[id] = tasks;
    },
    patchTasksOfWorkOrder(
      state: TaskState,
      action: PayloadAction<{
        workOrderId: number;
        tasks: Task[];
      }>
    ) {
      const { tasks, workOrderId } = action.payload;
      state.tasksByWorkOrder[workOrderId] = tasks;
    },
    getTasksByPM(
      state: TaskState,
      action: PayloadAction<{ id: number; tasks: Task[] }>
    ) {
      const { tasks, id } = action.payload;
      state.tasksByPreventiveMaintenance[id] = tasks;
    },
    patchTasksOfPM(
      state: TaskState,
      action: PayloadAction<{
        preventiveMaintenanceId: number;
        tasks: Task[];
      }>
    ) {
      const { tasks, preventiveMaintenanceId } = action.payload;
      state.tasksByPreventiveMaintenance[preventiveMaintenanceId] = tasks;
    },
    patchTask(
      state: TaskState,
      action: PayloadAction<{
        workOrderId: number;
        task: Task;
      }>
    ) {
      const { task, workOrderId } = action.payload;
      state.tasksByWorkOrder[workOrderId] = state.tasksByWorkOrder[
        workOrderId
      ].map((t) => {
        if (t.id === task.id) {
          return task;
        }
        return t;
      });
    },
    deleteTask(
      state: TaskState,
      action: PayloadAction<{
        workOrderId: number;
        id: number;
      }>
    ) {
      const { id, workOrderId } = action.payload;
      state.tasksByWorkOrder[workOrderId] = state.tasksByWorkOrder[
        workOrderId
      ].filter((task) => task.id !== id);
    },
    setLoadingByTask(
      state: TaskState,
      action: PayloadAction<{ loading: boolean; id: number }>
    ) {
      const { loading, id } = action.payload;
      state.loadingTasks = { ...state.loadingTasks, [id]: loading };
    }
  }
});
export const reducer = slice.reducer;

export const getTasksByWorkOrder =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingByTask({ id, loading: true }));
    try {
      const tasks = await api.get<Task[]>(`${basePath}/work-order/${id}`);
      dispatch(slice.actions.getTasksByWorkOrder({ id, tasks }));
    } catch {
    } finally {
      dispatch(slice.actions.setLoadingByTask({ id, loading: false }));
    }
  };

export const patchTasksOfWorkOrder =
  (workOrderId: number, taskBases: any[]): AppThunk =>
  async (dispatch) => {
    if (taskBases.length) {
      const tasks = await api.patch<Task[]>(
        `${basePath}/work-order/${workOrderId}`,
        taskBases,
        null
      );
      dispatch(
        slice.actions.patchTasksOfWorkOrder({
          workOrderId,
          tasks
        })
      );
    }
  };
export const getTasksByPreventiveMaintenance =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingByTask({ id, loading: true }));
    try {
      const tasks = await api.get<Task[]>(
        `${basePath}/preventive-maintenance/${id}`
      );
      dispatch(slice.actions.getTasksByPM({ id, tasks }));
    } catch {
    } finally {
      dispatch(slice.actions.setLoadingByTask({ id, loading: false }));
    }
  };

export const patchTasksOfPreventiveMaintenance =
  (preventiveMaintenanceId: number, taskBases: any[]): AppThunk =>
  async (dispatch) => {
    if (taskBases.length) {
      const tasks = await api.patch<Task[]>(
        `${basePath}/preventive-maintenance/${preventiveMaintenanceId}`,
        taskBases,
        null
      );
      dispatch(
        slice.actions.patchTasksOfPM({
          preventiveMaintenanceId,
          tasks
        })
      );
    }
  };
export const deleteTask =
  (workOrderId: number, id: number): AppThunk =>
  async (dispatch) => {
    const response = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = response;
    if (success) {
      dispatch(slice.actions.deleteTask({ workOrderId, id }));
    }
  };

export const patchTask =
  (workOrderId: number, taskId: number, task): AppThunk =>
  async (dispatch) => {
    const taskResponse = await api.patch<Task>(
      `${basePath}/${taskId}`,
      task,
      null
    );
    dispatch(
      slice.actions.patchTask({
        workOrderId,
        task: taskResponse
      })
    );
  };
export default slice;
