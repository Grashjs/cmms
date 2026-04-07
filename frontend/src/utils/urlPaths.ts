import {
  Business,
  Construction,
  FlashOn,
  Hotel,
  LocalHospital,
  PrecisionManufacturing,
  Restaurant,
  School
} from '@mui/icons-material';
import { ElementType } from 'react';
import { TFunction } from 'react-i18next';
import { homeUrl } from '../config';

export const getAssetUrl = (id) => {
  return `/app/assets/${id}/details`;
};

export const getLocationUrl = (id) => {
  return `/app/locations/${id}`;
};

export const getUserUrl = (id) => {
  return `/app/people-teams/people/${id}`;
};
export const getTeamUrl = (id) => {
  return `/app/people-teams/teams/${id}`;
};

export const getRequestUrl = (id) => {
  return `/app/requests/${id}`;
};

export const getWorkOrderUrl = (id) => {
  return `/app/work-orders/${id}`;
};

export const getPartUrl = (id) => {
  return `/app/inventory/parts/${id}`;
};

export const getMeterUrl = (id) => {
  return `/app/meters/${id}`;
};

export const getCustomerUrl = (id) => {
  return `/app/vendors-customers/customers/${id}`;
};
export const getVendorUrl = (id) => {
  return `/app/vendors-customers/vendors/${id}`;
};
export const getPurchaseOrderUrl = (id) => {
  return `/app/purchase-orders/${id}`;
};
export const getPreventiveMaintenanceUrl = (id) => {
  return `/app/preventive-maintenances/${id}`;
};

export const getLocalizedHomeUrl = (
  path: string,
  lang: string,
  params?: Record<string, string>
) => {
  const base = homeUrl || 'https://atlas-cmms.com';
  const url = new URL(`${lang.replace('_', '-').toLowerCase()}/${path}`, base);

  if (params) {
    Object.entries(params).forEach(([key, value]) =>
      url.searchParams.set(key, value)
    );
  }

  return url.toString();
};
