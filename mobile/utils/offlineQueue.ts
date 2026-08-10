import AsyncStorage from '@react-native-async-storage/async-storage';
import api, { authHeader } from './api';
import type { AppDispatch } from '../store';
import Comment, { CommentPostDTO } from '../models/comment';
import type Labor from '../models/labor';
import type File from '../models/file';
import type { FileType } from '../models/file';
import type WorkOrder from '../models/workOrder';
import type { Task } from '../models/tasks';
import workOrderSlice from '../slices/workOrder';
import taskSlice from '../slices/task';
import laborSlice from '../slices/labor';
import commentSlice from '../slices/comment';
import fileSlice from '../slices/file';
import { offlineQueueSlice } from '../slices/offlineQueue';

const STORAGE_KEY = '@atlas/offline-queue';

export type OfflineMutationInput =
  | {
      type: 'changeStatus';
      workOrderId: number;
      body: { status: string; feedback?: string; signature?: string };
      optimistic?: Partial<WorkOrder>;
    }
  | {
      type: 'patchTask';
      workOrderId: number;
      taskId: number;
      task: Record<string, unknown>;
    }
  | {
      type: 'controlTimer';
      workOrderId: number;
      start: boolean;
    }
  | {
      type: 'createComment';
      comment: CommentPostDTO;
    }
  | {
      type: 'addFiles';
      files: { uri: string; name: string; type: string }[];
      fileType: FileType;
      taskId?: number;
      hidden?: 'true' | 'false';
    };

export type OfflineMutation = OfflineMutationInput & { id: string };

const readQueue = async (): Promise<OfflineMutation[]> => {
  const raw = await AsyncStorage.getItem(STORAGE_KEY);
  if (!raw) return [];
  try {
    return JSON.parse(raw) as OfflineMutation[];
  } catch {
    return [];
  }
};

const writeQueue = async (queue: OfflineMutation[]) => {
  await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(queue));
  return queue.length;
};

export const syncOfflineQueueCount = async (dispatch: AppDispatch) => {
  const pending = await readQueue().then((q) => q.length);
  dispatch(offlineQueueSlice.actions.setPending({ pending }));
  return pending;
};

export const enqueueOfflineMutation = async (
  mutation: OfflineMutation,
  dispatch: AppDispatch
) => {
  const queue = await readQueue();
  queue.push(mutation);
  await writeQueue(queue);
  dispatch(offlineQueueSlice.actions.setPending({ pending: queue.length }));
  return queue.length;
};

export const flushOfflineQueue = async (dispatch: AppDispatch): Promise<number> => {
  const queue = await readQueue();
  if (!queue.length) return 0;

  dispatch(offlineQueueSlice.actions.setFlushing({ flushing: true }));
  const remaining: OfflineMutation[] = [];
  let flushed = 0;

  try {
    for (const item of queue) {
      try {
        await executeMutation(item, dispatch);
        flushed += 1;
      } catch {
        remaining.push(item, ...queue.slice(queue.indexOf(item) + 1));
        break;
      }
    }
    await writeQueue(remaining);
    dispatch(offlineQueueSlice.actions.setPending({ pending: remaining.length }));
  } finally {
    dispatch(offlineQueueSlice.actions.setFlushing({ flushing: false }));
  }

  return flushed;
};

async function executeMutation(
  item: OfflineMutation,
  dispatch: AppDispatch
): Promise<void> {
  switch (item.type) {
    case 'changeStatus': {
      const workOrder = await api.patch<WorkOrder>(
        `work-orders/${item.workOrderId}/change-status`,
        item.body
      );
      dispatch(workOrderSlice.actions.editWorkOrder({ workOrder }));
      break;
    }
    case 'patchTask': {
      const task = await api.patch<Task>(
        `tasks/${item.taskId}`,
        item.task,
        null,
        true
      );
      dispatch(
        taskSlice.actions.patchTask({ workOrderId: item.workOrderId, task })
      );
      break;
    }
    case 'controlTimer': {
      const labor = await api.post<Labor>(
        `labors/work-order/${item.workOrderId}?start=${item.start}`,
        {}
      );
      dispatch(
        laborSlice.actions.controlTimer({
          labor,
          workOrderId: item.workOrderId
        })
      );
      break;
    }
    case 'createComment': {
      const comment = await api.post<Comment>('comments', item.comment);
      dispatch(
        commentSlice.actions.addComment({
          workOrderId: item.comment.workOrder.id,
          comment
        })
      );
      break;
    }
    case 'addFiles': {
      const formData = new FormData();
      const companyId = await AsyncStorage.getItem('companyId');
      const headers = await authHeader(false);
      delete headers['Content-Type'];
      item.files.forEach((file) => {
        //@ts-ignore
        formData.append('files', file);
      });
      formData.append('folder', `company ${companyId}`);
      formData.append('type', item.fileType);
      formData.append('hidden', item.hidden);
      const baseRoute = `files/upload`;
      const files = await api.post<File[]>(
        item.taskId ? `${baseRoute}?taskId=${item.taskId}` : baseRoute,
        formData,
        { headers },
        true,
        true
      );
      dispatch(fileSlice.actions.addFiles({ files }));
      break;
    }
  }
}

export const applyOptimisticMutation = (
  item: OfflineMutation,
  dispatch: AppDispatch
) => {
  if (item.type === 'changeStatus' && item.optimistic) {
    dispatch(
      workOrderSlice.actions.editWorkOrder({
        workOrder: {
          id: item.workOrderId,
          ...item.optimistic
        } as WorkOrder
      })
    );
  }
  if (item.type === 'patchTask') {
    dispatch(
      taskSlice.actions.patchTask({
        workOrderId: item.workOrderId,
        task: { id: item.taskId, ...item.task } as Task
      })
    );
  }
};

export const createMutationId = () =>
  `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
