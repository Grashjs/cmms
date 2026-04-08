import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import Comment, {
  CommentPostDTO,
  CommentPatchDTO
} from '../models/owns/comment';
import api, { authHeader } from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'comments';

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
    try {
      dispatch(slice.actions.setLoadingComments({ loading: true }));
      const comments = await api.post<Comment[]>(
        `${basePath}/search/${workOrderId}`,
        {}
      );
      dispatch(slice.actions.getComments({ workOrderId, comments }));
    } finally {
      dispatch(slice.actions.setLoadingComments({ loading: false }));
    }
  };

export const createComment =
  (comment: CommentPostDTO): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingCreate({ loading: true }));
      const newComment = await api.post<Comment>(basePath, comment);
      dispatch(
        slice.actions.addComment({
          workOrderId: comment.workOrder.id,
          comment: newComment
        })
      );
      return newComment;
    } finally {
      dispatch(slice.actions.setLoadingCreate({ loading: false }));
    }
  };

export const updateComment =
  (id: number, comment: CommentPatchDTO, workOrderId: number): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingUpdate({ loading: true }));
      const updatedComment = await api.patch<Comment>(
        `${basePath}/${id}`,
        comment
      );
      dispatch(
        slice.actions.updateComment({ workOrderId, comment: updatedComment })
      );
      return updatedComment;
    } finally {
      dispatch(slice.actions.setLoadingUpdate({ loading: false }));
    }
  };

export const deleteComment =
  (id: number, workOrderId: number): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingDelete({ loading: true }));
      await api.deletes<{ success: boolean; message: string }>(
        `${basePath}/${id}`
      );
      dispatch(slice.actions.deleteComment({ workOrderId, commentId: id }));
    } finally {
      dispatch(slice.actions.setLoadingDelete({ loading: false }));
    }
  };

export const getCommentsCountByWorkOrder =
  (workOrderId: number): AppThunk =>
  async (dispatch) => {
    try {
      const response = await api.get<{ success: boolean; message: string }>(
        `${basePath}/count/${workOrderId}`
      );
      const count = parseInt(response.message, 10);
      dispatch(slice.actions.setCommentsCount({ workOrderId, count }));
    } catch (error) {
      console.error('Failed to fetch comments count:', error);
    }
  };

export default slice;
