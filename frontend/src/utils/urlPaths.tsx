import { Business, FlashOn, PrecisionManufacturing } from '@mui/icons-material';
import { ElementType } from 'react';

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

export const industriesLinks: {
  title: string;
  href: string;
  icon: ElementType;
}[] = [
  {
    title: 'Facility Management',
    href: '/industries/open-source-facility-management-software',
    icon: Business
  },
  {
    title: 'Energy & Utilities',
    href: '/industries/open-source-energy-utilities-maintenance-software',
    icon: FlashOn
  },
  {
    title: 'Manufacturing',
    href: '/industries/open-source-manufacturing-maintenance-software',
    icon: PrecisionManufacturing
  }
];

export const featuresLinks: { title: string; href: string }[] = [
  {
    title: 'Work Order Management',
    href: '/features/work-orders'
  },
  {
    title: 'Asset Management',
    href: '/features/assets'
  },
  {
    title: 'Preventive Maintenance',
    href: '/features/preventive-maintenance'
  },
  {
    title: 'Inventory Management',
    href: '/features/inventory'
  },
  {
    title: 'Analytics and Reporting',
    href: '/features/analytics'
  }
];
