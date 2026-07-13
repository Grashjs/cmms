import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import Location, { LocationMiniDTO, LocationRow } from '../models/location';
import api from '../utils/api';
import { getInitialPage, Page, SearchCriteria } from '../models/page';
import { revertAll } from '../utils/redux';

interface LocationState {
  locations: Page<Location>;
  locationInfos: { [key: number]: { location?: Location } };
  locationsHierarchy: LocationRow[];
  locationsMini: LocationMiniDTO[];
  currentPageNum: number;
  lastPage: boolean;
  loadingGet: boolean;
}

const initialState: LocationState = {
  locations: getInitialPage<Location>(),
  locationInfos: {},
  locationsHierarchy: [],
  locationsMini: [],
  currentPageNum: 0,
  lastPage: true,
  loadingGet: false
};

const slice = createSlice({
  name: 'locations',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getLocations(
      state: LocationState,
      action: PayloadAction<{ locations: Page<Location> }>
    ) {
      const { locations } = action.payload;
      state.locations = locations;
      state.currentPageNum = 0;
      state.lastPage = locations.last;
    },
    getMoreLocations(
      state: LocationState,
      action: PayloadAction<{ locations: Page<Location> }>
    ) {
      const { locations } = action.payload;
      state.locations.content = state.locations.content.concat(
        locations.content
      );
      state.currentPageNum = state.currentPageNum + 1;
      state.lastPage = locations.last;
    },
    getLocationDetails(
      state: LocationState,
      action: PayloadAction<{ location; id: number }>
    ) {
      const { location, id } = action.payload;
      if (state.locationInfos[id]) {
        state.locationInfos[id] = { ...state.locationInfos[id], location };
      } else state.locationInfos[id] = { location };
    },
    getLocationsMini(
      state: LocationState,
      action: PayloadAction<{ locations: LocationMiniDTO[] }>
    ) {
      const { locations } = action.payload;
      state.locationsMini = locations;
    },
    addLocation(
      state: LocationState,
      action: PayloadAction<{ location: Location }>
    ) {
      const { location } = action.payload;
      state.locations.content = [...state.locations.content, location];
    },
    editLocation(
      state: LocationState,
      action: PayloadAction<{ location: Location }>
    ) {
      const { location } = action.payload;
      if (state.locationInfos[location.id]) {
        state.locationInfos[location.id].location = location;
      } else state.locationInfos[location.id] = { location };
    },
    deleteLocation(
      state: LocationState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      const locationIndex = state.locations.content.findIndex(
        (location) => location.id === id
      );
      if (locationIndex !== -1)
        state.locations.content.splice(locationIndex, 1);
    },
    getLocationChildren(
      state: LocationState,
      action: PayloadAction<{ locations: LocationRow[]; id: number }>
    ) {
      const { locations, id } = action.payload;
      const parent = state.locationsHierarchy.findIndex(
        (location) => location.id === id
      );
      if (parent !== -1)
        state.locationsHierarchy[parent].childrenFetched = true;

      state.locationsHierarchy = locations.reduce((acc, location) => {
        //check if location already exists in state
        const locationInState = state.locationsHierarchy.findIndex(
          (location1) => location1.id === location.id
        );
        //not found
        if (locationInState === -1) return [...acc, location];
        //found
        acc[locationInState] = location;
        return acc;
      }, state.locationsHierarchy);
    },
    setLoadingGet(
      state: LocationState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getLocations =
  (criteria: SearchCriteria): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const locations = await api.post<Page<Location>>(
        `locations/search`,
        criteria
      );
      dispatch(slice.actions.getLocations({ locations }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const getMoreLocations =
  (criteria: SearchCriteria, pageNum: number): AppThunk =>
  async (dispatch) => {
    criteria = { ...criteria, pageNum };
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const locations = await api.post<Page<Location>>(
        `locations/search`,
        criteria
      );
      dispatch(slice.actions.getMoreLocations({ locations }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const getLocationDetails =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const location = await api.get<Location>(`locations/${id}`);
    dispatch(
      slice.actions.getLocationDetails({
        id,
        location
      })
    );
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };
export const getLocationsMini = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoadingGet({ loading: true }));
  const locations = await api.get<LocationMiniDTO[]>('locations/mini');
  dispatch(slice.actions.getLocationsMini({ locations }));
  dispatch(slice.actions.setLoadingGet({ loading: false }));
};
export const addLocation =
  (location): AppThunk =>
  async (dispatch) => {
    const locationResponse = await api.post<Location>('locations', location);
    dispatch(slice.actions.addLocation({ location: locationResponse }));
  };
export const editLocation =
  (id: number, location): AppThunk =>
  async (dispatch) => {
    const locationResponse = await api.patch<Location>(
      `locations/${id}`,
      location
    );
    dispatch(slice.actions.editLocation({ location: locationResponse }));
  };
export const deleteLocation =
  (id: number): AppThunk =>
  async (dispatch) => {
    const locationResponse = await api.deletes<{ success: boolean }>(
      `locations/${id}`
    );
    const { success } = locationResponse;
    if (success) {
      dispatch(slice.actions.deleteLocation({ id }));
    }
  };

export const getLocationChildren =
  (id: number, parents: number[]): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const locations = await api.get<Location[]>(`locations/children/${id}`);
    dispatch(
      slice.actions.getLocationChildren({
        id,
        locations: locations.map((location) => {
          return { ...location, hierarchy: [...parents, location.id] };
        })
      })
    );
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };
export default slice;
