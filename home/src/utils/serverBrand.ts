import { apiUrl, BrandRawConfig, brandRawConfig, customLogoPaths } from "../config";
import { LicenseEntitlement, LicensingState } from "../models/owns/license";

const DEFAULT_WHITE_LOGO = "/static/images/logo/logo-white.png";
const DEFAULT_DARK_LOGO = "/static/images/logo/logo.png";
const CUSTOM_DARK_LOGO = `${apiUrl}images/custom-logo.png`;
const CUSTOM_WHITE_LOGO = `${apiUrl}images/custom-logo-white.png`;
import { cache } from "react";

interface LicenseState {
  state: LicensingState;
}
const initialState: LicenseState = {
  state: {
    valid: false,
    entitlements: [],
    expirationDate: null,
    planName: null,
  },
};
export const getLicenseValidityServer = cache(async (): Promise<LicensingState> => {
  try {
    const response = await fetch(`${apiUrl}license/state`, {
      next: { revalidate: 3600 * 24 },
    });
    if (!response.ok) return initialState.state;
    return await response.json();
  } catch (error) {
    console.error("Failed to fetch license server-side:", error);
    return initialState.state;
  }
});
export interface BrandConfig extends BrandRawConfig {
  logo: { white: string; dark: string };
}

export async function getBrandServer(): Promise<BrandConfig> {
  const defaultBrand: Omit<BrandConfig, "logo"> = {
    name: "Atlas CMMS",
    shortName: "Atlas",
    website: "https://www.atlas-cmms.com",
    mail: "contact@atlas-cmms.com",
    phone: "+212 6 30 69 00 50",
    addressStreet: "410, Boulevard Zerktouni, Hamad, №1",
    addressCity: "Casablanca-Morocco 20040",
  };

  const licensingState = await getLicenseValidityServer();
  const isLicenseValid =
    licensingState.valid && licensingState.entitlements.some((e: LicenseEntitlement) => e === "BRANDING");

  return {
    logo: {
      white: customLogoPaths
        ? isLicenseValid == null
          ? null
          : isLicenseValid
            ? CUSTOM_WHITE_LOGO
            : DEFAULT_WHITE_LOGO
        : DEFAULT_WHITE_LOGO,
      dark: customLogoPaths
        ? isLicenseValid == null
          ? null
          : isLicenseValid
            ? CUSTOM_DARK_LOGO
            : DEFAULT_DARK_LOGO
        : DEFAULT_DARK_LOGO,
    },
    ...(isLicenseValid && brandRawConfig ? brandRawConfig : defaultBrand),
  };
}
