import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import Location, {
  LocationMiniDTO,
  LocationRow
} from '../models/owns/location';
import api, { authHeader } from '../utils/api';
import { revertAll } from 'src/utils/redux';
import {
  getInitialPage,
  Page,
  Pageable,
  pageableToQueryParams,
  SearchCriteria
} from '../models/owns/page';
import {
  cancellableFetch,
} from 'src/utils/cancellableRequest';

interface LocationState {
  locations: Page<Location>;
  locationsHierarchy: LocationRow[];
  locationsMini: LocationMiniDTO[];
  loadingGet: boolean;
  loadingHierarchy: boolean;
  childrenPages: { [key: number]: Page<Location> };
}

const initialState: LocationState = {
  locations: getInitialPage<Location>(),
  locationsHierarchy: [],
  locationsMini: [],
  loadingGet: false,
  loadingHierarchy: false,
  childrenPages: {}
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
      state.locations.content.push(location);
    },
    editLocation(
      state: LocationState,
      action: PayloadAction<{ location: Location }>
    ) {
      const { location } = action.payload;
      const locationIndex = state.locations.content.findIndex(
        (loc) => loc.id === location.id
      );
      if (locationIndex === -1) {
        state.locations.content = [...state.locations.content, location];
      } else {
        state.locations[locationIndex] = location;
      }
    },
    deleteLocation(
      state: LocationState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      const locationIndex = state.locations.content.findIndex(
        (location) => location.id === id
      );
      state.locations.content.splice(locationIndex, 1);
    },
    getLocationChildrenPaginated(
      state: LocationState,
      action: PayloadAction<{
        locations: Page<Location>;
        id: number;
        parents: number[];
      }>
    ) {
      const { locations, id, parents } = action.payload;
      const parent = state.locationsHierarchy.findIndex(
        (location) => location.id === id
      );
      if (parent !== -1)
        state.locationsHierarchy[parent].childrenFetched = true;

      state.locationsHierarchy = locations.content.reduce((acc, location) => {
        const locationInState = state.locationsHierarchy.findIndex(
          (l) => l.id === location.id
        );
        const locationWithHierarchy = {
          ...location,
          hierarchy: [...parents, location.id]
        } as LocationRow;
        if (locationInState === -1) return [...acc, locationWithHierarchy];
        acc[locationInState] = locationWithHierarchy;
        return acc;
      }, state.locationsHierarchy);

      state.childrenPages[id] = locations;
    },
    setLoadingHierarchy(
      state: LocationState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingHierarchy = loading;
    },
    setLoadingGet(
      state: LocationState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    resetHierarchy(state: LocationState, action: PayloadAction<{}>) {
      state.locationsHierarchy = [];
    }
  }
});

export const reducer = slice.reducer;

export const getLocations =
  (criteria: SearchCriteria): AppThunk =>
  async (dispatch) => {
    await cancellableFetch(
      dispatch,
      'getLocations',
      (signal) => api.post<Page<Location>>(`locations/search`, criteria, { signal }),
      (locations) => dispatch(slice.actions.getLocations({ locations })),
      (loading) => dispatch(slice.actions.setLoadingGet({ loading }))
    );
  };
export const getLocationsMini = (): AppThunk => async (dispatch) => {
  await cancellableFetch(
    dispatch,
    'getLocationsMini',
    (signal) => api.get<LocationMiniDTO[]>('locations/mini', { signal }),
    (locations) => dispatch(slice.actions.getLocationsMini({ locations })),
    (loading) => dispatch(slice.actions.setLoadingGet({ loading }))
  );
};
export const getPublicLocationsMini =
  (portalUUID: string): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const locations = await api.get<LocationMiniDTO[]>(
        `locations/public/mini/${portalUUID}`,
        { headers: authHeader(true) }
      );
      dispatch(slice.actions.getLocationsMini({ locations }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const addLocation =
  (location): AppThunk =>
  async (dispatch) => {
    const locationResponse = await api.post<Location>('locations', location);
    dispatch(slice.actions.addLocation({ location: locationResponse }));
    return locationResponse;
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
export const getSingleLocation =
  (id: number): AppThunk =>
  async (dispatch) => {
    const locationResponse = await api.get<Location>(`locations/${id}`);
    dispatch(slice.actions.editLocation({ location: locationResponse }));
    return locationResponse;
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

export const getLocationChildrenPaginated =
  (id: number, parents: number[], pageable: Pageable): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingHierarchy({ loading: true }));
    const locations = await api.get<Page<Location>>(
      `locations/children/${id}/paginated?${pageableToQueryParams(pageable)}`
    );
    dispatch(
      slice.actions.getLocationChildrenPaginated({
        id,
        locations,
        parents
      })
    );
    dispatch(slice.actions.setLoadingHierarchy({ loading: false }));
  };

export const resetLocationsHierarchy =
  (pageable: Pageable, callApi: boolean): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.resetHierarchy({}));
    if (callApi) {
      dispatch(getLocationChildrenPaginated(0, [], pageable));
    }
  };

export default slice;
