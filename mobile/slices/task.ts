import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import api from '../utils/api';
import { Task } from '../models/tasks';
import { revertAll } from '../utils/redux';
import { getPreparedRuntime } from '../offline/runtime';
import {
  assertSessionGeneration,
  sessionGeneration,
  sessionChangeInProgress
} from '../offline/sessionBridge';

const basePath = 'tasks';

interface TaskState {
  tasksByWorkOrder: { [id: number]: Task[] };
  loadingTasks: { [id: number]: boolean };
}

const initialState: TaskState = {
  tasksByWorkOrder: {},
  loadingTasks: {}
};

const slice = createSlice({
  name: 'tasks',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    removePreparedTasks(state, action: PayloadAction<number[]>) {
      action.payload.forEach((id) => {
        delete state.tasksByWorkOrder[id];
        delete state.loadingTasks[id];
      });
    },
    getTasks(
      state: TaskState,
      action: PayloadAction<{ id: number; tasks: Task[] }>
    ) {
      const { tasks, id } = action.payload;
      state.tasksByWorkOrder[id] = tasks;
    },
    patchTasks(
      state: TaskState,
      action: PayloadAction<{
        workOrderId: number;
        tasks: Task[];
      }>
    ) {
      const { tasks, workOrderId } = action.payload;
      state.tasksByWorkOrder[workOrderId] = tasks;
    },
    patchTask(
      state: TaskState,
      action: PayloadAction<{
        workOrderId: number;
        task: Task;
      }>
    ) {
      const { task, workOrderId } = action.payload;
      state.tasksByWorkOrder[workOrderId] = (
        state.tasksByWorkOrder[workOrderId] || []
      ).map((t) => {
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
      state.tasksByWorkOrder[workOrderId] = (
        state.tasksByWorkOrder[workOrderId] || []
      ).filter((task) => task.id !== id);
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

export const getTasks =
  (id: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const prepared = getPreparedRuntime(id);
    if (prepared) return prepared.refresh(id);
    dispatch(slice.actions.setLoadingByTask({ id, loading: true }));
    try {
      const tasks = await api.get<Task[]>(`${basePath}/work-order/${id}`);
      assertSessionGeneration(generation);
      const nowPrepared = getPreparedRuntime(id);
      if (nowPrepared) return nowPrepared.refresh(id);
      dispatch(slice.actions.getTasks({ id, tasks }));
    } catch {
    } finally {
      if (generation === sessionGeneration() && !sessionChangeInProgress())
        dispatch(slice.actions.setLoadingByTask({ id, loading: false }));
    }
  };

export const patchTasks =
  (workOrderId: number, taskBases: any[]): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    if (taskBases.length) {
      const tasks = await api.patch<Task[]>(
        `${basePath}/work-order/${workOrderId}`,
        taskBases,
        null,
        true
      );
      assertSessionGeneration(generation);
      const prepared = getPreparedRuntime(workOrderId);
      if (prepared) {
        await prepared.refresh(workOrderId);
        return tasks;
      }
      dispatch(
        slice.actions.patchTasks({
          workOrderId,
          tasks
        })
      );
    }
  };

export const deleteTask =
  (workOrderId: number, id: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const response = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = response;
    assertSessionGeneration(generation);
    if (success) {
      const prepared = getPreparedRuntime(workOrderId);
      if (prepared) {
        await prepared.refresh(workOrderId);
        return;
      }
      dispatch(slice.actions.deleteTask({ workOrderId, id }));
    }
  };

export const patchTask =
  (workOrderId: number, taskId: number, task): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const prepared = getPreparedRuntime(workOrderId);
    if (prepared) {
      const patch: { value?: string | number; notes?: string } = {};
      if (Object.prototype.hasOwnProperty.call(task, 'value'))
        patch.value = task.value;
      if (Object.prototype.hasOwnProperty.call(task, 'notes'))
        patch.notes = task.notes;
      return prepared.patchTask(workOrderId, taskId, patch);
    }
    const taskResponse = await api.patch<Task>(
      `${basePath}/${taskId}`,
      task,
      null,
      true
    );
    assertSessionGeneration(generation);
    const nowPrepared = getPreparedRuntime(workOrderId);
    if (nowPrepared) {
      await nowPrepared.ingestTask(workOrderId, taskResponse);
      return taskResponse;
    }
    dispatch(
      slice.actions.patchTask({
        workOrderId,
        task: taskResponse
      })
    );
    return taskResponse;
  };
export const removePreparedTasks = slice.actions.removePreparedTasks;
export default slice;
