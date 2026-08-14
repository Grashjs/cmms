import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import api from '../utils/api';
import { runOrQueue } from '../utils/offlineMutations';
import { Task } from '../models/tasks';
import { revertAll } from '../utils/redux';

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

export const getTasks =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingByTask({ id, loading: true }));
    try {
      const tasks = await api.get<Task[]>(`${basePath}/work-order/${id}`);
      dispatch(slice.actions.getTasks({ id, tasks }));
    } catch {
    } finally {
      dispatch(slice.actions.setLoadingByTask({ id, loading: false }));
    }
  };

export const patchTasks =
  (workOrderId: number, taskBases: any[]): AppThunk =>
  async (dispatch) => {
    if (taskBases.length) {
      const tasks = await api.patch<Task[]>(
        `${basePath}/work-order/${workOrderId}`,
        taskBases,
        null,
        true
      );
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
    await runOrQueue(
      dispatch,
      async () => {
        const taskResponse = await api.patch<Task>(
          `${basePath}/${taskId}`,
          task,
          null,
          true
        );
        dispatch(
          slice.actions.patchTask({
            workOrderId,
            task: taskResponse
          })
        );
        return taskResponse;
      },
      {
        type: 'patchTask',
        workOrderId,
        taskId,
        task
      }
    );
  };
export default slice;
