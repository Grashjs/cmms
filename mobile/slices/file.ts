import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import File, { FileType } from '../models/file';
import api, { authHeader } from '../utils/api';
import { getInitialPage, Page, SearchCriteria } from '../models/page';
import { AsyncStorage } from 'react-native';
import { revertAll } from '../utils/redux';

const basePath = 'files';

interface FileState {
  files: Page<File>;
  singleFile: File;
  loadingGet: boolean;
}

const initialState: FileState = {
  files: getInitialPage<File>(),
  singleFile: null,
  loadingGet: false
};

const slice = createSlice({
  name: 'files',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getFiles(state: FileState, action: PayloadAction<{ files: Page<File> }>) {
      const { files } = action.payload;
      state.files = files;
    },
    getSingleFile(state: FileState, action: PayloadAction<{ file: File }>) {
      const { file } = action.payload;
      state.singleFile = file;
    },
    editFile(state: FileState, action: PayloadAction<{ file: File }>) {
      const { file } = action.payload;
      const inContent = state.files.content.some(
        (file1) => file1.id === file.id
      );
      if (inContent) {
        state.files.content = state.files.content.map((file1) => {
          if (file1.id === file.id) {
            return file;
          }
          return file1;
        });
      } else {
        state.singleFile = file;
      }
    },
    addFile(state: FileState, action: PayloadAction<{ file: File }>) {
      const { file } = action.payload;
      state.files.content = [...state.files.content, file];
    },
    addFiles(state: FileState, action: PayloadAction<{ files: File[] }>) {
      const { files } = action.payload;
      let visibleFiles = files.filter((file) => !file.hidden);
      state.files.content = [...state.files.content, ...visibleFiles];
    },
    deleteFile(state: FileState, action: PayloadAction<{ id: number }>) {
      const { id } = action.payload;
      const fileIndex = state.files.content.findIndex((file) => file.id === id);
      state.files.content.splice(fileIndex, 1);
    },
    setLoadingGet(
      state: FileState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSingleFile(state: FileState, action: PayloadAction<{}>) {
      state.singleFile = null;
    }
  }
});

export const reducer = slice.reducer;

export const getFiles =
  (criteria: SearchCriteria): AppThunk =>
    async (dispatch) => {
      try {
        dispatch(slice.actions.setLoadingGet({ loading: true }));
        const files = await api.post<Page<File>>(`${basePath}/search`, criteria);
        dispatch(slice.actions.getFiles({ files }));
      } finally {
        dispatch(slice.actions.setLoadingGet({ loading: false }));
      }
    };

export const getSingleFile =
  (id: number): AppThunk =>
    async (dispatch) => {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const file = await api.get<File>(`${basePath}/${id}`);
      dispatch(slice.actions.getSingleFile({ file }));
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    };

export const editFile =
  (id: number, file): AppThunk =>
    async (dispatch) => {
      const fileResponse = await api.patch<File>(`${basePath}/${id}`, file);
      dispatch(slice.actions.editFile({ file: fileResponse }));
    };

export const addFiles =
  (
    files: { uri: string; name: string; type: string }[],
    fileType: FileType = 'OTHER',
    taskId?: number,
    hidden?: 'true' | 'false'
  ): AppThunk =>
    async (dispatch) => {
      let formData = new FormData();
      const companyId = await AsyncStorage.getItem('companyId');
      const headers = await authHeader(false);
      delete headers['Content-Type'];
      files.forEach((file) => {
        //@ts-ignore
        formData.append('files', file);
      });
      formData.append('folder', `company ${companyId}`);
      formData.append('type', fileType);
      formData.append('hidden', hidden);
      const baseRoute = `${basePath}/upload`;
      const filesResponse = await api.post<File[]>(
        taskId ? `${baseRoute}?taskId=${taskId}` : baseRoute,
        formData,
        {
          headers
        },
        true,
        true
      );
      dispatch(slice.actions.addFiles({ files: filesResponse }));
      return filesResponse.map((file) => file.id);
    };
export const deleteFile =
  (id: number): AppThunk =>
    async (dispatch) => {
      const fileResponse = await api.deletes<{ success: boolean }>(
        `${basePath}/${id}`
      );
      const { success } = fileResponse;
      if (success) {
        dispatch(slice.actions.deleteFile({ id }));
      }
    };
export const clearSingleFile = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSingleFile({}));
};

export default slice;
