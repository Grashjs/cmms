import {
  apiUrl,
  BrandRawConfig,
  brandRawConfig,
  customLogoPaths
} from '../config';
import { getLicenseValidityServer } from '../slices/license';
import { LicenseEntitlement } from '../models/owns/license';

const DEFAULT_WHITE_LOGO = '/static/images/logo/logo-white.png';
const DEFAULT_DARK_LOGO = '/static/images/logo/logo.png';
const CUSTOM_DARK_LOGO = `${apiUrl}images/custom-logo.png`;
const CUSTOM_WHITE_LOGO = `${apiUrl}images/custom-logo-white.png`;

export interface BrandConfig extends BrandRawConfig {
  logo: { white: string; dark: string };
}

export async function getBrandServer(): Promise<BrandConfig> {
  const defaultBrand: Omit<BrandConfig, 'logo'> = {
    name: 'Atlas CMMS',
    shortName: 'Atlas',
    website: 'https://www.atlas-cmms.com',
    mail: 'contact@atlas-cmms.com',
    phone: '+212 6 30 69 00 50',
    addressStreet: '410, Boulevard Zerktouni, Hamad, №1',
    addressCity: 'Casablanca-Morocco 20040'
  };

  const licensingState = await getLicenseValidityServer();
  const isLicenseValid = licensingState.valid && licensingState.entitlements.some((e: LicenseEntitlement) => e === 'BRANDING');

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
        : DEFAULT_DARK_LOGO
    },
    ...(isLicenseValid && brandRawConfig ? brandRawConfig : defaultBrand)
  };
}
