import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import Comment, { CommentPostDTO, CommentPatchDTO } from '../models/comment';
import api, { authHeader } from '../utils/api';
import { AppThunk } from '../store';
import { revertAll } from '../utils/redux';
import { getPreparedRuntime } from '../offline/runtime';
import {
  assertSessionGeneration,
  sessionGeneration,
  sessionChangeInProgress
} from '../offline/sessionBridge';

const basePath = 'comments';

// A successful mutation must not become retryable because the local projection failed.
export interface ConfirmedCommentMutation {
  comment?: Comment;
  localUpdateError?: unknown;
}

async function updateConfirmedCommentLocally(
  generation: number,
  update: () => Promise<void>,
  comment?: Comment
): Promise<ConfirmedCommentMutation> {
  try {
    await update();
    assertSessionGeneration(generation);
    return { comment };
  } catch (localUpdateError) {
    // Do not let a response for another account affect this screen.
    assertSessionGeneration(generation);
    return { comment, localUpdateError };
  }
}

interface CommentState {
  commentsByWorkOrder: { [key: number]: Comment[] };
  commentsCountByWorkOrder: { [key: number]: number };
  loadingComments: boolean;
  loadingCreate: boolean;
  loadingUpdate: boolean;
  loadingDelete: boolean;
}

const initialState: CommentState = {
  commentsByWorkOrder: {},
  commentsCountByWorkOrder: {},
  loadingComments: false,
  loadingCreate: false,
  loadingUpdate: false,
  loadingDelete: false
};

const slice = createSlice({
  name: 'comments',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    removePreparedComments(state, action: PayloadAction<number[]>) {
      action.payload.forEach((id) => {
        delete state.commentsByWorkOrder[id];
        delete state.commentsCountByWorkOrder[id];
      });
    },
    getComments(
      state: CommentState,
      action: PayloadAction<{ workOrderId: number; comments: Comment[] }>
    ) {
      const { workOrderId, comments } = action.payload;
      state.commentsByWorkOrder[workOrderId] = comments;
    },
    addComment(
      state: CommentState,
      action: PayloadAction<{ workOrderId: number; comment: Comment }>
    ) {
      const { workOrderId, comment } = action.payload;
      if (!state.commentsByWorkOrder[workOrderId]) {
        state.commentsByWorkOrder[workOrderId] = [];
      }
      state.commentsByWorkOrder[workOrderId] = [
        comment,
        ...state.commentsByWorkOrder[workOrderId]
      ];
      state.commentsCountByWorkOrder[workOrderId]++;
    },
    updateComment(
      state: CommentState,
      action: PayloadAction<{ workOrderId: number; comment: Comment }>
    ) {
      const { workOrderId, comment } = action.payload;
      if (state.commentsByWorkOrder[workOrderId]) {
        state.commentsByWorkOrder[workOrderId] = state.commentsByWorkOrder[
          workOrderId
        ].map((c) => (c.id === comment.id ? comment : c));
      }
    },
    deleteComment(
      state: CommentState,
      action: PayloadAction<{ workOrderId: number; commentId: number }>
    ) {
      const { workOrderId, commentId } = action.payload;
      if (state.commentsByWorkOrder[workOrderId]) {
        state.commentsByWorkOrder[workOrderId] = state.commentsByWorkOrder[
          workOrderId
        ].filter((c) => c.id !== commentId);
        state.commentsCountByWorkOrder[workOrderId]--;
      }
    },
    setLoadingComments(
      state: CommentState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingComments = loading;
    },
    setLoadingCreate(
      state: CommentState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingCreate = loading;
    },
    setLoadingUpdate(
      state: CommentState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingUpdate = loading;
    },
    setLoadingDelete(
      state: CommentState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingDelete = loading;
    },
    setCommentsCount(
      state: CommentState,
      action: PayloadAction<{ workOrderId: number; count: number }>
    ) {
      const { workOrderId, count } = action.payload;
      state.commentsCountByWorkOrder[workOrderId] = count;
    }
  }
});

export const reducer = slice.reducer;

export const getCommentsByWorkOrder =
  (workOrderId: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const prepared = getPreparedRuntime(workOrderId);
    if (prepared) return prepared.refresh(workOrderId);
    try {
      dispatch(slice.actions.setLoadingComments({ loading: true }));
      const comments = await api.post<Comment[]>(
        `${basePath}/search/${workOrderId}`,
        {}
      );
      assertSessionGeneration(generation);
      const nowPrepared = getPreparedRuntime(workOrderId);
      if (nowPrepared) return nowPrepared.refresh(workOrderId);
      dispatch(slice.actions.getComments({ workOrderId, comments }));
    } finally {
      if (generation === sessionGeneration() && !sessionChangeInProgress())
        dispatch(slice.actions.setLoadingComments({ loading: false }));
    }
  };

export const createComment =
  (comment: CommentPostDTO): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    try {
      dispatch(slice.actions.setLoadingCreate({ loading: true }));
      const initiallyPrepared = getPreparedRuntime(comment.workOrder.id);
      const create = async () => {
        assertSessionGeneration(generation);
        const newComment = await api.post<Comment>(basePath, comment);
        assertSessionGeneration(generation);
        const prepared = getPreparedRuntime(comment.workOrder.id);
        if (prepared) {
          return updateConfirmedCommentLocally(
            generation,
            () =>
              initiallyPrepared
                ? prepared.ingestComment(comment.workOrder.id, newComment)
                : prepared.refresh(comment.workOrder.id),
            newComment
          );
        }
        dispatch(
          slice.actions.addComment({
            workOrderId: comment.workOrder.id,
            comment: newComment
          })
        );
        return { comment: newComment } as ConfirmedCommentMutation;
      };
      return await (initiallyPrepared
        ? initiallyPrepared.runConnectedComment(comment.workOrder.id, create)
        : create());
    } finally {
      if (generation === sessionGeneration() && !sessionChangeInProgress())
        dispatch(slice.actions.setLoadingCreate({ loading: false }));
    }
  };

export const updateComment =
  (id: number, comment: CommentPatchDTO, workOrderId: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    try {
      dispatch(slice.actions.setLoadingUpdate({ loading: true }));
      const initiallyPrepared = getPreparedRuntime(workOrderId);
      const update = async () => {
        assertSessionGeneration(generation);
        const updatedComment = await api.patch<Comment>(
          `${basePath}/${id}`,
          comment
        );
        assertSessionGeneration(generation);
        const prepared = getPreparedRuntime(workOrderId);
        if (prepared) {
          return updateConfirmedCommentLocally(
            generation,
            () =>
              initiallyPrepared
                ? prepared.ingestComment(workOrderId, updatedComment)
                : prepared.refresh(workOrderId),
            updatedComment
          );
        }
        dispatch(
          slice.actions.updateComment({ workOrderId, comment: updatedComment })
        );
        return { comment: updatedComment } as ConfirmedCommentMutation;
      };
      return await (initiallyPrepared
        ? initiallyPrepared.runConnectedComment(workOrderId, update)
        : update());
    } finally {
      if (generation === sessionGeneration() && !sessionChangeInProgress())
        dispatch(slice.actions.setLoadingUpdate({ loading: false }));
    }
  };

export const deleteComment =
  (id: number, workOrderId: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    try {
      dispatch(slice.actions.setLoadingDelete({ loading: true }));
      const initiallyPrepared = getPreparedRuntime(workOrderId);
      const remove = async () => {
        assertSessionGeneration(generation);
        await api.deletes<{ success: boolean; message: string }>(
          `${basePath}/${id}`
        );
        assertSessionGeneration(generation);
        const prepared = getPreparedRuntime(workOrderId);
        if (prepared) {
          return updateConfirmedCommentLocally(generation, () =>
            initiallyPrepared
              ? prepared.removeComment(workOrderId, id)
              : prepared.refresh(workOrderId)
          );
        }
        dispatch(slice.actions.deleteComment({ workOrderId, commentId: id }));
        return {} as ConfirmedCommentMutation;
      };
      return await (initiallyPrepared
        ? initiallyPrepared.runConnectedComment(workOrderId, remove)
        : remove());
    } finally {
      if (generation === sessionGeneration() && !sessionChangeInProgress())
        dispatch(slice.actions.setLoadingDelete({ loading: false }));
    }
  };

export const getCommentsCountByWorkOrder =
  (workOrderId: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    if (getPreparedRuntime(workOrderId)) return;
    try {
      const response = await api.get<{ success: boolean; message: string }>(
        `${basePath}/count/${workOrderId}`
      );
      assertSessionGeneration(generation);
      if (getPreparedRuntime(workOrderId)) return;
      const count = parseInt(response.message, 10);
      dispatch(slice.actions.setCommentsCount({ workOrderId, count }));
    } catch (error) {
      console.error('Failed to fetch comments count:', error);
    }
  };

export const removePreparedComments = slice.actions.removePreparedComments;
export default slice;
