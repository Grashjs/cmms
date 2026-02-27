import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import _ from 'lodash';
import type { AppThunk } from 'src/store';
import axios from 'src/utils/axios';
import objectArray from 'src/utils/objectArray';
import type { Project, Task, List, Member } from 'src/models/projects_board';

interface ProjectsBoardState {
  isLoaded: boolean;
  lists: {
    byId: Record<string, List>;
    allIds: string[];
  };
  tasks: {
    byId: Record<string, Task>;
    allIds: string[];
  };
  members: {
    byId: Record<string, Member>;
    allIds: string[];
  };
}

const initialState: ProjectsBoardState = {
  isLoaded: false,
  lists: {
    byId: {},
    allIds: []
  },
  tasks: {
    byId: {},
    allIds: []
  },
  members: {
    byId: {},
    allIds: []
  }
};

const slice = createSlice({
  name: 'projects_board',
  initialState,
  reducers: {
    getBoard(
      state: ProjectsBoardState,
      action: PayloadAction<{ project: Project }>
    ) {
      const { project } = action.payload;

      state.lists.byId = objectArray(project.lists);
      state.lists.allIds = Object.keys(state.lists.byId);
      state.tasks.byId = objectArray(project.tasks);
      state.tasks.allIds = Object.keys(state.tasks.byId);
      state.members.byId = objectArray(project.members);
      state.members.allIds = Object.keys(state.members.byId);
      state.isLoaded = true;
    },
    updateList(
      state: ProjectsBoardState,
      action: PayloadAction<{ list: List }>
    ) {
      const { list } = action.payload;

      state.lists.byId[list.id] = list;
    },

    moveTask(
      state: ProjectsBoardState,
      action: PayloadAction<{
        taskId: string;
        position: number;
        listId?: string;
      }>
    ) {
      const { taskId, position, listId } = action.payload;
      const { listId: sourceListId } = state.tasks.byId[taskId];

      _.pull(state.lists.byId[sourceListId].taskIds, taskId);
      if (listId) {
        state.tasks.byId[taskId].listId = listId;
        state.lists.byId[listId].taskIds.splice(position, 0, taskId);
      } else {
        state.lists.byId[sourceListId].taskIds.splice(position, 0, taskId);
      }
    }
  }
});

export const reducer = slice.reducer;

export const getBoard = (): AppThunk => async (dispatch) => {
  const response = await axios.get<{ project: Project }>(
    '/api/projects_board/board'
  );

  dispatch(slice.actions.getBoard(response.data));
};

export const updateList =
  (listId: string, update: any): AppThunk =>
  async (dispatch) => {
    const response = await axios.post<{ list: List }>(
      '/api/projects_board/list/update',
      {
        listId,
        update
      }
    );

    dispatch(slice.actions.updateList(response.data));
  };

export const moveTask =
  (taskId: string, position: number, listId?: string): AppThunk =>
  async (dispatch) => {
    await axios.post('/api/projects_board/tasks/move', {
      taskId,
      position,
      listId
    });

    dispatch(
      slice.actions.moveTask({
        taskId,
        position,
        listId
      })
    );
  };

export default slice;
