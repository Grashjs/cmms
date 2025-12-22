const getRuntimeValue = (
  key: string,
  defaultValue = ''
): string | undefined => {
  const envValue = process.env[`REACT_APP_${key}`];
  const runtimeValue = window.__RUNTIME_CONFIG__?.[key]?.trim();
  return envValue || runtimeValue || defaultValue;
};

export const firebaseConfig = {
  apiKey: getRuntimeValue('API_KEY'),
  authDomain: getRuntimeValue('AUTH_DOMAIN'),
  databaseURL: getRuntimeValue('DATABASE_URL'),
  projectId: getRuntimeValue('PROJECT_ID'),
  storageBucket: getRuntimeValue('STORAGE_BUCKET'),
  messagingSenderId: getRuntimeValue('MESSAGING_SENDER_ID'),
  appId: getRuntimeValue('ID'),
  measurementId: getRuntimeValue('MEASUREMENT_ID')
};

export const googleMapsConfig = {
  apiKey: getRuntimeValue('GOOGLE_KEY')
};

const rawApiUrl = getRuntimeValue('API_URL');
export const apiUrl = rawApiUrl
  ? rawApiUrl.endsWith('/')
    ? rawApiUrl
    : rawApiUrl + '/'
  : 'http://localhost:8080/';

export const muiLicense = getRuntimeValue('MUI_X_LICENSE');

export const zendeskKey = '';

export const googleTrackingId = getRuntimeValue('GOOGLE_TRACKING_ID');
export const oauth2Provider = getRuntimeValue('OAUTH2_PROVIDER') as
  | 'GOOGLE'
  | 'MICROSOFT';

export const isEmailVerificationEnabled =
  getRuntimeValue('INVITATION_VIA_EMAIL') === 'true';

export const isCloudVersion = getRuntimeValue('CLOUD_VERSION') === 'true';

const apiHostName = new URL(apiUrl).hostname;
export const IS_LOCALHOST =
  apiHostName === 'localhost' || apiHostName === '127.0.0.1';

export const isSSOEnabled = getRuntimeValue('ENABLE_SSO') === 'true';

export const customLogoPaths: { white?: string; dark: string } =
  getRuntimeValue('LOGO_PATHS')
    ? JSON.parse(getRuntimeValue('LOGO_PATHS'))
    : null;
type ThemeColors = {
  primary: string;
  secondary: string;
  success: string;
  warning: string;
  error: string;
  info: string;
  black: string;
  white: string;
  primaryAlt: string;
};

export const customColors: ThemeColors = getRuntimeValue('CUSTOM_COLORS')
  ? JSON.parse(getRuntimeValue('CUSTOM_COLORS'))
  : null;

export interface BrandRawConfig {
  name: string;
  shortName: string;
  website: string;
  mail: string;
  addressStreet: string;
  phone: string;
  addressCity: string;
}
export const brandRawConfig: BrandRawConfig = getRuntimeValue('BRAND_CONFIG')
  ? JSON.parse(getRuntimeValue('BRAND_CONFIG'))
  : null;

export const demoLink: string = getRuntimeValue('DEMO_LINK');

export const isWhiteLabeled: boolean = !!(customLogoPaths || brandRawConfig);

export const IS_ORIGINAL_CLOUD = !isWhiteLabeled && isCloudVersion;
