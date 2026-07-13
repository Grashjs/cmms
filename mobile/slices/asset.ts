import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import { AssetDTO, AssetMiniDTO, AssetRow } from '../models/asset';
import api from '../utils/api';
import WorkOrder from '../models/workOrder';
import { getInitialPage, Page, SearchCriteria } from '../models/page';
import { revertAll } from '../utils/redux';

const basePath = 'assets';

interface AssetState {
  assets: Page<AssetDTO>;
  assetsHierarchy: AssetRow[];
  assetInfos: { [key: number]: { asset?: AssetDTO; workOrders: WorkOrder[] } };
  assetsByLocation: { [key: number]: AssetDTO[] };
  assetsByPart: { [key: number]: AssetDTO[] };
  assetsMini: AssetMiniDTO[];
  currentPageNum: number;
  lastPage: boolean;
  loadingGet: boolean;
  loadingWorkOrders: boolean;
}

const initialState: AssetState = {
  assets: getInitialPage<AssetDTO>(),
  assetsHierarchy: [],
  assetInfos: {},
  assetsByLocation: {},
  assetsByPart: {},
  assetsMini: [],
  currentPageNum: 0,
  lastPage: true,
  loadingGet: false,
  loadingWorkOrders: false
};

const slice = createSlice({
  name: 'assets',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getAssets(
      state: AssetState,
      action: PayloadAction<{ assets: Page<AssetDTO> }>
    ) {
      const { assets } = action.payload;
      state.assets = assets;
      state.currentPageNum = 0;
      state.lastPage = assets.last;
    },
    getMoreAssets(
      state: AssetState,
      action: PayloadAction<{ assets: Page<AssetDTO> }>
    ) {
      const { assets } = action.payload;
      state.assets.content = state.assets.content.concat(assets.content);
      state.currentPageNum = state.currentPageNum + 1;
      state.lastPage = assets.last;
    },
    getAssetsMini(
      state: AssetState,
      action: PayloadAction<{ assets: AssetMiniDTO[] }>
    ) {
      const { assets } = action.payload;
      state.assetsMini = assets;
    },
    addAsset(state: AssetState, action: PayloadAction<{ asset: AssetDTO }>) {
      const { asset } = action.payload;
      state.assets.content = [...state.assets.content, asset];
    },
    editAsset(state: AssetState, action: PayloadAction<{ asset: AssetDTO }>) {
      const { asset } = action.payload;
      if (state.assetInfos[asset.id]) {
        state.assetInfos[asset.id].asset = asset;
      } else state.assetInfos[asset.id] = { asset, workOrders: [] };
    },
    deleteAsset(state: AssetState, action: PayloadAction<{ id: number }>) {
      const { id } = action.payload;
      const assetIndex = state.assets.content.findIndex(
        (asset) => asset.id === id
      );
      state.assets.content.splice(assetIndex, 1);
    },
    setLoadingGet(
      state: AssetState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    setLoadingWorkOrders(
      state: AssetState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingWorkOrders = loading;
    },
    getAssetChildren(
      state: AssetState,
      action: PayloadAction<{ assets: AssetRow[]; id: number }>
    ) {
      const { assets, id } = action.payload;
      const parent = state.assetsHierarchy.findIndex(
        (asset) => asset.id === id
      );
      if (parent !== -1) state.assetsHierarchy[parent].childrenFetched = true;

      state.assetsHierarchy = assets.reduce((acc, asset) => {
        //check if asset already exists in state
        const assetInState = state.assetsHierarchy.findIndex(
          (asset1) => asset1.id === asset.id
        );
        //not found
        if (assetInState === -1) return [...acc, asset];
        //found
        acc[assetInState] = asset;
        return acc;
      }, state.assetsHierarchy);
    },
    getAssetDetails(
      state: AssetState,
      action: PayloadAction<{ asset: AssetDTO; id: number }>
    ) {
      const { asset, id } = action.payload;
      if (state.assetInfos[id]) {
        state.assetInfos[id] = { ...state.assetInfos[id], asset };
      } else state.assetInfos[id] = { asset, workOrders: [] };
    },
    getAssetWorkOrders(
      state: AssetState,
      action: PayloadAction<{ workOrders: WorkOrder[]; id: number }>
    ) {
      const { workOrders, id } = action.payload;
      if (state.assetInfos[id]) {
        state.assetInfos[id] = { ...state.assetInfos[id], workOrders };
      } else state.assetInfos[id] = { workOrders };
    },
    getAssetsByLocation(
      state: AssetState,
      action: PayloadAction<{ assets: AssetDTO[]; id: number }>
    ) {
      const { assets, id } = action.payload;
      state.assetsByLocation[id] = assets;
    },
    getAssetsByPart(
      state: AssetState,
      action: PayloadAction<{ assets: AssetDTO[]; id: number }>
    ) {
      const { assets, id } = action.payload;
      state.assetsByPart[id] = assets;
    }
  }
});

export const reducer = slice.reducer;

export const getAssets =
  (criteria: SearchCriteria): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const assets = await api.post<Page<AssetDTO>>(
        `${basePath}/search`,
        criteria
      );
      dispatch(slice.actions.getAssets({ assets }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const getMoreAssets =
  (criteria: SearchCriteria, pageNum: number): AppThunk =>
  async (dispatch) => {
    criteria = { ...criteria, pageNum };
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const assets = await api.post<Page<AssetDTO>>(
        `${basePath}/search`,
        criteria
      );
      dispatch(slice.actions.getMoreAssets({ assets }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const getAssetsMini = (locationId?: number|null): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoadingGet({ loading: true }));
  const assets = await api.get<AssetMiniDTO[]>(`${basePath}/mini?locationId=${locationId??""}`);
  dispatch(slice.actions.getAssetsMini({ assets }));
  dispatch(slice.actions.setLoadingGet({ loading: false }));
};
export const addAsset =
  (asset): AppThunk =>
  async (dispatch) => {
    const assetResponse = await api.post<AssetDTO>(basePath, asset);
    dispatch(slice.actions.addAsset({ asset: assetResponse }));
  };
export const editAsset =
  (id: number, asset: Partial<AssetDTO>): AppThunk =>
  async (dispatch) => {
    const assetResponse = await api.patch<AssetDTO>(`${basePath}/${id}`, asset);
    dispatch(slice.actions.editAsset({ asset: assetResponse }));
  };
export const getSingleAsset =
  (id: number): AppThunk =>
  async (dispatch) => {
    const assetResponse = await api.get<AssetDTO>(`${basePath}/${id}`);
    dispatch(slice.actions.editAsset({ asset: assetResponse }));
  };
export const deleteAsset =
  (id: number): AppThunk =>
  async (dispatch) => {
    const assetResponse = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = assetResponse;
    if (success) {
      dispatch(slice.actions.deleteAsset({ id }));
    }
  };

export const getAssetChildren =
  (id: number, parents: number[]): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const assets = await api.get<AssetDTO[]>(`${basePath}/children/${id}`);
    dispatch(
      slice.actions.getAssetChildren({
        id,
        assets: assets.map((asset) => {
          return { ...asset, hierarchy: [...parents, asset.id] };
        })
      })
    );
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };

export const getAssetDetails =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const asset = await api.get<AssetDTO>(`${basePath}/${id}`);
    dispatch(
      slice.actions.getAssetDetails({
        id,
        asset
      })
    );
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };
export const getAssetByNfc =
  (nfcId: string): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const asset = await api.get<AssetDTO>(`${basePath}/nfc/${nfcId}`);
    dispatch(slice.actions.setLoadingGet({ loading: false }));
    return asset.id;
  };
export const getAssetByBarcode =
  (barCode: string): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const asset = await api.get<AssetDTO>(`${basePath}/barcode/${barCode}`);
    dispatch(slice.actions.setLoadingGet({ loading: false }));
    return asset.id;
  };
export const getAssetWorkOrders =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingWorkOrders({ loading: true }));
    const workOrders = await api.get<WorkOrder[]>(`work-orders/asset/${id}`);
    dispatch(
      slice.actions.getAssetWorkOrders({
        id,
        workOrders
      })
    );
    dispatch(slice.actions.setLoadingWorkOrders({ loading: false }));
  };

export const getAssetsByLocation =
  (id: number): AppThunk =>
  async (dispatch) => {
    const assets = await api.get<AssetDTO[]>(`${basePath}/location/${id}`);
    dispatch(
      slice.actions.getAssetsByLocation({
        id,
        assets
      })
    );
  };

export const getAssetsByPart =
  (id: number): AppThunk =>
  async (dispatch) => {
    const assets = await api.get<AssetDTO[]>(`${basePath}/part/${id}`);
    dispatch(
      slice.actions.getAssetsByPart({
        id,
        assets
      })
    );
  };
export default slice;
