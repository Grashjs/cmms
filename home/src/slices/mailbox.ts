import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import axios from 'src/utils/axios';
import objectArray from 'src/utils/objectArray';
import type { Mail, Tag } from 'src/models/mailbox';

interface MailState {
  mails: {
    byId: Record<string, Mail>;
    allIds: string[];
  };
  tags: Tag[];
  sidebarOpen: boolean;
}

const initialState: MailState = {
  mails: {
    byId: {},
    allIds: []
  },
  tags: [],
  sidebarOpen: false
};

const slice = createSlice({
  name: 'mail',
  initialState,
  reducers: {
    getTags(state: MailState, action: PayloadAction<{ tags: Tag[] }>) {
      const { tags } = action.payload;

      state.tags = tags;
    },
    getMails(state: MailState, action: PayloadAction<{ mails: Mail[] }>) {
      const { mails } = action.payload;

      state.mails.byId = objectArray(mails);
      state.mails.allIds = Object.keys(state.mails.byId);
    },
    getMail(state: MailState, action: PayloadAction<{ mail: Mail }>) {
      const { mail } = action.payload;

      state.mails.byId[mail.id] = mail;

      if (!state.mails.allIds.includes(mail.id)) {
        state.mails.allIds.push(mail.id);
      }
    },
    openSidebar(state: MailState): void {
      state.sidebarOpen = true;
    },
    closeSidebar(state: MailState): void {
      state.sidebarOpen = false;
    }
  }
});

export const reducer = slice.reducer;

export const getTags = (): AppThunk => async (dispatch) => {
  const response = await axios.get<{ tags: Tag[] }>('/api/mailbox/tags');

  dispatch(slice.actions.getTags(response.data));
};

export const getMails =
  (params: {}): AppThunk =>
  async (dispatch) => {
    const response = await axios.get<{ mails: Mail[] }>('/api/mailbox/mails', {
      params
    });

    dispatch(slice.actions.getMails(response.data));
  };

export const getMail =
  (mailboxCategory: string): AppThunk =>
  async (dispatch) => {
    const response = await axios.get<{ mail: Mail }>('/api/mailbox/mail', {
      params: {
        mailboxCategory
      }
    });

    dispatch(slice.actions.getMail(response.data));
  };

export const openSidebar =
  (): AppThunk =>
  async (dispatch): Promise<void> => {
    dispatch(slice.actions.openSidebar());
  };

export const closeSidebar =
  (): AppThunk =>
  async (dispatch): Promise<void> => {
    dispatch(slice.actions.closeSidebar());
  };

export default slice;
