"use client";
import {
  Business,
  Construction,
  FlashOn,
  Hotel,
  LocalHospital,
  PrecisionManufacturing,
  Restaurant,
  School,
} from "@mui/icons-material";
import { ElementType } from "react";
import type { useTranslations } from "next-intl";

export type TFunction = ReturnType<typeof useTranslations>;

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

export const getIndustriesLinks = (
  t: TFunction,
): {
  title: string;
  href: string;
  icon: ElementType;
}[] => [
  {
    title: t("manufacturing"),
    href: "/industries/open-source-manufacturing-maintenance-software",
    icon: PrecisionManufacturing,
  },
  {
    title: t("facility_management"),
    href: "/industries/open-source-facility-management-software",
    icon: Business,
  },
  {
    title: t("food_and_beverage"),
    href: "/industries/open-source-food-and-beverage-maintenance-software",
    icon: Restaurant,
  },
  {
    title: t("healthcare"),
    href: "/industries/open-source-healthcare-maintenance-software",
    icon: LocalHospital,
  },
  {
    title: t("energy_and_utilities"),
    href: "/industries/open-source-energy-utilities-maintenance-software",
    icon: FlashOn,
  },
  {
    title: t("education"),
    href: "/industries/open-source-education-maintenance-software",
    icon: School,
  },
  {
    title: t("hospitality"),
    href: "/industries/open-source-hospitality-maintenance-software",
    icon: Hotel,
  },
  {
    title: t("construction"),
    href: "/industries/open-source-construction-maintenance-software",
    icon: Construction,
  },
];

export const getFeaturesLinks = (t: TFunction): { title: string; href: string }[] => [
  {
    title: t("work_order_management"),
    href: "/features/work-orders",
  },
  {
    title: t("asset_management"),
    href: "/features/assets",
  },
  {
    title: t("preventive_maintenance"),
    href: "/features/preventive-maintenance",
  },
  {
    title: t("inventory_management"),
    href: "/features/inventory",
  },
  {
    title: t("analytics_and_reporting"),
    href: "/features/analytics",
  },
];
