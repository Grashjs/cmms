import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import Team, { TeamMiniDTO } from '../models/owns/team';
import api from '../utils/api';
import { getInitialPage, Page, SearchCriteria } from '../models/owns/page';
import { revertAll } from 'src/utils/redux';

const basePath = 'teams';
interface TeamState {
  teams: Page<Team>;
  singleTeam: Team;
  teamsMini: TeamMiniDTO[];
  loadingGet: boolean;
}

const initialState: TeamState = {
  teams: getInitialPage<Team>(),
  singleTeam: null,
  teamsMini: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'teams',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getTeams(state: TeamState, action: PayloadAction<{ teams: Page<Team> }>) {
      const { teams } = action.payload;
      state.teams = teams;
    },
    getSingleTeam(state: TeamState, action: PayloadAction<{ team: Team }>) {
      const { team } = action.payload;
      state.singleTeam = team;
    },
    editTeam(state: TeamState, action: PayloadAction<{ team: Team }>) {
      const { team } = action.payload;
      const inContent = state.teams.content.some(
        (team1) => team1.id === team.id
      );
      if (inContent) {
        state.teams.content = state.teams.content.map((team1) => {
          if (team1.id === team.id) {
            return team;
          }
          return team1;
        });
      } else {
        state.singleTeam = team;
      }
    },
    getTeamsMini(
      state: TeamState,
      action: PayloadAction<{ teams: TeamMiniDTO[] }>
    ) {
      const { teams } = action.payload;
      state.teamsMini = teams;
    },
    addTeam(state: TeamState, action: PayloadAction<{ team: Team }>) {
      const { team } = action.payload;
      state.teams.content = [...state.teams.content, team];
    },
    deleteTeam(state: TeamState, action: PayloadAction<{ id: number }>) {
      const { id } = action.payload;
      const teamIndex = state.teams.content.findIndex((team) => team.id === id);
      state.teams.content.splice(teamIndex, 1);
    },
    setLoadingGet(
      state: TeamState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSingleTeam(state: TeamState, action: PayloadAction<{}>) {
      state.singleTeam = null;
    }
  }
});

export const reducer = slice.reducer;

export const getTeams =
  (criteria: SearchCriteria): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const teams = await api.post<Page<Team>>(`${basePath}/search`, criteria);
      dispatch(slice.actions.getTeams({ teams }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const getSingleTeam =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const team = await api.get<Team>(`${basePath}/${id}`);
    dispatch(slice.actions.getSingleTeam({ team }));
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };

export const editTeam =
  (id: number, team): AppThunk =>
  async (dispatch) => {
    const teamResponse = await api.patch<Team>(`${basePath}/${id}`, team);
    dispatch(slice.actions.editTeam({ team: teamResponse }));
  };

export const getTeamsMini = (): AppThunk => async (dispatch) => {
  const teams = await api.get<TeamMiniDTO[]>('teams/mini');
  dispatch(slice.actions.getTeamsMini({ teams }));
};
export const addTeam =
  (team): AppThunk =>
  async (dispatch) => {
    const teamResponse = await api.post<Team>('teams', team);
    dispatch(slice.actions.addTeam({ team: teamResponse }));
  };
export const deleteTeam =
  (id: number): AppThunk =>
  async (dispatch) => {
    const teamResponse = await api.deletes<{ success: boolean }>(`teams/${id}`);
    const { success } = teamResponse;
    if (success) {
      dispatch(slice.actions.deleteTeam({ id }));
    }
  };
export const clearSingleTeam = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSingleTeam({}));
};

export default slice;
